package scenes;

import core.GameLoop;
import core.InputHandler;
import core.WaveManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import model.Bullet;
import model.Gaod;
import model.Player;
import model.Obstacle;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GameScene — the main gameplay screen.
 *
 * Responsibilities:
 *   - Manages all game objects (players, gaods, bullets)
 *   - Runs the game loop (update + render)
 *   - Handles spawning, collisions, respawning, and game over
 */
public class GameScene {

    public static final int WIDTH  = 960;
    public static final int HEIGHT = 640;

    // --- Player spawn points (corners) ---
    private static final double[][] SPAWN_POINTS = {
        { 80,  80  },  // Player 1 — top left
        { 840, 80  },  // Player 2 — top right
        { 80,  520 },  // Player 3 — bottom left
        { 840, 520 },  // Player 4 — bottom right
    };

    // --- Player colors ---
    private static final Color[] PLAYER_COLORS = {
        Color.web("#00d4ff"),  // P1 — cyan
        Color.web("#ff6b35"),  // P2 — orange
        Color.web("#7fff00"),  // P3 — chartreuse
        Color.web("#ff69b4"),  // P4 — pink
    };

    // --- JavaFX ---
    private final Scene scene;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // --- Game Objects ---
    private final List<Player> players = new ArrayList<>();
    private final List<Gaod>   gaods   = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();

    // --- Systems ---
    private final GameLoop    gameLoop;
    private final InputHandler inputHandler;
    private final WaveManager  waveManager;

    // --- State ---
    private boolean gameOver   = false;
    private int     score      = 0;     // frames survived
    private long    startTime  = 0;
    private int     playerCount;
    private final Stage stage;
    private int levelMessageTimer = 120;
<<<<<<< Updated upstream



=======
    private Image blueGaodImg;
    private Image redGaodImg;
    private Image bulletImg;
    private Image plantsImg;
    private Font hudFont;
    private Font mainFont;
    private Font msgFont;
    private Font infoFont;
    private Font nameFont;
    private double mouseX = 0;
    private double mouseY = 0;
    private boolean mouseTracked = false;
>>>>>>> Stashed changes

    public GameScene(Stage stage, int playerCount) {
        this.stage = stage;
        this.playerCount = playerCount;

        //--- Game assets ---
        Image floorTile = new Image( getClass().getResourceAsStream("/assets/tile.png"));
        List <Image> playerAvatar = new ArrayList<>(); // add player avatars here




        canvas = new Canvas(WIDTH, HEIGHT);
        gc     = canvas.getGraphicsContext2D();
        scene  = new Scene(new StackPane(canvas), WIDTH, HEIGHT);

        // Render tile
        gc.clearRect(0, 0, GameScene.WIDTH, GameScene.HEIGHT);
        drawFloor(gc, floorTile, GameScene.WIDTH, GameScene.HEIGHT);

        // Create players
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(i, "P" + (i + 1),
                SPAWN_POINTS[i][0], SPAWN_POINTS[i][1]));
        }

        waveManager   = new WaveManager(WIDTH, HEIGHT);
        inputHandler  = new InputHandler(scene, players);

        startTime = System.currentTimeMillis();
        gameLoop = new GameLoop(this::update, () -> render(floorTile));
        gameLoop.start();

        // Mouse shooting and continuous aiming for Player 1
        scene.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            mouseTracked = true;
        });
        scene.setOnMouseDragged(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            mouseTracked = true;
        });
        scene.setOnMousePressed(e -> {
            if (gameOver) {
                gameLoop.stop();
                MenuScene menu = new MenuScene(stage);
                stage.setScene(menu.getScene());
                return;
            }

            mouseX = e.getX();
            mouseY = e.getY();
            mouseTracked = true;

            if (!players.isEmpty() && players.get(0).isAlive()) {
                Player p1 = players.get(0);
                double angle = Math.atan2(
<<<<<<< Updated upstream
                        e.getY() - (p1.getY() + Player.SIZE / 2.0),
                        e.getX() - (p1.getX() + Player.SIZE / 2.0)
                );
=======
                        mouseY - (p1.getY() + Player.SIZE / 2.0),
                        mouseX - (p1.getX() + Player.SIZE / 2.0));
>>>>>>> Stashed changes
                fireBullet(p1, angle);
                p1.triggerShootCooldown();
            }
        });
        stage.setOnCloseRequest(e -> gameLoop.stop());
    }

    // -------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------

    private void update() {
        if (gameOver) return;

        inputHandler.update();

        // Update players
        for (Player p : players) {
            p.update(WIDTH, HEIGHT, obstacles);

            // Override P1 aim angle with mouse aim if mouse is tracked
            if (p.getPlayerIndex() == 0 && p.isAlive() && mouseTracked) {
                double angle = Math.atan2(
                    mouseY - (p.getY() + Player.SIZE / 2.0),
                    mouseX - (p.getX() + Player.SIZE / 2.0)
                );
                p.setAimAngle(angle);
            }

            // Respawn if timer expired and at least one ally is alive
            if (!p.isAlive() && p.getRespawnTimer() == 0 && anyPlayerAlive()) {
                p.respawn(SPAWN_POINTS[p.getPlayerIndex()][0],
                          SPAWN_POINTS[p.getPlayerIndex()][1]);
            }

            // Keyboard shooting (P2, P3, P4)
            if (p.getPlayerIndex() > 0 && p.canShoot()) {
                fireBullet(p, p.getAimAngle());
                p.triggerShootCooldown();
            }
        }

        // Spawn new Gaods from wave manager
        int oldLevel = waveManager.getCurrentLevel();
        gaods.addAll(waveManager.update(gaods));

        if(waveManager.getCurrentLevel() > oldLevel){
            levelMessageTimer = 120; //showw level message
        }

        // Update Gaods
        for (Gaod g : gaods) {
            g.update(players);

            // Check Gaod → Player collision
            for (Player p : players) {
                if (p.isAlive() && g.isAlive() && g.isCollidingWithPlayer(p)) {
                    p.takeDamage(waveManager.getRespawnDelayFrames());
                }
            }
        }

        // Update bullets
        for (Bullet b : bullets) {
            b.update(WIDTH, HEIGHT);

            // Check Bullet → Gaod collision
            for (Gaod g : gaods) {
                if (b.isActive() && g.isAlive() && b.isCollidingWith(g)) {
                    g.takeDamage(Bullet.DAMAGE);
                    b.deactivate();
                    if (!g.isAlive()) score += 10;
                }
            }
        }

        // Remove dead/inactive objects
        gaods.removeIf(g   -> !g.isAlive());
        bullets.removeIf(b -> !b.isActive());

        // Check game over — all players dead
        if (!anyPlayerAlive()) {
            gameOver = true;
            gameLoop.stop();
        }

        if(levelMessageTimer > 0){
            levelMessageTimer--;
        }
    }

    private void fireBullet(Player p, double angle) {
        double bx = p.getX() + Player.SIZE / 2.0 - Bullet.SIZE / 2.0;
        double by = p.getY() + Player.SIZE / 2.0 - Bullet.SIZE / 2.0;
        bullets.add(new Bullet(bx, by, angle, p.getPlayerIndex()));
        p.triggerShootCooldown();
    }

    private boolean anyPlayerAlive() {
        return players.stream().anyMatch(Player::isAlive);
    }

    // -------------------------------------------------------
    // RENDER
    // -------------------------------------------------------

    private void render(Image floorTile) {
        // Background
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.clearRect(0, 0, GameScene.WIDTH, GameScene.HEIGHT);
        drawFloor(gc, floorTile, GameScene.WIDTH, GameScene.HEIGHT);
        drawBullets();
        drawGaods();
        drawPlayers();
        drawHUD();
        drawLevelMessage();

        if (gameOver) drawGameOver();
    }

    private void drawGrid() {
        gc.setStroke(Color.web("#16213e"));
        gc.setLineWidth(1);
        for (int x = 0; x < WIDTH;  x += 40) gc.strokeLine(x, 0, x, HEIGHT);
        for (int y = 0; y < HEIGHT; y += 40) gc.strokeLine(0, y, WIDTH, y);
    }

    private void drawPlayers() {
        for (Player p : players) {
            if (!p.isAlive()) {
                // Draw respawn countdown
                gc.setFill(Color.web("#ffffff44"));
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                gc.fillText(p.getName() + " respawning in " +
                    (p.getRespawnTimer() / 60 + 1) + "s",
                    SPAWN_POINTS[p.getPlayerIndex()][0] - 20,
                    SPAWN_POINTS[p.getPlayerIndex()][1] - 10);
                continue;
            }

            Color color = PLAYER_COLORS[p.getPlayerIndex()];
            double x = p.getX(), y = p.getY(), s = Player.SIZE;

            // Shadow
            gc.setFill(Color.web("#00000055"));
            gc.fillOval(x + 3, y + 3, s, s);

            // Body
            gc.setFill(color);
            gc.fillOval(x, y, s, s);

            // Outline
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(x, y, s, s);

            // Name + health
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 11));
            gc.fillText(p.getName() + " ❤".repeat(p.getHealth()), x - 5, y - 6);

            // Aim line
            gc.setStroke(color.deriveColor(0, 1, 1, 0.5));
            gc.setLineWidth(1);
            double cx = x + s / 2, cy = y + s / 2;
            gc.strokeLine(cx, cy,
                cx + Math.cos(p.getAimAngle()) * 20,
                cy + Math.sin(p.getAimAngle()) * 20);
        }
    }

    private void drawGaods() {
        for (Gaod g : gaods) {
            if (!g.isAlive()) continue;
            double x = g.getX(), y = g.getY(), s = Gaod.SIZE;

            // Body
            gc.setFill(Color.web("#e94560"));
            gc.fillRect(x, y, s, s);

            // Eyes
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + 5, y + 6, 5, 5);
            gc.fillOval(x + 14, y + 6, 5, 5);

            // Outline
            gc.setStroke(Color.web("#ff0000"));
            gc.setLineWidth(1);
            gc.strokeRect(x, y, s, s);
        }
    }

    private void drawBullets() {
        gc.setFill(Color.YELLOW);
        for (Bullet b : bullets) {
            if (b.isActive()) {
                gc.fillOval(b.getX(), b.getY(), Bullet.SIZE, Bullet.SIZE);
            }
        }
    }

    private void drawHUD() {
        // Wave info bar
        gc.setFill(Color.web("#00000088"));
        gc.fillRect(0, 0, WIDTH, 36);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gc.fillText("LEVEL " + waveManager.getCurrentLevel(), 10, 22);

        gc.setFill(Color.web("#ffcc00"));
        gc.fillText("⏱ " + waveManager.getSecondsRemaining() + "s", 130, 22);

        gc.setFill(Color.web("#00d4ff"));
        gc.fillText("SCORE: " + score, 250, 22);

        // Gaods alive
        long alive = gaods.stream().filter(Gaod::isAlive).count();
        gc.setFill(Color.web("#e94560"));
        gc.fillText("GAODS: " + alive, 420, 22);

        // Player key bindings reminder (bottom bar)
        gc.setFill(Color.web("#00000088"));
        gc.fillRect(0, HEIGHT - 24, WIDTH, 24);
        gc.setFill(Color.web("#ffffff88"));
        gc.setFont(Font.font("Arial", 11));
        gc.fillText("P1: WASD+Mouse  P2: Arrows+L  P3: IJKL+U  P4: Numpad", 10, HEIGHT - 7);
    }

    private void drawLevelMessage() {
        if (levelMessageTimer <= 0 || gameOver) return;

        gc.setFill(Color.web("#00000099"));
        gc.fillRoundRect(WIDTH / 2.0 - 160, HEIGHT / 2.0 - 60, 320, 100, 20, 20);

        gc.setFill(Color.web("#ffcc00"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        gc.fillText("LEVEL " + waveManager.getCurrentLevel(), WIDTH / 2.0 - 100, HEIGHT / 2.0);
    }

    private void drawGameOver() {
        gc.setFill(Color.web("#000000bb"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#e94560"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        gc.fillText("GAME OVER", WIDTH / 2.0 - 170, HEIGHT / 2.0 - 30);

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        gc.fillText("Survived: " + elapsed + "s   |   Level: " +
            waveManager.getCurrentLevel() + "   |   Score: " + score,
            WIDTH / 2.0 - 230, HEIGHT / 2.0 + 20);

        gc.setFill(Color.web("#ffcc00"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.fillText("Click anywhere to return to menu", WIDTH / 2.0 - 140, HEIGHT / 2.0 + 60);
    }

    private void drawFloor(GraphicsContext gc, Image floorTile, int width, int height) {
        gc.setImageSmoothing(false); // keep pixel art crisp
        for (int y = 0; y < height; y += 32) {
            for (int x = 0; x < width; x += 32) { // [TO ADD] change tile in the ends i.e x <= width
                gc.drawImage(floorTile, x, y, 32, 32);
            }
        }
    }

    public Scene getScene() { return scene; }
}
