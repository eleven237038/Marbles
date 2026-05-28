import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Map<String, Clip> soundClips;
    private Map<String, Boolean> soundLoaded;
    private boolean soundEnabled = true;

    // 音效文件路径常量
    public static final String GAME_BEGIN = "GameBegin.wav";
    public static final String BACK_TO_MENU = "从菜单返回主页.wav";
    public static final String BACK_TO_GAME = "从菜单返回游戏.wav";
    public static final String DROP_AND_SCORE = "掉落并加分.wav";
    public static final String FOUR_CLEAR = "四消.wav";
    public static final String MASSIVE_CLEAR = "大量消除1.wav";
    public static final String NO_CLEAR = "打击后黏附无消除.wav";
    public static final String GAME_FAIL = "晋级&失败.wav";
    public static final String THREE_CLEAR = "钝角三消.wav";

    private SoundManager() {
        soundClips = new HashMap<>();
        soundLoaded = new HashMap<>();
        preloadAllSounds();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

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

    private void preloadSound(String fileName) {
        try {
            File soundFile = new File(ResourceUtil.getSoundPath(fileName));
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

    public void playSound(String fileName) {
        if (!soundEnabled) return;

        // 检查是否已预加载且可用
        if (soundClips.containsKey(fileName) && soundClips.get(fileName) != null) {
            Clip clip = soundClips.get(fileName);
            // 重置到开头并播放
            clip.setFramePosition(0);
            clip.start();
        } else if (soundLoaded.containsKey(fileName) && !soundLoaded.get(fileName)) {
            // 尝试动态加载
            try {
                File soundFile = new File(ResourceUtil.getSoundPath(fileName));
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

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void stopAllSounds() {
        for (Clip clip : soundClips.values()) {
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
        }
    }

    public void stopSound(String fileName) {
        if (soundClips.containsKey(fileName)) {
            Clip clip = soundClips.get(fileName);
            if (clip != null && clip.isRunning()) {
                clip.stop();
                clip.setFramePosition(0);
            }
        }
    }

    // 播放音效的便捷方法
    public void playGameBegin() { playSound(GAME_BEGIN); }
    public void playBackToMenu() { playSound(BACK_TO_MENU); }
    public void playBackToGame() { playSound(BACK_TO_GAME); }
    public void playDropAndScore() { playSound(DROP_AND_SCORE); }
    public void playFourClear() { playSound(FOUR_CLEAR); }
    public void playMassiveClear() { playSound(MASSIVE_CLEAR); }
    public void playNoClear() { playSound(NO_CLEAR); }
    public void playGameFail() { playSound(GAME_FAIL); }
    public void playThreeClear() { playSound(THREE_CLEAR); }
}