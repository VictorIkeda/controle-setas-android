using System.Runtime.InteropServices;
using SetasPc.Core;

namespace SetasPc.Tests;

public class SendInputKeySinkTests
{
    [Fact]
    public void NativeInput_HasWindowsX64Size()
    {
        // Em x64, INPUT tem 40 bytes; tamanho errado faz o SendInput falhar sem aviso.
        Assert.Equal(40, Marshal.SizeOf<SendInputKeySink.NativeInput>());
    }

    [Theory]
    [InlineData(Arrows.Up, 0x48)]
    [InlineData(Arrows.Down, 0x50)]
    [InlineData(Arrows.Left, 0x4B)]
    [InlineData(Arrows.Right, 0x4D)]
    public void Build_UsesArrowScanCodes(Arrows key, ushort scan)
    {
        var input = SendInputKeySink.Build(key, down: true);
        Assert.Equal(SendInputKeySink.InputKeyboard, input.Type);
        Assert.Equal(scan, input.Data.Keyboard.Scan);
        Assert.Equal(0, input.Data.Keyboard.VirtualKey);
    }

    [Fact]
    public void Build_Down_IsExtendedScanCode()
    {
        var input = SendInputKeySink.Build(Arrows.Up, down: true);
        Assert.Equal(SendInputKeySink.ExtendedKey | SendInputKeySink.ScanCode, input.Data.Keyboard.Flags);
    }

    [Fact]
    public void Build_Up_AddsKeyUpFlag()
    {
        var input = SendInputKeySink.Build(Arrows.Up, down: false);
        Assert.Equal(SendInputKeySink.ExtendedKey | SendInputKeySink.ScanCode | SendInputKeySink.KeyUp,
            input.Data.Keyboard.Flags);
    }
}
