package model;

/**
 * Obstacle  — an obstacle  in a map.
 * Spawns and persists anywhere in the map.
 */

public class Obstacle {

    private final double x, y;
    private final int    width, height;
    private final String spritePath;
    private final boolean isSolid;
    private final int sx, sy, sw, sh;

    public Obstacle (double x, double y, int width, int height, String spritePath, boolean isSolid, int sx, int sy, int sw, int sh) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.spritePath = spritePath;
        this.isSolid = isSolid;
        this.sx = sx;
        this.sy = sy;
        this.sw = sw;
        this.sh = sh;
    }

    /*Check collision */
    public boolean isColliding (double ex, double ey, int eSize) {
        if (!isSolid) return false;
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
    public boolean isSolid()     { return isSolid; }
    public int    getSx()        { return sx; }
    public int    getSy()        { return sy; }
    public int    getSw()        { return sw; }
    public int    getSh()        { return sh; }
}

