import java.util.prefs.Preferences;

public class Level {
    private static final String PREF_KEY_CURRENT_LEVEL = "currentLevel";
    private static final String PREF_PREFIX_HIGH_SCORE = "levelHighScore_";

    private int currentLevel;
    private int[] levelHighScores;

    public static final int MAX_LEVEL = 4;

    private static Level instance;

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

    // ========== 关卡配置模块 ==========

    /** 第1关配置 */
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

    /** 第2关配置 */
    public static class Level2 {
        public static final int WIN_SCORE = 1000;
        public static final int COLOR_TYPE_COUNT = 4;
        public static final double FALL_SPEED_MULT = 1.0;
        public static final double BASE_FALL_SPEED = 3.0;
        public static final double MAX_FALL_SPEED = 15.0;
        public static final double SPEED_INCREASE_RATE = 0.1;
        public static final boolean HAS_BOSS_SANS = false;
        public static final boolean HAS_CREEPER = true;   // creeper出现
        public static final boolean HAS_BEDROCK = false;
        public static final boolean HAS_HEART = false;
    }

    /** 第3关配置 */
    public static class Level3 {
        public static final int WIN_SCORE = 1500;
        public static final int COLOR_TYPE_COUNT = 4;     // 限制为4以避免自然生成creeper
        public static final double FALL_SPEED_MULT = 1.0;
        public static final double BASE_FALL_SPEED = 3.0;
        public static final double MAX_FALL_SPEED = 15.0;
        public static final double SPEED_INCREASE_RATE = 0.1;
        public static final boolean HAS_BOSS_SANS = false;
        public static final boolean HAS_CREEPER = true;   // creeper出现
        public static final boolean HAS_BEDROCK = true;   // bedrock出现
        public static final boolean HAS_HEART = false;
    }

    /** 第4关配置 */
    public static class Level4 {
        public static final int WIN_SCORE = 2000;
        public static final int COLOR_TYPE_COUNT = 4; 
        public static final double FALL_SPEED_MULT = 1.0;
        // 匹配 150 BPM 节拍恒定底速 (四分音符) 的物理呈现
        public static final double BASE_FALL_SPEED = 15.0; 
        public static final double MAX_FALL_SPEED = 150.0;
        // 第4关不再加速，完全按乐理节拍硬核恒速下落
        public static final double SPEED_INCREASE_RATE = 0.0;
        public static final boolean HAS_BOSS_SANS = true;
        public static final boolean HAS_CREEPER = false;  // 第4关不自然生成creeper（由BossSans触发）
        public static final boolean HAS_BEDROCK = false;  // 第4关不自然生成bedrock
        public static final boolean HAS_HEART = true;      // heart出现
    }

    // ========== 根据关卡号获取对应配置 ==========

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

    // ========== 对外接口 ==========

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getUnlockedLevelCount() {
        int unlocked = 1;
        for (int i = 1; i <= currentLevel; i++) {
            if (levelHighScores[i - 1] > 0) {
                unlocked = i + 1;
            }
        }
        return unlocked;
    }

    public boolean isLevelUnlocked(int level) {
        if (level == 1) return true;
        return level <= getUnlockedLevelCount();
    }

    public int getWinScore() {
        return (int) getLevelConfig(currentLevel)[0];
    }

    public int getColorTypeCount() {
        return (int) getLevelConfig(currentLevel)[1];
    }

    public double getFallSpeedMultiplier() {
        return (double) getLevelConfig(currentLevel)[2];
    }

    public double getBaseFallSpeed() {
        return (double) getLevelConfig(currentLevel)[3];
    }

    public double getMaxFallSpeed() {
        return (double) getLevelConfig(currentLevel)[4];
    }

    public double getSpeedIncreaseRate() {
        return (double) getLevelConfig(currentLevel)[5];
    }

    public boolean hasBossSans() {
        return (boolean) getLevelConfig(currentLevel)[6];
    }

    public boolean hasBossSans(int level) {
        return (boolean) getLevelConfig(level)[6];
    }

    public boolean hasCreeper() {
        return (boolean) getLevelConfig(currentLevel)[7];
    }

    public boolean hasCreeper(int level) {
        return (boolean) getLevelConfig(level)[7];
    }

    public boolean hasBedrock() {
        return (boolean) getLevelConfig(currentLevel)[8];
    }

    public boolean hasBedrock(int level) {
        return (boolean) getLevelConfig(level)[8];
    }

    public boolean hasHeart() {
        return (boolean) getLevelConfig(currentLevel)[9];
    }

    public boolean hasHeart(int level) {
        return (boolean) getLevelConfig(level)[9];
    }

    public int getLevelHighScore() {
        return levelHighScores[currentLevel - 1];
    }

    public void updateLevelHighScore(int score) {
        if (score > levelHighScores[currentLevel - 1]) {
            levelHighScores[currentLevel - 1] = score;
            saveLevelHighScore(currentLevel, score);
        }
    }

    public boolean isWinConditionMet(int score) {
        return score >= getWinScore();
    }

    public void nextLevel() {
        if (currentLevel < MAX_LEVEL) {
            currentLevel++;
            saveCurrentLevel(currentLevel);
        }
    }

    public void resetToLevel(int level) {
        currentLevel = level;
        saveCurrentLevel(currentLevel);
    }

    public void setCurrentLevel(int level) {
        if (level >= 1 && level <= MAX_LEVEL) {
            currentLevel = level;
            saveCurrentLevel(currentLevel);
        }
    }

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
            // Ignore
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
            // Ignore
        }
    }
}