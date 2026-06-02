/**
 * Marbles Game - A hex-grid marble shooting puzzle game
 * Group: 21
 *
 * Team Members:
 *   Chen Chen     - 24008980
 *   Keyu Ding     - 24009027
 *   Feng Dang     - 24008988
 *   Chaoran Liu   - 24008977
 *
 * Course: Games Programming (3-2)
 * Assignment 2
 */

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Marbles - 六边形网格弹珠管理类
 * Marbles - Hexagonal grid marble management class
 */
public class Marbles {
    private static final double SQRT3 = Math.sqrt(3);
    private static final int MIN_GROUP_SIZE = 3;
    private static final double CREEPER_BLAST_RADIUS = 3.0;

    private int rowCount;
    private Marble[][] marbles;
    private double ySpacing;
    private Random random = new Random();
    private double baseX;
    private double accumulatedY;
    private int screenWidth;
    private int maxRowCount;
    private double side;
    private double fallSpeedMultiplier = 1.0;

    private BiConsumer<Marble, Integer> scoreListener;

    private List<ScreenGame.ScoreNumber> scoreNumbers = new ArrayList<>();
    private int lastRoundTotalScore = 0;
    private double lastLaunchX = 0;
    private double lastLaunchY = 0;

    private static final Color[] MARBLE_BASE_COLORS = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200),
            new Color(34, 139, 34),
            new Color(105, 105, 105)
    };

    private double currentFallSpeed;
    private double baseFallSpeed;
    private double maxFallSpeed;
    private double speedIncreaseRate;
    private double gameTimeInLevel = 0;
    private boolean speedManuallySet = false;

    private boolean hasCreeper = false;
    private boolean hasBedrock = false;
    private boolean hasHeart = false;

    private int rowGroupCounter = 0;
    private int creeperInGroup = 0;
    private int bedrockInGroup = 0;

    private boolean newRowInvincible = false;
    private int newestRow = -1;

    // BossSans技能状态 / BossSans skill state
    private int alternateColorRows = 0;

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
    public void setMaxRowCount(int maxRowCount) { this.maxRowCount = maxRowCount; }
    public void setFallSpeedMultiplier(double mult) { this.fallSpeedMultiplier = mult; }

    /**
     * 强制设置下落速度（被BossSans切阶段调用）
     * Force set fall speed (called by BossSans phase change)
     */
    public void setCurrentFallSpeed(double speed) {
        this.currentFallSpeed = speed;
        this.speedManuallySet = true;
    }

    public void setLastLaunchPosition(double x, double y) {
        this.lastLaunchX = x;
        this.lastLaunchY = y;
        this.lastRoundTotalScore = 0;
    }

    /**
     * 添加回合总得分显示
     * Add round total score display
     */
    public void addRoundTotalScore() {
        if (lastRoundTotalScore > 0) {
            scoreNumbers.add(new ScreenGame.ScoreNumber(lastLaunchX, lastLaunchY - 60, lastRoundTotalScore));
            lastRoundTotalScore = 0;
        }
    }

    /**
     * 更新分数数字动画
     * Update score number animations
     */
    public void updateScoreNumbers() {
        scoreNumbers.removeIf(score -> !score.update());
    }

    private Color getMarbleColor(int colorType) {
        if (colorType >= 1 && colorType <= 6) return MARBLE_BASE_COLORS[colorType];
        return Color.WHITE;
    }

    /**
     * 初始化弹珠网格
     * Initialize marble grid
     */
    public void StartMarbles(int screenWidth, int screenHeight, int initialRowCount) {
        this.screenWidth = screenWidth;
        this.ySpacing = side * 1.5;

        int totalRows = maxRowCount + initialRowCount;
        this.rowCount = initialRowCount;
        this.marbles = new Marble[totalRows][];
        this.accumulatedY = 0; // 确保无残差干扰 / Ensure no residual interference

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

    /**
     * 统计板上heart数量
     * Count hearts on board
     */
    private int countHeartsOnBoard() {
        int count = 0;
        if (marbles == null) return 0;
        for (Marble[] row : marbles) {
            if (row == null) continue;
            for (Marble m : row) {
                if (m != null && m.getColorType() == Marble.HEART && m.isInitialized() && !m.isPopping() && !m.isDead() && !m.isFalling()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 检查是否还有存活的heart弹珠（包括正在下落的）
     * Check if there are any surviving hearts (including falling ones)
     */
    public boolean hasAnyHeartsOnScreen() {
        if (marbles == null) return false;
        for (Marble[] row : marbles) {
            if (row == null) continue;
            for (Marble m : row) {
                if (m != null && m.getColorType() == Marble.HEART && m.isInitialized() && !m.isDead()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 添加新弹珠行
     * Add new marble row
     */
    public void AddMarbleRow(int row, int screenWidth, int initialRowCount) {
        // 新生成的弹珠继承AccumulatedY造成的余位偏移，完美消除上下行产生的网格裂缝
        // New marbles inherit AccumulatedY offset, perfectly eliminating grid gaps between rows
        double baseY = -2 * side + accumulatedY;
        double xSpacing = side * SQRT3;

        if (marbles == null || row == maxRowCount) {
            double[] initialBaseX = { side * SQRT3, side * SQRT3 / 2 };
            this.baseX = initialBaseX[random.nextInt(2)];
            this.rowCount = initialRowCount;
            this.marbles = new Marble[maxRowCount + initialRowCount][];
            rowGroupCounter = 0;
            creeperInGroup = 0;
            bedrockInGroup = 0;
        } else if (row >= marbles.length) {
            Marble[][] newMarbles = new Marble[marbles.length + 1][];
            System.arraycopy(marbles, 0, newMarbles, 0, marbles.length);
            this.marbles = newMarbles;
        }

        rowGroupCounter++;
        boolean lastRowInGroup = (rowGroupCounter == 4);

        int perRow = (int)(screenWidth / xSpacing);
        this.marbles[row] = new Marble[perRow];

        int targetCreepers = 0;
        int targetBedrocks = 0;

        if (hasCreeper || hasBedrock) {
            if (hasCreeper) {
                if (lastRowInGroup) {
                    int remaining = 3 - creeperInGroup;
                    if (remaining > 0) targetCreepers = 1 + random.nextInt(remaining);
                } else {
                    if (creeperInGroup < 3 && random.nextDouble() < 0.3) targetCreepers = 1;
                }
            }
            if (hasBedrock) {
                if (lastRowInGroup) {
                    targetBedrocks = Math.max(3 - bedrockInGroup, 0);
                    if (targetBedrocks == 0 && bedrockInGroup < 3) targetBedrocks = 3 - bedrockInGroup;
                    targetBedrocks = Math.max(targetBedrocks, 3) + random.nextInt(3);
                } else {
                    targetBedrocks = 1 + random.nextInt(2);
                }
            }
        }

        List<Integer> creeperPositions = new ArrayList<>();
        List<Integer> bedrockPositions = new ArrayList<>();

        for (int col = 0; col < perRow; col++) {
            if (creeperPositions.size() < targetCreepers && random.nextDouble() < 0.15) creeperPositions.add(col);
            if (bedrockPositions.size() < targetBedrocks && !creeperPositions.contains(col) && random.nextDouble() < 0.15) bedrockPositions.add(col);
        }

        while (creeperPositions.size() < targetCreepers && creeperPositions.size() < perRow) {
            int pos = random.nextInt(perRow);
            if (!creeperPositions.contains(pos)) creeperPositions.add(pos);
        }
        while (bedrockPositions.size() < targetBedrocks) {
            int pos = random.nextInt(perRow);
            if (!creeperPositions.contains(pos) && !bedrockPositions.contains(pos)) bedrockPositions.add(pos);
        }

        int currentHearts = countHeartsOnBoard();
        boolean spawnHeart = false;
        if (hasHeart && currentHearts == 0 && random.nextDouble() < 0.15) {
            spawnHeart = true;
        }

        // 预先为heart随机选择一个放置列 / Randomly choose a column for heart placement in advance
        int heartCol = -1;
        if (spawnHeart) {
            List<Integer> availableCols = new ArrayList<>();
            for (int col = 0; col < perRow; col++) {
                if (!creeperPositions.contains(col) && !bedrockPositions.contains(col)) {
                    availableCols.add(col);
                }
            }
            if (!availableCols.isEmpty()) {
                heartCol = availableCols.get(random.nextInt(availableCols.size()));
            }
        }

        int lastColor = -1;
        for (int col = 0; col < perRow; col++) {
            this.marbles[row][col] = new Marble();
            this.marbles[row][col].init(baseX + col * xSpacing, baseY, row, col);

            if (targetCreepers > 0 && creeperPositions.contains(col)) {
                this.marbles[row][col].setColorType(Marble.CREEPER);
                creeperInGroup++;
            } else if (targetBedrocks > 0 && bedrockPositions.contains(col)) {
                this.marbles[row][col].setColorType(Marble.BEDROCK);
                bedrockInGroup++;
            } else if (heartCol == col) {
                this.marbles[row][col].setColorType(Marble.HEART);
            } else {
                if (alternateColorRows > 0) {
                    int cType;
                    do {
                        cType = random.nextInt(Level.getInstance().getColorTypeCount()) + 1;
                    } while (cType == lastColor);
                    lastColor = cType;
                    this.marbles[row][col].setColorType(cType);
                } else {
                    this.marbles[row][col].setColorType(random.nextInt(Level.getInstance().getColorTypeCount()) + 1);
                }
            }
        }

        if (alternateColorRows > 0) alternateColorRows--;

        if (lastRowInGroup) {
            rowGroupCounter = 0;
            creeperInGroup = 0;
            bedrockInGroup = 0;
        }

        newestRow = row;

        // 使用更安全的距比判断，防止频繁生成新行导致阵列完全不对齐
        // Use safer distance ratio judgment to prevent misalignment from frequent new row generation
        double state1 = side * SQRT3;
        double state2 = side * SQRT3 / 2.0;
        if (Math.abs(this.baseX - state1) < 0.1) {
            this.baseX = state2;
        } else {
            this.baseX = state1;
        }
    }

    public boolean isNewestRow(int row) { return row == newestRow; }
    public void initRow(int screenWidth, int screenHeight) { StartMarbles(screenWidth, screenHeight, 4); }

    /**
     * 更新弹珠网格状态
     * Update marble grid state
     */
    public void update(double dt, double deadline) {
        if (marbles == null) return;

        newRowInvincible = false;
        updateScoreNumbers();
        updateGameTime(dt);

        double yMove = currentFallSpeed * dt * fallSpeedMultiplier;

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble hex = marbles[r][c];
                if (hex != null) {
                    if (!hex.isFalling()) {
                        double newY = hex.getCenterY() + yMove;
                        hex.setCenter(hex.getCenterX(), newY);
                    }
                    hex.update(dt);
                    hex.recalculateVerticesIfDirty();
                    if (hex.isDead()) marbles[r][c] = null;
                }
            }
        }

        accumulatedY += yMove;

        // 先减去ySpacing，让内部传递出来的残余偏移保留在accumulatedY中
        // Subtract ySpacing first, keeping residual offset in accumulatedY
        while (accumulatedY >= ySpacing) {
            newRowInvincible = true;
            int newRow = maxRowCount + rowCount;
            this.rowCount++;
            accumulatedY -= ySpacing;
            AddMarbleRow(newRow, screenWidth, this.rowCount);
        }

        updateWarnState(deadline, dt);
    }

    public Marble getHex(int row, int col) {
        if (marbles != null && row >= 0 && row < marbles.length && col >= 0 && col < marbles[row].length) {
            return marbles[row][col];
        }
        return null;
    }

    public int getRowCount() { return rowCount; }
    public int getMarblesLength() { return marbles != null ? marbles.length : 0; }

    private boolean isMarbleActive(Marble m) {
        return m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone() && !m.isDead();
    }

    public Marble[] getRow(int row) {
        if (marbles != null && row >= 0 && row < marbles.length) return marbles[row];
        return null;
    }

    /**
     * 获取相邻弹珠
     * Get neighboring marbles
     */
    private List<Marble> getNeighbors(int r, int c) {
        List<Marble> neighbors = new ArrayList<>();
        if (marbles == null || r < 0 || r >= marbles.length || marbles[r] == null || c < 0 || c >= marbles[r].length) {
            return neighbors;
        }
        Marble current = marbles[r][c];
        if (current == null) return neighbors;

        double thresholdSq = (side * SQRT3 * 1.1) * (side * SQRT3 * 1.1);

        for (int dr = -1; dr <= 1; dr++) {
            int nr = r + dr;
            if (nr >= 0 && nr < marbles.length && marbles[nr] != null) {
                for (int dc = -2; dc <= 2; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nc = c + dc;
                    if (nc >= 0 && nc < marbles[nr].length) {
                        Marble neighbor = marbles[nr][nc];
                        if (isMarbleActive(neighbor)) {
                            double dx = current.getCenterX() - neighbor.getCenterX();
                            double dy = current.getCenterY() - neighbor.getCenterY();
                            if (dx * dx + dy * dy <= thresholdSq) {
                                neighbors.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        return neighbors;
    }

    /**
     * 附加发射的弹珠到网格
     * Attach launched marble to grid
     */
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

        List<Marble> neighbors = getNeighbors(targetRow, targetCol);
        boolean launchedIsCreeper = launchMarble.getColorType() == Marble.CREEPER;
        boolean hitGridCreeper = false;
        List<Marble> hitCreepers = new ArrayList<>();

        for (Marble n : neighbors) {
            if (n.getColorType() == Marble.CREEPER) {
                hitGridCreeper = true;
                hitCreepers.add(n);
            }
        }

        if (launchedIsCreeper && hitGridCreeper) {
            nukeBoard();
            checkFloatingMarbles();
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (launchedIsCreeper) {
            creeperBlast(exactX, exactY);
            checkFloatingMarbles();
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (hitGridCreeper) {
            for (Marble hc : hitCreepers) {
                creeperBlast(hc.getCenterX(), hc.getCenterY());
            }
            checkFloatingMarbles();
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (launchMarble.getColorType() == Marble.BEDROCK) {
            ResourceManager.getInstance().playNoClear();
            triggerCollisionAnimation(targetRow, targetCol, exactX, exactY);
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (launchMarble.getColorType() == Marble.CREEPER) {
            ResourceManager.getInstance().playNoClear();
            triggerCollisionAnimation(targetRow, targetCol, exactX, exactY);
            checkFloatingMarbles();
            addRoundTotalScore();
        } else {
            checkConnectedFromLaunch(targetRow, targetCol, launchMarble.getColorType());
        }
    }

    /**
     * 检查从发射点出发的连通性
     * Check connectivity from launch point
     */
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

            int groupSize = connectedGroup.size();
            if (groupSize == 3) ResourceManager.getInstance().playThreeClear();
            else if (groupSize == 4) ResourceManager.getInstance().playFourClear();
            else if (groupSize > 4) ResourceManager.getInstance().playMassiveClear();

            int pointsPerMarble = (groupSize <= 3) ? 10 : (groupSize <= 6) ? 15 : 20;

            for (Marble m : connectedGroup) {
                if (!m.isScored() && scoreListener != null) {
                    scoreListener.accept(m, pointsPerMarble);
                    m.setScored(true);
                    lastRoundTotalScore += pointsPerMarble;
                    Color marbleColor = getMarbleColor(m.getColorType());
                    scoreNumbers.add(new ScreenGame.ScoreNumber(m.getCenterX(), m.getCenterY() - 15, pointsPerMarble, marbleColor));
                }
                double dist = 0;
                if (launchM != null) {
                    dist = Math.sqrt(Math.pow(m.getCenterX() - originX, 2) + Math.pow(m.getCenterY() - originY, 2));
                }
                double delay = dist / 600.0;
                m.startPop(delay);
            }

            checkFloatingMarbles();
            addRoundTotalScore();

        } else {
            ResourceManager.getInstance().playNoClear();
            Marble launchM = marbles[launchRow][launchCol];
            if (launchM != null) {
                double collisionX = launchM.getCenterX();
                double collisionY = launchM.getCenterY();
                triggerCollisionAnimation(launchRow, launchCol, collisionX, collisionY);
            }
        }
    }

    /**
     * 触发碰撞动画
     * Trigger collision animation
     */
    private void triggerCollisionAnimation(int launchRow, int launchCol, double collisionX, double collisionY) {
        if (marbles == null) return;
        double threshold = side * SQRT3 * 1.05;
        double thresholdSq = threshold * threshold;
        for (int dr = -1; dr <= 1; dr++) {
            int nr = launchRow + dr;
            if (nr < 0 || nr >= marbles.length || marbles[nr] == null) continue;
            for (int dc = -1; dc <= 1; dc++) {
                if (dc == 0 && dr == 0) continue;
                int nc = launchCol + dc;
                if (nc < 0 || nc >= marbles[nr].length) continue;
                Marble m = marbles[nr][nc];
                if (m == null || !m.isInitialized() || m.isPopping() || m.isFalling() || m.isAlone()) continue;
                double dx = m.getCenterX() - collisionX;
                double dy = m.getCenterY() - collisionY;
                if (dx * dx + dy * dy <= thresholdSq) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double delay = (dist / threshold) * 0.02;
                    m.startCollision(collisionX, collisionY, delay);
                }
            }
        }
    }

    /**
     * 检查悬浮弹珠（孤立弹珠检测）
     * Check floating marbles (orphaned marble detection)
     */
    private void checkFloatingMarbles() {
        if (marbles == null) return;

        double minY = Double.MAX_VALUE;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (isMarbleActive(m)) {
                    if (m.getCenterY() < minY) minY = m.getCenterY();
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
                    if (m.getCenterY() <= minY + side * 1.5) ceilingMarbles.add(m);
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
                    if (newRowInvincible && isNewestRow(r)) continue;

                    if (m.getColorType() == Marble.CREEPER) {
                        creeperBlast(m.getCenterX(), m.getCenterY());
                    } else {
                        if (!m.isScored() && scoreListener != null) {
                            scoreListener.accept(m, 20);
                            m.setScored(true);
                            Color marbleColor = getMarbleColor(m.getColorType());
                            scoreNumbers.add(new ScreenGame.ScoreNumber(m.getCenterX(), m.getCenterY() - 15, 20, marbleColor));
                            lastRoundTotalScore += 20;
                            ResourceManager.getInstance().playDropAndScore();
                        }
                        m.startFalling(random.nextDouble() * 0.1);
                    }
                }
            }
        }
    }

    /**
     * 深度优先搜索：检查悬浮弹珠
     * DFS: Check floating marbles
     */
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

    /**
     * 深度优先搜索：检查颜色连通性
     * DFS: Check color connectivity
     */
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

    public double getVerticalSpacing() { return ySpacing; }

    /**
     * 更新警戒状态
     * Update warning state
     */
    public void updateWarnState(double deadline, double dt) {
        if (marbles == null) return;
        double warnDist = side * 5.2;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone()) {
                    double distToDeadline = m.getCenterY() - (deadline - warnDist);
                    boolean inWarnZone = m.getCenterY() >= deadline - warnDist;
                    m.setWarn(inWarnZone);
                    if (inWarnZone) {
                        double intensity = Math.min(1.0, distToDeadline / warnDist);
                        m.updateWarn(dt, intensity);
                    }
                }
            }
        }
    }

    /**
     * 检查是否存在警戒状态的弹珠
     * Check if any marbles are in warning state
     */
    public boolean hasWarning() {
        if (marbles == null) return false;
        for (Marble[] row : marbles) {
            if (row == null) continue;
            for (Marble m : row) {
                if (m != null && m.isWarn()) return true;
            }
        }
        return false;
    }

    /**
     * 绘制弹珠网格
     * Draw marble grid
     */
    public void draw(Graphics2D g) {
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                if (hex != null) hex.draw(g);
            }
        }
        drawScoreNumbers(g);
    }

    private void drawScoreNumbers(Graphics2D g) {
        for (ScreenGame.ScoreNumber sn : scoreNumbers) {
            sn.draw(g);
        }
    }

    /**
     * 重置行数据
     * Reset row data
     */
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
        scoreNumbers.clear();
        lastRoundTotalScore = 0;
    }

    /**
     * 重置关卡速度
     * Reset level speed
     */
    public void resetLevelSpeed() {
        resetSpeedState();
    }

    private void resetSpeedState() {
        this.currentFallSpeed = this.baseFallSpeed;
        this.gameTimeInLevel = 0;
        this.speedManuallySet = false;
    }

    /**
     * 设置关卡速度参数
     * Set level speed parameters
     */
    public void setLevelSpeedParams(double baseSpeed, double maxSpeed, double increaseRate) {
        this.baseFallSpeed = baseSpeed;
        this.maxFallSpeed = maxSpeed;
        this.speedIncreaseRate = increaseRate;
        resetSpeedState();
    }

    /**
     * 设置特殊弹珠配置
     * Set special marble configuration
     */
    public void setSpecialMarbleConfig(boolean creeper, boolean bedrock, boolean heart) {
        this.hasCreeper = creeper;
        this.hasBedrock = bedrock;
        this.hasHeart = heart;
    }

    /**
     * 更新游戏时间（用于速度增加）
     * Update game time (for speed increase)
     */
    public void updateGameTime(double dt) {
        if (this.speedManuallySet) return;
        this.gameTimeInLevel += dt;
        double newSpeed = this.baseFallSpeed + this.gameTimeInLevel * this.speedIncreaseRate;
        this.currentFallSpeed = Math.min(newSpeed, this.maxFallSpeed);
    }

    public double getCurrentFallSpeed() {
        return currentFallSpeed;
    }

    private double getBlastRadiusSq() {
        double hexUnit = side * 1.5;
        return hexUnit * CREEPER_BLAST_RADIUS * hexUnit * CREEPER_BLAST_RADIUS;
    }

    /**
     * Creeper爆炸效果
     * Creeper blast effect
     */
    private void creeperBlast(double centerX, double centerY) {
        if (marbles == null) return;

        double blastRadiusSq = getBlastRadiusSq();
        boolean playedSound = false;

        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (m == null || !m.isInitialized() || m.isPopping() || m.isFalling() || m.isAlone()) continue;
                if (m.isImmuneToCreeper()) continue;
                if (newRowInvincible && isNewestRow(r)) continue;

                double dx = m.getCenterX() - centerX;
                double dy = m.getCenterY() - centerY;
                double distSq = dx * dx + dy * dy;

                if (distSq <= blastRadiusSq) {
                    if (!m.isScored() && scoreListener != null) {
                        scoreListener.accept(m, 20);
                        m.setScored(true);
                        lastRoundTotalScore += 20;
                        Color marbleColor = getMarbleColor(m.getColorType());
                        scoreNumbers.add(new ScreenGame.ScoreNumber(m.getCenterX(), m.getCenterY() - 15, 20, marbleColor));
                    }
                    m.startPop(0);

                    if (!playedSound) {
                        ResourceManager.getInstance().playMassiveClear();
                        playedSound = true;
                    }
                }
            }
        }
    }

    /**
     * 清除所有弹珠（核弹效果）
     * Nuke all marbles (nuke effect)
     */
    private void nukeBoard() {
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            if (newRowInvincible && isNewestRow(r)) continue;

            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone()) {
                    if (!m.isImmuneToCreeper()) {
                        if (!m.isScored() && scoreListener != null) {
                            scoreListener.accept(m, 20);
                            m.setScored(true);
                            lastRoundTotalScore += 20;
                            Color marbleColor = getMarbleColor(m.getColorType());
                            scoreNumbers.add(new ScreenGame.ScoreNumber(m.getCenterX(), m.getCenterY() - 15, 20, marbleColor));
                        }
                        m.startPop(random.nextDouble() * 0.3);
                    }
                }
            }
        }
        ResourceManager.getInstance().playMassiveClear();
    }

    // ================= BossSans技能实现 / BossSans Skill Implementation =================

    /**
     * 技能1：使新落下的两行变成隔色相邻
     * Skill 1: Make next two rows have alternating adjacent colors
     */
    public void skillAlternateColors(int rows) {
        this.alternateColorRows = rows;
    }

    /**
     * 技能2：生成3个bedrock构成的正三角形或倒三角形结构
     * Skill 2: Generate 3-bedrock triangle formation
     */
    public void skillBedrockRadius() {
        if (marbles == null) return;

        // 收集所有可作为三角形顶点的活跃正常弹珠 / Collect all active normal marbles that can be triangle apexes
        List<Marble> validApexes = new ArrayList<>();
        for (Marble[] row : marbles) {
            if (row == null) continue;
            for (Marble m : row) {
                if (isMarbleActive(m) && m.isNormalMarble() && m.getColorType() != Marble.HEART) {
                    validApexes.add(m);
                }
            }
        }
        if (validApexes.isEmpty()) return;

        // 随机选择一个顶点 / Randomly select one apex
        Marble apex = validApexes.get(random.nextInt(validApexes.size()));
        int apexRow = apex.getRow();
        int apexCol = apex.getCol();
        boolean isUpward = random.nextBoolean(); // true=正三角, false=倒三角 / true=upward, false=downward

        // 正三角形（顶点朝上）：顶点在(row,col)，下方两点在(row+1, col-1)和(row+1, col)
        // 倒三角形（顶点朝下）：顶点在(row,col)，上方两点在(row-1, col-1)和(row-1, col)
        // Upward triangle: apex at (row,col), bottom two at (row+1, col-1) and (row+1, col)
        // Downward triangle: apex at (row,col), top two at (row-1, col-1) and (row-1, col)
        int[][] triangleOffsets;
        if (isUpward) {
            triangleOffsets = new int[][] {
                {0, 0},           // 顶点 / Apex
                {1, -1},          // 左下 / Bottom left
                {1, 0}            // 右下 / Bottom right
            };
        } else {
            triangleOffsets = new int[][] {
                {0, 0},           // 顶点 / Apex
                {-1, -1},         // 左上 / Top left
                {-1, 0}           // 右上 / Top right
            };
        }

        // 检查三个位置是否都有效，收集有效位置 / Check if all three positions are valid, collect valid positions
        List<Marble> triangleMarbles = new ArrayList<>();
        for (int[] offset : triangleOffsets) {
            int checkRow = apexRow + offset[0];
            int checkCol = apexCol + offset[1];
            if (checkRow >= 0 && checkRow < marbles.length &&
                marbles[checkRow] != null && checkCol >= 0 && checkCol < marbles[checkRow].length) {
                Marble m = marbles[checkRow][checkCol];
                if (m != null && isMarbleActive(m) && m.isNormalMarble() && m.getColorType() != Marble.HEART) {
                    triangleMarbles.add(m);
                }
            }
        }

        // 如果三个位置都有效，转换为bedrock / If all three positions are valid, convert to bedrock
        if (triangleMarbles.size() == 3) {
            for (Marble m : triangleMarbles) {
                m.setColorType(Marble.BEDROCK);
            }
        }
    }

    /**
     * 技能3：在随机选中行生成3个不相邻的Bedrock
     * Skill 3: Generate 3 non-adjacent Bedrocks in a random row
     */
    public void skillBedrockRow() {
        if (marbles == null) return;

        // 找到所有有活跃弹珠的行 / Find all rows with active marbles
        List<Integer> activeRows = new ArrayList<>();
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            boolean hasActive = false;
            for (Marble m : marbles[r]) {
                if (m != null && isMarbleActive(m) && m.isNormalMarble()) {
                    hasActive = true;
                    break;
                }
            }
            if (hasActive) activeRows.add(r);
        }
        if (activeRows.isEmpty()) return;

        // 随机选一行 / Randomly select one row
        int targetRow = activeRows.get(random.nextInt(activeRows.size()));

        // 统计该行的列数 / Count columns in that row
        int cols = marbles[targetRow].length;

        // 生成3个不相邻的随机位置 / Generate 3 non-adjacent random positions
        List<Integer> bedrockCols = new ArrayList<>();
        int attempts = 0;
        while (bedrockCols.size() < 3 && attempts < 100) {
            int col = random.nextInt(cols);
            // 检查是否与已有的bedrock相邻（相邻定义：列索引差 >= 2）/ Check if adjacent to existing bedrock (adjacent defined as: column index difference >= 2)
            boolean adjacent = false;
            for (int existingCol : bedrockCols) {
                if (Math.abs(col - existingCol) < 2) {
                    adjacent = true;
                    break;
                }
            }
            if (!adjacent) {
                bedrockCols.add(col);
            }
            attempts++;
        }

        // 将这些位置设置为Bedrock / Set these positions to Bedrock
        for (int col : bedrockCols) {
            if (marbles[targetRow][col] != null && isMarbleActive(marbles[targetRow][col])) {
                marbles[targetRow][col].setColorType(Marble.BEDROCK);
            }
        }
    }

    /**
     * 技能5：瞬间向下传送N行（物理坐标与存储槽索引同时同步向下位移，并补充空槽确保无缝连接）
     * Skill 5: Instantly teleport down N rows (physical coordinates and storage slot indices sync down, add new slots for seamless connection)
     */
    public void skillTeleportDown(int rows) {
        if (marbles == null) return;

        for (int i = 0; i < rows; i++) {
            // 所有弹珠物理向下移动一层 / All marbles physically move down one layer
            for (int r = 0; r < marbles.length; r++) {
                if (marbles[r] != null) {
                    for (Marble m : marbles[r]) {
                        if (m != null && !m.isDead() && !m.isFalling() && !m.isPopping()) {
                            m.setCenter(m.getCenterX(), m.getCenterY() + ySpacing);
                        }
                    }
                }
            }
            // 在顶部立刻生成新的一行填补空隙 / Immediately generate new row at top to fill gap
            int newRow = maxRowCount + rowCount;
            this.rowCount++;
            AddMarbleRow(newRow, screenWidth, this.rowCount);
        }
    }
}