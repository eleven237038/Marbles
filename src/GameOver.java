import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * GameOver - 游戏结束界面
 * 显示 GAME OVER 或 LEVEL COMPLETE，以及相应按钮
 */
public class GameOver extends JPanel implements MouseListener {

    public interface GameOverListener {
        void onRestart();
        void onBackToMenu();
        void onNextLevel();
    }

    private boolean win;
    private int score;

    private BufferedImage restartImg;
    private BufferedImage menuImg;
    private BufferedImage nextImg;

    private Rectangle restartBtn;
    private Rectangle menuBtn;
    private Rectangle nextBtn;

    private GameOverListener listener;

    public GameOver(
            boolean win,
            int score,
            BufferedImage restartImg,
            BufferedImage menuImg,
            BufferedImage nextImg,
            GameOverListener listener
    ) {
        this.win = win;
        this.score = score;

        this.restartImg = restartImg;
        this.menuImg = menuImg;
        this.nextImg = nextImg;

        this.listener = listener;

        addMouseListener(this);

        setOpaque(false);

        restartBtn = new Rectangle(390, 420, 300, 90);
        menuBtn = new Rectangle(390, 540, 300, 90);
        nextBtn = new Rectangle(390, 420, 300, 90);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // 半透明背景
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,getWidth(),getHeight());

        // 标题
        g2.setFont(new Font("Arial", Font.BOLD, 72));

        if(win) {
            g2.setColor(new Color(255,220,80));
            drawCenteredString(g2, "LEVEL COMPLETE!", 200);
        }
        else {
            g2.setColor(new Color(255,80,80));
            drawCenteredString(g2, "GAME OVER", 200);
        }

        // Score
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        g2.setColor(Color.WHITE);

        drawCenteredString(g2, "Score : " + score, 320);

        // 按钮
        if(win) {
            drawButton(g2, nextImg, nextBtn, "NEXT LEVEL");
        }
        else {
            drawButton(g2, restartImg, restartBtn, "RESTART");
        }

        drawButton(g2, menuImg, menuBtn, "MAIN MENU");
    }

    private void drawButton(Graphics2D g2,
                            BufferedImage img,
                            Rectangle rect,
                            String text)
    {
        if(img != null) {
            g2.drawImage(img,
                    rect.x,
                    rect.y,
                    rect.width,
                    rect.height,
                    null);
        }
        else {
            g2.setColor(new Color(60,60,60));
            g2.fillRoundRect(rect.x, rect.y,
                    rect.width, rect.height,
                    30,30);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));

        FontMetrics fm = g2.getFontMetrics();

        int tx = rect.x + (rect.width - fm.stringWidth(text))/2;
        int ty = rect.y + rect.height/2 + 10;

        g2.drawString(text, tx, ty);
    }

    private void drawCenteredString(Graphics2D g2,
                                    String text,
                                    int y)
    {
        FontMetrics fm = g2.getFontMetrics();

        int x = (getWidth() - fm.stringWidth(text))/2;

        g2.drawString(text, x, y);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        Point p = e.getPoint();

        if(win) {

            if(nextBtn.contains(p)) {
                listener.onNextLevel();
            }

        } else {

            if(restartBtn.contains(p)) {
                listener.onRestart();
            }
        }

        if(menuBtn.contains(p)) {
            listener.onBackToMenu();
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public static BufferedImage loadButtonImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.out.println("警告：找不到图片 " + path);
            return null;
        }
    }
}