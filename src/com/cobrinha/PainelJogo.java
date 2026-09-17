package com.cobrinha;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Painel do jogo: roda o loop de 60 quadros por segundo, lê mouse e teclado,
 * desenha o mundo com {@link Graphics2D} e monta a interface (HUD, placar,
 * minimapa e as telas de menu, pausa e fim de jogo).
 */
public class PainelJogo extends JPanel implements ActionListener {

    private enum Estado { MENU, JOGANDO, PAUSADO, FIM }

    private final Timer temporizador = new Timer(Config.MS_POR_QUADRO, this);
    private final Random rnd = new Random();

    private Estado estado = Estado.MENU;
    private Mundo mundo = new Mundo(rnd);

    // --- entrada ---
    private int ponteiroX = -1;
    private int ponteiroY = -1;
    private boolean turboMouse;
    private boolean turboTecla;

    // --- câmera ---
    private double camX;
    private double camY;
    private double zoom = Config.ZOOM_INICIAL;

    // --- tempo e desempenho ---
    private double tempo;
    private long ultimoNano;
    private double fpsMedio = Config.FPS;

    // --- estatísticas congeladas na morte ---
    private double pontosFinais;
    private double comprimentoFinal;
    private int abatesFinais;
    private int posicaoFinal;

    // Formas reaproveitadas para não alocar milhares de objetos por quadro.
    private final Ellipse2D.Double circulo = new Ellipse2D.Double();
    private final Line2D.Double linha = new Line2D.Double();
    private final Path2D.Double caminho = new Path2D.Double();
    private final RoundRectangle2D.Double painel = new RoundRectangle2D.Double();

    private static final Color BRILHO_COMIDA = new Color(255, 255, 255, 120);

    private final Font fonteTitulo = new Font("SansSerif", Font.BOLD, 60);
    private final Font fonteGrande = new Font("SansSerif", Font.BOLD, 26);
    private final Font fonteMedia = new Font("SansSerif", Font.BOLD, 16);
    private final Font fonteNormal = new Font("SansSerif", Font.PLAIN, 14);
    private final Font fontePequena = new Font("SansSerif", Font.PLAIN, 12);
    private final Font fonteNome = new Font("SansSerif", Font.BOLD, 13);

    // Limites do mundo visíveis na tela, usados para descartar o que está fora.
    private double visivelX0;
    private double visivelY0;
    private double visivelX1;
    private double visivelY1;

    public PainelJogo() {
        setPreferredSize(new java.awt.Dimension(Config.LARGURA_JANELA, Config.ALTURA_JANELA));
        setBackground(Config.COR_FUNDO);
        setFocusable(true);
        setDoubleBuffered(true);

        MouseAdapter controles = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                moverPonteiro(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                moverPonteiro(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (estado == Estado.MENU || estado == Estado.FIM) {
                        iniciarPartida();
                    } else {
                        turboMouse = true;
                    }
                }
                requestFocusInWindow();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    turboMouse = false;
                }
            }
        };
        addMouseListener(controles);
        addMouseMotionListener(controles);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tratarTecla(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                tratarTecla(e.getKeyCode(), false);
            }
        });
    }

    /** Liga o loop do jogo. Chamado depois que a janela aparece. */
    public void iniciar() {
        ultimoNano = System.nanoTime();
        temporizador.start();
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Entrada
    // ------------------------------------------------------------------

    void moverPonteiro(int x, int y) {
        ponteiroX = x;
        ponteiroY = y;
    }

    void tratarTecla(int codigo, boolean pressionada) {
        switch (codigo) {
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_SHIFT:
                turboTecla = pressionada;
                return;
            default:
                break;
        }
        if (!pressionada) {
            return;
        }
        switch (codigo) {
            case KeyEvent.VK_P:
            case KeyEvent.VK_ESCAPE:
                if (estado == Estado.JOGANDO) {
                    estado = Estado.PAUSADO;
                } else if (estado == Estado.PAUSADO) {
                    estado = Estado.JOGANDO;
                }
                break;
            case KeyEvent.VK_R:
                iniciarPartida();
                break;
            case KeyEvent.VK_ENTER:
                if (estado == Estado.PAUSADO) {
                    estado = Estado.JOGANDO;
                } else {
                    iniciarPartida();
                }
                break;
            default:
                break;
        }
    }

    /** Começa uma partida nova, do zero. */
    void iniciarPartida() {
        mundo = new Mundo(rnd);
        estado = Estado.JOGANDO;
        turboMouse = false;
        turboTecla = false;
        zoom = Config.ZOOM_INICIAL;
        camX = mundo.jogador.cabecaX;
        camY = mundo.jogador.cabecaY;
        pontosFinais = 0;
        comprimentoFinal = 0;
        abatesFinais = 0;
        posicaoFinal = 0;
    }

    // ------------------------------------------------------------------
    // Loop
    // ------------------------------------------------------------------

    @Override
    public void actionPerformed(ActionEvent e) {
        passo();
    }

    /** Um quadro: mede o FPS, aplica a entrada, roda o mundo e repinta. */
    void passo() {
        long agora = System.nanoTime();
        if (ultimoNano != 0) {
            double delta = (agora - ultimoNano) / 1_000_000_000.0;
            if (delta > 1e-6) {
                fpsMedio += (1.0 / delta - fpsMedio) * 0.08;
            }
        }
        ultimoNano = agora;

        if (estado != Estado.PAUSADO) {
            aplicarEntrada();
            mundo.atualizar();
            tempo += 1.0 / Config.FPS;
            if (estado == Estado.JOGANDO && !mundo.jogador.viva) {
                encerrarPartida();
            }
        }
        atualizarCamera();
        repaint();
    }

    private void aplicarEntrada() {
        Cobra jogador = mundo.jogador;
        if (jogador == null || !jogador.viva) {
            return;
        }
        if (estado == Estado.JOGANDO) {
            jogador.imune = false;
            // A cabeça fica sempre no centro da tela: o ângulo alvo sai direto
            // da posição do ponteiro em relação a esse centro.
            if (ponteiroX >= 0) {
                double dx = ponteiroX - getWidth() / 2.0;
                double dy = ponteiroY - getHeight() / 2.0;
                if (dx * dx + dy * dy > 16) {
                    jogador.anguloAlvo = Math.atan2(dy, dx);
                }
            }
            jogador.querTurbo = turboMouse || turboTecla;
        } else {
            // No menu e na tela de fim a cobra do jogador só passeia de enfeite.
            jogador.imune = true;
            jogador.querTurbo = false;
            jogador.anguloAlvo = jogador.angulo + 0.012;
        }
    }

    private void encerrarPartida() {
        estado = Estado.FIM;
        pontosFinais = mundo.jogador.pontos;
        comprimentoFinal = mundo.jogador.comprimentoCorpo();
        abatesFinais = mundo.jogador.abates;
        posicaoFinal = mundo.melhorPosicaoJogador == Integer.MAX_VALUE ? 0 : mundo.melhorPosicaoJogador;
        turboMouse = false;
        turboTecla = false;
    }

    private void atualizarCamera() {
        Cobra jogador = mundo.jogador;
        if (jogador != null && jogador.viva) {
            camX = jogador.cabecaX;
            camY = jogador.cabecaY;
        }
        double raioReferencia = Config.raioDePontos(Config.PONTOS_INICIAIS);
        double raioAtual = jogador != null ? jogador.raioAtual : raioReferencia;
        double alvo = Config.ZOOM_INICIAL * Math.pow(raioReferencia / raioAtual, Config.ZOOM_EXPOENTE);
        alvo = Config.limitar(alvo, Config.ZOOM_MINIMO, Config.ZOOM_INICIAL);
        zoom += (alvo - zoom) * Config.ZOOM_SUAVIDADE;
    }

    // ------------------------------------------------------------------
    // Desenho
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics graficos) {
        super.paintComponent(graficos);
        Graphics2D g = (Graphics2D) graficos.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int largura = Math.max(1, getWidth());
            int altura = Math.max(1, getHeight());

            g.setColor(Config.COR_FUNDO);
            g.fillRect(0, 0, largura, altura);

            double meiaLargura = largura / (2.0 * zoom);
            double meiaAltura = altura / (2.0 * zoom);
            visivelX0 = camX - meiaLargura;
            visivelX1 = camX + meiaLargura;
            visivelY0 = camY - meiaAltura;
            visivelY1 = camY + meiaAltura;

            AffineTransform original = g.getTransform();
            g.translate(largura / 2.0, altura / 2.0);
            g.scale(zoom, zoom);
            g.translate(-camX, -camY);

            desenharGrade(g);
            desenharBordaArena(g);
            desenharComidas(g);
            desenharCobras(g);

            g.setTransform(original);

            desenharNomes(g, largura, altura);
            // No menu a arena fica só de cenário: nada de estatísticas vazias.
            if (estado != Estado.MENU) {
                desenharHud(g, altura);
                desenharPlacar(g, largura);
                desenharMinimapa(g, largura, altura);
            }
            desenharFps(g, largura, altura);
            desenharTelas(g, largura, altura);
        } finally {
            g.dispose();
        }
    }

    /** Grade sutil de fundo, desenhada só na parte visível do mundo. */
    private void desenharGrade(Graphics2D g) {
        g.setColor(Config.COR_GRADE);
        g.setStroke(new BasicStroke((float) (1.0 / zoom)));
        double passo = Config.ESPACO_GRADE;
        double x0 = Math.floor(visivelX0 / passo) * passo;
        for (double x = x0; x <= visivelX1; x += passo) {
            linha.setLine(x, visivelY0, x, visivelY1);
            g.draw(linha);
        }
        double y0 = Math.floor(visivelY0 / passo) * passo;
        for (double y = y0; y <= visivelY1; y += passo) {
            linha.setLine(visivelX0, y, visivelX1, y);
            g.draw(linha);
        }
    }

    /** Borda circular da arena, com um halo neon. A linha mata quem encosta. */
    private void desenharBordaArena(Graphics2D g) {
        double r = Config.RAIO_ARENA;
        circulo.setFrame(-r, -r, r * 2, r * 2);
        for (int i = 3; i >= 1; i--) {
            g.setColor(comAlfa(Config.COR_BORDA_ARENA, 22 * i));
            g.setStroke(new BasicStroke((float) (i * 9 / zoom)));
            g.draw(circulo);
        }
        g.setColor(Config.COR_BORDA_ARENA);
        g.setStroke(new BasicStroke((float) (3.0 / zoom)));
        g.draw(circulo);
    }

    private void desenharComidas(Graphics2D g) {
        List<Comida> comidas = mundo.comidas;
        double margem = 24;
        // O halo é caro: só as bolinhas gordas (ou a tela bem de perto) ganham um.
        double valorComHalo = Config.COMIDA_VALOR_MAX * (zoom > 0.75 ? 0.45 : 0.9);
        for (int i = 0; i < comidas.size(); i++) {
            Comida c = comidas.get(i);
            if (!c.viva || foraDaVista(c.x, c.y, margem)) {
                continue;
            }
            double raio = c.raioVisual(tempo);
            if (c.valor >= valorComHalo && raio * zoom > 3.0) {
                g.setColor(comAlfa(c.cor, 58));
                circulo.setFrame(c.x - raio * 1.85, c.y - raio * 1.85, raio * 3.7, raio * 3.7);
                g.fill(circulo);
            }
            g.setColor(c.cor);
            circulo.setFrame(c.x - raio, c.y - raio, raio * 2, raio * 2);
            g.fill(circulo);
            if (raio * zoom > 4.0) {
                double brilho = raio * 0.42;
                g.setColor(BRILHO_COMIDA);
                circulo.setFrame(c.x - raio * 0.3 - brilho, c.y - raio * 0.3 - brilho,
                        brilho * 2, brilho * 2);
                g.fill(circulo);
            }
        }
    }

    private void desenharCobras(Graphics2D g) {
        List<Cobra> cobras = mundo.cobras;
        for (int i = 0; i < cobras.size(); i++) {
            Cobra cobra = cobras.get(i);
            if (!cobra.viva) {
                continue;
            }
            desenharCobra(g, cobra);
        }
    }

    private void desenharCobra(Graphics2D g, Cobra cobra) {
        List<Vec2> segmentos = cobra.segmentos;
        if (segmentos.isEmpty()) {
            return;
        }
        double raio = cobra.raioAtual;
        double margem = raio * 3;
        // Descarte rápido: corpo inteiro fora da tela.
        if (cobra.cabecaX + cobra.alcanceCorpo + margem < visivelX0
                || cobra.cabecaX - cobra.alcanceCorpo - margem > visivelX1
                || cobra.cabecaY + cobra.alcanceCorpo + margem < visivelY0
                || cobra.cabecaY - cobra.alcanceCorpo - margem > visivelY1) {
            return;
        }

        Color corBase = cobra.corCorpo;
        Color corFaixa = Cobra.escurecer(corBase, 0.32);
        Color corContorno = Cobra.escurecer(corBase, 0.62);

        // Silhueta: uma única linha grossa por baixo dos círculos.
        caminho.reset();
        boolean primeiro = true;
        for (int i = segmentos.size() - 1; i >= 0; i--) {
            Vec2 s = segmentos.get(i);
            if (primeiro) {
                caminho.moveTo(s.x(), s.y());
                primeiro = false;
            } else {
                caminho.lineTo(s.x(), s.y());
            }
        }
        g.setColor(corContorno);
        g.setStroke(new BasicStroke((float) ((raio + 2.2) * 2), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g.draw(caminho);

        if (cobra.turbinando) {
            g.setColor(comAlfa(cobra.corBrilho, 55));
            g.setStroke(new BasicStroke((float) ((raio + 7) * 2), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(caminho);
        }

        // Os círculos amostrados do rastro, da cauda para a cabeça.
        for (int i = segmentos.size() - 1; i >= 0; i--) {
            Vec2 s = segmentos.get(i);
            if (foraDaVista(s.x(), s.y(), margem)) {
                continue;
            }
            g.setColor((i / 5) % 2 == 0 ? corBase : corFaixa);
            circulo.setFrame(s.x() - raio, s.y() - raio, raio * 2, raio * 2);
            g.fill(circulo);
        }

        desenharCabeca(g, cobra, raio);
    }

    private void desenharCabeca(Graphics2D g, Cobra cobra, double raio) {
        if (foraDaVista(cobra.cabecaX, cobra.cabecaY, raio * 4)) {
            return;
        }
        double x = cobra.cabecaX;
        double y = cobra.cabecaY;

        g.setColor(comAlfa(cobra.corBrilho, cobra.turbinando ? 90 : 45));
        circulo.setFrame(x - raio * 1.7, y - raio * 1.7, raio * 3.4, raio * 3.4);
        g.fill(circulo);

        g.setColor(cobra.corBrilho);
        circulo.setFrame(x - raio, y - raio, raio * 2, raio * 2);
        g.fill(circulo);

        // Olhos: dois pontos claros com a pupila olhando para onde a cobra vai.
        double lado = 0.62;
        double distanciaOlho = raio * 0.52;
        double raioOlho = raio * 0.36;
        double raioPupila = raioOlho * 0.55;
        for (int s = -1; s <= 1; s += 2) {
            double ang = cobra.angulo + s * lado;
            double ox = x + Math.cos(ang) * distanciaOlho;
            double oy = y + Math.sin(ang) * distanciaOlho;
            g.setColor(Color.WHITE);
            circulo.setFrame(ox - raioOlho, oy - raioOlho, raioOlho * 2, raioOlho * 2);
            g.fill(circulo);
            double px = ox + Math.cos(cobra.angulo) * raioOlho * 0.42;
            double py = oy + Math.sin(cobra.angulo) * raioOlho * 0.42;
            g.setColor(new Color(18, 18, 30));
            circulo.setFrame(px - raioPupila, py - raioPupila, raioPupila * 2, raioPupila * 2);
            g.fill(circulo);
        }
    }

    /** Nomes das cobras, desenhados em coordenadas de tela para ficarem legíveis. */
    private void desenharNomes(Graphics2D g, int largura, int altura) {
        g.setFont(fonteNome);
        List<Cobra> cobras = mundo.cobras;
        for (int i = 0; i < cobras.size(); i++) {
            Cobra cobra = cobras.get(i);
            if (!cobra.viva || foraDaVista(cobra.cabecaX, cobra.cabecaY, 120)) {
                continue;
            }
            int x = (int) Math.round((cobra.cabecaX - camX) * zoom + largura / 2.0);
            int y = (int) Math.round((cobra.cabecaY - camY) * zoom + altura / 2.0
                    - (cobra.raioAtual * zoom + 14));
            String texto = cobra.nome;
            int meio = g.getFontMetrics().stringWidth(texto) / 2;
            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(texto, x - meio + 1, y + 1);
            g.setColor(cobra.humana ? Config.COR_JOGADOR : comAlfa(cobra.corBrilho, 210));
            g.drawString(texto, x - meio, y);
        }
    }

    private void desenharHud(Graphics2D g, int altura) {
        Cobra jogador = mundo.jogador;
        boolean vivo = jogador != null && jogador.viva;
        double pontos = vivo ? jogador.pontos : pontosFinais;
        double comprimento = vivo ? jogador.comprimentoCorpo() : comprimentoFinal;
        int abates = vivo ? jogador.abates : abatesFinais;
        int posicao = vivo ? mundo.posicaoDe(jogador) : posicaoFinal;

        // Painel de estatísticas no canto superior esquerdo.
        desenharPainel(g, 16, 16, 232, 120);
        g.setFont(fonteMedia);
        g.setColor(Config.COR_TEXTO_FRACO);
        g.drawString("PONTOS", 32, 42);
        g.drawString("COMPRIMENTO", 32, 66);
        g.drawString("ABATES", 32, 90);
        g.drawString("POSIÇÃO", 32, 114);
        g.setColor(Config.COR_TEXTO);
        desenharTextoDireita(g, String.valueOf((int) pontos), 232, 42);
        desenharTextoDireita(g, String.valueOf((int) comprimento), 232, 66);
        desenharTextoDireita(g, String.valueOf(abates), 232, 90);
        desenharTextoDireita(g, posicao > 0 ? posicao + "º de " + mundo.totalDeCobras() : "-", 232, 114);

        // Barra de turbo no canto inferior esquerdo.
        int barraX = 16;
        int barraY = altura - 46;
        int barraLargura = 232;
        int barraAltura = 16;
        double sobra = Math.max(0, pontos - Config.TURBO_PONTOS_MINIMOS);
        double carga = Config.limitar(sobra / Config.TURBO_PONTOS_BARRA_CHEIA, 0, 1);
        boolean podeTurbo = pontos > Config.TURBO_PONTOS_MINIMOS;

        g.setFont(fontePequena);
        g.setColor(Config.COR_TEXTO_FRACO);
        g.drawString("TURBO", barraX, barraY - 6);
        painel.setRoundRect(barraX, barraY, barraLargura, barraAltura, 8, 8);
        g.setColor(new Color(22, 26, 42));
        g.fill(painel);
        if (carga > 0) {
            painel.setRoundRect(barraX, barraY, Math.max(8, barraLargura * carga), barraAltura, 8, 8);
            g.setColor(podeTurbo ? Config.COR_TURBO : new Color(90, 90, 110));
            g.fill(painel);
        }
        if (vivo && jogador.turbinando) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            painel.setRoundRect(barraX, barraY, barraLargura, barraAltura, 8, 8);
            g.draw(painel);
        }
    }

    private void desenharFps(Graphics2D g, int largura, int altura) {
        g.setFont(fontePequena);
        g.setColor(Config.COR_TEXTO_FRACO);
        desenharTextoDireita(g, String.format("%.0f FPS", fpsMedio), largura - 16, altura - 16);
    }

    private void desenharPlacar(Graphics2D g, int largura) {
        List<Cobra> ranking = mundo.ranking;
        int linhas = Math.min(Config.PLACAR_LINHAS, ranking.size());
        int painelLargura = 218;
        int painelAltura = 40 + linhas * 20;
        int x = largura - painelLargura - 16;
        desenharPainel(g, x, 16, painelLargura, painelAltura);

        g.setFont(fonteMedia);
        g.setColor(Config.COR_TEXTO_FRACO);
        g.drawString("PLACAR", x + 16, 40);

        g.setFont(fonteNormal);
        for (int i = 0; i < linhas; i++) {
            Cobra c = ranking.get(i);
            int y = 62 + i * 20;
            boolean eu = c.humana;
            g.setColor(eu ? Config.COR_JOGADOR : comAlfa(c.corBrilho, 225));
            g.drawString((i + 1) + ". " + encurtar(c.nome, 13), x + 16, y);
            g.setColor(eu ? Config.COR_JOGADOR : Config.COR_TEXTO_FRACO);
            desenharTextoDireita(g, String.valueOf((int) c.pontos), x + painelLargura - 16, y);
        }
    }

    private void desenharMinimapa(Graphics2D g, int largura, int altura) {
        int raio = Config.MINIMAPA_RAIO;
        int centroX = largura - raio - Config.MINIMAPA_MARGEM;
        int centroY = altura - raio - Config.MINIMAPA_MARGEM - 34;

        circulo.setFrame(centroX - raio, centroY - raio, raio * 2, raio * 2);
        g.setColor(new Color(10, 13, 24, 205));
        g.fill(circulo);
        g.setColor(comAlfa(Config.COR_BORDA_ARENA, 150));
        g.setStroke(new BasicStroke(2f));
        g.draw(circulo);

        double escala = raio / Config.RAIO_ARENA;
        List<Cobra> cobras = mundo.cobras;
        for (int i = 0; i < cobras.size(); i++) {
            Cobra c = cobras.get(i);
            if (!c.viva) {
                continue;
            }
            double px = centroX + c.cabecaX * escala;
            double py = centroY + c.cabecaY * escala;
            double tamanho = c.humana ? 4.2 : 2.6 + Math.min(2.4, c.pontos / 900.0);
            g.setColor(c.humana ? Config.COR_JOGADOR : comAlfa(c.corCorpo, 220));
            circulo.setFrame(px - tamanho, py - tamanho, tamanho * 2, tamanho * 2);
            g.fill(circulo);
            if (c.humana) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.4f));
                circulo.setFrame(px - tamanho - 3, py - tamanho - 3, (tamanho + 3) * 2, (tamanho + 3) * 2);
                g.draw(circulo);
            }
        }
    }

    private void desenharTelas(Graphics2D g, int largura, int altura) {
        if (estado == Estado.JOGANDO) {
            return;
        }
        g.setColor(new Color(6, 8, 16, estado == Estado.MENU ? 175 : 155));
        g.fillRect(0, 0, largura, altura);
        int meioX = largura / 2;
        int meioY = altura / 2;

        switch (estado) {
            case MENU -> {
                g.setFont(fonteTitulo);
                g.setColor(Config.COR_JOGADOR);
                desenharTextoCentralizado(g, "COBRA ARENA", meioX, meioY - 80);
                g.setFont(fonteGrande);
                g.setColor(Config.COR_TEXTO);
                desenharTextoCentralizado(g, "Cresça, sobreviva, lidere o placar", meioX, meioY - 34);
                g.setFont(fonteNormal);
                g.setColor(Config.COR_TEXTO_FRACO);
                desenharTextoCentralizado(g, "O mouse guia a cobrinha — a cabeça fica sempre no centro da tela",
                        meioX, meioY + 6);
                desenharTextoCentralizado(g, "Botão esquerdo, ESPAÇO ou SHIFT dão turbo (custa pontos)",
                        meioX, meioY + 28);
                desenharTextoCentralizado(g, "Encostar a cabeça em outra cobra ou na borda é morte na certa",
                        meioX, meioY + 50);
                desenharTextoCentralizado(g, "P / ESC pausa    •    R / ENTER reinicia", meioX, meioY + 72);
                g.setFont(fonteMedia);
                g.setColor(Config.COR_TURBO);
                desenharTextoCentralizado(g, "ENTER ou clique para começar", meioX, meioY + 118);
            }
            case PAUSADO -> {
                g.setFont(fonteTitulo);
                g.setColor(Config.COR_TEXTO);
                desenharTextoCentralizado(g, "PAUSADO", meioX, meioY - 10);
                g.setFont(fonteNormal);
                g.setColor(Config.COR_TEXTO_FRACO);
                desenharTextoCentralizado(g, "P, ESC ou ENTER para voltar    •    R para reiniciar",
                        meioX, meioY + 30);
            }
            case FIM -> {
                g.setFont(fonteTitulo);
                g.setColor(new Color(255, 110, 130));
                desenharTextoCentralizado(g, "VOCÊ FOI ABATIDO", meioX, meioY - 70);
                g.setFont(fonteGrande);
                g.setColor(Config.COR_TEXTO);
                desenharTextoCentralizado(g, (int) pontosFinais + " pontos", meioX, meioY - 20);
                g.setFont(fonteNormal);
                g.setColor(Config.COR_TEXTO_FRACO);
                desenharTextoCentralizado(g, "Comprimento: " + (int) comprimentoFinal
                        + "    •    Abates: " + abatesFinais
                        + "    •    Melhor posição: " + (posicaoFinal > 0 ? posicaoFinal + "º" : "-"),
                        meioX, meioY + 16);
                g.setFont(fonteMedia);
                g.setColor(Config.COR_TURBO);
                desenharTextoCentralizado(g, "R, ENTER ou clique para jogar de novo", meioX, meioY + 62);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilitários de desenho
    // ------------------------------------------------------------------

    private void desenharPainel(Graphics2D g, double x, double y, double largura, double altura) {
        painel.setRoundRect(x, y, largura, altura, 14, 14);
        g.setColor(Config.COR_PAINEL);
        g.fill(painel);
        g.setColor(new Color(46, 54, 84));
        Stroke anterior = g.getStroke();
        g.setStroke(new BasicStroke(1f));
        g.draw(painel);
        g.setStroke(anterior);
    }

    private void desenharTextoCentralizado(Graphics2D g, String texto, int x, int y) {
        int metade = g.getFontMetrics().stringWidth(texto) / 2;
        g.drawString(texto, x - metade, y);
    }

    private void desenharTextoDireita(Graphics2D g, String texto, int x, int y) {
        g.drawString(texto, x - g.getFontMetrics().stringWidth(texto), y);
    }

    private boolean foraDaVista(double x, double y, double margem) {
        return x + margem < visivelX0 || x - margem > visivelX1
                || y + margem < visivelY0 || y - margem > visivelY1;
    }

    private static String encurtar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite - 1) + "…";
    }

    private static Color comAlfa(Color cor, int alfa) {
        return new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), Math.max(0, Math.min(255, alfa)));
    }
}
