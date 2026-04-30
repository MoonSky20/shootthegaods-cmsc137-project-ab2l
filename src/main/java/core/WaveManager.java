package core;

import model.Gaod;
import model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WaveManager — controls wave progression and Gaod spawning.
 *
 * Rules from the proposal:
 *   - Waves last 30–60 seconds (1800–3600 frames at 60fps)
 *   - Every level: enemy speed increases, more enemies spawn
 *   - Wave duration increases every 4 levels by 15s (capped at 60s)
 *   - Respawn delay increases 5s every 3 levels (capped at 15s)
 */
public class WaveManager {

    private static final int BASE_WAVE_DURATION    = 30 * 60; // 30s in frames
    private static final int MAX_WAVE_DURATION     = 60 * 60; // 60s cap
    private static final int DURATION_INCREASE     = 15 * 60; // +15s every 4 levels
    private static final int BASE_RESPAWN_DELAY    = 5  * 60; // 5s in frames
    private static final int MAX_RESPAWN_DELAY     = 15 * 60; // 15s cap
    private static final int RESPAWN_INCREASE      = 5  * 60; // +5s every 3 levels

    private static final double BASE_GAOD_SPEED    = 1.5;
    private static final double SPEED_INCREASE     = 0.3; // per level
    private static final int    BASE_GAOD_COUNT    = 3;
    private static final int    COUNT_INCREASE     = 2;   // per level

    private int  currentLevel    = 1;
    private int  waveTimer       = 0;   // frames elapsed in current wave
    private int  spawnTimer      = 0;   // frames until next spawn
    private int  gaodSpawnInterval = 90; // spawn a Gaod every 1.5s initially
    private int  gaodsSpawnedThisWave = 0;

    private final Random random = new Random();
    private final int mapWidth;
    private final int mapHeight;

    public WaveManager(int mapWidth, int mapHeight) {
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;
    }

    /**
     * Updates wave timer and spawns Gaods.
     * Returns a list of newly spawned Gaods this frame.
     */
    public List<Gaod> update(List<Gaod> currentGaods) {
        List<Gaod> newGaods = new ArrayList<>();
        waveTimer++;
        spawnTimer++;

        int totalToSpawn = BASE_GAOD_COUNT + (currentLevel - 1) * COUNT_INCREASE;

        // Spawn Gaods periodically until we've spawned enough for this wave
        if (gaodsSpawnedThisWave < totalToSpawn && spawnTimer >= gaodSpawnInterval) {
            spawnTimer = 0;
            newGaods.add(spawnGaod());
            gaodsSpawnedThisWave++;
        }

        // if all gaods for this wave ay defeated na
        if (gaodsSpawnedThisWave >= totalToSpawn && currentGaods.isEmpty()){
            advanceLevel();
            return newGaods;
        }

        // Advance wave when timer expires
        if (waveTimer >= getWaveDuration()) {
            advanceLevel();
        }

        return newGaods;
    }

    /** Advances to the next level and resets wave state. */
    private void advanceLevel() {
        currentLevel++;
        waveTimer  = 0;
        spawnTimer = 0;
        gaodsSpawnedThisWave = 0;
        System.out.println("[Wave] Level " + currentLevel + " started!");
    }

    /** Spawns a Gaod at a random edge of the map. */
    private Gaod spawnGaod() {
        double x, y;
        int edge = random.nextInt(4);
        switch (edge) {
            case 0 -> { x = random.nextDouble() * mapWidth;  y = -Gaod.SIZE; }          // top
            case 1 -> { x = random.nextDouble() * mapWidth;  y = mapHeight; }            // bottom
            case 2 -> { x = -Gaod.SIZE;                      y = random.nextDouble() * mapHeight; } // left
            default-> { x = mapWidth;                        y = random.nextDouble() * mapHeight; } // right
        }
        return new Gaod(x, y, getGaodSpeed(), 1);
    }

    /** Wave duration in frames, increases every 4 levels (+15s, capped at 60s). */
    public int getWaveDuration() {
        int bonus = ((currentLevel - 1) / 4) * DURATION_INCREASE;
        return Math.min(BASE_WAVE_DURATION + bonus, MAX_WAVE_DURATION);
    }

    /** Respawn delay in frames, increases every 3 levels (+5s, capped at 15s). */
    public int getRespawnDelayFrames() {
        int bonus = ((currentLevel - 1) / 3) * RESPAWN_INCREASE;
        return Math.min(BASE_RESPAWN_DELAY + bonus, MAX_RESPAWN_DELAY);
    }

    /** Gaod speed for the current level. */
    public double getGaodSpeed() {
        return BASE_GAOD_SPEED + (currentLevel - 1) * SPEED_INCREASE;
    }

    /** Seconds remaining in the current wave. */
    public int getSecondsRemaining() {
        return (getWaveDuration() - waveTimer) / 60;
    }

    public int getCurrentLevel() { return currentLevel; }
    public int getWaveTimer()    { return waveTimer; }
}
