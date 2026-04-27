package model;

/**
 * Bullet — a projectile fired by a player.
 * Travels in a straight line until it hits a Gaod, obstacle, or leaves the map.
 */
public class Bullet {

    public static final int    SIZE   = 8;
    public static final double SPEED  = 8.0;
    public static final int    DAMAGE = 1;

    private double  x, y;
    private double  dx, dy;       // normalized direction * speed
    private int     ownerIndex;   // which player fired this
    private boolean active = true;

    public Bullet(double startX, double startY, double angle, int ownerIndex) {
        this.x          = startX;
        this.y          = startY;
        this.dx         = Math.cos(angle) * SPEED;
        this.dy         = Math.sin(angle) * SPEED;
        this.ownerIndex = ownerIndex;
    }

    /**
     * Moves the bullet forward each frame.
     * Deactivates if it leaves the map.
     */
    public void update(int mapWidth, int mapHeight) {
        if (!active) return;
        x += dx;
        y += dy;
        if (x < 0 || x > mapWidth || y < 0 || y > mapHeight) {
            active = false;
        }
    }

    /** Checks AABB collision with a Gaod. */
    public boolean isCollidingWith(Gaod gaod) {
        return x < gaod.getX() + Gaod.SIZE && x + SIZE > gaod.getX() &&
               y < gaod.getY() + Gaod.SIZE && y + SIZE > gaod.getY();
    }

    public void deactivate()      { active = false; }
    public boolean isActive()     { return active; }
    public double  getX()         { return x; }
    public double  getY()         { return y; }
    public int     getOwnerIndex(){ return ownerIndex; }
}
