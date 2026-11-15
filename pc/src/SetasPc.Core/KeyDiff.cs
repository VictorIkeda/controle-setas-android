namespace SetasPc.Core;

public static class KeyDiff
{
    static readonly Arrows[] Order = { Arrows.Up, Arrows.Down, Arrows.Left, Arrows.Right };

    /// <summary>Teclas a soltar (primeiro) e a apertar para ir de <paramref name="prev"/> a <paramref name="next"/>.</summary>
    public static IReadOnlyList<(Arrows key, bool down)> Compute(Arrows prev, Arrows next)
    {
        var result = new List<(Arrows, bool)>(4);
        foreach (var k in Order)
            if (prev.HasFlag(k) && !next.HasFlag(k)) result.Add((k, false));
        foreach (var k in Order)
            if (!prev.HasFlag(k) && next.HasFlag(k)) result.Add((k, true));
        return result;
    }
}
