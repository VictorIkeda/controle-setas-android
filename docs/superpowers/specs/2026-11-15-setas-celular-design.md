# Setas no celular para jogos de luta no Windows — Design

Data: 2026-11-15

> **Mudança (2026-11-15, decisão do usuário):** o programa do PC funciona **sem driver**, usando o
> `SendInput` do Windows com scan codes. Se o driver Interception estiver instalado, ele é usado
> automaticamente; `--sem-driver` força o SendInput. Risco aceito: alguns anti-cheats ignoram teclas
> do SendInput; nesse caso, as opções são instalar o Interception ou usar a placa Pico.

## Intenção e o porquê

O usuário joga jogos de luta no PC com Windows. Ele quer usar o celular Android, deitado ao lado do teclado, como **direcional**: setas na tela que se pressionam deslizando o dedo, mais um analógico. Os botões de ataque continuam no **teclado físico**. O celular envia apenas as quatro setas do teclado (↑ ↓ ← →).

Jogo de luta exige três coisas acima de tudo:
1. **Atraso mínimo.** Um combo perdido por atraso torna o app inútil.
2. **Teclas simultâneas.** Diagonais (↓+→) e qualquer combinação das quatro setas precisam funcionar.
3. **Deslizar sem levantar o dedo.** Movimentos como meia-lua (↓ → ↘ → →) são feitos arrastando o dedo pelas setas.

## Objetivos

- App Android com tela de controle: 4 setas (layout do desenho: ↑ em cima; ← ↓ → embaixo) e um analógico ao lado.
- Programa no Windows que recebe o estado do celular e pressiona/solta as setas de verdade.
- Ligação **somente por cabo USB**.
- Multitoque: vários dedos ao mesmo tempo; cada dedo pode deslizar entre as setas.
- Ao deslizar entre duas setas vizinhas, existe uma faixa (no vão entre elas) onde as duas ficam pressionadas, para facilitar diagonais.
- Analógico convertido em 8 direções → combinação de setas (ex.: inclinado para baixo-direita = ↓+→).
- Setas e analógico podem ser usados juntos; a tecla fica pressionada se qualquer um deles a pedir.
- Teclas do celular e do teclado físico funcionam juntas no Windows.

## Não-objetivos (por agora)

- Botões de ataque no celular.
- Wi-Fi ou Bluetooth.
- Controle de Xbox virtual / analógico "de verdade".
- Placa externa (ex.: Raspberry Pi Pico) como teclado USB. Fica como plano B se o anti-cheat de algum jogo bloquear o driver.
- Remapear para outras teclas, temas.
- iPhone, macOS, Linux.
- Mais de um celular ao mesmo tempo.

## Anti-objetivos (falha mesmo que "funcione")

- **Tecla presa:** se o cabo soltar, o app fechar ou a conexão cair, o PC precisa soltar todas as setas na hora. Uma seta presa no meio da luta é inaceitável.
- Atraso perceptível ou irregular entre tocar e a tecla chegar no jogo.
- Perder eventos curtos (um toque rápido precisa virar um aperto e uma soltura).
- O celular apagar a tela ou o Android pausar o app no meio da partida.

## Restrições

- Android (qualquer aparelho razoavelmente recente; minSdk a definir pelo construtor, sugestão 24+).
- Windows 10/11.
- Cabo USB com **depuração USB** ativada no celular (feito uma vez pelo usuário).
- Muitos jogos leem a tecla pelo **código físico (scan code)**, não pelo caractere. O PC deve enviar as setas com scan codes (setas são teclas "estendidas").
- **O Windows precisa ver as setas como vindas de um teclado de verdade** (pedido do usuário: "simule um segundo teclado"), para que jogos com anti-cheat não as ignorem. Por isso não se usa `SendInput`, que marca a tecla como simulada.
- O driver **Interception** precisa ser instalado uma vez, como administrador, e exige reiniciar o PC.
- Risco aceito pelo usuário: alguns anti-cheats detectam ou bloqueiam o Interception. Se acontecer, o plano B é a placa externa.

## Abordagens consideradas

| Abordagem | Prós | Contras | Decisão |
|---|---|---|---|
| **A) App + programa no PC, via cabo USB** | Funciona em qualquer Android; atraso ~1–2 ms; controle total de como a tecla é enviada | Precisa abrir o programa no PC; depuração USB ativada uma vez | **Escolhida pelo usuário** (somente cabo) |
| B) Celular como teclado Bluetooth (HID) | Nada para instalar no PC; jogo vê um teclado real | 10–20 ms de atraso variável; alguns celulares bloqueiam o recurso | Descartada pelo atraso |
| C) Celular como teclado USB (gadget HID) | Atraso mínimo, sem programa no PC | Exige root no celular | Descartada |

Para a parte do PC (como a tecla chega ao Windows como teclado de verdade):

| Abordagem | Prós | Contras | Decisão |
|---|---|---|---|
| `SendInput` | Simples, nada para instalar | O Windows marca a tecla como simulada; anti-cheat pode ignorar | Descartada |
| **Driver Interception** | Gratuito, assinado; a tecla entra no caminho dos teclados reais | Instalação com reinício; alguns anti-cheats detectam/bloqueiam | **Escolhida pelo usuário** |
| Placa Raspberry Pi Pico como teclado USB | Teclado físico de verdade; funciona com qualquer anti-cheat | Compra de hardware; gravar firmware | Plano B |
| Driver próprio | — | Exige assinatura paga da Microsoft; modo de teste faz anti-cheat recusar rodar | Descartada |

## Como funciona (decidido)

### Ligação
- O PC usa o **adb** (Android platform-tools) para fazer `adb reverse tcp:<porta> tcp:<porta>`. Assim o app no celular conecta em `127.0.0.1:<porta>` e a conexão chega ao programa do PC pelo cabo.
- O programa do PC roda o `adb reverse` sozinho ao iniciar e quando um celular é conectado. *(Chamada minha: evita o usuário digitar comandos.)*
- TCP com Nagle desligado (`TCP_NODELAY`) dos dois lados.

### O que é enviado
- O celular envia o **estado completo** das 4 setas (um byte com 4 bits) a cada mudança, e não eventos de "apertou/soltou". Assim um pacote perdido ou fora de ordem nunca deixa tecla presa: o próximo estado corrige. *(Chamada minha.)*
- O celular também envia um sinal de vida (ex.: a cada 100 ms). Se o PC ficar sem receber nada por um tempo curto (ex.: 300 ms) ou a conexão cair, ele **solta todas as setas**. *(Chamada minha; valores ajustáveis pelo construtor.)*

### No Windows
- As setas são enviadas pelo **driver Interception**, com scan codes e a flag de tecla estendida, através de um dos teclados físicos presentes (o teclado normal do usuário).
- **Primeiro passo da construção:** um teste rápido só do lado do PC (Interception apertando setas) nos jogos do usuário, antes de fazer o app do celular. Se algum jogo ignorar, decidir com o usuário se parte para o plano B.
- O PC só envia "apertou/soltou" para as teclas que mudaram em relação ao último estado.
- Ao fechar o programa, solta todas as setas.

### No celular
- Tela cheia, sempre acesa enquanto o app estiver aberto, sem barra de navegação atrapalhando.
- Cada dedo na tela é acompanhado separadamente. A cada movimento, calcula-se quais setas estão sob cada dedo; o estado final é a **união** de todos os dedos e do analógico.
- O toque precisa responder no instante em que o dedo encosta (sem esperar para distinguir toque de gesto).
- **Vibração bem leve** sempre que uma seta passa a ser apertada, inclusive ao deslizar o dedo de uma seta para outra (pedido do usuário). Usa o "clique" curto do próprio Android, não uma vibração longa. A vibração não pode atrasar o envio da tecla ao PC.
- O **analógico não vibra** (pedido do usuário).
- Botão para ligar/desligar a vibração. *(Chamada minha.)*
- Indicador de conexão (conectado / desconectado).

### Layout
- Funciona na **horizontal e na vertical** e se reorganiza quando o celular gira (pedido do usuário).
  - Layout padrão na horizontal: setas à esquerda, analógico à direita. *(Chamada minha.)*
  - Layout padrão na vertical: setas embaixo, analógico em cima, como no desenho original, com o analógico acrescentado. *(Chamada minha.)*
  - Cada orientação guarda o seu próprio layout ajustado. *(Chamada minha: posições que servem deitado raramente servem em pé.)*
  - Girar o celular no meio de um toque solta todas as setas, para nenhuma ficar presa.

### Cadeado de ajuste (pedido do usuário)
- Ícone de **cadeado** no topo da tela.
  - **Fechado** (padrão): modo de jogo. Nada se move.
  - **Aberto:** modo de ajuste. Arrastar move o elemento; puxar o canto muda o tamanho. Nenhuma seta é enviada ao PC enquanto estiver aberto.
- O que se ajusta: o **bloco das 4 setas** (move e redimensiona junto, mantendo a forma) e o **analógico** (separado). *(Chamada minha: com as setas em bloco, as zonas de diagonal continuam sempre no lugar certo. Pode mudar para "cada seta separada" se o usuário preferir.)*
- Ao fechar o cadeado, o layout é salvo no celular e continua igual da próxima vez que o app abrir.
- Botão "Voltar ao padrão" visível só com o cadeado aberto.
- Nada pode sair da tela, nem ficar pequeno demais para tocar.
- O analógico nasce onde o dedo encosta dentro da área dele (analógico "flutuante") e tem uma zona morta central para não disparar direções sem querer.
- 8 setores de 45° no analógico: ↑, ↗, →, ↘, ↓, ↙, ←, ↖.
- **Diagonais nas setas:** os vãos entre ↓ e ← e entre ↓ e → dão ↙ e ↘. Os **cantos vazios acima de ← e de →** (ao lado do ↑) são zonas de ↖ (↑+←) e ↗ (↑+→). Assim, deslizar de ← até ↑ passa por ↖, e pular para frente e para trás fica fácil só com as setas. *(Chamada minha.)*

### Teclado físico *(chamada minha)*
- O teclado físico é usado só para os ataques; as setas vêm só do celular. Por isso o PC pode soltar as setas sem se preocupar com uma seta física apertada ao mesmo tempo. Se isso mudar, o PC vai precisar acompanhar o estado do teclado físico.

### Instalação do driver *(chamada minha)*
- O programa do PC verifica se o Interception está instalado. Se não estiver, mostra como instalar (um comando como administrador, depois reiniciar).
- Como a tecla entra pelo driver, ela chega também a jogos rodando como administrador.

## Deixado para o construtor

- Linguagens e bibliotecas. Sugestão: **Kotlin** com uma View própria de toque no Android; **C# (.NET)** gerando um único `.exe` no Windows.
- Número da porta, formato exato da mensagem, tempos do sinal de vida.
- Tamanho da faixa de diagonal entre setas e da zona morta do analógico.
- Como o programa do PC encontra o `adb` (junto com o programa ou já instalado).
- Interface do programa do PC (console simples é suficiente).
