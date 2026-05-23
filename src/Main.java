import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

public class Main extends GameEngine {
    private Marbles hexGrid;
    private LaunchPad launchPad;
    private LaunchMarble launchMarble;
    private double mouseX = 0;
    private double mouseY = 0;

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
            initMarbleGrid();
        });
    }

    @Override
    public void setupWindow(int width, int height) {
        mWidth = 483;
        mHeight = 1080;
        super.setupWindow(mWidth, mHeight);
        mFrame.setResizable(false);
    }

    private void initMarbleGrid() {
        hexGrid = new Marbles();
        hexGrid.initRow(mWidth, mHeight);
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
        launchPad.setCannonPosition(mWidth, mHeight);
        launchMarble = new LaunchMarble();
        launchMarble.init((int) launchPad.cannon.x, (int) launchPad.cannon.y, 0, 0);
    }

    @Override
    public void update(double dt) {
        if (hexGrid != null) {
            hexGrid.update(dt);
        }
        if (launchMarble != null) {
            launchMarble.update(dt);
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
            hexGrid.draw(mGraphics);  // Priority 2 (bottom)
        }
        if (launchPad != null) {
            launchPad.drawLaunchPad(mGraphics, mWidth, mHeight);  // Priority 1
            launchPad.drawCannon(mGraphics, mouseX, mouseY);  // Priority 0 (top)
        }
        if (launchMarble != null) {
            launchMarble.draw(mGraphics);
        }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (launchMarble != null && !launchMarble.isLaunched()) {
            launchMarble.launch(mouseX, mouseY);
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_SPACE) {
            if (launchMarble != null && !launchMarble.isLaunched()) {
                launchMarble.launch(mouseX, mouseY);
            }
        }
    }
}