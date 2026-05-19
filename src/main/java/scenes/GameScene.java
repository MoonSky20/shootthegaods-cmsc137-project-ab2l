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
 * - Manages all game objects (players, gaods, bullets)
 * - Runs the game loop (update + render)
 * - Handles spawning, collisions, respawning, and game over
 */
public class GameScene {

    public static final int WIDTH = 960;
    public static final int HEIGHT = 640;

    // --- Player spawn points (corners) ---
    private static final double[][] SPAWN_POINTS = {
            { 80, 80 }, // Player 1 — top left
            { 840, 80 }, // Player 2 — top right
            { 80, 520 }, // Player 3 — bottom left
            { 840, 520 }, // Player 4 — bottom right
    };

    // --- Player colors ---
    private static final Color[] PLAYER_COLORS = {
            Color.web("#00d4ff"), // P1 — cyan
            Color.web("#ff6b35"), // P2 — orange
            Color.web("#7fff00"), // P3 — chartreuse
            Color.web("#ff69b4"), // P4 — pink
    };

    // --- JavaFX ---
    private final Scene scene;
    private final Canvas canvas;
    private final GraphicsContext gc;

    // --- Game Objects ---
    private final List<Player> players = new ArrayList<>();
    private final List<Gaod> gaods = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();

    // --- Systems ---
    private GameLoop gameLoop;
    private final InputHandler inputHandler;
    private final WaveManager waveManager;

    // --- State ---
    private boolean gameOver = false;
    private int score = 0; // frames survived
    private long startTime = 0;
    private int playerCount;
    private final Stage stage;
    private int levelMessageTimer = 120;

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
    private boolean mouseDown = false;


    public GameScene(Stage stage, int playerCount) {
        this.stage = stage;
        this.playerCount = playerCount;

        hudFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/space_mono.ttf"), 14);
        if (hudFont == null) hudFont = Font.font("Arial", FontWeight.BOLD, 14);
        
        mainFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 56);
        if (mainFont == null) mainFont = Font.font("Arial", FontWeight.BOLD, 56);
        
        msgFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 42);
        if (msgFont == null) msgFont = Font.font("Arial", FontWeight.BOLD, 42);
        
        infoFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/space_mono.ttf"), 20);
        if (infoFont == null) infoFont = Font.font("Arial", FontWeight.BOLD, 20);

        nameFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/space_mono.ttf"), 12);
        if (nameFont == null) nameFont = Font.font("Arial", FontWeight.BOLD, 12);

        // --- Game assets ---
        Image floorTile = new Image(getClass().getResourceAsStream("/assets/tile.png"));
        try {
            bulletImg = new Image(getClass().getResourceAsStream("/assets/shooting/Charge.png"));
            plantsImg = new Image(getClass().getResourceAsStream("/assets/obstacles/Plants.png"));
            blueGaodImg = new Image(getClass().getResourceAsStream("/assets/enemy_charac/enemy_blue/Run.png"));
            redGaodImg = new Image(getClass().getResourceAsStream("/assets/enemy_charac/enemy_red/Run.png"));
        } catch (Exception e) {}

        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        javafx.scene.control.Button backBtn = makeBtn("MENU");
        backBtn.setFocusTraversable(false);
        backBtn.setOnAction(e -> {
            gameOver = true;
            gameLoop.stop();
            stage.setScene(new MenuScene(stage).getScene());
        });
        
        javafx.scene.layout.HBox topNav = new javafx.scene.layout.HBox(backBtn);
        topNav.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        topNav.setPadding(new javafx.geometry.Insets(4, 10, 0, 0));
        topNav.setPickOnBounds(false);
        
        StackPane root = new StackPane(canvas, topNav);
        root.getStyleClass().add("root");
        try {
            root.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {}
        
        scene = new Scene(root, WIDTH, HEIGHT);

        // Render tile
        gc.clearRect(0, 0, GameScene.WIDTH, GameScene.HEIGHT);
        drawFloor(gc, floorTile, GameScene.WIDTH, GameScene.HEIGHT);

        // Spawn solid obstacles
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 15; i++) { // solid trees
            double ox = 100 + rand.nextDouble() * (WIDTH - 200);
            double oy = 100 + rand.nextDouble() * (HEIGHT - 200);
            // Shifted sx to 448 and reduced crop width (sw) to 64
            obstacles.add(new Obstacle(ox, oy, 30, 30, "/assets/obstacles/Plants.png", true, 448, 0, 64, 96));
        }

        // Create players
        for (int i = 0; i < playerCount; i++) {
            players.add(new Player(i, "P" + (i + 1),
                    SPAWN_POINTS[i][0], SPAWN_POINTS[i][1]));
        }

        waveManager = new WaveManager(WIDTH, HEIGHT);
        inputHandler = new InputHandler(scene, players);

        startTime = System.currentTimeMillis();
        gameLoop = new GameLoop(this::update, () -> render(floorTile));
        gameLoop.start();

        // Continuous mouse tracking and continuous shooting for Player 1
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
            mouseDown = true;
            if (!players.isEmpty()) {
                players.get(0).setShooting(true);
            }
        });
        scene.setOnMouseReleased(e -> {
            mouseDown = false;
            if (!players.isEmpty()) {
                players.get(0).setShooting(false);
            }
        });

        stage.setOnCloseRequest(e -> gameLoop.stop());
    }

    // -------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------

    private void update() {
        if (gameOver)
            return;

        inputHandler.update();

        if (!players.isEmpty()) {
            players.get(0).setShooting(mouseDown || inputHandler.isKeyPressed(javafx.scene.input.KeyCode.SPACE));
        }

        // Update players
        for (Player p : players) {
            p.update(WIDTH, HEIGHT, obstacles);

            // Override P1 aim angle if mouse is tracked (to prevent movement snapping)
            if (p.getPlayerIndex() == 0 && mouseTracked && p.isAlive()) {
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

            // Unified auto-shooting for all players (P1 mouse/SPACE, others keyboard)
            if (p.canShoot()) {
                fireBullet(p, p.getAimAngle());
            }
        }

        // Spawn new Gaods from wave manager
        int oldLevel = waveManager.getCurrentLevel();
        gaods.addAll(waveManager.update(gaods));

        if (waveManager.getCurrentLevel() > oldLevel) {
            levelMessageTimer = 120; // showw level message
        }

        // Update Gaods
        for (Gaod g : gaods) {
            g.update(players, obstacles);

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
                    if (!g.isAlive())
                        score += 10;
                }
            }
        }

        // Remove dead/inactive objects
        gaods.removeIf(g -> !g.isAlive());
        bullets.removeIf(b -> !b.isActive());

        // Check game over — all players dead
        if (!anyPlayerAlive()) {
            gameOver = true;
            gameLoop.stop();
            core.ScoreManager.updateLocalHighScore(score);
        }

        if (levelMessageTimer > 0) {
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
        drawObstacles();
        drawBullets();
        drawGaods();
        drawPlayers();
        drawHUD();
        drawLevelMessage();

        if (gameOver)
            drawGameOver();
    }

    private void drawGrid() {
        gc.setStroke(Color.web("#16213e"));
        gc.setLineWidth(1);
        for (int x = 0; x < WIDTH; x += 40)
            gc.strokeLine(x, 0, x, HEIGHT);
        for (int y = 0; y < HEIGHT; y += 40)
            gc.strokeLine(0, y, WIDTH, y);
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

            if (p.getImage() != null) {
                Image img = p.isMoving() && p.getRunImage() != null ? p.getRunImage() : p.getImage();
                double frameHeight = img.getHeight();
                // Assume square frames for horizontal sprite sheet
                double frameWidth = frameHeight;
                int totalFrames = (int) Math.max(1, img.getWidth() / frameWidth);
                int currentFrame = (p.getAnimationTick() / 6) % totalFrames;
                double sx = currentFrame * frameWidth;
                double sy = 0;

                // Make the sprite bigger (e.g., 80x80) and center it on the player's hitbox
                double drawSize = 80;
                double drawX = x + s / 2 - drawSize / 2;
                double drawY = y + s / 2 - drawSize / 2;

                gc.drawImage(img, sx, sy, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
            } else {
                // Body
                gc.setFill(color);
                gc.fillOval(x, y, s, s);

                // Outline
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                gc.strokeOval(x, y, s, s);
            }

            // Name + health
            String nameText = p.getName() + " ❤".repeat(p.getHealth());
            gc.setFont(nameFont);
            
            // Draw a small background for readability
            gc.setFill(Color.web("#000000bb"));
            double textW = nameText.length() * 8.5;
            gc.fillRoundRect(x + s/2 - textW/2 - 4, y - 22, textW + 8, 18, 5, 5);

            gc.setFill(Color.WHITE);
            gc.fillText(nameText, x + s/2 - textW/2, y - 8);

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
            if (!g.isAlive())
                continue;
            double x = g.getX(), y = g.getY(), s = Gaod.SIZE;

            Image img = (g.getType() == Gaod.Type.RED) ? redGaodImg : blueGaodImg;
            if (img != null) {
                double frameHeight = img.getHeight();
                double frameWidth = frameHeight;
                int totalFrames = (int) Math.max(1, img.getWidth() / frameWidth);
                int currentFrame = (g.getAnimationTick() / 6) % totalFrames;
                double sx = currentFrame * frameWidth;
                
                double drawSize = 80; // larger than hitbox
                double drawX = x + s / 2 - drawSize / 2;
                double drawY = y + s / 2 - drawSize / 2;
                
                gc.drawImage(img, sx, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
            } else {
                // Fallback rendering
                gc.setFill(g.getType() == Gaod.Type.RED ? Color.web("#e94560") : Color.web("#4560e9"));
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

            // Health bar (only show for gaods with more than 1 max health)
            if (g.getMaxHealth() > 1) {
                double barW = 36;
                double barH = 5;
                double barX = x + s / 2.0 - barW / 2.0;
                double barY = y - 10;
                double hpRatio = (double) g.getHealth() / g.getMaxHealth();

                // Background
                gc.setFill(Color.web("#00000099"));
                gc.fillRoundRect(barX - 1, barY - 1, barW + 2, barH + 2, 3, 3);

                // Red underlay
                gc.setFill(Color.web("#e94560"));
                gc.fillRoundRect(barX, barY, barW, barH, 2, 2);

                // Green fill proportional to HP
                Color hpColor = hpRatio > 0.5 ? Color.web("#7fff00") : (hpRatio > 0.25 ? Color.web("#ffcc00") : Color.web("#ff4444"));
                gc.setFill(hpColor);
                gc.fillRoundRect(barX, barY, barW * hpRatio, barH, 2, 2);
            }
        }
    }

    private void drawBullets() {
        for (Bullet b : bullets) {
            if (b.isActive()) {
                if (bulletImg != null) {
                    double frameHeight = bulletImg.getHeight();
                    double frameWidth = frameHeight; // assume square frames
                    int totalFrames = (int) Math.max(1, bulletImg.getWidth() / frameWidth);
                    int currentFrame = (b.getAnimationTick() / 4) % totalFrames;
                    double sx = currentFrame * frameWidth;

                    double drawSize = 64; // visually larger than the actual hitbox
                    double drawX = b.getX() + Bullet.SIZE / 2.0 - drawSize / 2.0;
                    double drawY = b.getY() + Bullet.SIZE / 2.0 - drawSize / 2.0;

                    gc.save();
                    gc.translate(b.getX() + Bullet.SIZE / 2.0, b.getY() + Bullet.SIZE / 2.0);
                    gc.rotate(Math.toDegrees(b.getAngle()));
                    gc.translate(-(b.getX() + Bullet.SIZE / 2.0), -(b.getY() + Bullet.SIZE / 2.0));

                    gc.drawImage(bulletImg, sx, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
                    gc.restore();
                } else {
                    gc.setFill(Color.YELLOW);
                    gc.fillOval(b.getX(), b.getY(), Bullet.SIZE, Bullet.SIZE);
                }
            }
        }
    }

    private void drawHUD() {
        // Header background (Retro-box style)
        gc.setFill(Color.web("#1a0f0aee"));
        gc.fillRect(0, 0, WIDTH, 50);
        
        // Header Border
        gc.setStroke(Color.web("#c8600a"));
        gc.setLineWidth(3);
        gc.strokeRect(0, 0, WIDTH, 50);

        gc.setFill(Color.WHITE);
        gc.setFont(hudFont);
        gc.fillText("LEVEL " + waveManager.getCurrentLevel(), 20, 31);

        gc.setFill(Color.web("#ffcc00"));
        gc.fillText("⏱ " + waveManager.getSecondsRemaining() + "s", 140, 31);

        gc.setFill(Color.web("#00d4ff"));
        gc.fillText("SCORE: " + score, 260, 31);

        // Gaods alive
        long alive = gaods.stream().filter(Gaod::isAlive).count();
        gc.setFill(Color.web("#e94560"));
        gc.fillText("GAODS: " + alive, 410, 31);

        // Footer background
        gc.setFill(Color.web("#1a0f0aee"));
        gc.fillRect(0, HEIGHT - 35, WIDTH, 35);
        
        // Footer Border
        gc.setStroke(Color.web("#c8600a"));
        gc.strokeRect(0, HEIGHT - 35, WIDTH, 35);

        gc.setFill(Color.web("#FFE066"));
        gc.setFont(nameFont); // Use smaller retro font for footer
        gc.fillText("P1: WASD+Mouse  P2: Arrows+L  P3: IJKL+U  P4: Numpad", 20, HEIGHT - 12);
    }

    private void drawLevelMessage() {
        if (levelMessageTimer <= 0 || gameOver)
            return;

        gc.setFill(Color.web("#00000099"));
        gc.fillRoundRect(WIDTH / 2.0 - 160, HEIGHT / 2.0 - 60, 320, 100, 20, 20);

        gc.setFill(Color.web("#ffcc00"));
        gc.setFont(msgFont);
        gc.fillText("LEVEL " + waveManager.getCurrentLevel(), WIDTH / 2.0 - 100, HEIGHT / 2.0);
    }

    private void drawGameOver() {
        gc.setFill(Color.web("#000000bb"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#e94560"));
        gc.setFont(mainFont);
        gc.fillText("GAME OVER", WIDTH / 2.0 - 170, HEIGHT / 2.0 - 30);

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        gc.setFill(Color.WHITE);
        gc.setFont(infoFont);
        gc.fillText("Survived: " + elapsed + "s   |   Level: " +
                waveManager.getCurrentLevel() + "   |   Score: " + score,
                WIDTH / 2.0 - 280, HEIGHT / 2.0 + 20);

        gc.setFill(Color.web("#ffcc00"));
        gc.setFont(hudFont);
        gc.fillText("Click anywhere to return to menu", WIDTH / 2.0 - 160, HEIGHT / 2.0 + 60);
    }

    private void drawObstacles() {
        if (plantsImg == null) return;
        for (Obstacle o : obstacles) {
            double drawSizeW = o.getSw();
            double drawSizeH = o.getSh();
            double drawX = o.getX() + (o.getWidth() / 2.0) - (drawSizeW / 2.0);
            double drawY = o.getY() + o.getHeight() - drawSizeH;

            gc.drawImage(plantsImg, o.getSx(), o.getSy(), o.getSw(), o.getSh(), drawX, drawY, drawSizeW, drawSizeH);
        }
    }

    private void drawFloor(GraphicsContext gc, Image floorTile, int width, int height) {
        gc.setImageSmoothing(false); // keep pixel art crisp
        for (int y = 0; y < height; y += 32) {
            for (int x = 0; x < width; x += 32) {
                gc.drawImage(floorTile, x, y, 32, 32);
            }
        }
    }

    public Scene getScene() {
        return scene;
    }

    public List<Player> getPlayers() {
        return players;
    }

    private javafx.scene.control.Button makeBtn(String label) {
        javafx.scene.control.Button btn = new javafx.scene.control.Button(label);
        String base = "-fx-background-color:#EAC33E;-fx-text-fill:#1a0f0a;" +
                "-fx-font-weight:bold;-fx-font-size:16px;" +
                "-fx-padding:12px;" +
                "-fx-border-color:#1a0f0a;-fx-border-width:3px;" +
                "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
        String hover = "-fx-background-color:#1a0f0a;-fx-text-fill:#EAC33E;" +
                "-fx-font-weight:bold;-fx-font-size:16px;" +
                "-fx-padding:12px;" +
                "-fx-border-color:#EAC33E;-fx-border-width:3px;" +
                "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }
}
