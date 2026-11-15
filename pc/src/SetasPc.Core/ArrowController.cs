namespace SetasPc.Core;

/// <summary>
/// Aplica os estados recebidos do celular ao <see cref="IKeySink"/>, e solta tudo
/// se o celular ficar calado por mais que <c>timeout</c>. Seguro para várias threads.
/// </summary>
public sealed class ArrowController
{
    readonly IKeySink _sink;
    readonly TimeSpan _timeout;
    readonly object _gate = new();
    Arrows _current;
    DateTime _lastByteAt;

    public ArrowController(IKeySink sink, TimeSpan timeout)
    {
        _sink = sink;
        _timeout = timeout;
    }

    public Arrows Current { get { lock (_gate) return _current; } }

    public void OnByte(byte b, DateTime now)
    {
        lock (_gate)
        {
            _lastByteAt = now;
            if (b > 15) return;
            Apply((Arrows)b);
        }
    }

    public void OnTick(DateTime now)
    {
        lock (_gate)
        {
            if (_current != Arrows.None && now - _lastByteAt > _timeout)
                Apply(Arrows.None);
        }
    }

    public void ReleaseAll()
    {
        lock (_gate) Apply(Arrows.None);
    }

    void Apply(Arrows next)
    {
        foreach (var (key, down) in KeyDiff.Compute(_current, next))
        {
            if (down) _sink.Press(key); else _sink.Release(key);
        }
        _current = next;
    }
}
