import java.awt.*;
import java.util.Random;

public class Marbles {
    private static final double SQRT3 = Math.sqrt(3);
    private int rowCount;
    private Marble[][] marbles;
    private double ySpacing;
    private Random random = new Random();
    private double baseX;
    private double accumulatedY;
    private int screenWidth;
    private int maxRowCount;
    private double side;

    public Marbles() {
        this.marbles = null;
        this.rowCount = 0;
        this.ySpacing = 0;
        this.baseX = 0;
        this.accumulatedY = 0;
        this.side = 24.22;
    }

    public double getSide() { return side; }

    public void setMaxRowCount(int maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public void StartMarbles(int screenWidth, int screenHeight, int initialRowCount) {
        this.screenWidth = screenWidth;
        this.ySpacing = side * 1.5;

        int totalRows = maxRowCount + initialRowCount;
        this.rowCount = initialRowCount;
        this.marbles = new Marble[totalRows][];

        for (int generatedRows = 0; generatedRows < initialRowCount; generatedRows++) {
            int actualRow = maxRowCount + generatedRows;
            AddMarbleRow(actualRow, screenWidth, initialRowCount);

            for (int r = maxRowCount; r < actualRow; r++) {
                if (marbles[r] == null) continue;
                for (int c = 0, len = marbles[r].length; c < len; c++) {
                    double cy = marbles[r][c].getCenterY();
                    marbles[r][c].setCenter(marbles[r][c].getCenterX(), cy + ySpacing);
                }
            }
        }
    }

    public void AddMarbleRow(int row, int screenWidth, int initialRowCount) {
        double baseY = -2 * side;
        double xSpacing = side * SQRT3;

        if (marbles == null || row == maxRowCount) {
            double[] initialBaseX = { side * SQRT3, side * SQRT3 / 2 };
            this.baseX = initialBaseX[random.nextInt(2)];
            this.rowCount = initialRowCount;
            this.marbles = new Marble[maxRowCount + initialRowCount][];
        } else if (row >= marbles.length) {
            Marble[][] newMarbles = new Marble[marbles.length + 1][];
            System.arraycopy(marbles, 0, newMarbles, 0, marbles.length);
            this.marbles = newMarbles;
        }

        int perRow = (int)(screenWidth / xSpacing);
        this.marbles[row] = new Marble[perRow];

        for (int col = 0; col < perRow; col++) {
            this.marbles[row][col] = new Marble();
            this.marbles[row][col].init(baseX + col * xSpacing, baseY, row, col);
            initEdgeAttachment(marbles[row][col], row, col);
        }
        this.baseX = baseX + (baseX % xSpacing == 0 ? -xSpacing / 2 : xSpacing / 2);
    }

    private void initEdgeAttachment(Marble hex, int row, int col) {
        int[][] edgeAttachment = hex.getEdgeAttachment();
        double centerX = hex.getCenterX();
        double refX = side * SQRT3;
        boolean isEvenCol = Math.abs(centerX - refX) < 0.001;

        if (isEvenCol) {
            edgeAttachment[0][0] = row + 1; edgeAttachment[0][1] = col + 1;
            edgeAttachment[1][0] = row;     edgeAttachment[1][1] = col + 1;
            edgeAttachment[2][0] = row - 1; edgeAttachment[2][1] = col + 1;
            edgeAttachment[3][0] = row - 1; edgeAttachment[3][1] = col;
            edgeAttachment[4][0] = row;     edgeAttachment[4][1] = col - 1;
            edgeAttachment[5][0] = row + 1; edgeAttachment[5][1] = col - 1;
        } else {
            edgeAttachment[0][0] = row + 1; edgeAttachment[0][1] = col;
            edgeAttachment[1][0] = row;     edgeAttachment[1][1] = col + 1;
            edgeAttachment[2][0] = row - 1; edgeAttachment[2][1] = col;
            edgeAttachment[3][0] = row - 1; edgeAttachment[3][1] = col - 1;
            edgeAttachment[4][0] = row;     edgeAttachment[4][1] = col - 1;
            edgeAttachment[5][0] = row + 1; edgeAttachment[5][1] = col - 1;
        }
    }

    public void initRow(int screenWidth, int screenHeight) {
        StartMarbles(screenWidth, screenHeight, 8);
    }

    public void update(double dt) {
        if (marbles == null) return;

        double yMove = side * 0.4 * dt;

        for (int r = maxRowCount; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                hex.setCenter(hex.getCenterX(), hex.getCenterY() + yMove);
                hex.recalculateVerticesIfDirty();
            }
        }

        accumulatedY += yMove;
        if (accumulatedY >= ySpacing) {
            int newRow = maxRowCount + rowCount;
            this.rowCount = rowCount + 1;
            AddMarbleRow(newRow, screenWidth, this.rowCount);
            accumulatedY -= ySpacing;
        }
    }

    public Marble getHex(int row, int col) {
        if (marbles != null && row >= 0 && row < marbles.length && col >= 0 && col < marbles[row].length) {
            return marbles[row][col];
        }
        return null;
    }

    public int getRowCount() {
        return rowCount;
    }

    public double getVerticalSpacing() {
        return ySpacing;
    }

    public void draw(Graphics2D g) {
        if (marbles == null) return;
        for (int r = maxRowCount; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                hex.draw(g);
            }
        }
    }

    public void resetRow() {
        if (marbles != null) {
            for (Marble[] row : marbles) {
                for (Marble hex : row) {
                    hex.reset();
                }
            }
        }
        marbles = null;
        rowCount = 0;
        ySpacing = 0;
    }
}