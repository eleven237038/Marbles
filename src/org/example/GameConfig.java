package org.example;

// 游戏配置常量 - 9:16手机屏幕比例，点顶六边形蜂巢式排列
public final class GameConfig {
    public static final int SCENE_WIDTH = 360;
    public static final int SCENE_HEIGHT = 640;

    public static final int HEX_SIZE = 15;
    public static final int GRID_COLS = 13;
    public static final int GRID_ROWS = 21;
    public static final int GRID_OFFSET_Y = 50;

    public static final int SHOOTER_Y = SCENE_HEIGHT - 80;
    public static final int SHOOTER_X = SCENE_WIDTH / 2;
    public static final int BALL_SPEED = 14;

    public static final double MARBLE_RADIUS = HEX_SIZE * 0.85;

    public static final double HEX_HEIGHT = HEX_SIZE * 2;
    public static final double HEX_WIDTH = HEX_SIZE * Math.sqrt(3);
    public static final double HEX_HORIZ_SPACING = HEX_WIDTH;
    public static final double HEX_VERT_SPACING = HEX_HEIGHT * 0.75;

    public static final int BASE_GAME_SPEED_MS = 16;

    private GameConfig() {}
}