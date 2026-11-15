# Setas no Celular — Plano de Construção

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android que vira um direcional (4 setas + analógico) para jogos de luta no Windows, enviando as setas pelo cabo USB a um programa no PC que as injeta pelo driver Interception.

**Architecture:** Dois programas. `pc/` é um console .NET que escuta TCP em `127.0.0.1:28777`, faz `adb reverse` para o celular alcançá-lo pelo cabo, e converte cada byte de estado em apertar/soltar via Interception. `android/` é um app Kotlin com uma View própria que lê multitoque, calcula o estado das setas e envia ao PC. Toda a lógica (diferença de estado, cão de guarda, acerto de toque, analógico, layout) fica em classes puras testadas sem aparelho.

**Tech Stack:** C# / .NET 8, xUnit, P/Invoke para `interception.dll`. Kotlin, Android Gradle Plugin, minSdk 24, View clássica (sem Compose), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-15-setas-celular-design.md`

## Global Constraints

- Ligação **somente por cabo USB**, via `adb reverse tcp:28777 tcp:28777`. O celular conecta em `127.0.0.1:28777`.
- TCP com `NoDelay = true` (PC) e `tcpNoDelay = true` (Android).
- **Protocolo:** cada mensagem é **1 byte** = estado completo das setas. Bits: `UP=1`, `DOWN=2`, `LEFT=4`, `RIGHT=8`. Bytes > 15 são ignorados pelo PC.
- O celular reenvia o estado atual **a cada 100 ms** (sinal de vida) e imediatamente a cada mudança.
- O PC solta todas as setas se ficar **300 ms** sem receber bytes, se a conexão cair, ou ao fechar.
- Setas injetadas pelo **Interception** com scan codes e flag E0: ↑ `0x48`, ↓ `0x50`, ← `0x4B`, → `0x4D`. Não usar `SendInput`.
- O teclado físico é só para ataques; o PC não acompanha o estado do teclado físico.
- Android: tela cheia imersiva, tela sempre acesa, gira entre horizontal e vertical (`fullSensor`).
- Vibração **só nas setas**, curta (`HapticFeedbackConstants.KEYBOARD_TAP`), quando um bit de seta passa de 0 para 1; analógico nunca vibra; liga/desliga pelo usuário.
- Com o cadeado **aberto**, o celular envia sempre `0`.
- Textos da interface em português.

## Review Focus

1. **Cabo arrancado com seta apertada** → o PC solta todas as setas em ≤ 300 ms (teste em Task 1 e Task 3).
2. **Android cancela o toque** (puxar a barra de notificação, gesto do sistema, `ACTION_CANCEL`) → estado vira 0 e é enviado (teste em Task 4).
3. **Celular gira ou app vai para segundo plano no meio de um toque** → estado 0 enviado antes de qualquer outra coisa (verificação manual em Task 7; lógica `releaseAll` testada em Task 4).
4. **Dois dedos na mesma seta, um levanta** → a seta continua apertada (teste em Task 4).
5. **Abrir o cadeado segurando uma seta** → estado 0 enviado e nada mais até fechar (teste em Task 4).

---

## File Structure

```
pc/
  SetasPc.sln
  src/SetasPc.Core/            lógica pura, sem Windows
    Arrows.cs                  [Flags] enum Arrows
    KeyDiff.cs                 diferença entre dois estados
    IKeySink.cs                interface de apertar/soltar
    ArrowController.cs         aplica estados, cão de guarda, soltar tudo
  src/SetasPc/                 executável
    Interception.cs            P/Invoke
    InterceptionKeySink.cs     IKeySink real
    Adb.cs                     adb reverse + detecção de celular
    PhoneServer.cs             servidor TCP
    Program.cs                 junta tudo; modo --teste
  tests/SetasPc.Tests/
android/
  settings.gradle.kts, build.gradle.kts, app/build.gradle.kts
  app/src/main/java/com/setas/controle/
    Arrows.kt                  constantes de bits
    BlockGeometry.kt           posição/tamanho do bloco e acerto de toque
    StickMapper.kt             analógico → bits
    TouchState.kt              junta dedos + analógico, decide vibração
    LayoutModel.kt             layouts padrão, limites, ajuste
    LayoutStore.kt             salva em SharedPreferences
    PhoneLink.kt               cliente TCP com reconexão e sinal de vida
    ControllerView.kt          desenho + MotionEvent
    MainActivity.kt
  app/src/test/java/com/setas/controle/   testes JVM
```

---

### Task 1: Núcleo do PC (estado, diferença, cão de guarda)

**Files:**
- Create: `pc/SetasPc.sln`, `pc/src/SetasPc.Core/{Arrows,KeyDiff,IKeySink,ArrowController}.cs`
- Test: `pc/tests/SetasPc.Tests/ArrowControllerTests.cs`

**Interfaces:**
- Produces:
  - `[Flags] enum Arrows : byte { None=0, Up=1, Down=2, Left=4, Right=8 }`
  - `static IReadOnlyList<(Arrows key, bool down)> KeyDiff.Compute(Arrows prev, Arrows next)` — soltas antes de apertos, ordem Up, Down, Left, Right.
  - `interface IKeySink { void Press(Arrows key); void Release(Arrows key); }`
  - `class ArrowController(IKeySink sink, TimeSpan timeout)` com `void OnByte(byte b, DateTime now)`, `void OnTick(DateTime now)`, `void ReleaseAll()`, `Arrows Current { get; }`.

- [ ] **Step 1:** Na raiz, `git init`; criar a solução e projetos (`dotnet new sln`, `classlib` net8.0, `console` net8.0-windows, `xunit`), referências entre eles.
- [ ] **Step 2: Testes que falham** em `ArrowControllerTests.cs` usando um `FakeSink` que grava `"+Up"`, `"-Up"` etc.:
  - `Byte_DownRight_PressesBoth`: `OnByte(10)` → `["+Down","+Right"]`.
  - `Byte_ThenDownOnly_ReleasesRight`: `OnByte(10)`, `OnByte(2)` → último evento `"-Right"`, `Current == Down`.
  - `SameState_NoEvents`: `OnByte(1)` duas vezes → só um `"+Up"`.
  - `ByteAbove15_Ignored`: `OnByte(16)` → nenhum evento.
  - `Watchdog_300ms_ReleasesAll`: `OnByte(1, t0)`; `OnTick(t0+299ms)` → nada; `OnTick(t0+301ms)` → `"-Up"`, `Current == None`.
  - `ReleaseAll_ReleasesOnlyHeld`: `OnByte(5)`, `ReleaseAll()` → `["-Up","-Left"]`.
  - `KeyDiff_ReleasesBeforePresses`: `Compute(Up, Down)` → `[(Up,false),(Down,true)]`.
- [ ] **Step 3:** `dotnet test pc` → FAIL (tipos não existem).
- [ ] **Step 4:** Implementar os quatro arquivos com as assinaturas acima. O cão de guarda compara `now - lastByteAt > timeout`.
- [ ] **Step 5:** `dotnet test pc` → todos PASS.
- [ ] **Step 6:** Commit `feat(pc): núcleo de estado das setas com cão de guarda`.

### Task 2: Interception e teste nos jogos (portão)

**Files:**
- Create: `pc/src/SetasPc/{Interception,InterceptionKeySink}.cs`, `pc/src/SetasPc/Program.cs` (só o modo `--teste` por enquanto)

**Interfaces:**
- Consumes: `IKeySink`, `Arrows`.
- Produces: `sealed class InterceptionKeySink : IKeySink, IDisposable` com `static InterceptionKeySink? TryCreate(out string erro)`.

- [ ] **Step 1:** `Interception.cs`: P/Invoke de `interception_create_context`, `interception_destroy_context`, `interception_send`, `interception_is_keyboard`, `interception_get_hardware_id`; struct `InterceptionKeyStroke { ushort code; ushort state; uint information; }`. Constantes: `KEY_DOWN=0`, `KEY_UP=1`, `KEY_E0=2`.
- [ ] **Step 2:** `InterceptionKeySink`: escolhe o primeiro dispositivo de teclado 1..10 com hardware id não vazio. `Press` envia `state = KEY_DOWN|KEY_E0` com o scan code da seta; `Release` usa `KEY_UP|KEY_E0`. `TryCreate` retorna `null` e uma mensagem em português se o contexto for nulo ou não houver teclado: instalar o Interception com `install-interception.exe /install` como administrador, reiniciar, e colocar `interception.dll` (x64) ao lado do `.exe`.
- [ ] **Step 3:** `Program.cs --teste`: após 5 s de contagem regressiva, faz em sequência: ↑ 0,5 s; → 0,5 s; ↓+→ 0,5 s; meia-lua ↓ → ↓+→ → → (100 ms cada); solta tudo. Sempre `ReleaseAll` em `finally` e em `Console.CancelKeyPress`.
- [ ] **Step 4:** `dotnet build pc` → sem erros.
- [ ] **Step 5 (manual, com o usuário):** instalar o Interception, abrir cada jogo dele no modo treino, rodar `SetasPc.exe --teste` e confirmar que o personagem se move. **Se algum jogo ignorar ou o anti-cheat reclamar, parar e levar ao usuário a decisão sobre o plano B (placa Pico).**
- [ ] **Step 6:** Commit `feat(pc): injeção pelo Interception e modo de teste`.

### Task 3: Servidor TCP + adb reverse

**Files:**
- Create: `pc/src/SetasPc/{Adb,PhoneServer}.cs`; Modify: `pc/src/SetasPc/Program.cs`
- Test: `pc/tests/SetasPc.Tests/PhoneServerTests.cs`

**Interfaces:**
- Consumes: `ArrowController`.
- Produces: `class PhoneServer(int port, ArrowController ctl)` com `Task RunAsync(CancellationToken ct)` e `event Action<bool> ConnectionChanged`. `static class Adb` com `bool TryReverse(int port, out string erro)`.

- [ ] **Step 1: Testes que falham** (rodam com `FakeSink`, porta livre aleatória):
  - `ClientSendsByte_ControllerPresses`: cliente envia `0x02` → em até 200 ms `Current == Down`.
  - `ClientDisconnects_ReleasesAll`: envia `0x01`, fecha o socket → em até 200 ms `Current == None`.
  - `SecondClient_ReplacesFirst`: o segundo cliente que conecta assume; o primeiro é fechado e as setas soltas.
- [ ] **Step 2:** `dotnet test pc` → FAIL.
- [ ] **Step 3:** `PhoneServer`: `TcpListener` em `IPAddress.Loopback`, `NoDelay = true`, lê byte a byte, chama `OnByte`; ao fim/erro de conexão `ReleaseAll`. Um timer de 50 ms chama `OnTick`.
- [ ] **Step 4:** `Adb.TryReverse`: procura `adb.exe` ao lado do executável, depois no `PATH`; roda `adb reverse tcp:28777 tcp:28777`; erro em português se não achar `adb` ou nenhum celular.
- [ ] **Step 5:** `Program.cs` (modo normal): cria o sink (sai com a mensagem se falhar), a cada 2 s tenta `Adb.TryReverse` enquanto não houver cliente, mostra status como no desenho: "Driver Interception: Instalado", "Celular (cabo USB): Conectado/Aguardando", "Setas agora: ↑ ↓ ← →". Soltar tudo ao sair.
- [ ] **Step 6:** `dotnet test pc` → PASS.
- [ ] **Step 7:** Commit `feat(pc): servidor pelo cabo USB com adb reverse`.

### Task 4: Lógica de toque do Android

**Files:**
- Create: `android/` (projeto Gradle, pacote `com.setas.controle`), `Arrows.kt`, `BlockGeometry.kt`, `StickMapper.kt`, `TouchState.kt`
- Test: `android/app/src/test/java/com/setas/controle/{BlockGeometryTest,StickMapperTest,TouchStateTest}.kt`

**Interfaces:**
- Produces:
  - `object Arrows { const val UP=1; DOWN=2; LEFT=4; RIGHT=8 }`
  - `data class BlockGeometry(val x: Float, val y: Float, val u: Float)` com `val gap = u*0.08f`, `val width = 3*u+2*gap`, `val height = 2*u+gap`, `fun hit(px: Float, py: Float): Int`.
  - `data class StickGeometry(val x: Float, val y: Float, val size: Float)`; `object StickMapper { fun map(dx: Float, dy: Float, size: Float): Int }`.
  - `class TouchState` com `fun setPointer(id: Int, mask: Int)`, `fun removePointer(id: Int)`, `fun setStick(mask: Int)`, `fun releaseAll()`, `val mask: Int` (união), `fun takeNewArrowBits(): Int` (bits que viraram 1 só pelos dedos nas setas, desde a última chamada).

Geometria (relativa ao bloco): ↑ em `(u+g, 0)`, ← `(0, u+g)`, ↓ `(u+g, u+g)`, → `(2u+2g, u+g)`, cada uma `u×u`, com tolerância de `0.08u` de cada lado. Cantos ↖ em `(0,0)` e ↗ em `(2u+2g, 0)`, tamanho `u×u`, dão `UP|LEFT` e `UP|RIGHT`. Analógico: deslocamento máximo `0.3·size`, zona morta `0.12·size`, 8 setores de 45° com o eixo y para cima (setor 0 = →, sentido anti-horário).

- [ ] **Step 1: Testes que falham** (com `u = 100`):
  - `hit_centerOfDown_isDown`: `hit(158, 158) == DOWN`.
  - `hit_gapBetweenDownAndRight_isBoth`: `hit(212, 158) == DOWN or RIGHT`.
  - `hit_upperLeftCorner_isUpLeft`: `hit(50, 50) == UP or LEFT`.
  - `hit_outside_isZero`: `hit(-30, -30) == 0`.
  - `stick_insideDeadZone_isZero`: `map(5f, 5f, 200f) == 0`.
  - `stick_downRight_isBoth`: `map(40f, 40f, 200f) == DOWN or RIGHT` (y da tela para baixo).
  - `stick_straightUp_isUp`: `map(0f, -50f, 200f) == UP`.
  - `twoFingersSameArrow_oneLifts_stillPressed`: `setPointer(1, UP)`, `setPointer(2, UP)`, `removePointer(1)` → `mask == UP`.
  - `stickDoesNotVibrate`: `setStick(DOWN)` → `takeNewArrowBits() == 0`.
  - `slideIntoNewArrow_vibratesOnce`: `setPointer(1, DOWN)`; take; `setPointer(1, DOWN or RIGHT)` → `takeNewArrowBits() == RIGHT`; chamar de novo → `0`.
  - `releaseAll_clearsEverything`: dedos e analógico → `releaseAll()` → `mask == 0`.
- [ ] **Step 2:** `./gradlew test` em `android/` → FAIL.
- [ ] **Step 3:** Implementar os quatro arquivos.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5:** Commit `feat(android): acerto de toque, analógico e estado`.

### Task 5: Layouts, cadeado e salvamento

**Files:**
- Create: `LayoutModel.kt`, `LayoutStore.kt`
- Test: `LayoutModelTest.kt`

**Interfaces:**
- Consumes: `BlockGeometry`, `StickGeometry`.
- Produces: `data class Layout(val block: BlockGeometry, val stick: StickGeometry)`; `object LayoutModel { fun default(w: Float, h: Float): Layout; fun clamp(l: Layout, w: Float, h: Float, minU: Float, minStick: Float): Layout; fun moveBlock(l, dx, dy); fun resizeBlock(l, dx, dy); fun moveStick(l, dx, dy); fun resizeStick(l, dx, dy) }` (os quatro retornam `Layout` sem limitar; quem chama aplica `clamp`). `class LayoutStore(prefs: SharedPreferences) { fun load(landscape: Boolean): Layout?; fun save(landscape: Boolean, l: Layout); fun clear(landscape: Boolean) }`.

Valores: `m = min(w,h)`. **Horizontal** (`w > h`): `u = 0.30·m`, bloco a `0.04·m` da esquerda, centralizado na vertical; analógico `size = 0.70·m`, a `0.04·m` da direita, centralizado na vertical. **Vertical**: `u = 0.27·w`, bloco centralizado na horizontal a `0.06·h` do fundo; analógico `size = 0.60·w`, centralizado na horizontal com o centro a `0.30·h`. Redimensionar: bloco `u += (dx+dy)/6`, analógico `size += (dx+dy)/2`. `minU = 48dp`, `minStick = 120dp`; nada sai da tela.

- [ ] **Step 1: Testes que falham:**
  - `defaultLandscape_blockLeftStickRight`: `default(2000f, 1000f)` → `block.x + block.width < stick.x`.
  - `defaultPortrait_stickAboveBlock`: `default(1000f, 2000f)` → `stick.y + stick.size < block.y`.
  - `defaults_fitOnScreen` para as duas orientações.
  - `clamp_blockDraggedOffScreen_staysInside`: `moveBlock(l, -5000f, 0f)` + clamp → `block.x == 0f`.
  - `clamp_tooSmall_raisedToMin`: `resizeBlock(l, -5000f, -5000f)` + clamp → `block.u == minU`.
- [ ] **Step 2:** `./gradlew test` → FAIL.
- [ ] **Step 3:** Implementar. `LayoutStore` guarda 6 floats com prefixo `h_` ou `v_`.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5:** Commit `feat(android): layouts por orientação com ajuste e limites`.

### Task 6: Ligação com o PC

**Files:**
- Create: `PhoneLink.kt`
- Test: `PhoneLinkTest.kt` (usa `ServerSocket` local em porta livre)

**Interfaces:**
- Produces: `class PhoneLink(host: String = "127.0.0.1", port: Int = 28777, heartbeatMs: Long = 100)` com `fun start()`, `fun stop()`, `fun send(mask: Int)` (não bloqueia; guarda o último estado e acorda a thread de envio), `var onConnectionChanged: ((Boolean) -> Unit)?`.

- [ ] **Step 1: Testes que falham:**
  - `sendsStateOnChange`: `send(10)` → servidor lê `10` em até 100 ms.
  - `heartbeatRepeatsState`: `send(1)` → servidor lê ao menos 3 bytes `1` em 400 ms.
  - `reconnectsAfterServerRestart`: fechar e reabrir o servidor → `onConnectionChanged(true)` de novo em até 2 s.
  - `stop_sendsZeroFirst`: `send(4)`, `stop()` → último byte recebido é `0`.
- [ ] **Step 2:** `./gradlew test` → FAIL.
- [ ] **Step 3:** Implementar com uma thread dedicada: `Socket` com `tcpNoDelay = true`, espera o próximo envio com timeout de `heartbeatMs`, reconecta a cada 500 ms se cair.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5:** Commit `feat(android): ligação com o PC com sinal de vida`.

### Task 7: Tela do app

**Files:**
- Create: `ControllerView.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `res/layout/activity_main.xml`, `res/values/strings.xml`

**Interfaces:**
- Consumes: tudo das Tasks 4–6.
- Produces: `class ControllerView` com `var locked: Boolean`, `var hapticsOn: Boolean`, `var onMask: ((Int) -> Unit)?`, `fun resetLayout()`.

Comportamento (igual ao desenho aprovado):
- Topo: indicador "Conectado"/"Desconectado", botão de vibração, cadeado ("Travado"/"Ajustando"), e "Voltar ao padrão" só com o cadeado aberto.
- Travado: `ACTION_DOWN`/`POINTER_DOWN`/`MOVE` atualizam cada dedo; um dedo que começou no analógico fica preso ao analógico até levantar; `UP`/`POINTER_UP` removem o dedo; **`ACTION_CANCEL` chama `releaseAll`**. Depois de cada evento: `onMask(mask)` primeiro, depois `performHapticFeedback(KEYBOARD_TAP)` se `hapticsOn` e `takeNewArrowBits() != 0`.
- Aberto: contorno tracejado e alça de 24dp no canto do bloco e do analógico; arrastar move, alça redimensiona; ao abrir chama `releaseAll` e `onMask(0)`; ao fechar salva com `LayoutStore`.
- `MainActivity`: `screenOrientation="fullSensor"`, `FLAG_KEEP_SCREEN_ON`, imersivo; `onPause` → `onMask(0)` e `link.stop()`; `onResume` → `link.start()`. Ao girar (`onSizeChanged` com nova orientação) → `releaseAll`, `onMask(0)`, carrega o layout salvo daquela orientação ou o padrão.

- [ ] **Step 1:** Implementar.
- [ ] **Step 2:** `./gradlew assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3:** `./gradlew test` → todos PASS.
- [ ] **Step 4:** Commit `feat(android): tela de controle com cadeado e rotação`.

### Task 8: Verificação de ponta a ponta (com o usuário)

- [ ] **Step 1:** Instalar o app (`adb install -r app/build/outputs/apk/debug/app-debug.apk`), rodar `SetasPc.exe`, conferir "Conectado" nos dois lados.
- [ ] **Step 2:** No modo treino de um jogo: cada seta, diagonais pelos vãos e cantos, meia-lua deslizando, várias setas com dois dedos, analógico nas 8 direções, vibração só nas setas.
- [ ] **Step 3:** Review Focus na prática: arrancar o cabo segurando →; puxar a barra de notificação segurando ↓; girar o celular segurando ←; abrir o cadeado segurando ↑. Em todos o personagem precisa parar.
- [ ] **Step 4:** Cadeado: mover e redimensionar nas duas orientações, fechar e reabrir o app, conferir que ficou salvo; "Voltar ao padrão".
- [ ] **Step 5:** Commit final com um `README.md` curto de instalação (Interception, depuração USB, como abrir).
