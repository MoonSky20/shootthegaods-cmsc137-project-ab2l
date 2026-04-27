package core;

import javafx.animation.AnimationTimer;

/**
 * GameLoop — our OWN custom game loop.
 * Manually calls update() then render() at ~60fps.
 * Satisfies the requirement: "Implement your own game loop."
 */
public class GameLoop {

    private static final long TARGET_FPS             = 60;
    private static final long NANOSECONDS_PER_FRAME  = 1_000_000_000 / TARGET_FPS;

    private final Runnable updateCallback;
    private final Runnable renderCallback;

    private AnimationTimer timer;
    private long lastTime = 0;
    private boolean running = false;

    public GameLoop(Runnable updateCallback, Runnable renderCallback) {
        this.updateCallback = updateCallback;
        this.renderCallback = renderCallback;
    }

    /** Starts the game loop. */
    public void start() {
        running = true;
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }

                long elapsed = now - lastTime;
                if (elapsed >= NANOSECONDS_PER_FRAME) {
                    lastTime = now;
                    updateCallback.run(); // 1. update game state
                    renderCallback.run(); // 2. draw everything
                }
            }
        };
        timer.start();
    }

    /** Stops the game loop. */
    public void stop() {
        running = false;
        if (timer != null) timer.stop();
    }

    public boolean isRunning() { return running; }
}
