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
    private boolean frozen = false;
    private double deadline;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("弹珠游戏");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            Main game = new Main(frame);

            StartMenu startMenu = new StartMenu(game);

            mainPanel.add(startMenu, "menu");
            mainPanel.add(game.mPanel, "game");

            frame.setContentPane(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.cardLayout = cardLayout;
            game.mainPanel = mainPanel;
        });
    }

    public Main(JFrame frame) {
        mFrame = frame;
        mPanel = new GamePanel();
        mWidth = 483;
        mHeight = 1080;

        mPanel.setDoubleBuffered(true);
        mPanel.addMouseListener(this);
        mPanel.addMouseMotionListener(this);

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        switch (e.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            Main.this.keyPressed(e);
                            return false;
                        case KeyEvent.KEY_RELEASED:
                            Main.this.keyReleased(e);
                            return false;
                        case KeyEvent.KEY_TYPED:
                            Main.this.keyTyped(e);
                            return false;
                        default:
                            return false;
                        }
                    }
                });

        Insets insets = mFrame.getInsets();
        mFrame.setSize(mWidth + insets.left + insets.right, mHeight + insets.top + insets.bottom);
    }

    @Override
    public void onStartGame() {
        cardLayout.show(mainPanel, "game");

        if (!gameStarted) {
            gameStarted = true;
            init();
            gameLoop(60);
        }
    }

    @Override
    public void init() {
        SwingUtilities.invokeLater(() -> {
            mFrame.setLayout(new BorderLayout());
            mPanel.setPreferredSize(new Dimension(mWidth, mHeight));
            mFrame.add(mPanel, BorderLayout.CENTER);
            mFrame.revalidate();
            initMarbleGrid();
        });
    }

    private void initMarbleGrid() {
        hexGrid = new Marbles();
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
        hexGrid.setMaxRowCount(18);
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();
        launchMarble = new LaunchMarble();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init((int) launchPad.cannon.x, (int) launchPad.cannon.y, 0, 0);
    }

    @Override
    public void update(double dt) {
        if (frozen) return;
        if (hexGrid != null) hexGrid.update(dt);
        if (launchMarble != null) launchMarble.update(dt);
        checkCollisions();
        collisionWithDeadline();
    }

    // 修复：引入连续碰撞检测(CCD)，防止低帧率下速度过快导致的穿模跳过问题
    private void checkCollisions() {
        if (launchMarble == null || !launchMarble.isLaunched() || hexGrid == null) return;

        double radius = hexGrid.getSide() * 0.866;
        double collisionDist = radius * 2 - 2;

        double prevX = launchMarble.getPrevCenterX();
        double prevY = launchMarble.getPrevCenterY();
        double currX = launchMarble.getCenterX();
        double currY = launchMarble.getCenterY();

        double dx = currX - prevX;
        double dy = currY - prevY;

        // 根据位移大小划分检测步长，确保每步跨度小于半径的一半
        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / (radius * 0.5));
        if (steps < 1) steps = 1;

        boolean collided = false;

        for (int i = 1; i <= steps; i++) {
            double checkX = prevX + dx * i / steps;
            double checkY = prevY + dy * i / steps;

            // 情况1：触顶判断
            if (checkY <= radius * 2) {
                collided = true;
                launchMarble.setCenter(checkX, checkY);
                break;
            }

            // 情况2：撞击到已有弹珠
            for (int r = hexGrid.getMaxRowCount(); r < hexGrid.getMarblesLength(); r++) {
                Marble[] row = hexGrid.getRow(r);
                if (row == null) continue;
                for (Marble m : row) {
                    if (m != null && m.isInitialized()) {
                        double dX = checkX - m.getCenterX();
                        double dY = checkY - m.getCenterY();
                        double distSq = dX * dX + dY * dY;
                        if (distSq <= collisionDist * collisionDist) {
                            collided = true;
                            launchMarble.setCenter(checkX, checkY);
                            break;
                        }
                    }
                }
                if (collided) break;
            }
            if (collided) break;
        }

        // 3. 处理碰撞后效
        if (collided) {
            hexGrid.attachMarble(launchMarble, mWidth);
            launchMarble = new LaunchMarble();
            launchMarble.setScreenSize(mWidth, mHeight);
            launchMarble.init((int) launchPad.cannon.x, (int) launchPad.cannon.y, 0, 0);
        }
    }

    // 修复：去掉硬编码的 50 列，基于当前实际行长迭代，防止越界或漏判
    private void collisionWithDeadline() {
        if (hexGrid == null) return;
        double radius = hexGrid.getSide() * 0.866;
        for (int r = hexGrid.getMaxRowCount(); r < hexGrid.getMarblesLength(); r++) {
            Marble[] row = hexGrid.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.length; c++) {
                Marble marble = row[c];
                if (marble != null && marble.isInitialized() && marble.getCenterY() + radius >= deadline) {
                    frozen = true;
                    return;
                }
            }
        }
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
        if (frozen) return;
        mouseX = event.getX();
        mouseY = event.getY();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (frozen) return;
        if (launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(event.getX(), event.getY());
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen) return;
        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(mouseX > 0 ? mouseX : launchPad.cannon.x + 100,
                    mouseY > 0 ? mouseY : launchPad.cannon.y);
        }
    }
}