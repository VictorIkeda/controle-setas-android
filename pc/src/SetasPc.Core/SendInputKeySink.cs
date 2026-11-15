using System.Runtime.InteropServices;

namespace SetasPc.Core;

/// <summary>
/// Aperta e solta as setas com o SendInput do próprio Windows (sem driver).
/// Usa o código físico da tecla (scan code), que é o que os jogos costumam ler.
/// Limite: o Windows marca a tecla como simulada; alguns anti-cheats ignoram.
/// </summary>
public sealed class SendInputKeySink : IKeySink
{
    public const uint InputKeyboard = 1;
    public const uint ExtendedKey = 0x0001;
    public const uint KeyUp = 0x0002;
    public const uint ScanCode = 0x0008;

    [StructLayout(LayoutKind.Sequential)]
    public struct KeyboardInput
    {
        public ushort VirtualKey;
        public ushort Scan;
        public uint Flags;
        public uint Time;
        public IntPtr ExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct MouseInput
    {
        public int Dx;
        public int Dy;
        public uint MouseData;
        public uint Flags;
        public uint Time;
        public IntPtr ExtraInfo;
    }

    [StructLayout(LayoutKind.Explicit)]
    public struct InputUnion
    {
        [FieldOffset(0)] public MouseInput Mouse;
        [FieldOffset(0)] public KeyboardInput Keyboard;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct NativeInput
    {
        public uint Type;
        public InputUnion Data;
    }

    [DllImport("user32.dll", SetLastError = true)]
    static extern uint SendInput(uint count, NativeInput[] inputs, int size);

    static readonly int InputSize = Marshal.SizeOf<NativeInput>();

    public void Press(Arrows key) => Send(key, down: true);
    public void Release(Arrows key) => Send(key, down: false);

    static void Send(Arrows key, bool down)
    {
        SendInput(1, new[] { Build(key, down) }, InputSize);
    }

    public static NativeInput Build(Arrows key, bool down) => new()
    {
        Type = InputKeyboard,
        Data = new InputUnion
        {
            Keyboard = new KeyboardInput
            {
                Scan = ScanCodeOf(key),
                Flags = ExtendedKey | ScanCode | (down ? 0 : KeyUp),
            },
        },
    };

    static ushort ScanCodeOf(Arrows key) => key switch
    {
        Arrows.Up => 0x48,
        Arrows.Down => 0x50,
        Arrows.Left => 0x4B,
        Arrows.Right => 0x4D,
        _ => throw new ArgumentOutOfRangeException(nameof(key), key, "Uma seta por vez."),
    };
}
