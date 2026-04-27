package scenes;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * MenuScene — the main menu.
 * Players choose how many players (1-4) then start the game.
 */
public class MenuScene {

    private final Scene scene;

    public MenuScene(Stage stage) {
        Canvas canvas = new Canvas(GameScene.WIDTH, GameScene.HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Draw background
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, GameScene.WIDTH, GameScene.HEIGHT);

        // Grid
        gc.setStroke(Color.web("#16213e"));
        gc.setLineWidth(1);
        for (int x = 0; x < GameScene.WIDTH;  x += 40) gc.strokeLine(x, 0, x, GameScene.HEIGHT);
        for (int y = 0; y < GameScene.HEIGHT; y += 40) gc.strokeLine(0, y, GameScene.WIDTH, y);

        // Title
        gc.setFill(Color.web("#e94560"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 52));
        gc.fillText("SHOOT THE GAODS!", 160, 160);

        gc.setFill(Color.web("#ffffff88"));
        gc.setFont(Font.font("Arial", 18));
        gc.fillText("Inspired by Box Head  •  Co-op Survival Shooter", 230, 200);

        // Buttons
        VBox buttons = new VBox(14);
        buttons.setAlignment(Pos.CENTER);
        buttons.setTranslateY(60);

        String[] labels = { "1 Player", "2 Players", "3 Players", "4 Players" };
        for (int i = 0; i < 4; i++) {
            final int count = i + 1;
            Button btn = new Button(labels[i]);
            btn.setPrefWidth(200);
            btn.setPrefHeight(44);
            btn.setStyle(
                "-fx-background-color: #0f3460;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #e94560;" +
                "-fx-border-width: 2;" +
                "-fx-cursor: hand;"
            );
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #e94560;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #e94560;" +
                "-fx-border-width: 2;" +
                "-fx-cursor: hand;"
            ));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #0f3460;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #e94560;" +
                "-fx-border-width: 2;" +
                "-fx-cursor: hand;"
            ));
            btn.setOnAction(e -> {
                GameScene game = new GameScene(stage, count);
                stage.setScene(game.getScene());
            });
            buttons.getChildren().add(btn);
        }

        StackPane root = new StackPane(canvas, buttons);
        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);
    }

    public Scene getScene() { return scene; }
}
