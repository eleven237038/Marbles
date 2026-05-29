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

    // ========== 开头对话系统 (更新为UT风格) ==========
    // 对话内容
    private static final String[] DIALOG_TEXT = {
        "嘿，你挺忙的嘛，是吧？",
        "这糟糕的游戏还没结束吗？",
        "你问我为什么在这儿？",
        "别在意这些细节，孩子。",
        "我都不知道你凭什么坚持到现在。",
        "总之，因为制作组的恶趣味...",
        "我得来给你找点乐子了。",
        "准备好度过一段糟糕的时光了吗？"
    };

    private static final int DIALOG_DURATION = 3500;  // 每句时长提升以利于阅读

    // 对话状态
    private int dialogIndex = -1;
    private long dialogShowTime = 0;
    private boolean dialogDone = false;
    
    // 战斗期间动态短对话系统（非阻塞）
    private String combatDialogText = null;
    private long combatDialogShowTime = 0;

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
        int[] frameXOffsets; // 每帧在水平方向的偏移量

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
        initActions();
        initHeartsArray();

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
        } catch (IOException e) {
            System.err.println("加载 heart 精灵图失败: " + e.getMessage());
        }
    }

    public void initHearts(double sansX, double sansY) {
        double startX = sansX - 16.2;
        double hy = sansY + 148 + 10 + 15;  

        for (int i = 0; i < 6; i++) {
            double hx = startX + i * HEART_SPACING;
            hearts[i].init(hx, hy);
        }
        heartCount = 6;
        heartsActive = true;
    }

    public void drawHearts(Graphics2D g) {
        if (!heartsActive || heartCount <= 0) return;
        
        for (int i = 0; i < heartCount; i++) {
            hearts[i].draw(g);
        }
    }

    private void loadSpriteSheets() {
        synchronized (LOAD_LOCK) {
            try {
                if (spriteSheets[0] == null) {
                    spriteSheets[0] = ImageIO.read(new File(SPRITE_PATH));
                }
            } catch (IOException e) {
                System.err.println("无法加载 Sans 精灵图: " + SPRITE_PATH);
            }
        }
    }

    private void initActions() {
        addAction(0, "Basic - Down", 29, 39, 98, 123, 2, new int[]{0, 192});
        addAction(0, "Basic - Right", 29, 165, 71, 123, 4, new int[]{0, 73, 146, 219});
        addAction(0, "Basic - Left", 33, 294, 71, 123, 4, new int[]{0, 73, 146, 219});
    }

    public void addAction(int sheetIndex, String name, int x, int y, int w, int h, int frames, int[] frameXOffsets) {
        actions.put(name, new AnimData(sheetIndex, x, y, w, h, frames, frameXOffsets));
    }

    public void play(String actionName, int delayMs) {
        if (!actions.containsKey(actionName)) return;

        currentAction = actionName;
        currentFrame = 0;
        
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

    public void stopAnimation() {
        if (animTimer != null) {
            animTimer.stop();
            animTimer = null;
        }
        isAnimating = false;
    }

    public void draw(Graphics2D g2d, int x, int y, double scale) {
        if (currentAction == null) return;
        AnimData data = actions.get(currentAction);
        if (data == null) return;
        BufferedImage sheet = spriteSheets[data.sheetIndex];
        if (sheet == null) return;

        int frameIndex = currentFrame % data.frameCount;
        int frameX = getFrameX(data, frameIndex);
        int frameY = data.startY;

        if (frameX + data.frameW > sheet.getWidth() || frameY + data.frameH > sheet.getHeight()) return;

        BufferedImage frameImg = sheet.getSubimage(frameX, frameY, data.frameW, data.frameH);
        int drawW = (int)(data.frameW * scale);
        int drawH = (int)(data.frameH * scale);
        g2d.drawImage(frameImg, x, y, drawW, drawH, null);
    }

    private int getFrameX(AnimData data, int frameIndex) {
        if (data.frameXOffsets != null && frameIndex < data.frameXOffsets.length) {
            return data.startX + data.frameXOffsets[frameIndex];
        }
        return data.startX + (frameIndex * data.frameW);
    }

    public void dispose() {
        stopAnimation();
    }

    // ========== UT风格 绘制对话框气泡 ==========
    public void drawUTBubble(Graphics2D g, String text, int bx, int by, boolean tailLeft) {
        String[] lines = text.split("\n");
        // 使用UT风格经典黑白气泡与字体
        Font font = new Font("Monospaced", Font.BOLD, 18);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, fm.stringWidth(l));

        int bw = maxW + 40;
        int bh = lines.length * 25 + 30;

        // 气泡底色
        g.setColor(Color.WHITE);
        g.fillRoundRect(bx, by, bw, bh, 15, 15);
        
        // 气泡边框
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(bx, by, bw, bh, 15, 15);

        // 尾巴
        if (tailLeft) {
            int[] px = {bx, bx - 25, bx};
            int[] py = {by + bh / 2 - 10, by + bh / 2 + 10, by + bh / 2 + 5};
            g.setColor(Color.WHITE);
            g.fillPolygon(px, py, 3);
            g.setColor(Color.BLACK);
            g.drawLine(bx - 25, by + bh / 2 + 10, bx, by + bh / 2 - 10);
            g.drawLine(bx - 25, by + bh / 2 + 10, bx, by + bh / 2 + 5);
            g.setColor(Color.WHITE);
            g.drawLine(bx, by + bh/2 - 8, bx, by + bh/2 + 3); // 遮盖气泡本身的黑边
        } else {
            int[] px = {bx + bw, bx + bw + 25, bx + bw};
            int[] py = {by + bh / 2 - 10, by + bh / 2 + 10, by + bh / 2 + 5};
            g.setColor(Color.WHITE);
            g.fillPolygon(px, py, 3);
            g.setColor(Color.BLACK);
            g.drawLine(bx + bw + 25, by + bh / 2 + 10, bx + bw, by + bh / 2 - 10);
            g.drawLine(bx + bw + 25, by + bh / 2 + 10, bx + bw, by + bh / 2 + 5);
            g.setColor(Color.WHITE);
            g.drawLine(bx + bw, by + bh/2 - 8, bx + bw, by + bh/2 + 3); 
        }

        g.setColor(Color.BLACK);
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], bx + 20, by + 30 + i * 25);
        }
    }

    // ========== 开头对话系统方法 ==========

    public void startDialog() {
        dialogIndex = 0;
        dialogShowTime = System.currentTimeMillis();
        dialogDone = false;
    }

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

    public boolean updateDialog() {
        if (dialogIndex >= 0 && dialogIndex < DIALOG_TEXT.length) {
            if (System.currentTimeMillis() - dialogShowTime >= DIALOG_DURATION) {
                advanceDialog();
                return true;
            }
        }
        return false;
    }

    public boolean isDialogDone() {
        return dialogDone;
    }

    public boolean isDialogActive() {
        return dialogIndex >= 0 && dialogIndex < DIALOG_TEXT.length;
    }

    public void resetDialog() {
        dialogIndex = -1;
        dialogDone = false;
        dialogShowTime = 0;
        combatDialogText = null;
    }

    public void drawDialog(Graphics2D g, int anchorX, int anchorY) {
        if (!isDialogActive()) return;

        String text = DIALOG_TEXT[dialogIndex];
        
        FontMetrics fm = g.getFontMetrics(new Font("Monospaced", Font.BOLD, 18));
        StringBuilder wrapped = new StringBuilder();
        int currentLineW = 0;
        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            int cw = fm.charWidth(c);
            if (currentLineW + cw > 220) {
                wrapped.append("\n");
                currentLineW = 0;
            }
            wrapped.append(c);
            currentLineW += cw;
        }
        
        // 渲染对话气泡，尾巴朝左指向Sans
        int bx = anchorX + 100;
        int by = anchorY - 80;
        drawUTBubble(g, wrapped.toString(), bx, by, true);
    }

    // ========== 战斗阶段非阻塞对话方法 ==========
    
    public void setCombatDialog(String text) {
        this.combatDialogText = text;
        this.combatDialogShowTime = System.currentTimeMillis();
    }

    public void drawCombatDialog(Graphics2D g, int anchorX, int anchorY) {
        if (combatDialogText == null) return;
        long elapsed = System.currentTimeMillis() - combatDialogShowTime;
        if (elapsed > 4000) {
            combatDialogText = null;
            return;
        }

        FontMetrics fm = g.getFontMetrics(new Font("Monospaced", Font.BOLD, 18));
        int maxW = 0;
        for (String l : combatDialogText.split("\n")) maxW = Math.max(maxW, fm.stringWidth(l));
        int bw = maxW + 40;

        // 渲染战斗期间气泡，位于Sans上方（居中，尾巴朝下）
        int bx = anchorX - bw / 2;
        int by = anchorY - 100;
        drawUTBubble(g, combatDialogText, bx, by, true); // tailLeft=true 尾巴在左侧底部
    }
}