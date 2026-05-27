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
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;

public class ScreenStart extends JPanel {
    public interface ScreenStartListener {
        void onStartGame();
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
                new Color[]{new Color(230, 245, 255), new Color(190, 225, 255)}
        );
        TITLE_FONT = new Font("Arial Black", Font.BOLD, 70);
    }

    private static final Map<String, BufferedImage> ICON_CACHE = new HashMap<>();

    private final int BTN_WIDTH = 200;
    private final int BTN_HEIGHT = 80;
    private int startX, startY;
    private int settingX, settingY;
    private int exitX, exitY;
    private final int SETTING_SIZE = 60;

    private int fallOffset = -300;
    private final Timer animationTimer = new Timer();
    private BufferedImage settingsIcon;
    private BufferedImage helpIcon;
    private BufferedImage soundIcon;

    public ScreenStart(ScreenStartListener listener) {
        this.listener = listener;
        setBackground(new Color(240, 248, 255));
        setPreferredSize(new Dimension(483, 483)); // 同步修改主菜单以适配正方形屏幕

        try {
            File settingsFile = new File("resources/settings.png");
            File helpFile = new File("resources/help.png");
            File soundFile = new File("resources/sound.png");

            if (settingsFile.exists()) settingsIcon = ImageIO.read(settingsFile);
            if (helpFile.exists()) helpIcon = ImageIO.read(helpFile);
            if (soundFile.exists()) soundIcon = ImageIO.read(soundFile);
        } catch (IOException e) {
            System.out.println("警告：找不到resources文件夹中的图标文件");
            settingsIcon = null;
            helpIcon = null;
            soundIcon = null;
        }

        initDecorMarbles();
        startAnimation();

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                startPressed = new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                settingPressed = new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my);
                repaint();

                if (startPressed) {
                    listener.onStartGame();
                }
                if (settingPressed) {
                    openSettings();
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

    private void updateHoverState(int mx, int my) {
        boolean oldStartHover = startHover;
        boolean oldSettingHover = settingHover;

        startHover = new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
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
        animationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (fallOffset < 0) {
                    fallOffset += 3;
                    repaint();
                } else {
                    this.cancel();
                }
            }
        }, 0, 16);
    }

    // [Bug 7] 修复：提供停止并在 GC 时清理 Timer 的机制
    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer.purge();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        enableHighQualityRender(g2d);

        int w = getWidth();
        int h = getHeight();
        startX = w / 2 - BTN_WIDTH / 2;
        startY = h / 2 - BTN_HEIGHT / 2 + 50; // 向下移动开始按钮

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
                new Color[]{new Color(230, 245, 255), new Color(190, 225, 255)}
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
        int baseY = h / 5 + offset;

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
        int titleY = h / 5 + offset;

        int[][] positions = {
                {centerX - 150, titleY - 60},
                {centerX + 150, titleY - 60},
                {centerX - 180, titleY},
                {centerX + 180, titleY},
                {centerX - 100, titleY + 50},
                {centerX, titleY + 70},
                {centerX + 100, titleY + 50},
                {centerX, titleY - 75}
        };

        for (int i = 0; i < decorMarbles.size(); i++) {
            Marble m = decorMarbles.get(i);
            m.setCenter(positions[i][0], positions[i][1]);
            m.setSide(28);
            m.draw(g2d);
        }
    }

    private void drawStartButton(Graphics2D g2d) {
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
        g2d.fillOval((int) cx - 6, (int) cy - 6, 12, 12);
    }

    private void openSettings() {
        JDialog settingsDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        settingsDialog.setSize(350, 250);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setResizable(false);
        settingsDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(240, 248, 255));

        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial Black", Font.BOLD, 28));
        titleLabel.setForeground(new Color(70, 150, 255));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 20));
        buttonPanel.setBackground(new Color(240, 248, 255));

        JButton soundButton = createStyledButtonStatic(isSoundOnStatic ? "Sound on" : "Sound off", true, "sound.png");
        soundButton.addActionListener(e -> {
            isSoundOnStatic = !isSoundOnStatic;
            soundButton.setText(isSoundOnStatic ? "Sound on" : "Sound off");
        });

        JButton helpButton = createStyledButtonStatic("How to play", true, "help.png");
        helpButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(settingsDialog,
                    "游戏玩法：\n1. 点击屏幕或按空格键发射弹珠\n2. 相同颜色的弹珠碰撞会消除\n3. 不要让弹珠堆到屏幕底部",
                    "How to play", JOptionPane.INFORMATION_MESSAGE);
        });

        buttonPanel.add(soundButton);
        buttonPanel.add(helpButton);

        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        settingsDialog.setContentPane(mainPanel);
        settingsDialog.setVisible(true);

        settingPressed = false;
        repaint();
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

    public static JButton createStyledButtonStatic(String text, boolean hasIcon, String iconName) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                Color c1 = getModel().isPressed() ? new Color(50, 140, 255) :
                        getModel().isRollover() ? new Color(100, 190, 255) : new Color(70, 150, 255);
                Color c2 = getModel().isPressed() ? new Color(30, 110, 255) :
                        getModel().isRollover() ? new Color(50, 140, 255) : new Color(30, 110, 255);

                LinearGradientPaint grad = new LinearGradientPaint(0, 0, 0, height,
                        new float[]{0, 1}, new Color[]{c1, c2});
                g2d.setPaint(grad);
                g2d.fillRoundRect(0, 0, width, height, 25, 25);

                g2d.setStroke(new BasicStroke(2f));
                g2d.setColor(Color.WHITE);
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 25, 25);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                String btnText = getText();
                int textWidth = fm.stringWidth(btnText);
                int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
                int textX = (width - textWidth) / 2;

                if (hasIcon) {
                    BufferedImage icon = loadIcon(iconName);
                    if (icon != null) {
                        g2d.drawImage(icon, 15, (height - 32) / 2, 32, 32, null);
                    }
                }

                g2d.drawString(btnText, textX, textY);
                g2d.dispose();
            }
        };

        button.setPreferredSize(new Dimension(250, 55));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
}