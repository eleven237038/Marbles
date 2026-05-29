import java.util.Random;

public class MarbleLaunch extends Marble {
    private double vx;
    private double vy;
    private double launchSpeed = 500;
    private boolean launched;
    private int screenWidth;
    private int screenHeight;
    private double prevCx;
    private double prevCy;
    private static int level2ShotCounter = 0;  // 第2关发射计数器
    private static final int CREEPER_INTERVAL = 6;  // 每6发必有一个creeper

    public MarbleLaunch() {
        super();
        this.vx = 0;
        this.vy = 0;
        this.launched = false;
        this.screenWidth = 0;
        this.screenHeight = 0;
    }

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
        setCenter(x, y);
        this.prevCx = x;
        this.prevCy = y;
    }

    public void update(double dt) {
        if (launched && screenWidth > 0 && screenHeight > 0) {
            prevCx = getCenterX();
            prevCy = getCenterY();

            double cx = prevCx + vx * dt;
            double cy = prevCy + vy * dt;
            double radius = getSide() * 0.866;

            // 左右壁碰撞 (包含半径，防止嵌入墙体)
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

    // 第2关：每6发必有一个creeper
    public void setCreeperForLevel2(Random random) {
        level2ShotCounter++;
        if (level2ShotCounter >= CREEPER_INTERVAL) {
            level2ShotCounter = 0;
            setColorType(CREEPER);
        } else if (random.nextDouble() < 1.0 / CREEPER_INTERVAL) {
            level2ShotCounter = 0;
            setColorType(CREEPER);
        }
    }

    public static void resetLevel2Counter() {
        level2ShotCounter = 0;
    }
}