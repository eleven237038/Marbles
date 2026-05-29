import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

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

    // 弹珠颜色映射（与Marble类中的颜色保持一致）
    private static final Color[] MARBLE_BASE_COLORS = {
            null,
            new Color(220, 30, 30),   // 红色
            new Color(20, 80, 220),   // 蓝色
            new Color(240, 200, 20),  // 黄色
            new Color(160, 30, 200)   // 紫色
    };

    private double currentFallSpeed;      // 当前下落速度
    private double baseFallSpeed;         // 基础下落速度
    private double maxFallSpeed;           // 最大下落速度
    private double speedIncreaseRate;    // 每秒增加的速度
    private double gameTimeInLevel = 0;          // 当前关卡游戏时间

    // 特殊弹珠生成配置
    private boolean hasCreeper = false;
    private boolean hasBedrock = false;
    private boolean hasHeart = false;

    // 每4行特殊弹珠计数器
    private int rowGroupCounter = 0;  // 0-3循环
    private int creeperInGroup = 0;  // 当前4行中已生成的creeper数量
    private int bedrockInGroup = 0;  // 当前4行中已生成的bedrock数量

    // 无敌帧状态（生成新行时生效）
    private boolean newRowInvincible = false;
    private int newestRow = -1;  // 最新生成的行号

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

    public void setFallSpeedMultiplier(double mult) {
        this.fallSpeedMultiplier = mult;
    }

    public void setLastLaunchPosition(double x, double y) {
        this.lastLaunchX = x;
        this.lastLaunchY = y;
        this.lastRoundTotalScore = 0;
    }

    public void addRoundTotalScore() {
        if (lastRoundTotalScore > 0) {
            scoreNumbers.add(new ScreenGame.ScoreNumber(lastLaunchX, lastLaunchY - 60, lastRoundTotalScore));
            lastRoundTotalScore = 0;
        }
    }

    public void updateScoreNumbers() {
        scoreNumbers.removeIf(score -> !score.update());
    }

    private void drawScoreNumbers(Graphics2D g) {
        for (ScreenGame.ScoreNumber sn : scoreNumbers) {
            sn.draw(g);
        }
    }

    // 根据颜色类型获取弹珠颜色
    private Color getMarbleColor(int colorType) {
        if (colorType >= 1 && colorType <= 4) {
            return MARBLE_BASE_COLORS[colorType];
        }
        return Color.WHITE;
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
            // 重置每4行计数器
            rowGroupCounter = 0;
            creeperInGroup = 0;
            bedrockInGroup = 0;
        } else if (row >= marbles.length) {
            Marble[][] newMarbles = new Marble[marbles.length + 1][];
            System.arraycopy(marbles, 0, newMarbles, 0, marbles.length);
            this.marbles = newMarbles;
        }

        // 更新每4行计数器
        rowGroupCounter++;
        boolean lastRowInGroup = (rowGroupCounter == 4);

        int perRow = (int)(screenWidth / xSpacing);
        this.marbles[row] = new Marble[perRow];

        // 确定本行特殊弹珠数量
        int targetCreepers = 0;
        int targetBedrocks = 0;

        if (hasCreeper || hasBedrock) {
            if (hasCreeper) {
                if (lastRowInGroup) {
                    // 最后一行：确保1-3个creeper（总量1-3）
                    int remaining = 3 - creeperInGroup;
                    if (remaining > 0) {
                        targetCreepers = 1 + random.nextInt(remaining);
                    }
                } else {
                    // 前3行：每行最多1个creeper，总量不超过3
                    if (creeperInGroup < 3 && random.nextDouble() < 0.3) {
                        targetCreepers = 1;
                    }
                }
            }
            if (hasBedrock) {
                if (lastRowInGroup) {
                    // 最后一行：确保最少3个bedrock
                    targetBedrocks = Math.max(3 - bedrockInGroup, 0);
                    if (targetBedrocks == 0 && bedrockInGroup < 3) {
                        targetBedrocks = 3 - bedrockInGroup;
                    }
                    // 无上限，多多益善
                    targetBedrocks = Math.max(targetBedrocks, 3) + random.nextInt(3);
                } else {
                    // 前3行：每行至少1个bedrock
                    targetBedrocks = 1 + random.nextInt(2);
                }
            }
        }

        // 分别收集creeper和bedrock的位置
        List<Integer> creeperPositions = new ArrayList<>();
        List<Integer> bedrockPositions = new ArrayList<>();

        for (int col = 0; col < perRow; col++) {
            if (creeperPositions.size() < targetCreepers && random.nextDouble() < 0.15) {
                creeperPositions.add(col);
            }
            if (bedrockPositions.size() < targetBedrocks && !creeperPositions.contains(col) && random.nextDouble() < 0.15) {
                bedrockPositions.add(col);
            }
        }

        // 确保达到最低数量
        while (creeperPositions.size() < targetCreepers && creeperPositions.size() < perRow) {
            int pos = random.nextInt(perRow);
            if (!creeperPositions.contains(pos)) {
                creeperPositions.add(pos);
            }
        }
        while (bedrockPositions.size() < targetBedrocks) {
            int pos = random.nextInt(perRow);
            if (!creeperPositions.contains(pos) && !bedrockPositions.contains(pos)) {
                bedrockPositions.add(pos);
            }
        }

        for (int col = 0; col < perRow; col++) {
            this.marbles[row][col] = new Marble();
            this.marbles[row][col].init(baseX + col * xSpacing, baseY, row, col);

            if (creeperPositions.contains(col)) {
                this.marbles[row][col].setColorType(Marble.CREEPER);
                creeperInGroup++;
            } else if (bedrockPositions.contains(col)) {
                this.marbles[row][col].setColorType(Marble.BEDROCK);
                bedrockInGroup++;
            } else if (hasHeart && random.nextDouble() < 0.02) {
                this.marbles[row][col].setColorType(Marble.HEART);
            }
        }

        // 如果是每组最后一行，重置计数器
        if (lastRowInGroup) {
            rowGroupCounter = 0;
            creeperInGroup = 0;
            bedrockInGroup = 0;
        }

        newestRow = row;

        this.baseX = baseX + (baseX % xSpacing == 0 ? -xSpacing / 2 : xSpacing / 2);
    }

    public boolean isNewestRow(int row) {
        return row == newestRow;
    }

    public void initRow(int screenWidth, int screenHeight) {
        StartMarbles(screenWidth, screenHeight, 4);
    }

    public void update(double dt, double deadline) {
        if (marbles == null) return;

        // 上一帧生成的新行，在本帧开始时取消无敌帧
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

                    if (hex.isDead()) {
                        marbles[r][c] = null;
                    }
                }
            }
        }

        accumulatedY += yMove;
        if (accumulatedY >= ySpacing) {
            // 进入无敌帧状态（保持到下一帧开始）
            newRowInvincible = true;
            int newRow = maxRowCount + rowCount;
            this.rowCount = rowCount + 1;
            AddMarbleRow(newRow, screenWidth, this.rowCount);
            accumulatedY -= ySpacing;
            // 无敌帧在下一帧开始时取消
        }

        updateWarnState(deadline, dt);
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

    public Marble[] getRow(int row) {
        if (marbles != null && row >= 0 && row < marbles.length) {
            return marbles[row];
        }
        return null;
    }

    // 新增：获取指定行列周围的邻居弹珠
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

        // 将新弹珠放置入网格
        marbles[targetRow][targetCol] = new Marble();
        marbles[targetRow][targetCol].setColorType(launchMarble.getColorType());
        marbles[targetRow][targetCol].init(exactX, exactY, targetRow, targetCol);

        // 新增：判断 Creeper 碰撞逻辑（仅包含：自身碰撞与被碰撞）
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
            // 两个 Creeper 碰撞：核爆清屏
            nukeBoard();
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (launchedIsCreeper) {
            // 自身碰撞：发射的Creeper落入网格，引发爆炸
            creeperBlast(exactX, exactY);
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (hitGridCreeper) {
            // 被碰撞：发射的普通弹珠碰到了网格内的Creeper
            for (Marble hc : hitCreepers) {
                creeperBlast(hc.getCenterX(), hc.getCenterY());
            }
            checkFloatingMarbles();
            addRoundTotalScore();
        } else if (launchMarble.getColorType() == Marble.BEDROCK) {
            // Bedrock无法触发同色相消
            checkFloatingMarbles();
            addRoundTotalScore();
        } else {
            // 全都是普通弹珠 -> 执行正常的同色三消检测
            checkConnectedFromLaunch(targetRow, targetCol, launchMarble.getColorType());
        }
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

            int groupSize = connectedGroup.size();

            if (groupSize == 3) {
                ResourceManager.getInstance().playThreeClear();
            } else if (groupSize == 4) {
                ResourceManager.getInstance().playFourClear();
            } else if (groupSize > 4) {
                ResourceManager.getInstance().playMassiveClear();
            }

            int pointsPerMarble;
            if (groupSize <= 3) {
                pointsPerMarble = 10;
            } else if (groupSize <= 6) {
                pointsPerMarble = 15;
            } else {
                pointsPerMarble = 20;
            }

            // 普通同色消除
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
                    // 无敌帧期间跳过最新生成的行
                    if (newRowInvincible && isNewestRow(r)) continue;

                    // "被消除" 触发：Creeper在悬空掉落（被消除）时触发爆炸
                    if (m.getColorType() == Marble.CREEPER) {
                        creeperBlast(m.getCenterX(), m.getCenterY());
                    } else {
                        if (!m.isScored() && scoreListener != null) {
                            scoreListener.accept(m, 20);
                            m.setScored(true);
                            // 掉落弹珠使用其本身的颜色
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

    public void resetLevelSpeed() {
        this.currentFallSpeed = this.baseFallSpeed;
        this.gameTimeInLevel = 0;
    }

    public void setLevelSpeedParams(double baseSpeed, double maxSpeed, double increaseRate) {
        this.baseFallSpeed = baseSpeed;
        this.maxFallSpeed = maxSpeed;
        this.speedIncreaseRate = increaseRate;
        this.currentFallSpeed = baseSpeed;
    }

    // 设置特殊弹珠生成配置
    public void setSpecialMarbleConfig(boolean creeper, boolean bedrock, boolean heart) {
        this.hasCreeper = creeper;
        this.hasBedrock = bedrock;
        this.hasHeart = heart;
    }

    // 启用creeper生成（由bossSans触发，用于第4关）
    public void enableCreeperGeneration() {
        this.hasCreeper = true;
    }

    public void updateGameTime(double dt) {
        this.gameTimeInLevel += dt;
        double newSpeed = this.baseFallSpeed + this.gameTimeInLevel * this.speedIncreaseRate;
        this.currentFallSpeed = Math.min(newSpeed, this.maxFallSpeed);
    }

    public double getCurrentFallSpeed() {
        return currentFallSpeed;
    }

    // 获取creeper爆炸半径的平方
    private double getBlastRadiusSq() {
        double hexUnit = side * 1.5;
        return hexUnit * CREEPER_BLAST_RADIUS * hexUnit * CREEPER_BLAST_RADIUS;
    }

    // Creeper爆炸：消除中心点周围+3六边形范围内所有普通弹珠
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

                // 无敌帧期间跳过最新生成的行
                if (newRowInvincible && isNewestRow(r)) continue;

                double dx = m.getCenterX() - centerX;
                double dy = m.getCenterY() - centerY;
                double distSq = dx * dx + dy * dy;

                if (distSq <= blastRadiusSq) {
                    // 触发消除
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

    // 新增：Creeper核爆清屏处理
    private void nukeBoard() {
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            // 无敌帧期间跳过最新生成的行
            if (newRowInvincible && isNewestRow(r)) continue;

            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone()) {
                    // 忽略对清屏核爆免疫的弹珠（例如Bedrock）
                    if (!m.isImmuneToCreeper()) {
                        if (!m.isScored() && scoreListener != null) {
                            scoreListener.accept(m, 20);
                            m.setScored(true);
                            lastRoundTotalScore += 20;
                            Color marbleColor = getMarbleColor(m.getColorType());
                            scoreNumbers.add(new ScreenGame.ScoreNumber(m.getCenterX(), m.getCenterY() - 15, 20, marbleColor));
                        }
                        // 略微随机的延迟爆炸带来非常酷炫的核爆视觉反馈
                        m.startPop(random.nextDouble() * 0.3);
                    }
                }
            }
        }
        ResourceManager.getInstance().playMassiveClear();
    }
}