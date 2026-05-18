package org.example;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Set;

/**
 * 弹珠游戏引擎 - 泡泡龙类型游戏
 */
public class GameEngine extends Canvas implements KeyListener, MouseMotionListener, MouseListener, Runnable {

    // 游戏状态枚举
    public enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    // 游戏对象
    private MarbleGrid grid;
    private Shooter shooter;
    private Marble flyingMarble;

    // 游戏状态
    private GameState state = GameState.MENU;
    private int score = 0;
    private int highScore = 0;
    private boolean gameRunning = false;
    private Thread gameThread;
    private BufferedImage buffer;
    private double mouseX, mouseY;

    // 下降机制参数 - 每发射1个球下降1/5行
    private int shootCount = 0;
    private static final int SHOOTS_PER_ROW = 5;  // 每5次发射下降一行
    private double targetScrollOffset = 0;      // 目标滚动偏移
    private double currentScrollOffset = 0;     // 当前滚动偏移（动画用）
    private static final double SCROLL_ANIM_DURATION = 300;  // 滚动动画持续时间(ms)
    private long scrollAnimStartTime = 0;       // 滚动动画开始时间
    private boolean isScrollAnimating = false;  // 是否正在播放滚动动画
    private double rowHeight;                   // 单行高度像素

    // 构造函数
    public GameEngine() {
        // 保持9:16比例（手机屏幕比例）
        setPreferredSize(new Dimension(GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT));
        setMinimumSize(new Dimension(GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT));
        setMaximumSize(new Dimension(GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT));

        grid = new MarbleGrid(GameConfig.GRID_ROWS, GameConfig.GRID_COLS);
        shooter = new Shooter();

        state = GameState.MENU;
        score = 0;
        highScore = 0;
        gameRunning = false;

        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        buffer = new BufferedImage(GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    public void start() {
        renderMenu(buffer.createGraphics());
        repaint();
    }

    public void startGame() {
        grid = new MarbleGrid(GameConfig.GRID_ROWS, GameConfig.GRID_COLS);
        grid.initialize();
        shooter = new Shooter();
        flyingMarble = null;
        score = 0;
        shootCount = 0;
        rowHeight = GameConfig.HEX_SIZE * 1.5;
        targetScrollOffset = 0;
        currentScrollOffset = 0;
        isScrollAnimating = false;
        state = GameState.PLAYING;
        gameRunning = true;

        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    public void restartGame() {
        startGame();
    }

    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();
        while (gameRunning) {
            try {
                Thread.sleep(GameConfig.BASE_GAME_SPEED_MS);
            } catch (InterruptedException e) {
                break;
            }
            if (gameThread != thisThread) break;
            if (state == GameState.PLAYING) {
                update();
                render();
            }
        }
    }

    public void update() {
        if (state != GameState.PLAYING) return;

        // 更新瞄准角度（基于鼠标位置）
        shooter.aim(mouseX, mouseY);

        // 处理滚动动画
        if (isScrollAnimating) {
            long elapsed = System.currentTimeMillis() - scrollAnimStartTime;
            if (elapsed >= SCROLL_ANIM_DURATION) {
                // 动画完成
                currentScrollOffset = targetScrollOffset;
                isScrollAnimating = false;
            } else {
                // 使用easeOutQuad缓动
                double t = elapsed / SCROLL_ANIM_DURATION;
                double ease = 1 - (1 - t) * (1 - t);
                currentScrollOffset += (targetScrollOffset - currentScrollOffset) * ease * 0.15;
            }
        }

        // 更新网格的像素滚动偏移
        grid.setScrollOffsetY(currentScrollOffset);

        // 更新飞行弹珠
        if (flyingMarble != null) {
            double newX = flyingMarble.getX() + flyingMarble.getVx();
            double newY = flyingMarble.getY() + flyingMarble.getVy();

            // 左右墙壁反弹
            if (newX <= GameConfig.MARBLE_RADIUS) {
                newX = GameConfig.MARBLE_RADIUS;
                flyingMarble.setVelocity(-flyingMarble.getVx(), flyingMarble.getVy());
            } else if (newX >= GameConfig.SCENE_WIDTH - GameConfig.MARBLE_RADIUS) {
                newX = GameConfig.SCENE_WIDTH - GameConfig.MARBLE_RADIUS;
                flyingMarble.setVelocity(-flyingMarble.getVx(), flyingMarble.getVy());
            }

            flyingMarble.setPosition(newX, newY);

            // 检测碰撞
            checkCollision();

            // 边界检测 - 弹珠到达顶部
            if (newY <= GameConfig.GRID_OFFSET_Y + GameConfig.MARBLE_RADIUS) {
                attachMarble();
            }
        }

        // 更新坠落的弹珠
        Set<Marble> allMarbles = grid.getAllMarbles();
        for (Marble m : allMarbles) {
            if (m.isFalling() || m.isSliding()) {
                m.update();
                if (m.getY() > GameConfig.SCENE_HEIGHT + GameConfig.MARBLE_RADIUS) {
                    grid.removeMarble(m.getRow(), m.getCol());
                    score += 10;
                }
            }
        }

        // 检测游戏结束 - 弹珠到达第21行
        for (int col = 0; col < GameConfig.GRID_COLS; col++) {
            Marble m = grid.getMarble(GameConfig.GRID_ROWS - 1, col);
            if (m != null && !m.isFalling() && !m.isSliding()) {
                state = GameState.GAME_OVER;
                if (score > highScore) highScore = score;
                render();
                return;
            }
        }
    }

    private void checkCollision() {
        if (flyingMarble == null) return;

        double marbleRadius = GameConfig.MARBLE_RADIUS;
        for (int row = 0; row < GameConfig.GRID_ROWS; row++) {
            for (int col = 0; col < GameConfig.GRID_COLS; col++) {
                Marble marble = grid.getMarble(row, col);
                if (marble != null) {
                    double[] pos = grid.getHexCenterWithScroll(row, col);
                    double dx = flyingMarble.getX() - pos[0];
                    double dy = flyingMarble.getY() - pos[1];
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance < marbleRadius * 2) {
                        attachMarble();
                        return;
                    }
                }
            }
        }
    }

    private void attachMarble() {
        if (flyingMarble == null) return;

        // 找到最近的空位（使用滚动后的位置）
        int bestRow = 0, bestCol = 0;
        double minDist = Double.MAX_VALUE;

        for (int row = 0; row < GameConfig.GRID_ROWS; row++) {
            for (int col = 0; col < GameConfig.GRID_COLS; col++) {
                if (grid.getMarble(row, col) == null) {
                    double[] pos = grid.getHexCenterWithScroll(row, col);
                    double dx = flyingMarble.getX() - pos[0];
                    double dy = flyingMarble.getY() - pos[1];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < minDist) {
                        minDist = dist;
                        bestRow = row;
                        bestCol = col;
                    }
                }
            }
        }

        // 添加弹珠
        grid.addMarble(bestRow, bestCol, flyingMarble.getColor());

        // 获取刚添加的弹珠
        Marble attached = grid.getMarble(bestRow, bestCol);

        // 检查3个或以上相同颜色弹珠相邻连接
        if (attached != null) {
            Set<Marble> connected = grid.findConnected(attached);
            if (connected.size() >= 3) {
                grid.removeMarbles(connected);
                score += connected.size() * 10;
                // 消除后检查孤立弹珠（悬空掉落）
                checkFloatingMarbles();
            }
        }

        flyingMarble = null;
        shooter.clearFlyingMarble();

        // 每发射1个球下降1/5行（五分之一行）
        shootCount++;
        double descentPerShot = rowHeight / SHOOTS_PER_ROW;
        targetScrollOffset += descentPerShot;

        // 启动滚动动画
        if (!isScrollAnimating) {
            isScrollAnimating = true;
            scrollAnimStartTime = System.currentTimeMillis();
        }

        // 检查是否需要生成新行
        checkAndGenerateNewRow();

        // 满5次发射后重置计数
        if (shootCount >= SHOOTS_PER_ROW) {
            shootCount = 0;
        }
    }

    /**
     * 检查是否需要生成新行（当弹珠滚动到顶部时）
     */
    private void checkAndGenerateNewRow() {
        // 如果滚动偏移超过一行高度，生成新行并重置偏移
        if (targetScrollOffset >= rowHeight) {
            targetScrollOffset -= rowHeight;
            currentScrollOffset = targetScrollOffset;
            // 将所有弹珠向下移动一行（在网格内）
            shiftMarblesDown();
        }
    }

    /**
     * 将所有弹珠向下移动一行（保持弹珠引用不变）
     */
    private void shiftMarblesDown() {
        // 从底部第二行开始，将弹珠向下移动一行
        for (int row = GameConfig.GRID_ROWS - 2; row >= 0; row--) {
            for (int col = 0; col < GameConfig.GRID_COLS; col++) {
                Marble marble = grid.getMarble(row, col);
                if (marble != null) {
                    // 移除原位置的弹珠
                    grid.removeMarble(row, col);
                    // 在新位置放置同一个弹珠对象
                    grid.placeMarble(row + 1, col, marble);
                }
            }
        }
        // 在顶部生成新行
        for (int col = 0; col < GameConfig.GRID_COLS; col++) {
            MarbleColor randomColor = MarbleColor.values()[(int)(Math.random() * 4)];
            grid.addMarble(0, col, randomColor);
        }
    }

    /**
     * 获取当前像素级滚动偏移
     */
    public double getPixelScrollOffset() {
        return currentScrollOffset;
    }

    /**
     * 检查孤立弹珠（不与顶部相连的弹珠），使其坠落并计分
     */
    private void checkFloatingMarbles() {
        Set<Marble> connected = grid.findAllConnectedFromTop();
        Set<Marble> allMarbles = grid.getAllMarbles();
        // 找出孤立的弹珠
        for (Marble m : allMarbles) {
            if (!connected.contains(m)) {
                m.startFalling();
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        if (buffer != null) {
            g.drawImage(buffer, 0, 0, this);
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    public void render() {
        Graphics2D g = buffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景色
        g.setColor(new Color(30, 30, 50));
        g.fillRect(0, 0, GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT);

        switch (state) {
            case MENU -> renderMenu(g);
            case PLAYING, PAUSED -> {
                renderGame(g);
                if (state == GameState.PAUSED) renderPaused(g);
            }
            case GAME_OVER -> {
                renderGame(g);
                renderGameOver(g);
            }
        }

        g.dispose();

        Graphics screen = getGraphics();
        if (screen != null) {
            screen.drawImage(buffer, 0, 0, null);
            screen.dispose();
        }
    }

    private void renderMenu(Graphics2D g) {
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String title = "MARBLE GAME";
        g.drawString(title, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(title)) / 2, 180);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        String subtitle = "Bubble Shooter";
        g.drawString(subtitle, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(subtitle)) / 2, 220);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        String start = "Click to Start";
        g.drawString(start, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(start)) / 2, 320);

        g.setColor(Color.GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("High Score: " + highScore, GameConfig.SCENE_WIDTH / 2 - 40, 370);

        g.drawString("SPACE to shoot | Match 3+ colors", GameConfig.SCENE_WIDTH / 2 - 90, 420);
        g.drawString("Every 5 shots - marbles push down!", GameConfig.SCENE_WIDTH / 2 - 100, 450);
    }

    private void renderGame(Graphics2D g) {
        // 网格区域背景
        g.setColor(new Color(20, 20, 40));
        g.fillRect(0, GameConfig.GRID_OFFSET_Y, GameConfig.SCENE_WIDTH,
                   GameConfig.SCENE_HEIGHT - GameConfig.GRID_OFFSET_Y);

        // 绘制危险线（弹珠触底判定线）
        g.setColor(new Color(255, 50, 50, 100));
        int dangerY = (int)(GameConfig.SHOOTER_Y - 40);
        g.drawLine(0, dangerY, GameConfig.SCENE_WIDTH, dangerY);

        // 绘制蜂窝状六边形网格背景（边边相连）
        grid.renderHoneycombBackground(g);

        // 绘制弹珠网格
        grid.render(g);

        // 绘制飞行弹珠
        if (flyingMarble != null) {
            flyingMarble.render(g);
        }

        // 绘制发射器（炮台）
        shooter.render(g);

        // 绘制HUD
        renderHUD(g);
    }

    private void renderHUD(Graphics2D g) {
        // 顶部状态栏
        g.setColor(new Color(15, 15, 30));
        g.fillRect(0, 0, GameConfig.SCENE_WIDTH, GameConfig.GRID_OFFSET_Y);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Score: " + score, 10, 18);

        g.setColor(Color.CYAN);
        g.drawString("Best: " + highScore, GameConfig.SCENE_WIDTH - 70, 18);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("SPACE-Shoot", GameConfig.SCENE_WIDTH / 2 - 35, 18);

        // 发射计数指示器
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 9));
        String shotsLeft = SHOOTS_PER_ROW - shootCount + "";
        g.drawString("[" + shotsLeft + "]", GameConfig.SCENE_WIDTH / 2 + 40, 18);
    }

    private void renderPaused(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT);
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String text = "PAUSED";
        g.drawString(text, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(text)) / 2,
                     GameConfig.SCENE_HEIGHT / 2);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        String resume = "Press P to Resume";
        g.drawString(resume, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(resume)) / 2,
                     GameConfig.SCENE_HEIGHT / 2 + 45);
    }

    private void renderGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 220));
        g.fillRect(0, 0, GameConfig.SCENE_WIDTH, GameConfig.SCENE_HEIGHT);
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 42));
        String go = "GAME OVER";
        g.drawString(go, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(go)) / 2, 180);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        String scoreStr = "Score: " + score;
        g.drawString(scoreStr, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(scoreStr)) / 2, 240);

        if (score >= highScore && score > 0) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            String newBest = "NEW HIGH SCORE!";
            g.drawString(newBest, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(newBest)) / 2, 285);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        String restart = "Click to Play Again";
        g.drawString(restart, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(restart)) / 2, 360);

        g.setColor(Color.CYAN);
        String menu = "ESC - Menu";
        g.drawString(menu, (GameConfig.SCENE_WIDTH - g.getFontMetrics().stringWidth(menu)) / 2, 400);
    }

    // 键盘事件
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (state) {
            case MENU -> {
                if (keyCode == KeyEvent.VK_ESCAPE) System.exit(0);
            }
            case PLAYING -> {
                if (keyCode == KeyEvent.VK_SPACE) {
                    // 空格发射弹珠
                    if (flyingMarble == null && !shooter.hasFlyingMarble()) {
                        flyingMarble = shooter.shoot();
                    }
                } else if (keyCode == KeyEvent.VK_P) {
                    state = GameState.PAUSED;
                    render();
                } else if (keyCode == KeyEvent.VK_ESCAPE) {
                    state = GameState.MENU;
                    gameRunning = false;
                    render();
                }
            }
            case PAUSED -> {
                if (keyCode == KeyEvent.VK_P) {
                    state = GameState.PLAYING;
                    render();
                } else if (keyCode == KeyEvent.VK_ESCAPE) {
                    state = GameState.MENU;
                    gameRunning = false;
                    render();
                }
            }
            case GAME_OVER -> {
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    state = GameState.MENU;
                    render();
                }
            }
        }
    }

    // 鼠标事件 - 全窗口范围
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (state == GameState.MENU) {
            startGame();
        } else if (state == GameState.GAME_OVER) {
            restartGame();
        } else if (state == GameState.PLAYING) {
            // 左键单击发射弹珠
            if (e.getButton() == MouseEvent.BUTTON1 && flyingMarble == null && !shooter.hasFlyingMarble()) {
                flyingMarble = shooter.shoot();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}

    public void stopGame() {
        gameRunning = false;
        if (gameThread != null) {
            gameThread.interrupt();
        }
    }
}