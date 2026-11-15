# Setas no Celular

Use o celular Android como direcional (4 setas + analógico) em jogos de luta no Windows.
O celular fica ligado ao PC por cabo USB; os ataques continuam no teclado.

## Instalar (uma vez)

### No PC
1. **Driver Interception** (faz o Windows ver as setas como um teclado de verdade):
   - Baixe a versão 1.0.1 em https://github.com/oblitum/Interception/releases
   - Abra o Prompt de Comando **como administrador** e rode
     `"command line installer\install-interception.exe" /install`
   - **Reinicie o PC.**
   - Copie `library\x64\interception.dll` para a pasta `dist\` (junto do `SetasPc.exe`).
2. A pasta `dist\` já traz o `SetasPc.exe` e o `adb.exe`. Precisa do .NET 8 Runtime (já instalado neste PC).

### No celular
1. Ative as **Opções do desenvolvedor**: Configurações → Sobre o telefone → toque 7 vezes em "Número da versão".
2. Em Opções do desenvolvedor, ative **Depuração USB**.
3. Ligue o cabo no PC e aceite o aviso "Permitir depuração USB" (marque "Sempre permitir").
4. Instale o app: `dist\adb.exe install -r dist\Setas.apk`

## Jogar
1. Ligue o celular pelo cabo.
2. Abra `dist\SetasPc.exe`. Ele mostra "Driver Interception: Instalado" e espera o celular.
3. Abra o app **Setas** no celular. Os dois lados mostram "Conectado".
4. Abra o jogo.

- **Cadeado** no topo: aberto, você arrasta para mover e puxa o quadrado azul para mudar o tamanho
  (setas em bloco e analógico separado). Fechado, salva. Cada orientação guarda o seu layout.
- **Vibração**: só nas setas; liga/desliga no topo.
- Se o cabo soltar, o app sair da tela ou o celular girar, todas as setas são soltas na hora.

## Testar o driver nos jogos
`dist\SetasPc.exe --teste` → em 5 s ele aperta ↑, →, ↓+→ e faz um meia-lua. Deixe o jogo em foco no modo treino.

## Compilar
- PC: `dotnet test pc` e `dotnet publish pc/src/SetasPc -c Release -r win-x64 --self-contained false -p:PublishSingleFile=true -o dist`
- Android: `cd android && gradlew test assembleDebug` (JDK 17 e Android SDK 34)
