package org.example;

import java.awt.Color;

/**
 * 弹珠颜色枚举 - 红黄绿蓝四种颜色
 */
public enum MarbleColor {
    RED(new Color(255, 80, 80)),
    YELLOW(new Color(255, 220, 60)),
    GREEN(new Color(80, 200, 80)),
    BLUE(new Color(80, 160, 255));

    private final Color color;

    MarbleColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}