import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.Timer;

/**
 * 独立计分板组件（左上角）
 * SCORE 和 BEST 同行，分数在下方，带装饰弹珠和星星动画
 * 优化：确保完整显示在窗口左上角，不超出边界
 */
public class ScoreBoard extends javax.swing.JComponent {
    private int currentScore = 0;
    private int highScore = 0;

    private List<DecorationMarble> decorationMarbles = new ArrayList<>();
    private double starRotation = 0;
    private Timer animationTimer;
    private Random random = new Random();

    // 计分板位置和尺寸 - 向右向下移动，确保装饰弹珠完整显示
    private int boardX = 20, boardY = 20;      // 从 (8,8) 改为 (20,20)
    private int boardWidth = 175, boardHeight = 85;

    public ScoreBoard() {
        setOpaque(false);
        setFocusable(false);
        // 设置组件边界，确保完整显示（扩大边界以容纳装饰弹珠）
        setBounds(0, 0, boardX + boardWidth + 35, boardY + boardHeight + 35);
        initDecorationMarbles();
        startAnimation();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        // 更新装饰弹珠位置
        updateMarblePositions();
    }

    private void initDecorationMarbles() {
        // 调整装饰弹珠位置，确保不超出左边界和上边界
        int[][] positions = {
                {boardX + boardWidth / 2, boardY - 10},           // 正上方
                {boardX - 8, boardY + boardHeight / 2},          // 正左方
                {boardX + boardWidth / 2, boardY + boardHeight + 8}, // 正下方
                {boardX + boardWidth + 8, boardY + boardHeight / 2}, // 正右方
                {boardX - 6, boardY + 8},                        // 左上角外
                {boardX + boardWidth + 6, boardY + 8},           // 右上角外
                {boardX - 6, boardY + boardHeight - 8},          // 左下角外
                {boardX + boardWidth + 6, boardY + boardHeight - 8} // 右下角外
        };
        decorationMarbles.clear();
        for (int[] pos : positions) {
            decorationMarbles.add(new DecorationMarble(pos[0], pos[1], random.nextInt(4) + 1));
        }
    }

    private void startAnimation() {
        animationTimer = new Timer(30, e -> {
            starRotation += 0.05;
            for (DecorationMarble m : decorationMarbles) m.update(0.03);
            updateMarblePositions();
            repaint();
        });
        animationTimer.start();
    }

    private void updateMarblePositions() {
        int[][] positions = {
                {boardX + boardWidth / 2, boardY - 10},
                {boardX - 8, boardY + boardHeight / 2},
                {boardX + boardWidth / 2, boardY + boardHeight + 8},
                {boardX + boardWidth + 8, boardY + boardHeight / 2},
                {boardX - 6, boardY + 8},
                {boardX + boardWidth + 6, boardY + 8},
                {boardX - 6, boardY + boardHeight - 8},
                {boardX + boardWidth + 6, boardY + boardHeight - 8}
        };
        for (int i = 0; i < decorationMarbles.size() && i < positions.length; i++) {
            decorationMarbles.get(i).x = positions[i][0];
            decorationMarbles.get(i).y = positions[i][1];
        }
    }

    public void updateScore(int score) {
        currentScore = score;
        repaint();
    }

    public void updateHighScore(int high) {
        highScore = high;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 阴影（减小偏移量，避免超出边界）
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(boardX + 2, boardY + 2, boardWidth, boardHeight, 18, 18);

        // 半透背景
        g2d.setColor(new Color(255, 255, 240, 235));
        g2d.fillRoundRect(boardX, boardY, boardWidth, boardHeight, 18, 18);

        // 彩虹边框
        LinearGradientPaint border = new LinearGradientPaint(boardX, boardY, boardX + boardWidth, boardY + boardHeight,
                new float[]{0, 0.25f, 0.5f, 0.75f, 1f},
                new Color[]{new Color(255, 100, 100), new Color(255, 200, 100),
                        new Color(100, 255, 100), new Color(100, 200, 255),
                        new Color(255, 100, 200)});
        g2d.setStroke(new BasicStroke(2.2f));
        g2d.setPaint(border);
        g2d.drawRoundRect(boardX, boardY, boardWidth, boardHeight, 18, 18);

        // 内边框光效
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawRoundRect(boardX + 2, boardY + 2, boardWidth - 4, boardHeight - 4, 14, 14);

        // 文字：SCORE 和 BEST 同一行
        Font textFont = new Font("Comic Sans MS", Font.BOLD, 14);
        g2d.setFont(textFont);
        String sText = "SCORE", bText = "BEST";
        FontMetrics fm = g2d.getFontMetrics();
        int sW = fm.stringWidth(sText), bW = fm.stringWidth(bText);
        int totalW = sW + 16 + bW;
        int startX = boardX + (boardWidth - totalW) / 2;

        // 阴影
        g2d.setColor(new Color(80, 50, 120, 100));
        g2d.drawString(sText, startX + 1, boardY + 27);
        g2d.drawString(bText, startX + sW + 16 + 1, boardY + 27);

        // 渐变文字
        LinearGradientPaint textGrad = new LinearGradientPaint(startX, boardY + 10, startX + totalW, boardY + 32,
                new float[]{0, 1}, new Color[]{new Color(220, 180, 255), new Color(100, 70, 140)});
        g2d.setPaint(textGrad);
        g2d.drawString(sText, startX, boardY + 27);
        g2d.drawString(bText, startX + sW + 16, boardY + 27);

        // 分数数字
        Font numFont = new Font("Arial Black", Font.BOLD, 22);
        g2d.setFont(numFont);
        String sVal = String.valueOf(currentScore), bVal = String.valueOf(highScore);
        fm = g2d.getFontMetrics();
        int svW = fm.stringWidth(sVal), bvW = fm.stringWidth(bVal);
        int valStartX = boardX + (boardWidth - (svW + 16 + bvW)) / 2;

        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.drawString(sVal, valStartX + 1, boardY + 63);
        g2d.drawString(bVal, valStartX + svW + 16 + 1, boardY + 63);

        LinearGradientPaint numGrad = new LinearGradientPaint(valStartX, boardY + 45, valStartX + totalW, boardY + 68,
                new float[]{0, 1}, new Color[]{new Color(255, 120, 40), new Color(180, 60, 0)});
        g2d.setPaint(numGrad);
        g2d.drawString(sVal, valStartX, boardY + 63);
        g2d.drawString(bVal, valStartX + svW + 16, boardY + 63);

        // 旋转星星（放在右上角内部）
        drawStar(g2d, boardX + boardWidth - 16, boardY + 16, 10, starRotation);

        // 装饰弹珠
        for (DecorationMarble dm : decorationMarbles) dm.draw(g2d);
    }

    private void drawStar(Graphics2D g, double x, double y, int size, double angle) {
        int[] xp = new int[10], yp = new int[10];
        for (int i = 0; i < 10; i++) {
            double rad = Math.PI * 2 * i / 10 + angle;
            double r = (i % 2 == 0) ? size : size * 0.4;
            xp[i] = (int) (x + Math.cos(rad) * r);
            yp[i] = (int) (y + Math.sin(rad) * r);
        }
        g.setColor(new Color(255, 215, 0, 220));
        g.fillPolygon(xp, yp, 10);
        g.setColor(new Color(255, 100, 0, 180));
        g.drawPolygon(xp, yp, 10);
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private class DecorationMarble {
        double x, y;
        int colorType;
        double floatOffset = 0, floatSpeed, phase;

        DecorationMarble(double x, double y, int color) {
            this.x = x;
            this.y = y;
            this.colorType = color;
            this.floatSpeed = 0.5 + Math.random() * 1.2;
            this.phase = Math.random() * Math.PI * 2;
        }

        void update(double dt) {
            floatOffset += floatSpeed * dt;
            if (floatOffset > Math.PI * 2) floatOffset -= Math.PI * 2;
        }

        void draw(Graphics2D g) {
            double r = 9;
            double cx = x;
            double cy = y + Math.sin(floatOffset + phase) * 2.5;

            Color base, bright, dark;
            switch (colorType) {
                case 1:
                    base = new Color(220, 30, 30);
                    bright = new Color(255, 130, 130);
                    dark = new Color(120, 10, 10);
                    break;
                case 2:
                    base = new Color(20, 80, 220);
                    bright = new Color(110, 190, 255);
                    dark = new Color(10, 40, 120);
                    break;
                case 3:
                    base = new Color(240, 200, 20);
                    bright = new Color(255, 250, 180);
                    dark = new Color(160, 120, 0);
                    break;
                default:
                    base = new Color(160, 30, 200);
                    bright = new Color(220, 130, 255);
                    dark = new Color(90, 10, 120);
            }

            RadialGradientPaint grad = new RadialGradientPaint(
                    new Point2D.Double(cx, cy), (float) r,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{bright, base, dark}
            );
            g.setPaint(grad);
            g.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));

            // 高光
            g.setColor(new Color(255, 255, 255, 160));
            g.fillOval((int) (cx - r * 0.35), (int) (cy - r * 0.35), (int) (r * 0.45), (int) (r * 0.45));
        }
    }
}