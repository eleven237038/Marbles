import java.util.prefs.Preferences;

public class Level {
    private static final String PREF_KEY_CURRENT_LEVEL = "currentLevel";
    private static final String PREF_PREFIX_HIGH_SCORE = "levelHighScore_";
    private static final int BASE_WIN_SCORE = 500;
    private static final int SCORE_INCREMENT = 500;

    private int currentLevel;
    private int[] levelHighScores;

    // Level difficulty parameters
    private static final int BASE_COLOR_TYPE_COUNT = 4;
    private static final int COLOR_TYPE_INCREMENT = 1;
    private static final int MAX_COLOR_TYPES = 6;

    private static final double BASE_FALL_SPEED_MULT = 1.0;
    private static final double FALL_SPEED_INCREMENT = 0.3;
    private static final double MAX_FALL_SPEED_MULT = 2.5;

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

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getUnlockedLevelCount() {
        int unlocked = 1;
        for (int i = 1; i < currentLevel; i++) {
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
        return BASE_WIN_SCORE + (currentLevel - 1) * SCORE_INCREMENT;
    }

    public int getColorTypeCount() {
        int count = BASE_COLOR_TYPE_COUNT + (currentLevel - 1) / 2;
        return Math.min(count, MAX_COLOR_TYPES);
    }

    public double getFallSpeedMultiplier() {
        double mult = BASE_FALL_SPEED_MULT + (currentLevel - 1) * FALL_SPEED_INCREMENT;
        return Math.min(mult, MAX_FALL_SPEED_MULT);
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