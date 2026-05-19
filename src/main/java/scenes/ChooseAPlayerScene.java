package scenes;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * ChooseAPlayerScene — local-only character selection screen.
 * Each player takes turns picking a character class before starting a local GameScene.
 * Network character selection is handled directly inside LobbyScene.
 */
public class ChooseAPlayerScene {

    private final Scene scene;
    private final List<ImageView> characterViews = new ArrayList<>();
    private final List<Image> characterImages = new ArrayList<>();
    private AnimationTimer timer;
    private int currentPlayerSelecting = 0;
    private final List<Image> selectedImages = new ArrayList<>();
    private final List<Image> selectedRunImages = new ArrayList<>();
    private Text titleText;

    public ChooseAPlayerScene(Stage stage, int playerCount) {
        Canvas canvas = new Canvas(GameScene.WIDTH, GameScene.HEIGHT);

        Font mainFont = Font.loadFont(getClass().getResourceAsStream("/assets/fonts/ari_main.ttf"), 48);
        if (mainFont == null)
            mainFont = Font.font("Arial", 48);
        DropShadow titleShadow = pixelShadow("#5a1a00", 3, 3);

        titleText = new Text("PLAYER 1 CHOOSE YOUR CLASS");
        titleText.setFont(mainFont);
        titleText.setFill(Color.web("#FFE066"));
        titleText.setEffect(titleShadow);

        VBox titleContainer = new VBox(titleText);
        titleContainer.setAlignment(Pos.CENTER);
        titleContainer.getStyleClass().add("retro-box");
        titleContainer.setMaxWidth(600);

        HBox options = new HBox(40);
        options.setAlignment(Pos.CENTER);

        options.getChildren()
                .add(createCharacterOption(stage, playerCount, "Wizard", "/assets/characters/wizard_male/Idle.png"));
        options.getChildren()
                .add(createCharacterOption(stage, playerCount, "Archer", "/assets/characters/archer_male/Idle.png"));
        options.getChildren()
                .add(createCharacterOption(stage, playerCount, "Swordsman", "/assets/characters/swordsman/Idle.png"));
        options.getChildren()
                .add(createCharacterOption(stage, playerCount, "Enchantress", "/assets/female_charc/Enchantress/Idle.png"));
        options.getChildren()
                .add(createCharacterOption(stage, playerCount, "Musketeer", "/assets/female_charc/Musketeer/Idle.png"));

        Button backBtn = makeBtn("BACK");
        backBtn.setMaxWidth(160);
        backBtn.setOnAction(e -> {
            if (timer != null)
                timer.stop();
            stage.setScene(new MenuScene(stage).getScene());
        });

        VBox content = new VBox(40, titleContainer, options, backBtn);
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(canvas, content);
        root.getStyleClass().add("root");

        scene = new Scene(root, GameScene.WIDTH, GameScene.HEIGHT);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        } catch (Exception ignored) {
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 100_000_000) {
                    tick++;
                    for (int i = 0; i < characterViews.size(); i++) {
                        Image img = characterImages.get(i);
                        ImageView view = characterViews.get(i);
                        if (img != null) {
                            double frameHeight = img.getHeight();
                            double frameWidth = frameHeight;
                            int totalFrames = (int) Math.max(1, img.getWidth() / frameWidth);
                            int currentFrame = tick % totalFrames;
                            view.setViewport(new Rectangle2D(currentFrame * frameWidth, 0, frameWidth, frameHeight));
                        }
                    }
                    lastUpdate = now;
                }
            }
            private long lastUpdate = 0;
            private int tick = 0;
        };
        timer.start();
    }

    private VBox createCharacterOption(Stage stage, int playerCount, String name, String imagePath) {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);

        Image img = null;
        try {
            img = new Image(getClass().getResourceAsStream(imagePath));
        } catch (Exception e) {
            System.err.println("Could not load image: " + imagePath);
        }

        ImageView view = new ImageView();
        if (img != null) {
            view.setImage(img);
            double frameHeight = img.getHeight();
            view.setViewport(new Rectangle2D(0, 0, frameHeight, frameHeight));
            characterViews.add(view);
            characterImages.add(img);
        }

        Image runImg = null;
        try {
            String runPath = imagePath.replace("Idle.png", "Run.png");
            runImg = new Image(getClass().getResourceAsStream(runPath));
        } catch (Exception e) {
            System.err.println("Could not load run image for: " + imagePath);
        }

        view.setFitWidth(100);
        view.setFitHeight(100);
        view.setPreserveRatio(true);

        Button btn = makeBtn(name);
        btn.setPrefWidth(160);
        final Image finalImg = img;
        final Image finalRunImg = runImg;
        btn.setOnAction(e -> {
            selectedImages.add(finalImg);
            selectedRunImages.add(finalRunImg);
            currentPlayerSelecting++;

            if (currentPlayerSelecting < playerCount) {
                titleText.setText("PLAYER " + (currentPlayerSelecting + 1) + " CHOOSE YOUR CLASS");
            } else {
                GameScene gameScene = new GameScene(stage, playerCount);
                for (int i = 0; i < playerCount; i++) {
                    model.Player p = gameScene.getPlayers().get(i);
                    p.setImage(selectedImages.get(i));
                    if (selectedRunImages.get(i) != null) {
                        p.setRunImage(selectedRunImages.get(i));
                    }
                }
                if (timer != null)
                    timer.stop();
                stage.setScene(gameScene.getScene());
            }
        });

        box.getChildren().addAll(view, btn);
        return box;
    }

    private Button makeBtn(String label) {
        Button btn = new Button(label);

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
