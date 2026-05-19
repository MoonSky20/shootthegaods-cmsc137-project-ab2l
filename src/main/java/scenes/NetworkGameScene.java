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
 *   - Renders everything using the same pixel-art style as GameScene
 *
 * No simulation happens here — the server is authoritative.
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

    // ---- Assets ----
    private Image floorTile;
    private Font  hudFont;
    private Font  bigFont;

    // -------------------------------------------------------
    public NetworkGameScene(Stage stage, GameServer server, GameClient client, int myIndex) {
        this.stage   = stage;
        this.server  = server;
        this.client  = client;
        this.myIndex = myIndex >= 0 ? myIndex : client.getAssignedIndex();

        canvas = new Canvas(WIDTH, HEIGHT);
        gc     = canvas.getGraphicsContext2D();
        scene  = new Scene(new StackPane(canvas), WIDTH, HEIGHT);

        loadAssets();
        setupInput();
        setupNetworkCallbacks();

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
        } catch (Exception e) {
            floorTile = null; // fallback to solid color
        }
        hudFont = Font.font("Arial", FontWeight.BOLD, 14);
        bigFont = Font.font("Arial", FontWeight.BOLD, 42);
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
            // Received on background thread — update volatile field only
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
    // Render  (called as "render" by GameLoop)
    // -------------------------------------------------------

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

        GamePacket.GameStatePacket state = currentState;
        if (state == null) {
            drawWaiting();
            return;
        }

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

            Color c = PLAYER_COLORS[ps.index];
            double x = ps.x, y = ps.y, s = 28;

            // Glow ring for local player
            if (ps.index == myIndex) {
                gc.setFill(c.deriveColor(0, 1, 1, 0.18));
                gc.fillOval(x - 7, y - 7, s + 14, s + 14);
            }

            // Shadow
            gc.setFill(Color.web("#00000055"));
            gc.fillOval(x + 3, y + 3, s, s);

            // Body
            gc.setFill(c);
            gc.fillOval(x, y, s, s);

            // Outline (thicker for local player)
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(ps.index == myIndex ? 2.5 : 1.5);
            gc.strokeOval(x, y, s, s);

            // Name + health
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 11));
            String tag = ps.index == myIndex ? "►" : "";
            gc.fillText(tag + "P" + (ps.index + 1) + " ❤".repeat(Math.max(0, ps.health)),
                x - 5, y - 6);

            // Aim line
            gc.setStroke(c.deriveColor(0, 1, 1, 0.55));
            gc.setLineWidth(1.5);
            double cx = x + s / 2, cy = y + s / 2;
            gc.strokeLine(cx, cy,
                cx + Math.cos(ps.aimAngle) * 22,
                cy + Math.sin(ps.aimAngle) * 22);
        }
    }

    private void drawGaods(GamePacket.GameStatePacket state) {
        for (GamePacket.GaodState gs : state.gaods) {
            if (!gs.alive) continue;
            double x = gs.x, y = gs.y, s = 24;

            gc.setFill(Color.web("#e94560"));
            gc.fillRect(x, y, s, s);

            // Eyes
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + 5, y + 6, 5, 5);
            gc.fillOval(x + 14, y + 6, 5, 5);

            // Angry brows
            gc.setStroke(Color.web("#cc0000"));
            gc.setLineWidth(1.5);
            gc.strokeLine(x + 4,  y + 4, x + 10, y + 7);
            gc.strokeLine(x + 13, y + 7, x + 20, y + 4);

            gc.setStroke(Color.web("#ff0000"));
            gc.setLineWidth(1);
            gc.strokeRect(x, y, s, s);
        }
    }

    private void drawBullets(GamePacket.GameStatePacket state) {
        for (GamePacket.BulletState bs : state.bullets) {
            if (!bs.active) continue;
            Color oc = PLAYER_COLORS[Math.min(bs.ownerIndex, PLAYER_COLORS.length - 1)];
            // Glow
            gc.setFill(oc.deriveColor(0, 1, 1, 0.35));
            gc.fillOval(bs.x - 3, bs.y - 3, 14, 14);
            // Core
            gc.setFill(Color.YELLOW);
            gc.fillOval(bs.x, bs.y, 8, 8);
        }
    }

    /** Crosshair at mouse cursor for the local player. */
    private void drawReticle(GamePacket.GameStatePacket state) {
        int idx = myIndex >= 0 ? myIndex : client.getAssignedIndex();
        if (idx < 0 || idx >= state.players.size()) return;
        GamePacket.PlayerState me = state.players.get(idx);
        if (!me.alive) return;

        Color c = PLAYER_COLORS[idx];

        // Faint line from player center to cursor
        gc.setStroke(c.deriveColor(0, 1, 1, 0.22));
        gc.setLineWidth(1);
        gc.strokeLine(me.x + 14, me.y + 14, mouseX, mouseY);

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
        // Top bar
        gc.setFill(Color.web("#00000099"));
        gc.fillRect(0, 0, WIDTH, 36);

        gc.setFill(Color.web("#FFE066"));
        gc.setFont(hudFont);
        gc.fillText("LEVEL " + state.level, 10, 22);

        gc.setFill(Color.web("#ffcc00"));
        gc.fillText("⏱ " + state.secondsRemaining + "s", 140, 22);

        gc.setFill(Color.web("#00d4ff"));
        gc.fillText("SCORE: " + state.score, 260, 22);

        long alive = state.gaods.stream().filter(g -> g.alive).count();
        gc.setFill(Color.web("#e94560"));
        gc.fillText("GAODS: " + alive, 440, 22);

        // "You are P?" indicator top-right
        gc.setFill(PLAYER_COLORS[Math.min(myIndex, PLAYER_COLORS.length - 1)]);
        gc.fillText("YOU = P" + (myIndex + 1), WIDTH - 110, 22);

        // Bottom bar
        gc.setFill(Color.web("#00000099"));
        gc.fillRect(0, HEIGHT - 24, WIDTH, 24);
        gc.setFill(Color.web("#ffffff88"));
        gc.setFont(Font.font("Arial", 11));
        gc.fillText("WASD / Arrows to move  •  Hold Mouse or SPACE to shoot toward cursor", 10, HEIGHT - 7);
    }

    private void drawLevelFlash(GamePacket.GameStatePacket state) {
        if (levelFlashTimer <= 0) return;
        double alpha = Math.min(1.0, levelFlashTimer / 40.0);
        String hex   = String.format("%02x", (int)(alpha * 255));

        gc.setFill(Color.web("#00000099"));
        gc.fillRoundRect(WIDTH / 2.0 - 170, HEIGHT / 2.0 - 60, 340, 110, 20, 20);

        gc.setFill(Color.web("#FFE066" + hex));
        gc.setFont(bigFont);
        gc.fillText("LEVEL " + state.level, WIDTH / 2.0 - 110, HEIGHT / 2.0 + 10);
    }

    private void drawGameOver(GamePacket.GameStatePacket state) {
        gc.setFill(Color.web("#000000cc"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#e94560"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        gc.fillText("GAME OVER", WIDTH / 2.0 - 175, HEIGHT / 2.0 - 30);

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        gc.fillText("Survived: " + elapsed + "s  |  Level: " + state.level + "  |  Score: " + state.score,
            WIDTH / 2.0 - 220, HEIGHT / 2.0 + 20);

        gc.setFill(Color.web("#FFE066"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.fillText("Click anywhere to return to menu", WIDTH / 2.0 - 145, HEIGHT / 2.0 + 60);
    }

    // -------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------

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
