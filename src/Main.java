import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class Main extends GameEngine implements StartMenu.StartMenuListener {
    private Marbles hexGrid;
    private LaunchPad launchPad;
    private LaunchMarble launchMarble;
    private double mouseX = 0;
    private double mouseY = 0;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private boolean gameStarted = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 创建统一的主窗口
            JFrame frame = new JFrame("弹珠游戏");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // 使用CardLayout实现面板切换
            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            // 创建游戏实例
            Main game = new Main();
            game.setupWindow(frame, 483, 1080);

            // 创建主菜单并传入回调
            StartMenu startMenu = new StartMenu(game);

            // 添加两个面板到布局管理器
            mainPanel.add(startMenu, "menu");
            mainPanel.add(game.mPanel, "game");

            // 设置窗口内容
            frame.setContentPane(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null); // 窗口居中
            frame.setVisible(true);

            // 保存引用供回调使用
            game.cardLayout = cardLayout;
            game.mainPanel = mainPanel;
        });
    }

    // 实现StartMenuListener接口的回调方法
    @Override
    public void onStartGame() {
        // 平滑切换到游戏面板
        cardLayout.show(mainPanel, "game");

        // 只初始化和启动游戏一次
        if (!gameStarted) {
            gameStarted = true;
            init();
            gameLoop(60);
        }
    }

    @Override
    public void init() {
        SwingUtilities.invokeLater(() -> {
            initMarbleGrid();
        });
    }

    @Override
    public void setupWindow(JFrame frame, int width, int height) {
        mWidth = 483;
        mHeight = 1080;
        super.setupWindow(frame, mWidth, mHeight);
    }

    private void initMarbleGrid() {
        hexGrid = new Marbles();
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
        hexGrid.setMaxRowCount(18);
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        launchMarble = new LaunchMarble();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init((int) launchPad.cannon.x, (int) launchPad.cannon.y, 0, 0);
    }

    @Override
    public void update(double dt) {
        if (hexGrid != null) hexGrid.update(dt);
        if (launchMarble != null) launchMarble.update(dt);
    }

    @Override
    public void paintComponent() {
        changeBackgroundColor(255, 255, 255);
        clearBackground(mWidth, mHeight);

        Graphics2D g2 = (Graphics2D) mGraphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hexGrid != null) hexGrid.draw(g2);
        if (launchPad != null) {
            launchPad.drawLaunchPad(g2, mWidth, mHeight);
            launchPad.drawCannon(g2, mouseX, mouseY);
        }
        if (launchMarble != null) launchMarble.draw(g2);
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(event.getX(), event.getY());
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(mouseX > 0 ? mouseX : launchPad.cannon.x + 100,
                    mouseY > 0 ? mouseY : launchPad.cannon.y);
        }
    }
}