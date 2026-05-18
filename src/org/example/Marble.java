package org.example;

import java.awt.Color;
import java.awt.Graphics2D;

// 弹珠类
public class Marble {
    private int row;
    private int col;
    private MarbleColor color;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private boolean falling;
    private boolean fromShooter;

    private boolean sliding;
    private double slideStartX;
    private double slideStartY;
    private double slideTargetX;
    private double slideTargetY;
    private long slideStartTime;
    private static final long SLIDE_DURATION = 400;

    public Marble(int row, int col, MarbleColor color) {
        this.row = row;
        this.col = col;
        this.color = color;
        this.x = 0;
        this.y = 0;
        this.vx = 0;
        this.vy = 0;
        this.falling = false;
        this.fromShooter = false;
        this.sliding = false;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
    }

    public void startFalling() {
        this.falling = true;
        this.vy = 0;
    }

    public void markFromShooter() {
        this.fromShooter = true;
    }

    public boolean isFromShooter() {
        return fromShooter;
    }

    public void update() {
        if (falling) {
            vy += 0.5;
            y += vy;
        }
        if (sliding) {
            long elapsed = System.currentTimeMillis() - slideStartTime;
            if (elapsed >= SLIDE_DURATION) {
                x = slideTargetX;
                y = slideTargetY;
                sliding = false;
            } else {
                double t = (double) elapsed / SLIDE_DURATION;
                double ease = 1 - (1 - t) * (1 - t);
                x = slideStartX + (slideTargetX - slideStartX) * ease;
                y = slideStartY + (slideTargetY - slideStartY) * ease;
            }
        }
    }

    public void startSliding(double targetX, double targetY, long startTime) {
        this.sliding = true;
        this.slideStartX = x;
        this.slideStartY = y;
        this.slideTargetX = targetX;
        this.slideTargetY = targetY;
        this.slideStartTime = startTime;
    }

    public boolean isSliding() {
        return sliding;
    }

    public void render(Graphics2D g, double scrollOffsetY) {
        int radius = (int) GameConfig.MARBLE_RADIUS;
        double drawY = y + scrollOffsetY;
        g.setColor(color.getColor());
        g.fillOval((int) (x - radius), (int) (drawY - radius), radius * 2, radius * 2);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval((int) (x - radius + 2), (int) (drawY - radius + 2), radius, radius);
    }

    public void render(Graphics2D g) {
        int radius = (int) GameConfig.MARBLE_RADIUS;
        g.setColor(color.getColor());
        g.fillOval((int) (x - radius), (int) (y - radius), radius * 2, radius * 2);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval((int) (x - radius + 2), (int) (y - radius + 2), radius, radius);
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
    public MarbleColor getColor() { return color; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public boolean isFalling() { return falling; }
}