import java.awt.*;
import java.awt.geom.*;

public class LaunchPad {
    public Point2D.Double cannon;
    private double headAngle = -Math.PI / 2;
    private int nextMarbleColor;
    private double topY;

    private static final double SCALE = 1.25;

    private static final int BASE_WIDTH = (int)(80 * SCALE);
    private static final int BASE_HEIGHT = (int)(45 * SCALE);
    private static final int TURRET_WIDTH = (int)(60 * SCALE);
    private static final int TURRET_HEIGHT = (int)(40 * SCALE);
    private static final double BARREL_LEN = 45 * SCALE;

    private static final int AMMO_SLOT_SIZE = (int)(16 * SCALE);
    private static final int AMMO_OFFSET_X = (int)(30 * SCALE);

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

    private static final Color[] MARBLE_COLORS = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200)
    };

    private double side;
    private int maxRowCount;

    public LaunchPad(double side, int maxRowCount) {
        this.side = side;
        this.maxRowCount = maxRowCount;
        this.cannon = new Point2D.Double();
        this.nextMarbleColor = 1;
    }

    private double calculateTopY() {
        return maxRowCount % 2 == 1 ?
                3 * ((maxRowCount - 1) / 2.0 + Math.sqrt(3) / 2) * side :
                3 * ((maxRowCount - 2) / 2.0 + Math.sqrt(3) / 2 + 0.5) * side;
    }

    public void setCannonPosition(int w, int h) {
        topY = calculateTopY();
        cannon.x = w / 2.0;
        // 整体下移 40 像素 (原 +70 → +110)
        cannon.y = topY + (int)(110 * SCALE);
    }

    public double getTopY() {
        return topY;
    }

    public Point2D.Double getMuzzlePosition() {
        double muzzleX = cannon.x + Math.cos(headAngle) * BARREL_LEN;
        double muzzleY = cannon.y + Math.sin(headAngle) * BARREL_LEN;
        return new Point2D.Double(muzzleX, muzzleY);
    }

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

    public void drawCannon(Graphics2D g, double mx, double my) {
        double dx = mx - cannon.x;
        double dy = my - cannon.y;
        if (dy < 0) {
            headAngle = Math.atan2(dy, dx);
            // 角度范围：-150° 到 -30°（更宽，覆盖虚线两端）
            double maxLeft = -Math.PI * 5 / 6;   // -150°
            double maxRight = -Math.PI / 6;      // -30°
            if (headAngle < maxLeft) headAngle = maxLeft;
            if (headAngle > maxRight) headAngle = maxRight;
        } else {
            headAngle = -Math.PI / 2;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // ---------- 底座 ----------
        int baseX = (int)(cannon.x - BASE_WIDTH/2);
        int baseY = (int)(cannon.y - BASE_HEIGHT/2);
        Point2D center = new Point2D.Double(cannon.x, cannon.y - BASE_HEIGHT/4);
        float[] stops = {0f, 0.7f, 1f};
        Color[] baseColors = {BASE_COLOR_TOP, BASE_COLOR_BOTTOM, new Color(200, 70, 20)};
        RadialGradientPaint baseGrad = new RadialGradientPaint(center, BASE_WIDTH/1.5f, stops, baseColors);
        g.setPaint(baseGrad);
        g.fillOval(baseX, baseY, BASE_WIDTH, BASE_HEIGHT);

        g.setColor(new Color(255, 255, 200, 120));
        g.setStroke(new BasicStroke((int)(2 * SCALE)));
        g.drawOval(baseX + (int)(2 * SCALE), baseY + (int)(2 * SCALE), BASE_WIDTH - (int)(4 * SCALE), BASE_HEIGHT - (int)(4 * SCALE));

        g.setColor(new Color(255, 255, 180));
        drawStar(g, cannon.x - BASE_WIDTH/2 - (int)(5 * SCALE), cannon.y - (int)(5 * SCALE), (int)(6 * SCALE));
        drawStar(g, cannon.x + BASE_WIDTH/2 + (int)(5 * SCALE), cannon.y - (int)(5 * SCALE), (int)(6 * SCALE));

        // ---------- 主炮塔 ----------
        int turretX = (int)(cannon.x - TURRET_WIDTH/2);
        int turretY = (int)(cannon.y - TURRET_HEIGHT/2);
        Point2D turretCenter = new Point2D.Double(cannon.x, cannon.y - (int)(5 * SCALE));
        RadialGradientPaint turretGrad = new RadialGradientPaint(turretCenter, TURRET_WIDTH/1.3f,
                new float[]{0f, 0.8f, 1f},
                new Color[]{TURRET_COLOR_TOP, TURRET_COLOR_BOTTOM, new Color(180, 80, 30)});
        g.setPaint(turretGrad);
        g.fillRoundRect(turretX, turretY, TURRET_WIDTH, TURRET_HEIGHT, (int)(20 * SCALE), (int)(20 * SCALE));

        g.setColor(new Color(255, 255, 220, 100));
        g.fillRoundRect(turretX + (int)(5 * SCALE), turretY + (int)(2 * SCALE), TURRET_WIDTH - (int)(10 * SCALE), (int)(8 * SCALE), (int)(5 * SCALE), (int)(5 * SCALE));

        // 眼睛
        int eyeRadius = (int)(9 * SCALE);
        int leftEyeX = (int)(cannon.x - 16 * SCALE);
        int leftEyeY = (int)(cannon.y - 12 * SCALE);
        int rightEyeX = (int)(cannon.x + 8 * SCALE);
        int rightEyeY = (int)(cannon.y - 12 * SCALE);

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

        // 猫耳
        g.setColor(new Color(255, 160, 80));
        int[] earXLeft = {
                (int)(cannon.x - 28 * SCALE),
                (int)(cannon.x - 38 * SCALE),
                (int)(cannon.x - 22 * SCALE)
        };
        int[] earYLeft = {
                (int)(cannon.y - 20 * SCALE),
                (int)(cannon.y - 32 * SCALE),
                (int)(cannon.y - 25 * SCALE)
        };
        g.fillPolygon(earXLeft, earYLeft, 3);
        int[] earXRight = {
                (int)(cannon.x + 20 * SCALE),
                (int)(cannon.x + 30 * SCALE),
                (int)(cannon.x + 14 * SCALE)
        };
        int[] earYRight = {
                (int)(cannon.y - 20 * SCALE),
                (int)(cannon.y - 32 * SCALE),
                (int)(cannon.y - 25 * SCALE)
        };
        g.fillPolygon(earXRight, earYRight, 3);

        // ---------- 炮管 ----------
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

        // ---------- 弹药舱 ----------
        int slotX = (int)(cannon.x + AMMO_OFFSET_X);
        int slotY = (int)(cannon.y - 12 * SCALE);
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

        // 漂浮小星星
        long time = System.currentTimeMillis();
        float angle1 = (time % 1000) / 1000f * (float)Math.PI * 2;
        drawStar(g, slotX + (int)(Math.cos(angle1) * 10 * SCALE), slotY + (int)(Math.sin(angle1) * 10 * SCALE), (int)(2 * SCALE));
        drawStar(g, slotX - (int)(Math.cos(angle1+2) * 8 * SCALE), slotY + (int)(Math.sin(angle1+1.5) * 8 * SCALE), (int)(2 * SCALE));

        // ---------- 瞄准辅助线：动态长度，恰好到达虚线（topY） ----------
        // 计算从炮塔 (cannon.x, cannon.y) 沿角度方向到 y = topY 所需的距离
        // 公式: distance = (cannon.y - topY) / (-sin(headAngle))
        double dyToLine = cannon.y - topY;
        double sinTheta = Math.sin(headAngle);
        double distanceToLine = dyToLine / (-sinTheta);
        // 防止负数或过大
        if (distanceToLine < 0) distanceToLine = dyToLine;
        int lineEndX = (int)(cannon.x + Math.cos(headAngle) * distanceToLine);
        int lineEndY = (int)(cannon.y + Math.sin(headAngle) * distanceToLine);
        // 确保终点 y 坐标严格为 topY（避免浮点误差）
        lineEndY = (int) topY;

        g.setColor(new Color(255, 100, 200, 100));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{(int)(6 * SCALE), (int)(8 * SCALE)}, 0));
        g.drawLine((int)cannon.x, (int)cannon.y, lineEndX, lineEndY);

        // 能量环（放在辅助线末端）
        g.setStroke(new BasicStroke((int)(3 * SCALE)));
        g.setColor(new Color(100, 200, 255, 180));
        g.drawOval(lineEndX - (int)(12 * SCALE), lineEndY - (int)(12 * SCALE), (int)(24 * SCALE), (int)(24 * SCALE));
        g.setColor(new Color(255, 180, 80, 200));
        g.drawOval(lineEndX - (int)(8 * SCALE), lineEndY - (int)(8 * SCALE), (int)(16 * SCALE), (int)(16 * SCALE));

        // 靶心
        g.setStroke(new BasicStroke((float)(1.5 * SCALE)));
        g.setColor(new Color(255, 50, 100, 200));
        g.drawOval(lineEndX - (int)(6 * SCALE), lineEndY - (int)(6 * SCALE), (int)(12 * SCALE), (int)(12 * SCALE));
        g.drawOval(lineEndX - (int)(2 * SCALE), lineEndY - (int)(2 * SCALE), (int)(4 * SCALE), (int)(4 * SCALE));
        g.fillOval(lineEndX - (int)(1 * SCALE), lineEndY - (int)(1 * SCALE), (int)(2 * SCALE), (int)(2 * SCALE));

        g.setStroke(new BasicStroke(1));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private void drawStar(Graphics2D g, double x, double y, int size) {
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        double outerR = size;
        double innerR = size * 0.4;
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2 * i / 10 - Math.PI / 2;
            double r = (i % 2 == 0) ? outerR : innerR;
            xPoints[i] = (int)(x + Math.cos(angle) * r);
            yPoints[i] = (int)(y + Math.sin(angle) * r);
        }
        g.setColor(new Color(255, 255, 200, 200));
        g.fillPolygon(xPoints, yPoints, 10);
        g.setColor(new Color(255, 200, 50));
        g.drawPolygon(xPoints, yPoints, 10);
    }

    public void setNextMarbleColorType(int type) {
        if (type >= 1 && type <= 4) this.nextMarbleColor = type;
    }
    public int getNextMarbleColorType() { return nextMarbleColor; }
    public void fire() { }
}