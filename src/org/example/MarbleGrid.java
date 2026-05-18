package org.example;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 六边形弹珠网格 - 点顶蜂巢式排列
public class MarbleGrid {
    private Marble[][] grid;
    private int rows;
    private int cols;
    private double scrollOffsetY = 0;

    public MarbleGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Marble[rows][cols];
    }

    public void setScrollOffsetY(double offset) {
        this.scrollOffsetY = offset;
    }

    public double getScrollOffsetY() {
        return scrollOffsetY;
    }

    public static double[] getHexCenter(int row, int col) {
        double size = GameConfig.HEX_SIZE;
        double horizSpacing = size * Math.sqrt(3);
        double vertSpacing = size * 1.5;
        double xOffset = (row % 2 == 1) ? horizSpacing * 0.5 : 0;
        double x = col * horizSpacing + size * Math.sqrt(3) / 2 + xOffset;
        double y = row * vertSpacing + size + GameConfig.GRID_OFFSET_Y;
        return new double[]{x, y};
    }

    public double[] getHexCenterWithScroll(int row, int col) {
        double[] base = getHexCenter(row, col);
        base[1] += scrollOffsetY;
        return base;
    }

    private void drawPointyHex(Graphics2D g, double cx, double cy, Color borderColor) {
        double size = GameConfig.HEX_SIZE;
        double w = size * Math.sqrt(3) / 2;
        Path2D path = new Path2D.Double();
        path.moveTo(cx - w, cy - size * 0.5);
        path.lineTo(cx, cy - size);
        path.lineTo(cx + w, cy - size * 0.5);
        path.lineTo(cx + w, cy + size * 0.5);
        path.lineTo(cx, cy + size);
        path.lineTo(cx - w, cy + size * 0.5);
        path.closePath();
        g.setColor(borderColor);
        g.draw(path);
    }

    public void renderHoneycombBackground(Graphics2D g) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double[] center = getHexCenterWithScroll(row, col);
                drawPointyHex(g, center[0], center[1], new Color(50, 50, 75));
            }
        }
    }

    public List<int[]> getNeighbors(int row, int col) {
        List<int[]> neighbors = new ArrayList<>();
        if (row % 2 == 0) {
            int[][] offsets = {{-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, 0}, {1, 1}};
            for (int[] offset : offsets) {
                int newRow = row + offset[0];
                int newCol = col + offset[1];
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                    neighbors.add(new int[]{newRow, newCol});
                }
            }
        } else {
            int[][] offsets = {{-1, -1}, {-1, 0}, {0, -1}, {0, 1}, {1, -1}, {1, 0}};
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

    public Set<Marble> findAllConnectedFromTop() {
        Set<Marble> connected = new HashSet<>();
        Set<String> visited = new HashSet<>();
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

    public void initialize() {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < cols; col++) {
                MarbleColor randomColor = MarbleColor.values()[(int) (Math.random() * 4)];
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