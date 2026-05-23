import java.awt.*;
import javax.swing.*;

public class Main extends GameEngine {
    private Marbles hexGrid;
    private LaunchPad launchPad;

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
            initMarbleRowGenerator();
        });
    }

    @Override
    public void setupWindow(int width, int height) {
        mWidth = 483;
        mHeight = 1080;
        super.setupWindow(mWidth, mHeight);
        mFrame.setResizable(false);
    }

    private void initMarbleRowGenerator() {
        hexGrid = new Marbles();
        hexGrid.initRow(mWidth, mHeight);
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
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

        if (launchPad != null) {
            launchPad.draw(mGraphics, mWidth, mHeight);
        }
        if (hexGrid != null) {
            hexGrid.draw(mGraphics);
        }
    }
}