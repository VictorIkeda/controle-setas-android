namespace SetasPc.Core;

/// <summary>Estado das quatro setas, igual ao byte enviado pelo celular.</summary>
[Flags]
public enum Arrows : byte
{
    None = 0,
    Up = 1,
    Down = 2,
    Left = 4,
    Right = 8,
}
