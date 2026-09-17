# Cobra Arena

Jogo de cobrinha estilo arena escrito em **Java puro com Swing**: sem Maven, sem
Gradle, sem nenhuma biblioteca externa. Basta um JDK instalado — no VS Code é só
apertar **F5**.

Você controla uma cobra neon no meio de uma arena circular disputada por 12 bots.
Coma as bolinhas de energia para crescer, force os outros a bater em você e tente
chegar ao topo do placar sem encostar a cabeça em ninguém.

## Como jogar

- A **cabeça da sua cobra fica sempre no centro da tela**: ela persegue o ponteiro
  do mouse, então o ângulo para onde você quer ir sai direto da posição do cursor.
- **Comer** bolinhas aumenta seus pontos; mais pontos significam corpo mais longo
  e mais grosso, giro um pouco mais lento e câmera mais aberta.
- O **ímã** puxa para a boca toda comida que passar perto da cabeça — dá para
  "raspar" o chão passando rente às bolinhas.
- O **turbo** acelera de 3,0 para 5,6 unidades por quadro, mas consome pontos a
  cada quadro e vai largando bolinhas atrás de você. Ele só funciona acima de um
  mínimo de pontos, então não dá para torrar tudo.
- **Você morre** quando a *sua* cabeça encosta no corpo de outra cobra, ou quando
  bate na borda circular da arena. Não existe colisão com o próprio corpo: pode se
  enrolar à vontade.
- Quem morre **vira comida** espalhada por onde o corpo estava. Fechar o caminho
  de uma cobra grande é o jeito mais rápido de engordar.
- Os bots renascem com outro nome alguns instantes depois de morrer, então a
  arena nunca esvazia.

## Controles

| Ação | Tecla / botão |
| --- | --- |
| Virar | Mover o mouse |
| Turbo | Botão esquerdo do mouse, **ESPAÇO** ou **SHIFT** |
| Pausar | **P** ou **ESC** |
| Reiniciar | **R** ou **ENTER** |
| Começar a partida | **ENTER** ou clique |

Na tela aparecem: pontos, comprimento, abates e posição no ranking (canto superior
esquerdo), o placar dos 8 primeiros (canto superior direito), a barra de turbo e o
contador de FPS (parte de baixo) e um minimapa circular no canto inferior direito.

## Como rodar

### Pelo VS Code (F5)

1. Instale o **Extension Pack for Java** (`vscjava.vscode-java-pack`) — o VS Code
   já sugere a extensão ao abrir a pasta, porque ela está em `.vscode/extensions.json`.
2. Abra a pasta do projeto.
3. Aperte **F5**. A configuração `Cobra Arena` de `.vscode/launch.json` já aponta
   para a classe `com.cobrinha.Main`, com as fontes em `src` e as classes
   compiladas indo para `bin` (definido em `.vscode/settings.json`).

### Pelo terminal

```bash
bash run.sh       # Linux e macOS
```

```bat
run.bat           :: Windows
```

(No Linux e no macOS também dá para marcar o script como executável uma vez com
`chmod +x run.sh` e depois chamar `./run.sh`.)

Ou na mão, se preferir:

```bash
javac -encoding UTF-8 -d bin $(find src -name "*.java")
java -cp bin com.cobrinha.Main
```

Requisito: **JDK 17 ou mais novo** (o código usa `record` e `switch` de expressão).
Confira com `javac -version`.

## Estrutura do código

Tudo vive no pacote `com.cobrinha`, dentro de `src/`:

| Arquivo | Responsabilidade |
| --- | --- |
| `Main.java` | Cria o `JFrame` e entrega o painel para o loop do jogo. |
| `PainelJogo.java` | Loop de 60 FPS com `javax.swing.Timer`, leitura de mouse e teclado, desenho com `Graphics2D` (com antialias), HUD, placar, minimapa e as telas de menu, pausa e fim de jogo. |
| `Mundo.java` | A arena: lista de cobras e de comidas, ímã, colisões, borda que mata, renascimento dos bots, ranking e a grade espacial que acelera as buscas por comida. |
| `Cobra.java` | O corpo é o **rastro dos pontos por onde a cabeça passou**, cortado no comprimento atual. Os círculos desenhados (e usados na colisão) são amostrados desse rastro a cada `raio * 0,55` unidades. |
| `CerebroBot.java` | IA dos bots: soma três vetores de desejo — fugir do corpo das outras cobras, voltar ao centro perto da borda e caçar a comida com a melhor relação valor/distância — e ainda solta um turbo ocasional para escapar de enrascada. |
| `Comida.java` | Bolinha de energia, com valor, cor e pulsação visual. |
| `Config.java` | **Todos** os números de balanceamento do jogo. |
| `Vec2.java` | Vetor 2D imutável, escrito como `record`. |

### Como o corpo funciona

A cada quadro a cabeça anda e sua nova posição entra na frente de um rastro. O
rastro é então cortado no comprimento atual do corpo (que cresce com os pontos),
e os círculos do corpo são amostrados ao longo desse rastro a cada
`raio * Config.PASSO_SEGMENTO` unidades. É por isso que o corpo acompanha
exatamente a curva que a cabeça descreveu, e por isso que uma cobra mais gorda tem
círculos mais espaçados sem abrir buracos.

## Como ajustar o balanceamento

Todos os números ficam em `src/com/cobrinha/Config.java`, agrupados por assunto.
Os mais divertidos de mexer:

| Constante | O que faz |
| --- | --- |
| `RAIO_ARENA` | Tamanho da arena (padrão 2600). |
| `QTD_COMIDA` | Bolinhas que a arena tenta manter no chão (padrão 850). |
| `VELOCIDADE_BASE` / `VELOCIDADE_TURBO` | Velocidade normal e no turbo (3,0 e 5,6 por quadro). |
| `TURBO_CUSTO_POR_QUADRO` / `TURBO_PONTOS_MINIMOS` | Quanto o turbo custa e a partir de quantos pontos ele liga. |
| `GIRO_MAXIMO` / `GIRO_MINIMO` | Quão rápido uma cobra pequena e uma cobra enorme conseguem virar. |
| `RAIO_*` e `COMPRIMENTO_*` | As curvas de crescimento: grossura e comprimento em função dos pontos. |
| `QTD_BOTS` / `BOT_QUADROS_RENASCIMENTO` | Quantos bots disputam a arena e quanto demoram para renascer. |
| `BOT_PESO_FUGA` / `BOT_PESO_CENTRO` / `BOT_PESO_CACA` | Os três pesos da IA. Aumente a fuga para bots medrosos, a caça para bots gulosos. |
| `MORTE_FRACAO_COMIDA` | Quanto dos pontos de quem morre volta para a arena como comida. |
| `ZOOM_INICIAL` / `ZOOM_MINIMO` / `ZOOM_EXPOENTE` | Quanto a câmera abre conforme você cresce. |
| `PALETA_COBRAS` / `PALETA_COMIDA` | As cores neon. |

As funções `raioDePontos`, `comprimentoDePontos`, `fatorVelocidade` e `giroDeRaio`,
no fim do `Config.java`, concentram as fórmulas de progressão — mexer nelas muda a
curva inteira do jogo sem tocar em mais nenhum arquivo.
