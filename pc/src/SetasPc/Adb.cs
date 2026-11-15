using System.Diagnostics;

namespace SetasPc;

/// <summary>Liga a porta do celular à do PC pelo cabo USB (adb reverse).</summary>
internal static class Adb
{
    public static bool TryReverse(int port, out string erro)
    {
        var adb = Find();
        if (adb is null)
        {
            erro = "Não encontrei o adb.exe. Coloque-o na pasta deste programa (vem no Android platform-tools).";
            return false;
        }

        try
        {
            var psi = new ProcessStartInfo(adb, $"reverse tcp:{port} tcp:{port}")
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
            };
            using var p = Process.Start(psi)!;
            p.StandardOutput.ReadToEnd();
            p.StandardError.ReadToEnd();
            if (!p.WaitForExit(5000)) { p.Kill(); }
            if (p.ExitCode == 0)
            {
                erro = "";
                return true;
            }
        }
        catch (Exception)
        {
            // cai na mensagem abaixo
        }

        erro = "Nenhum celular encontrado. Ligue o cabo USB e ative a depuração USB no celular " +
               "(na primeira vez, aceite o aviso \"Permitir depuração USB\" na tela do celular).";
        return false;
    }

    static string? Find()
    {
        var candidates = new List<string> { Path.Combine(AppContext.BaseDirectory, "adb.exe") };
        foreach (var dir in (Environment.GetEnvironmentVariable("PATH") ?? "").Split(Path.PathSeparator))
            if (dir.Length > 0) candidates.Add(Path.Combine(dir, "adb.exe"));
        candidates.Add(Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Android", "Sdk", "platform-tools", "adb.exe"));
        return candidates.FirstOrDefault(File.Exists);
    }
}
