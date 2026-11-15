using System.Net;
using System.Net.Sockets;
using SetasPc.Core;

namespace SetasPc.Tests;

public class PhoneServerTests : IAsyncLifetime
{
    readonly FakeSink _sink = new();
    readonly CancellationTokenSource _cts = new();
    ArrowController _ctl = null!;
    PhoneServer _server = null!;
    Task _run = null!;
    int _port;

    public async Task InitializeAsync()
    {
        var probe = new TcpListener(IPAddress.Loopback, 0);
        probe.Start();
        _port = ((IPEndPoint)probe.LocalEndpoint).Port;
        probe.Stop();

        _ctl = new ArrowController(_sink, TimeSpan.FromMilliseconds(300));
        _server = new PhoneServer(_port, _ctl);
        _run = _server.RunAsync(_cts.Token);
        await Task.Delay(100);
    }

    public async Task DisposeAsync()
    {
        _cts.Cancel();
        try { await _run; } catch (OperationCanceledException) { }
    }

    async Task<TcpClient> Connect()
    {
        var c = new TcpClient { NoDelay = true };
        await c.ConnectAsync(IPAddress.Loopback, _port);
        return c;
    }

    static async Task Eventually(Func<bool> cond, int ms = 200)
    {
        var end = DateTime.UtcNow.AddMilliseconds(ms);
        while (!cond() && DateTime.UtcNow < end) await Task.Delay(5);
        Assert.True(cond());
    }

    [Fact]
    public async Task ClientSendsByte_ControllerPresses()
    {
        using var c = await Connect();
        await c.GetStream().WriteAsync(new byte[] { 0x02 });
        await Eventually(() => _ctl.Current == Arrows.Down);
    }

    [Fact]
    public async Task ClientDisconnects_ReleasesAll()
    {
        var c = await Connect();
        await c.GetStream().WriteAsync(new byte[] { 0x01 });
        await Eventually(() => _ctl.Current == Arrows.Up);
        c.Close();
        await Eventually(() => _ctl.Current == Arrows.None);
    }

    [Fact]
    public async Task SecondClient_ReplacesFirst()
    {
        using var first = await Connect();
        await first.GetStream().WriteAsync(new byte[] { 0x01 });
        await Eventually(() => _ctl.Current == Arrows.Up);

        using var second = await Connect();
        await Eventually(() => _ctl.Current == Arrows.None);

        var buf = new byte[1];
        var readTask = first.GetStream().ReadAsync(buf).AsTask();
        var done = await Task.WhenAny(readTask, Task.Delay(1000));
        Assert.Same(readTask, done);
        Assert.Equal(0, await readTask);

        await second.GetStream().WriteAsync(new byte[] { 0x02 });
        await Eventually(() => _ctl.Current == Arrows.Down);
        await Task.Delay(100);
        Assert.Equal(Arrows.Down, _ctl.Current);
    }
}
