using System.Net;
using System.Net.Sockets;

namespace SetasPc.Core;

/// <summary>
/// Escuta o celular em 127.0.0.1 (chega pelo cabo via adb reverse). Um celular por vez:
/// um novo cliente substitui o anterior. Qualquer desconexão solta todas as setas.
/// </summary>
public sealed class PhoneServer
{
    readonly int _port;
    readonly ArrowController _ctl;
    readonly object _gate = new();
    TcpClient? _client;
    int _generation;

    /// <summary>Primeiro byte que o PC envia: prova ao celular que o programa está mesmo aberto.</summary>
    public const byte Hello = 0xA5;

    public PhoneServer(int port, ArrowController ctl)
    {
        _port = port;
        _ctl = ctl;
    }

    public event Action<bool>? ConnectionChanged;

    public async Task RunAsync(CancellationToken ct)
    {
        var listener = new TcpListener(IPAddress.Loopback, _port);
        listener.Start();
        using var ticker = new Timer(_ => _ctl.OnTick(DateTime.UtcNow), null, 50, 50);
        try
        {
            while (!ct.IsCancellationRequested)
            {
                var client = await listener.AcceptTcpClientAsync(ct);
                client.NoDelay = true;
                int gen;
                TcpClient? old;
                lock (_gate)
                {
                    old = _client;
                    _client = client;
                    gen = ++_generation;
                }
                if (old is not null)
                {
                    old.Close();
                    _ctl.ReleaseAll();
                }
                try { client.GetStream().WriteByte(Hello); }
                catch (IOException) { }
                ConnectionChanged?.Invoke(true);
                _ = Task.Run(() => ReadLoop(client, gen, ct), CancellationToken.None);
            }
        }
        finally
        {
            listener.Stop();
            lock (_gate)
            {
                _generation++;
                _client?.Close();
            }
            _ctl.ReleaseAll();
        }
    }

    async Task ReadLoop(TcpClient client, int gen, CancellationToken ct)
    {
        var buf = new byte[64];
        try
        {
            var stream = client.GetStream();
            while (true)
            {
                int n = await stream.ReadAsync(buf, ct);
                if (n == 0) break;
                var now = DateTime.UtcNow;
                lock (_gate)
                {
                    if (gen != _generation) return;
                    for (int i = 0; i < n; i++) _ctl.OnByte(buf[i], now);
                }
            }
        }
        catch (Exception)
        {
            // Conexão caiu ou foi substituída: tratado abaixo.
        }
        finally
        {
            client.Close();
        }

        bool wasCurrent;
        lock (_gate)
        {
            wasCurrent = gen == _generation;
            if (wasCurrent)
            {
                _client = null;
                _ctl.ReleaseAll();
            }
        }
        if (wasCurrent) ConnectionChanged?.Invoke(false);
    }
}
