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

import java.util.prefs.Preferences;

/**
 * Level - 关卡管理类，管理游戏关卡配置和进度
 * Level - Level management class, managing game level configuration and progress
 */
public class Level {
    private static final String PREF_KEY_CURRENT_LEVEL = "currentLevel";
    private static final String PREF_PREFIX_HIGH_SCORE = "levelHighScore_";

    private int currentLevel;
    private int[] levelHighScores;

    public static final int MAX_LEVEL = 4;

    private static Level instance;

    /**
     * 获取单例实例
     * Get singleton instance
     */
    public static Level getInstance() {
        if (instance == null) {
            instance = new Level();
        }
        return instance;
    }

    private Level() {
        currentLevel = loadCurrentLevel();
        levelHighScores = new int[MAX_LEVEL];
        for (int i = 0; i < levelHighScores.length; i++) {
            levelHighScores[i] = loadLevelHighScore(i + 1);
        }
    }

    // ========== 关卡配置模块 / Level Configuration Module ==========

    /**
     * 第1关配置 / Level 1 Configuration
     */
    public static class Level1 {
        public static final int WIN_SCORE = 500;
        public static final int COLOR_TYPE_COUNT = 4;
        public static final double FALL_SPEED_MULT = 1.0;
        public static final double BASE_FALL_SPEED = 3.0;
        public static final double MAX_FALL_SPEED = 15.0;
        public static final double SPEED_INCREASE_RATE = 0.1;
        public static final boolean HAS_BOSS_SANS = false;
        public static final boolean HAS_CREEPER = false;
        public static final boolean HAS_BEDROCK = false;
        public static final boolean HAS_HEART = false;
    }

    /**
     * 第2关配置 / Level 2 Configuration
     */
    public static class Level2 {
        public static final int WIN_SCORE = 1000;
        public static final int COLOR_TYPE_COUNT = 4;
        public static final double FALL_SPEED_MULT = 1.0;
        public static final double BASE_FALL_SPEED = 3.0;
        public static final double MAX_FALL_SPEED = 15.0;
        public static final double SPEED_INCREASE_RATE = 0.1;
        public static final boolean HAS_BOSS_SANS = false;
        public static final boolean HAS_CREEPER = true;   // Creeper appears / Creeper出现
        public static final boolean HAS_BEDROCK = false;
        public static final boolean HAS_HEART = false;
    }

    /**
     * 第3关配置 / Level 3 Configuration
     */
    public static class Level3 {
        public static final int WIN_SCORE = 1500;
        public static final int COLOR_TYPE_COUNT = 4;     // Limited to 4 to avoid natural creeper spawning / 限制为4以避免自然生成creeper
        public static final double FALL_SPEED_MULT = 1.0;
        public static final double BASE_FALL_SPEED = 3.0;
        public static final double MAX_FALL_SPEED = 15.0;
        public static final double SPEED_INCREASE_RATE = 0.1;
        public static final boolean HAS_BOSS_SANS = false;
        public static final boolean HAS_CREEPER = true;   // Creeper appears / Creeper出现
        public static final boolean HAS_BEDROCK = true;   // Bedrock appears / Bedrock出现
        public static final boolean HAS_HEART = false;
    }

    /**
     * 第4关配置 / Level 4 Configuration
     */
    public static class Level4 {
        public static final int WIN_SCORE = 2000;
        public static final int COLOR_TYPE_COUNT = 4;
        public static final double FALL_SPEED_MULT = 1.0;
        // Matches 150 BPM beat constant base speed (quarter note) physics presentation / 匹配150BPM节拍恒定底速的物理呈现
        public static final double BASE_FALL_SPEED = 7.5;
        public static final double MAX_FALL_SPEED = 75.0;
        // Level 4 no longer accelerates, strictly following music beat constant speed / 第4关不再加速，完全按乐理节拍恒速下落
        public static final double SPEED_INCREASE_RATE = 0.0;
        public static final boolean HAS_BOSS_SANS = true;
        public static final boolean HAS_CREEPER = false;  // Level 4 no natural creeper (triggered by BossSans) / 第4关不自然生成creeper（由BossSans触发）
        public static final boolean HAS_BEDROCK = false;  // Level 4 no natural bedrock / 第4关不自然生成bedrock
        public static final boolean HAS_HEART = true;      // Heart appears / Heart出现
    }

    // ========== 根据关卡号获取对应配置 / Get configuration by level number ==========

    private Object[] getLevelConfig(int level) {
        switch (level) {
            case 1: return new Object[]{Level1.WIN_SCORE, Level1.COLOR_TYPE_COUNT, Level1.FALL_SPEED_MULT,
                                         Level1.BASE_FALL_SPEED, Level1.MAX_FALL_SPEED, Level1.SPEED_INCREASE_RATE,
                                         Level1.HAS_BOSS_SANS, Level1.HAS_CREEPER, Level1.HAS_BEDROCK, Level1.HAS_HEART};
            case 2: return new Object[]{Level2.WIN_SCORE, Level2.COLOR_TYPE_COUNT, Level2.FALL_SPEED_MULT,
                                         Level2.BASE_FALL_SPEED, Level2.MAX_FALL_SPEED, Level2.SPEED_INCREASE_RATE,
                                         Level2.HAS_BOSS_SANS, Level2.HAS_CREEPER, Level2.HAS_BEDROCK, Level2.HAS_HEART};
            case 3: return new Object[]{Level3.WIN_SCORE, Level3.COLOR_TYPE_COUNT, Level3.FALL_SPEED_MULT,
                                         Level3.BASE_FALL_SPEED, Level3.MAX_FALL_SPEED, Level3.SPEED_INCREASE_RATE,
                                         Level3.HAS_BOSS_SANS, Level3.HAS_CREEPER, Level3.HAS_BEDROCK, Level3.HAS_HEART};
            case 4: return new Object[]{Level4.WIN_SCORE, Level4.COLOR_TYPE_COUNT, Level4.FALL_SPEED_MULT,
                                         Level4.BASE_FALL_SPEED, Level4.MAX_FALL_SPEED, Level4.SPEED_INCREASE_RATE,
                                         Level4.HAS_BOSS_SANS, Level4.HAS_CREEPER, Level4.HAS_BEDROCK, Level4.HAS_HEART};
            default: return new Object[]{Level1.WIN_SCORE, Level1.COLOR_TYPE_COUNT, Level1.FALL_SPEED_MULT,
                                         Level1.BASE_FALL_SPEED, Level1.MAX_FALL_SPEED, Level1.SPEED_INCREASE_RATE,
                                         Level1.HAS_BOSS_SANS, Level1.HAS_CREEPER, Level1.HAS_BEDROCK, Level1.HAS_HEART};
        }
    }

    // ========== 对外接口 / Public Interface ==========

    /**
     * 获取当前关卡
     * Get current level
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 获取已解锁关卡数量
     * Get unlocked level count
     */
    public int getUnlockedLevelCount() {
        return MAX_LEVEL;
    }

    /**
     * 检查关卡是否解锁
     * Check if level is unlocked
     */
    public boolean isLevelUnlocked(int level) {
        return level >= 1 && level <= MAX_LEVEL;
    }

    /**
     * 获取获胜目标分数
     * Get win target score
     */
    public int getWinScore() {
        return (int) getLevelConfig(currentLevel)[0];
    }

    /**
     * 获取颜色类型数量
     * Get color type count
     */
    public int getColorTypeCount() {
        return (int) getLevelConfig(currentLevel)[1];
    }

    /**
     * 获取下落速度倍率
     * Get fall speed multiplier
     */
    public double getFallSpeedMultiplier() {
        return (double) getLevelConfig(currentLevel)[2];
    }

    /**
     * 获取基础下落速度
     * Get base fall speed
     */
    public double getBaseFallSpeed() {
        return (double) getLevelConfig(currentLevel)[3];
    }

    /**
     * 获取最大下落速度
     * Get max fall speed
     */
    public double getMaxFallSpeed() {
        return (double) getLevelConfig(currentLevel)[4];
    }

    /**
     * 获取速度增加率
     * Get speed increase rate
     */
    public double getSpeedIncreaseRate() {
        return (double) getLevelConfig(currentLevel)[5];
    }

    /**
     * 检查是否有BossSans
     * Check if has BossSans
     */
    public boolean hasBossSans() {
        return (boolean) getLevelConfig(currentLevel)[6];
    }

    /**
     * 检查指定关卡是否有BossSans
     * Check if specific level has BossSans
     */
    public boolean hasBossSans(int level) {
        return (boolean) getLevelConfig(level)[6];
    }

    /**
     * 检查是否有Creeper
     * Check if has Creeper
     */
    public boolean hasCreeper() {
        return (boolean) getLevelConfig(currentLevel)[7];
    }

    /**
     * 检查指定关卡是否有Creeper
     * Check if specific level has Creeper
     */
    public boolean hasCreeper(int level) {
        return (boolean) getLevelConfig(level)[7];
    }

    /**
     * 检查是否有Bedrock
     * Check if has Bedrock
     */
    public boolean hasBedrock() {
        return (boolean) getLevelConfig(currentLevel)[8];
    }

    /**
     * 检查指定关卡是否有Bedrock
     * Check if specific level has Bedrock
     */
    public boolean hasBedrock(int level) {
        return (boolean) getLevelConfig(level)[8];
    }

    /**
     * 检查是否有Heart
     * Check if has Heart
     */
    public boolean hasHeart() {
        return (boolean) getLevelConfig(currentLevel)[9];
    }

    /**
     * 检查指定关卡是否有Heart
     * Check if specific level has Heart
     */
    public boolean hasHeart(int level) {
        return (boolean) getLevelConfig(level)[9];
    }

    /**
     * 获取关卡最高分
     * Get level high score
     */
    public int getLevelHighScore() {
        return levelHighScores[currentLevel - 1];
    }

    /**
     * 更新关卡最高分
     * Update level high score
     */
    public void updateLevelHighScore(int score) {
        if (score > levelHighScores[currentLevel - 1]) {
            levelHighScores[currentLevel - 1] = score;
            saveLevelHighScore(currentLevel, score);
        }
    }

    /**
     * 检查是否满足获胜条件
     * Check if win condition is met
     */
    public boolean isWinConditionMet(int score) {
        return score >= getWinScore();
    }

    /**
     * 进入下一关
     * Go to next level
     */
    public void nextLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
            saveCurrentLevel(currentLevel);
        }
    }

    /**
     * 重置到指定关卡
     * Reset to specified level
     */
    public void resetToLevel(int level) {
        currentLevel = level;
        saveCurrentLevel(currentLevel);
    }

    /**
     * 设置当前关卡
     * Set current level
     */
    public void setCurrentLevel(int level) {
        if (level >= 1 && level <= MAX_LEVEL) {
            currentLevel = level;
            saveCurrentLevel(currentLevel);
        }
    }

    /**
     * 重置所有进度
     * Reset all progress
     */
    public void resetAllProgress() {
        currentLevel = 1;
        saveCurrentLevel(currentLevel);
        for (int i = 0; i < levelHighScores.length; i++) {
            levelHighScores[i] = 0;
            saveLevelHighScore(i + 1, 0);
        }
    }

    private int loadCurrentLevel() {
        try {
            return Preferences.userNodeForPackage(Level.class).getInt(PREF_KEY_CURRENT_LEVEL, 1);
        } catch (Exception e) {
            return 1;
        }
    }

    private void saveCurrentLevel(int level) {
        try {
            Preferences.userNodeForPackage(Level.class).putInt(PREF_KEY_CURRENT_LEVEL, level);
        } catch (Exception e) {
            // Ignore / 忽略
        }
    }

    private int loadLevelHighScore(int level) {
        try {
            return Preferences.userNodeForPackage(Level.class).getInt(PREF_PREFIX_HIGH_SCORE + level, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveLevelHighScore(int level, int score) {
        try {
            Preferences.userNodeForPackage(Level.class).putInt(PREF_PREFIX_HIGH_SCORE + level, score);
        } catch (Exception e) {
            // Ignore / 忽略
        }
    }
}