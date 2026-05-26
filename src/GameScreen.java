import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GameScreen - 整合弹珠游戏所有核心游戏逻辑
 * 包括: Marble[][] 生成规则、发射弹珠规则、碰撞检测、消除规则等
 */
public class GameScreen {
    // ==================== 常量定义 ====================
    private static final double SQRT3 = Math.sqrt(3);
    private static final int MIN_GROUP_SIZE = 3;
    private static final double SCALE = 1.25;
    private static final double BASE_RATIO_W = 1.0 / 5.0;
    private static final double BASE_RATIO_H = 0.45;
    private static final double BARREL_LEN = 45 * SCALE;
    private static final int AMMO_SLOT_SIZE = (int)(16 * SCALE);
    private static final double AMMO_OFFSET_X_RATIO = 0.375;

    // ==================== 颜色定义 ====================
    private static final Color[] MARBLE_COLORS = {
        null,
        new Color(220, 30, 30),
        new Color(20, 80, 220),
        new Color(240, 200, 20),
        new Color(160, 30, 200)
    };

    private static final Color BASE_COLOR_TOP = new Color(255, 180, 80);
    private static final Color BASE_COLOR_BOTTOM = new Color(255, 100, 40);
    private static final Color TURRET_COLOR_TOP = new Color(255, 200, 110);
    private static final Color TURRET_COLOR_BOTTOM = new Color(255, 140, 60);
    private static final Color BARREL_COLOR_TOP = new Color(220, 220, 240);
    private static final Color BARREL_COLOR_BOTTOM = new Color(160, 170, 200);
    private static final Color BARREL_TIP_COLOR = new Color(255, 220, 120);
    private static final Color AMMO_BG_COLOR = new Color(70, 50, 80);
    private static final Color EYE_WHITE = new Color(255, 255, 255);
    private static final Color EYE_PUPIL = new Color(40, 40, 60);
    private static final Color EYE_HIGHLIGHT = new Color(255, 255, 255);

    // ==================== 游戏状态 ====================
    private int screenWidth;
    private int screenHeight;
    private double side;
    private int maxRowCount;
    private double ySpacing;
    private double baseX;
    private double accumulatedY;
    private int rowCount;
    private Marble[][] marbles;
    private Random random = new Random();

    // 发射台状态
    private Point2D.Double cannon;
    private double headAngle = -Math.PI / 2;
    private int nextMarbleColor;
    private double topY;
    private int currentBaseWidth;
    private int currentBaseHeight;

    // 发射弹珠状态
    private LaunchMarble launchMarble;
    private boolean marbleLaunched = false;

    // ==================== Marble 内部类 ====================
    public static class Marble {
        private double cx, cy;
        private double side;
        private boolean initialized;
        private boolean verticesDirty;
        public static final int RED = 1;
        public static final int BLUE = 2;
        public static final int YELLOW = 3;
        public static final int PURPLE = 4;

        // 动画状态相关
        private boolean popping = false;
        private boolean falling = false;
        private boolean dead = false;
        private double popDelay = 0;
        private double popProgress = 0;

        // 掉落物理参数
        private double fallVy = 0;
        private double fallAy = 1500;

        // 单独动画状态
        private boolean alone = false;
        private double aloneSlideVx = 0;
        private double aloneSlideVy = 0;
        private double aloneDelay = 0;

        // 颜色定义
        private static final Color[] BASE_COLOR = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200)
        };
        private static final Color[] BRIGHT_COLOR = {
            null,
            new Color(255, 130, 130),
            new Color(110, 190, 255),
            new Color(255, 250, 180),
            new Color(220, 130, 255)
        };
        private static final Color[] DARK_COLOR = {
            null,
            new Color(120, 10, 10),
            new Color(10, 40, 120),
            new Color(160, 120, 0),
            new Color(90, 10, 120)
        };

        private static final Random random = new Random();
        private int colorType;
        private int row;
        private int col;
        private int[][] edgeAttachment;
        private boolean markedForRemove = false;

        public Marble() {
            this.cx = 0;
            this.cy = 0;
            this.side = 24.22;
            this.initialized = false;
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
            this.initialized = true;
        }

        public void startPop(double delay) {
            this.popping = true;
            this.popDelay = delay;
            this.popProgress = 0;
        }

        public void startFalling(double delay) {
            this.falling = true;
            this.popping = false;
            this.alone = false;
            this.popDelay = delay;
            this.fallVy = -150 - (random.nextDouble() * 80);
        }

        public void startAlone(int triggerEdge, double delay) {
            this.alone = true;
            this.falling = false;
            this.popping = false;
            this.popDelay = delay;
            double angle = triggerEdge * Math.PI / 3.0;
            double slideSpeed = 120 + random.nextDouble() * 60;
            this.aloneSlideVx = Math.cos(angle) * slideSpeed;
            this.aloneSlideVy = -Math.abs(Math.sin(angle) * slideSpeed);
        }

        public boolean isAlone() { return alone; }
        public boolean isPopping() { return popping; }
        public boolean isFalling() { return falling; }
        public boolean isDead() { return dead; }

        public void update(double dt) {
            if (popping) {
                if (popDelay > 0) {
                    popDelay -= dt;
                } else {
                    popProgress += dt * 6.0;
                    if (popProgress >= 1.0) {
                        dead = true;
                    }
                }
            } else if (alone) {
                if (popDelay > 0) {
                    popDelay -= dt;
                } else {
                    if (aloneDelay < 0.3) {
                        cx += aloneSlideVx * dt;
                        cy += aloneSlideVy * dt;
                        aloneDelay += dt;
                        verticesDirty = true;
                    } else {
                        cy += (400 + random.nextDouble() * 100) * dt;
                        verticesDirty = true;
                        if (cy > 1500) {
                            dead = true;
                        }
                    }
                }
            } else if (falling) {
                if (popDelay > 0) {
                    popDelay -= dt;
                } else {
                    cy += fallVy * dt;
                    fallVy += fallAy * dt;
                    verticesDirty = true;
                    if (cy > 1500) {
                        dead = true;
                    }
                }
            }
        }

        public void setCenter(double cx, double cy) {
            this.cx = cx;
            this.cy = cy;
            this.verticesDirty = true;
        }

        public void recalculateVerticesIfDirty() {}
        public void setSide(double side) { this.side = side; }
        public double getCenterX() { return cx; }
        public double getCenterY() { return cy; }
        public double getSide() { return side; }
        public boolean isInitialized() { return initialized; }
        public int getColorType() { return colorType; }
        public void setColorType(int colorType) { this.colorType = colorType; }
        public int getRow() { return row; }
        public int getCol() { return col; }
        public int[][] getEdgeAttachment() { return edgeAttachment; }
        public void setEdgeAttachment(int[][] edgeAttachment) { this.edgeAttachment = edgeAttachment; }
        public void markForRemove(boolean b) { markedForRemove = b; }
        public boolean isMarkedForRemove() { return markedForRemove; }

        public void draw(Graphics2D g) {
            if (!initialized || dead) return;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double scale = 1.0;
            float alpha = 1.0f;

            if (popping && popDelay <= 0) {
                scale = 1.0 + popProgress * 0.4;
                alpha = 1.0f - (float)popProgress;
                if (alpha < 0f) alpha = 0f;
                if (alpha > 1f) alpha = 1f;
            }

            Composite originalComposite = null;
            if (alpha < 1.0f) {
                originalComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            }

            double radius = side * 0.866 * scale;
            double x = cx - radius;
            double y = cy - radius;
            double diameter = radius * 2;

            Color base = BASE_COLOR[colorType];
            Color bright = BRIGHT_COLOR[colorType];
            Color dark = DARK_COLOR[colorType];

            g.setColor(new Color(0,0,0,40));
            g.fillOval((int)(x + 2 * scale), (int)(y + 2 * scale), (int)diameter, (int)diameter);

            Point2D center = new Point2D.Double(cx, cy);
            float[] stop = {0f, 0.6f, 1f};
            Color[] gradColor = {bright, base, dark};
            RadialGradientPaint ballGrad = new RadialGradientPaint(center, (float)radius, stop, gradColor);
            g.setPaint(ballGrad);
            g.fillOval((int)x, (int)y, (int)diameter, (int)diameter);

            g.setStroke(new BasicStroke(1.8f * (float)scale));
            g.setColor(new Color(0,0,0,70));
            g.drawOval((int)x, (int)y, (int)diameter, (int)diameter);

            g.setStroke(new BasicStroke(0.8f * (float)scale));
            g.setColor(new Color(255,255,255,50));
            g.drawOval((int)(x + 1 * scale), (int)(y + 1 * scale), (int)(diameter - 2 * scale), (int)(diameter - 2 * scale));

            g.setColor(new Color(255,255,255,200));
            double highlightR = radius * 0.32;
            g.fillOval((int)(cx - radius * 0.48), (int)(cy - radius * 0.48), (int)highlightR, (int)highlightR);

            g.setColor(new Color(255,255,255,120));
            int reflect1Size = Math.max(1, (int)(4 * scale));
            int reflect2Size = Math.max(1, (int)(3 * scale));
            g.fillOval((int)(cx + radius * 0.25), (int)(cy - radius * 0.2), reflect1Size, reflect1Size);
            g.fillOval((int)(cx - radius * 0.2), (int)(cy + radius * 0.3), reflect2Size, reflect2Size);

            if (alpha < 1.0f) {
                g.setComposite(originalComposite);
            }
        }

        public void reset() {
            this.cx = 0;
            this.cy = 0;
            this.side = 24.22;
            this.initialized = false;
            this.popping = false;
            this.falling = false;
            this.alone = false;
            this.dead = false;
            this.popDelay = 0;
            this.popProgress = 0;
            this.fallVy = 0;
            this.aloneDelay = 0;
        }
    }

    // ==================== LaunchMarble 内部类 ====================
    public class LaunchMarble {
        private double vx;
        private double vy;
        private double launchSpeed = 500;
        private boolean launched;
        private double prevCx;
        private double prevCy;
        private Marble marble;

        public LaunchMarble() {
            this.vx = 0;
            this.vy = 0;
            this.launched = false;
            this.prevCx = 0;
            this.prevCy = 0;
            this.marble = new Marble();
        }

        public void setScreenSize(int width, int height) {
            // 仅供继承GameEngine的类使用,GameScreen直接使用screenWidth/screenHeight
        }

        public void init(double cx, double cy, int row, int col) {
            marble.init(cx, cy, row, col);
            this.prevCx = cx;
            this.prevCy = cy;
        }

        public void launch(double targetX, double targetY) {
            double dx = targetX - getCenterX();
            double dy = targetY - getCenterY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance > 1) {
                vx = (dx / distance) * launchSpeed;
                vy = (dy / distance) * launchSpeed;
                launched = true;
                prevCx = getCenterX();
                prevCy = getCenterY();
            }
        }

        public void reset(double x, double y) {
            this.vx = 0;
            this.vy = 0;
            this.launched = false;
            marble.setCenter(x, y);
            this.prevCx = x;
            this.prevCy = y;
        }

        public void update(double dt) {
            if (launched && screenWidth > 0 && screenHeight > 0) {
                prevCx = getCenterX();
                prevCy = getCenterY();

                double cx = prevCx + vx * dt;
                double cy = prevCy + vy * dt;
                double radius = side * 0.866;

                // 左右墙壁碰撞
                if (cx <= radius) {
                    cx = radius;
                    vx = -vx;
                } else if (cx >= screenWidth - radius) {
                    cx = screenWidth - radius;
                    vx = -vx;
                }

                // 顶部判定:接触顶部时停止,触发吸附
                if (cy <= radius) {
                    cy = radius;
                    vy = 0;
                    vx = 0;
                    launched = false;
                }

                marble.setCenter(cx, cy);
            }
        }

        public boolean isLaunched() { return launched; }
        public double getVx() { return vx; }
        public double getVy() { return vy; }
        public double getPrevCenterX() { return prevCx; }
        public double getPrevCenterY() { return prevCy; }
        public void setLaunchSpeed(double speed) { this.launchSpeed = speed; }

        public double getCenterX() { return marble.getCenterX(); }
        public double getCenterY() { return marble.getCenterY(); }
        public double getSide() { return marble.getSide(); }
        public int getColorType() { return marble.getColorType(); }
        public void setColorType(int type) { marble.setColorType(type); }

        public void draw(Graphics2D g) {
            if (!launched) return;
            marble.draw(g);
        }
    }

    // ==================== GameScreen 构造函数 ====================
    public GameScreen() {
        this.side = 24.22;
        this.marbles = null;
        this.rowCount = 0;
        this.ySpacing = 0;
        this.baseX = 0;
        this.accumulatedY = 0;
        this.cannon = new Point2D.Double();
        this.nextMarbleColor = 1;
        this.launchMarble = new LaunchMarble();
    }

    // ==================== 初始化 ====================
    public void init(int screenWidth, int screenHeight, int maxRowCount) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.maxRowCount = maxRowCount;
        this.side = 24.22;
        initRow(screenWidth, screenHeight);
        setCannonPosition(screenWidth, screenHeight);
        prepareNextMarble();
    }

    public double getSide() { return side; }
    public int getMaxRowCount() { return maxRowCount; }
    public void setMaxRowCount(int maxRowCount) { this.maxRowCount = maxRowCount; }

    // ==================== Marble[][] 生成规则 ====================
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
        }
        this.baseX = baseX + (baseX % xSpacing == 0 ? -xSpacing / 2 : xSpacing / 2);
    }

    public void initRow(int screenWidth, int screenHeight) {
        StartMarbles(screenWidth, screenHeight, 4);
    }

    // ==================== 游戏更新 ====================
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

        // 更新发射弹珠
        if (launchMarble.isLaunched()) {
            launchMarble.update(dt);
        }
    }

    public Marble getHex(int row, int col) {
        if (marbles != null && row >= 0 && row < marbles.length && col >= 0 && col < marbles[row].length) {
            return marbles[row][col];
        }
        return null;
    }

    public int getRowCount() { return rowCount; }
    public int getMarblesLength() { return marbles != null ? marbles.length : 0; }
    public double getVerticalSpacing() { return ySpacing; }

    // ==================== 发射弹珠规则 ====================
    public void prepareNextMarble() {
        nextMarbleColor = random.nextInt(4) + 1;
    }

    public void launchMarbleAt(double targetX, double targetY) {
        Point2D.Double muzzle = getMuzzlePosition();
        launchMarble = new LaunchMarble();
        launchMarble.setColorType(nextMarbleColor);
        launchMarble.init(muzzle.x, muzzle.y, -1, -1);
        launchMarble.launch(targetX, targetY);
        marbleLaunched = true;
    }

    public boolean isMarbleLaunched() { return marbleLaunched; }
    public LaunchMarble getLaunchMarble() { return launchMarble; }
    public void setMarbleLaunched(boolean launched) { this.marbleLaunched = launched; }

    public int getNextMarbleColorType() { return nextMarbleColor; }
    public void setNextMarbleColorType(int type) {
        if (type >= 1 && type <= 4) this.nextMarbleColor = type;
    }

    // ==================== 发射台相关 ====================
    private double calculateTopY() {
        return maxRowCount % 2 == 1 ?
                3 * ((maxRowCount - 1) / 2.0 + Math.sqrt(3) / 2) * side :
                3 * ((maxRowCount - 2) / 2.0 + Math.sqrt(3) / 2 + 0.5) * side;
    }

    public void setCannonPosition(int w, int h) {
        currentBaseWidth = (int)(w * BASE_RATIO_W);
        currentBaseHeight = (int)(currentBaseWidth * BASE_RATIO_H);

        cannon.x = w / 2.0;
        cannon.y = h - (h / 5.0);
        topY = cannon.y - currentBaseHeight;
    }

    public double getTopY() { return topY; }

    public Point2D.Double getMuzzlePosition() {
        double muzzleX = cannon.x + Math.cos(headAngle) * BARREL_LEN;
        double muzzleY = cannon.y + Math.sin(headAngle) * BARREL_LEN;
        return new Point2D.Double(muzzleX, muzzleY);
    }

    public void updateCannonAngle(double mx, double my) {
        double dx = mx - cannon.x;
        double dy = my - cannon.y;
        if (dy < 0) {
            headAngle = Math.atan2(dy, dx);
            double maxLeft = -Math.PI * 5 / 6;
            double maxRight = -Math.PI / 6;
            if (headAngle < maxLeft) headAngle = maxLeft;
            if (headAngle > maxRight) headAngle = maxRight;
        } else {
            headAngle = -Math.PI / 2;
        }
    }

    public double getCannonX() { return cannon.x; }
    public double getCannonY() { return cannon.y; }

    // ==================== 碰撞检测与吸附 ====================
    private boolean isMarbleActive(Marble m) {
        return m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone();
    }

    public void attachMarble(Marble launchMarble, int screenWidth) {
        double lx = launchMarble.getCenterX();
        double ly = launchMarble.getCenterY();
        double xSpacing = side * SQRT3;

        // 1. 寻找参考球,推算网格偏移
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

        // 2. 计算目标行
        int rowOffset = (int) Math.round((ref.getCenterY() - ly) / ySpacing);
        int targetRow = ref.getRow() + rowOffset;
        if (targetRow < 0) targetRow = 0;

        // 3. 计算基础X偏移
        double refBaseX = ref.getCenterX() - ref.getCol() * xSpacing;
        double targetBaseX = refBaseX;

        int refBaseState = (int) Math.round(refBaseX / (xSpacing / 2.0));
        if (Math.abs(targetRow - ref.getRow()) % 2 == 1) {
            targetBaseX = (refBaseState == 1) ? xSpacing : (xSpacing / 2.0);
        }

        // 4. 计算目标列
        int targetCol = (int) Math.round((lx - targetBaseX) / xSpacing);
        int maxCols = (int)(screenWidth / xSpacing);
        if (targetCol < 0) targetCol = 0;
        if (targetCol > maxCols) targetCol = maxCols;

        // 检测位置是否被占用
        boolean isOccupied = false;
        if (targetRow < marbles.length && marbles[targetRow] != null && targetCol < marbles[targetRow].length) {
            isOccupied = isMarbleActive(marbles[targetRow][targetCol]);
        }

        // 邻域搜索找空位
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

        // 扩容
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

        // 5. 实例化弹珠
        double exactX = targetBaseX + targetCol * xSpacing;
        double exactY = ref.getCenterY() - (targetRow - ref.getRow()) * ySpacing;

        marbles[targetRow][targetCol] = new Marble();
        marbles[targetRow][targetCol].setColorType(launchMarble.getColorType());
        marbles[targetRow][targetCol].init(exactX, exactY, targetRow, targetCol);

        checkConnectedFromLaunch(targetRow, targetCol, launchMarble.getColorType());
    }

    // ==================== 消除规则 ====================
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
                double dist = 0;
                if (launchM != null) {
                    dist = Math.sqrt(Math.pow(m.getCenterX() - originX, 2) + Math.pow(m.getCenterY() - originY, 2));
                }
                double delay = dist / 600.0;
                m.startPop(delay);
            }

            checkFloatingMarbles();
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

    // ==================== 悬空检测与消除 ====================
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

    private void checkFloatingMarbles() {
        if (marbles == null) return;

        // 找最高位置
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

        // 天花板节点
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

        // DFS遍历
        for (Marble cm : ceilingMarbles) {
            if (!visited[cm.getRow()][cm.getCol()]) {
                dfsFloating(cm.getRow(), cm.getCol(), visited);
            }
        }

        // 未遍历到的开始掉落
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (int c = 0; c < marbles[r].length; c++) {
                Marble m = marbles[r][c];
                if (isMarbleActive(m) && !visited[r][c]) {
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

    public Marble[] getRow(int row) {
        if (marbles != null && row >= 0 && row < marbles.length) {
            return marbles[row];
        }
        return null;
    }

    // ==================== 绘制 ====================
    public void draw(Graphics2D g) {
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                if (hex != null) hex.draw(g);
            }
        }
    }

    public void drawLaunchMarble(Graphics2D g) {
        if (launchMarble != null) {
            launchMarble.draw(g);
        }
    }

    public void drawLaunchPad(Graphics2D g, int w, int h) {
        setCannonPosition(w, h);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[]{10, 7}, 0));
        for (int i = 0; i < w; i += (int)(20 * SCALE)) {
            float hue = (float) (i / (float) w);
            Color rainbow = Color.getHSBColor(hue, 0.8f, 0.9f);
            g.setColor(rainbow);
            g.drawLine(i, (int) topY, Math.min(i + (int)(12 * SCALE), w), (int) topY);
        }
        g.setStroke(new BasicStroke(1));
    }

    public void drawCannon(Graphics2D g, double mx, double my) {
        updateCannonAngle(mx, my);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int baseX = (int)(cannon.x - currentBaseWidth/2);
        int baseY = (int)(cannon.y - currentBaseHeight/2);
        Point2D center = new Point2D.Double(cannon.x, cannon.y - currentBaseHeight/4);
        float[] stops = {0f, 0.7f, 1f};
        Color[] baseColors = {BASE_COLOR_TOP, BASE_COLOR_BOTTOM, new Color(200, 70, 20)};
        RadialGradientPaint baseGrad = new RadialGradientPaint(center, currentBaseWidth/1.5f, stops, baseColors);
        g.setPaint(baseGrad);
        g.fillOval(baseX, baseY, currentBaseWidth, currentBaseHeight);

        g.setColor(new Color(255, 255, 200, 120));
        g.setStroke(new BasicStroke((int)(2 * SCALE)));
        g.drawOval(baseX + (int)(2 * SCALE), baseY + (int)(2 * SCALE), currentBaseWidth - (int)(4 * SCALE), currentBaseHeight - (int)(4 * SCALE));

        g.setColor(new Color(255, 255, 180));
        drawStar(g, cannon.x - currentBaseWidth/2 - (int)(5 * SCALE), cannon.y - (int)(5 * SCALE), (int)(6 * SCALE));
        drawStar(g, cannon.x + currentBaseWidth/2 + (int)(5 * SCALE), cannon.y - (int)(5 * SCALE), (int)(6 * SCALE));

        int turretWidth = (int)(currentBaseWidth * 0.75);
        int turretHeight = (int)(turretWidth * BASE_RATIO_H);
        int turretX = (int)(cannon.x - turretWidth/2);
        int turretY = (int)(cannon.y - turretHeight/2);
        Point2D turretCenter = new Point2D.Double(cannon.x, cannon.y - (int)(5 * SCALE));
        RadialGradientPaint turretGrad = new RadialGradientPaint(turretCenter, turretWidth/1.3f,
                new float[]{0f, 0.8f, 1f},
                new Color[]{TURRET_COLOR_TOP, TURRET_COLOR_BOTTOM, new Color(180, 80, 30)});
        g.setPaint(turretGrad);
        g.fillRoundRect(turretX, turretY, turretWidth, turretHeight, (int)(20 * SCALE), (int)(20 * SCALE));

        g.setColor(new Color(255, 255, 220, 100));
        g.fillRoundRect(turretX + (int)(5 * SCALE), turretY + (int)(2 * SCALE), turretWidth - (int)(10 * SCALE), (int)(8 * SCALE), (int)(5 * SCALE), (int)(5 * SCALE));

        int eyeRadius = (int)(currentBaseWidth * 0.1125);
        int leftEyeX = (int)(cannon.x - currentBaseWidth * 0.2);
        int leftEyeY = (int)(cannon.y - currentBaseWidth * 0.15);
        int rightEyeX = (int)(cannon.x + currentBaseWidth * 0.1);
        int rightEyeY = (int)(cannon.y - currentBaseWidth * 0.15);

        g.setColor(EYE_WHITE);
        g.fillOval(leftEyeX - eyeRadius, leftEyeY - eyeRadius/2, eyeRadius*2, eyeRadius);
        g.fillOval(rightEyeX - eyeRadius, rightEyeY - eyeRadius/2, eyeRadius*2, eyeRadius);

        double angleToMouse = Math.atan2(my - leftEyeY, mx - leftEyeX);
        double pupilOffsetX = Math.cos(angleToMouse) * (2.5 * SCALE);
        double pupilOffsetY = Math.sin(angleToMouse) * (2.5 * SCALE);
        g.setColor(EYE_PUPIL);
        g.fillOval((int)(leftEyeX - 3 * SCALE + pupilOffsetX), (int)(leftEyeY - 3 * SCALE + pupilOffsetY), (int)(6 * SCALE), (int)(6 * SCALE));
        g.fillOval((int)(rightEyeX - 3 * SCALE + pupilOffsetX), (int)(rightEyeY - 3 * SCALE + pupilOffsetY), (int)(6 * SCALE), (int)(6 * SCALE));

        g.setColor(EYE_HIGHLIGHT);
        g.fillOval((int)(leftEyeX - 1 * SCALE + pupilOffsetX), (int)(leftEyeY - 4 * SCALE + pupilOffsetY), (int)(3 * SCALE), (int)(3 * SCALE));
        g.fillOval((int)(rightEyeX - 1 * SCALE + pupilOffsetX), (int)(rightEyeY - 4 * SCALE + pupilOffsetY), (int)(3 * SCALE), (int)(3 * SCALE));

        g.setColor(new Color(255, 160, 80));
        int[] earXLeft = {
                (int)(cannon.x - currentBaseWidth * 0.35),
                (int)(cannon.x - currentBaseWidth * 0.475),
                (int)(cannon.x - currentBaseWidth * 0.275)
        };
        int[] earYLeft = {
                (int)(cannon.y - currentBaseWidth * 0.25),
                (int)(cannon.y - currentBaseWidth * 0.4),
                (int)(cannon.y - currentBaseWidth * 0.3125)
        };
        g.fillPolygon(earXLeft, earYLeft, 3);
        int[] earXRight = {
                (int)(cannon.x + currentBaseWidth * 0.25),
                (int)(cannon.x + currentBaseWidth * 0.375),
                (int)(cannon.x + currentBaseWidth * 0.175)
        };
        int[] earYRight = {
                (int)(cannon.y - currentBaseWidth * 0.25),
                (int)(cannon.y - currentBaseWidth * 0.4),
                (int)(cannon.y - currentBaseWidth * 0.3125)
        };
        g.fillPolygon(earXRight, earYRight, 3);

        int barrelStartX = (int)(cannon.x + Math.cos(headAngle) * (12 * SCALE));
        int barrelStartY = (int)(cannon.y + Math.sin(headAngle) * (12 * SCALE));
        int barrelEndX = (int)(cannon.x + Math.cos(headAngle) * BARREL_LEN);
        int barrelEndY = (int)(cannon.y + Math.sin(headAngle) * BARREL_LEN);

        Point2D barrelStart = new Point2D.Double(barrelStartX, barrelStartY);
        Point2D barrelEnd = new Point2D.Double(barrelEndX, barrelEndY);
        LinearGradientPaint barrelGrad = new LinearGradientPaint(barrelStart, barrelEnd,
                new float[]{0f, 1f}, new Color[]{BARREL_COLOR_TOP, BARREL_COLOR_BOTTOM});
        g.setStroke(new BasicStroke((int)(12 * SCALE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(barrelGrad);
        g.drawLine(barrelStartX, barrelStartY, barrelEndX, barrelEndY);

        g.setStroke(new BasicStroke((int)(15 * SCALE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 200, 100, 60));
        g.drawLine(barrelStartX, barrelStartY, barrelEndX, barrelEndY);

        int tipX = (int)(cannon.x + Math.cos(headAngle) * BARREL_LEN);
        int tipY = (int)(cannon.y + Math.sin(headAngle) * BARREL_LEN);
        g.setStroke(new BasicStroke(1));
        RadialGradientPaint tipGrad = new RadialGradientPaint(tipX, tipY, (int)(12 * SCALE),
                new float[]{0f, 1f}, new Color[]{BARREL_TIP_COLOR, new Color(255, 100, 30)});
        g.setPaint(tipGrad);
        g.fillOval(tipX - (int)(8 * SCALE), tipY - (int)(8 * SCALE), (int)(16 * SCALE), (int)(16 * SCALE));
        g.setColor(Color.WHITE);
        g.fillOval(tipX - (int)(3 * SCALE), tipY - (int)(3 * SCALE), (int)(6 * SCALE), (int)(6 * SCALE));

        int slotX = (int)(cannon.x + currentBaseWidth * AMMO_OFFSET_X_RATIO);
        int slotY = (int)(cannon.y - currentBaseWidth * 0.15);
        int size = AMMO_SLOT_SIZE;

        Point2D slotCenter = new Point2D.Double(slotX, slotY);
        RadialGradientPaint slotGrad = new RadialGradientPaint(slotCenter, size/2f,
                new float[]{0f, 0.6f, 1f}, new Color[]{new Color(160, 100, 180), AMMO_BG_COLOR, new Color(30, 20, 40)});
        g.setPaint(slotGrad);
        g.fillRoundRect(slotX - size/2, slotY - size/2, size, size, (int)(8 * SCALE), (int)(8 * SCALE));

        g.setColor(new Color(255, 215, 0, 200));
        g.setStroke(new BasicStroke((float)(1.5 * SCALE)));
        g.drawRoundRect(slotX - size/2, slotY - size/2, size, size, (int)(8 * SCALE), (int)(8 * SCALE));

        int marbleRadius = (int)(size * 0.4);
        Color marbleColor = MARBLE_COLORS[nextMarbleColor];
        if (marbleColor != null) {
            Point2D marbleCenter = new Point2D.Double(slotX, slotY);
            RadialGradientPaint marbleGrad = new RadialGradientPaint(marbleCenter, marbleRadius,
                    new float[]{0f, 0.7f, 1f}, new Color[]{marbleColor.brighter(), marbleColor, marbleColor.darker()});
            g.setPaint(marbleGrad);
            g.fillOval(slotX - marbleRadius, slotY - marbleRadius, marbleRadius*2, marbleRadius*2);
            g.setColor(new Color(0,0,0,60));
            g.drawOval(slotX - marbleRadius, slotY - marbleRadius, marbleRadius*2, marbleRadius*2);
            g.setColor(Color.WHITE);
            g.fillOval(slotX - marbleRadius/2, slotY - marbleRadius/2, marbleRadius/2, marbleRadius/2);
        }

        long time = System.currentTimeMillis();
        float angle1 = (time % 1000) / 1000f * (float)Math.PI * 2;
        drawStar(g, slotX + (int)(Math.cos(angle1) * 10 * SCALE), slotY + (int)(Math.sin(angle1) * 10 * SCALE), (int)(2 * SCALE));
        drawStar(g, slotX - (int)(Math.cos(angle1+2) * 8 * SCALE), slotY + (int)(Math.sin(angle1+1.5) * 8 * SCALE), (int)(2 * SCALE));

        double dyToLine = cannon.y - topY;
        double sinTheta = Math.sin(headAngle);
        double distanceToLine = dyToLine / (-sinTheta);
        if (distanceToLine < 0) distanceToLine = dyToLine;
        int lineEndX = (int)(cannon.x + Math.cos(headAngle) * distanceToLine);
        int lineEndY = (int)(cannon.y + Math.sin(headAngle) * distanceToLine);
        lineEndY = (int) topY;

        g.setColor(new Color(255, 100, 200, 100));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{(int)(6 * SCALE), (int)(8 * SCALE)}, 0));
        g.drawLine((int)cannon.x, (int)cannon.y, lineEndX, lineEndY);

        g.setStroke(new BasicStroke((int)(3 * SCALE)));
        g.setColor(new Color(100, 200, 255, 180));
        g.drawOval(lineEndX - (int)(12 * SCALE), lineEndY - (int)(12 * SCALE), (int)(24 * SCALE), (int)(24 * SCALE));
        g.setColor(new Color(255, 180, 80, 200));
        g.drawOval(lineEndX - (int)(8 * SCALE), lineEndY - (int)(8 * SCALE), (int)(16 * SCALE), (int)(16 * SCALE));

        g.setStroke(new BasicStroke((float)(1.5 * SCALE)));
        g.setColor(new Color(255, 50, 100, 200));
        g.drawOval(lineEndX - (int)(6 * SCALE), lineEndY - (int)(6 * SCALE), (int)(12 * SCALE), (int)(12 * SCALE));
        g.drawOval(lineEndX - (int)(2 * SCALE), lineEndY - (int)(2 * SCALE), (int)(4 * SCALE), (int)(4 * SCALE));
        g.fillOval(lineEndX - (int)(1 * SCALE), lineEndY - (int)(1 * SCALE), (int)(2 * SCALE), (int)(2 * SCALE));

        g.setStroke(new BasicStroke(1));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private static final int[] STAR_X_POINTS = new int[10];
    private static final int[] STAR_Y_POINTS = new int[10];

    private void drawStar(Graphics2D g, double x, double y, int size) {
        double outerR = size;
        double innerR = size * 0.4;
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2 * i / 10 - Math.PI / 2;
            double r = (i % 2 == 0) ? outerR : innerR;
            STAR_X_POINTS[i] = (int)(x + Math.cos(angle) * r);
            STAR_Y_POINTS[i] = (int)(y + Math.sin(angle) * r);
        }
        g.setColor(new Color(255, 255, 200, 200));
        g.fillPolygon(STAR_X_POINTS, STAR_Y_POINTS, 10);
        g.setColor(new Color(255, 200, 50));
        g.drawPolygon(STAR_X_POINTS, STAR_Y_POINTS, 10);
    }

    // ==================== 重置 ====================
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