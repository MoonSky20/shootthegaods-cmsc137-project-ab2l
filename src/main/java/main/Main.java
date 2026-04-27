package main;

import javafx.application.Application;
import javafx.stage.Stage;
import scenes.MenuScene;

/**
 * main.Main — entry point for Shoot The Gaods!
 * A 1-4 player co-op top-down survival shooter.
 */
public class Main extends Application {

    public static final String TITLE  = "Shoot The Gaods!";
    public static final int    WIDTH  = 960;
    public static final int    HEIGHT = 640;

    @Override
    public void start(Stage stage) {
        stage.setTitle(TITLE);
        stage.setResizable(false);

        MenuScene menu = new MenuScene(stage);
        stage.setScene(menu.getScene());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
