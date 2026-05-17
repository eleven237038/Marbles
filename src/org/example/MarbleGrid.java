package org.example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 六边形弹珠网格 - 点顶六边形蜂巢式排列
 *
 * 点顶六边形（Pointy-topped）蜂巢式排列：
 * - 六边形尖朝上/下（垂直方向）
 * - 水平紧密排列，宽度方向相切
 * - 偶数行与奇数行垂直方向偏移（蜂巢结构）
 */
public class MarbleGrid {
    private Marble[][] grid;
    private int rows;
    private int cols;
    private double scrollOffsetY = 0;  // 像素级滚动偏移

    public MarbleGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Marble[rows][cols];
    }

    /**
     * 设置滚动偏移（像素级平滑下降）
     */
    public void setScrollOffsetY(double offset) {
        this.scrollOffsetY = offset;
    }

    public double getScrollOffsetY() {
        return scrollOffsetY;
    }

    /**
     * 计算六边形中心像素坐标（点顶蜂巢式）
     */
    public static double[] getHexCenter(int row, int col) {
        double size = GameConfig.HEX_SIZE;
        double horizSpacing = size * Math.sqrt(3);
        double vertSpacing = size * 1.5;
        double xOffset = (row % 2 == 1) ? horizSpacing * 0.5 : 0;

        double x = col * horizSpacing + size * Math.sqrt(3) / 2 + xOffset;
        double y = row * vertSpacing + size + GameConfig.GRID_OFFSET_Y;
        return new double[]{x, y};
    }

    /**
     * 获取六边形中心坐标（带滚动偏移）
     */
    public double[] getHexCenterWithScroll(int row, int col) {
        double[] base = getHexCenter(row, col);
        base[1] += scrollOffsetY;
        return base;
    }

    /**
     * 绘制点顶六边形
     */
    private void drawPointyHex(Graphics2D g, double cx, double cy, Color borderColor) {
        double size = GameConfig.HEX_SIZE;
        double w = size * Math.sqrt(3) / 2;  // 六边形半宽

        Path2D path = new Path2D.Double();
        // 6个顶点：左上, 上, 右上, 右下, 下, 左下
        path.moveTo(cx - w, cy - size * 0.5);       // 左上
        path.lineTo(cx, cy - size);                  // 上（尖）
        path.lineTo(cx + w, cy - size * 0.5);        // 右上
        path.lineTo(cx + w, cy + size * 0.5);         // 右下
        path.lineTo(cx, cy + size);                  // 下（尖）
        path.lineTo(cx - w, cy + size * 0.5);         // 左下
        path.closePath();

        g.setColor(borderColor);
        g.draw(path);
    }

    /**
     * 绘制蜂巢状网格背景
     */
    public void renderHoneycombBackground(Graphics2D g) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double[] center = getHexCenterWithScroll(row, col);
                drawPointyHex(g, center[0], center[1], new Color(50, 50, 75));
            }
        }
    }

    /**
     * 获取六边形的6个邻居（蜂巢式排列）
     *
     * 蜂巢式邻居布局（每个六边形有6个相邻）：
     *       /\        /\
     *      /1\2\      /1\2\
     *      \0/3/  =>  \0/3/
     *       \4/5/      \4\5/
     *
     * 偶数行邻居:                    奇数行邻居:
     *   上左(-1,0) 上右(-1,1)       上左(-1,-1) 上右(-1,0)
     *   左(0,-1)   右(0,1)          左(0,-1)   右(0,1)
     *   下左(1,0)  下右(1,1)        下左(1,-1)  下右(1,0)
     */
    public List<int[]> getNeighbors(int row, int col) {
        List<int[]> neighbors = new ArrayList<>();

        if (row % 2 == 0) {
            // 偶数行（尖朝上）
            int[][] offsets = {
                {-1, 0},   // 上左
                {-1, 1},   // 上右
                {0, -1},   // 左
                {0, 1},    // 右
                {1, 0},    // 下左
                {1, 1}     // 下右
            };
            for (int[] offset : offsets) {
                int newRow = row + offset[0];
                int newCol = col + offset[1];
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    neighbors.add(new int[]{newRow, newCol});
                }
            }
        } else {
            // 奇数行（尖朝下）
            int[][] offsets = {
                {-1, -1},  // 上左
                {-1, 0},   // 上右
                {0, -1},   // 左
                {0, 1},    // 右
                {1, -1},   // 下左
                {1, 0}     // 下右
            };
            for (int[] offset : offsets) {
                int newRow = row + offset[0];
                int newCol = col + offset[1];
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    neighbors.add(new int[]{newRow, newCol});
                }
            }
        }
        return neighbors;
    }

    /**
     * 检查两个六边形位置是否相邻（用于碰撞检测）
     */
    public boolean areNeighbors(int row1, int col1, int row2, int col2) {
        for (int[] neighbor : getNeighbors(row1, col1)) {
            if (neighbor[0] == row2 && neighbor[1] == col2) {
                return true;
            }
        }
        return false;
    }

    public void addMarble(int row, int col, MarbleColor color) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            grid[row][col] = new Marble(row, col, color);
            double[] pos = getHexCenter(row, col);
            grid[row][col].setPosition(pos[0], pos[1]);
        }
    }

    /**
     * 放置已存在的弹珠到指定位置
     */
    public void placeMarble(int row, int col, Marble marble) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            marble.setRow(row);
            marble.setCol(col);
            double[] pos = getHexCenter(row, col);
            marble.setPosition(pos[0], pos[1]);
            grid[row][col] = marble;
        }
    }

    public Marble getMarble(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return grid[row][col];
        }
        return null;
    }

    public void removeMarble(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            grid[row][col] = null;
        }
    }

    /**
     * 查找所有相邻的同颜色弹珠（连通分量）
     */
    public Set<Marble> findConnected(Marble start) {
        Set<Marble> connected = new HashSet<>();
        Set<String> visited = new HashSet<>();
        findConnectedRecursive(start, connected, visited);
        return connected;
    }

    private void findConnectedRecursive(Marble marble, Set<Marble> connected, Set<String> visited) {
        if (marble == null) return;

        String key = marble.getRow() + "," + marble.getCol();
        if (visited.contains(key)) return;
        visited.add(key);

        connected.add(marble);

        // 检查所有6个邻居
        List<int[]> neighbors = getNeighbors(marble.getRow(), marble.getCol());
        for (int[] neighbor : neighbors) {
            Marble neighborMarble = getMarble(neighbor[0], neighbor[1]);
            if (neighborMarble != null && neighborMarble.getColor() == marble.getColor()) {
                findConnectedRecursive(neighborMarble, connected, visited);
            }
        }
    }

    public void removeMarbles(Set<Marble> marbles) {
        for (Marble m : marbles) {
            removeMarble(m.getRow(), m.getCol());
        }
    }

    /**
     * 查找所有与顶部行弹珠相连的弹珠（连通分量）
     */
    public Set<Marble> findAllConnectedFromTop() {
        Set<Marble> connected = new HashSet<>();
        Set<String> visited = new HashSet<>();
        // 从顶部行的所有弹珠开始BFS
        for (int col = 0; col < cols; col++) {
            Marble topMarble = getMarble(0, col);
            if (topMarble != null) {
                bfsConnected(topMarble, connected, visited);
            }
        }
        return connected;
    }

    private void bfsConnected(Marble start, Set<Marble> connected, Set<String> visited) {
        java.util.Queue<Marble> queue = new java.util.LinkedList<>();
        queue.add(start);
        visited.add(start.getRow() + "," + start.getCol());
        connected.add(start);

        while (!queue.isEmpty()) {
            Marble current = queue.poll();
            List<int[]> neighbors = getNeighbors(current.getRow(), current.getCol());
            for (int[] neighbor : neighbors) {
                Marble neighborMarble = getMarble(neighbor[0], neighbor[1]);
                if (neighborMarble != null && !visited.contains(neighbor[0] + "," + neighbor[1])) {
                    visited.add(neighbor[0] + "," + neighbor[1]);
                    connected.add(neighborMarble);
                    queue.add(neighborMarble);
                }
            }
        }
    }

    /**
     * 获取所有弹珠
     */
    public Set<Marble> getAllMarbles() {
        Set<Marble> allMarbles = new HashSet<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Marble m = getMarble(row, col);
                if (m != null) {
                    allMarbles.add(m);
                }
            }
        }
        return allMarbles;
    }

    /**
     * 初始化网格 - 仅生成5行弹珠，蜂巢式排列
     */
    public void initialize() {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < cols; col++) {
                MarbleColor randomColor = MarbleColor.values()[(int)(Math.random() * 4)];
                addMarble(row, col, randomColor);
            }
        }
    }

    public void render(Graphics2D g) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Marble marble = grid[row][col];
                if (marble != null) {
                    marble.render(g, scrollOffsetY);
                }
            }
        }
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}