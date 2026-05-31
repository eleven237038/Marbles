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

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import javax.sound.sampled.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ResourceManager - 统一管理资源路径和音效
 * ResourceManager - Unified resource path and sound effect management
 * 合并了ResourceUtil和SoundManager的功能
 * Combines functionality of ResourceUtil and SoundManager
 */
public class ResourceManager {
    private static ResourceManager instance;

    // ========== 资源路径管理 / Resource Path Management ==========
    private static String resourceBasePath;

    // ========== 音效管理 / Sound Effect Management ==========
    private Map<String, Clip> soundClips;
    private Map<String, Boolean> soundLoaded;
    private boolean soundEnabled = true;

    // 音乐管理 / Music management
    private Clip musicClip = null;
    private String currentMusic = null;

    // 音效文件路径常量 / Sound effect file path constants
    public static final String GAME_BEGIN = "GameBegin.wav";
    public static final String BACK_TO_MENU = "从菜单返回主页.wav";
    public static final String BACK_TO_GAME = "从菜单返回游戏.wav";
    public static final String DROP_AND_SCORE = "掉落并加分.wav";
    public static final String FOUR_CLEAR = "四消.wav";
    public static final String MASSIVE_CLEAR = "大量消除1.wav";
    public static final String NO_CLEAR = "打击后黏附无消除.wav";
    public static final String GAME_FAIL = "晋级&失败.wav";
    public static final String THREE_CLEAR = "钝角三消.wav";

    // 音乐文件路径常量 / Music file path constants
    public static final String MUSIC_BONELESS = "骨质疏松.wav";
    public static final String MUSIC_JUSTICE = "正义之矛.wav";

    private ResourceManager() {
        soundClips = new HashMap<>();
        soundLoaded = new HashMap<>();
        preloadAllSounds();
    }

    /**
     * 获取单例实例 / Get singleton instance
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }

    // ========== 资源路径方法 / Resource Path Methods ==========

    /**
     * 获取资源文件夹的基础路径
     * Get base path of resources folder
     */
    public static String getResourceBasePath() {
        if (resourceBasePath != null) {
            return resourceBasePath;
        }

        try {
            CodeSource codeSource = ResourceManager.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File classLocation = new File(codeSource.getLocation().toURI());
                if (classLocation.isFile()) {
                    resourceBasePath = classLocation.getParentFile().getParentFile().getParent() + File.separator;
                } else {
                    resourceBasePath = classLocation.getParentFile().getParentFile().getParent() + File.separator;
                }
            }
        } catch (URISyntaxException e) {
            // 回退到当前工作目录 / Fallback to current working directory
        }

        if (resourceBasePath == null) {
            resourceBasePath = System.getProperty("user.dir") + File.separator;
        }

        // 验证resources文件夹存在 / Verify resources folder exists
        File resourcesDir = new File(resourceBasePath + "resources");
        if (!resourcesDir.exists() || !resourcesDir.isDirectory()) {
            File parent = new File(resourceBasePath).getParentFile();
            if (parent != null && new File(parent, "resources").exists()) {
                resourceBasePath = parent.getAbsolutePath() + File.separator;
            }
        }

        return resourceBasePath;
    }

    /**
     * 构建资源文件的完整路径
     * Build complete path for resource file
     */
    public static String getResourcePath(String relativePath) {
        return getResourceBasePath() + "resources" + File.separator + relativePath;
    }

    /**
     * 构建音效文件的完整路径
     * Build complete path for sound file
     */
    public static String getSoundPath(String fileName) {
        return getResourceBasePath() + "resources" + File.separator + "sound" + File.separator + fileName;
    }

    /**
     * 构建图片资源的完整路径
     * Build complete path for image resource
     */
    public static String getImagePath(String fileName) {
        return getResourceBasePath() + "resources" + File.separator + "image" + File.separator + fileName;
    }

    // ========== 音效方法 / Sound Effect Methods ==========

    /**
     * 预加载所有音效 / Preload all sound effects
     */
    private void preloadAllSounds() {
        preloadSound(GAME_BEGIN);
        preloadSound(BACK_TO_MENU);
        preloadSound(BACK_TO_GAME);
        preloadSound(DROP_AND_SCORE);
        preloadSound(FOUR_CLEAR);
        preloadSound(MASSIVE_CLEAR);
        preloadSound(NO_CLEAR);
        preloadSound(GAME_FAIL);
        preloadSound(THREE_CLEAR);
    }

    /**
     * 预加载单个音效 / Preload single sound effect
     */
    private void preloadSound(String fileName) {
        try {
            File soundFile = new File(getSoundPath(fileName));
            if (soundFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                soundClips.put(fileName, clip);
                soundLoaded.put(fileName, true);
                System.out.println("预加载音效成功: " + fileName);
            } else {
                soundLoaded.put(fileName, false);
                System.err.println("音效文件不存在: " + soundFile.getAbsolutePath());
            }
        } catch (UnsupportedAudioFileException e) {
            System.out.println("不支持的音频格式: " + fileName);
            soundLoaded.put(fileName, false);
        } catch (LineUnavailableException e) {
            System.out.println("音频线路不可用: " + fileName);
            soundLoaded.put(fileName, false);
        } catch (IOException e) {
            System.out.println("读取音效文件失败: " + fileName);
            soundLoaded.put(fileName, false);
        }
    }

    /**
     * 播放音效 / Play sound effect
     */
    public void playSound(String fileName) {
        if (!soundEnabled) return;

        if (soundClips.containsKey(fileName) && soundClips.get(fileName) != null) {
            Clip clip = soundClips.get(fileName);
            clip.setFramePosition(0);
            clip.start();
        } else if (soundLoaded.containsKey(fileName) && !soundLoaded.get(fileName)) {
            try {
                File soundFile = new File(getSoundPath(fileName));
                if (soundFile.exists()) {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    soundClips.put(fileName, clip);
                    soundLoaded.put(fileName, true);
                    clip.start();
                    System.out.println("动态加载并播放音效: " + fileName);
                }
            } catch (Exception e) {
                System.out.println("播放音效失败: " + fileName);
            }
        }
    }

    /**
     * 设置音效启用状态 / Set sound enabled state
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * 停止所有音效 / Stop all sound effects
     */
    public void stopAllSounds() {
        for (Clip clip : soundClips.values()) {
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
        }
    }

    /**
     * 停止指定音效 / Stop specified sound effect
     */
    public void stopSound(String fileName) {
        if (soundClips.containsKey(fileName)) {
            Clip clip = soundClips.get(fileName);
            if (clip != null && clip.isRunning()) {
                clip.stop();
                clip.setFramePosition(0);
            }
        }
    }

    // 播放音效的便捷方法 / Convenient methods for playing sound effects
    public void playGameBegin() { playSound(GAME_BEGIN); }
    public void playBackToMenu() { playSound(BACK_TO_MENU); }
    public void playBackToGame() { playSound(BACK_TO_GAME); }
    public void playDropAndScore() { playSound(DROP_AND_SCORE); }
    public void playFourClear() { playSound(FOUR_CLEAR); }
    public void playMassiveClear() { playSound(MASSIVE_CLEAR); }
    public void playNoClear() { playSound(NO_CLEAR); }
    public void playGameFail() { playSound(GAME_FAIL); }
    public void playThreeClear() { playSound(THREE_CLEAR); }

    // ========== 音乐播放方法 / Music Playback Methods ==========

    /**
     * 获取音乐文件的完整路径
     * Get complete path for music file
     */
    public static String getMusicPath(String fileName) {
        return getResourceBasePath() + "resources" + File.separator + "music" + File.separator + fileName;
    }

    /**
     * 播放背景音乐（循环）
     * Play background music (loop)
     */
    public void playMusic(String fileName) {
        if (!soundEnabled) return;

        // 如果正在播放同一首音乐，不重复播放 / If same music is playing, don't replay
        if (fileName.equals(currentMusic) && musicClip != null && musicClip.isRunning()) {
            return;
        }

        // 停止当前音乐 / Stop current music
        stopMusic();

        try {
            File musicFile = new File(getMusicPath(fileName));
            if (musicFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
                musicClip = AudioSystem.getClip();
                musicClip.open(audioStream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                currentMusic = fileName;
                System.out.println("开始播放音乐: " + fileName);
            } else {
                System.err.println("音乐文件不存在: " + musicFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("播放音乐失败: " + fileName + " - " + e.getMessage());
        }
    }

    /**
     * 停止当前播放的音乐
     * Stop currently playing music
     */
    public void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
        currentMusic = null;
    }

    /**
     * 暂停当前播放的音乐
     * Pause currently playing music
     */
    public void pauseMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
        }
    }

    /**
     * 恢复暂停的音乐
     * Resume paused music
     */
    public void resumeMusic() {
        if (musicClip != null && !musicClip.isRunning()) {
            musicClip.start();
        }
    }

    /**
     * 播放骨质疏松.mp3（背景音乐）
     * Play Boneless.mp3 (background music)
     */
    public void playBonelessMusic() { playMusic(MUSIC_BONELESS); }

    /**
     * 播放正义之矛.mp3（背景音乐）
     * Play Justice.mp3 (background music)
     */
    public void playJusticeMusic() { playMusic(MUSIC_JUSTICE); }
}