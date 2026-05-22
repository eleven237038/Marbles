import java.awt.*;
import javax.swing.*;

public class Main extends GameEngine {
    private HexGrid hexGrid;

    public static void main(String[] args) {
        Main game = new Main();
        GameEngine.createGame(game, 60);
    }

    @Override
    public void init() {
        SwingUtilities.invokeLater(() -> {
            mFrame.setLayout(new BorderLayout());
            mPanel.setPreferredSize(new Dimension(mWidth, mHeight));
            mFrame.add(mPanel, BorderLayout.CENTER);
            mFrame.revalidate();
            initHexGrid();
        });
    }

    @Override
    public void setupWindow(int width, int height) {
        mWidth = 483;
        mHeight = 1080;
        super.setupWindow(mWidth, mHeight);
        mFrame.setResizable(false);
    }

    private void initHexGrid() {
        hexGrid = new HexGrid();
        hexGrid.initGrid(mWidth);
    }

    @Override
    public void update(double dt) {
        if (hexGrid != null) {
            hexGrid.update(dt);
        }
    }

    @Override
    public void paintComponent() {
        changeBackgroundColor(255, 255, 255);
        clearBackground(mWidth, mHeight);
        changeColor(0, 0, 0);

        ((Graphics2D) mGraphics).setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (hexGrid != null) {
            hexGrid.draw(mGraphics);
        }
    }
}