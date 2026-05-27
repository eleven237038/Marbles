import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
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
    private ScreenStart startScreen;

    // 布局尺寸常量
    public static final int GAME_ZONE_WIDTH = 483;  // 游戏区域宽度 (右侧)
    public static final int LEFT_ZONE_WIDTH = 250;  // 左侧空白区域宽度 (放置计分板、控制区)
    public static final int TOTAL_WIDTH = LEFT_ZONE_WIDTH + GAME_ZONE_WIDTH; // 窗口总宽度: 733像素
    public static final int GAME_HEIGHT = 560;      // 游戏区域高度 (适当拉高，留出底部操作空间)

    private boolean gamePaused = false;

    // 计分相关
    private int currentScore = 0;
    private int highScore = 0;
    private BoardScore scoreBoard;
    private CustomGlassPane glassPane;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("弹珠游戏 - 豪华版");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            Main game = new Main(frame);

            ScreenStart startScreen = new ScreenStart(game);
            game.startScreen = startScreen;

            // 游戏主面板包裹容器 (靠右对齐或精确控制)
            JPanel gameContainer = new JPanel(null);
            gameContainer.setOpaque(false);
            gameContainer.setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));
            
            // 游戏面板放置在最右侧
            game.mPanel.setBounds(LEFT_ZONE_WIDTH, 0, GAME_ZONE_WIDTH, GAME_HEIGHT);
            gameContainer.add(game.mPanel);

            mainPanel.add(startScreen, "menu");
            mainPanel.add(gameContainer, "game");

            frame.setContentPane(mainPanel);
            frame.pack();

            // 调整窗口尺寸：无缝贴合
            Insets insets = frame.getInsets();
            int targetWidth = TOTAL_WIDTH + insets.left + insets.right;
            int targetHeight = GAME_HEIGHT + insets.top + insets.bottom;
            frame.setSize(targetWidth, targetHeight);

            startScreen.setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));

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
        mWidth = GAME_ZONE_WIDTH;
        mHeight = GAME_HEIGHT;

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
        highScore = loadHighScore();
    }

    @Override
    public void onStartGame() {
        cardLayout.show(mainPanel, "game");
        currentScore = 0;

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.updateScores(currentScore, highScore);
        }

        if (!gameStarted) {
            gameStarted = true;
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

        launchMarble = new MarbleLaunch();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);
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

        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / (radius * 0.25));
        if (steps < 1) steps = 1;

        boolean collided = false;

        for (int i = 1; i <= steps; i++) {
            double checkX = prevX + dx * i / steps;
            double checkY = prevY + dy * i / steps;

            if (checkY <= radius) {
                collided = true;
                launchMarble.setCenter(checkX, checkY);
                break;
            }

            for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
                Marble[] row = hexGrid.getRow(r);
                if (row == null) continue;
                for (Marble m : row) {
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

            int nextColor = launchPad.getNextMarbleColorType();
            launchMarble = new MarbleLaunch();
            launchMarble.setScreenSize(mWidth, mHeight);
            launchMarble.setColorType(nextColor);
            launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);

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
                if (marble != null && marble.isInitialized() && !marble.isPopping() && !marble.isFalling() && marble.getCenterY() + radius >= deadline) {
                    openScreenGameOverMenu(false);
                    return;
                }
            }
        }
    }

    @Override
    public void paintComponent() {
        Graphics2D g2 = (Graphics2D) mGraphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LinearGradientPaint bg = new LinearGradientPaint(
                0, 0, 0, mHeight,
                new float[]{0, 1},
                new Color[]{new Color(245, 250, 255), new Color(210, 230, 255)}
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, mWidth, mHeight);

        // 绘制一条分割左侧空白区和游戏区的精致分界竖线
        g2.setColor(new Color(200, 220, 240, 180));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(0, 0, 0, mHeight);

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
        hexGrid = null;
        launchMarble = null;
        mPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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
        frozen = false;
        gamePaused = false;
        currentScore = 0;
        if (glassPane != null) {
            glassPane.updateScores(currentScore, highScore);
        }
        init();
        mPanel.repaint();
    }

    @Override
    public void onNextLevel() {
        onRestart();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // 鼠标移至右侧游戏面板内，需要转换坐标差
        mouseX = e.getX();
        mouseY = e.getY();
        if (frozen || gamePaused) return;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (frozen || gamePaused) return;
        if (launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen || gamePaused) return;
        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(mouseX > 0 ? mouseX : launchPad.cannon.x,
                    mouseY > 0 ? mouseY : launchPad.cannon.y - 100);
        }
    }

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
        private int overlayMode = 0;
        private boolean isScreenGameOverWin = false;

        public CustomGlassPane() {
            setOpaque(false);
            setFocusable(false);
            setLayout(null);

            // 将计分板精确定位在左侧空白区中
            scoreBoard = new BoardScore();
            add(scoreBoard);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point p = e.getPoint();
                    if (overlayMode == 0 && pauseButtonRect != null && pauseButtonRect.contains(p)) {
                        pausePressed = true;
                        repaint();
                        openPauseMenu();
                        pausePressed = false;
                        Point mp = getMousePosition();
                        pauseHover = (mp != null && pauseButtonRect.contains(mp));
                        repaint();
                    } else if (overlayMode != 0) {
                        handleOverlayClick(p);
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
                    Point p = e.getPoint();
                    if (overlayMode == 0) {
                        boolean newHover = pauseButtonRect != null && pauseButtonRect.contains(p);
                        if (pauseHover != newHover) {
                            pauseHover = newHover;
                            repaint();
                            setCursor(pauseHover ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        }
                    } else {
                        updateOverlayHover(p);
                    }
                }
            });
        }

        private void handleOverlayClick(Point p) {
            if (overlayMode == 1) {
                if (lastResumeBtn != null && lastResumeBtn.contains(p)) {
                    closePauseMenu();
                } else if (lastHelpBtn != null && lastHelpBtn.contains(p)) {
                    JOptionPane.showMessageDialog(mPanel,
                            "游戏玩法：\n1. 移动鼠标瞄准，点击/按空格键发射弹珠。\n2. 3个或更多同色相连即可消除。\n3. 不要让弹珠越过底部虚线！",
                            "游戏指南", JOptionPane.INFORMATION_MESSAGE);
                } else if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    closePauseMenu();
                    returnToMenu();
                }
            } else if (overlayMode == 2) {
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

            // 绘制左侧区域的渐变底色
            LinearGradientPaint leftBg = new LinearGradientPaint(
                    0, 0, LEFT_ZONE_WIDTH, 0,
                    new float[]{0f, 1f},
                    new Color[]{new Color(235, 243, 255), new Color(215, 230, 250)}
            );
            g2d.setPaint(leftBg);
            g2d.fillRect(0, 0, LEFT_ZONE_WIDTH, h);

            // 绘制左侧装饰分隔线条
            g2d.setColor(new Color(180, 205, 235));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawLine(LEFT_ZONE_WIDTH - 2, 0, LEFT_ZONE_WIDTH - 2, h);

            // 绘制暂停按钮 (放置在左侧空白区下方)
            if (overlayMode == 0) {
                int btnW = 180;
                int btnH = 55;
                int btnX = (LEFT_ZONE_WIDTH - btnW) / 2;
                int btnY = h - btnH - 30;
                pauseButtonRect = new Rectangle(btnX, btnY, btnW, btnH);

                RoundRectangle2D btnShape = new RoundRectangle2D.Double(btnX, btnY, btnW, btnH, 18, 18);
                if (pausePressed) {
                    g2d.setPaint(new LinearGradientPaint(btnX, btnY, btnX, btnY + btnH, new float[]{0, 1},
                            new Color[]{new Color(50, 130, 240, 230), new Color(30, 100, 220, 230)}));
                } else if (pauseHover) {
                    g2d.setPaint(new LinearGradientPaint(btnX, btnY, btnX, btnY + btnH, new float[]{0, 1},
                            new Color[]{new Color(100, 180, 255, 230), new Color(50, 130, 240, 230)}));
                } else {
                    g2d.setPaint(new LinearGradientPaint(btnX, btnY, btnX, btnY + btnH, new float[]{0, 1},
                            new Color[]{new Color(120, 190, 255, 180), new Color(70, 140, 240, 180)}));
                }
                g2d.fill(btnShape);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2f));
                g2d.draw(btnShape);

                // 绘制按钮文字
                g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
                String btnText = "PAUSE";
                FontMetrics fm = g2d.getFontMetrics();
                int tx = btnX + (btnW - fm.stringWidth(btnText)) / 2;
                int ty = btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(btnText, tx, ty);
            } else {
                drawOverlayContent(g2d, w, h);
            }
        }

        private void drawOverlayContent(Graphics2D g2d, int w, int h) {
            g2d.setColor(new Color(0, 0, 0, 190));
            g2d.fillRect(0, 0, w, h);

            int centerX = w / 2;
            int centerY = h / 2;

            if (overlayMode == 1) {
                drawPauseMenuOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 2) {
                drawScreenGameOverOverlay(g2d, centerX, centerY);
            }
        }

        private void drawPauseMenuOverlay(Graphics2D g2d, int cx, int cy) {
            g2d.setFont(new Font("Arial Black", Font.BOLD, 42));
            g2d.setColor(new Color(70, 150, 255));
            String title = "PAUSED";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 120);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy - 40;

            Rectangle resumeBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
            drawOverlayButton(g2d, resumeBtn, "Resume", new Color(70, 150, 255));

            Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
            drawOverlayButton(g2d, helpBtn, "How to play", new Color(100, 190, 255));

            Rectangle quitBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, quitBtn, "Quit Game", new Color(220, 70, 70));

            lastResumeBtn = resumeBtn;
            lastHelpBtn = helpBtn;
            lastQuitBtn = quitBtn;
        }

        private void drawScreenGameOverOverlay(Graphics2D g2d, int cx, int cy) {
            g2d.setFont(new Font("Arial Black", Font.BOLD, 48));
            if (isScreenGameOverWin) {
                g2d.setColor(new Color(255, 215, 0));
                String title = "VICTORY!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);
            } else {
                g2d.setColor(new Color(255, 80, 80));
                String title = "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);
            }

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 28));
            g2d.setColor(Color.WHITE);
            String scoreText = "Score: " + currentScore;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, cx - fm.stringWidth(scoreText) / 2, cy - 60);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy;

            if (!isScreenGameOverWin) {
                Rectangle restartBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawOverlayButton(g2d, restartBtn, "Restart", new Color(70, 150, 255));
                lastRestartBtn = restartBtn;
            }

            Rectangle menuBtn = new Rectangle(cx - btnWidth / 2, startY + (isScreenGameOverWin ? 0 : btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, menuBtn, "Main Menu", new Color(100, 190, 255));
            lastMenuBtn = menuBtn;
        }

        private void drawOverlayButton(Graphics2D g2d, Rectangle rect, String text, Color baseColor) {
            LinearGradientPaint grad = new LinearGradientPaint(
                rect.x, rect.y, rect.x, rect.y + rect.height,
                new float[]{0, 1},
                new Color[]{baseColor, baseColor.darker()}
            );
            g2d.setPaint(grad);
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 18, 18);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
            int textY = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        }

        private Rectangle lastResumeBtn = null;
        private Rectangle lastHelpBtn = null;
        private Rectangle lastQuitBtn = null;
        private Rectangle lastRestartBtn = null;
        private Rectangle lastMenuBtn = null;

        @Override
        public boolean contains(int x, int y) {
            if (overlayMode == 0) {
                return pauseButtonRect != null && pauseButtonRect.contains(x, y);
            }
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
    }
}