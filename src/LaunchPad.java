import java.awt.*;

public class LaunchPad {
    private double side;
    private int maxRowCount;

    public LaunchPad(double side, int maxRowCount) {
        this.side = side;
        this.maxRowCount = maxRowCount;
    }

    public void draw(Graphics2D g, int mWidth, int mHeight) {
        double topY;
        if (maxRowCount % 2 == 1) {
            topY = (3 * ((maxRowCount - 1) / 2.0 + Math.sqrt(3) / 2)) * side;
        } else {
            topY = (3 * ((maxRowCount - 2) / 2.0 + Math.sqrt(3) / 2 + 0.5)) * side;
        }

        g.setColor(Color.GRAY);
        g.fillRect(0, (int) topY, mWidth, mHeight - (int) topY);
        g.setColor(Color.BLACK);
        g.drawRect(0, (int) topY, mWidth, mHeight - (int) topY);
    }
}