package com.cobrinha;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Uma cobra da arena.
 *
 * <p>O corpo não é uma lista de "pedaços" independentes: é o rastro dos pontos
 * por onde a cabeça passou, cortado no comprimento atual. Os círculos que
 * aparecem na tela (e que valem para a colisão) são amostrados desse rastro a
 * cada {@code raio * Config.PASSO_SEGMENTO} unidades, então o corpo acompanha
 * exatamente a curva que a cabeça descreveu.</p>
 */
public class Cobra {

    public final boolean humana;
    public String nome;
    public Color corCorpo;
    public Color corBrilho;

    public double cabecaX;
    public double cabecaY;
    /** Direção atual da cabeça, em radianos. */
    public double angulo;
    /** Direção desejada: o mouse define a do jogador, o cérebro a dos bots. */
    public double anguloAlvo;
    public double pontos;
    public int abates;
    public boolean viva = true;
    /** Pedido de turbo neste quadro (botão do mouse, ESPAÇO/SHIFT ou IA). */
    public boolean querTurbo;
    /** Se o turbo realmente está ativo (precisa de pontos suficientes). */
    public boolean turbinando;
    /** Ligada no menu e durante os primeiros quadros de vida. */
    public boolean imune;
    public int idadeEmQuadros;
    public CerebroBot cerebro;

    /** Raio do corpo neste quadro (recalculado em cada atualização). */
    public double raioAtual;
    /** Distância da cabeça até o segmento mais distante: descarte rápido. */
    public double alcanceCorpo;
    /** Círculos do corpo, da cabeça para a cauda. */
    public final List<Vec2> segmentos = new ArrayList<>();

    /** Rastro cru da cabeça: o primeiro elemento é a posição mais recente. */
    private final ArrayDeque<Vec2> rastro = new ArrayDeque<>();
    private double comprimentoRastro;
    /** Pontos já gastos no turbo e ainda não largados como bolinha. */
    private double creditoTurbo;

    public Cobra(String nome, boolean humana, Color corCorpo, double x, double y,
                 double angulo, double pontos) {
        this.nome = nome;
        this.humana = humana;
        this.corCorpo = corCorpo;
        this.corBrilho = clarear(corCorpo, 0.45);
        this.cabecaX = x;
        this.cabecaY = y;
        this.angulo = angulo;
        this.anguloAlvo = angulo;
        this.pontos = pontos;
        this.raioAtual = Config.raioDePontos(pontos);
        this.imune = true;

        // Nasce com o corpo já esticado para trás, em linha reta.
        double comprimento = Config.comprimentoDePontos(pontos);
        double passo = Math.max(2.0, raioAtual * Config.PASSO_SEGMENTO);
        int quantos = (int) Math.ceil(comprimento / passo) + 1;
        for (int i = 0; i < quantos; i++) {
            double d = i * passo;
            rastro.addLast(new Vec2(x - Math.cos(angulo) * d, y - Math.sin(angulo) * d));
        }
        comprimentoRastro = (quantos - 1) * passo;
        amostrarSegmentos();
    }

    /** Velocidade em unidades por quadro, já com turbo e penalidade de tamanho. */
    public double velocidade() {
        double base = turbinando ? Config.VELOCIDADE_TURBO : Config.VELOCIDADE_BASE;
        return base * Config.fatorVelocidade(pontos);
    }

    public double comprimentoCorpo() {
        return Config.comprimentoDePontos(pontos);
    }

    /** Alcance do ímã que puxa a comida para a boca. */
    public double alcanceIma() {
        return raioAtual * Config.IMA_ALCANCE_FATOR_RAIO + Config.IMA_ALCANCE_EXTRA;
    }

    public boolean invulneravel() {
        return imune || idadeEmQuadros < Config.QUADROS_IMUNIDADE;
    }

    /** Posição da boca, um pouco à frente do centro da cabeça. */
    public double bocaX() {
        return cabecaX + Math.cos(angulo) * raioAtual * 0.6;
    }

    public double bocaY() {
        return cabecaY + Math.sin(angulo) * raioAtual * 0.6;
    }

    /**
     * Avança um quadro: gasta turbo, gira em direção ao alvo, anda, estende o
     * rastro, corta o excedente e reamostra os círculos do corpo.
     */
    public void atualizar(Mundo mundo) {
        if (!viva) {
            return;
        }
        idadeEmQuadros++;
        raioAtual = Config.raioDePontos(pontos);

        // --- turbo: consome pontos e larga bolinhas atrás da cobra ---
        turbinando = querTurbo && pontos > Config.TURBO_PONTOS_MINIMOS;
        if (turbinando) {
            double custo = Math.min(Config.TURBO_CUSTO_POR_QUADRO,
                    Math.max(0, pontos - Config.TURBO_PONTOS_MINIMOS));
            pontos -= custo;
            creditoTurbo += custo;
            if (creditoTurbo >= Config.TURBO_VALOR_BOLINHA) {
                creditoTurbo -= Config.TURBO_VALOR_BOLINHA;
                mundo.largarBolinhaDeTurbo(this);
            }
        }

        // --- giro limitado pelo tamanho ---
        double giroMaximo = Config.giroDeRaio(raioAtual);
        if (turbinando) {
            giroMaximo *= Config.GIRO_FATOR_TURBO;
        }
        double diferenca = Vec2.normalizarAngulo(anguloAlvo - angulo);
        angulo = Vec2.normalizarAngulo(angulo + Config.limitar(diferenca, -giroMaximo, giroMaximo));

        // --- deslocamento ---
        double v = velocidade();
        cabecaX += Math.cos(angulo) * v;
        cabecaY += Math.sin(angulo) * v;

        estenderRastro();
        amostrarSegmentos();
    }

    /** Adiciona a cabeça ao rastro e corta a cauda no comprimento atual. */
    private void estenderRastro() {
        Vec2 novaCabeca = new Vec2(cabecaX, cabecaY);
        Vec2 anterior = rastro.peekFirst();
        if (anterior != null) {
            comprimentoRastro += anterior.distancia(novaCabeca);
        }
        rastro.addFirst(novaCabeca);

        double alvo = comprimentoCorpo();
        // Remove pontos inteiros da cauda enquanto sobrar comprimento.
        while (rastro.size() > 2) {
            Vec2 ultimo = rastro.peekLast();
            Vec2 penultimo = penultimoDoRastro();
            if (penultimo == null) {
                break;
            }
            double trecho = ultimo.distancia(penultimo);
            if (comprimentoRastro - trecho < alvo) {
                break;
            }
            rastro.pollLast();
            comprimentoRastro -= trecho;
        }
        // Encolhe o último trecho para a cauda não ficar tremendo.
        double excesso = comprimentoRastro - alvo;
        if (excesso > 0 && rastro.size() >= 2) {
            Vec2 ultimo = rastro.pollLast();
            Vec2 penultimo = rastro.peekLast();
            double trecho = ultimo.distancia(penultimo);
            if (trecho > 1e-6) {
                double t = Math.min(1.0, excesso / trecho);
                rastro.addLast(ultimo.interpolado(penultimo, t));
                comprimentoRastro -= trecho * t;
            } else {
                rastro.addLast(ultimo);
            }
        }
    }

    private Vec2 penultimoDoRastro() {
        Iterator<Vec2> it = rastro.descendingIterator();
        if (!it.hasNext()) {
            return null;
        }
        it.next();
        return it.hasNext() ? it.next() : null;
    }

    /**
     * Percorre o rastro da cabeça para a cauda e marca um círculo a cada
     * {@code raio * PASSO_SEGMENTO} unidades percorridas.
     */
    private void amostrarSegmentos() {
        segmentos.clear();
        double passo = Math.max(2.0, raioAtual * Config.PASSO_SEGMENTO);
        double sobra = 0;
        double maiorDistancia2 = 0;
        Vec2 anterior = null;

        for (Vec2 ponto : rastro) {
            if (anterior == null) {
                segmentos.add(ponto);
                anterior = ponto;
                continue;
            }
            double trecho = anterior.distancia(ponto);
            if (trecho < 1e-9) {
                continue;
            }
            double andado = 0;
            while (sobra + (trecho - andado) >= passo && segmentos.size() < Config.MAX_SEGMENTOS) {
                andado += passo - sobra;
                sobra = 0;
                Vec2 amostra = anterior.interpolado(ponto, andado / trecho);
                segmentos.add(amostra);
                double dx = amostra.x() - cabecaX;
                double dy = amostra.y() - cabecaY;
                maiorDistancia2 = Math.max(maiorDistancia2, dx * dx + dy * dy);
            }
            sobra += trecho - andado;
            anterior = ponto;
            if (segmentos.size() >= Config.MAX_SEGMENTOS) {
                break;
            }
        }
        alcanceCorpo = Math.sqrt(maiorDistancia2);
    }

    /** Último ponto do rastro: é de lá que saem as bolinhas do turbo. */
    public Vec2 pontaDaCauda() {
        Vec2 ultimo = rastro.peekLast();
        return ultimo != null ? ultimo : new Vec2(cabecaX, cabecaY);
    }

    public double distanciaDoCentro() {
        return Math.sqrt(cabecaX * cabecaX + cabecaY * cabecaY);
    }

    static Color clarear(Color cor, double fator) {
        int r = (int) Math.round(cor.getRed() + (255 - cor.getRed()) * fator);
        int g = (int) Math.round(cor.getGreen() + (255 - cor.getGreen()) * fator);
        int b = (int) Math.round(cor.getBlue() + (255 - cor.getBlue()) * fator);
        return new Color(r, g, b);
    }

    static Color escurecer(Color cor, double fator) {
        int r = (int) Math.round(cor.getRed() * (1 - fator));
        int g = (int) Math.round(cor.getGreen() * (1 - fator));
        int b = (int) Math.round(cor.getBlue() * (1 - fator));
        return new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }
}
