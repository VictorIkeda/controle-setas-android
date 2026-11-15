using SetasPc.Core;

namespace SetasPc;

/// <summary>Aperta e solta as setas pelo driver Interception, como se viessem de um teclado real.</summary>
internal sealed class InterceptionKeySink : IKeySink, IDisposable
{
    const string InstallHelp =
        "O driver Interception não está pronto.\n" +
        "1. Baixe o Interception (versão 1.0.1) em github.com/oblitum/Interception/releases.\n" +
        "2. Abra o Prompt de Comando como administrador e rode:\n" +
        "     \"command line installer\\install-interception.exe\" /install\n" +
        "3. Reinicie o PC.\n" +
        "4. Copie library\\x64\\interception.dll para a mesma pasta deste programa.";

    readonly IntPtr _context;
    readonly int _device;

    InterceptionKeySink(IntPtr context, int device)
    {
        _context = context;
        _device = device;
    }

    public static InterceptionKeySink? TryCreate(out string erro)
    {
        IntPtr context;
        try
        {
            context = Interception.CreateContext();
        }
        catch (DllNotFoundException)
        {
            erro = "Não encontrei o arquivo interception.dll.\n\n" + InstallHelp;
            return null;
        }

        if (context == IntPtr.Zero)
        {
            erro = InstallHelp;
            return null;
        }

        var buffer = new byte[500];
        for (int d = Interception.FirstKeyboard; d <= Interception.LastKeyboard; d++)
        {
            if (Interception.IsKeyboard(d) == 0) continue;
            if (Interception.GetHardwareId(context, d, buffer, (uint)buffer.Length) > 0)
            {
                erro = "";
                return new InterceptionKeySink(context, d);
            }
        }

        Interception.DestroyContext(context);
        erro = "O driver Interception está instalado, mas nenhum teclado foi encontrado. " +
               "Ligue um teclado ao PC e reinicie depois de instalar o driver.";
        return null;
    }

    public void Press(Arrows key) => Send(key, Interception.KeyDown);
    public void Release(Arrows key) => Send(key, Interception.KeyUp);

    void Send(Arrows key, ushort upDown)
    {
        var stroke = new Interception.KeyStroke
        {
            Code = ScanCode(key),
            State = (ushort)(upDown | Interception.KeyE0),
        };
        Interception.Send(_context, _device, ref stroke, 1);
    }

    static ushort ScanCode(Arrows key) => key switch
    {
        Arrows.Up => 0x48,
        Arrows.Down => 0x50,
        Arrows.Left => 0x4B,
        Arrows.Right => 0x4D,
        _ => throw new ArgumentOutOfRangeException(nameof(key), key, "Uma seta por vez."),
    };

    public void Dispose() => Interception.DestroyContext(_context);
}
