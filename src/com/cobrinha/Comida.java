package com.cobrinha;

import java.awt.Color;

/**
 * Bolinha de energia espalhada pela arena. Vale pontos, pulsa na tela e pode
 * ser puxada pelo ímã da cabeça de uma cobra.
 */
public class Comida {

    /** Posição mutável: o ímã empurra a bolinha para a boca da cobra. */
    public double x;
    public double y;
    public final double valor;
    public final double raio;
    public final Color cor;
    /** Marcada como falsa quando é comida; o mundo limpa depois. */
    public boolean viva = true;

    private final double fase;
    private final double velocidadePulso;

    public Comida(double x, double y, double valor, double raio, Color cor,
                  double fase, double velocidadePulso) {
        this.x = x;
        this.y = y;
        this.valor = valor;
        this.raio = raio;
        this.cor = cor;
        this.fase = fase;
        this.velocidadePulso = velocidadePulso;
    }

    /** Raio desenhado no instante informado (em segundos), já com a pulsação. */
    public double raioVisual(double tempo) {
        double pulso = Math.sin(tempo * velocidadePulso + fase);
        return raio * (1.0 + Config.COMIDA_AMPLITUDE_PULSO * pulso);
    }

    /** Move a bolinha na direção de um ponto, sem passar dele. */
    public void atrairPara(double alvoX, double alvoY, double velocidade) {
        double dx = alvoX - x;
        double dy = alvoY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1e-6) {
            return;
        }
        double passo = Math.min(dist, velocidade);
        x += dx / dist * passo;
        y += dy / dist * passo;
    }

    public Vec2 posicao() {
        return new Vec2(x, y);
    }
}
