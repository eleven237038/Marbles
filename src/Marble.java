import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

public class Marble {
    private double cx, cy;
    private double side;
    private boolean initialized;
    private boolean verticesDirty;
    public static final int RED = 1;
    public static final int BLUE = 2;
    public static final int YELLOW = 3;
    public static final int PURPLE = 4;
    public static final int CREEPER = 5;
    public static final int BEDROCK = 6;
    public static final int HEART = 7;

    // 静态sprite图像（延迟加载）
    private static BufferedImage creeperSprite = null;
    private static BufferedImage bedrockSprite = null;
    private static BufferedImage heartSprite = null;

    // 动画状态相关
    private boolean popping = false;
    private boolean falling = false; // 新增：掉落状态
    private boolean dead = false;
    private double popDelay = 0;
    private double popProgress = 0;

    // 掉落物理参数
    private double fallVy = 0;
    private double fallAy = 1500; // 重力加速度
    private static final double FALL_DEATH_Y = 1500;  // 掉落死亡阈值

    // 单独动画状态（无附着时触发：先斜向上滑再快速下落）
    private boolean alone = false;
    private double aloneSlideVx = 0; // 斜向滑动的水平速度
    private double aloneSlideVy = 0; // 斜向滑动的垂直速度
    private double aloneDelay = 0;

    // 碰撞动画状态（无消除触发时触发：沿碰撞点向外移动再回弹）
    private boolean colliding = false;
    private double collisionDelay = 0;
    private double collisionDirX = 0; // 碰撞方向（单位向量）
    private double collisionDirY = 0;
    private double collisionOffsetX = 0; // 当前偏移量
    private double collisionOffsetY = 0;
    private double collisionVelX = 0; // 偏移速度
    private double collisionVelY = 0;
    private double collisionPhase = 0; // 0=延迟中, 1=外移, 2=回弹
    private double collisionOriginalCx = 0;
    private double collisionOriginalCy = 0;

    // 主体色系
    private static final Color[] BASE_COLOR = {
            null,
            new Color(220, 30, 30),
            new Color(20, 80, 220),
            new Color(240, 200, 20),
            new Color(160, 30, 200)
    };
    // 亮部高光底色
    private static final Color[] BRIGHT_COLOR = {
            null,
            new Color(255, 130, 130),
            new Color(110, 190, 255),
            new Color(255, 250, 180),
            new Color(220, 130, 255)
    };
    // 暗部加深色
    private static final Color[] DARK_COLOR = {
            null,
            new Color(120, 10, 10),
            new Color(10, 40, 120),
            new Color(160, 120, 0),
            new Color(90, 10, 120)
    };

    private static final Random random = new Random();

    // Cached rendering constants (avoid per-frame allocation)
    private static final float[] GRADIENT_STOPS = {0f, 0.6f, 1f};
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 40);
    private static final Color BORDER_COLOR = new Color(0, 0, 0, 70);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 200);
    private static final Color REFLECTION_COLOR = new Color(255, 255, 255, 120);
    private static final Color INNER_BORDER_COLOR = new Color(255, 255, 255, 50);

    public Point2D.Double cachedCenter = new Point2D.Double();
    private int colorType;
    private int row;
    private int col;
    private boolean markedForRemove = false;
    private boolean scored = false;

    // 警戒状态：靠近deadline时闪烁
    private boolean warn = false;
    private double warnProgress = 0;
    private double warnIntensity = 0; // 0~1，越接近deadline值越大

    // 静态初始化：加载特殊弹珠sprite
    static {
        loadSpecialSprites();
    }

    private static void loadSpecialSprites() {
        try {
            String basePath = ResourceManager.getImagePath("Marbles/");
            creeperSprite = ImageIO.read(new File(basePath + "creeper.png"));
            bedrockSprite = ImageIO.read(new File(basePath + "bedrock.png"));
            heartSprite = ImageIO.read(new File(basePath + "heart.png"));
            System.out.println("成功加载特殊弹珠sprite!");
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

    // 判断是否为免疫creeper消除的类型
    public boolean isImmuneToCreeper() {
        return colorType == BEDROCK || colorType == HEART;
    }

    // 判断是否为普通弹珠（可被creeper消除）
    public boolean isNormalMarble() {
        return colorType >= RED && colorType <= PURPLE;
    }

    // 开始触发消除动画并设置延迟时间
    public void startPop(double delay) {
        this.popping = true;
        this.popDelay = delay;
        this.popProgress = 0;
    }

    // 开始触发无附着掉落动画
    public void startFalling(double delay) {
        this.falling = true;
        this.popping = false;
        this.alone = false;
        this.popDelay = delay;
        // 初始赋予一个轻微向上的速度，配合重力实现先上抛再下落的视觉效果
        this.fallVy = -150 - (random.nextDouble() * 80);
    }

    // 开始触发无附着单独动画：先沿边缘斜向上滑动，再快速下落（不触发deadline碰撞）
    public void startAlone(int triggerEdge, double delay) {
        this.alone = true;
        this.falling = false;
        this.popping = false;
        this.popDelay = delay;
        // 根据触发边计算斜向滑动方向（边缘0-5对应六边形的六个边）
        // 斜向下的角度：边缘0=右上方, 1=右下方, 2=下方, 3=左下方, 4=左上方, 5=上方
        double angle = triggerEdge * Math.PI / 3.0;
        double slideSpeed = 120 + random.nextDouble() * 60;
        this.aloneSlideVx = Math.cos(angle) * slideSpeed;
        this.aloneSlideVy = -Math.abs(Math.sin(angle) * slideSpeed); // 确保向上
    }

    // 开始触发碰撞动画：沿碰撞点连线方向向外小幅移动再回弹
    public void startCollision(double targetX, double targetY, double delay) {
        this.colliding = true;
        this.collisionDelay = delay;
        this.collisionPhase = 0;
        // 计算从碰撞点到自身的方向向量
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

    public void update(double dt) {
        // 更新碰撞动画：沿碰撞点向外移动再回弹（独立于其他状态）
        if (colliding) {
            if (collisionPhase == 0) {
                // 延迟阶段
                collisionDelay -= dt;
                if (collisionDelay <= 0) {
                    collisionPhase = 1;
                    // 给予一个向外的初速度
                    double speed = 40 + random.nextDouble() * 20;
                    collisionVelX = collisionDirX * speed;
                    collisionVelY = collisionDirY * speed;
                }
            } else if (collisionPhase == 1) {
                // 向外移动阶段（减速）
                collisionOffsetX += collisionVelX * dt;
                collisionOffsetY += collisionVelY * dt;
                collisionVelX *= (1 - dt * 8); // 阻力衰减
                collisionVelY *= (1 - dt * 8);
                // 当速度衰减到足够小时，进入回弹阶段
                double speed = Math.sqrt(collisionVelX * collisionVelX + collisionVelY * collisionVelY);
                if (speed < 20) {
                    collisionPhase = 2;
                }
                verticesDirty = true;
            } else if (collisionPhase == 2) {
                // 回弹阶段（弹簧式回到原点）
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
        // 更新弹出动画
        if (popping) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                popProgress += dt * 6.0; // 动画速度，约 0.16 秒播放完成
                if (popProgress >= 1.0) {
                    dead = true;
                }
            }
        }
        // 更新无附着单独动画：先斜向滑出再快速下落（不触发deadline）
        else if (alone) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                // 阶段1：斜向滑动（约0.3秒）
                if (aloneDelay < 0.3) {
                    cx += aloneSlideVx * dt;
                    cy += aloneSlideVy * dt;
                    aloneDelay += dt;
                    verticesDirty = true;
                }
                // 阶段2：快速下落（之后一直执行）
                else {
                    cy += (400 + random.nextDouble() * 100) * dt;
                    verticesDirty = true;
                    if (cy > FALL_DEATH_Y) {
                        dead = true;
                    }
                }
            }
        }
        // 更新掉落动画
        else if (falling) {
            if (popDelay > 0) {
                popDelay -= dt;
            } else {
                cy += fallVy * dt;
                fallVy += fallAy * dt;
                verticesDirty = true;
                // 当掉出屏幕可视范围时标记死亡
                if (cy > FALL_DEATH_Y) {
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

    public void markForRemove(boolean b) { markedForRemove = b; }
    public boolean isMarkedForRemove() { return markedForRemove; }
    public void setScored(boolean scored) { this.scored = scored; }
    public boolean isScored() { return scored; }
    public boolean isWarn() { return warn; }
    public void setWarn(boolean warn) { this.warn = warn; }

    public void updateWarn(double dt, double intensity) {
        if (warn) {
            warnProgress += dt * (6.0 + intensity * 18.0);
            warnIntensity = intensity;
        }
    }

    public void draw(Graphics2D g) {
        // 如果未初始化或动画已经播放完成完全消失，则不绘制
        if (!initialized || dead) return;

        // 判断是否为传说之下风格的弹珠
        if (Main.utMarble) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF); // 像素风不需要抗锯齿
            double scale = 1.0;
            if (popping && popDelay <= 0) {
                scale = 1.0 + popProgress * 0.4;
            }
            double radius = side * 0.866 * scale;
            double drawCx = cx + collisionOffsetX;
            double drawCy = cy + collisionOffsetY;

            if (colorType == CREEPER) {
                g.setColor(new Color(0, 180, 0));
                g.fillRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
                g.setColor(Color.BLACK);
                g.fillRect((int)drawCx - 8, (int)drawCy - 8, 4, 4);
                g.fillRect((int)drawCx + 4, (int)drawCy - 8, 4, 4);
                g.fillRect((int)drawCx - 2, (int)drawCy - 2, 4, 6);
            } else if (colorType == BEDROCK) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
            } else if (colorType == HEART) {
                g.setColor(Color.RED);
                g.fillRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
            } else {
                Color base = BASE_COLOR[colorType];
                if (base == null) base = Color.WHITE;
                g.setColor(base);
                g.fillRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2));
                g.drawRect((int)(drawCx - radius), (int)(drawCy - radius), (int)(radius*2), (int)(radius*2));
            }
            
            if (warn) {
                g.setColor(Color.RED);
                g.drawRect((int)(drawCx - radius - 2), (int)(drawCy - radius - 2), (int)(radius*2 + 4), (int)(radius*2 + 4));
            }
            
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double scale = 1.0;
        float alpha = 1.0f;

        // 当处于正在爆裂的状态，并且延迟时间已到，开始放大且透明化
        if (popping && popDelay <= 0) {
            scale = 1.0 + popProgress * 0.4; // 最大膨胀 1.4 倍
            alpha = 1.0f - (float)popProgress;
            if (alpha < 0f) alpha = 0f;
            if (alpha > 1f) alpha = 1f;
        }

        Composite originalComposite = null;
        if (alpha < 1.0f) {
            originalComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        // 精准半径：结合动画放大倍数
        double radius = side * 0.866 * scale;
        double drawCx = cx + collisionOffsetX;
        double drawCy = cy + collisionOffsetY;

        // 绘制特殊弹珠sprite
        if (colorType == CREEPER && creeperSprite != null) {
            int spriteSize = (int)(radius * 2);
            g.drawImage(creeperSprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
        } else if (colorType == BEDROCK && bedrockSprite != null) {
            int spriteSize = (int)(radius * 2);
            g.drawImage(bedrockSprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
        } else if (colorType == HEART && heartSprite != null) {
            int spriteSize = (int)(radius * 2);
            g.drawImage(heartSprite, (int)(drawCx - radius), (int)(drawCy - radius), spriteSize, spriteSize, null);
        } else {
            // 普通弹珠绘制
            double x = drawCx - radius;
            double y = drawCy - radius;
            double diameter = radius * 2;

            Color base = BASE_COLOR[colorType];
            Color bright = BRIGHT_COLOR[colorType];
            Color dark = DARK_COLOR[colorType];

            // 阴影
            g.setColor(SHADOW_COLOR);
            g.fillOval((int)Math.round(x + 2 * scale), (int)Math.round(y + 2 * scale), (int)Math.round(diameter), (int)Math.round(diameter));

            // 球体渐变
            cachedCenter.setLocation(drawCx, drawCy);
            Color[] gradColor = {bright, base, dark};
            RadialGradientPaint ballGrad = new RadialGradientPaint(cachedCenter, (float)radius, GRADIENT_STOPS, gradColor);
            g.setPaint(ballGrad);
            g.fillOval((int)Math.round(x), (int)Math.round(y), (int)Math.round(diameter), (int)Math.round(diameter));

            // 双层精致边框
            g.setStroke(new BasicStroke(1.8f * (float)scale));
            g.setColor(BORDER_COLOR);
            g.drawOval((int)Math.round(x), (int)Math.round(y), (int)Math.round(diameter), (int)Math.round(diameter));

            g.setStroke(new BasicStroke(0.8f * (float)scale));
            g.setColor(INNER_BORDER_COLOR);
            g.drawOval((int)Math.round(x + 1 * scale), (int)Math.round(y + 1 * scale), (int)Math.round(diameter - 2 * scale), (int)Math.round(diameter - 2 * scale));

            // 主高光
            g.setColor(HIGHLIGHT_COLOR);
            double highlightR = radius * 0.32;
            g.fillOval((int)Math.round(drawCx - radius * 0.48), (int)Math.round(drawCy - radius * 0.48), (int)Math.round(highlightR), (int)Math.round(highlightR));

            // 玻璃反光点
            g.setColor(REFLECTION_COLOR);
            int reflect1Size = Math.max(1, (int)(4 * scale));
            int reflect2Size = Math.max(1, (int)(3 * scale));
            g.fillOval((int)Math.round(drawCx + radius * 0.25), (int)Math.round(drawCy - radius * 0.2), reflect1Size, reflect1Size);
            g.fillOval((int)Math.round(drawCx - radius * 0.2), (int)Math.round(drawCy + radius * 0.3), reflect2Size, reflect2Size);
        }

        // 还原画布透明度
        if (alpha < 1.0f) {
            g.setComposite(originalComposite);
        }

        // 警戒闪烁：靠近deadline时，弹珠周围散发红色光芒闪烁
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

    public void reset() {
        this.cx = 0;
        this.cy = 0;
        this.side = 24.22;
        this.initialized = false;

        // 重置动画状态
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