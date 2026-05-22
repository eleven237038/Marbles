import java.awt.*;

public class HexGrid extends Marble {
    private Marble[] hexLayer;
    private int hexagonsPerRow;
    private double hSpacing;
    private double vSpacing;

    public HexGrid() {
        super();
        this.hexLayer = null;
        this.hexagonsPerRow = 0;
        this.hSpacing = 0;
        this.vSpacing = 0;
    }

    public void initGrid(int screenWidth) {
        double side = getSide();
        int perRow = (int)(screenWidth / (side * Math.sqrt(3)));
        this.hexagonsPerRow = perRow;
        this.hSpacing = side * Math.sqrt(3);
        this.vSpacing = side * 1.5;
        this.hexLayer = new Marble[perRow * 2];

        double layer1Start = side * Math.sqrt(3) / 2;
        double layer2Start = side * Math.sqrt(3);

        for (int col = 0; col < perRow; col++) {
            double x = layer1Start + col * hSpacing;
            double y = side;
            hexLayer[col] = new Marble();
            hexLayer[col].init(x, y);
        }

        for (int col = 0; col < perRow; col++) {
            double x = layer2Start + col * hSpacing;
            double y = side + vSpacing;
            hexLayer[perRow + col] = new Marble();
            hexLayer[perRow + col].init(x, y);
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