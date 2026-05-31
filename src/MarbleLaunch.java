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

import java.util.Random;

/**
 * MarbleLaunch - 发射弹珠类，继承自Marble
 * MarbleLaunch - Launchable marble class, extends Marble
 */
public class MarbleLaunch extends Marble {
    // 水平/垂直速度 / Horizontal and vertical velocity
    private double vx;
    private double vy;
    // 发射速度 / Launch speed
    private double launchSpeed = 500;
    // 是否已发射 / Whether the marble has been launched
    private boolean launched;
    private int screenWidth;
    private int screenHeight;
    // 上一次位置用于碰撞检测 / Previous position for collision detection
    private double prevCx;
    private double prevCy;
    // Creeper生成计数器 / Creeper spawn counter
    private static int creeperCounter = 0;
    // Bedrock生成计数器 / Bedrock spawn counter
    private static int bedrockCounter = 0;
    // 生成间隔 / Spawn intervals
    private static final int CREEPER_INTERVAL = 6;
    private static final int BEDROCK_INTERVAL = 12;

    public MarbleLaunch() {
        super();
        this.vx = 0;
        this.vy = 0;
        this.launched = false;
        this.screenWidth = 0;
        this.screenHeight = 0;
    }

    /**
     * 设置屏幕尺寸，用于边界碰撞检测
     * Set screen size for boundary collision detection
     */
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    @Override
    public void init(double cx, double cy, int row, int col) {
        super.init(cx, cy, row, col);
        this.prevCx = cx;
        this.prevCy = cy;
    }

    /**
     * 发射弹珠到目标位置
     * Launch marble towards target position
     */
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

    /**
     * 重置弹珠到指定位置
     * Reset marble to specified position
     */
    public void reset(double x, double y) {
        this.vx = 0;
        this.vy = 0;
        this.launched = false;
        setCenter(x, y);
        this.prevCx = x;
        this.prevCy = y;
    }

    /**
     * 更新弹珠位置，包含边界碰撞检测
     * Update marble position with boundary collision
     */
    public void update(double dt) {
        if (launched && screenWidth > 0 && screenHeight > 0) {
            prevCx = getCenterX();
            prevCy = getCenterY();

            double cx = prevCx + vx * dt;
            double cy = prevCy + vy * dt;
            double radius = getSide() * 0.866;

            // Left/right wall collision / 左右壁碰撞
            if (cx <= radius) {
                cx = radius;
                vx = -vx;
            } else if (cx >= screenWidth - radius) {
                cx = screenWidth - radius;
                vx = -vx;
            }

            setCenter(cx, cy);
            recalculateVerticesIfDirty();
        }
    }

    public boolean isLaunched() {
        return launched;
    }

    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public double getPrevCenterX() { return prevCx; }
    public double getPrevCenterY() { return prevCy; }

    public void setLaunchSpeed(double speed) {
        this.launchSpeed = speed;
    }

    /**
     * 根据关卡设置特殊弹珠（第2关creeper，第3关creeper+bedrock）
     * Set special marble type for level (Level 2: creeper, Level 3: creeper+bedrock)
     */
    public void setSpecialMarbleForLevel(Random random, int level) {
        if (level == 2) {
            creeperCounter++;
            if (creeperCounter >= CREEPER_INTERVAL) {
                creeperCounter = 0;
                setColorType(CREEPER);
            } else if (random.nextDouble() < 1.0 / CREEPER_INTERVAL) {
                creeperCounter = 0;
                setColorType(CREEPER);
            }
        } else if (level == 3) {
            int originalColor = getColorType();
            creeperCounter++;
            if (creeperCounter >= CREEPER_INTERVAL) {
                creeperCounter = 0;
                setColorType(CREEPER);
            } else if (random.nextDouble() < 1.0 / CREEPER_INTERVAL) {
                creeperCounter = 0;
                setColorType(CREEPER);
            }
            if (getColorType() == originalColor) {
                bedrockCounter++;
                if (bedrockCounter >= BEDROCK_INTERVAL) {
                    bedrockCounter = 0;
                    setColorType(BEDROCK);
                } else if (random.nextDouble() < 1.0 / BEDROCK_INTERVAL) {
                    bedrockCounter = 0;
                    setColorType(BEDROCK);
                }
            }
        }
        // Level 4: 不调用此逻辑以防止自然生成特殊弹珠 / Level 4: skip to prevent natural special marble spawning
    }

    public static void resetCounters() {
        creeperCounter = 0;
        bedrockCounter = 0;
    }
}