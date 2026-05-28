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
import java.awt.geom.Point2D;

public class Main extends GameEngine implements ScreenStart.ScreenStartListener {
    private Marbles hexGrid;
    private ScreenGame launchPad;
    private MarbleLaunch launchMarble;
    private double mouseX = 0;
    private double mouseY = 0;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    public boolean gameStarted = false;
    private boolean frozen = false;
    private double deadline;
    private Random random = new Random();
    private ScreenStart startScreen;

    // 布局尺寸常量
    public static final int GAME_ZONE_WIDTH = 483;
    public static final int LEFT_ZONE_WIDTH = 250;
    public static final int TOTAL_WIDTH = LEFT_ZONE_WIDTH + GAME_ZONE_WIDTH;
    public static final int GAME_HEIGHT = 560;
    private static final double CANNON_MOVE_SPEED = 400;

    private boolean gamePaused = false;

    private boolean upPressed = false;
    private boolean downPressed = false;

    private boolean wasStarted = false;

    private int currentScore = 0;
    private int levelHighScore = 0;
    private int levelWinScore = 0;
    private boolean levelWon = false;
    private int highScore = 0;
    private CustomGlassPane glassPane;
    
    // Sans 角色状态
    private Sans sans;
    private boolean sansActive = false;
    private double sansX, sansY;
    private boolean sansAnimating = false;

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

            JPanel gameContainer = new JPanel(null);
            gameContainer.setOpaque(false);
            gameContainer.setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));

            game.mPanel.setBounds(LEFT_ZONE_WIDTH, 0, GAME_ZONE_WIDTH, GAME_HEIGHT);
            gameContainer.add(game.mPanel);

            mainPanel.add(startScreen, "menu");
            mainPanel.add(gameContainer, "game");

            frame.setContentPane(mainPanel);
            frame.pack();

            Insets insets = frame.getInsets();
            int targetWidth = TOTAL_WIDTH + insets.left + insets.right;
            int targetHeight = GAME_HEIGHT + insets.top + insets.bottom;
            frame.setSize(targetWidth, targetHeight);

            startScreen.setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.cardLayout = cardLayout;
            game.mainPanel = mainPanel;

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
        SoundManager.getInstance().playGameBegin();

        cardLayout.show(mainPanel, "game");
        currentScore = 0;

        boolean wasStartedLocal = gameStarted;
        gameStarted = true;

        if (!wasStartedLocal) {
            if (startScreen != null) {
                startScreen.stopAnimation();
            }
            init();
            gameLoop(60);
        } else {
            init();
        }

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
        }
        
        // 如果是第三关，则触发 Sans 彩蛋
        if (Level.getInstance().getCurrentLevel() == 3) {
            startSansIntro();
        }
    }

    @Override
    public void init() {
        Level level = Level.getInstance();
        levelWinScore = level.getWinScore();
        levelHighScore = level.getLevelHighScore();
        levelWon = false;

        // 初始化 Sans 角色并重置状态
        if (sans == null) {
            sans = new Sans();
        }
        sansActive = false;
        sansAnimating = false;

        initMarbleGrid();
        upPressed = false;
        downPressed = false;

        hexGrid.setScoreListener((marble, points) -> {
            currentScore += points;
            if (currentScore > levelHighScore) {
                levelHighScore = currentScore;
                level.updateLevelHighScore(levelHighScore);
            }
            if (!levelWon && level.isWinConditionMet(currentScore)) {
                levelWon = true;
            }
            if (glassPane != null) {
                glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
            }
        });
    }

    private void initMarbleGrid() {
        Level level = Level.getInstance();
        hexGrid = new Marbles();
        launchPad = new ScreenGame();
        hexGrid.setMaxRowCount(18);
        hexGrid.setFallSpeedMultiplier(level.getFallSpeedMultiplier());
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();

        launchMarble = new MarbleLaunch();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);
        launchPad.setNextMarbleColorType(random.nextInt(level.getColorTypeCount()) + 1);
    }

    @Override
    public void update(double dt) {
        if (frozen || gamePaused) return;
        if (hexGrid != null) hexGrid.update(dt, deadline);

        if (launchPad != null) {
            boolean moved = false;
            if (upPressed) {
                launchPad.cannon.y -= CANNON_MOVE_SPEED * dt;
                moved = true;
            }
            if (downPressed) {
                launchPad.cannon.y += CANNON_MOVE_SPEED * dt;
                moved = true;
            }

            if (moved) {
                if (launchPad.cannon.y < deadline) launchPad.cannon.y = deadline;
                if (launchPad.cannon.y > mHeight) launchPad.cannon.y = mHeight;

                if (launchMarble != null && !launchMarble.isLaunched()) {
                    launchMarble.setCenter(launchPad.cannon.x, launchPad.cannon.y);
                }
            }
        }

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

            int colorTypeCount = Level.getInstance().getColorTypeCount();
            launchPad.setNextMarbleColorType(random.nextInt(colorTypeCount) + 1);
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
                new Color[]{new Color(188, 195, 255), new Color(188, 195, 255)}
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, mWidth, mHeight);

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
        SoundManager.getInstance().playBackToMenu();
        if (glassPane != null) {
            glassPane.showOverlay(1, false);
        }
    }

    public void closePauseMenu() {
        SoundManager.getInstance().playBackToMenu();

        gamePaused = false;
        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        mPanel.repaint();
    }

    private void openScreenGameOverMenu(boolean win) {
        if (!win) {
            SoundManager.getInstance().playGameFail();
        }

        frozen = true;
        gamePaused = true;
        if (glassPane != null) {
            glassPane.showOverlay(2, win);
        }
    }

    private void returnToMenu() {
        SoundManager.getInstance().playBackToMenu();

        frozen = false;
        gamePaused = false;
        gameStarted = false;
        hexGrid = null;
        launchMarble = null;
        upPressed = false;
        downPressed = false;
        mPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        if (glassPane != null) {
            glassPane.hideOverlay();
        }

        cardLayout.show(mainPanel, "menu");

        if (startScreen != null) {
            startScreen.restartAnimation();
        }
    }

    public void onBackToMenu() {
        returnToMenu();
    }

    public void onRestart() {
        SoundManager.getInstance().playBackToMenu();

        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        frozen = false;
        gamePaused = false;
        currentScore = 0;
        upPressed = false;
        downPressed = false;
        init();
        if (glassPane != null) {
            glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
        }
        mPanel.repaint();

        // 第3关开始时，触发 Sans 彩蛋
        if (Level.getInstance().getCurrentLevel() == 3) {
            startSansIntro();
        }
    }

    /**
     * Sans出场动画处理：
     * 冻结游戏界面，让Sans从屏幕左侧外使用Right动作走入到左侧面板居中空白处，
     * 然后自动保持Down状态第一个动作，随即解除游戏界面冻结状态。
     */
    private void startSansIntro() {
        // 冻结游戏，暂停游戏逻辑
        frozen = true;
        gamePaused = true;
        sansActive = true;
        sansAnimating = true;

        // Sans 停止位置：窗口左边界和游戏区左边界的中心，减去一半站立图宽度
        int targetX = LEFT_ZONE_WIDTH / 2 - 47;
        int targetY = GAME_HEIGHT - 320;  // 计分板下方、暂停按钮上方的安全区域
        
        sansX = -80;  // 起始位置（屏幕左侧外）
        sansY = targetY;

        final double finalX = targetX;
        final double finalY = targetY;
        
        // 播放向右行走的动画
        sans.play("Basic - Right", 150);

        final long ANIM_DURATION = 2000;  // 2秒行走动画
        final long startTime = System.currentTimeMillis();

        javax.swing.Timer sansTimer = new javax.swing.Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double t = Math.min(1.0, (double) elapsed / ANIM_DURATION);

            sansX = -80 + (finalX - (-80)) * t;
            sansY = finalY;

            // 必须重绘 GlassPane 以显示动画
            if (glassPane != null) {
                glassPane.repaint();
            }

            // 动画到达目的地
            if (elapsed >= ANIM_DURATION) {
                ((javax.swing.Timer) e.getSource()).stop();
                sans.stopAnimation();
                sansAnimating = false;
                
                // 动画结束后，自动解除冻结继续游戏
                frozen = false;
                gamePaused = false;
                
                if (glassPane != null) {
                    glassPane.repaint();
                }
            }
        });
        sansTimer.start();
    }

    public void onNextLevel() {
        Level.getInstance().nextLevel();
        onRestart();
    }

    @Override
    public void onSelectLevel(int level) {
        SoundManager.getInstance().playGameBegin();
        Level.getInstance().setCurrentLevel(level);

        cardLayout.show(mainPanel, "game");
        currentScore = 0;

        wasStarted = gameStarted;
        gameStarted = true;

        if (!wasStarted) {
            if (startScreen != null) {
                startScreen.stopAnimation();
            }
            init();
            gameLoop(60);
        } else {
            init();
        }

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
        }
        
        // 如果选择的是第三关，触发 Sans 彩蛋
        if (level == 3) {
            startSansIntro();
        }
    }

    @Override
    public void onOpenSettings() {
        SoundManager.getInstance().playBackToMenu();
        if (glassPane != null) {
            glassPane.showSettings();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (frozen || gamePaused) return;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (frozen || gamePaused) return;
        if (launchMarble != null && !launchMarble.isLaunched()) {
            performLaunch(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen || gamePaused) return;

        if (event.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
        if (event.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;

        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            performLaunch(mouseX > 0 ? mouseX : launchPad.cannon.x,
                    mouseY > 0 ? mouseY : launchPad.cannon.y - 100);
        }
    }

    private void performLaunch(double targetX, double targetY) {
        launchPad.updateCannonAngle(targetX, targetY);
        launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
        Point2D.Double muzzle = launchPad.getMuzzlePosition();
        launchMarble.launch(muzzle.x, muzzle.y);

        if (hexGrid != null) {
            hexGrid.setLastLaunchPosition(muzzle.x, muzzle.y);
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
        if (event.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
    }

    private int loadHighScore() {
        return Preferences.userNodeForPackage(Main.class).getInt("highScore", 0);
    }

    private void saveHighScore(int score) {
        Preferences.userNodeForPackage(Main.class).putInt("highScore", score);
    }

    class CustomGlassPane extends JComponent {
        private Rectangle pauseButtonRect;
        private boolean pauseHover = false, pausePressed = false;
        private int overlayMode = 0;
        private boolean isScreenGameOverWin = false;

        private boolean animating = false;
        private long animStartTime = 0;
        private static final long ANIM_DURATION = 350;
        private static final double ANIM_OVERSHOOT = 0.12;
        private javax.swing.Timer animTimer;
        private int returnToMode = 0;

        public CustomGlassPane() {
            setOpaque(false);
            setFocusable(false);
            setLayout(null);

            animTimer = new javax.swing.Timer(16, e -> {
                if (animating) {
                    long elapsed = System.currentTimeMillis() - animStartTime;
                    if (elapsed >= ANIM_DURATION) {
                        animating = false;
                        animTimer.stop();
                    }
                    repaint();
                }
            });

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point p = e.getPoint();
                    if (overlayMode == 0) {
                        if (gameStarted && pauseButtonRect != null && pauseButtonRect.contains(p)) {
                            pausePressed = true;
                            repaint();
                            openPauseMenu();
                            pausePressed = false;
                            Point mp = getMousePosition();
                            pauseHover = (mp != null && pauseButtonRect.contains(mp));
                            repaint();
                        }
                    } else {
                        if (!animating) {
                            handleOverlayClick(p);
                        }
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
                        boolean newHover = gameStarted && pauseButtonRect != null && pauseButtonRect.contains(p);
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
                    showHelp();
                } else if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    closePauseMenu();
                    returnToMenu();
                }
            } else if (overlayMode == 2) {
                if (levelWon && lastRestartBtn != null && lastRestartBtn.contains(p)) {
                    onNextLevel();
                } else if (!isScreenGameOverWin && !levelWon && lastRestartBtn != null && lastRestartBtn.contains(p)) {
                    onRestart();
                } else if (lastMenuBtn != null && lastMenuBtn.contains(p)) {
                    onBackToMenu();
                }
            } else if (overlayMode == 3) {
                if (lastSettingsBtn != null && lastSettingsBtn.contains(p)) {
                    boolean oldState = SoundManager.getInstance().isSoundEnabled();
                    boolean newState = !oldState;
                    SoundManager.getInstance().setSoundEnabled(newState);
                    ScreenStart.isSoundOnStatic = newState;
                    repaint();
                    if (!oldState && newState) {
                        SoundManager.getInstance().playBackToMenu();
                    }
                } else if (lastHelpBtn != null && lastHelpBtn.contains(p)) {
                    showHelp();
                } else if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    closeSettings();
                }
            
            } else if (overlayMode == 4) {
                if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    hideHelp();
                }
            }
        }

        private void updateOverlayHover(Point p) {
            if (overlayMode != 0 && !animating) {
                boolean hoveringAny = false;
                if (overlayMode == 1) {
                    hoveringAny = (lastResumeBtn != null && lastResumeBtn.contains(p)) ||
                            (lastHelpBtn != null && lastHelpBtn.contains(p)) ||
                            (lastQuitBtn != null && lastQuitBtn.contains(p));
                } else if (overlayMode == 2) {
                    hoveringAny = ((levelWon || !isScreenGameOverWin) && lastRestartBtn != null && lastRestartBtn.contains(p)) ||
                            (lastMenuBtn != null && lastMenuBtn.contains(p));
                } else if (overlayMode == 3) {
                    hoveringAny = (lastSettingsBtn != null && lastSettingsBtn.contains(p)) ||
                            (lastHelpBtn != null && lastHelpBtn.contains(p)) ||
                            (lastQuitBtn != null && lastQuitBtn.contains(p));
                } else if (overlayMode == 4) {
                    hoveringAny = (lastQuitBtn != null && lastQuitBtn.contains(p));
                }
                setCursor(hoveringAny ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        }

        public void showOverlay(int mode, boolean win) {
            overlayMode = mode;
            isScreenGameOverWin = win;
            setVisible(true);
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void showSettings() {
            overlayMode = 3;
            isScreenGameOverWin = false;
            setVisible(true);
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void showHelp() {
            SoundManager.getInstance().playBackToMenu();
            returnToMode = overlayMode;
            overlayMode = 4;
            isScreenGameOverWin = false;
            setVisible(true);
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void hideHelp() {
            SoundManager.getInstance().playBackToMenu();
            overlayMode = returnToMode;
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void hideOverlay() {
            animating = false;
            if (animTimer != null) animTimer.stop();
            overlayMode = 0;

            if (gameStarted) {
            } else {
                setVisible(false);
            }
            repaint();
        }

        public void closeSettings() {
            SoundManager.getInstance().playBackToMenu();
            hideOverlay();
        }

        private int getCurrentOffsetY(int h) {
            if (!animating) return 0;
            long elapsed = System.currentTimeMillis() - animStartTime;
            double t = Math.min(1.0, (double) elapsed / ANIM_DURATION);
            if (t < 0.7) {
                t = t / 0.7;
                t = 1 - Math.pow(1 - t, 2);
                return (int)(-h * (1 - t));
            } else {
                t = (t - 0.7) / 0.3;
                t = 1 - Math.pow(1 - t, 3);
                return (int)(-h * ANIM_OVERSHOOT * (1 - t));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            if (gameStarted) {
                LinearGradientPaint leftBg = new LinearGradientPaint(
                        0, 0, LEFT_ZONE_WIDTH, 0,
                        new float[]{0f, 1f},
                        new Color[]{new Color(188, 195, 255), new Color(188, 195, 255)}
                );
                g2d.setPaint(leftBg);
                g2d.fillRect(0, 0, LEFT_ZONE_WIDTH, h);

                g2d.setColor(new Color(180, 205, 235));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawLine(LEFT_ZONE_WIDTH - 2, 0, LEFT_ZONE_WIDTH - 2, h);

                if (launchPad != null) {
                    launchPad.drawScoreBoard(g2d, LEFT_ZONE_WIDTH, h);
                }

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

                g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
                String btnText = "PAUSE";
                FontMetrics fm = g2d.getFontMetrics();
                int tx = btnX + (btnW - fm.stringWidth(btnText)) / 2;
                int ty = btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(btnText, tx, ty);
                
                // ==========================================
                // 绘制跨越面板的 Sans
                // ==========================================
                if (sansActive && sans != null) {
                    if (sansAnimating) {
                        // 播放右走动画
                        sans.draw(g2d, (int) sansX, (int) sansY, 1.2);
                    } else {
                        // 动画结束后锁定保持 Down 的第一帧 (frameIndex: 0)
                        sans.drawSpecificFrame(g2d, "Basic - Down", 0, (int) sansX, (int) sansY, 1.2);
                    }
                }
            }

            if (overlayMode != 0) {
                drawOverlayContent(g2d, w, h);
            }
        }

        private void drawOverlayContent(Graphics2D g2d, int w, int h) {
            g2d.setColor(new Color(0, 0, 0, 190));
            g2d.fillRect(0, 0, w, h);

            int offsetY = getCurrentOffsetY(h);
            g2d.translate(0, offsetY);

            int centerX = w / 2;
            int centerY = h / 2;

            if (overlayMode == 1) {
                drawPauseMenuOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 2) {
                drawScreenGameOverOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 3) {
                drawSettingsOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 4) {
                drawHelpOverlay(g2d, centerX, centerY);
            }

            g2d.translate(0, -offsetY);
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
            g2d.setFont(new Font("Arial Black", Font.BOLD, 42));
            if (levelWon) {
                g2d.setColor(new Color(255, 215, 0));
                String title = "LEVEL CLEAR!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);
            } else if (isScreenGameOverWin) {
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

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 26));
            g2d.setColor(Color.WHITE);
            String scoreText = "Score: " + currentScore;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, cx - fm.stringWidth(scoreText) / 2, cy - 70);

            String targetText = "Target: " + levelWinScore;
            fm = g2d.getFontMetrics();
            g2d.drawString(targetText, cx - fm.stringWidth(targetText) / 2, cy - 35);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy + 10;

            if (levelWon) {
                Rectangle nextLevelBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawOverlayButton(g2d, nextLevelBtn, "Next Level", new Color(70, 200, 100));
                lastRestartBtn = nextLevelBtn;
            } else if (!isScreenGameOverWin) {
                Rectangle restartBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawOverlayButton(g2d, restartBtn, "Restart", new Color(70, 150, 255));
                lastRestartBtn = restartBtn;
            }

            int menuOffset = (levelWon || isScreenGameOverWin) ? (levelWon ? btnHeight + btnSpacing : 0) : btnHeight + btnSpacing;
            Rectangle menuBtn = new Rectangle(cx - btnWidth / 2, startY + menuOffset, btnWidth, btnHeight);
            drawOverlayButton(g2d, menuBtn, "Main Menu", new Color(100, 190, 255));
            lastMenuBtn = menuBtn;
        }

        private void drawSettingsOverlay(Graphics2D g2d, int cx, int cy) {
            g2d.setFont(new Font("Arial Black", Font.BOLD, 36));
            g2d.setColor(new Color(70, 150, 255));
            String title = "SETTINGS";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy - 20;

            Rectangle soundBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
            String soundText = SoundManager.getInstance().isSoundEnabled() ? "Sound: ON" : "Sound: OFF";
            drawOverlayButton(g2d, soundBtn, soundText, new Color(70, 150, 255));
            lastSettingsBtn = soundBtn;

            Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
            drawOverlayButton(g2d, helpBtn, "How to play", new Color(100, 190, 255));
            lastHelpBtn = helpBtn;

            Rectangle backBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, backBtn, "Back", new Color(220, 70, 70));
            lastQuitBtn = backBtn;
        }

        private void drawHelpOverlay(Graphics2D g2d, int cx, int cy) {
            String[] lines = {
                    "1. Move mouse to aim, click or press SPACE to launch marble",
                    "2. 3 or more same-color connected marbles will be eliminated",
                    "3. Do not let marbles cross the bottom dashed line!"
            };

            g2d.setFont(new Font("Arial Black", Font.BOLD, 36));
            g2d.setColor(new Color(70, 150, 255));
            String title = "HOW TO PLAY";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
            g2d.setColor(Color.WHITE);
            int lineHeight = 36;
            int startY = cy - 20;
            for (int i = 0; i < lines.length; i++) {
                fm = g2d.getFontMetrics();
                g2d.drawString(lines[i], cx - fm.stringWidth(lines[i]) / 2, startY + i * lineHeight);
            }

            int btnWidth = 220;
            int btnHeight = 55;
            int btnY = startY + lines.length * lineHeight + 30;
            Rectangle backBtn = new Rectangle(cx - btnWidth / 2, btnY, btnWidth, btnHeight);
            drawOverlayButton(g2d, backBtn, "Back", new Color(220, 70, 70));
            lastQuitBtn = backBtn;
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
        private Rectangle lastSettingsBtn = null;

        @Override
        public boolean contains(int x, int y) {
            if (animating) return true;

            if (overlayMode == 0) {
                return gameStarted && pauseButtonRect != null && pauseButtonRect.contains(x, y);
            }

            return true;
        }

        public void updateScores(int score, int high, int levelHigh, int levelWin) {
            if (launchPad != null) {
                launchPad.updateScore(score);
                launchPad.updateHighScore(high);
                launchPad.updateLevelScores(score, levelHigh, levelWin);
                mPanel.repaint();
            }
            repaint();
        }
    }
}