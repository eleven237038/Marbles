import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class StartMenu extends JPanel {
    // 回调接口，用于通知主类开始游戏
    public interface StartMenuListener {
        void onStartGame();
    }

    private final StartMenuListener listener;
    private final ArrayList<Marble> decorMarbles = new ArrayList<>();
    private boolean startHover = false;
    private boolean settingHover = false;
    private boolean exitHover = false;

    private final int BTN_WIDTH = 200;
    private final int BTN_HEIGHT = 80;
    private int startX, startY;
    private int settingX, settingY;
    private int exitX, exitY;
    private final int SETTING_SIZE = 60;

    private int fallOffset = -300;
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
                startHover = new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                settingHover = new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my);
                exitHover = new Rectangle(exitX, exitY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                repaint();
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
                    fallOffset += 3;
                    repaint();
                } else {
                    this.cancel();
                }
            }
        }, 0, 16);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        enableHighQualityRender(g2d);

        int w = getWidth();
        int h = getHeight();
        startX = w / 2 - BTN_WIDTH / 2;
        startY = h / 2 - BTN_HEIGHT / 2;

        settingX = 30;
        settingY = h - SETTING_SIZE - 30;

        exitX = w / 2 - BTN_WIDTH / 2;
        exitY = startY + BTN_HEIGHT + 30;

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
        Font titleFont = new Font("Arial Black", Font.BOLD, 70);
        g2d.setFont(titleFont);
        String title = "MARBLE";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int baseX = w / 2 - titleWidth / 2;
        int baseY = h / 5 + offset;

        // 多层阴影效果
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.drawString(title, baseX + 6, baseY + 6);
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawString(title, baseX + 3, baseY + 3);

        // 彩虹渐变文字
        LinearGradientPaint textGrad = new LinearGradientPaint(
                baseX, baseY - 40, baseX + titleWidth, baseY + 40,
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

        // 白色描边
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.drawString(title, baseX, baseY);

        // 黑色细描边
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.drawString(title, baseX, baseY);
    }

    private void drawCompactMarbles(Graphics2D g2d, int w, int h, int offset) {
        int centerX = w / 2;
        int titleY = h / 5 + offset;

        // 完全按照您的要求修改：
        // 第8个弹珠x坐标与最下方中间弹珠相同(centerX)
        // y坐标改回到RB字母正上方，紧贴字母
        int[][] positions = {
                {centerX - 150, titleY - 60},
                {centerX + 150, titleY - 60},
                {centerX - 180, titleY},
                {centerX + 180, titleY},
                {centerX - 100, titleY + 50},
                {centerX, titleY + 70}, // 最下方中间弹珠
                {centerX + 100, titleY + 50},
                {centerX, titleY - 75}  // x=centerX(与最下方弹珠相同)，y=-75(RB正上方)
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
        Color c1 = startHover ? new Color(100, 190, 255) : new Color(70, 150, 255);
        Color c2 = startHover ? new Color(50, 140, 255) : new Color(30, 110, 255);
        LinearGradientPaint btnGrad = new LinearGradientPaint(startX, startY, startX, startY + BTN_HEIGHT,
                new float[]{0, 1}, new Color[]{c1, c2});
        g2d.setPaint(btnGrad);
        g2d.fill(btn);

        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        // Play三角形图标
        int x = startX + BTN_WIDTH / 2;
        int y = startY + BTN_HEIGHT / 2;
        int[] xP = {x - 18, x + 18, x - 18};
        int[] yP = {y - 18, y, y + 18};
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