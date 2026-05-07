package scenes;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.text.Text;


/**
 * MenuScene — the main menu.
 * Players choose how many players (1-4) then start the game.
 */
public class MenuScene {

    private final Scene scene;

    public MenuScene(Stage stage) {
        Canvas canvas = new Canvas(GameScene.WIDTH, GameScene.HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Load fonts
        Font mainFont = Font.loadFont(
                getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"),
                52);

        Font subFont = Font.loadFont(
                getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"),
                13);

        // Dropshadow
        DropShadow btnShadow = new DropShadow();
        btnShadow.setColor(Color.web("#5a1a00"));
        btnShadow.setRadius(0);
        btnShadow.setOffsetX(4);
        btnShadow.setOffsetY(4);

        // Title
        Text title = new Text("SHOOT THE GAODS!");
        title.setFont(mainFont);
        title.setFill(Color.web("#FFE066"));

        // Pixel-style hard shadow (no blur, offset only)
        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.web("#5a1a00"));
        titleShadow.setRadius(0);
        titleShadow.setSpread(0);
        titleShadow.setOffsetX(3);
        titleShadow.setOffsetY(3);
        title.setEffect(titleShadow);

        // Subtitle
       Text subtitle = new Text("[ Inspired by Box Head  •  Co-op Survival Shooter ]");
       subtitle.setFont(subFont);
       subtitle.setFill(Color.web("#FFE066"));
       subtitle.setEffect(titleShadow);
        // Buttons
        VBox buttons = new VBox(14);
        buttons.setAlignment(Pos.CENTER);
        buttons.setTranslateY(60);

        String[] labels = { "1 Player", "2 Players", "3 Players"};
        String baseStyle =
                "-fx-background-color: #3b1a08;" +   // dark wood brown
                        "-fx-text-fill: #FFE066;" +           // warm yellow text
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-border-color: #c8600a;" +        // burnt orange border
                        "-fx-border-width: 3;" +
                        "-fx-background-radius: 0;" +         // sharp pixel corners
                        "-fx-border-radius: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 9 0 9 0;";
        String hoverStyle =
                "-fx-background-color: #c8600a;" +   // orange fill on hover
                    "-fx-text-fill: #FFE066;" +
                    "-fx-font-size: 14px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-border-color: #FFE066;" +        // yellow border on hover
                    "-fx-border-width: 3;" +
                    "-fx-background-radius: 0;" +
                    "-fx-border-radius: 0;" +
                    "-fx-cursor: hand;" +
                    "-fx-padding: 9 0 9 0;";

        for (int i = 0; i < 3; i++) {
            final int count = i + 1;
            Button btn = new Button(labels[i]);
            btn.setPrefWidth(200);
            btn.setPrefHeight(44);
            btn.setStyle( baseStyle);
            btn.setEffect(btnShadow);

            btn.setOnMouseEntered(e -> btn.setStyle( hoverStyle));
            btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
            btn.setOnAction(e -> { // Launch game
                GameScene game = new GameScene(stage, count);
                stage.setScene(game.getScene());
            });
            buttons.getChildren().add(btn);
        }

        // Stack title, sub and button
        VBox content = new VBox(10, title, subtitle, buttons);
        content.setAlignment(Pos.CENTER);
        VBox.setMargin(buttons, new javafx.geometry.Insets(20, 0, 0, 0));

        StackPane root = new StackPane(canvas, content);
        StackPane.setAlignment(content,Pos.CENTER);
        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);

        // add css
        root.getStyleClass().add("root");
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
    }

    public Scene getScene() { return scene; }
}
