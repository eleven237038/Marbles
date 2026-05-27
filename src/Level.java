import java.util.prefs.Preferences;

public class Level {
    private static final String PREF_KEY_CURRENT_LEVEL = "currentLevel";
    private static final String PREF_PREFIX_HIGH_SCORE = "levelHighScore_";
    private static final int BASE_WIN_SCORE = 500;
    private static final int SCORE_INCREMENT = 500;

    private int currentLevel;
    private int[] levelHighScores;

    private static Level instance;

    public static Level getInstance() {
        if (instance == null) {
            instance = new Level();
        }
        return instance;
    }

    private Level() {
        currentLevel = loadCurrentLevel();
        levelHighScores = new int[100];
        for (int i = 0; i < levelHighScores.length; i++) {
            levelHighScores[i] = loadLevelHighScore(i + 1);
        }
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getWinScore() {
        return BASE_WIN_SCORE + (currentLevel - 1) * SCORE_INCREMENT;
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
        currentLevel++;
        saveCurrentLevel(currentLevel);
    }

    public void resetToLevel(int level) {
        currentLevel = level;
        saveCurrentLevel(currentLevel);
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