import java.awt.*;
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

    private static final Color[] COLORS = {
        null,       // 占位符，使索引与颜色值对应
        Color.RED,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA
    };
    private static final Random random = new Random();
    private int colorType;
    private int row;    // 六边形所在行
    private int col;    // 六边形编号
    private int[][] edgeAttachment; // [行][编号] 附着判定: 0=右上, 1=正右, 2=右下, 3=左下, 4=正左, 5=左上

    // 顶点属性: 0=顶部, 1=右上, 2=右下, 3=底部, 4=左下, 5=左上 (相对于中心点的方位)
    private int[] xVertices;
    private int[] yVertices;

    public Marble() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.xVertices = new int[6];
        this.yVertices = new int[6];
        this.initialized = false;
        this.colorType = random.nextInt(4) + 1;
        this.row = 0;
        this.col = 0;
        this.edgeAttachment = new int[6][2]; // [方向][0=row, 1=col]
        this.verticesDirty = false;
    }

    public void init(double cx, double cy, int row, int col) {
        this.cx = cx;
        this.cy = cy;
        this.row = row;
        this.col = col;
        calculateVertices();
        this.initialized = true;
    }

    private void calculateVertices() {
        double startAngle = -Math.PI / 2;  // 从顶部开始 (90度)
        for (int i = 0; i < 6; i++) {
            double rad = startAngle + Math.toRadians(i * 60);
            xVertices[i] = (int)(cx + side * Math.cos(rad));
            yVertices[i] = (int)(cy + side * Math.sin(rad));
        }
    }

    public void update(double dt) {
    }

    public void setCenter(double cx, double cy) {
        this.cx = cx;
        this.cy = cy;
        this.verticesDirty = true;
    }

    public void recalculateVerticesIfDirty() {
        if (verticesDirty) {
            calculateVertices();
            verticesDirty = false;
        }
    }

    public void setSide(double side) {
        this.side = side;
        if (initialized) {
            calculateVertices();
        }
    }

    public double getCenterX() { return cx; }
    public double getCenterY() { return cy; }
    public double getSide() { return side; }
    public int[] getXVertices() { return xVertices; }
    public int[] getYVertices() { return yVertices; }
    public boolean isInitialized() { return initialized; }
    public Color getColor() { return COLORS[colorType]; }
    public int getColorType() { return colorType; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int[][] getEdgeAttachment() { return edgeAttachment; }
    public void setEdgeAttachment(int[][] edgeAttachment) { this.edgeAttachment = edgeAttachment; }

    public void draw(Graphics2D g) {
        if (!initialized) return;
        g.setColor(COLORS[colorType]);
        g.fillPolygon(xVertices, yVertices, 6);
        g.setColor(Color.BLACK);
        g.drawPolygon(xVertices, yVertices, 6);
    }

    public void reset() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;
    }
}