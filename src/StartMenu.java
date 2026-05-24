import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class StartMenu extends JPanel {
    public interface StartMenuListener {
        void onStartGame();
    }

    private final StartMenuListener listener;
    private final ArrayList<Marble> decorMarbles = new ArrayList<>();
    private boolean startHover = false;
    private boolean settingHover = false;
    private boolean exitHover = false;

    private final int BTN_WIDTH = 180;
    private final int BTN_HEIGHT = 70;
    private int startX, startY;
    private int settingX, settingY;
    private int exitX, exitY;
    private final int SETTING_SIZE = 60;
    private static final Font TITLE_FONT = new Font("Arial Black", Font.BOLD, 54);

    private int fallOffset = -350;
    private static final int ANIMATION_RANGE = 350;
    private static final int ANIMATION_STEP = 3;
    private static final int TIMER_INTERVAL = 16;
    private final Timer animationTimer = new Timer();

    public StartMenu(StartMenuListener listener) {
        this.listener = listener;
        setBackground(new Color(240, 248, 255));
        setPreferredSize(new Dimension(483, 1080));

        initDecorMarbles();
        startAnimation();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                if (new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my)) {
                    listener.onStartGame();
                }
                if (new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my)) {
                    openSettings();
                }
                if (new Rectangle(exitX, exitY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my)) {
                    System.exit(0);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                boolean newStartHover = new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                boolean newSettingHover = new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my);
                boolean newExitHover = new Rectangle(exitX, exitY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                if (newStartHover != startHover || newSettingHover != settingHover || newExitHover != exitHover) {
                    startHover = newStartHover;
                    settingHover = newSettingHover;
                    exitHover = newExitHover;
                    repaint();
                }
            }
        });
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
                    fallOffset += ANIMATION_STEP;
                    repaint();
                } else {
                    this.cancel();
                }
            }
        }, 0, TIMER_INTERVAL);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        enableHighQualityRender(g2d);

        int w = getWidth();
        int h = getHeight();
        startX = w / 2 - BTN_WIDTH / 2;
        startY = h / 2 - BTN_HEIGHT / 2 + 35;

        settingX = 30;
        settingY = h - SETTING_SIZE - 30;

        exitX = w / 2 - BTN_WIDTH / 2;
        exitY = startY + BTN_HEIGHT + 20;

        drawBackground(g2d, w, h);
        drawLuxuryTitle(g2d, w, h, fallOffset);
        drawCompactMarbles(g2d, w, h, fallOffset);
        drawStartButton(g2d);
        drawExitButton(g2d);
        drawStandardGearButton(g2d, h);
    }

    private void enableHighQualityRender(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
        int baseX = (w - titleWidth) / 2;
        int baseY = h / 5 + offset;

        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.drawString(title, baseX + 4, baseY + 4);
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawString(title, baseX + 2, baseY + 2);

        LinearGradientPaint textGrad = new LinearGradientPaint(
                baseX, baseY - 35, baseX + titleWidth, baseY + 35,
                new float[]{0, 0.25f, 0.5f, 0.75f, 1f},
                new Color[]{
                        new Color(255, 70, 70),
                        new Color(255, 180, 50),
                        new Color(50, 200, 255),
                        new Color(180, 70, 255),
                        new Color(255, 90, 150)
                }
        );
        g2d.setPaint(textGrad);
        g2d.drawString(title, baseX, baseY);

        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.drawString(title, baseX, baseY);

        g2d.setStroke(new BasicStroke(1f));
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.drawString(title, baseX, baseY);
    }

    private void drawCompactMarbles(Graphics2D g2d, int w, int h, int offset) {
        int centerX = w / 2;
        int titleY = h / 5 + offset;

        // 初始位置
        int[][] startPositions = {
                {centerX - 100, titleY - 400},
                {centerX + 100, titleY - 400},
                {centerX - 160, titleY - 380},
                {centerX + 160, titleY - 380},
                {centerX - 60, titleY - 360},
                {centerX, titleY - 420},
                {centerX + 60, titleY - 360},
                {centerX, titleY - 480}
        };

        // 停止位置
        int[][] endPositions = {
                {centerX - 100, titleY - 85},
                {centerX + 100, titleY - 85},
                {centerX - 155, titleY - 25},
                {centerX + 155, titleY - 25},
                {centerX - 55, titleY + 50},
                {centerX, titleY + 70},
                {centerX + 55, titleY + 50},
                {centerX, titleY - 130}
        };

        for (int i = 0; i < decorMarbles.size(); i++) {
            Marble m = decorMarbles.get(i);
            float progress = offset >= 0 ? 1.0f : Math.min(1.0f, (offset + ANIMATION_RANGE) / (float) ANIMATION_RANGE);
            int cx = (int) (startPositions[i][0] + (endPositions[i][0] - startPositions[i][0]) * progress);
            int cy = (int) (startPositions[i][1] + (endPositions[i][1] - startPositions[i][1]) * progress);
            m.setCenter(cx, cy);
            m.setSide(26);
            m.draw(g2d);
        }
    }

    private void drawStartButton(Graphics2D g2d) {
        RoundRectangle2D btn = new RoundRectangle2D.Double(startX, startY, BTN_WIDTH, BTN_HEIGHT, 35, 35);
        Color c1 = startHover ? new Color(100, 190, 255) : new Color(70, 150, 255);
        Color c2 = startHover ? new Color(50, 140, 255) : new Color(30, 110, 255);
        LinearGradientPaint btnGrad = new LinearGradientPaint(startX, startY, startX, startY + BTN_HEIGHT,
                new float[]{0, 1}, new Color[]{c1, c2});
        g2d.setPaint(btnGrad);
        g2d.fill(btn);

        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        int x = startX + BTN_WIDTH / 2;
        int y = startY + BTN_HEIGHT / 2;
        int[] xP = {x - 16, x + 16, x - 16};
        int[] yP = {y - 16, y, y + 16};
        g2d.fillPolygon(xP, yP, 3);
    }

    private void drawExitButton(Graphics2D g2d) {
        RoundRectangle2D btn = new RoundRectangle2D.Double(exitX, exitY, BTN_WIDTH, BTN_HEIGHT, 35, 35);
        Color c1 = exitHover ? new Color(255, 100, 100) : new Color(200, 60, 60);
        Color c2 = exitHover ? new Color(180, 40, 40) : new Color(150, 30, 30);
        LinearGradientPaint btnGrad = new LinearGradientPaint(exitX, exitY, exitX, exitY + BTN_HEIGHT,
                new float[]{0, 1}, new Color[]{c1, c2});
        g2d.setPaint(btnGrad);
        g2d.fill(btn);

        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String text = "退出";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, exitX + (BTN_WIDTH - textWidth) / 2, exitY + BTN_HEIGHT / 2 + 8);
    }

    private void drawStandardGearButton(Graphics2D g2d, int h) {
        int x = settingX;
        int y = settingY;
        RoundRectangle2D btn = new RoundRectangle2D.Double(x, y, SETTING_SIZE, SETTING_SIZE, 20, 20);
        g2d.setColor(settingHover ? new Color(140, 140, 255) : new Color(110, 110, 245));
        g2d.fill(btn);
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        double cx = x + SETTING_SIZE / 2.0;
        double cy = y + SETTING_SIZE / 2.0;
        drawStandardGear(g2d, cx, cy, 22, 12, 8);
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
        JOptionPane.showMessageDialog(this, "设置功能开发中...");
    }
}