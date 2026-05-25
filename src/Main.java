import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.geom.RoundRectangle2D;
import java.awt.LinearGradientPaint;
import java.util.Random;

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
    private Random random = new Random();
    private StartMenu startMenu; // 保存菜单引用用于销毁 Timer

    // 暂停按钮相关
    private BufferedImage pauseIcon;
    private Rectangle pauseButtonBounds;
    private boolean pauseHover = false;
    private boolean pausePressed = false;
    private boolean gamePaused = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("弹珠游戏");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            Main game = new Main(frame);

            StartMenu startMenu = new StartMenu(game);
            game.startMenu = startMenu;

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
        mPanel.setPreferredSize(new Dimension(mWidth, mHeight));

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

        // 加载pause.png精灵图
        try {
            File pauseFile = new File("resources/pause.png");
            if (pauseFile.exists()) {
                pauseIcon = ImageIO.read(pauseFile);
            }
        } catch (IOException e) {
            pauseIcon = null;
        }

        // 先初始化按钮对象，位置在init中动态设置
        pauseButtonBounds = new Rectangle(30, 0, 60, 60);
    }

    @Override
    public void onStartGame() {
        cardLayout.show(mainPanel, "game");

        if (!gameStarted) {
            gameStarted = true;
            // [Bug 7] 修复：销毁菜单里的后台 Timer
            if (startMenu != null) {
                startMenu.stopAnimation();
            }
            init();
            gameLoop(60);
        }
    }

    // [Bug 6] 修复：安全退出接口
    @Override
    public void onExitGame() {
        System.exit(0);
    }

    @Override
    public void init() {
        initMarbleGrid();
        // [Bug 9] 修复: 此时 deadline 已经初始化好了，正确对齐位置
        pauseButtonBounds.y = (int) deadline + 20;
    }

    private void initMarbleGrid() {
        hexGrid = new Marbles();
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
        hexGrid.setMaxRowCount(18);
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();

        // 初始化：生成第一个当前弹珠和第一个下一个弹珠
        launchMarble = new LaunchMarble();
        launchMarble.setScreenSize(mWidth, mHeight);
        // 使用精确的 double 位置初始化
        launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);

        // 生成第一个下一个弹珠颜色
        launchPad.setNextMarbleColorType(random.nextInt(4) + 1);
    }

    @Override
    public void update(double dt) {
        if (frozen || gamePaused) return;
        if (hexGrid != null) hexGrid.update(dt);
        if (launchMarble != null) launchMarble.update(dt);
        checkCollisions();
        collisionWithDeadline();
    }

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

        // [Bug 11] 修复：加大碰撞检测分步密度，减少穿模
        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / (radius * 0.25));
        if (steps < 1) steps = 1;

        boolean collided = false;

        for (int i = 1; i <= steps; i++) {
            double checkX = prevX + dx * i / steps;
            double checkY = prevY + dy * i / steps;

            // [Bug 2] 修复：触顶判断改为真正的 radius 而不是 radius * 2
            if (checkY <= radius) {
                collided = true;
                launchMarble.setCenter(checkX, checkY);
                break;
            }

            for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
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

        if (collided) {
            hexGrid.attachMarble(launchMarble, mWidth);

            // [Bug 5, Bug 8, Bug 12] 修复: 在 init 时提前赋予下一次正确的颜色类型
            // 且不再强转整数保留 double 精确度，继承原始炮台状态
            int nextColor = launchPad.getNextMarbleColorType();
            launchMarble = new LaunchMarble();
            launchMarble.setScreenSize(mWidth, mHeight);
            launchMarble.setColorType(nextColor);
            launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);

            // 2. 随机生成新的下一个弹珠颜色，更新预览区
            launchPad.setNextMarbleColorType(random.nextInt(4) + 1);
        }
    }

    private void collisionWithDeadline() {
        if (hexGrid == null) return;
        double radius = hexGrid.getSide() * 0.866;
        for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
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
        // 绘制与主界面相同的渐变背景
        Graphics2D g2 = (Graphics2D) mGraphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LinearGradientPaint bg = new LinearGradientPaint(
                0, 0, 0, mHeight,
                new float[]{0, 1},
                new Color[]{new Color(230, 245, 255), new Color(190, 225, 255)}
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, mWidth, mHeight);

        if (hexGrid != null) hexGrid.draw(g2);
        if (launchPad != null) {
            launchPad.drawLaunchPad(g2, mWidth, mHeight);
            launchPad.drawCannon(g2, mouseX, mouseY);
        }
        if (launchMarble != null) launchMarble.draw(g2);

        // 最后绘制暂停按钮，确保在最上层
        drawPauseButton(g2);
    }

    // 图片完全占据按钮，无任何内边距
    private void drawPauseButton(Graphics2D g2) {
        int x = pauseButtonBounds.x;
        int y = pauseButtonBounds.y;
        int w = pauseButtonBounds.width;
        int h = pauseButtonBounds.height;

        // 绘制按钮背景
        RoundRectangle2D bg = new RoundRectangle2D.Double(x, y, w, h, 15, 15);

        if (pausePressed) {
            Color c1 = new Color(50, 140, 255, 200);
            Color c2 = new Color(30, 110, 255, 200);
            LinearGradientPaint grad = new LinearGradientPaint(x, y, x, y + h,
                    new float[]{0, 1}, new Color[]{c1, c2});
            g2.setPaint(grad);
        } else if (pauseHover) {
            Color c1 = new Color(100, 190, 255, 180);
            Color c2 = new Color(50, 140, 255, 180);
            LinearGradientPaint grad = new LinearGradientPaint(x, y, x, y + h,
                    new float[]{0, 1}, new Color[]{c1, c2});
            g2.setPaint(grad);
        } else {
            g2.setColor(new Color(255, 255, 255, 180));
        }
        g2.fill(bg);

        // 蓝色边框
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(70, 150, 255, 200));
        g2.draw(bg);

        // 绘制pause.png精灵图：完全填满按钮
        if (pauseIcon != null) {
            g2.drawImage(pauseIcon, x, y, w, h, null);
        } else {
            // 备用：蓝色暂停符号
            g2.setColor(new Color(70, 150, 255));
            g2.fillRect(x + 18, y + 15, 8, 30);
            g2.fillRect(x + 34, y + 15, 8, 30);
        }
    }

    public void openPauseMenu() {
        gamePaused = true;

        JDialog pauseDialog = new JDialog(SwingUtilities.getWindowAncestor(mPanel), "Pause Menu", Dialog.ModalityType.APPLICATION_MODAL);
        pauseDialog.setSize(350, 380);
        pauseDialog.setLocationRelativeTo(mPanel);
        pauseDialog.setResizable(false);
        pauseDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 点击右上角X关闭后继续游戏
        pauseDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gamePaused = false;
                mPanel.repaint();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(240, 248, 255));

        JLabel title = new JLabel("PAUSED", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 28));
        title.setForeground(new Color(70, 150, 255));
        mainPanel.add(title, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 0, 20));
        btnPanel.setBackground(new Color(240, 248, 255));

        // 1. Resume
        JButton btnResume = StartMenu.createStyledButtonStatic("Resume", true, "begin.png");
        btnResume.addActionListener(e -> {
            gamePaused = false;
            pauseDialog.dispose();
        });

        // 2. How to play
        JButton btnHelp = StartMenu.createStyledButtonStatic("How to play", true, "help.png");
        btnHelp.addActionListener(e -> {
            JOptionPane.showMessageDialog(pauseDialog,
                    "游戏玩法：\n1. 点击/空格发射弹珠\n2. 3个同色相连即消除\n3. 不要让弹珠碰到底部红线",
                    "How to play", JOptionPane.INFORMATION_MESSAGE);
        });

        // 3. Sound on/off
        JButton btnSound = StartMenu.createStyledButtonStatic(StartMenu.isSoundOnStatic ? "Sound on" : "Sound off", true, "sound.png");
        btnSound.addActionListener(e -> {
            StartMenu.isSoundOnStatic = !StartMenu.isSoundOnStatic;
            btnSound.setText(StartMenu.isSoundOnStatic ? "Sound on" : "Sound off");
        });

        // 4. Quit：完全重置游戏并返回主菜单
        JButton btnQuit = StartMenu.createStyledButtonStatic("Quit", true, "exit.png");
        btnQuit.addActionListener(e -> {
            gamePaused = false;
            frozen = false;
            gameStarted = false;
            // 清空游戏对象，释放资源
            hexGrid = null;
            launchMarble = null;
            // 恢复默认光标
            mPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            pauseDialog.dispose();
            // 切换到主菜单
            cardLayout.show(this.mainPanel, "menu");
        });

        btnPanel.add(btnResume);
        btnPanel.add(btnHelp);
        btnPanel.add(btnSound);
        btnPanel.add(btnQuit);

        mainPanel.add(btnPanel, BorderLayout.CENTER);
        pauseDialog.setContentPane(mainPanel);
        pauseDialog.setVisible(true);

        pausePressed = false;
        mPanel.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // [Bug 4] 修复: 暂停按钮的UI状态需要在拦截前处理
        mouseX = e.getX();
        mouseY = e.getY();

        boolean oldHover = pauseHover;
        pauseHover = pauseButtonBounds.contains(e.getPoint());
        if (pauseHover) {
            mPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            mPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (oldHover != pauseHover)
            mPanel.repaint();

        // 完成视觉更新后，再做游戏逻辑的拦截
        if (frozen || gamePaused) return;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (frozen) return;

        if (pauseButtonBounds.contains(e.getPoint())) {
            pausePressed = true;
            mPanel.repaint();
            openPauseMenu();
            return;
        }

        if (gamePaused) return;
        if (launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        pausePressed = false;
        mPanel.repaint();
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen || gamePaused) return;
        // [Bug 12] 修复: 保持精确 double
        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(mouseX > 0 ? mouseX : launchPad.cannon.x,
                    mouseY > 0 ? mouseY : launchPad.cannon.y - 100);
        }
    }
}