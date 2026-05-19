package scenes;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * Local buttons go straight to GameScene.
 * Host buttons open LobbyScene (host mode).
 * Join opens LobbyScene (join mode) with the typed IP.
 */
public class MenuScene {

    private final Scene scene;

    public MenuScene(Stage stage) {
        Canvas canvas = new Canvas(GameScene.WIDTH, GameScene.HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // ---- Fonts ----
        Font mainFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 48);
        Font subFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/space_mono.ttf"), 13);
        Font secFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/space_mono.ttf"), 13);
        if (mainFont == null)
            mainFont = Font.font("Arial", 48);
        if (subFont == null)
            subFont = Font.font("Arial", 13);
        if (secFont == null)
            secFont = Font.font("Arial", 13);

        DropShadow titleShadow = pixelShadow("#5a1a00", 3, 3);

        // ---- Title ----
        Text title = new Text("SHOOT THE GAODS!");
        title.setFont(mainFont);
        title.setFill(Color.web("#FFE066"));
        title.setEffect(titleShadow);

        VBox titleContainer = new VBox(title);
        titleContainer.setAlignment(Pos.CENTER);
        titleContainer.getStyleClass().add("retro-box");
        titleContainer.setMaxWidth(520);

        // ---- Subtitle ----
        Text subtitle = new Text("[ Inspired by Box Head  •  Co-op Survival Shooter ]");
        subtitle.setFont(subFont);
        subtitle.setFill(Color.web("#FCD34D"));

        VBox subtitleContainer = new VBox(subtitle);
        subtitleContainer.setAlignment(Pos.CENTER);
        subtitleContainer.getStyleClass().add("retro-box");
        subtitleContainer.setStyle("-fx-padding: 6px 12px;");
        subtitleContainer.setMaxWidth(520);

        // ---- Local Play panel ----
        Text localLabel = section("LOCAL PLAY", secFont);
        VBox localLCont = new VBox(localLabel);
        localLCont.getStyleClass().add("overlay-title");
        localLCont.setMaxWidth(VBox.USE_PREF_SIZE);
        localLCont.setMaxHeight(VBox.USE_PREF_SIZE);

        VBox localContent = new VBox(10);
        localContent.setAlignment(Pos.CENTER);
        // Extra top padding so buttons don't hide under the overlay label
        localContent.setPadding(new Insets(24, 16, 16, 16));
        localContent.getStyleClass().add("main-content-box");

        String[] localLabels = { "1 PLAYER", "2 PLAYER", "3 PLAYER" };
        for (int i = 0; i < 3; i++) {
            final int count = i + 1;
            Button btn = makeBtn(localLabels[i]);
            btn.setFont(secFont);
            btn.setPrefWidth(160);
            btn.setOnAction(e -> stage.setScene(new ChooseAPlayerScene(stage, count).getScene()));
            localContent.getChildren().add(btn);
        }

        // Add Local High Score
        Text localHighScoreText = new Text("★ HIGH SCORE: " + core.ScoreManager.getLocalHighScore() + " ★");
        localHighScoreText.setFont(secFont);
        localHighScoreText.setFill(Color.web("#FFE066"));
        localContent.getChildren().add(localHighScoreText);

        // StackPane: content box behind, label overlaid on top-left
        StackPane localPanel = new StackPane(localContent, localLCont);
        localPanel.setMaxWidth(220);
        StackPane.setAlignment(localLCont, Pos.TOP_LEFT);
        localLCont.setTranslateX(12);
        localLCont.setTranslateY(-14); // half the label height — sits on the border

        // ---- Network Play panel ----
        Text netLabel = section("NETWORK PLAY", secFont);
        VBox netLCont = new VBox(netLabel);
        netLCont.getStyleClass().add("overlay-title");
        netLCont.setMaxWidth(VBox.USE_PREF_SIZE);
        netLCont.setMaxHeight(VBox.USE_PREF_SIZE);

        HBox hostRow = new HBox(10);
        hostRow.setAlignment(Pos.CENTER);
        String[] hostLabels = { "Host 2P", "Host 3P", "Host 4P" };
        for (int i = 0; i < 3; i++) {
            final int count = i + 2;
            Button btn = makeBtn(hostLabels[i]);
            btn.setFont(secFont);
            btn.setPrefWidth(100);
            btn.setOnAction(e -> stage.setScene(new LobbyScene(stage, count).getScene()));
            hostRow.getChildren().add(btn);
        }

        // ---- Join row ----
        TextField ipField = new TextField();
        ipField.setPromptText("Host IP  (e.g. 192.168.1.42)");
        ipField.setPrefWidth(220);
        ipField.setStyle(
                "-fx-background-color:#3b1a08;-fx-text-fill:#FFE066;" +
                        "-fx-prompt-text-fill:#aa8844;-fx-font-size:12px;" +
                        "-fx-border-color:#c8600a;-fx-border-width:2;" +
                        "-fx-background-radius:0;-fx-border-radius:0;");

        // Error handling on join button
        Button joinBtn = makeBtn("Join");
        joinBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                ipField.setPromptText("↑ Enter host IP first!");
                return;
            }
            stage.setScene(new LobbyScene(stage, ip).getScene());
        });

        HBox joinRow = new HBox(10, ipField, joinBtn);
        joinRow.setAlignment(Pos.CENTER);

        // Add Network High Score
        Text netHighScoreText = new Text("★ HIGH SCORE: " + core.ScoreManager.getNetworkHighScore() + " ★");
        netHighScoreText.setFont(secFont);
        netHighScoreText.setFill(Color.web("#FFE066"));

        VBox netContent = new VBox(10, hostRow, joinRow, netHighScoreText);
        netContent.setAlignment(Pos.CENTER);
        // Extra top padding so host buttons don't hide under overlay label
        netContent.setPadding(new Insets(24, 16, 16, 16));
        netContent.getStyleClass().add("main-content-box");

        // StackPane: content box behind, label overlaid on top-left
        StackPane netPanel = new StackPane(netContent, netLCont);
        netPanel.setMaxWidth(380);
        StackPane.setAlignment(netLCont, Pos.TOP_LEFT);
        netLCont.setTranslateX(12);
        netLCont.setTranslateY(-14);

        // ---- Side-by-side panels ----
        HBox panels = new HBox(32, localPanel, netPanel);
        panels.setAlignment(Pos.CENTER);
        panels.setPadding(new Insets(20, 0, 0, 0));

        // ---- Root layout ----
        VBox content = new VBox(14,
                titleContainer,
                subtitleContainer,
                panels);
        content.setAlignment(Pos.CENTER);
        content.setFillWidth(false);

        StackPane root = new StackPane(canvas, content);
        StackPane.setAlignment(content, Pos.CENTER);
        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);

        root.getStyleClass().add("root");
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {
        }
    }

    // -------------------------------------------------------
    private Text section(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        t.setFill(Color.web("#c8600a"));
        return t;
    }

    private Button makeBtn(String label) {
        Button btn = new Button(label);

        String base = "-fx-background-color:#EAC33E;-fx-text-fill:#1a0f0a;" +
                "-fx-font-weight:bold;-fx-font-size:16px;" +
                "-fx-max-width:infinity;-fx-padding:12px;" +
                "-fx-border-color:#1a0f0a;-fx-border-width:3px;" +
                "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";

        // Hover: invert dark bg, yellow text, yellow border
        String hover = "-fx-background-color:#1a0f0a;-fx-text-fill:#EAC33E;" +
                "-fx-font-weight:bold;-fx-font-size:16px;" +
                "-fx-max-width:infinity;-fx-padding:12px;" +
                "-fx-border-color:#EAC33E;-fx-border-width:3px;" +
                "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private DropShadow pixelShadow(String hex, double ox, double oy) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(hex));
        ds.setRadius(0);
        ds.setSpread(0);
        ds.setOffsetX(ox);
        ds.setOffsetY(oy);
        return ds;
    }

    public Scene getScene() {
        return scene;
    }
}