import java.awt.*;
import javax.swing.Timer;

/**
 * 独立计分板组件 (左侧空白区)
 * 适配全新的窗口宽度布局，使其优雅展示在左侧
 */
public class BoardScore extends javax.swing.JComponent {
    private int currentScore = 0;
    private int highScore = 0;

    private double starRotation = 0;
    private Timer animationTimer;

    // 适配左侧 250 宽度的计分板尺寸
    private int boardX = 25, boardY = 30;
    private int boardWidth = 200, boardHeight = 110;

    public BoardScore() {
        setOpaque(false);
        setFocusable(false);
        // 定位并限定组件尺寸
        setBounds(0, 0, boardX + boardWidth + 35, boardY + boardHeight + 35);
        startAnimation();
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

        // 阴影
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(boardX + 3, boardY + 3, boardWidth, boardHeight, 22, 22);

        // 背景
        g2d.setColor(new Color(255, 255, 250, 240));
        g2d.fillRoundRect(boardX, boardY, boardWidth, boardHeight, 22, 22);

        // 彩虹渐变边框
        LinearGradientPaint border = new LinearGradientPaint(boardX, boardY, boardX + boardWidth, boardY + boardHeight,
                new float[]{0, 0.25f, 0.5f, 0.75f, 1f},
                new Color[]{new Color(255, 90, 90), new Color(255, 180, 80),
                        new Color(80, 240, 120), new Color(100, 180, 255),
                        new Color(240, 100, 220)});
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.setPaint(border);
        g2d.drawRoundRect(boardX, boardY, boardWidth, boardHeight, 22, 22);

        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.drawRoundRect(boardX + 2, boardY + 2, boardWidth - 4, boardHeight - 4, 18, 18);

        // 标题文字
        Font textFont = new Font("Comic Sans MS", Font.BOLD, 15);
        g2d.setFont(textFont);
        String sText = "SCORE", bText = "BEST";
        FontMetrics fm = g2d.getFontMetrics();
        int sW = fm.stringWidth(sText), bW = fm.stringWidth(bText);
        int totalW = sW + 30 + bW;
        int startX = boardX + (boardWidth - totalW) / 2;

        g2d.setColor(new Color(60, 40, 100, 90));
        g2d.drawString(sText, startX + 1, boardY + 32);
        g2d.drawString(bText, startX + sW + 30 + 1, boardY + 32);

        LinearGradientPaint textGrad = new LinearGradientPaint(startX, boardY + 10, startX + totalW, boardY + 35,
                new float[]{0, 1}, new Color[]{new Color(180, 120, 255), new Color(80, 50, 130)});
        g2d.setPaint(textGrad);
        g2d.drawString(sText, startX, boardY + 32);
        g2d.drawString(bText, startX + sW + 30, boardY + 32);

        // 分数数字
        Font numFont = new Font("Arial Black", Font.BOLD, 24);
        g2d.setFont(numFont);
        String sVal = String.valueOf(currentScore), bVal = String.valueOf(highScore);
        fm = g2d.getFontMetrics();
        int svW = fm.stringWidth(sVal), bvW = fm.stringWidth(bVal);
        int valStartX = boardX + (boardWidth - (svW + 30 + bvW)) / 2;

        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.drawString(sVal, valStartX + 1, boardY + 78);
        g2d.drawString(bVal, valStartX + svW + 30 + 1, boardY + 78);

        LinearGradientPaint numGrad = new LinearGradientPaint(valStartX, boardY + 50, valStartX + totalW, boardY + 85,
                new float[]{0, 1}, new Color[]{new Color(255, 110, 30), new Color(180, 40, 0)});
        g2d.setPaint(numGrad);
        g2d.drawString(sVal, valStartX, boardY + 78);
        g2d.drawString(bVal, valStartX + svW + 30, boardY + 78);

        drawStar(g2d, boardX + boardWidth - 18, boardY + 18, 11, starRotation);
    }

    private static final int[] STAR_X = new int[10];
    private static final int[] STAR_Y = new int[10];

    private void drawStar(Graphics2D g, double x, double y, int size, double angle) {
        for (int i = 0; i < 10; i++) {
            double rad = Math.PI * 2 * i / 10 + angle;
            double r = (i % 2 == 0) ? size : size * 0.4;
            STAR_X[i] = (int) (x + Math.cos(rad) * r);
            STAR_Y[i] = (int) (y + Math.sin(rad) * r);
        }
        g.setColor(new Color(255, 215, 0, 220));
        g.fillPolygon(STAR_X, STAR_Y, 10);
        g.setColor(new Color(255, 100, 0, 180));
        g.drawPolygon(STAR_X, STAR_Y, 10);
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private void startAnimation() {
        animationTimer = new Timer(30, e -> {
            starRotation += 0.05;
            repaint();
        });
        animationTimer.start();
    }
}