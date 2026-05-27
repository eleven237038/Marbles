import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BoardGame - 核心游戏界面同步类
 */
public class BoardGame {
    private static final double SQRT3 = Math.sqrt(3);
    private static final int MIN_GROUP_SIZE = 3;
    private static final double SCALE = 1.25;
    private static final double NEIGHBOR_DIST_RATIO = 1.1;
    private static final double FLOATING_DIST_RATIO = 1.2;

    private static final double BASE_RATIO_W = 1.0 / 4.8;
    private static final double BASE_RATIO_H = 0.45;
    private static final double BARREL_LEN = 45 * SCALE;
    private static final int AMMO_SLOT_SIZE = (int)(16 * SCALE);
    private static final double AMMO_OFFSET_X_RATIO = 0.375;          

    private static final double MARBLE_MOVE_SPEED = 0.4;            
    private static final double LAUNCH_SPEED = 1500;                  

    private static final double CANNON_Y_RATIO = 4.0 / 5.2;
    private static final double DEADLINE_MARGIN_RATIO = 0.0;           

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

    private int gameWidth;    
    private int gameHeight;   
    private double side;       
    private int maxRowCount;
    private double ySpacing;   
    private double xSpacing;  
    private double baseX;
    private double accumulatedY;
    private int rowCount;
    private Marble[][] marbles;
    private Random random = new Random();

    private double neighborThresholdSq;
    private double floatingThresholdSq;

    public Point2D.Double cannon;
    private double headAngle = -Math.PI / 2;
    private int nextMarbleColor;
    private double topY;              
    private int currentBaseWidth;
    private int currentBaseHeight;

    private MarbleLaunch launchMarble;
    private boolean marbleLaunched = false;

    public BoardGame() {
        this.side = 24.22;  
        this.marbles = null;
        this.rowCount = 0;
        this.ySpacing = 0;
        this.xSpacing = 0;
        this.baseX = 0;
        this.accumulatedY = 0;
        this.cannon = new Point2D.Double();
        this.nextMarbleColor = 1;
        this.launchMarble = new MarbleLaunch();
    }

    public void init(int gameWidth, int gameHeight, int maxRowCount) {
        this.gameWidth = gameWidth;
        this.gameHeight = gameHeight;
        this.maxRowCount = maxRowCount;
        this.side = 24.22;  

        this.ySpacing = side * 1.5;
        this.xSpacing = side * SQRT3;

        this.neighborThresholdSq = (side * SQRT3 * NEIGHBOR_DIST_RATIO) * (side * SQRT3 * NEIGHBOR_DIST_RATIO);
        this.floatingThresholdSq = (side * SQRT3 * FLOATING_DIST_RATIO) * (side * SQRT3 * FLOATING_DIST_RATIO);

        this.currentBaseWidth = (int)(gameWidth * BASE_RATIO_W);
        this.currentBaseHeight = (int)(currentBaseWidth * BASE_RATIO_H);

        initRow(gameWidth, gameHeight);
        setCannonPosition(gameWidth, gameHeight);
        prepareNextMarble();
    }

    public double getSide() { return side; }
    public int getMaxRowCount() { return maxRowCount; }
    public void setMaxRowCount(int maxRowCount) { this.maxRowCount = maxRowCount; }
    public int getGameWidth() { return gameWidth; }
    public int getGameHeight() { return gameHeight; }

    public void StartMarbles(int gameWidth, int gameHeight, int initialRowCount) {
        this.gameWidth = gameWidth;
        this.ySpacing = side * 1.5;
        this.xSpacing = side * SQRT3;

        int totalRows = maxRowCount + initialRowCount;
        this.rowCount = initialRowCount;
        this.marbles = new Marble[totalRows][];

        for (int generatedRows = 0; generatedRows < initialRowCount; generatedRows++) {
            int actualRow = maxRowCount + generatedRows;
            AddMarbleRow(actualRow, gameWidth, initialRowCount);

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

    public void AddMarbleRow(int row, int gameWidth, int initialRowCount) {
        double baseY = -2 * side;

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

        int perRow = (int)(gameWidth / xSpacing);
        this.marbles[row] = new Marble[perRow];

        for (int col = 0; col < perRow; col++) {
            this.marbles[row][col] = new Marble();
            this.marbles[row][col].init(baseX + col * xSpacing, baseY, row, col);
        }
        this.baseX = baseX + (baseX % xSpacing == 0 ? -xSpacing / 2 : xSpacing / 2);
    }

    public void initRow(int gameWidth, int gameHeight) {
        StartMarbles(gameWidth, gameHeight, 4);
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
            AddMarbleRow(newRow, gameWidth, this.rowCount);
            accumulatedY -= ySpacing;
        }

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

    public void prepareNextMarble() {
        nextMarbleColor = random.nextInt(4) + 1;
    }

    public void launchMarbleAt(double targetX, double targetY) {
        Point2D.Double muzzle = getMuzzlePosition();
        launchMarble = new MarbleLaunch();
        launchMarble.setColorType(nextMarbleColor);
        launchMarble.init(muzzle.x, muzzle.y, -1, -1);
        launchMarble.launch(targetX, targetY);
        marbleLaunched = true;
    }

    public boolean isMarbleLaunched() { return marbleLaunched; }
    public MarbleLaunch getMarbleLaunch() { return launchMarble; }
    public void setMarbleLaunched(boolean launched) { this.marbleLaunched = launched; }

    public int getNextMarbleColorType() { return nextMarbleColor; }
    public void setNextMarbleColorType(int type) {
        if (type >= 1 && type <= 4) this.nextMarbleColor = type;
    }

    public void setCannonPosition(int w, int h) {
        currentBaseWidth = (int)(w * BASE_RATIO_W);
        currentBaseHeight = (int)(currentBaseWidth * BASE_RATIO_H);

        cannon.x = w / 2.0;
        cannon.y = h - (h / 5.2);
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
    public Point2D.Double getCannon() { return cannon; }

    private boolean isMarbleActive(Marble m) {
        return m != null && m.isInitialized() && !m.isPopping() && !m.isFalling() && !m.isAlone();
    }

    public void attachMarble(Marble launchMarble, int gameWidth) {
        double lx = launchMarble.getCenterX();
        double ly = launchMarble.getCenterY();

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
        int maxCols = (int)(gameWidth / xSpacing);
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
                            if (dx * dx + dy * dy <= neighborThresholdSq) {
                                dfs(nr, nc, targetColor, visited, res);
                            }
                        }
                    }
                }
            }
        }
    }

    public void checkAllEdgeAttachments(int gameWidth) {
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
                    double xSpacing = this.xSpacing;

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
                            if (dx * dx + dy * dy <= floatingThresholdSq) {
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

    public void draw(Graphics2D g) {
        if (marbles == null) return;
        for (int r = 0; r < marbles.length; r++) {
            if (marbles[r] == null) continue;
            for (Marble hex : marbles[r]) {
                if (hex != null) hex.draw(g);
            }
        }
    }

    public void drawMarbleLaunch(Graphics2D g) {
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

    public void fire() { }
}