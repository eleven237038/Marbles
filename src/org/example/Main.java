package org.example;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

// 游戏主入口
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marble Game - Bubble Shooter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            GameEngine game = new GameEngine();
            frame.add(game);

            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.start();
        });
    }
}