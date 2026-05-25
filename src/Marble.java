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

    // 主体色系
    private static final Color[] BASE_COLOR = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200)
    };
    // 亮部高光底色
    private static final Color[] BRIGHT_COLOR = {
            null,
            new Color(255, 130, 130),
            new Color(110, 190, 255),
            new Color(255, 250, 180),
            new Color(220, 130, 255)
    };
    // 暗部加深色
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
    // 消除专用
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

    public void update(double dt) {}

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
    // 新增：用于在碰撞后接收发射弹珠的颜色
    public void setColorType(int colorType) { this.colorType = colorType; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int[][] getEdgeAttachment() { return edgeAttachment; }
    public void setEdgeAttachment(int[][] edgeAttachment) { this.edgeAttachment = edgeAttachment; }

    public void markForRemove(boolean b) { markedForRemove = b; }
    public boolean isMarkedForRemove() { return markedForRemove; }

    public void draw(Graphics2D g) {
        if (!initialized) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 精准半径：弹珠之间刚好接触，无重叠、无空隙
        double radius = side * 0.866;
        double x = cx - radius;
        double y = cy - radius;
        double diameter = radius * 2;

        Color base = BASE_COLOR[colorType];
        Color bright = BRIGHT_COLOR[colorType];
        Color dark = DARK_COLOR[colorType];

        // 阴影
        g.setColor(new Color(0,0,0,40));
        g.fillOval((int)(x+2), (int)(y+2), (int)diameter, (int)diameter);

        // 球体渐变
        Point2D center = new Point2D.Double(cx, cy);
        float[] stop = {0f, 0.6f, 1f};
        Color[] gradColor = {bright, base, dark};
        RadialGradientPaint ballGrad = new RadialGradientPaint(center, (float)radius, stop, gradColor);
        g.setPaint(ballGrad);
        g.fillOval((int)x, (int)y, (int)diameter, (int)diameter);

        // 双层精致边框
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(new Color(0,0,0,70));
        g.drawOval((int)x, (int)y, (int)diameter, (int)diameter);

        g.setStroke(new BasicStroke(0.8f));
        g.setColor(new Color(255,255,255,50));
        g.drawOval((int)(x+1), (int)(y+1), (int)(diameter-2), (int)(diameter-2));

        // 主高光
        g.setColor(new Color(255,255,255,200));
        double highlightR = radius * 0.32;
        g.fillOval((int)(cx-radius*0.48), (int)(cy-radius*0.48), (int)highlightR, (int)highlightR);

        // 玻璃反光点
        g.setColor(new Color(255,255,255,120));
        g.fillOval((int)(cx+radius*0.25), (int)(cy-radius*0.2), 4,4);
        g.fillOval((int)(cx-radius*0.2), (int)(cy+radius*0.3), 3,3);
    }

    public void reset() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;
    }
}