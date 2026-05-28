import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Sans - 角色渲染与精灵图(Sprite Sheet)动画控制类
 * 支持读取单个聚合精灵图，并按名称调用对应动画
 */
public class Sans {
    // 统一指向新的精灵图路径
    private static final String SPRITE_PATH = ResourceManager.getImagePath("Sans.png");
    
    // 使用数组存储精灵图（当前仅需一个）
    private static BufferedImage[] spriteSheets = new BufferedImage[1];
    private static final Object LOAD_LOCK = new Object();

    // 内部类：用于存储单个动作的切片数据
    private static class AnimData {
        int sheetIndex;     // 所属精灵图的索引
        int startX, startY; // 该动作在精灵图中的起始坐标
        int frameW, frameH; // 单帧的宽度和高度
        int frameCount;     // 该动作包含的总帧数
        int[] frameXOffsets; // 每帧在水平方向的偏移量（用于处理不规则排列）

        public AnimData(int sheetIndex, int x, int y, int w, int h, int count) {
            this(sheetIndex, x, y, w, h, count, null);
        }

        public AnimData(int sheetIndex, int x, int y, int w, int h, int count, int[] frameXOffsets) {
            this.sheetIndex = sheetIndex;
            this.startX = x;
            this.startY = y;
            this.frameW = w;
            this.frameH = h;
            this.frameCount = count;
            this.frameXOffsets = frameXOffsets;
        }
    }

    // 动作字典：存储所有已录入的动作
    private Map<String, AnimData> actions = new HashMap<>();
    
    // 当前正在播放的动作状态
    private String currentAction = null;
    private int currentFrame = 0;

    private javax.swing.Timer animTimer;
    private boolean isAnimating = false;

    public Sans() {
        loadSpriteSheets();
        initActions();
        
        // 默认设置为向下的基础动作
        if (actions.containsKey("Basic - Down")) {
            currentAction = "Basic - Down";
        }
    }

    private void loadSpriteSheets() {
        synchronized (LOAD_LOCK) {
            try {
                if (spriteSheets[0] == null) {
                    spriteSheets[0] = ImageIO.read(new File(SPRITE_PATH));
                    System.out.println("成功加载 Sans 精灵图!");
                }
            } catch (IOException e) {
                System.err.println("无法加载 Sans 精灵图: " + SPRITE_PATH);
            }
        }
    }

    /**
     * 录入 Sans 的动作数据
     * 仅保留：向左走、向右走、站立（向下）
     */
    private void initActions() {
        // ==========================================
        // 站立 (Basic - Down): 单帧，4顶点 (29,39)(29,162)(124,162)(124,39)
        // ==========================================
        addAction(0, "Basic - Down", 29, 39, 95, 123, 1);

        // ==========================================
        // 向右走 (Basic - Right): 4帧，每帧4顶点（不规则排列）
        // 帧0: (29,165)(100,165)(100,288)(29,288)   -> offset 0
        // 帧1: (102,165)(173,165)(173,288)(102,288)  -> offset 73
        // 帧2: (175,165)(175,288)(246,288)(246,165)   -> offset 146
        // 帧3: (248,165)(248,288)(319,165)(319,288)   -> offset 219
        // ==========================================
        addAction(0, "Basic - Right", 29, 165, 71, 123, 4, new int[]{0, 73, 146, 219});

        // ==========================================
        // 向左走 (Basic - Left): 4帧，每帧4顶点（不规则排列）
        // 帧0: (33,294)(104,294)(104,417)(33,417)    -> offset 0
        // 帧1: (106,294)(177,294)(177,417)(106,417)   -> offset 73
        // 帧2: (179,294)(250,294)(250,417)(179,417)   -> offset 146
        // 帧3: (252,294)(323,294)(323,417)(252,417)   -> offset 219
        // ==========================================
        addAction(0, "Basic - Left", 33, 294, 71, 123, 4, new int[]{0, 73, 146, 219});
    }

    /**
     * 注册一个新的单行动作序列
     */
    public void addAction(int sheetIndex, String name, int x, int y, int w, int h, int frames) {
        actions.put(name, new AnimData(sheetIndex, x, y, w, h, frames));
    }

    /**
     * 注册一个新的可能跨行的动作序列
     */
    public void addAction(int sheetIndex, String name, int x, int y, int w, int h, int frames, int[] frameXOffsets) {
        actions.put(name, new AnimData(sheetIndex, x, y, w, h, frames, frameXOffsets));
    }

    /**
     * 播放指定的动作
     */
    public void play(String actionName, int delayMs) {
        if (!actions.containsKey(actionName)) {
            System.err.println("警告: 未找到动作 '" + actionName + "'");
            return;
        }

        currentAction = actionName;
        currentFrame = 0; // 切换动作时重置帧
        
        stopAnimation();
        isAnimating = true;
        
        animTimer = new javax.swing.Timer(delayMs, e -> {
            AnimData data = actions.get(currentAction);
            if (data != null && data.frameCount > 0) {
                currentFrame = (currentFrame + 1) % data.frameCount;
            }
        });
        animTimer.start();
    }

    /**
     * 停止当前动画播放
     */
    public void stopAnimation() {
        if (animTimer != null) {
            animTimer.stop();
            animTimer = null;
        }
        isAnimating = false;
    }

    /**
     * 在指定坐标绘制当前动画的当前帧 (默认不缩放)
     */
    public void draw(Graphics2D g2d, int x, int y) {
        draw(g2d, x, y, 1.0);
    }

    /**
     * 缩放绘制动画帧
     */
    public void draw(Graphics2D g2d, int x, int y, double scale) {
        if (currentAction == null) return;

        AnimData data = actions.get(currentAction);
        if (data == null) return;

        BufferedImage sheet = spriteSheets[data.sheetIndex];
        if (sheet == null) return;

        // 计算当前帧的索引
        int frameIndex = currentFrame % data.frameCount;

        // 计算当前帧在精灵图上的具体X/Y坐标
        int frameX = getFrameX(data, frameIndex);
        int frameY = data.startY;

        // 防止切片越界
        if (frameX + data.frameW > sheet.getWidth() || frameY + data.frameH > sheet.getHeight()) {
            return;
        }

        // 提取子图像并绘制
        BufferedImage frameImg = sheet.getSubimage(frameX, frameY, data.frameW, data.frameH);
        int drawW = (int)(data.frameW * scale);
        int drawH = (int)(data.frameH * scale);
        g2d.drawImage(frameImg, x, y, drawW, drawH, null);
    }

    /**
     * 根据帧索引计算该帧在精灵图上的X坐标
     */
    private int getFrameX(AnimData data, int frameIndex) {
        if (data.frameXOffsets != null && frameIndex < data.frameXOffsets.length) {
            return data.startX + data.frameXOffsets[frameIndex];
        }
        // 默认：所有帧水平排列，每帧宽度为 frameW
        return data.startX + (frameIndex * data.frameW);
    }

    /**
     * 手动绘制特定动作的指定帧（不依赖 Timer, 保持静止）
     */
    public void drawSpecificFrame(Graphics2D g2d, String actionName, int frameIndex, int x, int y, double scale) {
        if (!actions.containsKey(actionName)) return;

        AnimData data = actions.get(actionName);
        BufferedImage sheet = spriteSheets[data.sheetIndex];
        if (sheet == null) return;

        int safeFrame = frameIndex % data.frameCount;
        int frameX = getFrameX(data, safeFrame);
        int frameY = data.startY;

        if (frameX + data.frameW <= sheet.getWidth() && frameY + data.frameH <= sheet.getHeight()) {
            BufferedImage frameImg = sheet.getSubimage(frameX, frameY, data.frameW, data.frameH);
            g2d.drawImage(frameImg, x, y, (int)(data.frameW * scale), (int)(data.frameH * scale), null);
        }
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public void dispose() {
        stopAnimation();
    }
}