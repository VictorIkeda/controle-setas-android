using SetasPc;
using SetasPc.Core;

const int Port = 28777;
Console.OutputEncoding = System.Text.Encoding.UTF8;

var sink = InterceptionKeySink.TryCreate(out var erro);
if (sink is null)
{
    Console.WriteLine(erro);
    Console.WriteLine("\nPressione Enter para sair.");
    Console.ReadLine();
    return 1;
}

var controller = new ArrowController(sink, TimeSpan.FromMilliseconds(300));
using var cts = new CancellationTokenSource();
Console.CancelKeyPress += (_, e) => { e.Cancel = true; cts.Cancel(); };
AppDomain.CurrentDomain.ProcessExit += (_, _) => controller.Shutdown();

try
{
    if (args.Contains("--teste"))
        return RunTest(controller);

    await RunNormal(controller, cts.Token);
    return 0;
}
finally
{
    controller.Shutdown();
    sink.Dispose();
}

static async Task RunNormal(ArrowController ctl, CancellationToken ct)
{
    var server = new PhoneServer(Port, ctl);
    bool connected = false;
    string adbStatus = "";
    server.ConnectionChanged += c => connected = c;
    var run = server.RunAsync(ct);

    Console.Clear();
    var nextAdb = DateTime.MinValue;
    while (!ct.IsCancellationRequested && !run.IsCompleted)
    {
        if (!connected && DateTime.UtcNow >= nextAdb)
        {
            adbStatus = Adb.TryReverse(Port, out var e) ? "Aguardando o app abrir..." : e;
            nextAdb = DateTime.UtcNow.AddSeconds(2);
        }
        Draw(connected, connected ? "" : adbStatus, ctl.Current);
        try { await Task.Delay(50, ct); } catch (OperationCanceledException) { }
    }
    try { await run; } catch (OperationCanceledException) { }
}

static void Draw(bool connected, string status, Arrows a)
{
    string On(Arrows k, string s) => a.HasFlag(k) ? $"[{s}]" : $" {s} ";
    var lines = new[]
    {
        "Setas Celular",
        "",
        "Driver Interception: Instalado",
        "Celular (cabo USB):  " + (connected ? "Conectado" : "Aguardando"),
        status,
        "",
        "Setas agora:  " + On(Arrows.Up, "↑") + On(Arrows.Down, "↓") + On(Arrows.Left, "←") + On(Arrows.Right, "→"),
        "",
        "Ctrl+C para sair. Se o cabo soltar, todas as setas são soltas na hora.",
    };
    try
    {
        Console.SetCursorPosition(0, 0);
        int w = Math.Max(20, Console.WindowWidth - 1);
        foreach (var l in lines) Console.WriteLine(l.Length > w ? l[..w] : l.PadRight(w));
    }
    catch (IOException)
    {
        // Saída redirecionada: sem tela de status.
    }
}

static int RunTest(ArrowController ctl)
{
    Console.WriteLine("Modo de teste: troque para o jogo (modo treino).");
    for (int s = 5; s > 0; s--)
    {
        Console.WriteLine($"Começando em {s}...");
        Thread.Sleep(1000);
    }

    void Hold(Arrows a, int ms)
    {
        var end = DateTime.UtcNow.AddMilliseconds(ms);
        while (DateTime.UtcNow < end)
        {
            ctl.OnByte((byte)a, DateTime.UtcNow);
            Thread.Sleep(20);
        }
    }

    Console.WriteLine("↑"); Hold(Arrows.Up, 500);
    Console.WriteLine("→"); Hold(Arrows.Right, 500);
    Console.WriteLine("↓+→"); Hold(Arrows.Down | Arrows.Right, 500);
    ctl.ReleaseAll(); Thread.Sleep(300);
    Console.WriteLine("Meia-lua ↓ ↘ →");
    Hold(Arrows.Down, 100);
    Hold(Arrows.Down | Arrows.Right, 100);
    Hold(Arrows.Right, 100);
    ctl.ReleaseAll();
    Console.WriteLine("Teste concluído. O personagem se moveu em todos os passos?");
    return 0;
}
