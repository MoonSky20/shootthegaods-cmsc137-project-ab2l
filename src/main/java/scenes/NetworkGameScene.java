package scenes;

import core.GameLoop;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import network.GameClient;
import network.GamePacket;
import network.GameServer;

import java.util.HashSet;
import java.util.Set;

/**
 * NetworkGameScene — the in-game screen for networked play.
 *
 * Responsibilities:
 *   - Reads the latest GameStatePacket from our GameClient
 *   - Sends PlayerInputPacket every frame
 *   - Renders everything 
 */
public class NetworkGameScene {

    public static final int WIDTH  = 960;
    public static final int HEIGHT = 640;

    private static final double[][] SPAWN_POINTS = {
            { 80, 80 }, { 840, 80 }, { 80, 520 }, { 840, 520 }
    };

    private static final Color[] PLAYER_COLORS = {
            Color.web("#00d4ff"), Color.web("#ff6b35"),
            Color.web("#7fff00"), Color.web("#ff69b4"),
    };

    // ---- JavaFX ----
    private final Scene  scene;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Stage  stage;

    // ---- Network ----
    private final GameClient client;
    private final GameServer server;   // null for non-host
    private int myIndex;

    // ---- Input ----
    private final Set<KeyCode> keys = new HashSet<>();
    private double mouseX = WIDTH / 2.0, mouseY = HEIGHT / 2.0;
    private boolean mouseDown = false;

    // ---- Render loop ----
    private final GameLoop renderLoop;

    // ---- State ----
    private volatile GamePacket.GameStatePacket currentState = null;
    private boolean gameOver = false;
    private long    startTime = System.currentTimeMillis();
    private int     levelFlashTimer = 0;
    private int     lastLevel = 1;

    // ---- Assets & Fonts ----
    private Image floorTile;
    private Image blueGaodImg;
    private Image redGaodImg;
    private Image bulletImg;
    private Image plantsImg;

    private Font  hudFont;
    private Font  mainFont;
    private Font  msgFont;
    private Font  infoFont;
    private Font  nameFont;

    private static final String[] CHAR_IDLE_PATHS = {
            "/assets/characters/wizard_male/Idle.png",
            "/assets/characters/archer_male/Idle.png",
            "/assets/characters/swordsman/Idle.png",
            "/assets/female_charc/Enchantress/Idle.png",
            "/assets/female_charc/Musketeer/Idle.png"
    };

    private static final String[] CHAR_RUN_PATHS = {
            "/assets/characters/wizard_male/Run.png",
            "/assets/characters/archer_male/Run.png",
            "/assets/characters/swordsman/Run.png",
            "/assets/female_charc/Enchantress/Run.png",
            "/assets/female_charc/Musketeer/Run.png"
    };

    private final Image[] charIdleImages = new Image[5];
    private final Image[] charRunImages = new Image[5];

    private final double[][] lastPositions = new double[4][2];
    private final int[] animationTicks = new int[4];

    private final java.util.List<model.Obstacle> obstacles = new java.util.ArrayList<>();

    // -------------------------------------------------------
    public NetworkGameScene(Stage stage, GameServer server, GameClient client, int myIndex, long seed) {
        this.stage   = stage;
        this.server  = server;
        this.client  = client;
        this.myIndex = myIndex >= 0 ? myIndex : client.getAssignedIndex();

        canvas = new Canvas(WIDTH, HEIGHT);
        gc     = canvas.getGraphicsContext2D();

        // Top-right exit MENU button inside HBox overlay
        javafx.scene.control.Button backBtn = makeBtn("MENU");
        backBtn.setFocusTraversable(false);
        backBtn.setOnAction(e -> returnToMenu());

        javafx.scene.layout.HBox topNav = new javafx.scene.layout.HBox(backBtn);
        topNav.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        topNav.setPadding(new javafx.geometry.Insets(4, 10, 0, 0));
        topNav.setPickOnBounds(false);

        StackPane root = new StackPane(canvas, topNav);
        root.getStyleClass().add("root");
        try {
            root.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {}

        scene  = new Scene(root, WIDTH, HEIGHT);

        loadAssets();
        setupInput();
        setupNetworkCallbacks();

        // Spawn authoritative static obstacles locally using identical seed
        obstacles.clear();
        java.util.Random rand = new java.util.Random(seed);
        for (int i = 0; i < 15; i++) {
            double ox = 100 + rand.nextDouble() * (WIDTH - 200);
            double oy = 100 + rand.nextDouble() * (HEIGHT - 200);
            obstacles.add(new model.Obstacle(ox, oy, 30, 30, "/assets/obstacles/Plants.png", true, 448, 0, 64, 96));
        }

        renderLoop = new GameLoop(this::sendInput, this::render);
        renderLoop.start();

        stage.setOnCloseRequest(e -> cleanup());
    }

    // -------------------------------------------------------
    // Assets
    // -------------------------------------------------------

    private void loadAssets() {
        try {
            floorTile = new Image(getClass().getResourceAsStream("/assets/tile.png"));
            bulletImg = new Image(getClass().getResourceAsStream("/assets/shooting/Charge.png"));
            plantsImg = new Image(getClass().getResourceAsStream("/assets/obstacles/Plants.png"));
            blueGaodImg = new Image(getClass().getResourceAsStream("/assets/enemy_charac/enemy_blue/Run.png"));
            redGaodImg = new Image(getClass().getResourceAsStream("/assets/enemy_charac/enemy_red/Run.png"));

            for (int i = 0; i < 5; i++) {
                charIdleImages[i] = new Image(getClass().getResourceAsStream(CHAR_IDLE_PATHS[i]));
                charRunImages[i] = new Image(getClass().getResourceAsStream(CHAR_RUN_PATHS[i]));
            }
        } catch (Exception e) {
            System.err.println("Error loading NetworkGameScene assets: " + e.getMessage());
        }

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
    }

    // -------------------------------------------------------
    // Input setup
    // -------------------------------------------------------

    private void setupInput() {
        scene.setOnKeyPressed(e  -> keys.add(e.getCode()));
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        scene.setOnMouseMoved(e   -> { mouseX = e.getX(); mouseY = e.getY(); });
        scene.setOnMouseDragged(e -> { mouseX = e.getX(); mouseY = e.getY(); });
        scene.setOnMousePressed(e -> {
            mouseX    = e.getX();
            mouseY    = e.getY();
            mouseDown = true;
            if (gameOver) returnToMenu();
        });
        scene.setOnMouseReleased(e -> mouseDown = false);
    }

    // -------------------------------------------------------
    // Network callbacks
    // -------------------------------------------------------

    private void setupNetworkCallbacks() {
        client.setOnStateReceived(state -> {
            // Update volatile field only
            if (state.level > lastLevel) {
                lastLevel       = state.level;
                levelFlashTimer = 120;
            }
            currentState = state;
            if (state.gameOver) {
                if (!gameOver) {
                    core.ScoreManager.updateNetworkHighScore(state.score);
                }
                gameOver = true;
            }
        });

        client.setOnDisconnect(() -> Platform.runLater(() -> {
            gameOver = true;
        }));

        // Update myIndex if it wasn't set before game started
        client.setOnLobbyReceived(lobby -> {
            if (lobby.type == GamePacket.LobbyPacket.Type.ASSIGN_INDEX && myIndex < 0) {
                myIndex = lobby.assignedIndex;
            }
        });
    }

    // -------------------------------------------------------
    // Send input to server  (called as "update" by GameLoop)
    // -------------------------------------------------------

    private void sendInput() {
        int idx = myIndex >= 0 ? myIndex : client.getAssignedIndex();
        if (idx < 0) return;

        boolean up, down, left, right, shooting;

        // Each player uses the same bindings they would in local play,
        // but since they're on their own machine they always control their own index.
        // All players get mouse-aim + WASD/arrow fallback.
        shooting = mouseDown || keys.contains(KeyCode.SPACE);

        switch (idx) {
            case 0 -> {
                up    = keys.contains(KeyCode.W);
                down  = keys.contains(KeyCode.S);
                left  = keys.contains(KeyCode.A);
                right = keys.contains(KeyCode.D);
            }
            case 1 -> {
                up    = keys.contains(KeyCode.UP)    || keys.contains(KeyCode.W);
                down  = keys.contains(KeyCode.DOWN)  || keys.contains(KeyCode.S);
                left  = keys.contains(KeyCode.LEFT)  || keys.contains(KeyCode.A);
                right = keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.D);
                shooting |= keys.contains(KeyCode.L);
            }
            case 2 -> {
                up    = keys.contains(KeyCode.I) || keys.contains(KeyCode.W);
                down  = keys.contains(KeyCode.K) || keys.contains(KeyCode.S);
                left  = keys.contains(KeyCode.J) || keys.contains(KeyCode.A);
                right = keys.contains(KeyCode.L) || keys.contains(KeyCode.D);
                shooting |= keys.contains(KeyCode.U);
            }
            default -> {
                up    = keys.contains(KeyCode.NUMPAD8) || keys.contains(KeyCode.W);
                down  = keys.contains(KeyCode.NUMPAD2) || keys.contains(KeyCode.S);
                left  = keys.contains(KeyCode.NUMPAD4) || keys.contains(KeyCode.A);
                right = keys.contains(KeyCode.NUMPAD6) || keys.contains(KeyCode.D);
                shooting |= keys.contains(KeyCode.NUMPAD0);
            }
        }

        client.sendInput(new GamePacket.PlayerInputPacket(
                idx, up, down, left, right, shooting, mouseX, mouseY));
    }

    // -------------------------------------------------------
    // Render
    // -------------------------------------------------------

    private int gaodAnimTick = 0;
    private int bulletAnimTick = 0;

    private void render() {
        // Floor
        if (floorTile != null) {
            gc.setImageSmoothing(false);
            for (int y = 0; y < HEIGHT; y += 32)
                for (int x = 0; x < WIDTH; x += 32)
                    gc.drawImage(floorTile, x, y, 32, 32);
        } else {
            gc.setFill(Color.web("#1a1a2e"));
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            drawGrid();
        }

        // Draw authoritative synchronized obstacles
        drawObstacles();

        GamePacket.GameStatePacket state = currentState;
        if (state == null) {
            drawWaiting();
            return;
        }

        gaodAnimTick++;
        bulletAnimTick++;

        drawBullets(state);
        drawGaods(state);
        drawPlayers(state);
        drawReticle(state);
        drawHUD(state);
        drawLevelFlash(state);

        if (levelFlashTimer > 0) levelFlashTimer--;
        if (gameOver || state.gameOver) drawGameOver(state);
    }

    // -------------------------------------------------------
    // Draw helpers
    // -------------------------------------------------------

    private void drawGrid() {
        gc.setStroke(Color.web("#16213e"));
        gc.setLineWidth(1);
        for (int x = 0; x < WIDTH;  x += 40) gc.strokeLine(x, 0, x, HEIGHT);
        for (int y = 0; y < HEIGHT; y += 40) gc.strokeLine(0, y, WIDTH, y);
    }

    private void drawWaiting() {
        gc.setFill(Color.web("#FFE06699"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        gc.fillText("Waiting for game state...", WIDTH / 2.0 - 160, HEIGHT / 2.0);
    }

    private void drawObstacles() {
        if (plantsImg == null) return;
        for (model.Obstacle o : obstacles) {
            double drawSizeW = o.getSw();
            double drawSizeH = o.getSh();
            double drawX = o.getX() + (o.getWidth() / 2.0) - (drawSizeW / 2.0);
            double drawY = o.getY() + o.getHeight() - drawSizeH;
            gc.drawImage(plantsImg, o.getSx(), o.getSy(), o.getSw(), o.getSh(), drawX, drawY, drawSizeW, drawSizeH);
        }
    }

    private void drawPlayers(GamePacket.GameStatePacket state) {
        for (GamePacket.PlayerState ps : state.players) {
            if (!ps.alive) {
                gc.setFill(Color.web("#ffffff44"));
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
                int sec = Math.max(0, ps.respawnTimer / 60 + 1);
                gc.fillText("P" + (ps.index + 1) + " respawning in " + sec + "s",
                        SPAWN_POINTS[ps.index][0] - 20,
                        SPAWN_POINTS[ps.index][1] - 10);
                continue;
            }

            Color c = PLAYER_COLORS[Math.max(0, Math.min(ps.index, PLAYER_COLORS.length - 1))];
            double x = ps.x, y = ps.y, s = 40; // s = 40 matches server side Player.SIZE

            // Determine if player is moving
            int cacheIdx = Math.max(0, Math.min(ps.index, 3));
            double lastX = lastPositions[cacheIdx][0];
            double lastY = lastPositions[cacheIdx][1];
            boolean isMoving = (ps.x != lastX || ps.y != lastY);
            lastPositions[cacheIdx][0] = ps.x;
            lastPositions[cacheIdx][1] = ps.y;

            animationTicks[cacheIdx]++;

            // Glow ring for local player
            if (ps.index == myIndex) {
                gc.setFill(c.deriveColor(0, 1, 1, 0.18));
                gc.fillOval(x - 7, y - 7, s + 14, s + 14);
            }

            // Shadow
            gc.setFill(Color.web("#00000055"));
            gc.fillOval(x + 3, y + 3, s, s);

            Image img = isMoving ? charRunImages[ps.characterIndex] : charIdleImages[ps.characterIndex];
            if (img != null) {
                double frameHeight = img.getHeight();
                double frameWidth = frameHeight;
                int totalFrames = (int) Math.max(1, img.getWidth() / frameWidth);
                int currentFrame = (animationTicks[cacheIdx] / 6) % totalFrames;
                double sx = currentFrame * frameWidth;

                double drawSize = 80;
                double drawX = x + s / 2.0 - drawSize / 2.0;
                double drawY = y + s / 2.0 - drawSize / 2.0;

                gc.setImageSmoothing(false);
                gc.drawImage(img, sx, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
            } else {
                // Fallback body
                gc.setFill(c);
                gc.fillOval(x, y, s, s);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(ps.index == myIndex ? 2.5 : 1.5);
                gc.strokeOval(x, y, s, s);
            }

            // Name + health readability bubble
            String nameText = "P" + (ps.index + 1) + " ❤".repeat(Math.max(0, ps.health));
            gc.setFont(nameFont);

            gc.setFill(Color.web("#000000bb"));
            double textW = nameText.length() * 8.5;
            gc.fillRoundRect(x + s/2.0 - textW/2.0 - 4, y - 22, textW + 8, 18, 5, 5);

            gc.setFill(Color.WHITE);
            gc.fillText(nameText, x + s/2.0 - textW/2.0, y - 8);

            // Aim line
            gc.setStroke(c.deriveColor(0, 1, 1, 0.55));
            gc.setLineWidth(1.5);
            double cx = x + s / 2, cy = y + s / 2;
            gc.strokeLine(cx, cy,
                    cx + Math.cos(ps.aimAngle) * 20,
                    cy + Math.sin(ps.aimAngle) * 20);
        }
    }

    private void drawGaods(GamePacket.GameStatePacket state) {
        for (GamePacket.GaodState gs : state.gaods) {
            if (!gs.alive) continue;
            double x = gs.x, y = gs.y, s = 40; // Gaod.SIZE is 40

            Image img = (gs.type == 1) ? redGaodImg : blueGaodImg;
            if (img != null) {
                double frameHeight = img.getHeight();
                double frameWidth = frameHeight;
                int totalFrames = (int) Math.max(1, img.getWidth() / frameWidth);
                int currentFrame = (gaodAnimTick / 6) % totalFrames;
                double sx = currentFrame * frameWidth;

                double drawSize = 80;
                double drawX = x + s / 2.0 - drawSize / 2.0;
                double drawY = y + s / 2.0 - drawSize / 2.0;

                gc.setImageSmoothing(false);
                gc.drawImage(img, sx, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
            } else {
                gc.setFill(gs.type == 1 ? Color.web("#e94560") : Color.web("#4560e9"));
                gc.fillRect(x, y, s, s);

                // Eyes
                gc.setFill(Color.YELLOW);
                gc.fillOval(x + 5, y + 6, 5, 5);
                gc.fillOval(x + 14, y + 6, 5, 5);

                gc.setStroke(Color.web("#ff0000"));
                gc.setLineWidth(1);
                gc.strokeRect(x, y, s, s);
            }

            // Health bar (only show for gaods with more than 1 max health)
            if (gs.maxHealth > 1) {
                double barW = 36;
                double barH = 5;
                double barX = x + s / 2.0 - barW / 2.0;
                double barY = y - 10;
                double hpRatio = (double) gs.health / gs.maxHealth;

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

    private void drawBullets(GamePacket.GameStatePacket state) {
        for (GamePacket.BulletState bs : state.bullets) {
            if (!bs.active) continue;
            if (bulletImg != null) {
                double frameHeight = bulletImg.getHeight();
                double frameWidth = frameHeight;
                int totalFrames = (int) Math.max(1, bulletImg.getWidth() / frameWidth);
                int currentFrame = (bulletAnimTick / 4) % totalFrames;
                double sx = currentFrame * frameWidth;

                double drawSize = 64;
                double drawX = bs.x + 8 / 2.0 - drawSize / 2.0; // Bullet.SIZE is 8
                double drawY = bs.y + 8 / 2.0 - drawSize / 2.0;

                gc.save();
                gc.translate(bs.x + 8 / 2.0, bs.y + 8 / 2.0);
                gc.rotate(Math.toDegrees(bs.angle));
                gc.translate(-(bs.x + 8 / 2.0), -(bs.y + 8 / 2.0));

                gc.setImageSmoothing(false);
                gc.drawImage(bulletImg, sx, 0, frameWidth, frameHeight, drawX, drawY, drawSize, drawSize);
                gc.restore();
            } else {
                gc.setFill(Color.YELLOW);
                gc.fillOval(bs.x, bs.y, 8, 8);
            }
        }
    }

    // Crosshair at mouse cursor for the local player. 
    private void drawReticle(GamePacket.GameStatePacket state) {
        int idx = myIndex >= 0 ? myIndex : client.getAssignedIndex();
        if (idx < 0 || idx >= state.players.size()) return;
        GamePacket.PlayerState me = state.players.get(idx);
        if (!me.alive) return;

        Color c = PLAYER_COLORS[Math.max(0, Math.min(idx, PLAYER_COLORS.length - 1))];

        // Faint line from player center to cursor
        gc.setStroke(c.deriveColor(0, 1, 1, 0.22));
        gc.setLineWidth(1);
        gc.strokeLine(me.x + 20, me.y + 20, mouseX, mouseY);

        // Crosshair
        double r = 12;
        gc.setStroke(mouseDown ? c : c.deriveColor(0, 1, 1, 0.75));
        gc.setLineWidth(mouseDown ? 2.0 : 1.5);
        gc.strokeLine(mouseX - r, mouseY,     mouseX + r, mouseY);
        gc.strokeLine(mouseX,     mouseY - r, mouseX,     mouseY + r);
        gc.strokeOval(mouseX - 6, mouseY - 6, 12, 12);

        gc.setFill(c);
        gc.fillOval(mouseX - 2, mouseY - 2, 4, 4);
    }

    private void drawHUD(GamePacket.GameStatePacket state) {
        // Header background (Retro-box style)
        gc.setFill(Color.web("#1a0f0aee"));
        gc.fillRect(0, 0, WIDTH, 50);

        // Header Border
        gc.setStroke(Color.web("#c8600a"));
        gc.setLineWidth(3);
        gc.strokeRect(0, 0, WIDTH, 50);

        gc.setFill(Color.WHITE);
        gc.setFont(hudFont);
        gc.fillText("LEVEL " + state.level, 20, 31);

        gc.setFill(Color.web("#ffcc00"));
        gc.fillText("⏱ " + state.secondsRemaining + "s", 140, 31);

        gc.setFill(Color.web("#00d4ff"));
        gc.fillText("SCORE: " + state.score, 260, 31);

        // Gaods alive
        long alive = state.gaods.stream().filter(g -> g.alive).count();
        gc.setFill(Color.web("#e94560"));
        gc.fillText("GAODS: " + alive, 410, 31);

        // "You are P?" indicator top-right (slightly shifted to the left of MENU button)
        int drawIdx = myIndex >= 0 ? myIndex : client.getAssignedIndex();
        if (drawIdx >= 0 && drawIdx < PLAYER_COLORS.length) {
            gc.setFill(PLAYER_COLORS[drawIdx]);
            gc.fillText("YOU = P" + (drawIdx + 1), WIDTH - 220, 31);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillText("YOU = P?", WIDTH - 220, 31);
        }

        // Footer background
        gc.setFill(Color.web("#1a0f0aee"));
        gc.fillRect(0, HEIGHT - 35, WIDTH, 35);

        // Footer Border (thick retro style identical to header)
        gc.setStroke(Color.web("#c8600a"));
        gc.setLineWidth(3);
        gc.strokeRect(0, HEIGHT - 35, WIDTH, 35);

        gc.setFill(Color.web("#FFE066"));
        gc.setFont(nameFont); // Use smaller retro font for footer
        gc.fillText("WASD / Arrows to move  •  Hold Mouse or SPACE to shoot toward cursor", 20, HEIGHT - 12);
    }

    private void drawLevelFlash(GamePacket.GameStatePacket state) {
        if (levelFlashTimer <= 0) return;
        gc.setFill(Color.web("#00000099"));
        gc.fillRoundRect(WIDTH / 2.0 - 160, HEIGHT / 2.0 - 60, 320, 100, 20, 20);

        gc.setFill(Color.web("#ffcc00"));
        gc.setFont(msgFont);
        gc.fillText("LEVEL " + state.level, WIDTH / 2.0 - 100, HEIGHT / 2.0);
    }

    private void drawGameOver(GamePacket.GameStatePacket state) {
        gc.setFill(Color.web("#000000bb"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#e94560"));
        gc.setFont(mainFont);
        gc.fillText("GAME OVER", WIDTH / 2.0 - 170, HEIGHT / 2.0 - 30);

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        gc.setFill(Color.WHITE);
        gc.setFont(infoFont);
        gc.fillText("Survived: " + elapsed + "s   |   Level: " +
                        state.level +   "   |   Score: " + state.score,
                WIDTH / 2.0 - 280, HEIGHT / 2.0 + 20);

        gc.setFill(Color.web("#ffcc00"));
        gc.setFont(hudFont);
        gc.fillText("Click anywhere to return to menu", WIDTH / 2.0 - 160, HEIGHT / 2.0 + 60);
    }

    // -------------------------------------------------------
    // Cleanup & Helpers
    // -------------------------------------------------------

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

    private void returnToMenu() {
        cleanup();
        Platform.runLater(() -> stage.setScene(new MenuScene(stage).getScene()));
    }

    private void cleanup() {
        renderLoop.stop();
        if (client != null) client.disconnect();
        if (server != null) server.stop();
    }

    public Scene getScene() { return scene; }
}