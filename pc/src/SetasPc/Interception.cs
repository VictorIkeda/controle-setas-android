using System.Runtime.InteropServices;

namespace SetasPc;

/// <summary>Chamadas nativas do driver Interception (interception.dll, x64).</summary>
internal static class Interception
{
    const string Dll = "interception.dll";

    public const ushort KeyDown = 0x00;
    public const ushort KeyUp = 0x01;
    public const ushort KeyE0 = 0x02;

    /// <summary>Teclados vão do dispositivo 1 ao 10.</summary>
    public const int FirstKeyboard = 1;
    public const int LastKeyboard = 10;

    [StructLayout(LayoutKind.Sequential)]
    public struct KeyStroke
    {
        public ushort Code;
        public ushort State;
        public uint Information;
    }

    [DllImport(Dll, EntryPoint = "interception_create_context", CallingConvention = CallingConvention.Cdecl)]
    public static extern IntPtr CreateContext();

    [DllImport(Dll, EntryPoint = "interception_destroy_context", CallingConvention = CallingConvention.Cdecl)]
    public static extern void DestroyContext(IntPtr context);

    [DllImport(Dll, EntryPoint = "interception_send", CallingConvention = CallingConvention.Cdecl)]
    public static extern int Send(IntPtr context, int device, ref KeyStroke stroke, uint nstroke);

    [DllImport(Dll, EntryPoint = "interception_is_keyboard", CallingConvention = CallingConvention.Cdecl)]
    public static extern int IsKeyboard(int device);

    [DllImport(Dll, EntryPoint = "interception_get_hardware_id", CallingConvention = CallingConvention.Cdecl)]
    public static extern uint GetHardwareId(IntPtr context, int device, byte[] buffer, uint bufferSize);
}
