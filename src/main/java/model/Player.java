package model;

import java.util.List;

/**
 * Player — represents one of the 1-4 co-op players.
 * Handles movement, health, respawn timer, and shooting state.
 */
public class Player {

    public static final int    SIZE          = 28;
    public static final double SPEED         = 3.5;
    public static final int    MAX_HEALTH    = 3;

    // Base respawn delay in frames (5s * 60fps = 300 frames)
    public static final int BASE_RESPAWN_DELAY = 300;

    private final int    playerIndex; // 0-3
    private final String name;
    private double x, y;

    // Health & respawn
    private int     health;
    private boolean alive;
    private int     respawnTimer;   // frames remaining until respawn

    // Movement flags (set by InputHandler)
    private boolean movingUp, movingDown, movingLeft, movingRight;
    private boolean shooting;

    // Facing direction (for non-mouse shooting)
    private double aimAngle = 0; // radians

    // Shoot cooldown (frames)
    private int shootCooldown = 0;
    public static final int SHOOT_COOLDOWN_FRAMES = 20; // ~3 shots/sec

    // Weapon
    private String currentWeapon = "Pistol";

    public Player(int playerIndex, String name, double startX, double startY) {
        this.playerIndex = playerIndex;
        this.name        = name;
        this.x           = startX;
        this.y           = startY;
        this.health      = MAX_HEALTH;
        this.alive       = true;
    }

    /**
     * Updates position, aim angle, and shoot cooldown.
     * Called every frame by the game loop.
     */
    public void update(int mapWidth, int mapHeight, List<Obstacle> Obstacles) {
        if (!alive) {
            // Count down respawn timer
            if (respawnTimer > 0) respawnTimer--;
            return;
        }

        // Movement
        double dx = 0, dy = 0;
        if (movingUp)    dy -= SPEED; // Add constraints here
        if (movingDown)  dy += SPEED;
        if (movingLeft)  dx -= SPEED;
        if (movingRight) dx += SPEED;

        // Diagonal normalization
        if (dx != 0 && dy != 0) {
            dx *= 0.707;
            dy *= 0.707;
        }

        // Resolve x axis

        x += dx;
        y += dy;

        // Update aim angle based on movement direction (for keyboard shooters)
        if (dx != 0 || dy != 0) aimAngle = Math.atan2(dy, dx);

        // Clamp to map
        x = Math.max(0, Math.min(mapWidth  - SIZE, x));
        y = Math.max(0, Math.min(mapHeight - SIZE, y));

        // Shoot cooldown
        if (shootCooldown > 0) shootCooldown--;
    }

    /** Called when this player is hit by a Gaod. */
    public void takeDamage(int respawnDelayFrames) {
        if (!alive) return;
        health--;
        if (health <= 0) {
            alive        = false;
            respawnTimer = respawnDelayFrames;
        }
    }

    /** Respawns the player at a given position. */
    public void respawn(double spawnX, double spawnY) {
        x      = spawnX;
        y      = spawnY;
        health = MAX_HEALTH;
        alive  = true;
        respawnTimer = 0;
    }

    /** Returns true if the player can shoot this frame. */
    public boolean canShoot() {
        return alive && shooting && shootCooldown == 0;
    }

    /** Call after a bullet is fired to start the cooldown. */
    public void triggerShootCooldown() {
        shootCooldown = SHOOT_COOLDOWN_FRAMES;
    }

    /** Checks AABB collision with a Gaod. */
    public boolean isCollidingWith(double ex, double ey, int eSize) {
        return x < ex + eSize && x + SIZE > ex &&
               y < ey + eSize && y + SIZE > ey;
    }

    // --- Getters & Setters ---
    public int     getPlayerIndex()  { return playerIndex; }
    public String  getName()         { return name; }
    public double  getX()            { return x; }
    public double  getY()            { return y; }
    public void    setX(double x)    { this.x = x; }
    public void    setY(double y)    { this.y = y; }
    public int     getHealth()       { return health; }
    public boolean isAlive()         { return alive; }
    public int     getRespawnTimer() { return respawnTimer; }
    public double  getAimAngle()     { return aimAngle; }
    public void    setAimAngle(double angle) { this.aimAngle = angle; }
    public String  getCurrentWeapon(){ return currentWeapon; }
    public void    setCurrentWeapon(String w) { this.currentWeapon = w; }

    private int    characterIndex = 0;
    public int     getCharacterIndex() { return characterIndex; }
    public void    setCharacterIndex(int idx) { this.characterIndex = idx; }

    public void setMovingUp(boolean v)    { movingUp    = v; }
    public void setMovingDown(boolean v)  { movingDown  = v; }
    public void setMovingLeft(boolean v)  { movingLeft  = v; }
    public void setMovingRight(boolean v) { movingRight = v; }
    public void setShooting(boolean v)    { shooting    = v; }
    public boolean isShooting()           { return shooting; }
}
