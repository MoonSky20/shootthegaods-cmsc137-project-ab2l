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
 * MenuScene — main menu.
 *
 * ── Local Play (same PC) ──   [1 Player]  [2 Players]  [3 Players]
 * ── Network Play (LAN) ──     [Host 2P]  [Host 3P]  [Host 4P]
 *                              [IP field]  [Join Game]
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
        Font subFont  = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 13);
        Font secFont  = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 15);
        if (mainFont == null) mainFont = Font.font("Arial", 48);
        if (subFont  == null) subFont  = Font.font("Arial", 13);
        if (secFont  == null) secFont  = Font.font("Arial", 15);

        DropShadow titleShadow = pixelShadow("#5a1a00", 3, 3);
        DropShadow btnShadow   = pixelShadow("#5a1a00", 4, 4);

        // ---- Title ----
        Text title = new Text("SHOOT THE GAODS!");
        title.setFont(mainFont);
        title.setFill(Color.web("#FFE066"));
        title.setEffect(titleShadow);

        Text subtitle = new Text("[ Inspired by Box Head  •  Co-op Survival Shooter ]");
        subtitle.setFont(subFont);
        subtitle.setFill(Color.web("#FFE066"));
        subtitle.setEffect(titleShadow);

        // ---- Section labels ----
        Text localLabel = section("─── Local Play (same PC) ───", secFont);
        Text netLabel   = section("─── Network Play (LAN / Wi-Fi) ───", secFont);

        // ---- Local play ----
        HBox localRow = new HBox(12);
        localRow.setAlignment(Pos.CENTER);
        String[] localLabels = { "1 Player", "2 Players", "3 Players" };
        for (int i = 0; i < 3; i++) {
            final int count = i + 1;
            Button btn = makeBtn(localLabels[i], 130, btnShadow);
            btn.setOnAction(e -> stage.setScene(new GameScene(stage, count).getScene()));
            localRow.getChildren().add(btn);
        }

        // ---- Host buttons ----
        HBox hostRow = new HBox(12);
        hostRow.setAlignment(Pos.CENTER);
        String[] hostLabels = { "Host 2P", "Host 3P", "Host 4P" };
        for (int i = 0; i < 3; i++) {
            final int count = i + 2;
            Button btn = makeBtn(hostLabels[i], 130, btnShadow);
            btn.setOnAction(e -> stage.setScene(new LobbyScene(stage, count).getScene()));
            hostRow.getChildren().add(btn);
        }

        // ---- Join row ----
        TextField ipField = new TextField();
        ipField.setPromptText("Host IP (e.g. 192.168.1.42)");
        ipField.setPrefWidth(240);
        ipField.setStyle(
            "-fx-background-color:#3b1a08;-fx-text-fill:#FFE066;" +
            "-fx-prompt-text-fill:#aa8844;-fx-font-size:13px;" +
            "-fx-border-color:#c8600a;-fx-border-width:2;" +
            "-fx-background-radius:0;-fx-border-radius:0;"
        );

        Button joinBtn = makeBtn("Join Game", 130, btnShadow);
        joinBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) {
                ipField.setPromptText("↑ Enter host IP first!");
                return;
            }
            stage.setScene(new LobbyScene(stage, ip).getScene());
        });

        HBox joinRow = new HBox(12, ipField, joinBtn);
        joinRow.setAlignment(Pos.CENTER);

        // ---- Layout ----
        VBox content = new VBox(14,
            title, subtitle,
            new Text(""),
            localLabel, localRow,
            new Text(""),
            netLabel, hostRow, joinRow
        );
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(canvas, content);
        StackPane.setAlignment(content, Pos.CENTER);
        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);

        root.getStyleClass().add("root");
        try {
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------
    private Text section(String text, Font font) {
        Text t = new Text(text);
        t.setFont(font);
        t.setFill(Color.web("#c8600a"));
        return t;
    }

    private Button makeBtn(String label, double width, DropShadow shadow) {
        Button btn = new Button(label);
        btn.setPrefWidth(width);
        btn.setPrefHeight(44);
        String base  = "-fx-background-color:#3b1a08;-fx-text-fill:#FFE066;-fx-font-size:13px;" +
                       "-fx-font-weight:bold;-fx-border-color:#c8600a;-fx-border-width:3;" +
                       "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;-fx-padding:9 0 9 0;";
        String hover = "-fx-background-color:#c8600a;-fx-text-fill:#FFE066;-fx-font-size:13px;" +
                       "-fx-font-weight:bold;-fx-border-color:#FFE066;-fx-border-width:3;" +
                       "-fx-background-radius:0;-fx-border-radius:0;-fx-cursor:hand;-fx-padding:9 0 9 0;";
        btn.setStyle(base);
        btn.setEffect(shadow);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private DropShadow pixelShadow(String hex, double ox, double oy) {
        DropShadow ds = new DropShadow();
        ds.setColor(Color.web(hex));
        ds.setRadius(0); ds.setSpread(0);
        ds.setOffsetX(ox); ds.setOffsetY(oy);
        return ds;
    }

    public Scene getScene() { return scene; }
}
