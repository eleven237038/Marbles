import java.awt.*;
import java.awt.geom.Point2D;

/**
 * ScreenGame - 大炮/发射台渲染 + 计分板
 */
public class ScreenGame {
    private static final double SCALE = 1.25;
    private static final double BARREL_LEN = 45 * SCALE;
    private static final int AMMO_SLOT_SIZE = (int)(16 * SCALE);
    private static final double AMMO_OFFSET_X_RATIO = 0.375;

    private static final double BASE_RATIO_W = 1.0 / 4.8;
    private static final double BASE_RATIO_H = 0.45;

    private static final Color[] MARBLE_COLORS = {
        null,
        new Color(220, 30, 30),
        new Color(20, 80, 220),
        new Color(240, 200, 20),
        new Color(160, 30, 200)
    };

    private static final Color BASE_COLOR_TOP = new Color(255, 180, 80);
    private static final Color BASE_COLOR_BOTTOM = new Color(255, 100, 40);
    private static final Color TURRET_COLOR_TOP = new Color(255, 200, 110);
    private static final Color TURRET_COLOR_BOTTOM = new Color(255, 140, 60);
    private static final Color BARREL_COLOR_TOP = new Color(220, 220, 240);
    private static final Color BARREL_COLOR_BOTTOM = new Color(160, 170, 200);
    private static final Color BARREL_TIP_COLOR = new Color(255, 220, 120);
    private static final Color AMMO_BG_COLOR = new Color(70, 50, 80);
    private static final Color EYE_WHITE = new Color(255, 255, 255);
    private static final Color EYE_PUPIL = new Color(40, 40, 60);
    private static final Color EYE_HIGHLIGHT = new Color(255, 255, 255);

    // 角度边界计算相关常量
    private static final double ANGLE_CLAMP_EPSILON = -0.001;
    private static final double SQRT3 = Math.sqrt(3);

    public Point2D.Double cannon;
    private double headAngle = -Math.PI / 2;
    private int nextMarbleColor;
    private double topY;
    private int currentBaseWidth;
    private int currentBaseHeight;
    private double screenWidth;
    private double maxLeftAngle;
    private double maxRightAngle;
    private boolean angleBoundsCached = false;

    // 计分板数据
    private int currentScore = 0;
    private int highScore = 0;
    private int levelHighScore = 0;
    private int levelWinScore = 0;

    public ScreenGame() {
        this.cannon = new Point2D.Double();
        this.nextMarbleColor = 1;
    }

    public void setCannonPosition(int w, int h) {
        currentBaseWidth = (int)(w * BASE_RATIO_W);
        currentBaseHeight = (int)(currentBaseWidth * BASE_RATIO_H);
        screenWidth = w;

        // 仅在初始化时设定炮台的原始位置和deadline (topY)。后续绘制时不覆盖此数据。
        if (cannon.x == 0 && cannon.y == 0) {
            cannon.x = w / 2.0;
            cannon.y = h - (h / 5.0);
            topY = cannon.y - currentBaseHeight; // topY就是我们的deadline基准线
        }
    }

    public double getTopY() { return topY; }

    public Point2D.Double getMuzzlePosition() {
        double muzzleX = cannon.x + Math.cos(headAngle) * BARREL_LEN;
        double muzzleY = cannon.y + Math.sin(headAngle) * BARREL_LEN;
        return new Point2D.Double(muzzleX, muzzleY);
    }

    public void updateCannonAngle(double mx, double my) {
        double dx = mx - cannon.x;
        double dy = my - cannon.y;
        headAngle = Math.atan2(dy, dx);

        // 重新计算扇形边界角度
        recalculateAngleBounds();

        // 限制炮台射击角度在动态扇形边界内
        if (headAngle > 0) {
            if (headAngle > Math.PI / 2) {
                headAngle = maxLeftAngle;
            } else {
                headAngle = maxRightAngle;
            }
        } else {
            if (headAngle < maxLeftAngle) headAngle = maxLeftAngle;
            if (headAngle > maxRightAngle) headAngle = maxRightAngle;
        }
    }

    // 重新计算扇形边界角度（当炮台位置变化时调用）
    private void recalculateAngleBounds() {
        double leftDy = topY - cannon.y;
        double rightDy = topY - cannon.y;
        if (leftDy >= 0) leftDy = ANGLE_CLAMP_EPSILON;
        if (rightDy >= 0) rightDy = ANGLE_CLAMP_EPSILON;

        maxLeftAngle = Math.atan2(leftDy, 0 - cannon.x);
        maxRightAngle = Math.atan2(rightDy, screenWidth - cannon.x);
    }

    public int getNextMarbleColorType() { return nextMarbleColor; }
    public void setNextMarbleColorType(int type) {
        if (type >= 1 && type <= 4) this.nextMarbleColor = type;
    }

    public void updateScore(int score) {
        currentScore = score;
    }

    public void updateHighScore(int high) {
        highScore = high;
    }

    public void updateLevelScores(int current, int levelHigh, int levelWin) {
        currentScore = current;
        levelHighScore = levelHigh;
        levelWinScore = levelWin;
    }

    public int getCurrentScore() { return currentScore; }
    public int getHighScore() { return highScore; }
    public int getLevelHighScore() { return levelHighScore; }
    public int getLevelWinScore() { return levelWinScore; }

    public void drawLaunchPad(Graphics2D g, int w, int h) {
        setCannonPosition(w, h);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[]{10, 7}, 0));
        for (int i = 0; i < w; i += (int)(20 * SCALE)) {
            float hue = (float) (i / (float) w);
            Color rainbow = Color.getHSBColor(hue, 0.8f, 0.9f);
            g.setColor(rainbow);
            g.drawLine(i, (int) topY, Math.min(i + (int)(12 * SCALE), w), (int) topY);
        }
        g.setStroke(new BasicStroke(1));
    }

    public void drawScoreBoard(Graphics2D g, int leftZoneWidth, int totalHeight) {
        int boardX = 25, boardY = 20;
        int boardWidth = 200, boardHeight = 190;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 阴影
        g.setColor(new Color(0, 0, 0, 30));
        g.fillRoundRect(boardX + 3, boardY + 3, boardWidth, boardHeight, 22, 22);

        // 背景
        g.setColor(new Color(255, 255, 250, 240));
        g.fillRoundRect(boardX, boardY, boardWidth, boardHeight, 22, 22);

        // 彩虹渐变边框
        LinearGradientPaint border = new LinearGradientPaint(boardX, boardY, boardX + boardWidth, boardY + boardHeight,
                new float[]{0, 0.25f, 0.5f, 0.75f, 1f},
                new Color[]{new Color(255, 90, 90), new Color(255, 180, 80),
                        new Color(80, 240, 120), new Color(100, 180, 255),
                        new Color(240, 100, 220)});
        g.setStroke(new BasicStroke(3.0f));
        g.setPaint(border);
        g.drawRoundRect(boardX, boardY, boardWidth, boardHeight, 22, 22);

        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(255, 255, 255, 150));
        g.drawRoundRect(boardX + 2, boardY + 2, boardWidth - 4, boardHeight - 4, 18, 18);

        Font textFont = new Font("Comic Sans MS", Font.BOLD, 13);
        Font numFont = new Font("Arial Black", Font.BOLD, 20);
        FontMetrics fmText = g.getFontMetrics(textFont);
        FontMetrics fmNum = g.getFontMetrics(numFont);

        // ============ Level 标题 ============
        String levelText = "LEVEL " + Level.getInstance().getCurrentLevel();
        int levelTextX = boardX + (boardWidth - fmText.stringWidth(levelText)) / 2;
        g.setFont(textFont);
        g.setColor(new Color(60, 40, 100, 90));
        g.drawString(levelText, levelTextX + 1, boardY + 22);

        LinearGradientPaint textGrad = new LinearGradientPaint(boardX, boardY + 10, boardX + boardWidth, boardY + 25,
                new float[]{0, 1}, new Color[]{new Color(180, 120, 255), new Color(80, 50, 130)});
        g.setPaint(textGrad);
        g.drawString(levelText, levelTextX, boardY + 22);

        // ============ Target 目标分 ============
        String tText = "TARGET";
        int tTextX = boardX + 20;
        g.setFont(textFont);
        g.setColor(new Color(60, 40, 100, 90));
        g.drawString(tText, tTextX + 1, boardY + 45);
        g.setPaint(textGrad);
        g.drawString(tText, tTextX, boardY + 45);

        String tVal = String.valueOf(levelWinScore);
        int tValX = boardX + boardWidth - 20 - fmNum.stringWidth(tVal);
        g.setFont(numFont);
        g.setColor(new Color(0, 0, 0, 50));
        g.drawString(tVal, tValX + 2, boardY + 70);
        LinearGradientPaint numGradTarget = new LinearGradientPaint(boardX, boardY + 50, boardX + boardWidth, boardY + 75,
                new float[]{0, 1}, new Color[]{new Color(0, 200, 100), new Color(0, 120, 50)});
        g.setPaint(numGradTarget);
        g.drawString(tVal, tValX, boardY + 70);

        // ============ 分割线1 ============
        g.setColor(new Color(200, 200, 200, 120));
        g.drawLine(boardX + 15, boardY + 80, boardX + boardWidth - 15, boardY + 80);

        // ============ 当前分 SCORE ============
        String sText = "SCORE";
        int sTextX = boardX + (boardWidth - fmText.stringWidth(sText)) / 2;
        g.setFont(textFont);
        g.setColor(new Color(60, 40, 100, 90));
        g.drawString(sText, sTextX + 1, boardY + 102);
        g.setPaint(textGrad);
        g.drawString(sText, sTextX, boardY + 102);

        String sVal = String.valueOf(currentScore);
        int sValX = boardX + (boardWidth - fmNum.stringWidth(sVal)) / 2;
        g.setFont(numFont);
        g.setColor(new Color(0, 0, 0, 50));
        g.drawString(sVal, sValX + 2, boardY + 130);
        LinearGradientPaint numGrad2 = new LinearGradientPaint(boardX, boardY + 105, boardX + boardWidth, boardY + 135,
                new float[]{0, 1}, new Color[]{new Color(40, 150, 255), new Color(0, 80, 180)});
        g.setPaint(numGrad2);
        g.drawString(sVal, sValX, boardY + 130);

        // ============ 分割线2 ============
        g.setColor(new Color(200, 200, 200, 120));
        g.drawLine(boardX + 15, boardY + 138, boardX + boardWidth - 15, boardY + 138);

        // ============ 最高分 BEST ============
        String bText = "BEST";
        int bTextX = boardX + 20;
        g.setFont(textFont);
        g.setColor(new Color(60, 40, 100, 90));
        g.drawString(bText, bTextX + 1, boardY + 160);
        g.setPaint(textGrad);
        g.drawString(bText, bTextX, boardY + 160);

        String bVal = String.valueOf(levelHighScore);
        int bValX = boardX + boardWidth - 20 - fmNum.stringWidth(bVal);
        g.setFont(numFont);
        g.setColor(new Color(0, 0, 0, 50));
        g.drawString(bVal, bValX + 2, boardY + 185);
        LinearGradientPaint numGrad = new LinearGradientPaint(boardX, boardY + 165, boardX + boardWidth, boardY + 190,
                new float[]{0, 1}, new Color[]{new Color(255, 110, 30), new Color(180, 40, 0)});
        g.setPaint(numGrad);
        g.drawString(bVal, bValX, boardY + 185);
    }

    public void drawCannon(Graphics2D g, double mx, double my) {
        updateCannonAngle(mx, my);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int baseX = (int)(cannon.x - currentBaseWidth/2);
        int baseY = (int)(cannon.y - currentBaseHeight/2);
        Point2D center = new Point2D.Double(cannon.x, cannon.y - currentBaseHeight/4);
        float[] stops = {0f, 0.7f, 1f};
        Color[] baseColors = {BASE_COLOR_TOP, BASE_COLOR_BOTTOM, new Color(200, 70, 20)};
        RadialGradientPaint baseGrad = new RadialGradientPaint(center, currentBaseWidth/1.5f, stops, baseColors);
        g.setPaint(baseGrad);
        g.fillOval(baseX, baseY, currentBaseWidth, currentBaseHeight);

        g.setColor(new Color(255, 255, 200, 120));
        g.setStroke(new BasicStroke((int)(2 * SCALE)));
        g.drawOval(baseX + (int)(2 * SCALE), baseY + (int)(2 * SCALE), currentBaseWidth - (int)(4 * SCALE), currentBaseHeight - (int)(4 * SCALE));

        int turretWidth = (int)(currentBaseWidth * 0.75);
        int turretHeight = (int)(turretWidth * BASE_RATIO_H);
        int turretX = (int)(cannon.x - turretWidth/2);
        int turretY = (int)(cannon.y - turretHeight/2);
        Point2D turretCenter = new Point2D.Double(cannon.x, cannon.y - (int)(5 * SCALE));
        RadialGradientPaint turretGrad = new RadialGradientPaint(turretCenter, turretWidth/1.3f,
                new float[]{0f, 0.8f, 1f},
                new Color[]{TURRET_COLOR_TOP, TURRET_COLOR_BOTTOM, new Color(180, 80, 30)});
        g.setPaint(turretGrad);
        g.fillRoundRect(turretX, turretY, turretWidth, turretHeight, (int)(20 * SCALE), (int)(20 * SCALE));

        g.setColor(new Color(255, 255, 220, 100));
        g.fillRoundRect(turretX + (int)(5 * SCALE), turretY + (int)(2 * SCALE), turretWidth - (int)(10 * SCALE), (int)(8 * SCALE), (int)(5 * SCALE), (int)(5 * SCALE));

        int eyeRadius = (int)(currentBaseWidth * 0.1125);
        int leftEyeX = (int)(cannon.x - currentBaseWidth * 0.2);
        int leftEyeY = (int)(cannon.y - currentBaseWidth * 0.15);
        int rightEyeX = (int)(cannon.x + currentBaseWidth * 0.1);
        int rightEyeY = (int)(cannon.y - currentBaseWidth * 0.15);

        g.setColor(EYE_WHITE);
        g.fillOval(leftEyeX - eyeRadius, leftEyeY - eyeRadius/2, eyeRadius*2, eyeRadius);
        g.fillOval(rightEyeX - eyeRadius, rightEyeY - eyeRadius/2, eyeRadius*2, eyeRadius);

        double angleToMouse = Math.atan2(my - leftEyeY, mx - leftEyeX);
        double pupilOffsetX = Math.cos(angleToMouse) * (2.5 * SCALE);
        double pupilOffsetY = Math.sin(angleToMouse) * (2.5 * SCALE);
        g.setColor(EYE_PUPIL);
        g.fillOval((int)(leftEyeX - 3 * SCALE + pupilOffsetX), (int)(leftEyeY - 3 * SCALE + pupilOffsetY), (int)(6 * SCALE), (int)(6 * SCALE));
        g.fillOval((int)(rightEyeX - 3 * SCALE + pupilOffsetX), (int)(rightEyeY - 3 * SCALE + pupilOffsetY), (int)(6 * SCALE), (int)(6 * SCALE));

        g.setColor(EYE_HIGHLIGHT);
        g.fillOval((int)(leftEyeX - 1 * SCALE + pupilOffsetX), (int)(leftEyeY - 4 * SCALE + pupilOffsetY), (int)(3 * SCALE), (int)(3 * SCALE));
        g.fillOval((int)(rightEyeX - 1 * SCALE + pupilOffsetX), (int)(rightEyeY - 4 * SCALE + pupilOffsetY), (int)(3 * SCALE), (int)(3 * SCALE));

        g.setColor(new Color(255, 160, 80));
        int[] earXLeft = {
                (int)(cannon.x - currentBaseWidth * 0.35),
                (int)(cannon.x - currentBaseWidth * 0.475),
                (int)(cannon.x - currentBaseWidth * 0.275)
        };
        int[] earYLeft = {
                (int)(cannon.y - currentBaseWidth * 0.25),
                (int)(cannon.y - currentBaseWidth * 0.4),
                (int)(cannon.y - currentBaseWidth * 0.3125)
        };
        g.fillPolygon(earXLeft, earYLeft, 3);
        int[] earXRight = {
                (int)(cannon.x + currentBaseWidth * 0.25),
                (int)(cannon.x + currentBaseWidth * 0.375),
                (int)(cannon.x + currentBaseWidth * 0.175)
        };
        int[] earYRight = {
                (int)(cannon.y - currentBaseWidth * 0.25),
                (int)(cannon.y - currentBaseWidth * 0.4),
                (int)(cannon.y - currentBaseWidth * 0.3125)
        };
        g.fillPolygon(earXRight, earYRight, 3);

        int barrelStartX = (int)(cannon.x + Math.cos(headAngle) * (12 * SCALE));
        int barrelStartY = (int)(cannon.y + Math.sin(headAngle) * (12 * SCALE));
        int barrelEndX = (int)(cannon.x + Math.cos(headAngle) * BARREL_LEN);
        int barrelEndY = (int)(cannon.y + Math.sin(headAngle) * BARREL_LEN);

        Point2D barrelStart = new Point2D.Double(barrelStartX, barrelStartY);
        Point2D barrelEnd = new Point2D.Double(barrelEndX, barrelEndY);
        LinearGradientPaint barrelGrad = new LinearGradientPaint(barrelStart, barrelEnd,
                new float[]{0f, 1f}, new Color[]{BARREL_COLOR_TOP, BARREL_COLOR_BOTTOM});
        g.setStroke(new BasicStroke((int)(12 * SCALE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(barrelGrad);
        g.drawLine(barrelStartX, barrelStartY, barrelEndX, barrelEndY);

        g.setStroke(new BasicStroke((int)(15 * SCALE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 200, 100, 60));
        g.drawLine(barrelStartX, barrelStartY, barrelEndX, barrelEndY);

        int tipX = (int)(cannon.x + Math.cos(headAngle) * BARREL_LEN);
        int tipY = (int)(cannon.y + Math.sin(headAngle) * BARREL_LEN);
        g.setStroke(new BasicStroke(1));
        RadialGradientPaint tipGrad = new RadialGradientPaint(tipX, tipY, (int)(12 * SCALE),
                new float[]{0f, 1f}, new Color[]{BARREL_TIP_COLOR, new Color(255, 100, 30)});
        g.setPaint(tipGrad);
        g.fillOval(tipX - (int)(8 * SCALE), tipY - (int)(8 * SCALE), (int)(16 * SCALE), (int)(16 * SCALE));
        g.setColor(Color.WHITE);
        g.fillOval(tipX - (int)(3 * SCALE), tipY - (int)(3 * SCALE), (int)(6 * SCALE), (int)(6 * SCALE));

        int slotX = (int)(cannon.x + currentBaseWidth * AMMO_OFFSET_X_RATIO);
        int slotY = (int)(cannon.y - currentBaseWidth * 0.15);
        int size = AMMO_SLOT_SIZE;

        Point2D slotCenter = new Point2D.Double(slotX, slotY);
        RadialGradientPaint slotGrad = new RadialGradientPaint(slotCenter, size/2f,
                new float[]{0f, 0.6f, 1f}, new Color[]{new Color(160, 100, 180), AMMO_BG_COLOR, new Color(30, 20, 40)});
        g.setPaint(slotGrad);
        g.fillRoundRect(slotX - size/2, slotY - size/2, size, size, (int)(8 * SCALE), (int)(8 * SCALE));

        g.setColor(new Color(255, 215, 0, 200));
        g.setStroke(new BasicStroke((float)(1.5 * SCALE)));
        g.drawRoundRect(slotX - size/2, slotY - size/2, size, size, (int)(8 * SCALE), (int)(8 * SCALE));

        int marbleRadius = (int)(size * 0.4);
        Color marbleColor = MARBLE_COLORS[nextMarbleColor];
        if (marbleColor != null) {
            Point2D marbleCenter = new Point2D.Double(slotX, slotY);
            RadialGradientPaint marbleGrad = new RadialGradientPaint(marbleCenter, marbleRadius,
                    new float[]{0f, 0.7f, 1f}, new Color[]{marbleColor.brighter(), marbleColor, marbleColor.darker()});
            g.setPaint(marbleGrad);
            g.fillOval(slotX - marbleRadius, slotY - marbleRadius, marbleRadius*2, marbleRadius*2);
            g.setColor(new Color(0,0,0,60));
            g.drawOval(slotX - marbleRadius, slotY - marbleRadius, marbleRadius*2, marbleRadius*2);
            g.setColor(Color.WHITE);
            g.fillOval(slotX - marbleRadius/2, slotY - marbleRadius/2, marbleRadius/2, marbleRadius/2);
        }

        double dyToLine = cannon.y - topY;
        double sinTheta = Math.sin(headAngle);
        double distanceToLine = dyToLine / (-sinTheta);
        if (distanceToLine < 0) distanceToLine = dyToLine;
        int lineEndX = (int)(cannon.x + Math.cos(headAngle) * distanceToLine);
        int lineEndY = (int)(cannon.y + Math.sin(headAngle) * distanceToLine);
        lineEndY = (int) topY;

        g.setColor(new Color(255, 100, 200, 100));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{(int)(6 * SCALE), (int)(8 * SCALE)}, 0));
        g.drawLine((int)cannon.x, (int)cannon.y, lineEndX, lineEndY);

        g.setStroke(new BasicStroke((int)(3 * SCALE)));
        g.setColor(new Color(100, 200, 255, 180));
        g.drawOval(lineEndX - (int)(12 * SCALE), lineEndY - (int)(12 * SCALE), (int)(24 * SCALE), (int)(24 * SCALE));
        g.setColor(new Color(255, 180, 80, 200));
        g.drawOval(lineEndX - (int)(8 * SCALE), lineEndY - (int)(8 * SCALE), (int)(16 * SCALE), (int)(16 * SCALE));

        g.setStroke(new BasicStroke((float)(1.5 * SCALE)));
        g.setColor(new Color(255, 50, 100, 200));
        g.drawOval(lineEndX - (int)(6 * SCALE), lineEndY - (int)(6 * SCALE), (int)(12 * SCALE), (int)(12 * SCALE));
        g.drawOval(lineEndX - (int)(2 * SCALE), lineEndY - (int)(2 * SCALE), (int)(4 * SCALE), (int)(4 * SCALE));
        g.fillOval(lineEndX - (int)(1 * SCALE), lineEndY - (int)(1 * SCALE), (int)(2 * SCALE), (int)(2 * SCALE));

        g.setStroke(new BasicStroke(1));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    // 分数数字动画类
    public static class ScoreNumber {
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
        private Color totalBaseColor;

        public ScoreNumber(double x, double y, int score, Color marbleColor) {
            this.x = x;
            this.y = y;
            this.scoreText = "+" + score;
            this.startTime = System.currentTimeMillis();
            this.textColor = marbleColor;
            this.isTotalScore = false;
            this.scoreValue = score;
        }

        public ScoreNumber(double x, double y, int totalScore) {
            this.x = x;
            this.y = y;
            this.scoreText = "+" + totalScore;
            this.startTime = System.currentTimeMillis() + TOTAL_DELAY;
            this.delayTime = TOTAL_DELAY;
            this.isTotalScore = true;
            this.scoreValue = totalScore;
            this.totalBaseColor = getBaseColorByScore(totalScore);
        }

        private Color getBaseColorByScore(int score) {
            if (score <= 30) {
                return new Color(0, 200, 200);
            } else if (score <= 60) {
                return new Color(255, 200, 50);
            } else if (score <= 100) {
                return new Color(160, 100, 220);
            } else {
                return new Color(255, 180, 80);
            }
        }

        private Color getGradientColor(float progress) {
            int r, g, b;

            if (scoreValue <= 30) {
                r = (int)(80 + (0 - 80) * progress);
                g = (int)(180 + (255 - 180) * progress);
                b = (int)(180 + (200 - 180) * progress);
            } else if (scoreValue <= 60) {
                r = (int)(200 + (255 - 200) * progress);
                g = (int)(150 + (215 - 150) * progress);
                b = (int)(30 + (50 - 30) * progress);
            } else if (scoreValue <= 100) {
                r = (int)(120 + (180 - 120) * progress);
                g = (int)(80 + (120 - 80) * progress);
                b = (int)(200 + (255 - 200) * progress);
            } else {
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

            int shadowAlpha = (int)(80 * alpha);
            if (shadowAlpha < 0) shadowAlpha = 0;
            if (shadowAlpha > 255) shadowAlpha = 255;
            g2d.setColor(new Color(0, 0, 0, shadowAlpha));
            g2d.drawString(scoreText, (int)(x - textWidth / 2 + 2), (int)(y + yOffset + 2));

            if (isTotalScore && alpha > 0.6f) {
                g2d.setColor(new Color(255, 255, 200, (int)(40 * alpha)));
                g2d.drawString(scoreText, (int)(x - textWidth / 2 - 1), (int)(y + yOffset - 1));
                g2d.drawString(scoreText, (int)(x - textWidth / 2 + 1), (int)(y + yOffset + 1));
            }

            g2d.setColor(drawColor);
            g2d.drawString(scoreText, (int)(x - textWidth / 2), (int)(y + yOffset));

            if (isTotalScore && alpha > 0.5f) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, safeAlpha * 0.25f));
                g2d.setColor(new Color(255, 255, 200));
                g2d.drawString(scoreText, (int)(x - textWidth / 2), (int)(y + yOffset - 2));
                g2d.drawString(scoreText, (int)(x - textWidth / 2), (int)(y + yOffset + 2));
            }

            g2d.setComposite(originalComposite);
        }
    }
}