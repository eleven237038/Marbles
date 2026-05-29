import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * BossSans - 角色渲染与精灵图(Sprite Sheet)动画控制类
 * 支持读取单个聚合精灵图，并按名称调用对应动画
 */
public class BossSans {
    // 统一指向新的精灵图路径
    private static final String SPRITE_PATH = ResourceManager.getImagePath("Sans.png");

    // ========== 开头对话系统 (原 IntroDialog) ==========
    // 对话内容
    private static final String[] DIALOG_TEXT = {
        "好了好了,停止这糟糕的游戏吧",
        "你说什么?",
        "我为什么会在这里?",
        "这些不重要孩子",
        "搞不懂你是凭借什么意志撑到现在的",
        "总之,由于这个制作组的无聊安排",
        "现在,我看不下去你继续浪费自己的生命了",
        "所以,你需要击败我才能继续游玩这个游戏"
    };

    // 对话框参数
    private static final int MAX_CHARS_PER_LINE = 20;
    private static final int TEXT_PADDING = 35;
    private static final int LINE_HEIGHT = 30;
    private static final int MIN_WIDTH = 300;
    private static final int MAX_WIDTH = 450;
    private static final int DIALOG_DURATION = 3000;  // 3秒

    // 对话状态
    private int dialogIndex = -1;
    private long dialogShowTime = 0;
    private BufferedImage dialogBubbleImg = null;
    private boolean dialogDone = false;

    // 回调接口
    private Runnable onDialogDone;
    
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

    // Hearts 数据
    private static BufferedImage heartSprite = null;
    private Heart[] hearts = new Heart[6];
    private boolean heartsActive = false;
    private int heartCount = 6;  // 当前剩余heart数量
    private static final double HEART_SIZE = 30;
    private static final double HEART_SPACING = 30;

    // 回调接口：当所有heart消失时的处理
    private Runnable onAllHeartsRemoved;

    // 内部类：独立的心形对象
    private static class Heart {
        private double cx, cy;
        private boolean initialized;

        Heart() {
            this.cx = 0;
            this.cy = 0;
            this.initialized = false;
        }

        void init(double cx, double cy) {
            this.cx = cx;
            this.cy = cy;
            this.initialized = true;
        }

        void draw(Graphics2D g) {
            if (!initialized || heartSprite == null) return;
            int size = (int) HEART_SIZE;
            int drawX = (int)(cx - size / 2);
            int drawY = (int)(cy - size / 2);
            g.drawImage(heartSprite, drawX, drawY, size, size, null);
        }
    }

    public BossSans() {
        loadSpriteSheets();
        loadHeartSprite();
        loadDialogBubble();
        initActions();
        initHeartsArray();

        // 默认设置为向下的基础动作
        if (actions.containsKey("Basic - Down")) {
            currentAction = "Basic - Down";
        }
    }

    private void initHeartsArray() {
        for (int i = 0; i < 6; i++) {
            hearts[i] = new Heart();
        }
        heartCount = 6;
        heartsActive = false;
    }

    /**
     * 移除一个heart，从右向左依次消失，当全部消失时触发回调
     */
    public void removeOneHeart() {
        if (heartCount <= 0) return;

        heartCount--;
        if (heartCount <= 0) {
            heartsActive = false;
            if (onAllHeartsRemoved != null) {
                onAllHeartsRemoved.run();
            }
        }
    }

    /**
     * 设置当所有heart消失时的回调
     */
    public void setOnAllHeartsRemoved(Runnable callback) {
        this.onAllHeartsRemoved = callback;
    }

    public int getHeartCount() {
        return heartCount;
    }

    private void loadHeartSprite() {
        try {
            String imagePath = ResourceManager.getImagePath("heart.png");
            heartSprite = ImageIO.read(new File(imagePath));
            System.out.println("成功加载 heart 精灵图!");
        } catch (IOException e) {
            System.err.println("加载 heart 精灵图失败: " + e.getMessage());
        }
    }

    /**
     * 在BossSans站立后，初始化6个heart在其下方一行
     */
    public void initHearts(double sansX, double sansY) {
        // 使第3、4颗heart的x中间位置与Sans中心对齐
        // Sans站立图绘制宽度98*1.2=117.6，中心偏移=117.6/2=58.8
        // heart索引2中心x = startX + 60，索引3中心x = startX + 90
        // 两颗中点 = (60+90)/2 + startX = startX + 75
        // 中点与Sans中心对齐: startX + 75 = sansX + 58.8 => startX = sansX - 16.2
        double startX = sansX - 16.2;
        // heart行的y最上位置与Sans的y最下位置间隔10个像素
        double hy = sansY + 148 + 10 + 15;  // sansY + 173

        for (int i = 0; i < 6; i++) {
            double hx = startX + i * HEART_SPACING;
            hearts[i].init(hx, hy);
        }
        heartCount = 6;
        heartsActive = true;
    }

    /**
     * 绘制 hearts（当BossSans站立时），实现从右向左依次消失的效果
     * 因为 index 从 0 递增到 5，0 是最左，5 是最右。
     * 要从右向左消失，我们应当只保留最左侧的 heartCount 个：即绘制 index 在 [0, heartCount - 1] 之间的 Hearts 即可。
     */
    public void drawHearts(Graphics2D g) {
        if (!heartsActive || heartCount <= 0) return;
        
        // 仅绘制最左侧未扣除的 heartCount 颗，实现“最右侧先开始减少并向左蔓延”的效果
        for (int i = 0; i < heartCount; i++) {
            hearts[i].draw(g);
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
        // 站立 (Basic - Down): 2帧，每帧4顶点（两帧交替）
        // 帧0: (29,39)(29,162)(124,162)(124,39)
        // 帧1: (221,39)(221,162)(319,162)(319,39)
        // ==========================================
        addAction(0, "Basic - Down", 29, 39, 98, 123, 2, new int[]{0, 192});

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

    // ========== 开头对话系统方法 (原 IntroDialog) ==========

    private void loadDialogBubble() {
        try {
            String imagePath = ResourceManager.getImagePath("Text Bubbles/1.png");
            dialogBubbleImg = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            System.err.println("未能加载对话框精灵图: " + e.getMessage());
        }
    }

    /**
     * 开始对话
     */
    public void startDialog() {
        dialogIndex = 0;
        dialogShowTime = System.currentTimeMillis();
        dialogDone = false;
    }

    /**
     * 进入下一句对话
     */
    public void advanceDialog() {
        if (dialogIndex >= 0 && dialogIndex < DIALOG_TEXT.length) {
            dialogIndex++;
            dialogShowTime = System.currentTimeMillis();
            if (dialogIndex >= DIALOG_TEXT.length) {
                dialogIndex = -1;
                dialogDone = true;
                if (onDialogDone != null) {
                    onDialogDone.run();
                }
            }
        }
    }

    /**
     * 更新对话（每帧调用，返回是否自动推进）
     */
    public boolean updateDialog() {
        if (dialogIndex >= 0 && dialogIndex < DIALOG_TEXT.length) {
            if (System.currentTimeMillis() - dialogShowTime >= DIALOG_DURATION) {
                advanceDialog();
                return true;
            }
        }
        return false;
    }

    /**
     * 是否对话结束
     */
    public boolean isDialogDone() {
        return dialogDone;
    }

    /**
     * 当前是否有对话在进行
     */
    public boolean isDialogActive() {
        return dialogIndex >= 0 && dialogIndex < DIALOG_TEXT.length;
    }

    /**
     * 设置对话结束时的回调
     */
    public void setOnDialogDone(Runnable callback) {
        this.onDialogDone = callback;
    }

    /**
     * 重置对话
     */
    public void resetDialog() {
        dialogIndex = -1;
        dialogDone = false;
        dialogShowTime = 0;
    }

    /**
     * 绘制对话框（在指定位置）
     */
    public void drawDialog(Graphics2D g, int anchorX, int anchorY) {
        if (!isDialogActive()) return;

        String text = DIALOG_TEXT[dialogIndex];
        Font font = new Font("SimHei", Font.BOLD, 18);
        FontMetrics fm = g.getFontMetrics(font);

        // 计算文字行数
        String line1, line2;
        int textWidth;
        if (text.length() > MAX_CHARS_PER_LINE) {
            line1 = text.substring(0, MAX_CHARS_PER_LINE);
            line2 = text.length() > MAX_CHARS_PER_LINE * 2
                ? text.substring(MAX_CHARS_PER_LINE * 2)
                : text.substring(MAX_CHARS_PER_LINE);
            textWidth = Math.max(fm.stringWidth(line1), fm.stringWidth(line2)) + TEXT_PADDING * 2;
        } else {
            line1 = text;
            line2 = null;
            textWidth = fm.stringWidth(text) + TEXT_PADDING * 2;
        }
        textWidth = Math.min(Math.max(textWidth, MIN_WIDTH), MAX_WIDTH);

        int lines = line2 != null ? 2 : 1;
        int bw = textWidth;
        int bh = lines * LINE_HEIGHT + 30;

        // 对话框位置（锚点在Sans头顶，偏移到右边）
        int bx = anchorX + 115;
        int by = anchorY - 50;

        // 绘制背景
        if (dialogBubbleImg != null) {
            g.drawImage(dialogBubbleImg, bx, by, bw, bh, null);
        } else {
            // 回退效果
            g.setColor(Color.WHITE);
            g.fillRoundRect(bx, by, bw, bh, 20, 20);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(bx, by, bw, bh, 20, 20);
            // 三角指向
            int[] px = {bx, bx - 20, bx + 20};
            int[] py = {by + bh / 2 - 10, by + bh / 2, by + bh / 2 + 10};
            g.setColor(Color.WHITE);
            g.fillPolygon(px, py, 3);
            g.setColor(Color.BLACK);
            g.drawLine(bx - 20, by + bh / 2, bx, by + bh / 2 - 10);
            g.drawLine(bx - 20, by + bh / 2, bx + 20, by + bh / 2 + 10);
        }

        // 绘制文字内容（居中）
        g.setColor(Color.BLACK);
        g.setFont(font);

        int totalTextWidth = Math.max(
            fm.stringWidth(line1),
            line2 != null ? fm.stringWidth(line2) : 0
        );
        int textX = bx + (bw - totalTextWidth) / 2;
        int textY = by + 45;

        g.drawString(line1, textX, textY);
        if (line2 != null) {
            g.drawString(line2, textX, textY + LINE_HEIGHT);
        }
    }

    public String[] getDialogText() {
        return DIALOG_TEXT;
    }

    public int getDialogIndex() {
        return dialogIndex;
    }
}