package core;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import model.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * InputHandler — tracks all currently pressed keys and updates player movement flags.
 *
 * Default key bindings (all on one keyboard):
 *
 *   Player 1: WASD to move,        mouse left-click to shoot (aimed at cursor)
 *   Player 2: Arrow keys to move,  L to shoot (shoots in movement direction)
 *   Player 3: IJKL to move,        U to shoot
 *   Player 4: Numpad 8456 to move, Numpad 0 to shoot
 */
public class InputHandler {

    private final Set<KeyCode> pressedKeys = new HashSet<>();
    private final List<Player> players;

    public InputHandler(Scene scene, List<Player> players) {
        this.players = players;
        scene.setOnKeyPressed(e  -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    /**
     * Called every frame by the game loop.
     * Translates pressed keys into movement/shoot flags for each player.
     */
    public void update() {
        // --- Player 1: WASD ---
        if (players.size() > 0) {
            Player p1 = players.get(0);
            p1.setMovingUp(pressedKeys.contains(KeyCode.W));
            p1.setMovingDown(pressedKeys.contains(KeyCode.S));
            p1.setMovingLeft(pressedKeys.contains(KeyCode.A));
            p1.setMovingRight(pressedKeys.contains(KeyCode.D));
        }

        // --- Player 2: Arrow Keys ---
        if (players.size() > 1) {
            Player p2 = players.get(1);
            p2.setMovingUp(pressedKeys.contains(KeyCode.UP));
            p2.setMovingDown(pressedKeys.contains(KeyCode.DOWN));
            p2.setMovingLeft(pressedKeys.contains(KeyCode.LEFT));
            p2.setMovingRight(pressedKeys.contains(KeyCode.RIGHT));
            p2.setShooting(pressedKeys.contains(KeyCode.L));
        }

        // --- Player 3: IJKL ---
        if (players.size() > 2) {
            Player p3 = players.get(2);
            p3.setMovingUp(pressedKeys.contains(KeyCode.I));
            p3.setMovingDown(pressedKeys.contains(KeyCode.K));
            p3.setMovingLeft(pressedKeys.contains(KeyCode.J));
            p3.setMovingRight(pressedKeys.contains(KeyCode.L));
            p3.setShooting(pressedKeys.contains(KeyCode.U));
        }

        // --- Player 4: Numpad 8456 ---
        if (players.size() > 3) {
            Player p4 = players.get(3);
            p4.setMovingUp(pressedKeys.contains(KeyCode.NUMPAD8));
            p4.setMovingDown(pressedKeys.contains(KeyCode.NUMPAD2));
            p4.setMovingLeft(pressedKeys.contains(KeyCode.NUMPAD4));
            p4.setMovingRight(pressedKeys.contains(KeyCode.NUMPAD6));
            p4.setShooting(pressedKeys.contains(KeyCode.NUMPAD0));
        }
    }

    public boolean isKeyPressed(KeyCode code) {
        return pressedKeys.contains(code);
    }
}
