import java.awt.*;

public class MarbleRowGenerator extends Marble {
    private Marble[] hexLayer;
    private int hexagonsPerRow;
    private int rowCount;
    private double hSpacing;
    private double vSpacing;
    private double layerStart;

    public MarbleRowGenerator() {
        super();
        this.hexLayer = null;
        this.hexagonsPerRow = 0;
        this.rowCount = 0;
        this.hSpacing = 0;
        this.vSpacing = 0;
        this.layerStart = 0;
    }

    public void initGrid(int screenWidth, int screenHeight) {
        double side = getSide();
        int perRow = (int)(screenWidth / (side * Math.sqrt(3)));
        int rows = (int)(screenHeight / (side * 1.5)) + 1;

        this.hexagonsPerRow = perRow;
        this.rowCount = rows;
        this.hSpacing = side * Math.sqrt(3);
        this.vSpacing = side * 1.5;
        this.layerStart = side * Math.sqrt(3) / 2;
        this.hexLayer = new Marble[perRow * rows];

        double centerX = layerStart;
        int index = 0;

        for (int row = 0; row < rows; row++) {
            double y = side + row * vSpacing;
            for (int col = 0; col < perRow; col++) {
                hexLayer[index] = new Marble();
                hexLayer[index].init(centerX + col * hSpacing, y);
                index++;
            }
            centerX = centerX + (row % 2 == 0 ? side * Math.sqrt(3) / 2 : -side * Math.sqrt(3) / 2);
        }
    }

    public void update(double dt) {
        if (hexLayer != null) {
            for (Marble hex : hexLayer) {
                hex.update(dt);
            }
        }
    }

    public void setLayer1StartX(double startX) {
        if (hexLayer == null) return;
        for (int col = 0; col < hexagonsPerRow; col++) {
            hexLayer[col].setCenter(startX + col * hSpacing, hexLayer[col].getCenterY());
        }
    }

    public void setLayer2StartX(double startX) {
        if (hexLayer == null) return;
        for (int col = 0; col < hexagonsPerRow; col++) {
            hexLayer[hexagonsPerRow + col].setCenter(startX + col * hSpacing, hexLayer[hexagonsPerRow + col].getCenterY());
        }
    }

    public Marble getLayer1Hex(int index) {
        if (hexLayer != null && index >= 0 && index < hexagonsPerRow) {
            return hexLayer[index];
        }
        return null;
    }

    public Marble getLayer2Hex(int index) {
        if (hexLayer != null && index >= 0 && index < hexagonsPerRow) {
            return hexLayer[hexagonsPerRow + index];
        }
        return null;
    }

    public Marble[] getAllHexagons() {
        return hexLayer;
    }

    public int getHexagonCount() {
        return hexLayer != null ? hexLayer.length : 0;
    }

    public double getHorizontalSpacing() {
        return hSpacing;
    }

    public double getVerticalSpacing() {
        return vSpacing;
    }

    public void draw(Graphics2D g) {
        if (hexLayer != null) {
            for (Marble hex : hexLayer) {
                hex.draw(g);
            }
        }
    }

    public void resetGrid() {
        if (hexLayer != null) {
            for (Marble hex : hexLayer) {
                hex.reset();
            }
        }
        hexLayer = null;
        hexagonsPerRow = 0;
        hSpacing = 0;
        vSpacing = 0;
    }
}