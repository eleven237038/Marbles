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
    }

    private final ScreenStartListener listener;
    private final ArrayList<Marble> decorMarbles = new ArrayList<>();
    private boolean startHover = false;
    private boolean settingHover = false;
    private boolean startPressed = false;
    private boolean settingPressed = false;
    public static boolean isSoundOnStatic = true;

    private static final LinearGradientPaint BG_GRADIENT;
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
        BG_GRADIENT = new LinearGradientPaint(
                0, 0, 0, 1,
                new float[]{0, 1},
                new Color[]{new Color(188, 195, 255), new Color(188, 195, 255)}
        );
        TITLE_FONT = new Font("Arial Black", Font.BOLD, 75);
    }

    private static final Map<String, BufferedImage> ICON_CACHE = new HashMap<>();

    private final int BTN_WIDTH = 220;
    private final int BTN_HEIGHT = 80;
    private int startX, startY;
    private int settingX, settingY;
    private final int SETTING_SIZE = 60;

    private BufferedImage[] playSprites;
    private int playSpriteIndex = 0;
    private static final int PLAY_SPRITE_COUNT = 4;
    private static final int PLAY_ANIM_DELAY = 10;
    private boolean playAnimating = false;
    private boolean playAnimStarted = false;
    private javax.swing.Timer playSpriteTimer;

    private int fallOffset = -300;

    private javax.swing.Timer animationTimer;
    private BufferedImage settingsIcon;

    public ScreenStart(ScreenStartListener listener) {
        this.listener = listener;
        setBackground(new Color(188, 195, 255));
        setPreferredSize(new Dimension(Main.TOTAL_WIDTH, Main.GAME_HEIGHT));

        SoundManager.getInstance().setSoundEnabled(isSoundOnStatic);

        try {
            settingsIcon = ImageIO.read(new File("resources/settings.png"));
        } catch (IOException e) {
            settingsIcon = null;
        }

        playSprites = new BufferedImage[PLAY_SPRITE_COUNT];
        for (int i = 0; i < PLAY_SPRITE_COUNT; i++) {
            try {
                File f = new File("resources/image/PLAY/PLAY-" + (i + 1) + ".png");
                if (f.exists()) {
                    playSprites[i] = ImageIO.read(f);
                }
            } catch (IOException e) {
                playSprites[i] = null;
            }
        }

        playSpriteTimer = new javax.swing.Timer(PLAY_ANIM_DELAY, e -> {
            if (playAnimating && playSpriteIndex < PLAY_SPRITE_COUNT - 1) {
                playSpriteIndex++;
                repaint();
            } else if (playSpriteIndex >= PLAY_SPRITE_COUNT - 1) {
                playAnimating = false;
                playSpriteTimer.stop();
                repaint();
            }
        });

        initDecorMarbles();

        animationTimer = new javax.swing.Timer(16, e -> {
            if (fallOffset < 0) {
                fallOffset += 8;
                if (fallOffset > 0) fallOffset = 0;
                repaint();
            } else {
                animationTimer.stop();
            }
        });

        startAnimation();

        javax.swing.Timer startDelayTimer = new javax.swing.Timer(PLAY_ANIM_DELAY, e -> {
            playAnimStarted = true;
            playAnimating = true;
            playSpriteTimer.start();
            repaint();
            ((javax.swing.Timer)e.getSource()).stop();
        });
        startDelayTimer.setRepeats(false);
        startDelayTimer.start();

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getPoint().x;
                int my = e.getPoint().y;
                startPressed = getPlayButtonBounds().contains(mx, my);
                settingPressed = new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my);
                repaint();

                if (startPressed) {
                    listener.onStartGame();
                } else if (settingPressed) {
                    openSettings();
                } else {
                    restartPlayAnimation();
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
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void restartPlayAnimation() {
        playSpriteIndex = 0;
        playAnimating = true;
        if (!playSpriteTimer.isRunning()) {
            playSpriteTimer.start();
        }
        repaint();
    }

    private Rectangle getPlayButtonBounds() {
        if (playSprites != null && playSprites[playSpriteIndex] != null) {
            int imgW = playSprites[playSpriteIndex].getWidth();
            int imgH = playSprites[playSpriteIndex].getHeight();
            int centerX = startX + BTN_WIDTH / 2;
            int centerY = startY + BTN_HEIGHT / 2;
            return new Rectangle(centerX - imgW / 2, centerY - imgH / 2, imgW, imgH);
        }
        return new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT);
    }

    private void updateHoverState(int mx, int my) {
        boolean oldStartHover = startHover;
        boolean oldSettingHover = settingHover;

        startHover = getPlayButtonBounds().contains(mx, my) && playAnimStarted;
        settingHover = new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my);

        if (startHover || settingHover) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        if (oldStartHover != startHover || oldSettingHover != settingHover) {
            repaint();
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
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    public void restartAnimation() {
        fallOffset = -300;
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        if (playSpriteTimer.isRunning()) {
            playSpriteTimer.stop();
        }
        playAnimStarted = false;
        playAnimating = false;
        playSpriteIndex = 0;
        repaint();

        javax.swing.Timer restartDelayTimer = new javax.swing.Timer(PLAY_ANIM_DELAY, e -> {
            playAnimStarted = true;
            playAnimating = true;
            playSpriteTimer.start();
            repaint();
            ((javax.swing.Timer)e.getSource()).stop();
        });
        restartDelayTimer.setRepeats(false);
        restartDelayTimer.start();
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (playSpriteTimer != null) {
            playSpriteTimer.stop();
        }
    }

    private void openSettings() {
        // 只打开设置面板，不切换音效
        listener.onOpenSettings();
        settingPressed = false;
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

        drawBackground(g2d, w, h);
        drawLuxuryTitle(g2d, w, h, fallOffset);
        drawCompactMarbles(g2d, w, h, fallOffset);
        drawStartButton(g2d);
        drawStandardGearButton(g2d, h);
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
        int baseY = h / 4 + offset;

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
        int titleY = h / 4 + offset;

        int[][] positions = {
                {centerX - 170, titleY - 60},
                {centerX + 170, titleY - 60},
                {centerX - 200, titleY},
                {centerX + 200, titleY},
                {centerX - 120, titleY + 50},
                {centerX, titleY + 80},
                {centerX + 120, titleY + 50},
                {centerX, titleY - 80}
        };

        for (int i = 0; i < decorMarbles.size(); i++) {
            Marble m = decorMarbles.get(i);
            m.setCenter(positions[i][0], positions[i][1]);
            m.setSide(28);
            m.draw(g2d);
        }
    }

    private void drawStartButton(Graphics2D g2d) {
        if (!playAnimStarted) {
            return;
        }

        BufferedImage sprite = playSprites[playSpriteIndex];
        if (sprite != null) {
            int imgW = sprite.getWidth();
            int imgH = sprite.getHeight();
            int centerX = startX + BTN_WIDTH / 2;
            int centerY = startY + BTN_HEIGHT / 2;
            int drawX = centerX - imgW / 2;
            int drawY = centerY - imgH / 2;

            g2d.drawImage(sprite, drawX, drawY, null);
        } else {
            RoundRectangle2D btn = new RoundRectangle2D.Double(startX, startY, BTN_WIDTH, BTN_HEIGHT, 35, 35);

            Color c1 = startPressed ? new Color(50, 140, 255) :
                    startHover ? new Color(100, 190, 255) : new Color(70, 150, 255);
            Color c2 = startPressed ? new Color(30, 110, 255) :
                    startHover ? new Color(50, 140, 255) : new Color(30, 110, 255);

            LinearGradientPaint btnGrad = new LinearGradientPaint(startX, startY, startX, startY + BTN_HEIGHT,
                    new float[]{0, 1}, new Color[]{c1, c2});
            g2d.setPaint(btnGrad);
            g2d.fill(btn);

            g2d.setStroke(new BasicStroke(2.5f));
            g2d.setColor(Color.WHITE);
            g2d.draw(btn);

            int x = startX + BTN_WIDTH / 2;
            int y = startY + BTN_HEIGHT / 2;
            int[] xP = {x - 18, x + 18, x - 18};
            int[] yP = {y - 18, y, y + 18};
            g2d.fillPolygon(xP, yP, 3);
        }
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
                File f = new File("resources/" + name);
                return f.exists() ? ImageIO.read(f) : null;
            } catch (IOException e) {
                return null;
            }
        });
    }
}