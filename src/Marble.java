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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Marble - 弹珠类，管理弹珠状态、渲染和动画
 * Marble - Marble entity class managing state, rendering and animation
 */
public class Marble {
    // 弹珠颜色类型常量 / Marble color type constants
    public static final int RED = 1;
    public static final int BLUE = 2;
    public static final int YELLOW = 3;
    public static final int PURPLE = 4;
    public static final int CREEPER = 5;
    public static final int BEDROCK = 6;
    public static final int HEART = 7;

    // 静态sprite图像（延迟加载）/ Static sprite images (lazy loaded)
    private static BufferedImage creeperSprite = null;
    private static BufferedImage bedrockSprite = null;
    private static BufferedImage heartSprite = null;

    // 普通弹珠sprite图像（UT风格）/ Normal marble sprite images (UT style)
    private static BufferedImage redSprite = null;
    private static BufferedImage blueSprite = null;
    private static BufferedImage yellowSprite = null;
    private static BufferedImage purpleSprite = null;

    // UT风格标记：启用时使用sprite图像渲染普通弹珠
    // UT style flag: use sprite images for normal marbles when enabled
    public static boolean utStyle = false;

    // 动画状态相关 / Animation state related
    private boolean popping = false;
    private boolean falling = false; // 掉落状态 / Falling state
    private boolean dead = false;
    private double popDelay = 0;
    private double popProgress = 0;

    // 掉落物理参数 / Falling physics parameters
    private double fallVy = 0;
    private double fallAy = 1500; // 重力加速度 / Gravity acceleration
    private static final double FALL_DEATH_Y = 1500;  // 掉落死亡阈值 / Fall death threshold

    // 单独动画状态 / Alone animation state
    private boolean alone = false;
    private double aloneSlideVx = 0;
    private double aloneSlideVy = 0;
    private double aloneDelay = 0;

    // 碰撞动画状态 / Collision animation state
    private boolean colliding = false;
    private double collisionDelay = 0;
    private double collisionDirX = 0;
    private double collisionDirY = 0;
    private double collisionOffsetX = 0;
    private double collisionOffsetY = 0;
    private double collisionVelX = 0;
    private double collisionVelY = 0;
    private double collisionPhase = 0;
    private double collisionOriginalCx = 0;
    private double collisionOriginalCy = 0;

    // 主体色系 / Main color palette
    private static final Color[] BASE_COLOR = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200)
    };
    // 亮部高光底色 / Bright highlight base color
    private static final Color[] BRIGHT_COLOR = {
            null,
            new Color(255, 130, 130),
            new Color(110, 190, 255),
            new Color(255, 250, 180),
            new Color(220, 130, 255)
    };
    // 暗部加深色 / Dark shade color
    private static final Color[] DARK_COLOR = {
            null,
            new Color(120, 10, 10),
            new Color(10, 40, 120),
            new Color(160, 120, 0),
            new Color(90, 10, 120)
    };

    private static final Random random = new Random();

    // 缓存的渲染常量 / Cached rendering constants
    private static final float[] GRADIENT_STOPS = {0f, 0.6f, 1f};
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 40);
    private static final Color BORDER_COLOR = new Color(0, 0, 0, 70);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 200);
    private static final Color REFLECTION_COLOR = new Color(255, 255, 255, 120);
    private static final Color INNER_BORDER_COLOR = new Color(255, 255, 255, 50);

    public Point2D.Double cachedCenter = new Point2D.Double();
    private double cx, cy;
    private double side;
    private boolean initialized;
    private boolean verticesDirty;
    private int colorType;
    private int row;
    private int col;
    private boolean markedForRemove = false;
    private boolean scored = false;

    // 警戒状态 / Warning state
    private boolean warn = false;
    private double warnProgress = 0;
    private double warnIntensity = 0;

    // 静态初始化：加载特殊弹珠sprite / Static initialization: load special marble sprites
    static {
        loadSpecialSprites();
    }

    /**
     * 加载特殊弹珠sprite图像
     * Load special marble sprite images
     */
    private static void loadSpecialSprites() {
        try {
            String basePath = ResourceManager.getImagePath("Marbles/");
            creeperSprite = ImageIO.read(new File(basePath + "creeper.png"));
            bedrockSprite = ImageIO.read(new File(basePath + "bedrock.png"));
            heartSprite = ImageIO.read(new File(basePath + "heart.png"));
            redSprite = ImageIO.read(new File(basePath + "red.png"));
            blueSprite = ImageIO.read(new File(basePath + "blue.png"));
            yellowSprite = ImageIO.read(new File(basePath + "yellow.png"));
            purpleSprite = ImageIO.read(new File(basePath + "purple.png"));
            System.out.println("成功加载特殊弹珠sprite!/Special marble sprites loaded successfully!");
        } catch (IOException e) {
            System.err.println("加载特殊弹珠sprite失败: " + e.getMessage());
        }
    }

    public Marble() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;
        this.colorType = random.nextInt(4) + 1;
        this.row = 0;
        this.col = 0;
        this.verticesDirty = false;
    }

    public void init(double cx, double cy, int row, int col) {
        this.cx = cx;
        this.cy = cy;
        this.row = row;
        this.col = col;
        this.initialized = true;
    }

    /**
     * 检查弹珠是否对creeper爆炸免疫
     * Check if marble is immune to creeper explosion
     */
    public boolean isImmuneToCreeper() {
        return colorType == BEDROCK || colorType == HEART;
    }

    /**
     * 检查弹珠是否为普通弹珠
     * Check if marble is a normal marble
     */
    public boolean isNormalMarble() {
        return colorType >= RED && colorType <= PURPLE;
    }

    /**
     * 开始消除动画
     * Start pop animation
     */
    public void startPop(double delay) {
        this.popping = true;
        this.popDelay = delay;
        this.popProgress = 0;
    }

    /**
     * 开始掉落动画
     * Start falling animation
     */
    public void startFalling(double delay) {
        this.falling = true;
        this.popping = false;
        this.alone = false;
        this.popDelay = delay;
        this.fallVy = -150 - (random.nextDouble() * 80);
    }

    /**
     * 开始单独滑行动画
     * Start alone sliding animation
     */
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

    /**
     * 开始碰撞动画
     * Start collision animation
     */
    public void startCollision(double targetX, double targetY, double delay) {
        this.colliding = true;
        this.collisionDelay = delay;
        this.collisionPhase = 0;
        double dx = this.cx - targetX;
        double dy = this.cy - targetY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            this.collisionDirX = dx / dist;
            this.collisionDirY = dy / dist;
        } else {
            this.collisionDirX = 0;
            this.collisionDirY = -1;
        }
        this.collisionOffsetX = 0;
        this.collisionOffsetY = 0;
        this.collisionVelX = 0;
        this.collisionVelY = 0;
        this.collisionOriginalCx = this.cx;
        this.collisionOriginalCy = this.cy;
    }

    public boolean isAlone() { return alone; }
    public boolean isColliding() { return colliding; }
    public boolean isPopping() { return popping; }
    public boolean isFalling() { return falling; }
    public boolean isDead() { return dead; }

    /**
     * 更新弹珠动画状态
     * Update marble animation state
     */
    public void update(double dt) {
        if (colliding) {
            if (collisionPhase == 0) {
                collisionDelay -= dt;
                if (collisionDelay <= 0) {
                    collisionPhase = 1;
                    double speed = 40 + random.nextDouble() * 20;
                    collisionVelX = collisionDirX * speed;
                    collisionVelY = collisionDirY * speed;
                }
            } else if (collisionPhase == 1) {
                collisionOffsetX += collisionVelX * dt;
                collisionOffsetY += collisionVelY * dt;
                collisionVelX *= (1 - dt * 8);
                collisionVelY *= (1 - dt * 8);
                double speed = Math.sqrt(collisionVelX * collisionVelX + collisionVelY * collisionVelY);
                if (speed < 20) {
                    collisionPhase = 2;
                }
                verticesDirty = true;
            } else if (collisionPhase == 2) {
                collisionOffsetX *= (1 - dt * 15);
                collisionOffsetY *= (1 - dt * 15);
                if (Math.abs(collisionOffsetX) < 0.5 && Math.abs(collisionOffsetY) < 0.5) {
                    collisionOffsetX = 0;
                    collisionOffsetY = 0;
                    colliding = false;
                }
                verticesDirty = true;
            }
        }

        if (popping) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                popProgress += dt * 6.0;
                if (popProgress >= 1.0) dead = true;
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
                    if (cy > FALL_DEATH_Y) dead = true;
                }
            }
        } else if (falling) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                cy += fallVy * dt;
                fallVy += fallAy * dt;
                verticesDirty = true;
                if (cy > FALL_DEATH_Y) dead = true;
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
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }

    public void markForRemove(boolean b) { markedForRemove = b; }
    public boolean isMarkedForRemove() { return markedForRemove; }
    public void setScored(boolean scored) { this.scored = scored; }
    public boolean isScored() { return scored; }
    public boolean isWarn() { return warn; }
    public void setWarn(boolean warn) { this.warn = warn; }

    /**
     * 更新警戒状态
     * Update warning state
     */
    public void updateWarn(double dt, double intensity) {
        if (warn) {
            warnProgress += dt * (6.0 + intensity * 18.0);
            warnIntensity = intensity;
        }
    }

    /**
     * 绘制弹珠
     * Draw marble
     */
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
        double drawCx = cx + collisionOffsetX;
        double drawCy = cy + collisionOffsetY;

        if (colorType == CREEPER && creeperSprite != null) {
            int spriteSize = (int)(radius * 2);
            g.drawImage(creeperSprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
        } else if (colorType == BEDROCK && bedrockSprite != null) {
            int spriteSize = (int)(radius * 2);
            g.drawImage(bedrockSprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
        } else if (colorType == HEART && heartSprite != null) {
            int spriteSize = (int)(radius * 2);
            g.drawImage(heartSprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
        } else if (utStyle) {
            BufferedImage sprite = null;
            if (colorType == RED) sprite = redSprite;
            else if (colorType == BLUE) sprite = blueSprite;
            else if (colorType == YELLOW) sprite = yellowSprite;
            else if (colorType == PURPLE) sprite = purpleSprite;

            if (sprite != null) {
                int spriteSize = (int)(radius * 2);
                g.drawImage(sprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
            } else {
                drawNormalMarble(g, drawCx, drawCy, radius, scale);
            }
        } else {
            drawNormalMarble(g, drawCx, drawCy, radius, scale);
        }

        if (alpha < 1.0f) {
            g.setComposite(originalComposite);
        }

        if (warn) {
            double t = (Math.sin(warnProgress) + 1.0) * 0.5;
            int alphaVal = (int)(60 * t);
            int borderAlpha = (int)(120 * t);
            double radius2 = side * 0.866 * 1.2;
            g.setColor(new Color(255, 30, 30, alphaVal));
            g.fillOval((int)Math.round(drawCx - radius2), (int)Math.round(drawCy - radius2), (int)Math.round(radius2 * 2), (int)Math.round(radius2 * 2));
            if (t > 0.3) {
                g.setColor(new Color(255, 80, 80, borderAlpha));
                g.setStroke(new BasicStroke(2.5f));
                g.drawOval((int)Math.round(drawCx - radius2 * 0.85), (int)Math.round(drawCy - radius2 * 0.85), (int)Math.round(radius2 * 1.7), (int)Math.round(radius2 * 1.7));
            }
        }
    }

    /**
     * 绘制普通弹珠（渐变效果）
     * Draw normal marble with gradient effect
     */
    private void drawNormalMarble(Graphics2D g, double drawCx, double drawCy, double radius, double scale) {
        double x = drawCx - radius;
        double y = drawCy - radius;
        double diameter = radius * 2;

        Color base = BASE_COLOR[colorType];
        Color bright = BRIGHT_COLOR[colorType];
        Color dark = DARK_COLOR[colorType];

        g.setColor(SHADOW_COLOR);
        g.fillOval((int)Math.round(x + 2 * scale), (int)Math.round(y + 2 * scale), (int)Math.round(diameter), (int)Math.round(diameter));

        cachedCenter.setLocation(drawCx, drawCy);
        Color[] gradColor = {bright, base, dark};
        RadialGradientPaint ballGrad = new RadialGradientPaint(cachedCenter, (float)radius, GRADIENT_STOPS, gradColor);
        g.setPaint(ballGrad);
        g.fillOval((int)Math.round(x), (int)Math.round(y), (int)Math.round(diameter), (int)Math.round(diameter));

        g.setStroke(new BasicStroke(1.8f * (float)scale));
        g.setColor(BORDER_COLOR);
        g.drawOval((int)Math.round(x), (int)Math.round(y), (int)Math.round(diameter), (int)Math.round(diameter));

        g.setStroke(new BasicStroke(0.8f * (float)scale));
        g.setColor(INNER_BORDER_COLOR);
        g.drawOval((int)Math.round(x + 1 * scale), (int)Math.round(y + 1 * scale), (int)Math.round(diameter - 2 * scale), (int)Math.round(diameter - 2 * scale));

        g.setColor(HIGHLIGHT_COLOR);
        double highlightR = radius * 0.32;
        g.fillOval((int)Math.round(drawCx - radius * 0.48), (int)Math.round(drawCy - radius * 0.48), (int)Math.round(highlightR), (int)Math.round(highlightR));

        g.setColor(REFLECTION_COLOR);
        int reflect1Size = Math.max(1, (int)(4 * scale));
        int reflect2Size = Math.max(1, (int)(3 * scale));
        g.fillOval((int)Math.round(drawCx + radius * 0.25), (int)Math.round(drawCy - radius * 0.2), reflect1Size, reflect1Size);
        g.fillOval((int)Math.round(drawCx - radius * 0.2), (int)Math.round(drawCy + radius * 0.3), reflect2Size, reflect2Size);
    }

    /**
     * 重置弹珠状态
     * Reset marble state
     */
    public void reset() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;
        this.popping = false;
        this.falling = false;
        this.alone = false;
        this.colliding = false;
        this.dead = false;
        this.popDelay = 0;
        this.popProgress = 0;
        this.fallVy = 0;
        this.aloneDelay = 0;
        this.collisionDelay = 0;
        this.collisionOffsetX = 0;
        this.collisionOffsetY = 0;
        this.collisionVelX = 0;
        this.collisionVelY = 0;
        this.collisionPhase = 0;
        this.scored = false;
        this.warn = false;
        this.warnProgress = 0;
        this.warnIntensity = 0;
    }
}