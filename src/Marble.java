import java.awt.*;
import java.util.Random;

public class Marble {
    private double cx, cy;
    private double side;
    private boolean verticesDirty;
    public static final int RED = 1;
    public static final int BLUE = 2;
    public static final int YELLOW = 3;
    public static final int PURPLE = 4;

    private static final Color[] COLORS = {
        null,
        Color.RED,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA
    };
    private static final Random random = new Random();
    private int colorType;
    private int row;
    private int col;
    private int[][] edgeAttachment;
    private int[] xVertices;
    private int[] yVertices;

    public Marble() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.xVertices = new int[6];
        this.yVertices = new int[6];
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
        calculateVertices();
    }

    private void calculateVertices() {
        double startAngle = -Math.PI / 2;
        for (int i = 0; i < 6; i++) {
            double rad = startAngle + Math.toRadians(i * 60);
            xVertices[i] = (int)(cx + side * Math.cos(rad));
            yVertices[i] = (int)(cy + side * Math.sin(rad));
        }
        verticesDirty = false;
    }

    public void setCenter(double cx, double cy) {
        this.cx = cx;
        this.cy = cy;
        this.verticesDirty = true;
    }

    public void recalculateVerticesIfDirty() {
        if (verticesDirty) {
            calculateVertices();
        }
    }

    public void setSide(double side) {
        this.side = side;
        calculateVertices();
    }

    public double getCenterX() { return cx; }
    public double getCenterY() { return cy; }
    public double getSide() { return side; }
    public int[] getXVertices() { return xVertices; }
    public int[] getYVertices() { return yVertices; }
    public Color getColor() { return COLORS[colorType]; }
    public int getColorType() { return colorType; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int[][] getEdgeAttachment() { return edgeAttachment; }

    public void draw(Graphics2D g) {
        g.setColor(COLORS[colorType]);
        g.fillPolygon(xVertices, yVertices, 6);
        g.setColor(Color.BLACK);
        g.drawPolygon(xVertices, yVertices, 6);
    }

    public void reset() {
        this.cx = 0;
        this.cy = 0;
        this.side = DEFAULT_SIDE;
        this.verticesDirty = false;
    }

    private static final double DEFAULT_SIDE = 24.22;
}