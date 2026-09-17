package com.cobrinha;

/**
 * Vetor 2D imutável. É usado para posições, direções, velocidades e para os
 * "vetores de desejo" que a IA dos bots soma para decidir para onde virar.
 */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    /** Cria um vetor a partir de um ângulo (radianos) e de um tamanho. */
    public static Vec2 doAngulo(double angulo, double tamanho) {
        return new Vec2(Math.cos(angulo) * tamanho, Math.sin(angulo) * tamanho);
    }

    public Vec2 mais(Vec2 outro) {
        return new Vec2(x + outro.x, y + outro.y);
    }

    public Vec2 menos(Vec2 outro) {
        return new Vec2(x - outro.x, y - outro.y);
    }

    public Vec2 vezes(double k) {
        return new Vec2(x * k, y * k);
    }

    public double tamanho() {
        return Math.sqrt(x * x + y * y);
    }

    public double tamanhoQuadrado() {
        return x * x + y * y;
    }

    public double distancia(Vec2 outro) {
        return Math.sqrt(distanciaQuadrada(outro));
    }

    public double distanciaQuadrada(Vec2 outro) {
        double dx = x - outro.x;
        double dy = y - outro.y;
        return dx * dx + dy * dy;
    }

    /** Ângulo do vetor em radianos, no intervalo (-PI, PI]. */
    public double angulo() {
        return Math.atan2(y, x);
    }

    public Vec2 normalizado() {
        double t = tamanho();
        return t < 1e-9 ? ZERO : new Vec2(x / t, y / t);
    }

    public Vec2 comTamanho(double novoTamanho) {
        return normalizado().vezes(novoTamanho);
    }

    /** Interpolação linear entre dois pontos (t = 0 devolve este vetor). */
    public Vec2 interpolado(Vec2 outro, double t) {
        return new Vec2(x + (outro.x - x) * t, y + (outro.y - y) * t);
    }

    /** Normaliza um ângulo para o intervalo (-PI, PI]. Utilitário de giro. */
    public static double normalizarAngulo(double angulo) {
        double a = (angulo + Math.PI) % (2 * Math.PI);
        if (a < 0) {
            a += 2 * Math.PI;
        }
        return a - Math.PI;
    }
}
