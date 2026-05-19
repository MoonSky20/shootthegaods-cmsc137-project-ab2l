package network;

import core.WaveManager;
import model.Bullet;
import model.Gaod;
import model.Obstacle;
import model.Player;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * GameServer — the authoritative multiplayer server.
 *
 * Flow:
 *   1. Host calls new GameServer(targetPlayerCount) and start()
 *   2. Server listens on PORT and accepts up to targetPlayerCount clients
 *   3. Each accepted client gets a ClientConnection thread + ASSIGN_INDEX packet
 *   4. When connectedCount == targetPlayerCount, server calls startGame() automatically
 *   5. Game loop runs the full simulation; broadcasts GameStatePacket ~60fps
 *   6. Clients send only PlayerInputPacket; server is single source of truth
 *
 * The host itself connects as a regular client (localhost) so the same
 * NetworkGameScene code works for both host and joining players.
 */
public class GameServer {

    public static final int PORT         = 55000;
    public static final int MAP_WIDTH    = 960;
    public static final int MAP_HEIGHT   = 640;
    static final long  TICK_MS     = 16;   // ~60 fps

    private final int targetCount;         // how many players the host wants
    private ServerSocket serverSocket;

    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();
    private final int[] chosenCharacters = new int[4]; // synchronized character choices (default Wizard = 0)
    private final boolean[] chosenReady = new boolean[4]; // tracks if client has selected their character

    // ---- Authoritative game state ----
    private final List<Player>   players = new ArrayList<>();
    private final List<Gaod>     gaods   = new CopyOnWriteArrayList<>();
    private final List<Bullet>   bullets = new CopyOnWriteArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>(); // static, no obstacle logic yet
    private WaveManager waveManager;

    private volatile boolean accepting   = false;
    private volatile boolean gameRunning = false;
    private int score         = 0;
    private int gaodIdCounter = 0;
    private final Map<Gaod, Integer> gaodIds = new IdentityHashMap<>();
    private int levelMessageTimer = 0;

    private static final double[][] SPAWN_POINTS = {
            { 80,  80  }, { 840, 80  }, { 80,  520 }, { 840, 520 }
    };

    // Callbacks so host UI can react (called from server threads)
    private Consumer<Integer> onPlayerJoined;   // arg = connected count
    private Runnable          onGameStarted;

    // Latest state snapshot (host-side NetworkGameScene reads this directly)
    private volatile GamePacket.GameStatePacket lastState;

    // -------------------------------------------------------
    public GameServer(int targetPlayerCount) {
        this.targetCount = Math.min(Math.max(1, targetPlayerCount), 4);
    }

    // -------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------

    /** Opens the server socket and begins accepting clients. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        serverSocket.setReuseAddress(true);
        accepting = true;
        System.out.println("[Server] Listening on " + getLocalIP() + ":" + PORT
                + "  (waiting for " + targetCount + " players)");

        Thread t = new Thread(this::acceptLoop, "server-accept");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        accepting    = false;
        gameRunning  = false;
        clients.forEach(ClientConnection::close);
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); }
        catch (IOException ignored) {}
    }

    public void setOnPlayerJoined(Consumer<Integer> cb) { onPlayerJoined = cb; }
    public void setOnGameStarted(Runnable cb)            { onGameStarted  = cb; }

    // -------------------------------------------------------
    // Accept loop
    // -------------------------------------------------------

    private void acceptLoop() {
        while (accepting) {
            try {
                Socket sock = serverSocket.accept();
                sock.setTcpNoDelay(true);

                int idx = clients.size();
                if (idx >= targetCount) { sock.close(); continue; }

                ClientConnection cc = new ClientConnection(sock, idx, () -> onClientDisconnect(idx), pkt -> handleLobbyPacket(idx, pkt));
                cc.initStreams();
                clients.add(cc);

                // Tell this client their index
                GamePacket.LobbyPacket assign = new GamePacket.LobbyPacket(GamePacket.LobbyPacket.Type.ASSIGN_INDEX);
                assign.assignedIndex  = idx;
                assign.connectedCount = clients.size();
                assign.targetCount    = targetCount;
                cc.sendLobby(assign);

                // Send already chosen characters to this new client so their UI slots synchronize perfectly
                for (int i = 0; i < idx; i++) {
                    if (chosenReady[i]) {
                        GamePacket.LobbyPacket existing = new GamePacket.LobbyPacket(GamePacket.LobbyPacket.Type.SELECT_CHARACTER);
                        existing.assignedIndex = i;
                        existing.characterIndex = chosenCharacters[i];
                        cc.sendLobby(existing);
                    }
                }

                // Tell everyone someone joined
                GamePacket.LobbyPacket joined = new GamePacket.LobbyPacket(GamePacket.LobbyPacket.Type.PLAYER_JOINED);
                joined.connectedCount = clients.size();
                joined.targetCount    = targetCount;
                broadcast(joined);

                Thread ct = new Thread(cc, "client-" + idx);
                ct.setDaemon(true);
                ct.start();

                System.out.println("[Server] Player " + idx + " joined (" + clients.size() + "/" + targetCount + ")");

                if (onPlayerJoined != null) onPlayerJoined.accept(clients.size());

            } catch (IOException e) {
                if (accepting) System.err.println("[Server] Accept error: " + e.getMessage());
                break;
            }
        }
    }

    private void handleLobbyPacket(int idx, GamePacket.LobbyPacket pkt) {
        if (pkt.type == GamePacket.LobbyPacket.Type.SELECT_CHARACTER) {
            pkt.assignedIndex = idx;
            chosenCharacters[idx] = pkt.characterIndex;
            chosenReady[idx] = true;
            broadcast(pkt);

            // Auto-start only when we hit target connected players AND all connected players have selected their class
            if (clients.size() == targetCount && allReady()) {
                accepting = false;
                try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
                startGame();
            }
        }
    }

    private boolean allReady() {
        for (int i = 0; i < clients.size(); i++) {
            if (!chosenReady[i]) return false;
        }
        return true;
    }

    /** Starts the game simulation (called automatically when lobby is full). */
    private void startGame() {
        // Build player list
        for (int i = 0; i < clients.size(); i++) {
            Player p = new Player(i, "P" + (i + 1), SPAWN_POINTS[i][0], SPAWN_POINTS[i][1]);
            p.setCharacterIndex(chosenCharacters[i]);
            players.add(p);
        }

        // Spawn authoritative obstacles with a shared seed
        long seed = new java.util.Random().nextLong();
        java.util.Random rand = new java.util.Random(seed);
        obstacles.clear();
        for (int i = 0; i < 15; i++) {
            double ox = 100 + rand.nextDouble() * (MAP_WIDTH - 200);
            double oy = 100 + rand.nextDouble() * (MAP_HEIGHT - 200);
            obstacles.add(new Obstacle(ox, oy, 30, 30, "/assets/obstacles/Plants.png", true, 448, 0, 64, 96));
        }

        waveManager = new WaveManager(MAP_WIDTH, MAP_HEIGHT);
        gameRunning = true;

        // Tell all clients to switch to the game scene
        GamePacket.LobbyPacket start = new GamePacket.LobbyPacket(GamePacket.LobbyPacket.Type.START_GAME);
        start.connectedCount = clients.size();
        start.targetCount    = targetCount;
        start.seed           = seed;
        broadcast(start);

        if (onGameStarted != null) onGameStarted.run();

        Thread gt = new Thread(this::gameLoop, "server-game");
        gt.setDaemon(true);
        gt.start();
    }

    // -------------------------------------------------------
    // Game loop
    // -------------------------------------------------------

    private void gameLoop() {
        long last = System.currentTimeMillis();
        while (gameRunning) {
            long now  = System.currentTimeMillis();
            if (now - last >= TICK_MS) {
                last = now;
                tick();
                GamePacket.GameStatePacket snap = buildSnapshot();
                lastState = snap;
                broadcastState(snap);
            } else {
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            }
        }
        // Send final game-over state
        if (lastState != null) {
            lastState.gameOver = true;
            broadcastState(lastState);
        }
    }

    private void tick() {
        // ---- Apply client inputs ----
        for (ClientConnection cc : clients) {
            GamePacket.PlayerInputPacket inp = cc.getLatestInput();
            if (inp == null) continue;
            int idx = cc.getPlayerIndex();
            if (idx >= players.size()) continue;
            Player p = players.get(idx);
            p.setMovingUp(inp.movingUp);
            p.setMovingDown(inp.movingDown);
            p.setMovingLeft(inp.movingLeft);
            p.setMovingRight(inp.movingRight);
            p.setShooting(inp.shooting);
        }

        // ---- Update players ----
        for (Player p : players) {
            p.update(MAP_WIDTH, MAP_HEIGHT, obstacles);

            // Override aim angle with mouse aim for all active network clients
            if (p.isAlive()) {
                for (ClientConnection cc : clients) {
                    if (cc.getPlayerIndex() == p.getPlayerIndex()) {
                        GamePacket.PlayerInputPacket inp = cc.getLatestInput();
                        if (inp != null) {
                            double angle = Math.atan2(
                                    inp.mouseY - (p.getY() + Player.SIZE / 2.0),
                                    inp.mouseX - (p.getX() + Player.SIZE / 2.0)
                            );
                            p.setAimAngle(angle);
                        }
                        break;
                    }
                }
            }

            if (!p.isAlive() && p.getRespawnTimer() == 0 && anyAlive()) {
                p.respawn(SPAWN_POINTS[p.getPlayerIndex()][0], SPAWN_POINTS[p.getPlayerIndex()][1]);
            }
            // Auto-fire while shoot flag is set
            if (p.canShoot()) {
                fireBullet(p, p.getAimAngle());
            }
        }

        // ---- Wave / Gaod spawning ----
        int prevLevel = waveManager.getCurrentLevel();
        List<Gaod> spawned = waveManager.update(new ArrayList<>(gaods));
        for (Gaod g : spawned) {
            gaodIds.put(g, gaodIdCounter++);
            gaods.add(g);
        }
        if (waveManager.getCurrentLevel() > prevLevel) levelMessageTimer = 120;

        // ---- Update Gaods ----
        for (Gaod g : gaods) {
            g.update(players, obstacles);
            for (Player p : players) {
                if (p.isAlive() && g.isAlive() && g.isCollidingWithPlayer(p)) {
                    p.takeDamage(waveManager.getRespawnDelayFrames());
                }
            }
        }

        // ---- Update bullets ----
        for (Bullet b : bullets) {
            b.update(MAP_WIDTH, MAP_HEIGHT);
            for (Gaod g : gaods) {
                if (b.isActive() && g.isAlive() && b.isCollidingWith(g)) {
                    g.takeDamage(Bullet.DAMAGE);
                    b.deactivate();
                    if (!g.isAlive()) score += 10;
                }
            }
        }

        gaods.removeIf(g   -> !g.isAlive());
        bullets.removeIf(b -> !b.isActive());

        if (!anyAlive()) gameRunning = false;
        if (levelMessageTimer > 0) levelMessageTimer--;
    }

    private void fireBullet(Player p, double angle) {
        double bx = p.getX() + Player.SIZE / 2.0 - Bullet.SIZE / 2.0;
        double by = p.getY() + Player.SIZE / 2.0 - Bullet.SIZE / 2.0;
        bullets.add(new Bullet(bx, by, angle, p.getPlayerIndex()));
        p.triggerShootCooldown();
    }

    private boolean anyAlive() {
        return players.stream().anyMatch(Player::isAlive);
    }

    // -------------------------------------------------------
    // Disconnect
    // -------------------------------------------------------

    private void onClientDisconnect(int idx) {
        System.out.println("[Server] Player " + idx + " disconnected.");
        if (idx < players.size()) {
            Player p = players.get(idx);
            while (p.isAlive()) p.takeDamage(99999);
        }
        chosenReady[idx] = false;
        GamePacket.LobbyPacket left = new GamePacket.LobbyPacket(GamePacket.LobbyPacket.Type.PLAYER_LEFT);
        left.connectedCount = (int) clients.stream().filter(ClientConnection::isConnected).count();
        left.targetCount    = targetCount;
        broadcast(left);
    }

    // -------------------------------------------------------
    // State snapshot
    // -------------------------------------------------------

    private GamePacket.GameStatePacket buildSnapshot() {
        GamePacket.GameStatePacket s = new GamePacket.GameStatePacket();
        s.level            = waveManager.getCurrentLevel();
        s.score            = score;
        s.secondsRemaining = waveManager.getSecondsRemaining();
        s.gameOver         = !gameRunning;

        s.players = new ArrayList<>();
        for (Player p : players) {
            GamePacket.PlayerState ps = new GamePacket.PlayerState();
            ps.index        = p.getPlayerIndex();
            ps.x            = p.getX();
            ps.y            = p.getY();
            ps.health       = p.getHealth();
            ps.alive        = p.isAlive();
            ps.aimAngle     = p.getAimAngle();
            ps.respawnTimer = p.getRespawnTimer();
            ps.shooting     = p.isShooting();
            ps.characterIndex = p.getCharacterIndex();
            s.players.add(ps);
        }

        s.gaods = new ArrayList<>();
        for (Gaod g : gaods) {
            GamePacket.GaodState gs = new GamePacket.GaodState();
            gs.id    = gaodIds.getOrDefault(g, -1);
            gs.x     = g.getX();
            gs.y     = g.getY();
            gs.alive = g.isAlive();
            gs.type  = g.getType() == Gaod.Type.RED ? 1 : 0;
            gs.health    = g.getHealth();
            gs.maxHealth = g.getMaxHealth();
            s.gaods.add(gs);
        }

        s.bullets = new ArrayList<>();
        for (Bullet b : bullets) {
            GamePacket.BulletState bs = new GamePacket.BulletState();
            bs.x          = b.getX();
            bs.y          = b.getY();
            bs.ownerIndex = b.getOwnerIndex();
            bs.active     = b.isActive();
            bs.angle      = b.getAngle();
            s.bullets.add(bs);
        }

        return s;
    }

    // -------------------------------------------------------
    // Broadcast
    // -------------------------------------------------------

    private void broadcastState(GamePacket.GameStatePacket s) {
        for (ClientConnection cc : clients) cc.sendState(s);
    }

    private void broadcast(GamePacket.LobbyPacket pkt) {
        for (ClientConnection cc : clients) cc.sendLobby(pkt);
    }

    // -------------------------------------------------------
    // Getters for host UI
    // -------------------------------------------------------

    public GamePacket.GameStatePacket getLastState()   { return lastState; }
    public int  getConnectedCount()                    { return clients.size(); }
    public int  getTargetCount()                       { return targetCount; }
    public boolean isGameRunning()                     { return gameRunning; }

    public String getLocalIP() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (UnknownHostException e) { return "127.0.0.1"; }
    }
}