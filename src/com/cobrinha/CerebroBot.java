package com.cobrinha;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cérebro de um bot. A cada quadro ele soma três vetores de desejo e vira a
 * cobra na direção da soma:
 *
 * <ol>
 *   <li><b>Fuga</b>: empurrão contrário aos corpos das outras cobras, mais
 *       forte quanto mais perto e mais à frente estiver o obstáculo.</li>
 *   <li><b>Centro</b>: puxão de volta para o meio da arena quando a cobra
 *       chega perto da borda que mata.</li>
 *   <li><b>Caça</b>: direção da comida com a melhor relação valor/distância
 *       dentro do campo de visão.</li>
 * </ol>
 *
 * <p>Quando a ameaça passa de um limiar, o bot ainda solta um turbo ocasional
 * para tentar escapar da enrascada.</p>
 */
public class CerebroBot {

    private final Random rnd;
    private final List<Comida> comidasPerto = new ArrayList<>();

    /** Ruído lento que dá personalidade ao rumo e evita bots em linha reta. */
    private double deriva;
    private int quadrosTurbo;
    private int quadrosAteDecidir;
    private Comida alvo;
    private double ameaca;

    public CerebroBot(Random rnd) {
        this.rnd = rnd;
        this.quadrosAteDecidir = rnd.nextInt(Config.BOT_QUADROS_ENTRE_DECISOES + 1);
    }

    public void pensar(Mundo mundo, Cobra eu) {
        Vec2 fuga = desejoDeFuga(mundo, eu);
        Vec2 centro = desejoDeCentro(eu);
        Vec2 caca = desejoDeCaca(mundo, eu);

        Vec2 desejo = fuga.vezes(Config.BOT_PESO_FUGA)
                .mais(centro.vezes(Config.BOT_PESO_CENTRO))
                .mais(caca.vezes(Config.BOT_PESO_CACA));

        deriva = deriva * Config.BOT_AMORTECIMENTO_RUIDO
                + (rnd.nextDouble() - 0.5) * Config.BOT_RUIDO;

        if (desejo.tamanhoQuadrado() < 1e-8) {
            // Sem nada em vista: segue em frente com uma curva preguiçosa.
            eu.anguloAlvo = eu.angulo + deriva * 4;
        } else {
            eu.anguloAlvo = Vec2.normalizarAngulo(desejo.angulo() + deriva);
        }

        eu.querTurbo = decidirTurbo(eu);
    }

    /** Soma dos empurrões contrários aos corpos alheios que estão por perto. */
    private Vec2 desejoDeFuga(Mundo mundo, Cobra eu) {
        double visao = Config.BOT_VISAO_CORPO + eu.raioAtual;
        double frenteX = Math.cos(eu.angulo);
        double frenteY = Math.sin(eu.angulo);
        double somaX = 0;
        double somaY = 0;
        double pior = 0;

        for (int k = 0; k < mundo.cobras.size(); k++) {
            Cobra outra = mundo.cobras.get(k);
            if (outra == eu || !outra.viva) {
                continue;
            }
            double distCabecas = Math.hypot(outra.cabecaX - eu.cabecaX, outra.cabecaY - eu.cabecaY);
            if (distCabecas > outra.alcanceCorpo + visao + outra.raioAtual) {
                continue; // corpo inteiro longe demais: descarte rápido
            }
            double limite = visao + outra.raioAtual;
            double limite2 = limite * limite;
            List<Vec2> segmentos = outra.segmentos;
            for (int i = 0; i < segmentos.size(); i += Config.BOT_PASSO_AMOSTRA) {
                Vec2 s = segmentos.get(i);
                double dx = eu.cabecaX - s.x();
                double dy = eu.cabecaY - s.y();
                double d2 = dx * dx + dy * dy;
                if (d2 > limite2 || d2 < 1e-9) {
                    continue;
                }
                double d = Math.sqrt(d2);
                double proximidade = 1.0 - d / limite;
                double peso = proximidade * proximidade;
                // O que está na frente do focinho assusta bem mais.
                double alinhamento = -(dx * frenteX + dy * frenteY) / d;
                if (alinhamento > 0) {
                    peso *= 1.0 + alinhamento * 1.6;
                }
                somaX += dx / d * peso;
                somaY += dy / d * peso;
                if (peso > pior) {
                    pior = peso;
                }
            }
        }
        ameaca = pior;
        Vec2 fuga = new Vec2(somaX, somaY);
        double tamanho = fuga.tamanho();
        if (tamanho < 1e-9) {
            return Vec2.ZERO;
        }
        // Limita a soma para um empurrão forte não saturar os outros desejos.
        return fuga.vezes(Math.min(2.0, tamanho) / tamanho);
    }

    /** Puxão de volta para o centro quando a borda da arena está perto. */
    private Vec2 desejoDeCentro(Cobra eu) {
        double distancia = eu.distanciaDoCentro();
        double limite = Config.RAIO_ARENA - Config.BOT_MARGEM_BORDA;
        if (distancia < limite || distancia < 1e-6) {
            return Vec2.ZERO;
        }
        double t = Math.min(1.4, (distancia - limite) / Config.BOT_MARGEM_BORDA);
        double intensidade = 0.25 + t * t * 1.75;
        return new Vec2(-eu.cabecaX / distancia, -eu.cabecaY / distancia).vezes(intensidade);
    }

    /** Direção da comida com a melhor relação valor/distância. */
    private Vec2 desejoDeCaca(Mundo mundo, Cobra eu) {
        quadrosAteDecidir--;
        if (quadrosAteDecidir <= 0 || alvo == null || !alvo.viva) {
            escolherAlvo(mundo, eu);
            quadrosAteDecidir = Config.BOT_QUADROS_ENTRE_DECISOES;
        }
        if (alvo == null || !alvo.viva) {
            return Vec2.ZERO;
        }
        Vec2 rumo = new Vec2(alvo.x - eu.cabecaX, alvo.y - eu.cabecaY);
        return rumo.normalizado();
    }

    private void escolherAlvo(Mundo mundo, Cobra eu) {
        mundo.coletarComidasPerto(eu.cabecaX, eu.cabecaY, Config.BOT_VISAO_COMIDA, comidasPerto);
        Comida melhor = null;
        double melhorNota = 0;
        for (int i = 0; i < comidasPerto.size(); i++) {
            Comida c = comidasPerto.get(i);
            double distancia = Math.hypot(c.x - eu.cabecaX, c.y - eu.cabecaY);
            // Comida colada na borda não compensa o risco.
            double distanciaDoCentro = Math.hypot(c.x, c.y);
            if (distanciaDoCentro > Config.RAIO_ARENA - Config.BOT_MARGEM_BORDA * 0.35) {
                continue;
            }
            double nota = c.valor / (distancia + Config.BOT_PESO_DISTANCIA_COMIDA);
            if (nota > melhorNota) {
                melhorNota = nota;
                melhor = c;
            }
        }
        alvo = melhor;
    }

    /** Turbo ocasional: só quando está ameaçado e tem pontos de sobra. */
    private boolean decidirTurbo(Cobra eu) {
        if (quadrosTurbo > 0) {
            quadrosTurbo--;
        } else if (ameaca > Config.BOT_LIMIAR_AMEACA
                && eu.pontos > Config.TURBO_PONTOS_MINIMOS * 2.5
                && rnd.nextDouble() < Config.BOT_CHANCE_TURBO) {
            int extra = Config.BOT_QUADROS_TURBO_MAX - Config.BOT_QUADROS_TURBO_MIN + 1;
            quadrosTurbo = Config.BOT_QUADROS_TURBO_MIN + rnd.nextInt(extra);
        }
        return quadrosTurbo > 0;
    }
}
