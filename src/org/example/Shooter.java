package org.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// 发射器 - 固定底部中心，炮台视觉效果
public class Shooter {
    private double x;
    private double y;
    private double angle;
    private Marble currentMarble;
    private Marble nextMarble;
    private Marble afterNextMarble;
    private Marble flyingMarble;
    private boolean aiming;

    public Shooter() {
        this.x = GameConfig.SHOOTER_X;
        this.y = GameConfig.SHOOTER_Y;
        this.angle = -Math.PI / 2;
        this.aiming = false;

        MarbleColor randomColor = MarbleColor.values()[(int) (Math.random() * 4)];
        currentMarble = new Marble(-1, -1, randomColor);
        currentMarble.setPosition(x, y);

        randomColor = MarbleColor.values()[(int) (Math.random() * 4)];
        nextMarble = new Marble(-1, -1, randomColor);
        nextMarble.setPosition(x + 45, y);

        randomColor = MarbleColor.values()[(int) (Math.random() * 4)];
        afterNextMarble = new Marble(-1, -1, randomColor);
        afterNextMarble.setPosition(x + 90, y);
    }

    public void prepareNextMarble() {
        currentMarble = nextMarble;
        currentMarble.setPosition(x, y);

        nextMarble = afterNextMarble;
        nextMarble.setPosition(x + 45, y);

        MarbleColor randomColor = MarbleColor.values()[(int) (Math.random() * 4)];
        afterNextMarble = new Marble(-1, -1, randomColor);
        afterNextMarble.setPosition(x + 90, y);
    }

    public void aim(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        angle = Math.atan2(dy, dx);
        if (angle > 0) angle = 0;
        if (angle < -Math.PI * 0.95) angle = -Math.PI * 0.95;
        aiming = true;
    }

    public Marble shoot() {
        if (currentMarble == null || flyingMarble != null) return null;

        Marble marble = currentMarble;
        marble.setPosition(x, y);
        double vx = Math.cos(angle) * GameConfig.BALL_SPEED;
        double vy = Math.sin(angle) * GameConfig.BALL_SPEED;
        marble.setVelocity(vx, vy);

        flyingMarble = marble;
        prepareNextMarble();

        return marble;
    }

    public void clearFlyingMarble() {
        flyingMarble = null;
    }

    public void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(60, 60, 80));
        g.fillOval((int) (x - 35), (int) (y - 15), 70, 40);

        g.setColor(new Color(80, 80, 100));
        g.fillOval((int) (x - 25), (int) (y - 10), 50, 30);

        g.setColor(new Color(100, 100, 120));
        g.setStroke(new BasicStroke(8));
        g.drawLine((int) x, (int) y,
                   (int) (x + Math.cos(angle) * 35),
                   (int) (y + Math.sin(angle) * 35));

        g.setColor(new Color(120, 120, 140));
        int barrelEndX = (int) (x + Math.cos(angle) * 35);
        int barrelEndY = (int) (y + Math.sin(angle) * 35);
        g.fillOval(barrelEndX - 6, barrelEndY - 6, 12, 12);

        g.setStroke(new BasicStroke(1));

        if (currentMarble != null && flyingMarble == null) {
            currentMarble.setPosition(x, y);
            currentMarble.render(g);
        }

        if (nextMarble != null) {
            int r = (int) GameConfig.MARBLE_RADIUS;
            g.setColor(new Color(40, 40, 60));
            g.fillRoundRect((int) (x + 30 - r), (int) (y - r) - 2, r * 2 + 4, r * 2 + 4, 6, 6);
            nextMarble.render(g);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 8));
            g.drawString("1", (int) (x + 45 - 3), (int) (y + r + 12));
        }

        if (afterNextMarble != null) {
            int r = (int) GameConfig.MARBLE_RADIUS;
            g.setColor(new Color(40, 40, 60));
            g.fillRoundRect((int) (x + 75 - r), (int) (y - r) - 2, r * 2 + 4, r * 2 + 4, 6, 6);
            afterNextMarble.render(g);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 8));
            g.drawString("2", (int) (x + 90 - 3), (int) (y + r + 12));
        }

        if (flyingMarble != null) {
            flyingMarble.render(g);
        }

        if (aiming && flyingMarble == null) {
            g.setColor(new Color(255, 255, 255, 60));
            g.drawLine((int) x, (int) y,
                       (int) (x + Math.cos(angle) * 200),
                       (int) (y + Math.sin(angle) * 200));
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public Marble getCurrentMarble() { return currentMarble; }
    public Marble getNextMarble() { return nextMarble; }
    public Marble getAfterNextMarble() { return afterNextMarble; }
    public Marble getFlyingMarble() { return flyingMarble; }
    public boolean hasFlyingMarble() { return flyingMarble != null; }
}