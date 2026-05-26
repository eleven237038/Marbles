import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;

public class Marble {
    private double cx, cy;
    private double side;
    private boolean initialized;
    private boolean verticesDirty;
    public static final int RED = 1;
    public static final int BLUE = 2;
    public static final int YELLOW = 3;
    public static final int PURPLE = 4;

    // 动画状态相关
    private boolean popping = false;
    private boolean falling = false;
    private boolean dead = false;
    private double popDelay = 0;
    private double popProgress = 0;

    // 掉落物理参数
    private double fallVy = 0;
    private double fallAy = 1500; // 重力加速度
    private static final double FALL_DEATH_Y = 1500;

    // 单独动画状态
    private boolean alone = false;
    private double aloneSlideVx = 0;
    private double aloneSlideVy = 0;
    private double aloneDelay = 0;

    // 得分标记 (新增)
    private boolean scored = false;

    // 主体色系
    private static final Color[] BASE_COLOR = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200)
    };
    private static final Color[] BRIGHT_COLOR = {
            null,
            new Color(255, 130, 130),
            new Color(110, 190, 255),
            new Color(255, 250, 180),
            new Color(220, 130, 255)
    };
    private static final Color[] DARK_COLOR = {
            null,
            new Color(120, 10, 10),
            new Color(10, 40, 120),
            new Color(160, 120, 0),
            new Color(90, 10, 120)
    };

    private static final Random random = new Random();
    private int colorType;
    private int row;
    private int col;
    private int[][] edgeAttachment;
    private boolean markedForRemove = false;

    public Marble() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;
        this.colorType = random.nextInt(4) + 1;
        this.row = 0;
        this.col = 0;
        this.edgeAttachment = new int[6][2];
        this.verticesDirty = false;
    }

    public void init(double cx, double cy, int row, int col) {
        this.cx = cx;
        this.cy = cy;
        this.row = row;
        this.col = col;
        this.initialized = true;
    }

    public void startPop(double delay) {
        this.popping = true;
        this.popDelay = delay;
        this.popProgress = 0;
    }

    public void startFalling(double delay) {
        this.falling = true;
        this.popping = false;
        this.alone = false;
        this.popDelay = delay;
        this.fallVy = -150 - (random.nextDouble() * 80);
    }

    public void startAlone(int triggerEdge, double delay) {
        this.alone = true;
        this.falling = false;
        this.popping = false;
        this.popDelay = delay;
        double angle = triggerEdge * Math.PI / 3.0;
        double slideSpeed = 120 + random.nextDouble() * 60;
        this.aloneSlideVx = Math.cos(angle) * slideSpeed;
        this.aloneSlideVy = -Math.abs(Math.sin(angle) * slideSpeed);
    }

    public boolean isAlone() { return alone; }
    public boolean isPopping() { return popping; }
    public boolean isFalling() { return falling; }
    public boolean isDead() { return dead; }
    public boolean isScored() { return scored; }
    public void setScored(boolean scored) { this.scored = scored; }

    public void update(double dt) {
        if (popping) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                popProgress += dt * 6.0;
                if (popProgress >= 1.0) {
                    dead = true;
                }
            }
        } else if (alone) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                if (aloneDelay < 0.3) {
                    cx += aloneSlideVx * dt;
                    cy += aloneSlideVy * dt;
                    aloneDelay += dt;
                    verticesDirty = true;
                } else {
                    cy += (400 + random.nextDouble() * 100) * dt;
                    verticesDirty = true;
                    if (cy > FALL_DEATH_Y) {
                        dead = true;
                    }
                }
            }
        } else if (falling) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                cy += fallVy * dt;
                fallVy += fallAy * dt;
                verticesDirty = true;
                if (cy > FALL_DEATH_Y) {
                    dead = true;
                }
            }
        }
    }

    public void setCenter(double cx, double cy) {
        this.cx = cx;
        this.cy = cy;
        this.verticesDirty = true;
    }

    public void recalculateVerticesIfDirty() {}
    public void setSide(double side) { this.side = side; }
    public double getCenterX() { return cx; }
    public double getCenterY() { return cy; }
    public double getSide() { return side; }
    public boolean isInitialized() { return initialized; }
    public int getColorType() { return colorType; }
    public void setColorType(int colorType) { this.colorType = colorType; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int[][] getEdgeAttachment() { return edgeAttachment; }
    public void setEdgeAttachment(int[][] edgeAttachment) { this.edgeAttachment = edgeAttachment; }

    public void markForRemove(boolean b) { markedForRemove = b; }
    public boolean isMarkedForRemove() { return markedForRemove; }

    public void draw(Graphics2D g) {
        if (!initialized || dead) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double scale = 1.0;
        float alpha = 1.0f;

        if (popping && popDelay <= 0) {
            scale = 1.0 + popProgress * 0.4;
            alpha = 1.0f - (float)popProgress;
            if (alpha < 0f) alpha = 0f;
            if (alpha > 1f) alpha = 1f;
        }

        Composite originalComposite = null;
        if (alpha < 1.0f) {
            originalComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        double radius = side * 0.866 * scale;
        double x = cx - radius;
        double y = cy - radius;
        double diameter = radius * 2;

        Color base = BASE_COLOR[colorType];
        Color bright = BRIGHT_COLOR[colorType];
        Color dark = DARK_COLOR[colorType];

        g.setColor(new Color(0,0,0,40));
        g.fillOval((int)(x + 2 * scale), (int)(y + 2 * scale), (int)diameter, (int)diameter);

        Point2D center = new Point2D.Double(cx, cy);
        float[] stop = {0f, 0.6f, 1f};
        Color[] gradColor = {bright, base, dark};
        RadialGradientPaint ballGrad = new RadialGradientPaint(center, (float)radius, stop, gradColor);
        g.setPaint(ballGrad);
        g.fillOval((int)x, (int)y, (int)diameter, (int)diameter);

        g.setStroke(new BasicStroke(1.8f * (float)scale));
        g.setColor(new Color(0,0,0,70));
        g.drawOval((int)x, (int)y, (int)diameter, (int)diameter);

        g.setStroke(new BasicStroke(0.8f * (float)scale));
        g.setColor(new Color(255,255,255,50));
        g.drawOval((int)(x + 1 * scale), (int)(y + 1 * scale), (int)(diameter - 2 * scale), (int)(diameter - 2 * scale));

        g.setColor(new Color(255,255,255,200));
        double highlightR = radius * 0.32;
        g.fillOval((int)(cx - radius * 0.48), (int)(cy - radius * 0.48), (int)highlightR, (int)highlightR);

        g.setColor(new Color(255,255,255,120));
        int reflect1Size = Math.max(1, (int)(4 * scale));
        int reflect2Size = Math.max(1, (int)(3 * scale));
        g.fillOval((int)(cx + radius * 0.25), (int)(cy - radius * 0.2), reflect1Size, reflect1Size);
        g.fillOval((int)(cx - radius * 0.2), (int)(cy + radius * 0.3), reflect2Size, reflect2Size);

        if (alpha < 1.0f) {
            g.setComposite(originalComposite);
        }
    }

    public void reset() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;
        this.popping = false;
        this.falling = false;
        this.alone = false;
        this.dead = false;
        this.popDelay = 0;
        this.popProgress = 0;
        this.fallVy = 0;
        this.aloneDelay = 0;
        this.scored = false;   // 重置得分标记
    }
}