import java.awt.*;
import java.awt.geom.Point2D;

public class LaunchPad {
    private double side;
    private int maxRowCount;
    private double topY;
    public Point2D.Double cannon;

    public LaunchPad(double side, int maxRowCount) {
        this.side = side;
        this.maxRowCount = maxRowCount;
        this.cannon = new Point2D.Double(0, 0);
    }

    private double calculateTopY() {
        if (maxRowCount % 2 == 1) {
            return 3 * ((maxRowCount - 1) / 2.0 + Math.sqrt(3) / 2) * side;
        } else {
            return (3 * ((maxRowCount - 2) / 2.0 + Math.sqrt(3) / 2 + 0.5)) * side;
        }
    }

    public void setCannonPosition(int mWidth, int mHeight) {
        updateCannonPosition(mWidth, mHeight);
    }

    public void drawLaunchPad(Graphics2D g, int mWidth, int mHeight) {
        updateCannonPosition(mWidth, mHeight);

        g.setColor(Color.GRAY);
        g.fillRect(0, (int) topY, mWidth, mHeight - (int) topY);
        g.setColor(Color.BLACK);
        g.drawRect(0, (int) topY, mWidth, mHeight - (int) topY);
    }

    private void updateCannonPosition(int mWidth, int mHeight) {
        topY = calculateTopY();
        cannon.x = mWidth / 2.0;
        cannon.y = mHeight - topY / 2.0;
    }

    public void drawCannon(Graphics2D g, double mouseX, double mouseY) {
        double dx = mouseX - cannon.x;
        double dy = mouseY - cannon.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 1) {
            return;
        }

        double angle = Math.atan2(dy, dx);

        double arrowLength = side * 2;
        double arrowX = cannon.x + Math.cos(angle) * arrowLength;
        double arrowY = cannon.y + Math.sin(angle) * arrowLength;

        double arrowHeadLength = side * 0.6;
        double arrowHeadAngle = Math.PI / 6;

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(4));

        g.drawLine((int) cannon.x, (int) cannon.y, (int) arrowX, (int) arrowY);

        double x1 = arrowX - arrowHeadLength * Math.cos(angle - arrowHeadAngle);
        double y1 = arrowY - arrowHeadLength * Math.sin(angle - arrowHeadAngle);
        double x2 = arrowX - arrowHeadLength * Math.cos(angle + arrowHeadAngle);
        double y2 = arrowY - arrowHeadLength * Math.sin(angle + arrowHeadAngle);

        g.fillPolygon(new int[]{(int) arrowX, (int) x1, (int) x2},
                new int[]{(int) arrowY, (int) y1, (int) y2}, 3);

        g.setStroke(new BasicStroke(1));
    }
}