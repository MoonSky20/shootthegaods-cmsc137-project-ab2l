package scenes;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import network.GameClient;
import network.GamePacket;
import network.GameServer;

/**
 * LobbyScene — the waiting room.
 *
 * HOST path  (playerCount > 1, no serverIP given):
 *   1. Creates a GameServer on port 55000
 *   2. Connects a local GameClient to 127.0.0.1
 *   3. Shows LAN IP + slot indicators; waits for others to join
 *   4. Server auto-starts when all slots fill → switches to NetworkGameScene
 *
 * JOIN path  (serverIP supplied):
 *   1. Connects a GameClient to the given IP
 *   2. Shows waiting UI; switches to NetworkGameScene on START_GAME
 *
 * SOLO path  (playerCount == 1):
 *   Goes straight to the local GameScene (no network needed).
 */
public class LobbyScene {

    private static final Color COLOR_GOLD   = Color.web("#FFE066");
    private static final Color COLOR_ORANGE = Color.web("#c8600a");
    private static final Color COLOR_DARK   = Color.web("#3b1a08");
    private static final Color COLOR_DIM    = Color.web("#ffffff44");

    // Player slot colours (matches GameScene / NetworkGameScene)
    private static final Color[] SLOT_COLORS = {
        Color.web("#00d4ff"), Color.web("#ff6b35"),
        Color.web("#7fff00"), Color.web("#ff69b4"),
    };

    private static final String[] CHARACTER_PATHS = {
        "/assets/characters/wizard_male/Idle.png",
        "/assets/characters/archer_male/Idle.png",
        "/assets/characters/swordsman/Idle.png",
        "/assets/female_charc/Enchantress/Idle.png",
        "/assets/female_charc/Musketeer/Idle.png"
    };

    private final Scene  scene;
    private final Stage  stage;

    // Labels we update live
    private Text statusText;
    private Text ipText;
    private final Text[] slotLabels = new Text[4];

    private GameServer server;
    private GameClient client;
    private int myIndex = -1;

    // Character choice state
    private final int[] chosenCharacters = new int[4]; // default Wizard = 0
    private final boolean[] hasChosenClass = new boolean[4];
    private final Button[] charButtons = new Button[5];
    private boolean hasChosen = false;
    private int currentConnectedCount = 0;
    private int currentTargetCount = 0;
    private final Canvas[] slotCanvases = new Canvas[4];
    private AnimationTimer timer;
    private final Image[] characterImages = new Image[5];

    // -------------------------------------------------------
    // Constructor — HOST flow
    // -------------------------------------------------------
    public LobbyScene(Stage stage, int playerCount) {
        this.stage = stage;

        initLobby(playerCount);

        Canvas canvas    = new Canvas(GameScene.WIDTH, GameScene.HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBackground(gc);

        StackPane root = buildUI(canvas, playerCount);
        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);
        applyCSS(root);

        if (playerCount == 1) {
            // No networking needed — go straight to local game
            Platform.runLater(() -> {
                cleanup();
                GameScene gs = new GameScene(stage, 1);
                stage.setScene(gs.getScene());
            });
            return;
        }

        startAsHost(playerCount);
    }

    // -------------------------------------------------------
    // Constructor — JOIN flow (called from MenuScene "Join" path)
    // -------------------------------------------------------
    public LobbyScene(Stage stage, String serverIP) {
        this.stage = stage;

        initLobby(-1);

        Canvas canvas      = new Canvas(GameScene.WIDTH, GameScene.HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawBackground(gc);

        // We don't know playerCount yet — show generic waiting UI
        StackPane root = buildUI(canvas, -1);
        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);
        applyCSS(root);

        startAsJoiner(serverIP);
    }

    // -------------------------------------------------------
    // UI construction
    // -------------------------------------------------------

    private void initLobby(int targetCount) {
        this.currentTargetCount = targetCount;
        for (int i = 0; i < 5; i++) {
            try {
                characterImages[i] = new Image(getClass().getResourceAsStream(CHARACTER_PATHS[i]));
            } catch (Exception e) {
                System.err.println("Error loading character image: " + CHARACTER_PATHS[i]);
            }
        }

        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            private int tick = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 100_000_000) { // ~10 updates per second
                    tick++;
                    for (int i = 0; i < 4; i++) {
                        drawSlot(i, tick);
                    }
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }

    private void drawSlot(int i, int tick) {
        Canvas box = slotCanvases[i];
        if (box == null) return;
        GraphicsContext gc = box.getGraphicsContext2D();
        double w = box.getWidth();
        double h = box.getHeight();

        gc.clearRect(0, 0, w, h);

        boolean needed = (currentTargetCount < 0) || (i < currentTargetCount);
        boolean connected = (i < currentConnectedCount);

        Color c = SLOT_COLORS[i];
        // Border
        gc.setStroke(needed ? c : Color.web("#333333"));
        gc.setLineWidth(3);
        gc.strokeRect(2, 2, w - 4, h - 4);

        // Fill background
        gc.setFill(needed ? c.deriveColor(0, 1, 0.15, 1) : Color.web("#1a1a1a"));
        gc.fillRect(3, 3, w - 6, h - 6);

        if (connected && needed) {
            int charIdx = chosenCharacters[i];
            Image img = characterImages[charIdx];
            if (img != null) {
                double frameHeight = img.getHeight();
                double frameWidth = frameHeight; // square frames
                int totalFrames = (int) Math.max(1, img.getWidth() / frameWidth);
                int currentFrame = tick % totalFrames;

                double drawW = 80;
                double drawH = 80;
                double drawX = (w - drawW) / 2.0;
                double drawY = (h - drawH) / 2.0 - 5; // offset up slightly for text/label

                gc.setImageSmoothing(false);
                gc.drawImage(img,
                             currentFrame * frameWidth, 0, frameWidth, frameHeight,
                             drawX, drawY, drawW, drawH);
            } else {
                gc.setFill(c);
                gc.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 32));
                gc.fillText("P" + (i + 1), 35, 58);
            }
        } else {
            gc.setFill(Color.web("#444444"));
            gc.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 32));
            gc.fillText("P" + (i + 1), 35, 58);
        }
    }

    private void disableSelectorButtons() {
        for (Button btn : charButtons) {
            btn.setDisable(true);
            btn.setStyle("-fx-background-color:#1a0f0a;-fx-text-fill:#555555;-fx-border-color:#444444;-fx-border-width:3px;-fx-background-radius:0;-fx-border-radius:0;");
        }
    }

    private StackPane buildUI(Canvas canvas, int targetCount) {
        Font mainFont = Font.loadFont(
            getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 42);
        Font subFont  = Font.loadFont(
            getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 14);
        if (mainFont == null) mainFont = Font.font("Arial", 42);
        if (subFont  == null) subFont  = Font.font("Arial", 14);

        DropShadow shadow = pixelShadow();

        // Title
        Text title = new Text("WAITING FOR PLAYERS");
        title.setFont(mainFont);
        title.setFill(COLOR_GOLD);
        title.setEffect(shadow);

        // IP line (filled in once server starts)
        ipText = new Text("Starting server...");
        ipText.setFont(subFont);
        ipText.setFill(COLOR_GOLD);
        ipText.setEffect(shadow);

        // Status line
        statusText = new Text("0 / " + (targetCount > 0 ? targetCount : "?") + " players connected");
        statusText.setFont(subFont);
        statusText.setFill(COLOR_GOLD);
        statusText.setEffect(shadow);

        // Player slot boxes
        HBox slots = new HBox(20);
        slots.setAlignment(Pos.CENTER);
        for (int i = 0; i < 4; i++) {
            boolean needed = (targetCount < 0) || (i < targetCount);
            slotLabels[i] = buildSlotLabel(i, needed, subFont);
            slots.getChildren().add(buildSlotBox(i, needed, slotLabels[i]));
        }

        // Character selector container
        Text selectTitle = new Text("CHOOSE YOUR CHARACTER");
        selectTitle.setFont(subFont);
        selectTitle.setFill(COLOR_GOLD);
        selectTitle.setEffect(shadow);

        HBox selectorRow = new HBox(12);
        selectorRow.setAlignment(Pos.CENTER);
        String[] classes = {"Wizard", "Archer", "Swordsman", "Enchantress", "Musketeer"};
        for (int cIdx = 0; cIdx < 5; cIdx++) {
            final int index = cIdx;
            Button btn = styledClassButton(classes[cIdx]);
            btn.setOnAction(e -> {
                if (myIndex == -1 || hasChosen) return;
                hasChosen = true;

                GamePacket.LobbyPacket pkt = new GamePacket.LobbyPacket(GamePacket.LobbyPacket.Type.SELECT_CHARACTER);
                pkt.characterIndex = index;
                client.sendLobby(pkt);

                disableSelectorButtons();
            });
            charButtons[cIdx] = btn;
            selectorRow.getChildren().add(btn);
        }

        VBox selectorBox = new VBox(10, selectTitle, selectorRow);
        selectorBox.setAlignment(Pos.CENTER);

        // Cancel / back button
        Button cancelBtn = styledButton("← Cancel");
        cancelBtn.setOnAction(e -> {
            cleanup();
            MenuScene menu = new MenuScene(stage);
            stage.setScene(menu.getScene());
        });

        VBox content = new VBox(20, title, ipText, statusText, slots, selectorBox, cancelBtn);
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(canvas, content);
        StackPane.setAlignment(content, Pos.CENTER);
        return root;
    }

    private StackPane buildSlotBox(int i, boolean needed, Text label) {
        Canvas box = new Canvas(110, 110);
        slotCanvases[i] = box;

        GraphicsContext gc = box.getGraphicsContext2D();
        Color c = SLOT_COLORS[i];
        // Border
        gc.setStroke(needed ? c : Color.web("#333333"));
        gc.setLineWidth(3);
        gc.strokeRect(2, 2, 106, 106);
        // Fill dimmed
        gc.setFill(needed ? c.deriveColor(0, 1, 0.15, 1) : Color.web("#1a1a1a"));
        gc.fillRect(3, 3, 104, 104);
        // Player number
        gc.setFill(needed ? c : Color.web("#444444"));
        gc.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 32));
        gc.fillText("P" + (i + 1), 35, 58);

        StackPane sp = new StackPane(box, label);
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
        return sp;
    }

    private Text buildSlotLabel(int i, boolean needed, Font font) {
        Text t = new Text(needed ? "Waiting..." : "—");
        t.setFont(font != null ? font : Font.font("Arial", 13));
        t.setFill(needed ? Color.web("#ffffff88") : Color.web("#333333"));
        t.setTranslateY(-6);
        return t;
    }

    // -------------------------------------------------------
    // HOST flow
    // -------------------------------------------------------

    private void startAsHost(int targetCount) {
        server = new GameServer(targetCount);

        server.setOnPlayerJoined(count -> Platform.runLater(() -> {
            updateSlots(count, targetCount);
            statusText.setText(count + " / " + targetCount + " players connected");
        }));

        server.setOnGameStarted(() -> Platform.runLater(() -> {
            // Host's local client has already received START_GAME via its lobby callback
        }));

        // Start server
        try {
            server.start();
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusText.setText("Failed to start server: " + e.getMessage());
                ipText.setText("Check that port 55000 is free.");
            });
            return;
        }

        String ip = server.getLocalIP();
        Platform.runLater(() -> ipText.setText("Your IP: " + ip + "  •  Port: " + GameServer.PORT + "  •  Share this with friends!"));

        // Host connects as a regular client to localhost
        client = new GameClient("127.0.0.1", GameServer.PORT);
        setupClientCallbacks(targetCount);

        Thread t = new Thread(() -> {
            try {
                client.connect();
                // index is assigned via callback
            } catch (Exception ex) {
                Platform.runLater(() -> statusText.setText("Host self-connect failed: " + ex.getMessage()));
            }
        }, "host-self-connect");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------
    // JOIN flow
    // -------------------------------------------------------

    private void startAsJoiner(String serverIP) {
        Platform.runLater(() -> {
            ipText.setText("Connecting to " + serverIP + ":" + GameServer.PORT + " ...");
            statusText.setText("Waiting for host to start the game...");
        });

        client = new GameClient(serverIP, GameServer.PORT);
        setupClientCallbacks(-1);

        Thread t = new Thread(() -> {
            try {
                client.connect();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    ipText.setText("Could not connect to " + serverIP);
                    statusText.setText("Error: " + ex.getMessage());
                });
            }
        }, "join-connect");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------
    // Shared client callbacks
    // -------------------------------------------------------

    private void setupClientCallbacks(int targetCount) {
        client.setOnLobbyReceived(lobby -> Platform.runLater(() -> {
            switch (lobby.type) {
                case ASSIGN_INDEX -> {
                    myIndex = lobby.assignedIndex;
                    int tc  = lobby.targetCount;
                    statusText.setText(lobby.connectedCount + " / " + tc + " players connected");
                    updateSlots(lobby.connectedCount, tc);
                }
                case PLAYER_JOINED -> {
                    statusText.setText(lobby.connectedCount + " / " + lobby.targetCount + " players connected");
                    updateSlots(lobby.connectedCount, lobby.targetCount);
                }
                case PLAYER_LEFT -> {
                    statusText.setText(lobby.connectedCount + " / " + lobby.targetCount + " players connected");
                    updateSlots(lobby.connectedCount, lobby.targetCount);
                }
                case SELECT_CHARACTER -> {
                    int pIdx = lobby.assignedIndex;
                    if (pIdx >= 0 && pIdx < 4) {
                        chosenCharacters[pIdx] = lobby.characterIndex;
                        hasChosenClass[pIdx] = true;
                        updateSlots(currentConnectedCount, currentTargetCount);
                    }
                }
                case START_GAME -> {
                    if (timer != null) timer.stop();
                    NetworkGameScene ngs = new NetworkGameScene(stage, server, client, myIndex, lobby.seed);
                    stage.setScene(ngs.getScene());
                }
                case HOST_DISCONNECTED -> {
                    cleanup();
                    statusText.setText("Host disconnected. Returning to menu...");
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> stage.setScene(new MenuScene(stage).getScene()));
                    }).start();
                }
                default -> {}
            }
        }));

        client.setOnDisconnect(() -> Platform.runLater(() -> {
            statusText.setText("Disconnected from server.");
        }));
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /** Marks the first N slots as "Connected", rest as "Waiting" or "—". */
    private void updateSlots(int connected, int target) {
        this.currentConnectedCount = connected;
        this.currentTargetCount = target;
        for (int i = 0; i < 4; i++) {
            if (i >= target) {
                slotLabels[i].setText("—");
            } else if (i < connected) {
                if (hasChosenClass[i]) {
                    String[] classes = {"Wizard", "Archer", "Swordsman", "Enchantress", "Musketeer"};
                    int charIdx = Math.max(0, Math.min(chosenCharacters[i], classes.length - 1));
                    String className = classes[charIdx];
                    if (i == myIndex) {
                        slotLabels[i].setText("YOU (" + className + ") ✔");
                    } else {
                        slotLabels[i].setText("P" + (i + 1) + " (" + className + ") ✔");
                    }
                    slotLabels[i].setFill(COLOR_GOLD);
                } else {
                    if (i == myIndex) {
                        slotLabels[i].setText("YOU  ✔");
                        slotLabels[i].setFill(COLOR_GOLD);
                    } else {
                        slotLabels[i].setText("Connected ✔");
                        slotLabels[i].setFill(COLOR_GOLD);
                    }
                }
            } else {
                slotLabels[i].setText("Waiting...");
                slotLabels[i].setFill(COLOR_DIM);
            }
        }
    }

    private void cleanup() {
        if (timer != null) timer.stop();
        if (client != null) client.disconnect();
        if (server != null) server.stop();
    }

    private void drawBackground(GraphicsContext gc) {
        gc.setFill(Color.web("#1a0a00"));
        gc.fillRect(0, 0, GameScene.WIDTH, GameScene.HEIGHT);
        // Subtle grid
        gc.setStroke(Color.web("#2a1500"));
        gc.setLineWidth(1);
        for (int x = 0; x < GameScene.WIDTH;  x += 40) gc.strokeLine(x, 0, x, GameScene.HEIGHT);
        for (int y = 0; y < GameScene.HEIGHT; y += 40) gc.strokeLine(0, y, GameScene.WIDTH, y);
    }

    private DropShadow pixelShadow() {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web("#5a1a00"));
        ds.setRadius(0); ds.setSpread(0);
        ds.setOffsetX(3); ds.setOffsetY(3);
        return ds;
    }

    private Button styledButton(String label) {
        Button btn = new Button(label);
        btn.setPrefWidth(180);
        btn.setPrefHeight(40);
        String base  = "-fx-background-color:#3b1a08;-fx-text-fill:#FFE066;-fx-font-size:13px;" +
                       "-fx-font-weight:bold;-fx-border-color:#c8600a;-fx-border-width:3;" +
                       "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
        String hover = "-fx-background-color:#c8600a;-fx-text-fill:#FFE066;-fx-font-size:13px;" +
                       "-fx-font-weight:bold;-fx-border-color:#FFE066;-fx-border-width:3;" +
                       "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button styledClassButton(String label) {
        Button btn = new Button(label);
        btn.setPrefWidth(125);
        btn.setPrefHeight(35);
        String base  = "-fx-background-color:#3b1a08;-fx-text-fill:#FFE066;-fx-font-size:12px;" +
                       "-fx-font-weight:bold;-fx-border-color:#c8600a;-fx-border-width:3;" +
                       "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
        String hover = "-fx-background-color:#c8600a;-fx-text-fill:#FFE066;-fx-font-size:12px;" +
                       "-fx-font-weight:bold;-fx-border-color:#FFE066;-fx-border-width:3;" +
                       "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisable()) btn.setStyle(hover);
        });
        btn.setOnMouseExited(e  -> {
            if (!btn.isDisable()) btn.setStyle(base);
        });
        return btn;
    }

    private void applyCSS(StackPane root) {
        try {
            root.getStyleClass().add("root");
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {}
    }

    public Scene getScene() { return scene; }
}