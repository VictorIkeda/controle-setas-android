namespace SetasPc.Core;

/// <summary>Destino que realmente aperta e solta as teclas.</summary>
public interface IKeySink
{
    void Press(Arrows key);
    void Release(Arrows key);
}
