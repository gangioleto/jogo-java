package com.cobrinha;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * A arena: guarda as cobras e as bolinhas, resolve o ímã, a comida comida, as
 * colisões, a borda que mata, o renascimento dos bots e o ranking.
 *
 * <p>Não depende de nada de interface gráfica, então pode rodar sozinho em uma
 * simulação sem janela.</p>
 */
public class Mundo {

    /** Lado da célula da grade que acelera as buscas por comida. */
    private static final double TAMANHO_CELULA = 160;

    public final List<Cobra> cobras = new ArrayList<>();
    public final List<Comida> comidas = new ArrayList<>();
    /** Cobras vivas ordenadas por pontos, da maior para a menor. */
    public final List<Cobra> ranking = new ArrayList<>();

    public Cobra jogador;
    public long quadro;
    public int melhorPosicaoJogador = Integer.MAX_VALUE;

    private final Random rnd;
    private final List<Cobra> mortosDoQuadro = new ArrayList<>();
    private final List<Comida> comidasPerto = new ArrayList<>();
    private final ArrayDeque<Long> filaRenascimento = new ArrayDeque<>();
    /** Nomes aposentados há pouco: quem renasce sempre estreia com outro. */
    private final ArrayDeque<String> nomesRecentes = new ArrayDeque<>();
    private final Comparator<Cobra> porPontos = (a, b) -> Double.compare(b.pontos, a.pontos);

    // Grade espacial: lista encadeada de índices de comida por célula.
    private final int colunas;
    private final int[] inicioCelula;
    private int[] proximaComida;

    public Mundo(Random rnd) {
        this.rnd = rnd;
        this.colunas = (int) Math.ceil(2 * Config.RAIO_ARENA / TAMANHO_CELULA) + 1;
        this.inicioCelula = new int[colunas * colunas];
        this.proximaComida = new int[Math.max(64, Config.QTD_COMIDA * 2)];

        for (int i = 0; i < Config.QTD_COMIDA; i++) {
            comidas.add(sortearComida());
        }
        jogador = new Cobra("Você", true, Config.COR_JOGADOR, 0, 0,
                rnd.nextDouble() * Math.PI * 2, Config.PONTOS_INICIAIS);
        cobras.add(jogador);
        for (int i = 0; i < Config.QTD_BOTS; i++) {
            cobras.add(criarBot());
        }
        atualizarRanking();
    }

    // ------------------------------------------------------------------
    // Ciclo de vida de um quadro
    // ------------------------------------------------------------------

    public void atualizar() {
        quadro++;
        reconstruirGrade();

        for (int i = 0; i < cobras.size(); i++) {
            Cobra c = cobras.get(i);
            if (c.viva && c.cerebro != null) {
                c.cerebro.pensar(this, c);
            }
        }
        for (int i = 0; i < cobras.size(); i++) {
            cobras.get(i).atualizar(this);
        }
        for (int i = 0; i < cobras.size(); i++) {
            Cobra c = cobras.get(i);
            if (c.viva) {
                atrairEComer(c);
            }
        }

        conferirBordas();
        conferirColisoes();
        processarMortes();
        renascerBots();
        reporComida();
        atualizarRanking();
    }

    /** Ímã: puxa a comida próxima para a boca e engole o que encostar. */
    private void atrairEComer(Cobra cobra) {
        double alcance = cobra.alcanceIma();
        double bocaX = cobra.bocaX();
        double bocaY = cobra.bocaY();
        coletarComidasPerto(bocaX, bocaY, alcance, comidasPerto);
        for (int i = 0; i < comidasPerto.size(); i++) {
            Comida c = comidasPerto.get(i);
            double distancia = Math.hypot(c.x - bocaX, c.y - bocaY);
            // Quanto mais perto da boca, mais rápido o ímã puxa.
            double forca = Config.IMA_VELOCIDADE * (1.15 - distancia / alcance * 0.6);
            c.atrairPara(bocaX, bocaY, Math.max(0.6, forca));
            if (Math.hypot(c.x - bocaX, c.y - bocaY) <= cobra.raioAtual + c.raio * 0.5) {
                c.viva = false;
                cobra.pontos += c.valor;
            }
        }
    }

    /** A borda circular da arena mata quem encostar nela. */
    private void conferirBordas() {
        for (int i = 0; i < cobras.size(); i++) {
            Cobra c = cobras.get(i);
            if (!c.viva) {
                continue;
            }
            if (c.distanciaDoCentro() + c.raioAtual * 0.35 >= Config.RAIO_ARENA) {
                marcarMorte(c);
            }
        }
    }

    /**
     * A SUA cabeça encostando no corpo de outra cobra mata VOCÊ. Não existe
     * colisão com o próprio corpo, então dá para se enrolar à vontade.
     */
    private void conferirColisoes() {
        for (int i = 0; i < cobras.size(); i++) {
            Cobra c = cobras.get(i);
            if (!c.viva || c.invulneravel() || mortosDoQuadro.contains(c)) {
                continue;
            }
            for (int j = 0; j < cobras.size(); j++) {
                Cobra outra = cobras.get(j);
                if (outra == c || !outra.viva) {
                    continue;
                }
                double limite = c.raioAtual * 0.75 + outra.raioAtual;
                double distCabecas = Math.hypot(outra.cabecaX - c.cabecaX, outra.cabecaY - c.cabecaY);
                if (distCabecas > outra.alcanceCorpo + limite) {
                    continue; // descarte rápido pela esfera envolvente
                }
                double limite2 = limite * limite;
                List<Vec2> segmentos = outra.segmentos;
                boolean bateu = false;
                for (int k = 0; k < segmentos.size(); k++) {
                    Vec2 s = segmentos.get(k);
                    double dx = s.x() - c.cabecaX;
                    double dy = s.y() - c.cabecaY;
                    if (dx * dx + dy * dy <= limite2) {
                        bateu = true;
                        break;
                    }
                }
                if (bateu) {
                    outra.abates++;
                    marcarMorte(c);
                    break;
                }
            }
        }
    }

    private void marcarMorte(Cobra c) {
        if (!mortosDoQuadro.contains(c)) {
            mortosDoQuadro.add(c);
        }
    }

    private void processarMortes() {
        for (int i = 0; i < mortosDoQuadro.size(); i++) {
            Cobra morta = mortosDoQuadro.get(i);
            morta.viva = false;
            espalharComidaDoCorpo(morta);
            cobras.remove(morta);
            if (!morta.humana) {
                filaRenascimento.addLast(quadro + Config.BOT_QUADROS_RENASCIMENTO);
                nomesRecentes.addLast(morta.nome);
                while (nomesRecentes.size() > Config.NOMES_EM_QUARENTENA) {
                    nomesRecentes.pollFirst();
                }
            }
        }
        mortosDoQuadro.clear();
    }

    private void renascerBots() {
        while (!filaRenascimento.isEmpty() && filaRenascimento.peekFirst() <= quadro) {
            filaRenascimento.pollFirst();
            cobras.add(criarBot());
        }
    }

    // ------------------------------------------------------------------
    // Comida
    // ------------------------------------------------------------------

    private void reporComida() {
        for (int i = comidas.size() - 1; i >= 0; i--) {
            if (!comidas.get(i).viva) {
                comidas.remove(i);
            }
        }
        int repostas = 0;
        while (comidas.size() < Config.QTD_COMIDA && repostas < Config.REPOSICAO_COMIDA_POR_QUADRO) {
            comidas.add(sortearComida());
            repostas++;
        }
    }

    private Comida sortearComida() {
        double angulo = rnd.nextDouble() * Math.PI * 2;
        double distancia = Config.RAIO_ARENA * 0.985 * Math.sqrt(rnd.nextDouble());
        double valor = Config.COMIDA_VALOR_MIN
                + rnd.nextDouble() * (Config.COMIDA_VALOR_MAX - Config.COMIDA_VALOR_MIN);
        Color cor = Config.PALETA_COMIDA[rnd.nextInt(Config.PALETA_COMIDA.length)];
        return criarComida(Math.cos(angulo) * distancia, Math.sin(angulo) * distancia, valor, cor);
    }

    private Comida criarComida(double x, double y, double valor, Color cor) {
        double t = Config.limitar(valor / Config.COMIDA_VALOR_MAX, 0, 1);
        double raio = Config.COMIDA_RAIO_MIN + (Config.COMIDA_RAIO_MAX - Config.COMIDA_RAIO_MIN) * t;
        double pulso = Config.COMIDA_PULSO_MIN
                + rnd.nextDouble() * (Config.COMIDA_PULSO_MAX - Config.COMIDA_PULSO_MIN);
        return new Comida(x, y, valor, raio, cor, rnd.nextDouble() * Math.PI * 2, pulso);
    }

    /** Bolinha largada atrás de quem está no turbo. */
    public void largarBolinhaDeTurbo(Cobra cobra) {
        if (comidas.size() >= Config.MAX_COMIDA) {
            return;
        }
        Vec2 cauda = cobra.pontaDaCauda();
        double x = cauda.x() + (rnd.nextDouble() - 0.5) * cobra.raioAtual;
        double y = cauda.y() + (rnd.nextDouble() - 0.5) * cobra.raioAtual;
        comidas.add(criarComida(dentroDaArena(x), dentroDaArena(y),
                Config.TURBO_VALOR_BOLINHA, Cobra.clarear(cobra.corCorpo, 0.25)));
    }

    /** Quem morre vira comida espalhada ao longo de onde o corpo estava. */
    private void espalharComidaDoCorpo(Cobra cobra) {
        double total = cobra.pontos * Config.MORTE_FRACAO_COMIDA;
        if (total <= 0 || cobra.segmentos.isEmpty()) {
            return;
        }
        int quantidade = (int) Math.ceil(total / Config.MORTE_VALOR_MAX_BOLINHA);
        quantidade = (int) Config.limitar(quantidade, Config.MORTE_MIN_BOLINHAS, Config.MORTE_MAX_BOLINHAS);
        quantidade = Math.min(quantidade, Config.MAX_COMIDA - comidas.size());
        if (quantidade <= 0) {
            return;
        }
        double valorCada = total / quantidade;
        Color cor = Cobra.clarear(cobra.corCorpo, 0.3);
        int ultimo = cobra.segmentos.size() - 1;
        for (int i = 0; i < quantidade; i++) {
            int indice = quantidade == 1 ? 0 : (int) Math.round((double) i * ultimo / (quantidade - 1));
            Vec2 s = cobra.segmentos.get(Math.min(ultimo, Math.max(0, indice)));
            double x = s.x() + (rnd.nextDouble() - 0.5) * Config.MORTE_ESPALHAMENTO;
            double y = s.y() + (rnd.nextDouble() - 0.5) * Config.MORTE_ESPALHAMENTO;
            comidas.add(criarComida(dentroDaArena(x), dentroDaArena(y), valorCada, cor));
        }
    }

    private double dentroDaArena(double coordenada) {
        return Config.limitar(coordenada, -Config.RAIO_ARENA * 0.99, Config.RAIO_ARENA * 0.99);
    }

    // ------------------------------------------------------------------
    // Grade espacial
    // ------------------------------------------------------------------

    private void reconstruirGrade() {
        Arrays.fill(inicioCelula, -1);
        if (proximaComida.length < comidas.size()) {
            proximaComida = new int[comidas.size() * 2];
        }
        for (int i = 0; i < comidas.size(); i++) {
            Comida c = comidas.get(i);
            int celula = indiceCelula(c.x, c.y);
            proximaComida[i] = inicioCelula[celula];
            inicioCelula[celula] = i;
        }
    }

    private int coluna(double coordenada) {
        int c = (int) ((coordenada + Config.RAIO_ARENA) / TAMANHO_CELULA);
        return c < 0 ? 0 : (c >= colunas ? colunas - 1 : c);
    }

    private int indiceCelula(double x, double y) {
        return coluna(y) * colunas + coluna(x);
    }

    /** Preenche {@code destino} com as bolinhas vivas dentro do raio pedido. */
    public void coletarComidasPerto(double x, double y, double raio, List<Comida> destino) {
        destino.clear();
        int x0 = coluna(x - raio);
        int x1 = coluna(x + raio);
        int y0 = coluna(y - raio);
        int y1 = coluna(y + raio);
        double raio2 = raio * raio;
        for (int cy = y0; cy <= y1; cy++) {
            int base = cy * colunas;
            for (int cx = x0; cx <= x1; cx++) {
                for (int i = inicioCelula[base + cx]; i != -1; i = proximaComida[i]) {
                    Comida c = comidas.get(i);
                    if (!c.viva) {
                        continue;
                    }
                    double dx = c.x - x;
                    double dy = c.y - y;
                    if (dx * dx + dy * dy <= raio2) {
                        destino.add(c);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Bots e ranking
    // ------------------------------------------------------------------

    private Cobra criarBot() {
        Vec2 lugar = sortearLugarSeguro();
        double angulo = rnd.nextDouble() * Math.PI * 2;
        double pontos = Config.BOT_PONTOS_MIN_INICIAL
                + rnd.nextDouble() * (Config.BOT_PONTOS_MAX_INICIAL - Config.BOT_PONTOS_MIN_INICIAL);
        Color cor = Config.PALETA_COBRAS[rnd.nextInt(Config.PALETA_COBRAS.length)];
        Cobra bot = new Cobra(sortearNome(), false, cor, lugar.x(), lugar.y(), angulo, pontos);
        bot.cerebro = new CerebroBot(rnd);
        return bot;
    }

    /** Sorteia um nome que ninguém vivo está usando no momento. */
    private String sortearNome() {
        for (int tentativa = 0; tentativa < 30; tentativa++) {
            String nome = Config.NOMES_BOTS[rnd.nextInt(Config.NOMES_BOTS.length)];
            boolean usado = nomesRecentes.contains(nome);
            for (int i = 0; i < cobras.size() && !usado; i++) {
                if (cobras.get(i).nome.equals(nome)) {
                    usado = true;
                }
            }
            if (!usado) {
                return nome;
            }
        }
        return Config.NOMES_BOTS[rnd.nextInt(Config.NOMES_BOTS.length)] + " II";
    }

    /** Procura um ponto longe dos corpos existentes para nascer. */
    private Vec2 sortearLugarSeguro() {
        Vec2 melhor = Vec2.ZERO;
        double melhorFolga = -1;
        for (int tentativa = 0; tentativa < 40; tentativa++) {
            double angulo = rnd.nextDouble() * Math.PI * 2;
            double distancia = Config.RAIO_ARENA * 0.82 * Math.sqrt(rnd.nextDouble());
            double x = Math.cos(angulo) * distancia;
            double y = Math.sin(angulo) * distancia;
            double folga = Double.MAX_VALUE;
            for (int i = 0; i < cobras.size(); i++) {
                Cobra c = cobras.get(i);
                if (!c.viva) {
                    continue;
                }
                List<Vec2> segmentos = c.segmentos;
                for (int k = 0; k < segmentos.size(); k += 4) {
                    Vec2 s = segmentos.get(k);
                    folga = Math.min(folga, Math.hypot(s.x() - x, s.y() - y));
                }
            }
            if (folga > 260) {
                return new Vec2(x, y);
            }
            if (folga > melhorFolga) {
                melhorFolga = folga;
                melhor = new Vec2(x, y);
            }
        }
        return melhor;
    }

    private void atualizarRanking() {
        ranking.clear();
        for (int i = 0; i < cobras.size(); i++) {
            Cobra c = cobras.get(i);
            if (c.viva) {
                ranking.add(c);
            }
        }
        ranking.sort(porPontos);
        if (jogador != null && jogador.viva) {
            int posicao = posicaoDe(jogador);
            if (posicao > 0 && posicao < melhorPosicaoJogador) {
                melhorPosicaoJogador = posicao;
            }
        }
    }

    /** Posição (1 = líder) de uma cobra no ranking; 0 se ela não está viva. */
    public int posicaoDe(Cobra cobra) {
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i) == cobra) {
                return i + 1;
            }
        }
        return 0;
    }

    public int totalDeCobras() {
        return cobras.size();
    }
}
