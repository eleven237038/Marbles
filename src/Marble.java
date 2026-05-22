import java.awt.*;
import java.util.Random;

public class Marble {
    private double cx, cy;
    private double side;
    private int[] xVertices;
    private int[] yVertices;
    private boolean initialized;
    private Color color;
    private static final Color[] COLORS = {Color.BLUE, Color.RED, Color.YELLOW};
    private static final Random random = new Random();

    public Marble() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.xVertices = new int[6];
        this.yVertices = new int[6];
        this.initialized = false;
        this.color = COLORS[random.nextInt(COLORS.length)];
    }

    public void init(double cx, double cy) {
        this.cx = cx;
        this.cy = cy;
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
        if (initialized) {
            calculateVertices();
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
    public Color getColor() { return color; }

    public void draw(Graphics2D g) {
        if (!initialized) return;
        g.setColor(color);
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