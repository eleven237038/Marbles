package org.example;

/**
 * 游戏配置常量
 * 使用点顶六边形网格（Pointy-topped）- 蜂巢式排列
 *
 * 点顶六边形特点：
 * - 尖朝上/下（垂直方向）
 * - 六边形高度 > 宽度
 * - 偶数行六边形尖朝上，奇数行尖朝下（向右偏移）
 */
public final class GameConfig {
    // 手机屏幕比例 9:16
    public static final int SCENE_WIDTH = 360;
    public static final int SCENE_HEIGHT = 640;

    // 六边形网格参数 - Pointy-topped蜂巢式排列
    public static final int HEX_SIZE = 15;                    // 六边形外接圆半径
    public static final int GRID_COLS = 13;                   // 列数
    public static final int GRID_ROWS = 21;                  // 行数
    public static final int GRID_OFFSET_Y = 50;              // 网格顶部偏移

    // 发射器参数（底部中心）
    public static final int SHOOTER_Y = SCENE_HEIGHT - 80;
    public static final int SHOOTER_X = SCENE_WIDTH / 2;
    public static final int BALL_SPEED = 14;

    // 弹珠参数
    public static final double MARBLE_RADIUS = HEX_SIZE * 0.85;

    // Pointy-topped六边形参数
    // 六边形高度 = 2 * HEX_SIZE（尖到尖）
    // 六边形宽度 = sqrt(3) * HEX_SIZE（边到边）
    public static final double HEX_HEIGHT = HEX_SIZE * 2;                    // 六边形高度（尖到尖）
    public static final double HEX_WIDTH = HEX_SIZE * Math.sqrt(3);           // 六边形宽度（边到边）

    // 水平间距（六边形中心到中心）= HEX_WIDTH
    public static final double HEX_HORIZ_SPACING = HEX_WIDTH;

    // 垂直间距（六边形中心到中心）= HEX_HEIGHT * 0.75
    public static final double HEX_VERT_SPACING = HEX_HEIGHT * 0.75;

    // 游戏速度
    public static final int BASE_GAME_SPEED_MS = 16;

    private GameConfig() {}
}