import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ScreenStart extends JPanel {
    public interface ScreenStartListener {
        void onStartGame();
        void onOpenSettings();
        void onSelectLevel(int level);
    }

    private final ScreenStartListener listener;
    private final ArrayList<Marble> decorMarbles = new ArrayList<>();
    private boolean startHover = false;
    private boolean settingHover = false;
    private boolean startPressed = false;
    private boolean settingPressed = false;
    public static boolean isSoundOnStatic = true;

    // 播放按钮跳动动画变量
    private long playBtnAnimStartTime = 0;
    private boolean isPlayBtnAnimating = false;
    private boolean startAnimTriggered = false;

    private static final Font TITLE_FONT;
    private static final Color TITLE_SHADOW_COLOR_1 = new Color(0, 0, 0, 50);
    private static final Color TITLE_SHADOW_COLOR_2 = new Color(0, 0, 0, 30);
    private static final Color TITLE_OUTLINE_COLOR = new Color(255, 255, 255, 220);
    private static final Color TITLE_SHADOW_OUTLINE = new Color(0, 0, 0, 80);
    private static final float[] TITLE_GRADIENT_STOPS = {0, 0.25f, 0.5f, 0.75f, 1f};
    private static final Color[] TITLE_GRADIENT_COLORS = {
            new Color(255, 70, 70),
            new Color(255, 180, 50),
            new Color(50, 200, 255),
            new Color(180, 70, 255),
            new Color(255, 90, 150)
    };

    static {
        // 增大了主标题尺寸
        TITLE_FONT = new Font("Comic Sans MS", Font.BOLD, 105);
    }

    private final Rectangle startBtnBounds = new Rectangle();
    private final Rectangle settingBtnBounds = new Rectangle();

    private static final Map<String, BufferedImage> ICON_CACHE = new HashMap<>();

    private final int BTN_WIDTH = 220;
    private final int BTN_HEIGHT = 80;
    private int startX, startY;
    private int settingX, settingY;
    private final int SETTING_SIZE = 60;
    private final int BTN_SPACING = 15;

    // Level select grid constants
    private static final int LEVEL_BTN_SIZE = 55;
    private static final int LEVEL_BTN_SPACING = 10;
    private static final int LEVEL_GRID_COLUMNS = 4;

    private int fallOffset = -300;

    private javax.swing.Timer animationTimer;
    private BufferedImage settingsIcon;
    private boolean showLevelSelectOverlay = false;
    private int[] levelHoverStates = new int[Level.MAX_LEVEL];

    public ScreenStart(ScreenStartListener listener) {
        this.listener = listener;
        setBackground(new Color(188, 195, 255));
        setPreferredSize(new Dimension(Main.TOTAL_WIDTH, Main.GAME_HEIGHT));

        ResourceManager.getInstance().setSoundEnabled(isSoundOnStatic);

        try {
            settingsIcon = loadIcon("settings.png");
        } catch (Exception e) {
            settingsIcon = null;
        }

        initDecorMarbles();

        animationTimer = new javax.swing.Timer(16, e -> {
            boolean active = false;
            if (fallOffset < 0) {
                fallOffset += 8;
                if (fallOffset >= 0) {
                    fallOffset = 0;
                }
                active = true;
            }
            
            // 下落结束后，首次触发跳动按键
            if (fallOffset == 0 && !startAnimTriggered) {
                startAnimTriggered = true;
                triggerPlayBtnAnimation();
                active = true;
            }
            
            if (isPlayBtnAnimating) {
                long elapsed = System.currentTimeMillis() - playBtnAnimStartTime;
                if (elapsed > 450) {
                    isPlayBtnAnimating = false;
                }
                active = true;
            }

            if (active) {
                repaint();
            } else if (fallOffset == 0 && !isPlayBtnAnimating) {
                animationTimer.stop();
            }
        });

        startAnimation();

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getPoint().x;
                int my = e.getPoint().y;
                if (showLevelSelectOverlay) {
                    handleLevelSelectOverlayClick(mx, my);
                } else {
                    startPressed = startBtnBounds.contains(mx, my);
                    settingPressed = settingBtnBounds.contains(mx, my);
                    repaint();

                    if (startPressed) {
                        showLevelSelectOverlay = true;
                        repaint();
                    } else if (settingPressed) {
                        openSettings();
                    } else {
                        // 若不是点击按键，则同样触发跳动出场动效
                        triggerPlayBtnAnimation();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                startPressed = false;
                settingPressed = false;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startHover = false;
                settingHover = false;
                for (int i = 0; i < levelHoverStates.length; i++) {
                    levelHoverStates[i] = 0;
                }
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void updateHoverState(int mx, int my) {
        boolean oldStartHover = startHover;
        boolean oldSettingHover = settingHover;

        startHover = startBtnBounds.contains(mx, my);
        settingHover = settingBtnBounds.contains(mx, my);

        if (showLevelSelectOverlay) {
            updateLevelSelectOverlayHover(mx, my);
        }

        if (startHover || settingHover) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        if (oldStartHover != startHover || oldSettingHover != settingHover) {
            repaint();
        }
    }

    private void updateLevelSelectOverlayHover(int mx, int my) {
        if (!showLevelSelectOverlay) return;

        int panelW = getWidth();
        int panelH = getHeight();
        int gridStartX = panelW / 2 - (LEVEL_GRID_COLUMNS * LEVEL_BTN_SIZE + (LEVEL_GRID_COLUMNS - 1) * LEVEL_BTN_SPACING) / 2;
        int gridStartY = panelH / 2 - LEVEL_BTN_SIZE / 2;

        int unlockedCount = Level.getInstance().getUnlockedLevelCount();
        boolean changed = false;
        for (int i = 0; i < Level.MAX_LEVEL; i++) {
            int col = i % LEVEL_GRID_COLUMNS;
            int row = i / LEVEL_GRID_COLUMNS;
            int x = gridStartX + col * (LEVEL_BTN_SIZE + LEVEL_BTN_SPACING);
            int y = gridStartY + row * (LEVEL_BTN_SIZE + LEVEL_BTN_SPACING);

            Rectangle rect = new Rectangle(x, y, LEVEL_BTN_SIZE, LEVEL_BTN_SIZE);
            int newState = rect.contains(mx, my) ? 1 : 0;
            if (levelHoverStates[i] != newState) {
                levelHoverStates[i] = newState;
                changed = true;
            }
        }
        if (changed) repaint();
    }

    private void handleLevelSelectOverlayClick(int mx, int my) {
        int panelW = getWidth();
        int panelH = getHeight();
        int closeBtnW = 120;
        int closeBtnH = 45;
        int closeBtnX = (panelW - closeBtnW) / 2;
        int closeBtnY = panelH - closeBtnH - 20;

        Rectangle closeBtn = new Rectangle(closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        if (closeBtn.contains(mx, my)) {
            showLevelSelectOverlay = false;
            repaint();
            return;
        }

        int gridStartX = panelW / 2 - (LEVEL_GRID_COLUMNS * LEVEL_BTN_SIZE + (LEVEL_GRID_COLUMNS - 1) * LEVEL_BTN_SPACING) / 2;
        int gridStartY = panelH / 2 - LEVEL_BTN_SIZE / 2;

        int unlockedCount = Level.getInstance().getUnlockedLevelCount();
        for (int i = 0; i < Level.MAX_LEVEL; i++) {
            int col = i % LEVEL_GRID_COLUMNS;
            int row = i / LEVEL_GRID_COLUMNS;
            int x = gridStartX + col * (LEVEL_BTN_SIZE + LEVEL_BTN_SPACING);
            int y = gridStartY + row * (LEVEL_BTN_SIZE + LEVEL_BTN_SPACING);

            Rectangle rect = new Rectangle(x, y, LEVEL_BTN_SIZE, LEVEL_BTN_SIZE);
            if (rect.contains(mx, my)) {
                int level = i + 1;
                if (Level.getInstance().isLevelUnlocked(level)) {
                    showLevelSelectOverlay = false;
                    listener.onSelectLevel(level);
                }
                return;
            }
        }
    }

    private void initDecorMarbles() {
        for (int i = 0; i < 8; i++) {
            Marble m = new Marble();
            m.init(0, 0, 0, 0);
            decorMarbles.add(m);
        }
    }

    private void startAnimation() {
        fallOffset = -300;
        startAnimTriggered = false;
        isPlayBtnAnimating = false;
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    public void restartAnimation() {
        fallOffset = -300;
        startAnimTriggered = false;
        isPlayBtnAnimating = false;
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private void triggerPlayBtnAnimation() {
        playBtnAnimStartTime = System.currentTimeMillis();
        isPlayBtnAnimating = true;
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    private void openSettings() {
        listener.onOpenSettings();
        settingPressed = false;
        repaint();
    }

    public void showLevelSelectOverlay() {
        showLevelSelectOverlay = true;
        repaint();
    }

    public void hideLevelSelectOverlay() {
        showLevelSelectOverlay = false;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        enableHighQualityRender(g2d);

        int w = getWidth();
        int h = getHeight();

        startX = w / 2 - BTN_WIDTH / 2;
        startY = h / 2 - BTN_HEIGHT / 2 + 60;

        settingX = 30;
        settingY = h - SETTING_SIZE - 30;

        settingBtnBounds.setBounds(settingX, settingY, SETTING_SIZE, SETTING_SIZE);

        drawBackground(g2d, w, h);
        drawLuxuryTitle(g2d, w, h, fallOffset);
        drawCompactMarbles(g2d, w, h, fallOffset);
        drawStartButton(g2d);
        drawStandardGearButton(g2d, h);

        if (showLevelSelectOverlay) {
            drawLevelSelectOverlay(g2d, w, h);
        }
    }

    private void enableHighQualityRender(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void drawBackground(Graphics2D g2d, int w, int h) {
        LinearGradientPaint bg = new LinearGradientPaint(
                0, 0, 0, h,
                new float[]{0, 1},
                new Color[]{new Color(188, 195, 255), new Color(188, 195, 255)}
        );
        g2d.setPaint(bg);
        g2d.fillRect(0, 0, w, h);
    }

    private void drawLuxuryTitle(Graphics2D g2d, int w, int h, int offset) {
        g2d.setFont(TITLE_FONT);
        String title = "MARBLE";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int baseX = w / 2 - titleWidth / 2;
        // 标题向下轻微偏移，适配变大后的尺寸
        int baseY = h / 4 + offset + 20;

        g2d.setColor(TITLE_SHADOW_COLOR_1);
        g2d.drawString(title, baseX + 6, baseY + 6);
        g2d.setColor(TITLE_SHADOW_COLOR_2);
        g2d.drawString(title, baseX + 3, baseY + 3);

        LinearGradientPaint textGrad = new LinearGradientPaint(
                baseX, baseY - 40, baseX + titleWidth, baseY + 40,
                TITLE_GRADIENT_STOPS, TITLE_GRADIENT_COLORS
        );
        g2d.setPaint(textGrad);
        g2d.drawString(title, baseX, baseY);

        g2d.setStroke(new BasicStroke(3.5f));
        g2d.setColor(TITLE_OUTLINE_COLOR);
        g2d.drawString(title, baseX, baseY);

        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(TITLE_SHADOW_OUTLINE);
        g2d.drawString(title, baseX, baseY);
    }

    private void drawCompactMarbles(Graphics2D g2d, int w, int h, int offset) {
        int centerX = w / 2;
        int titleY = h / 4 + offset + 20;

        // 扩宽弹珠与中心标题的距离，适配巨幅标题
        int[][] positions = {
                {centerX - 240, titleY - 80},
                {centerX + 240, titleY - 80},
                {centerX - 280, titleY + 10},
                {centerX + 280, titleY + 10},
                {centerX - 180, titleY + 90},
                {centerX, titleY + 120},
                {centerX + 180, titleY + 90},
                {centerX, titleY - 110}
        };

        for (int i = 0; i < decorMarbles.size(); i++) {
            Marble m = decorMarbles.get(i);
            m.setCenter(positions[i][0], positions[i][1]);
            m.setSide(28);
            m.draw(g2d);
        }
    }

    private void drawStartButton(Graphics2D g2d) {
        double scaleX = 1.0;

        // 按钮入场动画：从0平滑展开到1，带轻微弹性
        if (isPlayBtnAnimating) {
            long elapsed = System.currentTimeMillis() - playBtnAnimStartTime;
            double t = Math.min(elapsed / 800.0, 1.0); // 800ms展开

            // 从0到1的平滑过渡 + 微幅弹性
            scaleX = 0.0
                + 1.0 * easeOutBack(t)      // 主展开，带轻微回弹
                + 0.03 * sineWave(t, 0.4);  // 到达后的微幅振荡
        } else if (!startAnimTriggered) {
            scaleX = 0.0;
        }

        if (scaleX == 0.0) return;

        // 根据缩放计算新的大小和中心坐标
        int currentWidth = (int)(BTN_WIDTH * scaleX);
        int currentX = startX + (BTN_WIDTH - currentWidth) / 2;

        startBtnBounds.setBounds(currentX, startY, currentWidth, BTN_HEIGHT);
        RoundRectangle2D btn = new RoundRectangle2D.Double(currentX, startY, currentWidth, BTN_HEIGHT, 35, 35);

        Color c1 = startPressed ? new Color(50, 140, 255) :
                startHover ? new Color(100, 190, 255) : new Color(70, 150, 255);
        Color c2 = startPressed ? new Color(30, 110, 255) :
                startHover ? new Color(50, 140, 255) : new Color(30, 110, 255);

        LinearGradientPaint btnGrad = new LinearGradientPaint(currentX, startY, currentX, startY + BTN_HEIGHT,
                new float[]{0, 1}, new Color[]{c1, c2});
        g2d.setPaint(btnGrad);
        g2d.fill(btn);

        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        // 修改按钮文字和三角形呈现效果: "▶ PLAY"
        g2d.setFont(new Font("Arial Black", Font.BOLD, 28));
        String text = "PLAY";
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(text);

        int triSize = 20;
        int spacing = 10;
        int totalW = triSize + spacing + textW;

        int drawX = currentX + (currentWidth - totalW) / 2;
        int drawY = startY + BTN_HEIGHT / 2;

        // 绘制三角形 ▶
        int[] xP = {drawX, drawX + triSize, drawX};
        int[] yP = {drawY - triSize/2, drawY, drawY + triSize/2};
        g2d.setColor(Color.WHITE);
        g2d.fillPolygon(xP, yP, 3);

        // 绘制文本
        g2d.drawString(text, drawX + triSize + spacing, drawY + fm.getAscent()/2 - 3);
    }

    // easeOutBack：先回弹再超调
    private double easeOutBack(double t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    // 正弦波：用于到达后的微幅振荡
    private double sineWave(double t, double damping) {
        if (t <= 0) return 0;
        if (t >= 1) return 0;
        return Math.sin(t * Math.PI * 3) * Math.exp(-t / damping) * 0.5;
    }

    private void drawLevelSelectOverlay(Graphics2D g2d, int w, int h) {
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, w, h);

        int panelW = w;
        int panelH = h;

        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 36));
        g2d.setColor(new Color(255, 215, 0));
        String title = "SELECT LEVEL";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (panelW - fm.stringWidth(title)) / 2, 55);

        int gridStartX = panelW / 2 - (LEVEL_GRID_COLUMNS * LEVEL_BTN_SIZE + (LEVEL_GRID_COLUMNS - 1) * LEVEL_BTN_SPACING) / 2;
        int gridStartY = panelH / 2 - LEVEL_BTN_SIZE / 2;

        Level levelManager = Level.getInstance();
        int unlockedCount = levelManager.getUnlockedLevelCount();

        for (int i = 0; i < Level.MAX_LEVEL; i++) {
            int col = i % LEVEL_GRID_COLUMNS;
            int row = i / LEVEL_GRID_COLUMNS;
            int x = gridStartX + col * (LEVEL_BTN_SIZE + LEVEL_BTN_SPACING);
            int y = gridStartY + row * (LEVEL_BTN_SIZE + LEVEL_BTN_SPACING);
            int level = i + 1;

            boolean unlocked = level <= unlockedCount;
            boolean hover = levelHoverStates[i] == 1;

            RoundRectangle2D btn = new RoundRectangle2D.Double(x, y, LEVEL_BTN_SIZE, LEVEL_BTN_SIZE, 12, 12);

            if (unlocked) {
                Color c1 = hover ? new Color(100, 190, 255) : new Color(70, 150, 255);
                Color c2 = hover ? new Color(50, 140, 255) : new Color(30, 110, 255);
                LinearGradientPaint btnGrad = new LinearGradientPaint(x, y, x, y + LEVEL_BTN_SIZE,
                        new float[]{0, 1}, new Color[]{c1, c2});
                g2d.setPaint(btnGrad);
                g2d.fill(btn);

                g2d.setStroke(new BasicStroke(2f));
                g2d.setColor(Color.WHITE);
                g2d.draw(btn);

                g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(new Color(80, 80, 80, 200));
                g2d.fill(btn);

                g2d.setStroke(new BasicStroke(2f));
                g2d.setColor(new Color(100, 100, 100, 150));
                g2d.draw(btn);

                g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
                g2d.setColor(new Color(120, 120, 120));
            }

            String levelText = String.valueOf(level);
            fm = g2d.getFontMetrics();
            int textX = x + (LEVEL_BTN_SIZE - fm.stringWidth(levelText)) / 2;
            int textY = y + (LEVEL_BTN_SIZE + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(levelText, textX, textY);
        }

        int closeBtnW = 120;
        int closeBtnH = 45;
        int closeBtnX = (panelW - closeBtnW) / 2;
        int closeBtnY = panelH - closeBtnH - 20;

        RoundRectangle2D closeBtn = new RoundRectangle2D.Double(closeBtnX, closeBtnY, closeBtnW, closeBtnH, 18, 18);
        LinearGradientPaint closeGrad = new LinearGradientPaint(closeBtnX, closeBtnY, closeBtnX, closeBtnY + closeBtnH,
                new float[]{0, 1}, new Color[]{new Color(220, 70, 70), new Color(180, 50, 50)});
        g2d.setPaint(closeGrad);
        g2d.fill(closeBtn);

        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(Color.WHITE);
        g2d.draw(closeBtn);

        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);
        String closeText = "Close";
        fm = g2d.getFontMetrics();
        int closeTextX = closeBtnX + (closeBtnW - fm.stringWidth(closeText)) / 2;
        int closeTextY = closeBtnY + (closeBtnH + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(closeText, closeTextX, closeTextY);
    }

    private void drawStandardGearButton(Graphics2D g2d, int h) {
        int x = settingX;
        int y = settingY;

        if (settingHover || settingPressed) {
            RoundRectangle2D btn = new RoundRectangle2D.Double(x, y, SETTING_SIZE, SETTING_SIZE, 15, 15);
            Color c1 = settingPressed ? new Color(50, 140, 255, 180) :
                    new Color(100, 190, 255, 150);
            Color c2 = settingPressed ? new Color(30, 110, 255, 180) :
                    new Color(50, 140, 255, 150);
            LinearGradientPaint btnGrad = new LinearGradientPaint(x, y, x, y + SETTING_SIZE,
                    new float[]{0, 1}, new Color[]{c1, c2});
            g2d.setPaint(btnGrad);
            g2d.fill(btn);
        }

        if (settingsIcon != null) {
            g2d.drawImage(settingsIcon, x, y, SETTING_SIZE, SETTING_SIZE, null);
        } else {
            double cx = x + SETTING_SIZE / 2.0;
            double cy = y + SETTING_SIZE / 2.0;
            g2d.setColor(new Color(100, 140, 180));
            drawStandardGear(g2d, cx, cy, 22, 12, 8);
        }
    }

    private void drawStandardGear(Graphics2D g2d, double cx, double cy, double outerR, double innerR, int teeth) {
        GeneralPath gear = new GeneralPath();
        double angleStep = Math.PI / teeth;

        for (int i = 0; i < 2 * teeth; i++) {
            double angle = i * angleStep;
            double radius = (i % 2 == 0) ? outerR : innerR;
            double px = cx + radius * Math.cos(angle);
            double py = cy + radius * Math.sin(angle);
            if (i == 0) gear.moveTo(px, py);
            else gear.lineTo(px, py);
        }
        gear.closePath();
        g2d.fill(gear);
        g2d.setColor(Color.WHITE);
        g2d.fillOval((int) cx - 6, (int) cy - 6, 12, 12);
    }

    public static BufferedImage loadIcon(String iconName) {
        return ICON_CACHE.computeIfAbsent(iconName, name -> {
            try {
                File f = new File(ResourceManager.getImagePath(name));
                return f.exists() ? ImageIO.read(f) : null;
            } catch (IOException e) {
                return null;
            }
        });
    }
}