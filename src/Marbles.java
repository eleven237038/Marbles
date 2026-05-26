import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

public class Marbles {
    private static final double SQRT3 = Math.sqrt(3);
    private static final int MIN_GROUP_SIZE = 3;

    private int rowCount;
    private Marble[][] marbles;
    private double ySpacing;
    private Random random = new Random();
    private double baseX;
    private double accumulatedY;
    private int screenWidth;
    private int screenHeight;
    private int maxRowCount;
    private double side;

    // 得分监听器
    private BiConsumer<Marble, Integer> scoreListener;

    // 动画相关
    private long lastAnimationTime = 0;
    private float sparkleOffset = 0;

    public Marbles() {
        this.marbles = null;
        this.rowCount = 0;
        this.ySpacing = 0;
        this.baseX = 0;
        this.accumulatedY = 0;
        this.side = 24.22;
    }

    public void setScoreListener(BiConsumer<Marble, Integer> listener) {
        this.scoreListener = listener;
    }

    public double getSide() { return side; }
    public int getMaxRowCount() { return maxRowCount; }

    public void setMaxRowCount(int maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public void StartMarbles(int screenWidth, int screenHeight, int initialRowCount) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
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
        }
        this.baseX = baseX + (baseX % xSpacing == 0 ? -xSpacing / 2 : xSpacing / 2);
    }

    public void initRow(int screenWidth, int screenHeight) {
        StartMarbles(screenWidth, screenHeight, 4);
    }

    public void update(double dt) {
        if (marbles == null) return;

        double yMove = side * 0.4 * dt;

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble hex = marbles[r][c];
                if (hex != null) {
                    if (!hex.isFalling()) {
                        hex.setCenter(hex.getCenterX(), hex.getCenterY() + yMove);
                    }
                    hex.update(dt);
                    hex.recalculateVerticesIfDirty();

                    if (hex.isDead()) {
                        marbles[r][c] = null;
                    }
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

        // 更新动画偏移
        sparkleOffset += dt * 3;
        if (sparkleOffset > Math.PI * 2) sparkleOffset -= Math.PI * 2;
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

    private boolean isMarbleActive(Marble m) {
        return m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone();
    }

    public void checkAllEdgeAttachments(int screenWidth) {
        if (marbles == null) return;

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (!isMarbleActive(m)) continue;

                int[][] ea = m.getEdgeAttachment();
                boolean allZero = true;
                if (ea != null) {
                    for (int i = 0; i < ea.length; i++) {
                        if (ea[i][0] != 0 || ea[i][1] != 0) {
                            allZero = false;
                            break;
                        }
                    }
                }

                if (allZero) {
                    int triggerEdge = 0;
                    double xSpacing = side * SQRT3;
                    if (m.getCol() % 2 == 0) {
                        triggerEdge = 4;
                    } else {
                        triggerEdge = 1;
                    }
                    m.startAlone(triggerEdge, 0);
                    marbles[r][c] = null;
                }
            }
        }
    }

    public Marble[] getRow(int row) {
        if (marbles != null && row >= 0 && row < marbles.length) {
            return marbles[row];
        }
        return null;
    }

    public void attachMarble(Marble launchMarble, int screenWidth) {
        double lx = launchMarble.getCenterX();
        double ly = launchMarble.getCenterY();
        double xSpacing = side * SQRT3;

        Marble ref = null;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] != null) {
                for (Marble m : marbles[r]) {
                    if (isMarbleActive(m)) {
                        ref = m;
                        break;
                    }
                }
            }
            if (ref != null) break;
        }

        if (ref == null) return;

        int rowOffset = (int) Math.round((ref.getCenterY() - ly) / ySpacing);
        int targetRow = ref.getRow() + rowOffset;
        if (targetRow < 0) targetRow = 0;

        double refBaseX = ref.getCenterX() - ref.getCol() * xSpacing;
        double targetBaseX = refBaseX;

        int refBaseState = (int) Math.round(refBaseX / (xSpacing / 2.0));
        if (Math.abs(targetRow - ref.getRow()) % 2 == 1) {
            targetBaseX = (refBaseState == 1) ? xSpacing : (xSpacing / 2.0);
        }

        int targetCol = (int) Math.round((lx - targetBaseX) / xSpacing);
        int maxCols = (int) (screenWidth / xSpacing);
        if (targetCol < 0) targetCol = 0;
        if (targetCol > maxCols) targetCol = maxCols;

        boolean isOccupied = false;
        if (targetRow < marbles.length && marbles[targetRow] != null && targetCol < marbles[targetRow].length) {
            isOccupied = isMarbleActive(marbles[targetRow][targetCol]);
        }

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
                        occ = isMarbleActive(marbles[r][c]);
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

            targetBaseX = refBaseX;
            if (Math.abs(targetRow - ref.getRow()) % 2 == 1) {
                targetBaseX = (refBaseState == 1) ? xSpacing : (xSpacing / 2.0);
            }
        }

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

        double exactX = targetBaseX + targetCol * xSpacing;
        double exactY = ref.getCenterY() - (targetRow - ref.getRow()) * ySpacing;

        marbles[targetRow][targetCol] = new Marble();
        marbles[targetRow][targetCol].setColorType(launchMarble.getColorType());
        marbles[targetRow][targetCol].init(exactX, exactY, targetRow, targetCol);

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
            Marble launchM = marbles[launchRow][launchCol];
            double originX = launchM != null ? launchM.getCenterX() : 0;
            double originY = launchM != null ? launchM.getCenterY() : 0;

            for (Marble m : connectedGroup) {
                if (!m.isScored() && scoreListener != null) {
                    scoreListener.accept(m, 10);
                    m.setScored(true);
                }
                double dist = 0;
                if (launchM != null) {
                    dist = Math.hypot(m.getCenterX() - originX, m.getCenterY() - originY);
                }
                double delay = dist / 600.0;
                m.startPop(delay);
            }

            checkFloatingMarbles();
        }
    }

    private void checkFloatingMarbles() {
        if (marbles == null) return;

        double minY = Double.MAX_VALUE;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (isMarbleActive(m)) {
                    if (m.getCenterY() < minY) {
                        minY = m.getCenterY();
                    }
                }
            }
        }

        if (minY == Double.MAX_VALUE) return;

        List<Marble> ceilingMarbles = new ArrayList<>();
        boolean[][] visited = new boolean[marbles.length][];
        for (int i = 0; i < marbles.length; i++) {
            if (marbles[i] != null) visited[i] = new boolean[marbles[i].length];
        }

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (isMarbleActive(m)) {
                    if (m.getCenterY() <= minY + side * 1.5) {
                        ceilingMarbles.add(m);
                    }
                }
            }
        }

        for (Marble cm : ceilingMarbles) {
            if (!visited[cm.getRow()][cm.getCol()]) {
                dfsFloating(cm.getRow(), cm.getCol(), visited);
            }
        }

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (isMarbleActive(m) && !visited[r][c]) {
                    if (!m.isScored() && scoreListener != null) {
                        scoreListener.accept(m, 10);
                        m.setScored(true);
                    }
                    m.startFalling(random.nextDouble() * 0.1);
                }
            }
        }
    }

    private void dfsFloating(int r, int c, boolean[][] visited) {
        if (r < 0 || r >= marbles.length || marbles[r] == null || c < 0 || c >= marbles[r].length) return;
        if (visited[r][c]) return;

        Marble current = marbles[r][c];
        if (current == null || !current.isInitialized() || current.isPopping() || current.isFalling()) return;

        visited[r][c] = true;

        double thresholdSq = (side * SQRT3 * 1.2) * (side * SQRT3 * 1.2);

        for (int dr = -2; dr <= 2; dr++) {
            int nr = r + dr;
            if (nr >= 0 && nr < marbles.length && marbles[nr] != null) {
                for (int dc = -2; dc <= 2; dc++) {
                    int nc = c + dc;
                    if (nc >= 0 && nc < marbles[nr].length) {
                        Marble neighbor = marbles[nr][nc];
                        if (isMarbleActive(neighbor)) {
                            double dx = current.getCenterX() - neighbor.getCenterX();
                            double dy = current.getCenterY() - neighbor.getCenterY();
                            if (dx * dx + dy * dy <= thresholdSq) {
                                dfsFloating(nr, nc, visited);
                            }
                        }
                    }
                }
            }
        }
    }

    private void dfs(int r, int c, int targetColor, boolean[][] visited, List<Marble> res) {
        if (r < 0 || r >= marbles.length) return;
        if (marbles[r] == null || c < 0 || c >= marbles[r].length) return;
        if (visited[r][c]) return;

        Marble current = marbles[r][c];
        if (current == null || !current.isInitialized() || current.isPopping() || current.isFalling()) return;
        if (current.getColorType() != targetColor) return;

        visited[r][c] = true;
        res.add(current);

        double thresholdSq = (side * SQRT3 * 1.1) * (side * SQRT3 * 1.1);

        for (int dr = -1; dr <= 1; dr++) {
            int nr = r + dr;
            if (nr >= 0 && nr < marbles.length && marbles[nr] != null) {
                for (int dc = -2; dc <= 2; dc++) {
                    int nc = c + dc;
                    if (nc >= 0 && nc < marbles[nr].length) {
                        Marble neighbor = marbles[nr][nc];
                        if (isMarbleActive(neighbor) && neighbor.getColorType() == targetColor) {
                            double dx = current.getCenterX() - neighbor.getCenterX();
                            double dy = current.getCenterY() - neighbor.getCenterY();
                            if (dx * dx + dy * dy <= thresholdSq) {
                                dfs(nr, nc, targetColor, visited, res);
                            }
                        }
                    }
                }
            }
        }
    }

    public double getVerticalSpacing() {
        return ySpacing;
    }

    public void draw(Graphics2D g) {
        // 先绘制卡通风格左边界虚线（在弹珠底层，位于deadline最左端）
        drawCartoonLeftBoundary(g);

        // 再绘制弹珠
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                if (hex != null) hex.draw(g);
            }
        }
    }

    /**
     * 绘制卡通风格左边界虚线 - 位于deadline最左端（简洁版，无装饰）
     */
    private void drawCartoonLeftBoundary(Graphics2D g) {
        if (screenWidth <= 0 || screenHeight <= 0) return;

        // deadline最左端位置，x = 0
        double leftBoundaryX = 0;

        // 确定Y轴范围（从顶部到底部，覆盖整个游戏区域）
        double topY = -side * 3;
        double bottomY = screenHeight + side * 3;

        // 保存原始设置
        Stroke originalStroke = g.getStroke();

        // 启用抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ========== 1. 绘制外发光效果 ==========
        for (float offset = -3; offset <= 3; offset += 1.0f) {
            int alpha = (int) (20 - Math.abs(offset) * 5);
            g.setColor(new Color(255, 200, 100, Math.max(5, alpha)));
            g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) (leftBoundaryX + offset), (int) topY, (int) (leftBoundaryX + offset), (int) bottomY);
        }

        // ========== 2. 绘制主虚线（彩虹渐变） ==========
        float[] dashPattern = {16, 12};
        BasicStroke dashedStroke = new BasicStroke(
                4.0f,                    // 线宽
                BasicStroke.CAP_ROUND,   // 圆形端点
                BasicStroke.JOIN_ROUND,
                1.0f,
                dashPattern,
                0f
        );
        g.setStroke(dashedStroke);

        // 分段彩虹渐变
        int segments = 30;
        double segmentHeight = (bottomY - topY) / segments;

        for (int i = 0; i < segments; i++) {
            double startY = topY + i * segmentHeight;
            double endY = startY + segmentHeight;

            // 彩虹色
            float hue = (float) (i * 0.033);
            Color lineColor = Color.getHSBColor(hue, 0.9f, 0.8f);
            g.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 220));

            g.drawLine((int) leftBoundaryX, (int) startY, (int) leftBoundaryX, (int) endY);
        }

        // ========== 3. 绘制细虚线内层 ==========
        float[] innerDashPattern = {8, 14};
        BasicStroke innerStroke = new BasicStroke(
                2.0f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1.0f,
                innerDashPattern,
                4f
        );
        g.setStroke(innerStroke);

        for (int i = 0; i < segments; i++) {
            double startY = topY + i * segmentHeight;
            double endY = startY + segmentHeight;

            float hue = (float) ((i * 0.033 + 0.5) % 1.0);
            Color lineColor = Color.getHSBColor(hue, 0.95f, 0.95f);
            g.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 200));

            g.drawLine((int) leftBoundaryX, (int) startY, (int) leftBoundaryX, (int) endY);
        }

        // 恢复原始设置
        g.setStroke(originalStroke);
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