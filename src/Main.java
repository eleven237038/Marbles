import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.LinearGradientPaint;
import javax.swing.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

public class Main extends GameEngine implements StartScreen.StartScreenListener {
    private Marbles hexGrid;
    private LaunchPad launchPad;
    private LaunchMarble launchMarble;
    private double mouseX = 0, mouseY = 0;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private boolean gameStarted = false, frozen = false, gamePaused = false;
    private double deadline;
    private Random random = new Random();
    private StartScreen startScreen;

    private static final int WINDOW_WIDTH = 1080;
    private static final int DEADLINE_UP_OFFSET = 60;

    // 原来的浅蓝色渐变背景色
    private static final Color BG_COLOR_TOP = new Color(230, 245, 255);
    private static final Color BG_COLOR_BOTTOM = new Color(190, 225, 255);

    private BufferedImage pauseIcon;
    private int currentScore = 0, highScore = 0, roundScore = 0;
    private boolean roundEnded = true;
    private long roundEndTime = 0;
    private List<FloatingScore> floatingScores = new ArrayList<>();

    private CustomGlassPane glassPane;

    class FloatingScore {
        double x, y;
        int score;
        float alpha = 1.0f;
        double life = 1.0;
        public FloatingScore(double x, double y, int score) {
            this.x = x; this.y = y; this.score = score;
        }
        public boolean update(double dt) {
            life -= dt;
            alpha = (float)(life / 1.0);
            y -= 25 * dt;
            return life <= 0;
        }
    }

    // 自定义玻璃面板（包含计分板和暂停按钮）
    class CustomGlassPane extends JComponent {
        private Rectangle pauseButtonRect;
        private boolean pauseHover = false, pausePressed = false;
        private ScoreBoard scoreBoard;

        public CustomGlassPane() {
            setOpaque(false);
            setFocusable(false);
            setLayout(null);

            // 添加独立的计分板组件 - 让它自己管理位置
            scoreBoard = new ScoreBoard();
            add(scoreBoard);

            // 鼠标事件处理（暂停按钮）
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (pauseButtonRect != null && pauseButtonRect.contains(e.getPoint())) {
                        pausePressed = true;
                        repaint();
                        openPauseMenu();
                        pausePressed = false;
                        Point mp = getMousePosition();
                        pauseHover = (mp != null && pauseButtonRect.contains(mp));
                        repaint();
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
                    boolean newHover = pauseButtonRect != null && pauseButtonRect.contains(e.getPoint());
                    if (pauseHover != newHover) {
                        pauseHover = newHover;
                        repaint();
                        setCursor(pauseHover ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    }
                }
            });
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
        }

        public void updateScores(int score, int high) {
            scoreBoard.updateScore(score);
            scoreBoard.updateHighScore(high);
        }

        public void resetScore() {
            scoreBoard.updateScore(0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            // 暂停按钮（左下角）
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
        }

        @Override
        public boolean contains(int x, int y) {
            return pauseButtonRect != null && pauseButtonRect.contains(x, y);
        }

        public void resetHoverState() {
            pauseHover = false;
            pausePressed = false;
            repaint();
        }

        public void stopScoreBoardAnimation() {
            if (scoreBoard != null) scoreBoard.stopAnimation();
        }
    }

    // ==================== Main 核心 ====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("弹珠游戏");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);
            Main game = new Main(frame);
            StartScreen startScreen = new StartScreen(game);
            game.startScreen = startScreen;

            // 游戏容器面板，使用与游戏画面相同的背景色
            JPanel gameContainer = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // 使用与原游戏画面相同的浅蓝色渐变背景
                    LinearGradientPaint bg = new LinearGradientPaint(
                            0, 0, 0, getHeight(),
                            new float[]{0, 1},
                            new Color[]{BG_COLOR_TOP, BG_COLOR_BOTTOM}
                    );
                    g2d.setPaint(bg);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            gameContainer.setOpaque(false);
            gameContainer.add(game.mPanel, new GridBagConstraints());
            mainPanel.add(startScreen, "menu");
            mainPanel.add(gameContainer, "game");
            frame.setContentPane(mainPanel);
            frame.pack();
            Insets insets = frame.getInsets();
            int targetWidth = game.mWidth * 2;
            int targetHeight = game.mHeight + insets.top + insets.bottom;
            frame.setSize(targetWidth, targetHeight);
            startScreen.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
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
        mWidth = 483;
        mHeight = 483;
        mPanel.setDoubleBuffered(true);
        mPanel.addMouseListener(this);
        mPanel.addMouseMotionListener(this);
        mPanel.setPreferredSize(new Dimension(mWidth, mHeight));
        // 设置游戏面板不透明，由我们自己绘制背景
        mPanel.setOpaque(false);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED) keyPressed(e);
                    else if (e.getID() == KeyEvent.KEY_RELEASED) keyReleased(e);
                    else if (e.getID() == KeyEvent.KEY_TYPED) keyTyped(e);
                    return false;
                });
        try {
            File pf = new File("resources/pause.png");
            if (pf.exists()) pauseIcon = ImageIO.read(pf);
        } catch (IOException e) { pauseIcon = null; }

        // 加载最高分
        highScore = loadHighScore();
    }

    @Override
    public void onStartGame() {
        cardLayout.show(mainPanel, "game");

        // 重置游戏状态
        resetGame();

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.resetHoverState();
            glassPane.updateScores(currentScore, highScore);
        }
        if (!gameStarted) {
            gameStarted = true;
            if (startScreen != null) startScreen.stopAnimation();
            init();
            gameLoop(60);
        }
    }

    @Override
    public void onExitGame() {
        System.exit(0);
    }

    // 重置游戏所有状态
    private void resetGame() {
        // 重置分数
        currentScore = 0;
        roundScore = 0;
        roundEnded = true;
        frozen = false;
        gamePaused = false;

        // 清空浮动分数
        floatingScores.clear();

        // 重置弹珠网格
        if (hexGrid != null) {
            hexGrid.resetRow();
        }

        // 重新初始化弹珠网格
        initMarbleGrid();

        // 更新计分板显示
        if (glassPane != null) {
            glassPane.resetScore();
            glassPane.updateScores(currentScore, highScore);
        }
    }

    @Override
    public void init() {
        initMarbleGrid();
        hexGrid.setScoreListener((marble, points) -> {
            currentScore += points;
            roundScore += points;
            if (currentScore > highScore) {
                highScore = currentScore;
                saveHighScore(highScore);
            }
            floatingScores.add(new FloatingScore(marble.getCenterX(), marble.getCenterY(), points));
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
        double originalTopY = calculateOriginalTopY(mHeight);
        double newTopY = originalTopY - DEADLINE_UP_OFFSET;
        launchPad.setFixedDeadlineY(newTopY);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();
        launchMarble = new LaunchMarble();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);
        launchPad.setNextMarbleColorType(random.nextInt(4) + 1);
    }

    private double calculateOriginalTopY(int h) {
        int baseW = (int)(mWidth * 0.2);
        int baseH = (int)(baseW * 0.45);
        return (h - h/5.0) - baseH;
    }

    @Override
    public void update(double dt) {
        if (frozen || gamePaused) return;
        if (hexGrid != null) hexGrid.update(dt);
        if (launchMarble != null) launchMarble.update(dt);
        checkCollisions();
        collisionWithDeadline();
        if (!roundEnded && launchMarble != null && !launchMarble.isLaunched() && roundScore > 0) {
            roundEnded = true;
            roundEndTime = System.currentTimeMillis();
        }
        if (roundEnded && roundScore > 0 && System.currentTimeMillis() - roundEndTime > 500) {
            floatingScores.add(new FloatingScore(mWidth/2.0, mHeight - 150, roundScore));
            roundScore = 0;
            if (glassPane != null) glassPane.repaint();
        }
        floatingScores.removeIf(fs -> fs.update(dt));
    }

    private void checkCollisions() {
        if (launchMarble == null || !launchMarble.isLaunched() || hexGrid == null) return;
        double radius = hexGrid.getSide() * 0.866;
        double collisionDist = radius * 2 - 2;
        double prevX = launchMarble.getPrevCenterX();
        double prevY = launchMarble.getPrevCenterY();
        double currX = launchMarble.getCenterX();
        double currY = launchMarble.getCenterY();
        double dx = currX - prevX, dy = currY - prevY;
        int steps = (int) Math.ceil(Math.hypot(dx, dy) / (radius * 0.25));
        if (steps < 1) steps = 1;
        boolean collided = false;
        for (int i = 1; i <= steps; i++) {
            double cx = prevX + dx * i / steps;
            double cy = prevY + dy * i / steps;
            if (cy <= radius) { collided = true; launchMarble.setCenter(cx, cy); break; }
            for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
                Marble[] row = hexGrid.getRow(r);
                if (row == null) continue;
                for (Marble m : row) {
                    if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling()) {
                        if (Math.hypot(cx - m.getCenterX(), cy - m.getCenterY()) <= collisionDist) {
                            collided = true;
                            launchMarble.setCenter(cx, cy);
                            break;
                        }
                    }
                }
                if (collided) break;
            }
            if (collided) break;
        }
        if (collided) {
            roundEnded = false;
            hexGrid.attachMarble(launchMarble, mWidth);
            int nextColor = launchPad.getNextMarbleColorType();
            launchMarble = new LaunchMarble();
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
            for (Marble m : row) {
                if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && m.getCenterY() + radius >= deadline) {
                    frozen = true;
                    return;
                }
            }
        }
    }

    @Override
    public void paintComponent() {
        Graphics2D g2 = (Graphics2D) mGraphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 使用原来的浅蓝色渐变背景
        LinearGradientPaint bg = new LinearGradientPaint(
                0, 0, 0, mHeight,
                new float[]{0, 1},
                new Color[]{BG_COLOR_TOP, BG_COLOR_BOTTOM}
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, mWidth, mHeight);

        // 绘制弹珠网格
        if (hexGrid != null) hexGrid.draw(g2);

        // 绘制发射台
        if (launchPad != null) {
            launchPad.drawLaunchPad(g2, mWidth, mHeight);
            launchPad.drawCannon(g2, mouseX, mouseY, mWidth, mHeight);
        }

        // 绘制发射中的弹珠
        if (launchMarble != null) launchMarble.draw(g2);

        // 绘制浮动分数
        for (FloatingScore fs : floatingScores) {
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            if (fs.score >= 50) g2.setColor(new Color(0, 0, 0, (int)(fs.alpha*255)));
            else g2.setColor(new Color(180, 180, 220, (int)(fs.alpha*255)));
            g2.drawString(String.valueOf(fs.score), (int)fs.x-10, (int)fs.y);
        }
    }

    public void openPauseMenu() {
        gamePaused = true;
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(mPanel), "Pause Menu", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(350,380);
        dlg.setLocationRelativeTo(mPanel);
        dlg.setResizable(false);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { gamePaused=false; if(glassPane!=null) glassPane.resetHoverState(); mPanel.repaint(); }
        });
        JPanel panel = new JPanel(new BorderLayout(10,20));
        panel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
        panel.setBackground(new Color(240,248,255));
        JLabel title = new JLabel("PAUSED", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 28));
        title.setForeground(new Color(70,150,255));
        panel.add(title, BorderLayout.NORTH);
        JPanel btnPanel = new JPanel(new GridLayout(4,1,0,20));
        btnPanel.setBackground(new Color(240,248,255));
        JButton resume = StartScreen.createStyledButtonStatic("Resume", true, "begin.png");
        resume.addActionListener(e -> { gamePaused=false; if(glassPane!=null) glassPane.resetHoverState(); dlg.dispose(); });
        JButton help = StartScreen.createStyledButtonStatic("How to play", true, "help.png");
        help.addActionListener(e -> JOptionPane.showMessageDialog(dlg, "游戏玩法：\n1.点击/空格发射弹珠\n2.3个同色相连即消除\n3.不要让弹珠碰到底部红线","帮助",JOptionPane.INFORMATION_MESSAGE));
        JButton sound = StartScreen.createStyledButtonStatic(StartScreen.isSoundOnStatic ? "Sound on" : "Sound off", true, "sound.png");
        sound.addActionListener(e -> { StartScreen.isSoundOnStatic = !StartScreen.isSoundOnStatic; sound.setText(StartScreen.isSoundOnStatic ? "Sound on" : "Sound off"); });
        JButton quit = StartScreen.createStyledButtonStatic("Quit", true, "exit.png");
        quit.addActionListener(e -> {
            gamePaused=false; frozen=false; gameStarted=false; hexGrid=null; launchMarble=null;
            if (glassPane != null) {
                glassPane.setVisible(false);
                glassPane.stopScoreBoardAnimation();
            }
            mPanel.setCursor(Cursor.getDefaultCursor());
            dlg.dispose();
            cardLayout.show(mainPanel, "menu");
        });
        btnPanel.add(resume); btnPanel.add(help); btnPanel.add(sound); btnPanel.add(quit);
        panel.add(btnPanel, BorderLayout.CENTER);
        dlg.setContentPane(panel);
        dlg.setVisible(true);
        mPanel.repaint();
    }

    @Override public void mouseMoved(MouseEvent e) { mouseX=e.getX(); mouseY=e.getY(); if(frozen||gamePaused) return; }
    @Override public void mousePressed(MouseEvent e) { if(frozen||gamePaused) return; if(launchMarble!=null && !launchMarble.isLaunched()) { launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y); launchMarble.launch(e.getX(), e.getY()); } }
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void keyPressed(KeyEvent e) { if(frozen||gamePaused) return; if(e.getKeyCode()==KeyEvent.VK_SPACE && launchMarble!=null && !launchMarble.isLaunched()) { launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y); launchMarble.launch(mouseX>0?mouseX:launchPad.cannon.x, mouseY>0?mouseY:launchPad.cannon.y-100); } }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    private int loadHighScore() {
        return Preferences.userNodeForPackage(Main.class).getInt("highScore", 0);
    }

    private void saveHighScore(int s) {
        Preferences.userNodeForPackage(Main.class).putInt("highScore", s);
    }
}