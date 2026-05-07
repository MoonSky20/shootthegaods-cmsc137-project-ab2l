package model;

/**
 * Obstacle  — an obstacle  in a map.
 * Spawns and persists anywhere in the map.
 */

public class Obstacle {

    private final double x, y;
    private final int    width, height;
    private final String spritePath;

    public Obstacle (double x, double y, int width, int height, String spritePath) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.spritePath = spritePath;
    }

    /*Check collission */
    public boolean isColliding (double ex, double ey, int eSize) {
        return ex < x + width &&
                ex + eSize > x &&
                ey < y + height &&
                ey + eSize > y;
    }

    /*Getters and Setters */
    public double getX()         { return x; }
    public double getY()         { return y; }
    public int    getWidth()     { return width; }
    public int    getHeight()    { return height; }
    public String getSpritePath(){ return spritePath; }
}

