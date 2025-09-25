import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.IOException;

public class GamePanel extends JPanel implements Runnable {
    final short tileSize = 16, screenWidth = 512, screenHeight = 512;
    short fps = 30, gameState = 0, score = 0;
    double speed = 2;
    Thread gameThread;
    Player player;
    Random random = new Random();
    ArrayList<Point> plastic = new ArrayList<>();
    ArrayList<Point> fish = new ArrayList<>();
    BufferedImage menuScreen, fish1, fish2, bottle, background;
    BufferedImage[] submarineFrames = new BufferedImage[2];
    boolean fishAnimationToggle = true;
    int submarineFrameIndex = 0;
    long lastSubmarineFrameTime = System.nanoTime(), submarineFrameDuration = 150_000_000;

    public GamePanel() {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.blue);
        setDoubleBuffered(true);
        setFocusable(true);
        loadResources();
        player = new Player(this);
        addKeyListener(player.getKeyHandler());
        for (int i = 0; i < 5; i++) spawnPlastic();
        for (int i = 0; i < 3; i++) spawnFish();
    }

    private void loadResources() {
        try {
            menuScreen = ImageIO.read(getClass().getResourceAsStream("/code/TitleScreen.png"));
            fish1 = ImageIO.read(getClass().getResourceAsStream("/code/fish1.png"));
            fish2 = ImageIO.read(getClass().getResourceAsStream("/code/fish2.png"));
            bottle = ImageIO.read(getClass().getResourceAsStream("/code/bottle.png"));
            submarineFrames[0] = ImageIO.read(getClass().getResourceAsStream("/code/Player_Sprites/sub1.png"));
            submarineFrames[1] = ImageIO.read(getClass().getResourceAsStream("/code/Player_Sprites/sub2.png"));
            background = ImageIO.read(getClass().getResourceAsStream("/code/background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        double drawInterval = 1000000000.0 / fps;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (gameThread != null) {
            update();
            repaint();
            try {
                double remainingTime = nextDrawTime - System.nanoTime();
                Thread.sleep(Math.max((long)remainingTime / 1000000, 0));
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (gameState == 1) {
            player.update();
            speed += 0.01;
            updateObjects(plastic, false);
            updateObjects(fish, true);
            updateSubmarineAnimation();
        }
    }

    private void updateObjects(ArrayList<Point> objects, boolean isFish) {
        for (Point p : objects) {
            p.x -= speed;
            if (p.x + tileSize * 2 < 0) {
                p.x = screenWidth + random.nextInt(isFish ? 150 : 100);
                p.y = random.nextInt(screenHeight - tileSize * 2);
            }
            if (player.getBounds().intersects(new Rectangle(p.x, p.y, tileSize * 2, tileSize * 2))) {
                if (isFish) gameState = 2;
                else score++;
                p.x = screenWidth + random.nextInt(100);
                p.y = random.nextInt(screenHeight - tileSize * 2);
            }
        }
    }

    private void updateSubmarineAnimation() {
        if (System.nanoTime() - lastSubmarineFrameTime >= submarineFrameDuration) {
            submarineFrameIndex = (submarineFrameIndex + 1) % submarineFrames.length;
            lastSubmarineFrameTime = System.nanoTime();
        }
        fishAnimationToggle = (System.nanoTime() % 1000000000 < 500000000);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        switch (gameState) {
            case 0 -> drawMenu(g2);
            case 1 -> drawPlaying(g2);
            case 2 -> drawGameOver(g2);
        }
        g2.dispose();
    }

    private void drawMenu(Graphics2D g2) {
        if (menuScreen != null) g2.drawImage(menuScreen, 0, 0, screenWidth, screenHeight, null);
        else {
            g2.setColor(Color.black);
            g2.setFont(new Font("Arial", Font.BOLD, 40));
            g2.drawString("Ocean Cleanup", 120, 200);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.drawString("Press ENTER to Start", 160, 250);
        }
    }

    private void drawPlaying(Graphics2D g2) {
        g2.drawImage(background, 0, 0, screenWidth, screenHeight, null);
        g2.drawImage(submarineFrames[submarineFrameIndex], player.x, player.y, tileSize * 2, tileSize * 2, null);
        for (Point p : plastic) g2.drawImage(bottle, p.x, p.y, tileSize * 2, tileSize * 2, null);
        for (Point f : fish) g2.drawImage(fishAnimationToggle ? fish1 : fish2, f.x, f.y, tileSize * 2, tileSize * 2, null);
        g2.setColor(Color.black);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString("Score: " + score, 20, 30);
        g2.setColor(Color.blue);
      g2.fillRect(512, 0, 512, 1024);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        g2.setColor(Color.red);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.drawString("Game Over!", 150, 200);
        g2.drawString("Score: " + score, 180, 250);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Press R to Restart", 170, 300);
    }

    public void spawnPlastic() {
        plastic.add(new Point(screenWidth + random.nextInt(200), random.nextInt(screenHeight - tileSize * 2)));
    }

    public void spawnFish() {
        fish.add(new Point(screenWidth + random.nextInt(300), random.nextInt(screenHeight - tileSize * 2)));
    }

    public void resetGame() {
        score = 0;
        player.x = 50;
        player.y = screenHeight / 2;
        speed = 2;
        plastic.clear();
        fish.clear();
        for (int i = 0; i < 5; i++) spawnPlastic();
        for (int i = 0; i < 3; i++) spawnFish();
        gameState = 1;
    }
}

class Player {
    GamePanel gamePanel;
    KeyHandler keyHandler;
    int x = 50, y = 240, speed = 4;

    public Player(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        keyHandler = new KeyHandler(this);
    }

    public KeyHandler getKeyHandler() {
        return keyHandler;
    }

    public void update() {
        if (keyHandler.upPressed) y -= speed;
        if (keyHandler.downPressed) y += speed;
        if (keyHandler.leftPressed) x -= speed;
        if (keyHandler.rightPressed) x += speed;
        x = Math.max(0, Math.min(gamePanel.screenWidth - 32, x));
        y = Math.max(0, Math.min(gamePanel.screenHeight - 32, y));
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 32, 32);
    }
}

class KeyHandler extends java.awt.event.KeyAdapter {
    Player player;
    boolean upPressed, downPressed, leftPressed, rightPressed;

    public KeyHandler(Player player) {
        this.player = player;
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
        int code = e.getKeyCode();
        if (code == java.awt.event.KeyEvent.VK_W) upPressed = true;
        if (code == java.awt.event.KeyEvent.VK_S) downPressed = true;
        if (code == java.awt.event.KeyEvent.VK_A) leftPressed = true;
        if (code == java.awt.event.KeyEvent.VK_D) rightPressed = true;
        if (code == java.awt.event.KeyEvent.VK_ENTER && player.gamePanel.gameState == 0) player.gamePanel.resetGame();
        if (code == java.awt.event.KeyEvent.VK_R && player.gamePanel.gameState == 2) player.gamePanel.resetGame();
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
        int code = e.getKeyCode();
        if (code == java.awt.event.KeyEvent.VK_W) upPressed = false;
        if (code == java.awt.event.KeyEvent.VK_S) downPressed = false;
        if (code == java.awt.event.KeyEvent.VK_A) leftPressed = false;
        if (code == java.awt.event.KeyEvent.VK_D) rightPressed = false;
    }
}