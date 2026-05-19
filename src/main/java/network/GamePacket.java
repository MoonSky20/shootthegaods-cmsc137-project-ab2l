package network;

import java.io.Serializable;
import java.util.List;

/**
 * GamePacket — all serializable data that crosses the wire.
 *
 * CLIENT → SERVER : PlayerInputPacket   (every frame, tiny)
 * SERVER → CLIENT : GameStatePacket     (every frame, full snapshot)
 * BOTH DIRECTIONS : LobbyPacket         (connection handshake / control)
 */
public class GamePacket {

    // -------------------------------------------------------
    // CLIENT → SERVER  (sent each frame by every client)
    // -------------------------------------------------------
    public static class PlayerInputPacket implements Serializable {
        private static final long serialVersionUID = 1L;

        public int     playerIndex;
        public boolean movingUp, movingDown, movingLeft, movingRight;
        public boolean shooting;
        public double  mouseX, mouseY;   // screen-space cursor for P1 aim
        public long    timestamp;

        public PlayerInputPacket(int idx,
                                 boolean up, boolean down, boolean left, boolean right,
                                 boolean shooting, double mx, double my) {
            this.playerIndex = idx;
            this.movingUp    = up;
            this.movingDown  = down;
            this.movingLeft  = left;
            this.movingRight = right;
            this.shooting    = shooting;
            this.mouseX      = mx;
            this.mouseY      = my;
            this.timestamp   = System.currentTimeMillis();
        }
    }

    // -------------------------------------------------------
    // SERVER → CLIENT  (authoritative game snapshot)
    // -------------------------------------------------------
    public static class GameStatePacket implements Serializable {
        private static final long serialVersionUID = 1L;

        public List<PlayerState>  players;
        public List<GaodState>    gaods;
        public List<BulletState>  bullets;
        public int   level;
        public int   score;
        public int   secondsRemaining;
        public boolean gameOver;
        public long  serverTimestamp;

        public GameStatePacket() { serverTimestamp = System.currentTimeMillis(); }
    }

    // -------------------------------------------------------
    // LOBBY / CONTROL  (both directions)
    // -------------------------------------------------------
    public static class LobbyPacket implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum Type {
            ASSIGN_INDEX,       // server → new client: "you are player N"
            PLAYER_JOINED,      // server → all: someone connected
            PLAYER_LEFT,        // server → all: someone disconnected
            START_GAME,         // server → all: game is beginning
            PING,               // either direction: keep-alive / RTT
            HOST_DISCONNECTED,  // server → all: fatal, return to menu
            SELECT_CHARACTER    // client → server: chosen character, server → all: sync choice
        }

        public Type   type;
        public int    assignedIndex;   // valid for ASSIGN_INDEX
        public int    connectedCount;  // current number of connected players
        public int    targetCount;     // how many players the host wants
        public String message;         // optional human-readable text
        public long   timestamp;
        public int    characterIndex;  // valid for SELECT_CHARACTER
        public long   seed;            // valid for START_GAME

        public LobbyPacket(Type t) { type = t; timestamp = System.currentTimeMillis(); }
    }

    // -------------------------------------------------------
    // Lightweight state snapshots  (avoid sending full model objects)
    // -------------------------------------------------------
    public static class PlayerState implements Serializable {
        private static final long serialVersionUID = 1L;
        public int     index;
        public double  x, y;
        public int     health;
        public boolean alive;
        public double  aimAngle;
        public int     respawnTimer;
        public boolean shooting;
        public int     characterIndex;
    }

    public static class GaodState implements Serializable {
        private static final long serialVersionUID = 1L;
        public int    id;
        public double x, y;
        public boolean alive;
        public int    type; // 0 = blue, 1 = red
    }

    public static class BulletState implements Serializable {
        private static final long serialVersionUID = 1L;
        public double  x, y;
        public int     ownerIndex;
        public boolean active;
        public double  angle;
    }
}
