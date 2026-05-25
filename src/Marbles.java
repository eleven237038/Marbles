import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Marbles {
    private static final double SQRT3 = Math.sqrt(3);
    private static final int MIN_GROUP_SIZE = 3;
    // 偶数行六边形邻接方向
    private static final int[][] EVEN_ROW_DIRS = {
            {-1, 0}, {-1, 1},
            {0, -1},          {0, 1},
            {1, 0}, {1, 1}
    };
    // 奇数行六边形邻接方向
    private static final int[][] ODD_ROW_DIRS = {
            {-1, -1}, {-1, 0},
            {0, -1},          {0, 1},
            {1, -1}, {1, 0}
    };
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
    public int getMaxRowCount() { return maxRowCount; }

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
                    if (marbles[r][c] != null) {
                        double cy = marbles[r][c].getCenterY();
                        marbles[r][c].setCenter(marbles[r][c].getCenterX(), cy + ySpacing);
                    }
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

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                if (hex != null) {
                    hex.setCenter(hex.getCenterX(), hex.getCenterY() + yMove);
                    hex.recalculateVerticesIfDirty();
                }
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

    public int getMarblesLength() {
        return marbles != null ? marbles.length : 0;
    }

    // 新增：供 Main 类进行碰撞检测时获取行数据
    public Marble[] getRow(int row) {
        if (marbles != null && row >= 0 && row < marbles.length) {
            return marbles[row];
        }
        return null;
    }

    // 新增：核心算法，将发射的弹珠吸附到六边形网格的正确位置
    public void attachMarble(Marble launchMarble, int screenWidth) {
        double lx = launchMarble.getCenterX();
        double ly = launchMarble.getCenterY();
        double xSpacing = side * SQRT3;

        // 1. 寻找网格中的一个参考球，用来推算当前动态网格的绝对坐标偏移
        Marble ref = null;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] != null) {
                for (Marble m : marbles[r]) {
                    if (m != null && m.isInitialized()) {
                        ref = m;
                        break;
                    }
                }
            }
            if (ref != null) break;
        }

        if (ref == null) return;

        // 根据 Y 坐标差异推算目标行
        int rowOffset = (int) Math.round((ref.getCenterY() - ly) / ySpacing);
        int targetRow = ref.getRow() + rowOffset;

        // 预留的 0~17 空间兜底防越界
        if (targetRow < 0) targetRow = 0;

        // 3. 计算这一行的基础 X 偏移量 (处理交错排列 - 健壮的整型状态判断)
        double refBaseX = ref.getCenterX() - ref.getCol() * xSpacing;
        double targetBaseX = refBaseX;

        int refBaseState = (int) Math.round(refBaseX / (xSpacing / 2.0));
        if (Math.abs(targetRow - ref.getRow()) % 2 == 1) {
            targetBaseX = (refBaseState == 1) ? xSpacing : (xSpacing / 2.0);
        }

        // 4. 根据 X 坐标推算应该吸附在第几列
        int targetCol = (int) Math.round((lx - targetBaseX) / xSpacing);
        int maxCols = (int) (screenWidth / xSpacing);
        if (targetCol < 0) targetCol = 0;
        if (targetCol > maxCols) targetCol = maxCols;

        // 检测目标网格是否被占用
        boolean isOccupied = false;
        if (targetRow < marbles.length && marbles[targetRow] != null && targetCol < marbles[targetRow].length) {
            if (marbles[targetRow][targetCol] != null && marbles[targetRow][targetCol].isInitialized()) {
                isOccupied = true;
            }
        }

        // 如果计算出的完美位置被占用，启用邻域搜索
        if (isOccupied) {
            int bestRow = targetRow;
            int bestCol = targetCol;
            double minDistSq = Double.MAX_VALUE;

            for (int r = Math.max(0, targetRow - 2); r <= targetRow + 2; r++) {
                double rBaseX = refBaseX;
                if (Math.abs(r - ref.getRow()) % 2 == 1) {
                    rBaseX = (refBaseState == 1) ? xSpacing : (xSpacing / 2.0);
                }

                for (int c = Math.max(0, targetCol - 2); c <= targetCol + 2; c++) {
                    boolean occ = false;
                    if (r < marbles.length && marbles[r] != null && c < marbles[r].length) {
                        if (marbles[r][c] != null && marbles[r][c].isInitialized()) {
                            occ = true;
                        }
                    }
                    if (!occ) {
                        double cellX = rBaseX + c * xSpacing;
                        double cellY = ref.getCenterY() - (r - ref.getRow()) * ySpacing;
                        double dX = cellX - lx;
                        double dY = cellY - ly;
                        double distSq = dX * dX + dY * dY;

                        if (distSq < minDistSq) {
                            minDistSq = distSq;
                            bestRow = r;
                            bestCol = c;
                        }
                    }
                }
            }
            targetRow = bestRow;
            targetCol = bestCol;

            // 根据新找到的安全行重新校正 BaseX
            targetBaseX = refBaseX;
            if (Math.abs(targetRow - ref.getRow()) % 2 == 1) {
                targetBaseX = (refBaseState == 1) ? xSpacing : (xSpacing / 2.0);
            }
        }

        // 扩容处理
        if (targetRow >= marbles.length) {
            Marble[][] newMarbles = new Marble[targetRow + 2][];
            System.arraycopy(marbles, 0, newMarbles, 0, marbles.length);
            marbles = newMarbles;
        }

        if (marbles[targetRow] == null) {
            marbles[targetRow] = new Marble[maxCols + 1];
        }

        if (targetCol >= marbles[targetRow].length) {
            Marble[] newRow = new Marble[targetCol + 1];
            System.arraycopy(marbles[targetRow], 0, newRow, 0, marbles[targetRow].length);
            marbles[targetRow] = newRow;
        }

        // 5. 实例化球体，赋予绝对坐标，并继承发射球的颜色
        double exactX = targetBaseX + targetCol * xSpacing;
        double exactY = ref.getCenterY() - (targetRow - ref.getRow()) * ySpacing;

        marbles[targetRow][targetCol] = new Marble();
        marbles[targetRow][targetCol].setColorType(launchMarble.getColorType());
        marbles[targetRow][targetCol].init(exactX, exactY, targetRow, targetCol);

        initEdgeAttachment(marbles[targetRow][targetCol], targetRow, targetCol);

        checkConnectedFromLaunch(targetRow, targetCol, launchMarble.getColorType());
    }

    private void checkConnectedFromLaunch(int launchRow, int launchCol, int launchColor) {
        if (marbles == null) return;

        List<Marble> connectedGroup = new ArrayList<>();
        boolean[][] visited = new boolean[marbles.length][];
        for (int i = 0; i < marbles.length; i++) {
            if (marbles[i] != null) visited[i] = new boolean[marbles[i].length];
        }

        dfs(launchRow, launchCol, launchColor, visited, connectedGroup);

        if (connectedGroup.size() >= MIN_GROUP_SIZE) {
            for (Marble m : connectedGroup) {
                int r = m.getRow();
                int c = m.getCol();
                if (r >= 0 && r < marbles.length && c >= 0 && c < marbles[r].length) {
                    marbles[r][c] = null;
                }
            }
        }
    }

    private void dfs(int r, int c, int targetColor, boolean[][] visited, List<Marble> res) {
        if (r < 0 || r >= marbles.length) return;
        if (marbles[r] == null || c < 0 || c >= marbles[r].length) return;
        if (visited[r][c]) return;

        Marble current = marbles[r][c];
        if (current == null || !current.isInitialized()) return;
        if (current.getColorType() != targetColor) return;

        visited[r][c] = true;
        res.add(current);

        int[][] directions = (r % 2 == 0) ? EVEN_ROW_DIRS : ODD_ROW_DIRS;
        for (int[] dir : directions) {
            dfs(r + dir[0], c + dir[1], targetColor, visited, res);
        }
    }

    public double getVerticalSpacing() {
        return ySpacing;
    }

    public void draw(Graphics2D g) {
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                if (hex != null) hex.draw(g);
            }
        }
    }

    public void resetRow() {
        if (marbles != null) {
            for (Marble[] row : marbles) {
                if (row == null) continue;
                for (Marble hex : row) {
                    if (hex != null) hex.reset();
                }
            }
        }
        marbles = null;
        rowCount = 0;
        ySpacing = 0;
    }
}