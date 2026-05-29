import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import java.util.prefs.Preferences;
import java.awt.geom.Point2D;
import java.io.File;
import javax.imageio.ImageIO;

public class Main extends GameEngine implements ScreenStart.ScreenStartListener {
    private Marbles hexGrid;
    private ScreenGame launchPad;
    private MarbleLaunch launchMarble;
    private double mouseX = 0;
    private double mouseY = 0;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    public boolean gameStarted = false;
    private boolean frozen = false;
    private double deadline;
    private Random random = new Random();
    private ScreenStart startScreen;

    // 布局尺寸常量
    public static final int GAME_ZONE_WIDTH = 483;
    public static final int LEFT_ZONE_WIDTH = 250;
    public static final int TOTAL_WIDTH = LEFT_ZONE_WIDTH + GAME_ZONE_WIDTH;
    public static final int GAME_HEIGHT = 560;
    private static final double CANNON_MOVE_SPEED = 400;

    private boolean gamePaused = false;

    private boolean upPressed = false;
    private boolean downPressed = false;

    private boolean wasStarted = false;

    private int currentScore = 0;
    private int levelHighScore = 0;
    private int levelWinScore = 0;
    private boolean levelWon = false;
    private int highScore = 0;
    private CustomGlassPane glassPane;
    
    // BossSans 角色状态
    private BossSans sans;
    private boolean sansActive = false;
    private boolean sansIdle = false;
    private double sansX, sansY;
    private boolean sansAnimating = false;
    private javax.swing.Timer idleRepaintTimer = null;

    // BossSans 技能与计时器
    private double sansSkillTimer = 30.0;
    private int sansCreeperShots = 0;
    // 永久苦力怕发射台技能激活状态
    private boolean sansCreeperActive = false;
    private int sansSkill6Count = 0; // Tracking Skill 6 abuse
    private boolean sansLeaving = false; // Is Sans in the process of leaving?
    private double level4Timer = 0;      // Tracking 10 mins requirement

    private javax.swing.Timer dialogTimer = null;
    private boolean utStyleDone = false;

    // Undertale风格变化标记
    public static boolean utBg = false;
    public static boolean utFont = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("弹珠游戏 - 豪华版");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            Main game = new Main(frame);

            ScreenStart startScreen = new ScreenStart(game);
            game.startScreen = startScreen;

            JPanel gameContainer = new JPanel(null);
            gameContainer.setOpaque(false);
            gameContainer.setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));

            game.mPanel.setBounds(LEFT_ZONE_WIDTH, 0, GAME_ZONE_WIDTH, GAME_HEIGHT);
            gameContainer.add(game.mPanel);

            mainPanel.add(startScreen, "menu");
            mainPanel.add(gameContainer, "game");

            frame.setContentPane(mainPanel);
            frame.pack();

            Insets insets = frame.getInsets();
            int targetWidth = TOTAL_WIDTH + insets.left + insets.right;
            int targetHeight = GAME_HEIGHT + insets.top + insets.bottom;
            frame.setSize(targetWidth, targetHeight);

            startScreen.setPreferredSize(new Dimension(TOTAL_WIDTH, GAME_HEIGHT));

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            game.cardLayout = cardLayout;
            game.mainPanel = mainPanel;

            game.glassPane = game.new CustomGlassPane();
            frame.setGlassPane(game.glassPane);
            game.glassPane.setVisible(false);
        });
    }

    public Main(JFrame frame) {
        mFrame = frame;
        mPanel = new GamePanel();
        mWidth = GAME_ZONE_WIDTH;
        mHeight = GAME_HEIGHT;

        mPanel.setDoubleBuffered(true);
        mPanel.addMouseListener(this);
        mPanel.addMouseMotionListener(this);
        mPanel.setPreferredSize(new Dimension(mWidth, mHeight));

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        switch (e.getID()) {
                            case KeyEvent.KEY_PRESSED:
                                Main.this.keyPressed(e);
                                return false;
                            case KeyEvent.KEY_RELEASED:
                                Main.this.keyReleased(e);
                                return false;
                            case KeyEvent.KEY_TYPED:
                                Main.this.keyTyped(e);
                                return false;
                            default:
                                return false;
                        }
                    }
                });
        highScore = loadHighScore();
    }

    @Override
    public void onStartGame() {
        ResourceManager.getInstance().playGameBegin();

        cardLayout.show(mainPanel, "game");
        currentScore = 0;

        boolean wasStartedLocal = gameStarted;
        gameStarted = true;

        if (!wasStartedLocal) {
            if (startScreen != null) {
                startScreen.stopAnimation();
            }
            init();
            gameLoop(60);
        } else {
            init();
        }

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
        }
        
        // 如果该关有 BossSans 出场
        if (Level.getInstance().hasBossSans()) {
            startBossSansIntro();
        }
    }

    @Override
    public void init() {
        utBg = false;
        utFont = false;
        Marble.utStyle = false;

        Level level = Level.getInstance();
        levelWinScore = level.getWinScore();
        levelHighScore = level.getLevelHighScore();
        levelWon = false;

        utStyleDone = false;
        if (sans != null) sans.resetDialog();

        // 初始化 BossSans 角色并重置状态
        if (sans == null) {
            sans = new BossSans();
        }
        sansActive = false;
        sansIdle = false;
        sansAnimating = false;
        sansSkillTimer = 30.0;
        sansCreeperShots = 0;
        sansCreeperActive = false; // 重置苦力怕技能
        sansSkill6Count = 0;
        sansLeaving = false;
        level4Timer = 0;
        
        if (idleRepaintTimer != null) {
            idleRepaintTimer.stop();
            idleRepaintTimer = null;
        }

        initMarbleGrid();
        upPressed = false;
        downPressed = false;

        hexGrid.setScoreListener((marble, points) -> {
            // 检测heart掉落与消除
            if (marble != null && marble.getColorType() == Marble.HEART && sans != null && sansActive) {
                sans.removeOneHeart();
                // 当heart剩余2颗时，切换成正义之矛.mp3并放出非阻塞对话（并且下落速度切阶段）
                if (sans.getHeartCount() == 2) {
                    ResourceManager.getInstance().playJusticeMusic();
                    sans.setCombatDialog("看来我不能继续偷懒了。\n准备好度过一段糟糕的时光了吗？");
                    // 以节拍比例提速至 2.19x 等效的高潮致命坠落速度
                    hexGrid.setCurrentFallSpeed(26.28);
                }
            }

            currentScore += points;
            if (currentScore > levelHighScore) {
                levelHighScore = currentScore;
                level.updateLevelHighScore(levelHighScore);
            }
            if (!levelWon && level.isWinConditionMet(currentScore)) {
                levelWon = true;
            }
            if (glassPane != null) {
                glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
            }
        });
    }

    private void initMarbleGrid() {
        Level level = Level.getInstance();
        hexGrid = new Marbles();
        launchPad = new ScreenGame();
        hexGrid.setMaxRowCount(18);
        hexGrid.setFallSpeedMultiplier(level.getFallSpeedMultiplier());
        hexGrid.setLevelSpeedParams(level.getBaseFallSpeed(), level.getMaxFallSpeed(), level.getSpeedIncreaseRate());
        
        // 如果是 level4，则彻底关闭自然生成 creeper 逻辑，交由 BossSans 的技能来动态触发
        boolean enableCreeper = level.hasCreeper() && level.getCurrentLevel() != 4;
        hexGrid.setSpecialMarbleConfig(enableCreeper, level.hasBedrock(), level.hasHeart());
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();

        MarbleLaunch.resetCounters();

        launchMarble = new MarbleLaunch();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);
        launchPad.setNextMarbleColorType(random.nextInt(level.getColorTypeCount()) + 1);
    }

    @Override
    public void update(double dt) {
        if (frozen || gamePaused) return;
        if (hexGrid != null) hexGrid.update(dt, deadline);

        // 仅当过场动画结束、游戏正常开启且非暂停状态时，进行 Sans 状态和攻击计时。
        if (sansActive && gameStarted && !frozen && !gamePaused) {
            
            // 10 分钟枯燥超时机制检查
            if (Level.getInstance().getCurrentLevel() == 4) {
                level4Timer += dt;
                if (level4Timer >= 600.0 && !sansLeaving) {
                    triggerSansLeave(true, "呃...我们打了多久了？10分钟？\n你赢了，我太累了，先睡了。");
                    return;
                }
            }
            
            sansSkillTimer -= dt;
            if (sansSkillTimer <= 0) {
                triggerSansSkill();
                sansSkillTimer = 30.0;
            }
        }

        if (launchPad != null) {
            boolean moved = false;
            if (upPressed) {
                launchPad.cannon.y -= CANNON_MOVE_SPEED * dt;
                moved = true;
            }
            if (downPressed) {
                launchPad.cannon.y += CANNON_MOVE_SPEED * dt;
                moved = true;
            }

            if (moved) {
                if (launchPad.cannon.y < deadline) launchPad.cannon.y = deadline;
                if (launchPad.cannon.y > mHeight) launchPad.cannon.y = mHeight;

                if (launchMarble != null && !launchMarble.isLaunched()) {
                    launchMarble.setCenter(launchPad.cannon.x, launchPad.cannon.y);
                }
            }
        }

        if (launchMarble != null) launchMarble.update(dt);
        checkCollisions();
        collisionWithDeadline();
    }

    // Sans 走过场触发通用逻辑
    private void triggerSansLeave(boolean win, String dialog) {
        if (sansLeaving) return;
        sansLeaving = true;
        frozen = true; // 冻结整个弹珠与碰撞系统
        
        sans.setCombatDialog(dialog);
        sansIdle = true;
        sansAnimating = false;
        
        // 给予2.5秒的停留时间让玩家阅读完毕
        javax.swing.Timer waitTimer = new javax.swing.Timer(2500, ev -> {
            ((javax.swing.Timer) ev.getSource()).stop();
            sansIdle = false;
            sansAnimating = true;
            sans.play("Basic - Left", 150);
            
            javax.swing.Timer leaveTimer = new javax.swing.Timer(16, ev2 -> {
                sansX -= 3;
                if (glassPane != null) {
                    glassPane.repaint();
                }
                if (sansX < -250) {
                    ((javax.swing.Timer) ev2.getSource()).stop();
                    sansActive = false;
                    sansAnimating = false;
                    openScreenGameOverMenu(win);
                }
            });
            leaveTimer.start();
        });
        waitTimer.start();
    }

    // Trigger Undertale Sans signature boss battle skills and dialogs
    private void triggerSansSkill() {
        if (hexGrid == null || sans == null || sansLeaving) return;
        int skillType;
        
        // 当出现警戒 Marble 时，Sans 的触发技能恒定锁定为 6 (Creeper 支持)
        if (hexGrid.hasWarning()) {
            skillType = 6;
        } else {
            skillType = random.nextInt(6) + 1;
        }

        // 非技能 6 时，解除恒定 creeper 发射状态
        if (skillType != 6) {
            sansCreeperActive = false;
        }

        switch(skillType) {
            case 1:
                hexGrid.skillAlternateColors(2);
                String[] s1Dialogs = {
                    "嗯，颜色打乱。看看你有多喜欢彩虹吧。",
                    "打乱一下盘面...保持新鲜感，不是吗？",
                    "五彩缤纷的混乱来了。别头晕哦。"
                };
                sans.setCombatDialog(s1Dialogs[random.nextInt(s1Dialogs.length)]);
                break;
            case 2:
                hexGrid.skillBedrockRadius();
                String[] s2Dialogs = {
                    "不错的选择。字面意义上的。",
                    "封锁一些路径。希望你不介意。",
                    "给你一块 bedrock。"
                };
                sans.setCombatDialog(s2Dialogs[random.nextInt(s2Dialogs.length)]);
                break;
            case 3:
                hexGrid.skillBedrockRow();
                String[] s3Dialogs = {
                    "只有一个缝隙。好好把握机会吧，孩子。",
                    "几乎是一堵完整的墙。试试挤过去吧。"
                };
                sans.setCombatDialog(s3Dialogs[random.nextInt(s3Dialogs.length)]);
                break;
            case 4:
                String[] s4Dialogs = {
                    "其实我应该攻击了...\n但我太累了。",
                    "要不休息一下？什么都不做可是我的专长。",
                    "我就站在这里不动。\n顺便说一句，你做得很好。"
                };
                sans.setCombatDialog(s4Dialogs[random.nextInt(s4Dialogs.length)]);
                break;
            case 5:
                hexGrid.skillTeleportDown(2);
                String[] s5Dialogs = {
                    "哎呀，重力又出问题了。",
                    "小小的捷径...直接往下。",
                    "小心脚下。东西在往下滑。"
                };
                sans.setCombatDialog(s5Dialogs[random.nextInt(s5Dialogs.length)]);
                break;
            case 6:
                sansSkill6Count++;
                if (sansSkill6Count > 5) {
                    triggerSansLeave(false, "又是这个绿色的东西？\n看来你真的只知道用爆炸。\n我受够了，你自己玩吧。");
                    return;
                }
            
                sansCreeperActive = true;
                if (launchMarble != null) {
                    launchMarble.setColorType(Marble.CREEPER);
                }
                if (launchPad != null) {
                    launchPad.setNextMarbleColorType(Marble.CREEPER);
                }

                if (hexGrid.hasWarning()) {
                    String[] warnDialogs = {
                        "看起来你处境很紧张。\n给你一些爆炸支援吧。",
                        "哇，太近了。让我们清理一下空气。",
                        "你看起来快输了。让我们炸点东西吧。"
                    };
                    sans.setCombatDialog(warnDialogs[random.nextInt(warnDialogs.length)]);
                } else {
                    String[] s6Dialogs = {
                        "爆炸快递。小心轻放哦，孩子。",
                        "Boom 的时刻到了。别把自己炸上天了。",
                        "给你，玩玩这些绿色的朋友吧。"
                    };
                    sans.setCombatDialog(s6Dialogs[random.nextInt(s6Dialogs.length)]);
                }
                break;
        }
    }

    private void checkCollisions() {
        if (launchMarble == null || !launchMarble.isLaunched() || hexGrid == null) return;

        double radius = hexGrid.getSide() * 0.866;
        double collisionDist = radius * 2 - 2;

        double prevX = launchMarble.getPrevCenterX();
        double prevY = launchMarble.getPrevCenterY();
        double currX = launchMarble.getCenterX();
        double currY = launchMarble.getCenterY();

        double dx = currX - prevX;
        double dy = currY - prevY;

        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy) / (radius * 0.25));
        if (steps < 1) steps = 1;

        boolean collided = false;

        for (int i = 1; i <= steps; i++) {
            double checkX = prevX + dx * i / steps;
            double checkY = prevY + dy * i / steps;

            if (checkY <= radius) {
                collided = true;
                launchMarble.setCenter(checkX, radius);
                break;
            }

            for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
                Marble[] row = hexGrid.getRow(r);
                if (row == null) continue;
                for (Marble m : row) {
                    if (m != null && m.isInitialized() && !m.isPopping() && !m.isFalling()) {
                        double dX = checkX - m.getCenterX();
                        double dY = checkY - m.getCenterY();
                        double distSq = dX * dX + dY * dY;
                        if (distSq <= collisionDist * collisionDist) {
                            collided = true;
                            launchMarble.setCenter(checkX, checkY);
                            break;
                        }
                    }
                }
                if (collided) break;
            }
            if (collided) break;
        }

        if (collided) {
            hexGrid.attachMarble(launchMarble, mWidth);

            int nextColor = launchPad.getNextMarbleColorType();
            launchMarble = new MarbleLaunch();
            launchMarble.setScreenSize(mWidth, mHeight);
            launchMarble.setColorType(nextColor);
            launchMarble.init(launchPad.cannon.x, launchPad.cannon.y, 0, 0);

            // 如果永久苦力怕技能被 Sans 开启，大炮子弹和下一发备用子弹恒设为 CREEPER
            if (sansCreeperActive) {
                launchMarble.setColorType(Marble.CREEPER);
                launchPad.setNextMarbleColorType(Marble.CREEPER);
            } else if (sansCreeperShots > 0) {
                sansCreeperShots--;
                if (sansCreeperShots > 0) {
                    launchPad.setNextMarbleColorType(Marble.CREEPER);
                } else {
                    int colorTypeCount = Level.getInstance().getColorTypeCount();
                    launchPad.setNextMarbleColorType(random.nextInt(colorTypeCount) + 1);
                }
            } else {
                int level = Level.getInstance().getCurrentLevel();
                if (level == 2 || level == 3) {
                    launchMarble.setSpecialMarbleForLevel(random, level);
                }
                int colorTypeCount = Level.getInstance().getColorTypeCount();
                launchPad.setNextMarbleColorType(random.nextInt(colorTypeCount) + 1);
            }
        }
    }

    private void collisionWithDeadline() {
        if (sansLeaving) return; // 已执行过场时不重复触发
        if (hexGrid == null) return;
        double radius = hexGrid.getSide() * 0.866;
        for (int r = 0; r < hexGrid.getMarblesLength(); r++) {
            Marble[] row = hexGrid.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.length; c++) {
                Marble marble = row[c];
                if (marble != null && marble.isInitialized() && !marble.isPopping() && !marble.isFalling() && marble.getCenterY() + radius >= deadline) {
                    if (sansActive && Level.getInstance().getCurrentLevel() == 4) {
                        triggerSansLeave(false, "看来你已经到极限了。\n那么，游戏到此为止吧。");
                    } else {
                        openScreenGameOverMenu(false);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void paintComponent() {
        Graphics2D g2 = (Graphics2D) mGraphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (utBg) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, mWidth, mHeight);
        } else {
            LinearGradientPaint bg = new LinearGradientPaint(
                    0, 0, 0, mHeight,
                    new float[]{0, 1},
                    new Color[]{new Color(188, 195, 255), new Color(188, 195, 255)}
            );
            g2.setPaint(bg);
            g2.fillRect(0, 0, mWidth, mHeight);

            g2.setColor(new Color(200, 220, 240, 180));
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(0, 0, 0, mHeight);
        }

        if (hexGrid != null) hexGrid.draw(g2);
        if (launchPad != null) {
            launchPad.drawLaunchPad(g2, mWidth, mHeight);
            launchPad.drawCannon(g2, mouseX, mouseY);
        }
        if (launchMarble != null) launchMarble.draw(g2);
    }

    public void openPauseMenu() {
        gamePaused = true;
        ResourceManager.getInstance().playBackToMenu();
        ResourceManager.getInstance().pauseMusic();
        if (glassPane != null) {
            glassPane.showOverlay(1, false);
        }
    }

    public void closePauseMenu() {
        ResourceManager.getInstance().playBackToMenu();
        ResourceManager.getInstance().resumeMusic();

        gamePaused = false;
        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        mPanel.repaint();
    }

    private void openScreenGameOverMenu(boolean win) {
        if (!win) {
            ResourceManager.getInstance().playGameFail();
        }

        if (Level.getInstance().getCurrentLevel() != 4) {
            ResourceManager.getInstance().stopMusic();
        }

        frozen = true;
        gamePaused = true;
        if (glassPane != null) {
            glassPane.showOverlay(2, win);
        }
    }

    private void returnToMenu() {
        ResourceManager.getInstance().playBackToMenu();
        ResourceManager.getInstance().stopMusic();

        utBg = false;
        utFont = false;
        Marble.utStyle = false;

        frozen = false;
        gamePaused = false;
        gameStarted = false;
        hexGrid = null;
        launchMarble = null;
        upPressed = false;
        downPressed = false;
        mPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        if (glassPane != null) {
            glassPane.hideOverlay();
        }

        cardLayout.show(mainPanel, "menu");

        if (startScreen != null) {
            startScreen.restartAnimation();
        }
    }

    public void onBackToMenu() {
        returnToMenu();
    }

    public void onRestart() {
        ResourceManager.getInstance().playBackToMenu();
        if (Level.getInstance().getCurrentLevel() != 4) {
            ResourceManager.getInstance().stopMusic();
        }

        if (glassPane != null) {
            glassPane.hideOverlay();
        }
        frozen = false;
        gamePaused = false;
        currentScore = 0;
        upPressed = false;
        downPressed = false;
        init();
        if (glassPane != null) {
            glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
        }
        mPanel.repaint();

        if (Level.getInstance().hasBossSans()) {
            startBossSansIntro();
        }
    }

    private void startBossSansIntro() {
        frozen = true;
        gamePaused = true;
        sansActive = true;
        sansAnimating = true;

        utStyleDone = false;
        if (sans != null) sans.resetDialog();

        int targetX = LEFT_ZONE_WIDTH / 2 - 47;
        int targetY = GAME_HEIGHT - 320; 
        
        sansX = -80; 
        sansY = targetY;

        final double finalX = targetX;
        final double finalY = targetY;
        
        sans.play("Basic - Right", 150);

        final long ANIM_DURATION = 2000; 
        final long startTime = System.currentTimeMillis();

        javax.swing.Timer sansTimer = new javax.swing.Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double t = Math.min(1.0, (double) elapsed / ANIM_DURATION);

            sansX = -80 + (finalX - (-80)) * t;
            sansY = finalY;

            if (glassPane != null) {
                glassPane.repaint();
            }

            if (elapsed >= ANIM_DURATION) {
                ((javax.swing.Timer) e.getSource()).stop();
                sans.stopAnimation();
                sansAnimating = false;
                sansIdle = true;
                sans.play("Basic - Down", 500);

                sans.startDialog();

                dialogTimer = new javax.swing.Timer(100, null);
                dialogTimer.addActionListener(evt -> {
                    sans.updateDialog();
                    if (sans.isDialogDone()) {
                        ((javax.swing.Timer)evt.getSource()).stop();
                        checkIntroDone(); 
                    }
                });
                dialogTimer.start();

                idleRepaintTimer = new javax.swing.Timer(16, evt -> {
                    if (glassPane != null) {
                        glassPane.repaint();
                    }
                });
                idleRepaintTimer.start();

                if (glassPane != null) {
                    glassPane.repaint();
                }
            }
        });
        sansTimer.start();
    }

    private void checkIntroDone() {
        if (sans != null && sans.isDialogDone() && !utStyleDone) {
            utStyleDone = true;
            javax.swing.Timer styleTimer = new javax.swing.Timer(1000, null);
            styleTimer.addActionListener(new java.awt.event.ActionListener() {
                int step = 0;
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    step++;
                    if (step == 1) {
                        Main.utBg = true;
                    } else if (step == 2) {
                        Main.utFont = true;
                    } else if (step == 3) {
                        Marble.utStyle = true;
                        ((javax.swing.Timer)evt.getSource()).stop();

                        finishIntro(); 
                    }
                    if (glassPane != null) glassPane.repaint();
                    if (mPanel != null) mPanel.repaint();
                }
            });
            styleTimer.start();
        }
    }

    private void finishIntro() {
        frozen = false;
        gamePaused = false;

        ResourceManager.getInstance().playBonelessMusic();

        sansCreeperShots = 0;
        sansCreeperActive = false;
        sansSkillTimer = 30.0;

        sans.initHearts(sansX, sansY);
        sans.setOnAllHeartsRemoved(() -> {
            if (sansLeaving) return;
            triggerSansLeave(true, "猜我只是... 太懒了躲不开。\npapyrus，你想要点什么吗？");
        });
    }

    public void onNextLevel() {
        Level.getInstance().nextLevel();
        onRestart();
    }

    @Override
    public void onSelectLevel(int level) {
        ResourceManager.getInstance().playGameBegin();
        if (level != 4) {
            ResourceManager.getInstance().stopMusic();
        }
        Level.getInstance().setCurrentLevel(level);

        cardLayout.show(mainPanel, "game");
        currentScore = 0;

        wasStarted = gameStarted;
        gameStarted = true;

        if (!wasStarted) {
            if (startScreen != null) {
                startScreen.stopAnimation();
            }
            init();
            gameLoop(60);
        } else {
            init();
        }

        if (glassPane != null) {
            glassPane.setVisible(true);
            glassPane.updateScores(currentScore, highScore, levelHighScore, levelWinScore);
        }
        
        if (Level.getInstance().hasBossSans(level)) {
            startBossSansIntro();
        }
    }

    @Override
    public void onOpenSettings() {
        ResourceManager.getInstance().playBackToMenu();
        if (glassPane != null) {
            glassPane.showSettings();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (frozen || gamePaused) return;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (frozen || gamePaused) return;
        if (launchMarble != null && !launchMarble.isLaunched()) {
            performLaunch(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen || gamePaused) return;

        if (event.getKeyCode() == KeyEvent.VK_UP) upPressed = true;
        if (event.getKeyCode() == KeyEvent.VK_DOWN) downPressed = true;

        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null && !launchMarble.isLaunched()) {
            performLaunch(mouseX > 0 ? mouseX : launchPad.cannon.x,
                    mouseY > 0 ? mouseY : launchPad.cannon.y - 100);
        }
    }

    private void performLaunch(double targetX, double targetY) {
        launchPad.updateCannonAngle(targetX, targetY);
        launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
        Point2D.Double muzzle = launchPad.getMuzzlePosition();
        launchMarble.launch(muzzle.x, muzzle.y);

        if (hexGrid != null) {
            hexGrid.setLastLaunchPosition(muzzle.x, muzzle.y);
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
        if (event.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
    }

    private int loadHighScore() {
        return Preferences.userNodeForPackage(Main.class).getInt("highScore", 0);
    }

    private void saveHighScore(int score) {
        Preferences.userNodeForPackage(Main.class).putInt("highScore", score);
    }

    class CustomGlassPane extends JComponent {
        private Rectangle pauseButtonRect;
        private boolean pauseHover = false, pausePressed = false;
        private int overlayMode = 0;
        private boolean isScreenGameOverWin = false;

        private boolean animating = false;
        private long animStartTime = 0;
        private static final long ANIM_DURATION = 350;
        private static final double ANIM_OVERSHOOT = 0.12;
        private javax.swing.Timer animTimer;
        private int returnToMode = 0;

        public CustomGlassPane() {
            setOpaque(false);
            setFocusable(false);
            setLayout(null);

            animTimer = new javax.swing.Timer(16, e -> {
                if (animating) {
                    long elapsed = System.currentTimeMillis() - animStartTime;
                    if (elapsed >= ANIM_DURATION) {
                        animating = false;
                        animTimer.stop();
                    }
                    repaint();
                }
            });

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point p = e.getPoint();

                    if (sans != null && sans.isDialogActive()) {
                        sans.advanceDialog();
                        return; 
                    }

                    if (overlayMode == 0) {
                        if (gameStarted && pauseButtonRect != null && pauseButtonRect.contains(p)) {
                            pausePressed = true;
                            repaint();
                            openPauseMenu();
                            pausePressed = false;
                            Point mp = getMousePosition();
                            pauseHover = (mp != null && pauseButtonRect.contains(mp));
                            repaint();
                        }
                    } else {
                        if (!animating) {
                            handleOverlayClick(p);
                        }
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    pauseHover = false;
                    repaint();
                }
            };
            addMouseListener(mouseAdapter);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    if (sans != null && sans.isDialogActive()) {
                        setCursor(Cursor.getDefaultCursor());
                        return;
                    }

                    if (overlayMode == 0) {
                        boolean newHover = gameStarted && pauseButtonRect != null && pauseButtonRect.contains(p);
                        if (pauseHover != newHover) {
                            pauseHover = newHover;
                            repaint();
                            setCursor(pauseHover ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        }
                    } else {
                        updateOverlayHover(p);
                    }
                }
            });
        }

        private void handleOverlayClick(Point p) {
            if (overlayMode == 1) {
                if (lastResumeBtn != null && lastResumeBtn.contains(p)) {
                    closePauseMenu();
                } else if (lastHelpBtn != null && lastHelpBtn.contains(p)) {
                    showHelp();
                } else if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    closePauseMenu();
                    returnToMenu();
                }
            } else if (overlayMode == 2) {
                if (levelWon && lastRestartBtn != null && lastRestartBtn.contains(p)) {
                    onNextLevel();
                } else if (!isScreenGameOverWin && !levelWon && lastRestartBtn != null && lastRestartBtn.contains(p)) {
                    onRestart();
                } else if (lastMenuBtn != null && lastMenuBtn.contains(p)) {
                    onBackToMenu();
                }
            } else if (overlayMode == 3) {
                if (lastSettingsBtn != null && lastSettingsBtn.contains(p)) {
                    boolean oldState = ResourceManager.getInstance().isSoundEnabled();
                    boolean newState = !oldState;
                    ResourceManager.getInstance().setSoundEnabled(newState);
                    ScreenStart.isSoundOnStatic = newState;
                    repaint();
                    if (!oldState && newState) {
                        ResourceManager.getInstance().playBackToMenu();
                    }
                } else if (lastHelpBtn != null && lastHelpBtn.contains(p)) {
                    showHelp();
                } else if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    closeSettings();
                }
            
            } else if (overlayMode == 4) {
                if (lastQuitBtn != null && lastQuitBtn.contains(p)) {
                    hideHelp();
                }
            }
        }

        private void updateOverlayHover(Point p) {
            if (overlayMode != 0 && !animating) {
                boolean hoveringAny = false;
                if (overlayMode == 1) {
                    hoveringAny = (lastResumeBtn != null && lastResumeBtn.contains(p)) ||
                            (lastHelpBtn != null && lastHelpBtn.contains(p)) ||
                            (lastQuitBtn != null && lastQuitBtn.contains(p));
                } else if (overlayMode == 2) {
                    hoveringAny = ((levelWon || !isScreenGameOverWin) && lastRestartBtn != null && lastRestartBtn.contains(p)) ||
                            (lastMenuBtn != null && lastMenuBtn.contains(p));
                } else if (overlayMode == 3) {
                    hoveringAny = (lastSettingsBtn != null && lastSettingsBtn.contains(p)) ||
                            (lastHelpBtn != null && lastHelpBtn.contains(p)) ||
                            (lastQuitBtn != null && lastQuitBtn.contains(p));
                } else if (overlayMode == 4) {
                    hoveringAny = (lastQuitBtn != null && lastQuitBtn.contains(p));
                }
                setCursor(hoveringAny ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        }

        public void showOverlay(int mode, boolean win) {
            overlayMode = mode;
            isScreenGameOverWin = win;
            setVisible(true);
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void showSettings() {
            overlayMode = 3;
            isScreenGameOverWin = false;
            setVisible(true);
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void showHelp() {
            ResourceManager.getInstance().playBackToMenu();
            returnToMode = overlayMode;
            overlayMode = 4;
            isScreenGameOverWin = false;
            setVisible(true);
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void hideHelp() {
            ResourceManager.getInstance().playBackToMenu();
            overlayMode = returnToMode;
            animating = true;
            animStartTime = System.currentTimeMillis();
            if (!animTimer.isRunning()) animTimer.start();
            repaint();
        }

        public void hideOverlay() {
            animating = false;
            if (animTimer != null) animTimer.stop();
            overlayMode = 0;

            if (gameStarted) {
            } else {
                setVisible(false);
            }
            repaint();
        }

        public void closeSettings() {
            ResourceManager.getInstance().playBackToMenu();
            hideOverlay();
        }

        private int getCurrentOffsetY(int h) {
            if (!animating) return 0;
            long elapsed = System.currentTimeMillis() - animStartTime;
            double t = Math.min(1.0, (double) elapsed / ANIM_DURATION);
            if (t < 0.7) {
                t = t / 0.7;
                t = 1 - Math.pow(1 - t, 2);
                return (int)(-h * (1 - t));
            } else {
                t = (t - 0.7) / 0.3;
                t = 1 - Math.pow(1 - t, 3);
                return (int)(-h * ANIM_OVERSHOOT * (1 - t));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            if (gameStarted) {
                if (utBg) {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, LEFT_ZONE_WIDTH, h);
                    if (utFont) {
                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(3f));
                        g2d.drawLine(LEFT_ZONE_WIDTH - 2, 0, LEFT_ZONE_WIDTH - 2, h);
                    }
                } else {
                    LinearGradientPaint leftBg = new LinearGradientPaint(
                            0, 0, LEFT_ZONE_WIDTH, 0,
                            new float[]{0f, 1f},
                            new Color[]{new Color(188, 195, 255), new Color(188, 195, 255)}
                    );
                    g2d.setPaint(leftBg);
                    g2d.fillRect(0, 0, LEFT_ZONE_WIDTH, h);

                    g2d.setColor(new Color(180, 205, 235));
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawLine(LEFT_ZONE_WIDTH - 2, 0, LEFT_ZONE_WIDTH - 2, h);
                }

                if (launchPad != null) {
                    launchPad.drawScoreBoard(g2d, LEFT_ZONE_WIDTH, h);
                }

                int btnW = 180;
                int btnH = 55;
                int btnX = (LEFT_ZONE_WIDTH - btnW) / 2;
                int btnY = h - btnH - 30;
                pauseButtonRect = new Rectangle(btnX, btnY, btnW, btnH);
                RoundRectangle2D btnShape = new RoundRectangle2D.Double(btnX, btnY, btnW, btnH, 18, 18);

                if (utFont) {
                    g2d.setColor(Color.BLACK);
                    g2d.fill(btnShape);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(4f));
                    g2d.draw(btnShape);
                    
                    g2d.setFont(new Font("Monospaced", Font.BOLD, 22));
                    String btnText = "PAUSE";
                    FontMetrics fm = g2d.getFontMetrics();
                    int tx = btnX + (btnW - fm.stringWidth(btnText)) / 2;
                    int ty = btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(btnText, tx, ty);
                } else {
                    if (pausePressed) {
                        g2d.setPaint(new LinearGradientPaint(btnX, btnY, btnX, btnY + btnH, new float[]{0, 1},
                                new Color[]{new Color(50, 130, 240, 230), new Color(30, 100, 220, 230)}));
                    } else if (pauseHover) {
                        g2d.setPaint(new LinearGradientPaint(btnX, btnY, btnX, btnY + btnH, new float[]{0, 1},
                                new Color[]{new Color(100, 180, 255, 230), new Color(50, 130, 240, 230)}));
                    } else {
                        g2d.setPaint(new LinearGradientPaint(btnX, btnY, btnX, btnY + btnH, new float[]{0, 1},
                                new Color[]{new Color(120, 190, 255, 180), new Color(70, 140, 240, 180)}));
                    }
                    g2d.fill(btnShape);

                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.draw(btnShape);

                    g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
                    String btnText = "PAUSE";
                    FontMetrics fm = g2d.getFontMetrics();
                    int tx = btnX + (btnW - fm.stringWidth(btnText)) / 2;
                    int ty = btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(btnText, tx, ty);
                }
                
                if (sansActive && sans != null) {
                    if (sansAnimating) {
                        sans.draw(g2d, (int) sansX, (int) sansY, 1.2);
                    } else if (sansIdle) {
                        sans.draw(g2d, (int) sansX, (int) sansY, 1.2);
                        sans.drawHearts(g2d);
                    }
                    
                    sans.drawDialog(g2d, (int)sansX, (int)sansY);
                    sans.drawCombatDialog(g2d, (int)sansX, (int)sansY);
                }

            }

            if (overlayMode != 0) {
                drawOverlayContent(g2d, w, h);
            }
        }

        private void drawOverlayContent(Graphics2D g2d, int w, int h) {
            g2d.setColor(new Color(0, 0, 0, 190));
            g2d.fillRect(0, 0, w, h);

            int offsetY = getCurrentOffsetY(h);
            g2d.translate(0, offsetY);

            int centerX = w / 2;
            int centerY = h / 2;

            if (overlayMode == 1) {
                drawPauseMenuOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 2) {
                drawScreenGameOverOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 3) {
                drawSettingsOverlay(g2d, centerX, centerY);
            } else if (overlayMode == 4) {
                drawHelpOverlay(g2d, centerX, centerY);
            }

            g2d.translate(0, -offsetY);
        }

        private void drawUtButton(Graphics2D g2d, Rectangle rect, String text) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4f));
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
            
            g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
            int textY = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        }

        private void drawPauseMenuOverlay(Graphics2D g2d, int cx, int cy) {
            if (Main.utFont) {
                g2d.setFont(new Font("Monospaced", Font.BOLD, 42));
                g2d.setColor(Color.WHITE);
                String title = "PAUSED";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 120);

                int btnWidth = 220;
                int btnHeight = 55;
                int btnSpacing = 20;
                int startY = cy - 40;

                Rectangle resumeBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawUtButton(g2d, resumeBtn, "Resume");

                Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
                drawUtButton(g2d, helpBtn, "How to play");

                Rectangle quitBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
                drawUtButton(g2d, quitBtn, "Quit Game");

                lastResumeBtn = resumeBtn;
                lastHelpBtn = helpBtn;
                lastQuitBtn = quitBtn;
                return;
            }

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 42));
            g2d.setColor(new Color(70, 150, 255));
            String title = "PAUSED";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 120);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy - 40;

            Rectangle resumeBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
            drawOverlayButton(g2d, resumeBtn, "Resume", new Color(70, 150, 255));

            Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
            drawOverlayButton(g2d, helpBtn, "How to play", new Color(100, 190, 255));

            Rectangle quitBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, quitBtn, "Quit Game", new Color(220, 70, 70));

            lastResumeBtn = resumeBtn;
            lastHelpBtn = helpBtn;
            lastQuitBtn = quitBtn;
        }

        private void drawScreenGameOverOverlay(Graphics2D g2d, int cx, int cy) {
            if (Main.utFont) {
                g2d.setFont(new Font("Monospaced", Font.BOLD, 42));
                g2d.setColor(Color.WHITE);
                String title = (levelWon || isScreenGameOverWin) ? "DETERMINATION" : "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);

                g2d.setFont(new Font("Monospaced", Font.BOLD, 26));
                String scoreText = "SCORE: " + currentScore;
                fm = g2d.getFontMetrics();
                g2d.drawString(scoreText, cx - fm.stringWidth(scoreText) / 2, cy - 70);

                String targetText = "TARGET: " + levelWinScore;
                fm = g2d.getFontMetrics();
                g2d.drawString(targetText, cx - fm.stringWidth(targetText) / 2, cy - 35);

                int btnWidth = 220;
                int btnHeight = 55;
                int btnSpacing = 20;
                int startY = cy + 10;

                if (levelWon) {
                    Rectangle nextLevelBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                    drawUtButton(g2d, nextLevelBtn, "Next Level");
                    lastRestartBtn = nextLevelBtn;
                } else if (!isScreenGameOverWin) {
                    Rectangle restartBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                    drawUtButton(g2d, restartBtn, "Restart");
                    lastRestartBtn = restartBtn;
                }

                int menuOffset = (levelWon || isScreenGameOverWin) ? (levelWon ? btnHeight + btnSpacing : 0) : btnHeight + btnSpacing;
                Rectangle menuBtn = new Rectangle(cx - btnWidth / 2, startY + menuOffset, btnWidth, btnHeight);
                drawUtButton(g2d, menuBtn, "Main Menu");
                lastMenuBtn = menuBtn;
                return;
            }

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 42));
            if (levelWon) {
                g2d.setColor(new Color(255, 215, 0));
                String title = "LEVEL CLEAR!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);
            } else if (isScreenGameOverWin) {
                g2d.setColor(new Color(255, 215, 0));
                String title = "VICTORY!";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);
            } else {
                g2d.setColor(new Color(255, 80, 80));
                String title = "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 130);
            }

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 26));
            g2d.setColor(Color.WHITE);
            String scoreText = "Score: " + currentScore;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(scoreText, cx - fm.stringWidth(scoreText) / 2, cy - 70);

            String targetText = "Target: " + levelWinScore;
            fm = g2d.getFontMetrics();
            g2d.drawString(targetText, cx - fm.stringWidth(targetText) / 2, cy - 35);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy + 10;

            if (levelWon) {
                Rectangle nextLevelBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawOverlayButton(g2d, nextLevelBtn, "Next Level", new Color(70, 200, 100));
                lastRestartBtn = nextLevelBtn;
            } else if (!isScreenGameOverWin) {
                Rectangle restartBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                drawOverlayButton(g2d, restartBtn, "Restart", new Color(70, 150, 255));
                lastRestartBtn = restartBtn;
            }

            int menuOffset = (levelWon || isScreenGameOverWin) ? (levelWon ? btnHeight + btnSpacing : 0) : btnHeight + btnSpacing;
            Rectangle menuBtn = new Rectangle(cx - btnWidth / 2, startY + menuOffset, btnWidth, btnHeight);
            drawOverlayButton(g2d, menuBtn, "Main Menu", new Color(100, 190, 255));
            lastMenuBtn = menuBtn;
        }

        private void drawSettingsOverlay(Graphics2D g2d, int cx, int cy) {
            if (Main.utFont) {
                g2d.setFont(new Font("Monospaced", Font.BOLD, 36));
                g2d.setColor(Color.WHITE);
                String title = "SETTINGS";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

                int btnWidth = 220;
                int btnHeight = 55;
                int btnSpacing = 20;
                int startY = cy - 20;

                Rectangle soundBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
                String soundText = ResourceManager.getInstance().isSoundEnabled() ? "Sound: ON" : "Sound: OFF";
                drawUtButton(g2d, soundBtn, soundText);
                lastSettingsBtn = soundBtn;

                Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
                drawUtButton(g2d, helpBtn, "How to play");
                lastHelpBtn = helpBtn;

                Rectangle backBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
                drawUtButton(g2d, backBtn, "Back");
                lastQuitBtn = backBtn;
                return;
            }

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 36));
            g2d.setColor(new Color(70, 150, 255));
            String title = "SETTINGS";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

            int btnWidth = 220;
            int btnHeight = 55;
            int btnSpacing = 20;
            int startY = cy - 20;

            Rectangle soundBtn = new Rectangle(cx - btnWidth / 2, startY, btnWidth, btnHeight);
            String soundText = ResourceManager.getInstance().isSoundEnabled() ? "Sound: ON" : "Sound: OFF";
            drawOverlayButton(g2d, soundBtn, soundText, new Color(70, 150, 255));
            lastSettingsBtn = soundBtn;

            Rectangle helpBtn = new Rectangle(cx - btnWidth / 2, startY + btnHeight + btnSpacing, btnWidth, btnHeight);
            drawOverlayButton(g2d, helpBtn, "How to play", new Color(100, 190, 255));
            lastHelpBtn = helpBtn;

            Rectangle backBtn = new Rectangle(cx - btnWidth / 2, startY + 2 * (btnHeight + btnSpacing), btnWidth, btnHeight);
            drawOverlayButton(g2d, backBtn, "Back", new Color(220, 70, 70));
            lastQuitBtn = backBtn;
        }

        private void drawHelpOverlay(Graphics2D g2d, int cx, int cy) {
            String[] lines = {
                    "1. Move mouse to aim, click or press SPACE to launch marble",
                    "2. 3 or more same-color connected marbles will be eliminated",
                    "3. Do not let marbles cross the bottom dashed line!"
            };

            if (Main.utFont) {
                g2d.setFont(new Font("Monospaced", Font.BOLD, 36));
                g2d.setColor(Color.WHITE);
                String title = "HOW TO PLAY";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

                g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
                int lineHeight = 36;
                int startY = cy - 20;
                for (int i = 0; i < lines.length; i++) {
                    fm = g2d.getFontMetrics();
                    g2d.drawString(lines[i], cx - fm.stringWidth(lines[i]) / 2, startY + i * lineHeight);
                }

                int btnWidth = 220;
                int btnHeight = 55;
                int btnY = startY + lines.length * lineHeight + 30;
                Rectangle backBtn = new Rectangle(cx - btnWidth / 2, btnY, btnWidth, btnHeight);
                drawUtButton(g2d, backBtn, "Back");
                lastQuitBtn = backBtn;
                return;
            }

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 36));
            g2d.setColor(new Color(70, 150, 255));
            String title = "HOW TO PLAY";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(title, cx - fm.stringWidth(title) / 2, cy - 100);

            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 20));
            g2d.setColor(Color.WHITE);
            int lineHeight = 36;
            int startY = cy - 20;
            for (int i = 0; i < lines.length; i++) {
                fm = g2d.getFontMetrics();
                g2d.drawString(lines[i], cx - fm.stringWidth(lines[i]) / 2, startY + i * lineHeight);
            }

            int btnWidth = 220;
            int btnHeight = 55;
            int btnY = startY + lines.length * lineHeight + 30;
            Rectangle backBtn = new Rectangle(cx - btnWidth / 2, btnY, btnWidth, btnHeight);
            drawOverlayButton(g2d, backBtn, "Back", new Color(220, 70, 70));
            lastQuitBtn = backBtn;
        }

        private void drawOverlayButton(Graphics2D g2d, Rectangle rect, String text, Color baseColor) {
            LinearGradientPaint grad = new LinearGradientPaint(
                    rect.x, rect.y, rect.x, rect.y + rect.height,
                    new float[]{0, 1},
                    new Color[]{baseColor, baseColor.darker()}
            );
            g2d.setPaint(grad);
            g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 18, 18);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
            int textY = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        }

        private Rectangle lastResumeBtn = null;
        private Rectangle lastHelpBtn = null;
        private Rectangle lastQuitBtn = null;
        private Rectangle lastRestartBtn = null;
        private Rectangle lastMenuBtn = null;
        private Rectangle lastSettingsBtn = null;

        @Override
        public boolean contains(int x, int y) {
            if (animating) return true;

            if (sans != null && sans.isDialogActive()) {
                return true;
            }

            if (overlayMode == 0) {
                return gameStarted && pauseButtonRect != null && pauseButtonRect.contains(x, y);
            }

            return true;
        }

        public void updateScores(int score, int high, int levelHigh, int levelWin) {
            if (launchPad != null) {
                launchPad.updateScore(score);
                launchPad.updateHighScore(high);
                launchPad.updateLevelScores(score, levelHigh, levelWin);
                mPanel.repaint();
            }
            repaint();
        }
    }
}