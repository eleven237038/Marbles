import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Sans - 角色渲染与精灵图(Sprite Sheet)动画控制类
 * 支持读取包含多个动作的精灵图，并按名称调用对应动画
 */
public class Sans {
    private static final String SPRITE_PATH = "resources/image/Sans.png";
    private static BufferedImage spriteSheet;
    private static final Object LOAD_LOCK = new Object();

    // 内部类：用于存储单个动作的切片数据
    private class AnimData {
        int startX, startY; // 该动作在精灵图中的起始坐标
        int frameW, frameH; // 单帧的宽度和高度
        int frameCount;     // 该动作包含的总帧数

        public AnimData(int x, int y, int w, int h, int count) {
            this.startX = x;
            this.startY = y;
            this.frameW = w;
            this.frameH = h;
            this.frameCount = count;
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
        loadSpriteSheet();
        initActions();
        
        // 默认设置为向下的基础动作
        if (actions.containsKey("Basic - Down")) {
            currentAction = "Basic - Down";
        }
    }

    private void loadSpriteSheet() {
        if (spriteSheet != null) return;

        synchronized (LOAD_LOCK) {
            if (spriteSheet != null) return;
            try {
                spriteSheet = ImageIO.read(new File(SPRITE_PATH));
                System.out.println("成功加载 Sans 精灵图!");
            } catch (IOException e) {
                System.err.println("无法加载 Sans 精灵图: " + SPRITE_PATH);
                spriteSheet = null;
            }
        }
    }

    /**
     * 录入 Sans 的所有动作数据
     * 注意：这里的 x, y, w, h 是根据常规精灵图排布给出的估算值。
     * 你需要根据 Sans.png 实际的像素位置对 startX 和 startY 进行微调。
     */
    private void initActions() {
        int defaultW = 28; // 常规站立帧的大致宽度
        int defaultH = 32; // 常规站立帧的大致高度

        // 格式: addAction("动作名称", 起始X, 起始Y, 帧宽度, 帧高度, 帧数)
        
        // 基础行走动作
        addAction("Basic - Down", 0, 20, defaultW, defaultH, 4);
        addAction("Unused - Dangerous", 150, 20, defaultW, defaultH, 1);
        
        addAction("Basic - Left", 0, 60, defaultW, defaultH, 4);
        addAction("Shadow - Left", 150, 60, defaultW, defaultH, 4);
        
        addAction("Basic - Right", 0, 100, defaultW, defaultH, 4);
        addAction("Shadow - Right", 150, 100, defaultW, defaultH, 4);
        
        addAction("Basic - Up", 0, 140, defaultW, defaultH, 4);
        
        // 特殊交互动作
        addAction("Handshake", 0, 180, 42, defaultH, 4); // 握手动作包含两个角色，较宽
        addAction("Shrug", 0, 220, 32, defaultH, 2);     // 耸肩
        addAction("Trombone", 0, 260, 45, defaultH, 2);  // 吹长号较宽
        
        // 吧台凳子相关动作
        addAction("Stool", 0, 300, defaultW, defaultH, 2);
        addAction("Stool - Comb", 0, 340, defaultW, defaultH, 3);
        addAction("Stool - Chup", 0, 380, defaultW, defaultH, 12); // 长序列动作
        addAction("Stool - Buttscratch", 0, 420, defaultW, defaultH, 2);
        
        // 睡觉动作
        addAction("Sleep", 0, 460, defaultW, defaultH, 2);
        
        // 骑三轮车 (Trike)
        addAction("Trike", 0, 500, 38, 38, 3);
        addAction("Trike - Wink", 150, 500, 38, 38, 2);
        
        // 暗背景相关
        addAction("Dark BG", 0, 550, defaultW, defaultH, 2);
        addAction("Out To Lunch Sign", 0, 590, 45, 45, 2);
        addAction("Dark BG - Sleep Sideways (UNUSED)", 0, 640, 45, defaultH, 2);
        
        // 未使用的动作
        addAction("Laugh (UNUSED)", 0, 680, defaultW, defaultH, 2);
        addAction("Icecream (UNUSED)", 0, 720, defaultW, defaultH, 9);
    }

    /**
     * 注册一个新的动作序列
     */
    public void addAction(String name, int x, int y, int w, int h, int frames) {
        actions.put(name, new AnimData(x, y, w, h, frames));
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
        if (spriteSheet == null || currentAction == null) return;
        
        AnimData data = actions.get(currentAction);
        if (data == null) return;

        // 计算当前帧在精灵图上的具体X坐标
        int frameX = data.startX + (currentFrame * data.frameW);
        int frameY = data.startY;

        // 防止切片越界
        if (frameX + data.frameW > spriteSheet.getWidth() || frameY + data.frameH > spriteSheet.getHeight()) {
            return;
        }

        // 提取子图像
        BufferedImage frameImg = spriteSheet.getSubimage(frameX, frameY, data.frameW, data.frameH);

        // 绘制
        int drawW = (int)(data.frameW * scale);
        int drawH = (int)(data.frameH * scale);
        g2d.drawImage(frameImg, x, y, drawW, drawH, null);
    }

    /**
     * 手动绘制特定动作的指定帧（不依赖 Timer）
     */
    public void drawSpecificFrame(Graphics2D g2d, String actionName, int frameIndex, int x, int y, double scale) {
        if (spriteSheet == null || !actions.containsKey(actionName)) return;
        
        AnimData data = actions.get(actionName);
        int safeFrame = frameIndex % data.frameCount;
        
        int frameX = data.startX + (safeFrame * data.frameW);
        int frameY = data.startY;
        
        if (frameX + data.frameW <= spriteSheet.getWidth() && frameY + data.frameH <= spriteSheet.getHeight()) {
            BufferedImage frameImg = spriteSheet.getSubimage(frameX, frameY, data.frameW, data.frameH);
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