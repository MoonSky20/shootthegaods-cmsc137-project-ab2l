package model;

/**
 * Gaod — an enemy that chases the nearest living player.
 * Speed and count scale with the current wave level.
 */
public class Gaod {

    public static final int SIZE = 40;

    public enum Type {
        RED, BLUE
    }

    private double x, y;
    private double speed;
    private int    health;
    private int    maxHealth;
    private boolean alive = true;
    private Type type = (new java.util.Random().nextBoolean()) ? Type.RED : Type.BLUE;
    private int animationTick = 0;

    public Gaod(double x, double y, double speed, int health) {
        this.x         = x;
        this.y         = y;
        this.speed     = speed;
        this.health    = health;
        this.maxHealth = health;
    }

    public Gaod(double x, double y, double speed, int health, Type type) {
        this.x         = x;
        this.y         = y;
        this.speed     = speed;
        this.health    = health;
        this.maxHealth = health;
        this.type      = type;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    /**
     * Moves this Gaod toward the nearest living player.
     * Called every frame by the game loop.
     */
    public void update(java.util.List<Player> players) {
        update(players, java.util.Collections.emptyList());
    }

    public void update(java.util.List<Player> players, java.util.List<Obstacle> obstacles) {
        if (!alive) return;
        animationTick++;

        // Find nearest living player
        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : players) {
            if (!p.isAlive()) continue;
            double dist = distance(p.getX(), p.getY());
            if (dist < minDist) {
                minDist = dist;
                target  = p;
            }
        }

        if (target == null) return;

        // Move toward target
        double dx = (target.getX() + Player.SIZE / 2.0) - (x + SIZE / 2.0);
        double dy = (target.getY() + Player.SIZE / 2.0) - (y + SIZE / 2.0);
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            double moveX = (dx / len) * speed;
            double moveY = (dy / len) * speed;

            x += moveX;
            for (Obstacle o : obstacles) {
                if (o.isColliding(x, y, SIZE)) {
                    x -= moveX;
                    break;
                }
            }

            y += moveY;
            for (Obstacle o : obstacles) {
                if (o.isColliding(x, y, SIZE)) {
                    y -= moveY;
                    break;
                }
            }
        }
    }

    /** Called when hit by a bullet. */
    public void takeDamage(int dmg) {
        health -= dmg;
        if (health <= 0) alive = false;
    }

    /** Checks AABB collision with a player. */
    public boolean isCollidingWithPlayer(Player p) {
        return x < p.getX() + Player.SIZE && x + SIZE > p.getX() &&
               y < p.getY() + Player.SIZE && y + SIZE > p.getY();
    }

    private double distance(double px, double py) {
        double dx = (px + Player.SIZE / 2.0) - (x + SIZE / 2.0);
        double dy = (py + Player.SIZE / 2.0) - (y + SIZE / 2.0);
        return Math.sqrt(dx * dx + dy * dy);
    }

    // --- Getters ---
    public double  getX()        { return x; }
    public double  getY()        { return y; }
    public boolean isAlive()     { return alive; }
    public double  getSpeed()    { return speed; }
    public void    setSpeed(double s) { this.speed = s; }
    public int     getAnimationTick() { return animationTick; }
    public int     getHealth()   { return health; }
    public int     getMaxHealth(){ return maxHealth; }
}
