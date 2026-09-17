package com.cobrinha;

import java.awt.Color;

/**
 * TODOS os números de balanceamento do jogo moram aqui: tamanho da arena,
 * velocidades, custo do turbo, curvas de crescimento, cores e pesos da IA.
 * Mexer em qualquer valor deste arquivo muda a sensação do jogo sem precisar
 * tocar em nenhuma outra classe.
 */
public final class Config {

    private Config() {
    }

    // ------------------------------------------------------------------
    // Janela e loop principal
    // ------------------------------------------------------------------
    public static final String TITULO = "Cobra Arena";
    public static final int LARGURA_JANELA = 1280;
    public static final int ALTURA_JANELA = 760;
    public static final int FPS = 60;
    public static final int MS_POR_QUADRO = Math.max(1, 1000 / FPS);

    // ------------------------------------------------------------------
    // Arena
    // ------------------------------------------------------------------
    /** Raio da arena circular, em unidades de mundo (pixels no zoom 1). */
    public static final double RAIO_ARENA = 2600;
    /** Espaçamento da grade sutil desenhada no fundo. */
    public static final double ESPACO_GRADE = 110;
    /** Quantos quadros a cobra nasce invulnerável (evita morte instantânea). */
    public static final int QUADROS_IMUNIDADE = 45;

    // ------------------------------------------------------------------
    // Comida
    // ------------------------------------------------------------------
    /** Quantidade de bolinhas que a arena tenta manter no chão. */
    public static final int QTD_COMIDA = 850;
    /** Teto absoluto de bolinhas (mortes de cobras grandes geram muitas). */
    public static final int MAX_COMIDA = 3600;
    /** Quantas bolinhas podem ser repostas por quadro. */
    public static final int REPOSICAO_COMIDA_POR_QUADRO = 5;
    public static final double COMIDA_VALOR_MIN = 1.0;
    public static final double COMIDA_VALOR_MAX = 5.0;
    public static final double COMIDA_RAIO_MIN = 4.5;
    public static final double COMIDA_RAIO_MAX = 9.5;
    /** Amplitude e velocidade da pulsação visual das bolinhas. */
    public static final double COMIDA_AMPLITUDE_PULSO = 0.22;
    public static final double COMIDA_PULSO_MIN = 2.2;
    public static final double COMIDA_PULSO_MAX = 4.6;

    // ------------------------------------------------------------------
    // Ímã (comida atraída para a boca)
    // ------------------------------------------------------------------
    public static final double IMA_ALCANCE_EXTRA = 42;
    public static final double IMA_ALCANCE_FATOR_RAIO = 2.3;
    public static final double IMA_VELOCIDADE = 4.6;

    // ------------------------------------------------------------------
    // Cobras: crescimento, tamanho e movimento
    // ------------------------------------------------------------------
    public static final double PONTOS_INICIAIS = 12;
    /** Raio do corpo = RAIO_BASE + RAIO_GANHO * pontos^RAIO_EXPOENTE (com teto). */
    public static final double RAIO_BASE = 7.0;
    public static final double RAIO_GANHO = 1.15;
    public static final double RAIO_EXPOENTE = 0.42;
    public static final double RAIO_MAXIMO = 44.0;
    /** Comprimento do corpo = COMP_BASE + COMP_GANHO * pontos^COMP_EXPOENTE. */
    public static final double COMPRIMENTO_BASE = 95;
    public static final double COMPRIMENTO_GANHO = 18.0;
    public static final double COMPRIMENTO_EXPOENTE = 0.70;
    /** Distância entre os círculos amostrados do rastro, em raios. */
    public static final double PASSO_SEGMENTO = 0.55;
    /** Teto de círculos por cobra, só como trava de segurança. */
    public static final int MAX_SEGMENTOS = 3000;

    public static final double VELOCIDADE_BASE = 3.0;
    public static final double VELOCIDADE_TURBO = 5.6;
    /** Cobras grandes ficam ligeiramente mais lentas (fração perdida no topo). */
    public static final double PENALIDADE_VELOCIDADE = 0.18;
    public static final double PONTOS_VELOCIDADE_CHEIA = 6000;

    /** Giro máximo por quadro (radianos) de uma cobra pequena. */
    public static final double GIRO_MAXIMO = 0.105;
    /** Giro mínimo por quadro: nem a maior cobra vira menos que isso. */
    public static final double GIRO_MINIMO = 0.034;
    /** Quanto o turbo aperta o giro (1 = igual, <1 = vira menos no turbo). */
    public static final double GIRO_FATOR_TURBO = 0.88;

    // ------------------------------------------------------------------
    // Turbo
    // ------------------------------------------------------------------
    /** Só dá turbo acima deste total de pontos. */
    public static final double TURBO_PONTOS_MINIMOS = 26;
    /** Pontos consumidos por quadro de turbo. */
    public static final double TURBO_CUSTO_POR_QUADRO = 0.24;
    /** Cada bolinha largada atrás da cobra carrega este tanto de pontos. */
    public static final double TURBO_VALOR_BOLINHA = 2.4;
    /** Pontos de turbo que enchem a barra da interface. */
    public static final double TURBO_PONTOS_BARRA_CHEIA = 130;

    // ------------------------------------------------------------------
    // Morte
    // ------------------------------------------------------------------
    /** Fração dos pontos que volta para a arena como comida. */
    public static final double MORTE_FRACAO_COMIDA = 0.62;
    public static final double MORTE_VALOR_MAX_BOLINHA = 16.0;
    public static final int MORTE_MIN_BOLINHAS = 8;
    public static final int MORTE_MAX_BOLINHAS = 150;
    public static final double MORTE_ESPALHAMENTO = 14.0;

    // ------------------------------------------------------------------
    // Bots
    // ------------------------------------------------------------------
    public static final int QTD_BOTS = 12;
    /** Atraso (quadros) entre a morte de um bot e o nascimento do substituto. */
    public static final int BOT_QUADROS_RENASCIMENTO = 75;
    /** Quantos nomes de bots mortos ficam de quarentena antes de voltar. */
    public static final int NOMES_EM_QUARENTENA = 6;
    public static final double BOT_PONTOS_MIN_INICIAL = 12;
    public static final double BOT_PONTOS_MAX_INICIAL = 260;

    /** Pesos dos três vetores de desejo da IA. */
    public static final double BOT_PESO_FUGA = 3.1;
    public static final double BOT_PESO_CENTRO = 2.4;
    public static final double BOT_PESO_CACA = 1.0;

    /** Alcance com que o bot enxerga corpos alheios e comida. */
    public static final double BOT_VISAO_CORPO = 310;
    public static final double BOT_VISAO_COMIDA = 760;
    /** Passo de amostragem dos segmentos alheios (economia de CPU). */
    public static final int BOT_PASSO_AMOSTRA = 3;
    /** Distância da borda em que o bot começa a voltar para o centro. */
    public static final double BOT_MARGEM_BORDA = 430;
    /** Constante que suaviza a relação valor/distância da comida. */
    public static final double BOT_PESO_DISTANCIA_COMIDA = 90;
    /** Ruído que dá personalidade ao rumo do bot. */
    public static final double BOT_RUIDO = 0.030;
    public static final double BOT_AMORTECIMENTO_RUIDO = 0.93;
    /** Quadros entre duas escolhas de alvo de comida. */
    public static final int BOT_QUADROS_ENTRE_DECISOES = 9;
    /** Ameaça acima da qual o bot considera que está em uma enrascada. */
    public static final double BOT_LIMIAR_AMEACA = 0.85;
    public static final double BOT_CHANCE_TURBO = 0.35;
    public static final int BOT_QUADROS_TURBO_MIN = 20;
    public static final int BOT_QUADROS_TURBO_MAX = 55;

    // ------------------------------------------------------------------
    // Câmera
    // ------------------------------------------------------------------
    public static final double ZOOM_INICIAL = 1.05;
    public static final double ZOOM_MINIMO = 0.45;
    /** Quanto o zoom abre conforme a cobra engorda. */
    public static final double ZOOM_EXPOENTE = 0.42;
    /** Suavização do zoom por quadro (0..1). */
    public static final double ZOOM_SUAVIDADE = 0.05;

    // ------------------------------------------------------------------
    // Interface
    // ------------------------------------------------------------------
    public static final int PLACAR_LINHAS = 8;
    public static final int MINIMAPA_RAIO = 92;
    public static final int MINIMAPA_MARGEM = 26;

    // ------------------------------------------------------------------
    // Paleta neon
    // ------------------------------------------------------------------
    public static final Color COR_FUNDO = new Color(9, 11, 20);
    public static final Color COR_GRADE = new Color(26, 32, 54);
    public static final Color COR_BORDA_ARENA = new Color(120, 86, 255);
    public static final Color COR_TEXTO = new Color(226, 232, 248);
    public static final Color COR_TEXTO_FRACO = new Color(138, 150, 182);
    public static final Color COR_PAINEL = new Color(14, 17, 30, 190);
    public static final Color COR_JOGADOR = new Color(80, 240, 170);
    public static final Color COR_TURBO = new Color(255, 196, 74);

    /** Cores dos bots (o jogador tem a sua própria). */
    public static final Color[] PALETA_COBRAS = {
        new Color(255, 96, 140), new Color(96, 176, 255), new Color(255, 170, 70),
        new Color(180, 120, 255), new Color(90, 230, 240), new Color(240, 240, 120),
        new Color(120, 255, 130), new Color(255, 120, 220), new Color(120, 150, 255),
        new Color(255, 140, 90), new Color(150, 255, 210), new Color(210, 130, 255)
    };

    /** Cores das bolinhas de energia. */
    public static final Color[] PALETA_COMIDA = {
        new Color(255, 238, 140), new Color(150, 255, 200), new Color(160, 200, 255),
        new Color(255, 170, 200), new Color(200, 170, 255), new Color(255, 205, 150)
    };

    /** Nomes sorteados para os bots. */
    public static final String[] NOMES_BOTS = {
        "Jararaca", "Cascavel", "Sucuri", "Urutu", "Caninana", "Muçurana",
        "Boipeva", "Surucucu", "Coral", "Salamanta", "Dormideira", "Papa-Vento",
        "Naja", "Mamba", "Taipan", "Víbora", "Pitão", "Anaconda",
        "Cobra-Cega", "Cipó", "Cobra-Verde", "Bicudinha", "Fumaça", "Relâmpago",
        "Tempestade", "Neon", "Espiral", "Meteoro", "Cometa", "Nebulosa"
    };

    // ------------------------------------------------------------------
    // Curvas de crescimento (funções puras de balanceamento)
    // ------------------------------------------------------------------

    /** Raio do corpo para um total de pontos. */
    public static double raioDePontos(double pontos) {
        double p = Math.max(0, pontos);
        return Math.min(RAIO_MAXIMO, RAIO_BASE + RAIO_GANHO * Math.pow(p, RAIO_EXPOENTE));
    }

    /** Comprimento do corpo (em unidades de mundo) para um total de pontos. */
    public static double comprimentoDePontos(double pontos) {
        double p = Math.max(0, pontos);
        return COMPRIMENTO_BASE + COMPRIMENTO_GANHO * Math.pow(p, COMPRIMENTO_EXPOENTE);
    }

    /** Fator de velocidade: cobras grandes ficam ligeiramente mais lentas. */
    public static double fatorVelocidade(double pontos) {
        double t = Math.min(1, Math.max(0, pontos) / PONTOS_VELOCIDADE_CHEIA);
        return 1.0 - PENALIDADE_VELOCIDADE * t;
    }

    /** Giro máximo por quadro: cobras maiores viram mais devagar. */
    public static double giroDeRaio(double raio) {
        double giro = GIRO_MAXIMO * Math.sqrt(RAIO_BASE / Math.max(RAIO_BASE, raio));
        return Math.max(GIRO_MINIMO, giro);
    }

    public static double limitar(double valor, double minimo, double maximo) {
        return valor < minimo ? minimo : (valor > maximo ? maximo : valor);
    }
}
