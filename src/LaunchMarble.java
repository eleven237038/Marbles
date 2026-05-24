public class LaunchMarble extends Marble {
    private double vx;
    private double vy;
    private double launchSpeed = 500;
    private boolean launched;
    private int screenWidth;
    private int screenHeight;

    public LaunchMarble() {
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

    public void launch(double targetX, double targetY) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 1) {
            vx = (dx / distance) * launchSpeed;
            vy = (dy / distance) * launchSpeed;
            launched = true;
        }
    }

    public void reset(double x, double y) {
        this.vx = 0;
        this.vy = 0;
        this.launched = false;
        setCenter(x, y);
    }

    public void update(double dt) {
        if (launched && screenWidth > 0 && screenHeight > 0) {
            double cx = getCenterX() + vx * dt;
            double cy = getCenterY() + vy * dt;

            if (cx <= 0) {
                cx = 0;
                vx = -vx;
            } else if (cx >= screenWidth) {
                cx = screenWidth;
                vx = -vx;
            }

            if (cy <= 0) {
                cy = 0;
                vy = -vy;
            } else if (cy >= screenHeight) {
                cy = screenHeight;
                vy = -vy;
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

    public void setLaunchSpeed(double speed) {
        this.launchSpeed = speed;
    }
}