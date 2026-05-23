import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainMenu extends JPanel {
    private final JFrame frame;
    private final ArrayList<Marble> decorMarbles = new ArrayList<>();
    private boolean startHover = false;
    private boolean settingHover = false;

    // 缩短后的开始按钮尺寸
    private final int BTN_WIDTH = 180;
    private final int BTN_HEIGHT = 70;
    private int startX, startY;
    private final int SETTING_SIZE = 60;

    // 动画参数
    private int fallOffset = -250;
    private final Timer animationTimer = new Timer();

    public MainMenu() {
        frame = new JFrame("弹珠游戏 - 主菜单");
        frame.setSize(900, 650);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.add(this);
        setBackground(new Color(240, 248, 255));

        initDecorMarbles();
        startAnimation();

        // 鼠标交互
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                if (new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my)) {
                    JOptionPane.showMessageDialog(frame, "开始游戏！");
                    frame.dispose();
                }
                if (new Rectangle(30, getHeight() - SETTING_SIZE - 30, SETTING_SIZE, SETTING_SIZE).contains(mx, my)) {
                    JOptionPane.showMessageDialog(frame, "打开设置");
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                startHover = new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                settingHover = new Rectangle(30, getHeight() - SETTING_SIZE - 30, SETTING_SIZE, SETTING_SIZE).contains(mx, my);
                repaint();
            }
        });

        frame.setVisible(true);
    }

    // 初始化8颗装饰弹珠
    private void initDecorMarbles() {
        for (int i = 0; i < 8; i++) {
            Marble m = new Marble();
            m.init(0, 0, 0, 0);
            decorMarbles.add(m);
        }
    }

    // 启动下落动画
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
        // 向下偏移35像素，按钮位置下移
        startY = h / 2 - BTN_HEIGHT / 2 + 35;

        drawBackground(g2d, w, h);
        drawLuxuryTitle(g2d, w, h, fallOffset);
        drawCompactMarbles(g2d, w, h, fallOffset);
        drawStartButton(g2d);
        drawStandardGearButton(g2d, h);
    }

    // 高清渲染
    private void enableHighQualityRender(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // 淡蓝色背景
    private void drawBackground(Graphics2D g2d, int w, int h) {
        LinearGradientPaint bg = new LinearGradientPaint(
                0, 0, 0, h,
                new float[]{0, 1},
                new Color[]{new Color(230, 245, 255), new Color(190, 225, 255)}
        );
        g2d.setPaint(bg);
        g2d.fillRect(0, 0, w, h);
    }

    // 精美艺术字（整体位置上移）
    private void drawLuxuryTitle(Graphics2D g2d, int w, int h, int offset) {
        Font titleFont = new Font("Arial Black", Font.BOLD, 100);
        g2d.setFont(titleFont);
        String title = "MARBLE";
        int baseX = w / 2 - 230;
        int baseY = h / 4 + offset;

        // 阴影
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.drawString(title, baseX + 6, baseY + 6);
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawString(title, baseX + 3, baseY + 3);

        // 渐变填充
        LinearGradientPaint textGrad = new LinearGradientPaint(
                baseX, baseY - 60, baseX + 460, baseY + 60,
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

        // 描边
        g2d.setStroke(new BasicStroke(3.5f));
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.drawString(title, baseX, baseY);

        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.drawString(title, baseX, baseY);
    }

    // 8颗弹珠：顶部+两侧+艺术字正下方分布，不遮挡按钮
    private void drawCompactMarbles(Graphics2D g2d, int w, int h, int offset) {
        int centerX = w / 2;
        int titleY = h / 4 + offset;

        // 8颗弹珠精准点位（总数不变，下方新增弹珠）
        int[][] positions = {
                {centerX - 220, titleY - 75},  // 左上
                {centerX + 220, titleY - 75},  // 右上
                {centerX - 260, titleY},       // 左侧
                {centerX + 260, titleY},       // 右侧
                {centerX - 150, titleY + 60},  // 左下
                {centerX - 50, titleY + 80},   // 正下方左
                {centerX + 50, titleY + 80},   // 正下方右
                {centerX + 150, titleY + 60}   // 右下
        };

        for (int i = 0; i < decorMarbles.size(); i++) {
            Marble m = decorMarbles.get(i);
            m.setCenter(positions[i][0], positions[i][1]);
            m.setSide(28);
            m.draw(g2d);
        }
    }

    // 缩短版开始按钮
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

        // 播放图标
        int x = startX + BTN_WIDTH / 2;
        int y = startY + BTN_HEIGHT / 2;
        int[] xP = {x - 18, x + 18, x - 18};
        int[] yP = {y - 18, y, y + 18};
        g2d.fillPolygon(xP, yP, 3);
    }

    // 标准齿轮设置按钮
    private void drawStandardGearButton(Graphics2D g2d, int h) {
        int x = 30;
        int y = h - SETTING_SIZE - 30;
        RoundRectangle2D btn = new RoundRectangle2D.Double(x, y, SETTING_SIZE, SETTING_SIZE, 20, 20);
        g2d.setColor(settingHover ? new Color(140,140,255) : new Color(110,110,245));
        g2d.fill(btn);
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        double cx = x + SETTING_SIZE/2.0;
        double cy = y + SETTING_SIZE/2.0;
        drawStandardGear(g2d, cx, cy, 22, 12, 8);
    }

    // 绘制标准齿轮
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::new);
    }
}