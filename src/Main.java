import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
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
import java.util.prefs.Preferences;

public class Main extends GameEngine implements ScreenStart.ScreenStartListener, ScreenGameOver.ScreenGameOverListener {
    private Marbles hexGrid;
    private LaunchPad launchPad;
    private MarbleLaunch launchMarble;
    private double mouseX = 0;
    private double mouseY = 0;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private boolean gameStarted = false;
    private boolean frozen = false;
    private double deadline;
    private Random random = new Random();
    private ScreenStart startScreen; // 保存菜单引用用于销毁 Timer

    // 窗口尺寸
    private static final int WINDOW_WIDTH = 1080;

    // 暂停按钮相关
    private BufferedImage pauseIcon;
    private boolean gamePaused = false;

    // 计分相关
    private int currentScore = 0;
    private int highScore = 0;
    private BoardScore scoreBoard;
    private CustomGlassPane glassPane;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("弹珠游戏");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            Main game = new Main(frame);

            ScreenStart startScreen = new ScreenStart(game);
            game.startScreen = startScreen;

            // 创建居中容器
            JPanel gameContainer = new JPanel(new GridBagLayout());
            gameContainer.setOpaque(false);
            gameContainer.add(game.mPanel, new GridBagConstraints());

            // 开始界面直接填充窗口
            mainPanel.add(startScreen, "menu");
            mainPanel.add(gameContainer, "game");

            frame.setContentPane(mainPanel);
            frame.pack();

            // 设置窗口为游戏界面宽度的两倍，高度适应游戏界面
            Insets insets = frame.getInsets();
            int targetWidth = game.mWidth * 2;
            int targetHeight = game.mHeight + insets.top + insets.bottom;
            frame.setSize(targetWidth, targetHeight);

            // 开始界面填充整个窗口（在窗口大小设置之后）
            startScreen.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.cardLayout = cardLayout;
            game.mainPanel = mainPanel;

            // 初始化自定义玻璃面板和计分板
            game.glassPane = game.new CustomGlassPane();
            frame.setGlassPane(game.glassPane);
            game.glassPane.setVisible(false);
        });
    }

    public Main(JFrame frame) {
        mFrame = frame;
        mPanel = new GamePanel();
        mWidth = 483;
        mHeight = 483; // 游戏界面保持原有尺寸

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
        // 加载最高分
        highScore = loadHighScore();
    }

    @Override
    public void onStartGame() {
        cardLayout.show(mainPanel, "game");

        // 重置分数
        currentScore = 0;

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.updateScores(currentScore, highScore);
        }

        if (!gameStarted) {
            gameStarted = true;
            // 销毁菜单里的后台 Timer
            if (startScreen != null) {
                startScreen.stopAnimation();
            }
            init();
            gameLoop(60);
        }
    }

    @Override
    public void init() {
        initMarbleGrid();

        // 设置得分监听器
        hexGrid.setScoreListener((marble, points) -> {
            currentScore += points;
            if (currentScore > highScore) {
                highScore = currentScore;
                saveHighScore(highScore);
            }
            if (glassPane != null) {
                glassPane.updateScores(currentScore, highScore);
            }
        });
    }

    private void initMarbleGrid() {
        hexGrid = new Marbles();
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
        hexGrid.setMaxRowCount(18);
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();

        // 初始化：生成第一个当前弹珠和第一个下一个弹珠
        launchMarble = new MarbleLaunch();
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

        // 加大碰撞检测分步密度，减少穿模
        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / (radius * 0.25));
        if (steps < 1) steps = 1;

        boolean collided = false;

        for (int i = 1; i <= steps; i++) {
            double checkX = prevX + dx * i / steps;
            double checkY = prevY + dy * i / steps;

            // 触顶判断改为真正的 radius 而不是 radius * 2
            if (checkY <= radius) {
                collided = true;
                launchMarble.setCenter(checkX, checkY);
                break;
            }

            for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
                Marble[] row = hexGrid.getRow(r);
                if (row == null) continue;
                for (Marble m : row) {
                    // 忽略正在播放消除或掉落动画的弹珠
                    if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling()) {
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

            // 在 init 时提前赋予下一次正确的颜色类型
            int nextColor = launchPad.getNextMarbleColorType();
            launchMarble = new MarbleLaunch();
            launchMarble.setScreenSize(mWidth, mHeight);
            launchMarble.setColorType(nextColor);
            launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);

            // 随机生成新的下一个弹珠颜色，更新预览区
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
                // 忽略正在播放消除或掉落动画的弹珠，防止掉落期间触发判定
                if (marble != null && marble.isInitialized() && !marble.isPopping() && !marble.isFalling() && marble.getCenterY() + radius >= deadline) {
                    openScreenGameOverMenu(false);
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
    }

    public void openPauseMenu() {
        gamePaused = true;
        if (glassPane != null) {
            glassPane.showOverlay(1, false);
        }
    }

    public void closePauseMenu() {
        gamePaused = false;
        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        mPanel.repaint();
    }

    // ==================== ScreenGameOver 界面 ====================
    private void openScreenGameOverMenu(boolean win) {
        frozen = true;
        gamePaused = true;

        if (glassPane != null) {
            glassPane.showOverlay(2, win);
        }
    }

    private void returnToMenu() {
        frozen = false;
        gamePaused = false;
        gameStarted = false;

        // 清空游戏对象，释放资源
        hexGrid = null;
        launchMarble = null;

        // 恢复默认光标
        mPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        // 切换到主菜单
        cardLayout.show(mainPanel, "menu");
    }

    @Override
    public void onBackToMenu() {
        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        returnToMenu();
    }

    @Override
    public void onRestart() {
        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        // 重置游戏
        frozen = false;
        gamePaused = false;
        currentScore = 0;

        if (glassPane != null) {
            glassPane.updateScores(currentScore, highScore);
        }

        // 重新初始化游戏
        init();
        mPanel.repaint();
    }

    @Override
    public void onNextLevel() {
        // 目前没有关卡系统，和Restart一样
        onRestart();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        // 完成视觉更新后，再做游戏逻辑的拦截
        if (frozen || gamePaused) return;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (frozen) return;
        if (gamePaused) return;
        if (launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen || gamePaused) return;
        // 保持精确 double
        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(mouseX > 0 ? mouseX : launchPad.cannon.x,
                    mouseY > 0 ? mouseY : launchPad.cannon.y - 100);
        }
    }

    // ==================== 得分相关 ====================
    private int loadHighScore() {
        return Preferences.userNodeForPackage(Main.class).getInt("highScore", 0);
    }

    private void saveHighScore(int score) {
        Preferences.userNodeForPackage(Main.class).putInt("highScore", score);
    }

    // ==================== 自定义玻璃面板 ====================
    class CustomGlassPane extends JComponent {
        private Rectangle pauseButtonRect;
        private boolean pauseHover = false, pausePressed = false;

        // 覆盖层模式: 0=无, 1=暂停菜单, 2=游戏结束
        private int overlayMode = 0;
        private boolean isScreenGameOverWin = false;

        public CustomGlassPane() {
            setOpaque(false);
            setFocusable(false);
            setLayout(null);

            // 添加计分板
            scoreBoard = new BoardScore();
            add(scoreBoard);

            // 鼠标事件处理
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (overlayMode == 0 && pauseButtonRect != null && pauseButtonRect.contains(e.getPoint())) {
                        pausePressed = true;
                        repaint();
                        openPauseMenu();
                        pausePressed = false;
                        Point mp = getMousePosition();
                        pauseHover = (mp != null && pauseButtonRect.contains(mp));
                        repaint();
                    } else if (overlayMode != 0) {
                        // 处理覆盖层按钮点击
                        handleOverlayClick(e.getPoint());
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    pauseHover = false;
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (overlayMode == 0) {
                        boolean newHover = pauseButtonRect != null && pauseButtonRect.contains(e.getPoint());
                        if (pauseHover != newHover) {
                            pauseHover = newHover;
                            repaint();
                            setCursor(pauseHover ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        }
                    } else {
                        // 覆盖层模式下检测按钮悬停
                        updateOverlayHover(e.getPoint());
                    }
                }
            });
        }

        private void handleOverlayClick(Point p) {
            if (overlayMode == 1) {
                // 暂停菜单
                if (lastResumeBtn != null && lastResumeBtn.contains(p)) {
                    closePauseMenu();
                } else if (lastHelpBtn != null && lastHelpBtn.contains(p)) {
                    JOptionPane.showMessageDialog(mPanel,
                            "游戏玩法：\n1. 点击/空格发射弹珠\n2. 3个同色相连即消除\n3. 不要让弹珠碰到底部红线",
                            "How to play", JOptionPane.INFORMATION_MESSAGE);
                } else if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    closePauseMenu();
                    returnToMenu();
                }
            } else if (overlayMode == 2) {
                // 游戏结束
                if (!isScreenGameOverWin && lastRestartBtn != null && lastRestartBtn.contains(p)) {
                    onRestart();
                } else if (lastMenuBtn != null && lastMenuBtn.contains(p)) {
                    onBackToMenu();
                }
            }
        }

        private void updateOverlayHover(Point p) {
            repaint();
        }

        public void showOverlay(int mode, boolean win) {
            overlayMode = mode;
            isScreenGameOverWin = win;
            setVisible(true);
            repaint();
        }

        public void hideOverlay() {
            overlayMode = 0;
            setVisible(false);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            // 绘制暂停按钮（仅在非覆盖层模式时）
            if (overlayMode == 0) {
                int btnSize = 55;
                int btnX = 85;
                int btnY = h - btnSize - 15;
                pauseButtonRect = new Rectangle(btnX, btnY, btnSize, btnSize);

                if (pausePressed) {
                    Color c1 = new Color(50, 140, 255, 200);
                    Color c2 = new Color(30, 110, 255, 200);
                    LinearGradientPaint grad = new LinearGradientPaint(btnX, btnY, btnX, btnY+btnSize, new float[]{0,1}, new Color[]{c1,c2});
                    g2d.setPaint(grad);
                    g2d.fillRoundRect(btnX, btnY, btnSize, btnSize, 12, 12);
                } else if (pauseHover) {
                    Color c1 = new Color(100, 190, 255, 200);
                    Color c2 = new Color(50, 140, 255, 200);
                    LinearGradientPaint grad = new LinearGradientPaint(btnX, btnY, btnX, btnY+btnSize, new float[]{0,1}, new Color[]{c1,c2});
                    g2d.setPaint(grad);
                    g2d.fillRoundRect(btnX, btnY, btnSize, btnSize, 12, 12);
                } else {
                    g2d.setColor(new Color(255, 255, 255, 200));
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawRoundRect(btnX, btnY, btnSize, btnSize, 12, 12);
                }

                if (pauseIcon != null) {
                    g2d.drawImage(pauseIcon, btnX, btnY, btnSize, btnSize, null);
                } else {
                    g2d.setColor(new Color(70, 150, 255));
                    g2d.fillRect(btnX+16, btnY+14, 7, 27);
                    g2d.fillRect(btnX+32, btnY+14, 7, 27);
                }
            } else {
                // 绘制覆盖层内容
                drawOverlayContent(g2d, w, h);
            }
        }

        private void drawOverlayContent(Graphics2D g2d, int w, int h) {
            // 半透明背景
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, w, h);

            int centerX = w / 2;
            int centerY = h / 2;

            if (overlayMode == 1) {
                // 暂停菜单
                drawPauseMenuOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 2) {
                // 游戏结束
                drawScreenGameOverOverlay(g2d, centerX, centerY);
            }
        }

        private void drawPauseMenuOverlay(Graphics2D g2d, int cx, int cy) {
            // 标题
            g2d.setFont(new Font("Arial Black", Font.BOLD, 36));
            g2d.setColor(new Color(70, 150, 255));
            String title = "PAUSED";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

            // 按钮区域
            int btnWidth = 200;
            int btnHeight = 50;
            int btnSpacing = 15;
            int startY = cy - 50;

            // Resume按钮
            Rectangle resumeBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
            drawOverlayButton(g2d, resumeBtn, "Resume", new Color(70, 150, 255));

            // How to play按钮
            Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
            drawOverlayButton(g2d, helpBtn, "How to play", new Color(100, 190, 255));

            // Quit按钮
            Rectangle quitBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, quitBtn, "Quit", new Color(200, 60, 60));

            // 保存按钮引用用于点击检测
            lastResumeBtn = resumeBtn;
            lastHelpBtn = helpBtn;
            lastQuitBtn = quitBtn;
        }

        private void drawScreenGameOverOverlay(Graphics2D g2d, int cx, int cy) {
            // 标题
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            if (isScreenGameOverWin) {
                g2d.setColor(new Color(255, 220, 80));
                String title = "LEVEL COMPLETE!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 120);
            } else {
                g2d.setColor(new Color(255, 80, 80));
                String title = "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 120);
            }

            // 分数
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.setColor(Color.WHITE);
            String scoreText = "Score: " + currentScore;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, cx - fm.stringWidth(scoreText) / 2, cy - 60);

            // 按钮区域
            int btnWidth = 200;
            int btnHeight = 50;
            int btnSpacing = 15;
            int startY = cy - 20;

            if (!isScreenGameOverWin) {
                // Restart按钮
                Rectangle restartBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawOverlayButton(g2d, restartBtn, "Restart", new Color(70, 150, 255));
                lastRestartBtn = restartBtn;
            }

            // Main Menu按钮
            Rectangle menuBtn = new Rectangle(cx - btnWidth / 2, startY + (isScreenGameOverWin ? 0 : btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, menuBtn, "Main Menu", new Color(100, 190, 255));
            lastMenuBtn = menuBtn;
        }

        private void drawOverlayButton(Graphics2D g2d, Rectangle rect, String text, Color baseColor) {
            // 按钮背景
            LinearGradientPaint grad = new LinearGradientPaint(
                rect.x, rect.y, rect.x, rect.y + rect.height,
                new float[]{0, 1},
                new Color[]{baseColor, baseColor.darker()}
            );
            g2d.setPaint(grad);
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 15, 15);

            // 按钮文字
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
            int textY = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        }

        // 覆盖层按钮引用
        private Rectangle lastResumeBtn = null;
        private Rectangle lastHelpBtn = null;
        private Rectangle lastQuitBtn = null;
        private Rectangle lastRestartBtn = null;
        private Rectangle lastMenuBtn = null;

        public Rectangle getLastResumeBtn() { return lastResumeBtn; }
        public Rectangle getLastHelpBtn() { return lastHelpBtn; }
        public Rectangle getLastQuitBtn() { return lastQuitBtn; }
        public Rectangle getLastRestartBtn() { return lastRestartBtn; }
        public Rectangle getLastMenuBtn() { return lastMenuBtn; }

        @Override
        public boolean contains(int x, int y) {
            if (overlayMode == 0) {
                return pauseButtonRect != null && pauseButtonRect.contains(x, y);
            }
            // 检查覆盖层按钮点击
            if (overlayMode == 1) {
                if (lastResumeBtn != null && lastResumeBtn.contains(x, y)) return true;
                if (lastHelpBtn != null && lastHelpBtn.contains(x, y)) return true;
                if (lastQuitBtn != null && lastQuitBtn.contains(x, y)) return true;
            } else if (overlayMode == 2) {
                if (!isScreenGameOverWin && lastRestartBtn != null && lastRestartBtn.contains(x, y)) return true;
                if (lastMenuBtn != null && lastMenuBtn.contains(x, y)) return true;
            }
            return false;
        }

        public void updateScores(int score, int high) {
            if (scoreBoard != null) {
                scoreBoard.updateScore(score);
                scoreBoard.updateHighScore(high);
            }
        }

        public void resetScore() {
            if (scoreBoard != null) {
                scoreBoard.updateScore(0);
            }
        }

        public void stopBoardScoreAnimation() {
            if (scoreBoard != null) scoreBoard.stopAnimation();
        }

        public void resetHoverState() {
            pauseHover = false;
            pausePressed = false;
            repaint();
        }
    }
}