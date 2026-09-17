package com.cobrinha;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Ponto de entrada: monta a janela e entrega o painel para o loop do jogo. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame(Config.TITULO);
            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            PainelJogo painel = new PainelJogo();
            janela.setContentPane(painel);
            janela.pack();
            janela.setMinimumSize(janela.getSize());
            janela.setLocationRelativeTo(null);
            janela.setVisible(true);

            painel.iniciar();
        });
    }
}
