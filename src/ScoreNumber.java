import java.awt.*;

public class ScoreNumber {
    private double x, y;
    private String scoreText;
    private float alpha = 1.0f;
    private float scale = 0.5f;
    private float yOffset = 0;
    private long startTime;
    private long delayTime = 0;
    private static final long DURATION = 800;
    private static final long TOTAL_DELAY = 250;

    private Color textColor;
    private boolean isTotalScore;
    private int scoreValue;
    private Color totalBaseColor;  // 总分数字的基础颜色

    // 单个弹珠得分构造函数
    public ScoreNumber(double x, double y, int score, Color marbleColor) {
        this.x = x;
        this.y = y;
        this.scoreText = "+" + score;
        this.startTime = System.currentTimeMillis();
        this.textColor = marbleColor;
        this.isTotalScore = false;
        this.scoreValue = score;
    }

    // 总分数字构造函数 - 只显示数字，使用同一种颜色渐变
    public ScoreNumber(double x, double y, int totalScore) {
        this.x = x;
        this.y = y;
        this.scoreText = "+" + totalScore;  // 只显示数字，不加TOTAL前缀
        this.startTime = System.currentTimeMillis() + TOTAL_DELAY;
        this.delayTime = TOTAL_DELAY;
        this.isTotalScore = true;
        this.scoreValue = totalScore;
        // 根据总分大小选择基础色相（色相值0-360，避开红色区域0-15和340-360）
        this.totalBaseColor = getBaseColorByScore(totalScore);
    }

    // 根据总分获取基础颜色（同一种颜色的不同程度，不使用红色）
    private Color getBaseColorByScore(int score) {
        if (score <= 30) {
            // 青色系
            return new Color(0, 200, 200);
        } else if (score <= 60) {
            // 金色/橙黄色系
            return new Color(255, 200, 50);
        } else if (score <= 100) {
            // 紫色系
            return new Color(160, 100, 220);
        } else {
            // 亮金色系
            return new Color(255, 180, 80);
        }
    }

    // 获取渐变颜色（同一色相，不同亮度和饱和度）
    private Color getGradientColor(float progress) {
        int r, g, b;

        // 根据分数区间使用不同的渐变范围（同一色相，从亮到更亮或从淡到浓）
        if (scoreValue <= 30) {
            // 青色系：淡青 -> 亮青 -> 深青
            r = (int)(80 + (0 - 80) * progress);
            g = (int)(180 + (255 - 180) * progress);
            b = (int)(180 + (200 - 180) * progress);
        } else if (scoreValue <= 60) {
            // 金色系：淡金 -> 亮金 -> 橙金
            r = (int)(200 + (255 - 200) * progress);
            g = (int)(150 + (215 - 150) * progress);
            b = (int)(30 + (50 - 30) * progress);
        } else if (scoreValue <= 100) {
            // 紫色系：淡紫 -> 亮紫 -> 深紫
            r = (int)(120 + (180 - 120) * progress);
            g = (int)(80 + (120 - 80) * progress);
            b = (int)(200 + (255 - 200) * progress);
        } else {
            // 金色系：亮金 -> 彩虹金
            r = (int)(220 + (255 - 220) * progress);
            g = (int)(160 + (215 - 160) * progress);
            b = (int)(60 + (100 - 60) * progress);
        }

        return new Color(safeComponent(r), safeComponent(g), safeComponent(b));
    }

    private int safeComponent(int value) {
        if (value < 0) return 0;
        if (value > 255) return 255;
        return value;
    }

    public boolean update() {
        long currentTime = System.currentTimeMillis();
        if (currentTime < startTime) {
            return true;
        }

        long elapsed = currentTime - startTime;
        if (elapsed >= DURATION) {
            return false;
        }

        float progress = (float) elapsed / DURATION;
        alpha = 1.0f - progress;
        scale = 0.5f + progress * 1.0f;
        yOffset = -progress * 40;

        return true;
    }

    public void draw(Graphics2D g2d) {
        long currentTime = System.currentTimeMillis();
        if (currentTime < startTime) {
            return;
        }

        Composite originalComposite = g2d.getComposite();

        float safeAlpha = alpha;
        if (safeAlpha < 0) safeAlpha = 0;
        if (safeAlpha > 1) safeAlpha = 1;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeAlpha));

        int fontSize = isTotalScore ? (int)(28 * scale) : (int)(18 * scale);
        Font font = new Font("Arial Black", Font.BOLD, fontSize);
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(scoreText);

        // 计算当前颜色
        Color drawColor;
        if (isTotalScore) {
            long elapsed = currentTime - startTime;
            if (elapsed < 0) elapsed = 0;
            float progress = (float) elapsed / DURATION;
            if (progress < 0) progress = 0;
            if (progress > 1) progress = 1;
            drawColor = getGradientColor(progress);
        } else {
            drawColor = textColor;
        }

        // 绘制阴影
        int shadowAlpha = (int)(80 * alpha);
        if (shadowAlpha < 0) shadowAlpha = 0;
        if (shadowAlpha > 255) shadowAlpha = 255;
        g2d.setColor(new Color(0, 0, 0, shadowAlpha));
        g2d.drawString(scoreText, (int)(x - textWidth / 2 + 2), (int)(y + yOffset + 2));

        // 绘制描边效果
        if (isTotalScore && alpha > 0.6f) {
            g2d.setColor(new Color(255, 255, 200, (int)(40 * alpha)));
            g2d.drawString(scoreText, (int)(x - textWidth / 2 - 1), (int)(y + yOffset - 1));
            g2d.drawString(scoreText, (int)(x - textWidth / 2 + 1), (int)(y + yOffset + 1));
        }

        // 绘制主文字
        g2d.setColor(drawColor);
        g2d.drawString(scoreText, (int)(x - textWidth / 2), (int)(y + yOffset));

        // 为总分数字添加发光效果
        if (isTotalScore && alpha > 0.5f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeAlpha * 0.25f));
            g2d.setColor(new Color(255, 255, 200));
            g2d.drawString(scoreText, (int)(x - textWidth / 2), (int)(y + yOffset - 2));
            g2d.drawString(scoreText, (int)(x - textWidth / 2), (int)(y + yOffset + 2));
        }

        g2d.setComposite(originalComposite);
    }
}