package org.example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 发射器类 - 固定在底部中心，带炮台视觉效果
 */
public class Shooter {
    private double x;
    private double y;
    private double angle;  // 瞄准角度（弧度）
    private Marble currentMarble;
    private Marble nextMarble;
    private Marble afterNextMarble;  // 第二个预览弹珠
    private Marble flyingMarble;
    private boolean aiming;

    public Shooter() {
        this.x = GameConfig.SHOOTER_X;
        this.y = GameConfig.SHOOTER_Y;
        this.angle = -Math.PI / 2;  // 默认向上（-90度）
        this.aiming = false;
        // 初始化三个弹珠
        MarbleColor randomColor = MarbleColor.values()[(int)(Math.random() * 4)];
        currentMarble = new Marble(-1, -1, randomColor);
        currentMarble.setPosition(x, y);

        randomColor = MarbleColor.values()[(int)(Math.random() * 4)];
        nextMarble = new Marble(-1, -1, randomColor);
        nextMarble.setPosition(x + 45, y);

        randomColor = MarbleColor.values()[(int)(Math.random() * 4)];
        afterNextMarble = new Marble(-1, -1, randomColor);
        afterNextMarble.setPosition(x + 90, y);
    }

    /**
     * 准备下一个弹珠 - 滚动更新弹珠队列
     */
    public void prepareNextMarble() {
        // 滚动弹珠：current -> next -> afterNext -> new random
        currentMarble = nextMarble;
        currentMarble.setPosition(x, y);

        nextMarble = afterNextMarble;
        nextMarble.setPosition(x + 45, y);

        // 生成新的afterNextMarble
        MarbleColor randomColor = MarbleColor.values()[(int)(Math.random() * 4)];
        afterNextMarble = new Marble(-1, -1, randomColor);
        afterNextMarble.setPosition(x + 90, y);
    }

    /**
     * 根据目标位置调整瞄准角度
     */
    public void aim(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        angle = Math.atan2(dy, dx);

        // 限制角度范围：只允许向上发射（-180度到0度）
        if (angle > 0) angle = 0;           // 不允许向下
        if (angle < -Math.PI * 0.95) angle = (float)(-Math.PI * 0.95);  // 限制向后角度

        aiming = true;
    }

    /**
     * 发射弹珠
     */
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

        // 绘制炮台底座（圆形平台）
        g.setColor(new Color(60, 60, 80));
        g.fillOval((int)(x - 35), (int)(y - 15), 70, 40);

        // 炮台内圈
        g.setColor(new Color(80, 80, 100));
        g.fillOval((int)(x - 25), (int)(y - 10), 50, 30);

        // 绘制炮管
        g.setColor(new Color(100, 100, 120));
        g.setStroke(new BasicStroke(8));
        g.drawLine((int)x, (int)y,
                   (int)(x + Math.cos(angle) * 35),
                   (int)(y + Math.sin(angle) * 35));

        // 炮管头部
        g.setColor(new Color(120, 120, 140));
        int barrelEndX = (int)(x + Math.cos(angle) * 35);
        int barrelEndY = (int)(y + Math.sin(angle) * 35);
        g.fillOval(barrelEndX - 6, barrelEndY - 6, 12, 12);

        g.setStroke(new BasicStroke(1));

        // 当前弹珠（装填中）
        if (currentMarble != null && flyingMarble == null) {
            currentMarble.setPosition(x, y);
            currentMarble.render(g);
        }

        // 下一个弹珠预览 - 显示实际颜色
        if (nextMarble != null) {
            int r = (int)GameConfig.MARBLE_RADIUS;
            // 背景框
            g.setColor(new Color(40, 40, 60));
            g.fillRoundRect((int)(x + 30 - r), (int)(y - r) - 2, r * 2 + 4, r * 2 + 4, 6, 6);
            // 弹珠
            nextMarble.render(g);
            // 标签
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 8));
            g.drawString("1", (int)(x + 45 - 3), (int)(y + r + 12));
        }

        // 第二个预览弹珠 - 显示实际颜色
        if (afterNextMarble != null) {
            int r = (int)GameConfig.MARBLE_RADIUS;
            // 背景框
            g.setColor(new Color(40, 40, 60));
            g.fillRoundRect((int)(x + 75 - r), (int)(y - r) - 2, r * 2 + 4, r * 2 + 4, 6, 6);
            // 弹珠
            afterNextMarble.render(g);
            // 标签
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 8));
            g.drawString("2", (int)(x + 90 - 3), (int)(y + r + 12));
        }

        // 飞行弹珠
        if (flyingMarble != null) {
            flyingMarble.render(g);
        }

        // 绘制瞄准线
        if (aiming && flyingMarble == null) {
            g.setColor(new Color(255, 255, 255, 60));
            g.drawLine((int)x, (int)y,
                       (int)(x + Math.cos(angle) * 200),
                       (int)(y + Math.sin(angle) * 200));
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