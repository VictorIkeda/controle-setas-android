using SetasPc;
using SetasPc.Core;

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
void Shutdown()
{
    controller.ReleaseAll();
}
Console.CancelKeyPress += (_, e) => { Shutdown(); };
AppDomain.CurrentDomain.ProcessExit += (_, _) => Shutdown();

try
{
    if (args.Contains("--teste"))
        return RunTest(controller);

    Console.WriteLine("Modo normal ainda não implementado.");
    return 0;
}
finally
{
    Shutdown();
    sink.Dispose();
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
