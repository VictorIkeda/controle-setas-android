using SetasPc.Core;

namespace SetasPc.Tests;

public sealed class FakeSink : IKeySink
{
    public List<string> Events { get; } = new();
    public void Press(Arrows key) { lock (Events) Events.Add("+" + key); }
    public void Release(Arrows key) { lock (Events) Events.Add("-" + key); }
}

public class ArrowControllerTests
{
    static readonly DateTime T0 = new(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc);

    static (ArrowController ctl, FakeSink sink) Make()
    {
        var sink = new FakeSink();
        return (new ArrowController(sink, TimeSpan.FromMilliseconds(300)), sink);
    }

    [Fact]
    public void Byte_DownRight_PressesBoth()
    {
        var (ctl, sink) = Make();
        ctl.OnByte(10, T0);
        Assert.Equal(new[] { "+Down", "+Right" }, sink.Events);
    }

    [Fact]
    public void Byte_ThenDownOnly_ReleasesRight()
    {
        var (ctl, sink) = Make();
        ctl.OnByte(10, T0);
        ctl.OnByte(2, T0);
        Assert.Equal("-Right", sink.Events[^1]);
        Assert.Equal(Arrows.Down, ctl.Current);
    }

    [Fact]
    public void SameState_NoEvents()
    {
        var (ctl, sink) = Make();
        ctl.OnByte(1, T0);
        ctl.OnByte(1, T0);
        Assert.Equal(new[] { "+Up" }, sink.Events);
    }

    [Fact]
    public void ByteAbove15_Ignored()
    {
        var (ctl, sink) = Make();
        ctl.OnByte(16, T0);
        Assert.Empty(sink.Events);
        Assert.Equal(Arrows.None, ctl.Current);
    }

    [Fact]
    public void Watchdog_300ms_ReleasesAll()
    {
        var (ctl, sink) = Make();
        ctl.OnByte(1, T0);
        ctl.OnTick(T0.AddMilliseconds(299));
        Assert.Equal(new[] { "+Up" }, sink.Events);
        ctl.OnTick(T0.AddMilliseconds(301));
        Assert.Equal("-Up", sink.Events[^1]);
        Assert.Equal(Arrows.None, ctl.Current);
    }

    [Fact]
    public void ReleaseAll_ReleasesOnlyHeld()
    {
        var (ctl, sink) = Make();
        ctl.OnByte(5, T0);
        sink.Events.Clear();
        ctl.ReleaseAll();
        Assert.Equal(new[] { "-Up", "-Left" }, sink.Events);
    }

    [Fact]
    public void KeyDiff_ReleasesBeforePresses()
    {
        var diff = KeyDiff.Compute(Arrows.Up, Arrows.Down);
        Assert.Equal(new[] { (Arrows.Up, false), (Arrows.Down, true) }, diff);
    }
}
