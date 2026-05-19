package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ScoreManager {
    private static final String FILE_NAME = "highscores.properties";
    private static final String KEY_LOCAL_HIGH = "local_high_score";
    private static final String KEY_NETWORK_HIGH = "network_high_score";

    private static Properties properties = new Properties();

    static {
        loadScores();
    }

    private static synchronized void loadScores() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
            } catch (IOException e) {
                System.err.println("Error loading high scores: " + e.getMessage());
            }
        }
    }

    private static synchronized void saveScores() {
        try (FileOutputStream out = new FileOutputStream(FILE_NAME)) {
            properties.store(out, "Shoot the Gaods High Scores");
        } catch (IOException e) {
            System.err.println("Error saving high scores: " + e.getMessage());
        }
    }

    public static synchronized int getLocalHighScore() {
        String val = properties.getProperty(KEY_LOCAL_HIGH, "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static synchronized int getNetworkHighScore() {
        String val = properties.getProperty(KEY_NETWORK_HIGH, "0");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static synchronized boolean updateLocalHighScore(int newScore) {
        int current = getLocalHighScore();
        if (newScore > current) {
            properties.setProperty(KEY_LOCAL_HIGH, String.valueOf(newScore));
            saveScores();
            return true;
        }
        return false;
    }

    public static synchronized boolean updateNetworkHighScore(int newScore) {
        int current = getNetworkHighScore();
        if (newScore > current) {
            properties.setProperty(KEY_NETWORK_HIGH, String.valueOf(newScore));
            saveScores();
            return true;
        }
        return false;
    }
}
