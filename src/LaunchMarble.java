public class LaunchMarble extends Marble {
    private double vx;
    private double vy;
    private double launchSpeed = 21;
    private boolean launched;

    public LaunchMarble() {
        super();
        this.vx = 0;
        this.vy = 0;
        this.launched = false;
    }

    public void launch(double targetX, double targetY) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0) {
            vx = (dx / distance) * launchSpeed;
            vy = (dy / distance) * launchSpeed;
            launched = true;
        }
    }

    public void update(double dt) {
        if (launched) {
            setCenter(getCenterX() + vx * dt, getCenterY() + vy * dt);
        }
    }

    public boolean isLaunched() {
        return launched;
    }

    public void setLaunchSpeed(double speed) {
        this.launchSpeed = speed;
    }
}