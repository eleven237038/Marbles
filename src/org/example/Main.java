package org.example;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * 游戏主入口
 * 使用Swing事件调度线程初始化UI
 */
public class Main {

    public static void main(String[] args) {
        // 在Swing事件线程中初始化UI，确保线程安全
        SwingUtilities.invokeLater(() -> {
            // 创建游戏窗口
            JFrame frame = new JFrame("Marble Game - Bubble Shooter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 创建游戏引擎并添加到窗口
            GameEngine game = new GameEngine();
            frame.add(game);

            // 自动调整窗口大小以适应游戏画布
            frame.pack();
            frame.setResizable(false);

            // 居中显示窗口
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // 启动游戏渲染
            game.start();
        });
    }
}
