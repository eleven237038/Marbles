import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Sans - 角色渲染与精灵图(Sprite Sheet)动画控制类
 * 支持读取多个精灵图，并按名称调用对应动画
 */
public class Sans {
    // 定义两个精灵图的本地路径
    private static final String SPRITE_PATH_1 = getSpritePath1();
    private static final String SPRITE_PATH_2 = getSpritePath2();

    private static String getSpritePath1() {
        return ResourceUtil.getImagePath("Sans-1.png");
    }

    private static String getSpritePath2() {
        return ResourceUtil.getImagePath("Sans-2.png");
    }
    
    // 使用数组存储多个精灵图
    private static BufferedImage[] spriteSheets = new BufferedImage[2];
    private static final Object LOAD_LOCK = new Object();

    // 内部类：用于存储单个动作的切片数据
    private static class AnimData {
        int sheetIndex;     // 所属精灵图的索引 (0 为 Sans-1.png, 1 为 Sans-2.png)
        int startX, startY; // 该动作在精灵图中的起始坐标
        int frameW, frameH; // 单帧的宽度和高度
        int frameCount;     // 该动作包含的总帧数
        int framesPerRow;   // 每行包含的帧数（用于处理跨行的精灵图序列）

        public AnimData(int sheetIndex, int x, int y, int w, int h, int count) {
            this(sheetIndex, x, y, w, h, count, count); // 默认所有帧都在同一行
        }

        public AnimData(int sheetIndex, int x, int y, int w, int h, int count, int framesPerRow) {
            this.sheetIndex = sheetIndex;
            this.startX = x;
            this.startY = y;
            this.frameW = w;
            this.frameH = h;
            this.frameCount = count;
            this.framesPerRow = framesPerRow;
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
                    spriteSheets[0] = ImageIO.read(new File(SPRITE_PATH_1));
                    System.out.println("成功加载 Sans 精灵图 1!");
                }
            } catch (IOException e) {
                System.err.println("无法加载 Sans 精灵图 1: " + SPRITE_PATH_1);
            }

            try {
                if (spriteSheets[1] == null) {
                    spriteSheets[1] = ImageIO.read(new File(SPRITE_PATH_2));
                    System.out.println("成功加载 Sans 精灵图 2!");
                }
            } catch (IOException e) {
                System.err.println("无法加载 Sans 精灵图 2: " + SPRITE_PATH_2);
            }
        }
    }

    /**
     * 录入 Sans 的所有动作数据
     * 注意：这里的 x, y, w, h 是根据精灵图排布给出的估算值。
     * 实际渲染时若有轻微偏移，可调整这些参数。
     */
    private void initActions() {
        // ==========================================
        // 精灵图 1 (Sans-1.png) - 索引为 0
        // ==========================================
        int defaultW = 28; // 常规站立帧的大致宽度
        int defaultH = 32; // 常规站立帧的大致高度

        // 基础行走动作
        addAction(0, "Basic - Down", 0, 20, defaultW, defaultH, 4);
        addAction(0, "Unused - Dangerous", 150, 20, defaultW, defaultH, 1);
        
        addAction(0, "Basic - Left", 0, 60, defaultW, defaultH, 4);
        addAction(0, "Shadow - Left", 150, 60, defaultW, defaultH, 4);
        
        addAction(0, "Basic - Right", 0, 100, defaultW, defaultH, 4);
        addAction(0, "Shadow - Right", 150, 100, defaultW, defaultH, 4);
        
        addAction(0, "Basic - Up", 0, 140, defaultW, defaultH, 4);
        
        // 特殊交互动作
        addAction(0, "Handshake", 0, 180, 42, defaultH, 2); // 握手动作包含两个角色，较宽
        addAction(0, "Shrug", 0, 220, 32, defaultH, 2);     // 耸肩
        addAction(0, "Trombone", 0, 260, 45, defaultH, 2);  // 吹长号较宽
        
        // 吧台凳子相关动作
        addAction(0, "Stool", 0, 300, defaultW, defaultH, 2);
        addAction(0, "Stool - Comb", 0, 340, defaultW, defaultH, 2);
        addAction(0, "Stool - Chup", 0, 380, defaultW, defaultH, 11); // 长序列动作
        addAction(0, "Stool - Buttscratch", 0, 420, defaultW, defaultH, 2);
        
        // 睡觉动作
        addAction(0, "Sleep", 0, 460, defaultW, defaultH, 2);
        
        // 骑三轮车 (Trike)
        addAction(0, "Trike", 0, 500, 38, 38, 2);
        addAction(0, "Trike - Wink", 150, 500, 38, 38, 1);
        
        // 暗背景相关
        addAction(0, "Dark BG", 0, 550, defaultW, defaultH, 2);
        addAction(0, "Out To Lunch Sign", 0, 590, 45, 45, 2);
        addAction(0, "Dark BG - Sleep Sideways (UNUSED)", 0, 640, 45, defaultH, 2);
        
        // 未使用的动作
        addAction(0, "Laugh (UNUSED)", 0, 680, defaultW, defaultH, 2);
        addAction(0, "Icecream (UNUSED)", 0, 720, defaultW, defaultH, 9);


        // ==========================================
        // 精灵图 2 (Sans-2.png) - 索引为 1
        // ==========================================
        int chairW = 56; // 躺椅精灵图较宽
        int chairH = 56;
        
        // Papyrus 的客串动作
        addAction(1, "Papyrus - Pose", 0, 20, 45, 65, 1);
        addAction(1, "Papyrus - Anime", 0, 90, 45, 65, 2);
        
        // Sans 在躺椅上的动作序列
        addAction(1, "Lawnchair - Idle", 0, 160, chairW, chairH, 1);
        addAction(1, "Lawnchair - Comb", 0, 220, chairW, chairH, 3);
        addAction(1, "Lawnchair - Nail-File", 0, 280, chairW, chairH, 2);
        addAction(1, "Lawnchair - Sunbathing", 0, 340, chairW, chairH, 1);
        
        // 喝柠檬水是 8 帧动画，在图中分为两行，每行 4 帧
        // 使用带有 framesPerRow 参数的 addAction 方法
        addAction(1, "Lawnchair - Lemonade", 0, 400, chairW, chairH, 8, 4);
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
    public void addAction(int sheetIndex, String name, int x, int y, int w, int h, int frames, int framesPerRow) {
        actions.put(name, new AnimData(sheetIndex, x, y, w, h, frames, framesPerRow));
    }

    /**
     * 播放指定的动作
     * @param actionName 动作名称 (如 "Basic - Left")
     * @param delayMs 帧间隔(毫秒)，例如 150
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
     * 在指定坐标绘制当前动画的当前帧
     */
    public void draw(Graphics2D g2d, int x, int y) {
        draw(g2d, x, y, 1.0); // 默认不缩放
    }

    /**
     * 缩放绘制 (像素游戏通常需要放大显示)
     */
    public void draw(Graphics2D g2d, int x, int y, double scale) {
        if (currentAction == null) return;
        
        AnimData data = actions.get(currentAction);
        if (data == null) return;

        BufferedImage sheet = spriteSheets[data.sheetIndex];
        if (sheet == null) return;

        // 计算当前帧的行列位置 (处理换行逻辑)
        int col = currentFrame % data.framesPerRow;
        int row = currentFrame / data.framesPerRow;

        // 计算当前帧在精灵图上的具体X/Y坐标
        int frameX = data.startX + (col * data.frameW);
        int frameY = data.startY + (row * data.frameH);

        // 防止切片越界
        if (frameX + data.frameW > sheet.getWidth() || frameY + data.frameH > sheet.getHeight()) {
            return;
        }

        // 提取子图像
        BufferedImage frameImg = sheet.getSubimage(frameX, frameY, data.frameW, data.frameH);

        // 绘制
        int drawW = (int)(data.frameW * scale);
        int drawH = (int)(data.frameH * scale);
        g2d.drawImage(frameImg, x, y, drawW, drawH, null);
    }

    /**
     * 手动绘制特定动作的指定帧（不依赖 Timer）
     */
    public void drawSpecificFrame(Graphics2D g2d, String actionName, int frameIndex, int x, int y, double scale) {
        if (!actions.containsKey(actionName)) return;
        
        AnimData data = actions.get(actionName);
        BufferedImage sheet = spriteSheets[data.sheetIndex];
        if (sheet == null) return;

        int safeFrame = frameIndex % data.frameCount;
        
        int col = safeFrame % data.framesPerRow;
        int row = safeFrame / data.framesPerRow;

        int frameX = data.startX + (col * data.frameW);
        int frameY = data.startY + (row * data.frameH);
        
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