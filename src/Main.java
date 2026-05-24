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
    private boolean frozen = false;
    private double deadline;

    public void 冻结状态() {
        frozen = true;
    }

    private void collisionWithDeadline() {
        if (hexGrid == null) return;
        double radius = hexGrid.getSide() * 0.866;
        for (int r = hexGrid.getMaxRowCount(); r < hexGrid.getMarblesLength(); r++) {
            for (int c = 0; c < 50; c++) {
                Marble marble = hexGrid.getHex(r, c);
                if (marble != null && marble.getCenterY() + radius >= deadline) {
                    冻结状态();
                    return;
                }
            }
        }
    }

    public static void main(String[] args) {
        StartMenu.display();
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
        launchPad = new LaunchPad(hexGrid.getSide(), 18);
        hexGrid.setMaxRowCount(18);
        hexGrid.initRow(mWidth, mHeight);
        launchPad.setCannonPosition(mWidth, mHeight);
        deadline = launchPad.getTopY();
        launchMarble = new LaunchMarble();
        launchMarble.setScreenSize(mWidth, mHeight);
        launchMarble.init((int) launchPad.cannon.x, (int) launchPad.cannon.y, 0, 0);
    }

    @Override
    public void update(double dt) {
        if (frozen) return;
        if (hexGrid != null) hexGrid.update(dt);
        if (launchMarble != null) launchMarble.update(dt);
        collisionWithDeadline();
    }

    @Override
    public void paintComponent() {
        changeBackgroundColor(255, 255, 255);
        clearBackground(mWidth, mHeight);

        Graphics2D g2 = (Graphics2D) mGraphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hexGrid != null) hexGrid.draw(g2);
        if (launchPad != null) {
            launchPad.drawLaunchPad(g2, mWidth, mHeight);
            launchPad.drawCannon(g2, mouseX, mouseY);
        }
        if (launchMarble != null) launchMarble.draw(g2);
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (frozen) return;
        mouseX = event.getX();
        mouseY = event.getY();
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (frozen) return;
        if (launchMarble != null) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(event.getX(), event.getY());
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (frozen) return;
        if (event.getKeyCode() == KeyEvent.VK_SPACE && launchMarble != null) {
            launchMarble.reset(launchPad.cannon.x, launchPad.cannon.y);
            launchMarble.launch(mouseX > 0 ? mouseX : launchPad.cannon.x + 100,
                               mouseY > 0 ? mouseY : launchPad.cannon.y);
        }
    }
}