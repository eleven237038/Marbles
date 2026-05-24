import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;

public class StartMenu extends JPanel {
    // 回调接口，用于通知主类开始游戏
    public interface StartMenuListener {
        void onStartGame();
    }

    private final StartMenuListener listener;
    private final ArrayList<Marble> decorMarbles = new ArrayList<>();
    private boolean startHover = false;
    private boolean settingHover = false;
    private boolean startPressed = false; // 开始按钮按下状态
    private boolean settingPressed = false; // 设置按钮按下状态
    private boolean isSoundOn = true; // 声音开关状态

    private final int BTN_WIDTH = 200;
    private final int BTN_HEIGHT = 80;
    private int startX, startY;
    private int settingX, settingY;
    private final int SETTING_SIZE = 60;

    private int fallOffset = -300;
    private final Timer animationTimer = new Timer();
    private BufferedImage settingsIcon; // 设置图标图片

    public StartMenu(StartMenuListener listener) {
        this.listener = listener;
        setBackground(new Color(240, 248, 255));
        setPreferredSize(new Dimension(483, 1080));

        // 加载设置图标
        try {
            settingsIcon = ImageIO.read(new File("settings.png"));
        } catch (IOException e) {
            System.out.println("警告：找不到settings.png文件，将使用默认齿轮图标");
            settingsIcon = null;
        }

        initDecorMarbles();
        startAnimation();

        // 创建统一的鼠标事件适配器
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

            private void updateHoverState(int mx, int my) {
                boolean oldStartHover = startHover;
                boolean oldSettingHover = settingHover;

                startHover = new Rectangle(startX, startY, BTN_WIDTH, BTN_HEIGHT).contains(mx, my);
                settingHover = new Rectangle(settingX, settingY, SETTING_SIZE, SETTING_SIZE).contains(mx, my);

                // 设置鼠标光标
                if (startHover || settingHover) {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }

                // 只有当悬停状态改变时才重绘
                if (oldStartHover != startHover || oldSettingHover != settingHover) {
                    repaint();
                }
            }
        };

        // 同时添加两种监听器，确保所有鼠标事件都能被捕获
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
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

        // 添加按下状态的颜色变化，与设置窗口按钮一致
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

        // Play三角形图标
        int x = startX + BTN_WIDTH / 2;
        int y = startY + BTN_HEIGHT / 2;
        int[] xP = {x - 18, x + 18, x - 18};
        int[] yP = {y - 18, y, y + 18};
        g2d.fillPolygon(xP, yP, 3);
    }

    private void drawStandardGearButton(Graphics2D g2d, int h) {
        int x = settingX;
        int y = settingY;
        RoundRectangle2D btn = new RoundRectangle2D.Double(x, y, SETTING_SIZE, SETTING_SIZE, 20, 20);

        // 添加按下状态的颜色变化，与设置窗口按钮一致
        g2d.setColor(settingPressed ? new Color(50, 50, 200) :
                settingHover ? new Color(140, 140, 255) : new Color(110, 110, 245));
        g2d.fill(btn);
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(Color.WHITE);
        g2d.draw(btn);

        // 绘制设置图标
        if (settingsIcon != null) {
            // 缩放图片到按钮大小并居中绘制
            g2d.drawImage(settingsIcon, x + 5, y + 5, SETTING_SIZE - 10, SETTING_SIZE - 10, null);
        } else {
            // 备用：如果图片加载失败，显示原来的齿轮图标
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

    // 新的设置窗口实现（更大尺寸+英文按钮+声音开关）
    private void openSettings() {
        // 创建模态对话框（主窗口仍然可见，但需要先关闭设置窗口才能操作主窗口）
        JDialog settingsDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Settings", Dialog.ModalityType.APPLICATION_MODAL);
        settingsDialog.setSize(350, 250); // 窗口改大
        settingsDialog.setLocationRelativeTo(this); // 相对于主窗口居中
        settingsDialog.setResizable(false);
        settingsDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(240, 248, 255));

        // 标题标签
        JLabel titleLabel = new JLabel("Settings", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial Black", Font.BOLD, 28));
        titleLabel.setForeground(new Color(70, 150, 255));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 20));
        buttonPanel.setBackground(new Color(240, 248, 255));

        // 声音开关按钮
        JButton soundButton = createStyledButton(isSoundOn ? "Sound on" : "Sound off");
        soundButton.addActionListener(e -> {
            isSoundOn = !isSoundOn;
            soundButton.setText(isSoundOn ? "Sound on" : "Sound off");
            // 这里可以添加实际的声音控制逻辑
            // if (isSoundOn) { 开启声音 } else { 关闭声音 }
        });

        // 游戏帮助按钮
        JButton helpButton = createStyledButton("How to play");
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
    }

    // 创建与主菜单风格一致的按钮
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // 绘制渐变背景
                Color c1 = getModel().isPressed() ? new Color(50, 140, 255) :
                        getModel().isRollover() ? new Color(100, 190, 255) : new Color(70, 150, 255);
                Color c2 = getModel().isPressed() ? new Color(30, 110, 255) :
                        getModel().isRollover() ? new Color(50, 140, 255) : new Color(30, 110, 255);

                LinearGradientPaint grad = new LinearGradientPaint(0, 0, 0, height,
                        new float[]{0, 1}, new Color[]{c1, c2});
                g2d.setPaint(grad);
                g2d.fillRoundRect(0, 0, width, height, 25, 25);

                // 绘制白色边框
                g2d.setStroke(new BasicStroke(2f));
                g2d.setColor(Color.WHITE);
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 25, 25);

                // 绘制文字
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (width - fm.stringWidth(getText())) / 2;
                int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), textX, textY);

                g2d.dispose();
            }
        };

        button.setPreferredSize(new Dimension(250, 55)); // 按钮也相应变大
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }
}