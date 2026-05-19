package network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * GameClient — connects to a GameServer over TCP.
 *
 * Responsibilities:
 *   - Send PlayerInputPacket every frame (called by NetworkGameScene)
 *   - Receive GameStatePacket / LobbyPacket on a background thread
 *   - Fire callbacks so the UI can react without blocking
 */
public class GameClient {

    private final String host;
    private final int    port;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    private volatile boolean connected = false;
    private volatile int     assignedIndex = -1;
    private volatile GamePacket.GameStatePacket latestState = null;

    private Consumer<GamePacket.GameStatePacket> onStateReceived;
    private Consumer<GamePacket.LobbyPacket>     onLobbyReceived;
    private Runnable                             onDisconnect;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // -------------------------------------------------------
    // Connect / disconnect
    // -------------------------------------------------------

    /** Blocking connect — call off the FX thread. Throws on failure. */
    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 6000);
        socket.setTcpNoDelay(true);

        // IMPORTANT: ObjectOutputStream first, then ObjectInputStream
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;

        Thread recv = new Thread(this::receiveLoop, "client-recv");
        recv.setDaemon(true);
        recv.start();
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // -------------------------------------------------------
    // Send input  (called every frame from game loop thread)
    // -------------------------------------------------------

    public void sendInput(GamePacket.PlayerInputPacket pkt) {
        if (!connected) return;
        try {
            synchronized (out) {
                out.writeObject(pkt);
                out.reset();
                out.flush();
            }
        } catch (IOException e) {
            connected = false;
        }
    }

    public void sendLobby(GamePacket.LobbyPacket pkt) {
        if (!connected) return;
        try {
            synchronized (out) {
                out.writeObject(pkt);
                out.reset();
                out.flush();
            }
        } catch (IOException e) {
            connected = false;
        }
    }

    // -------------------------------------------------------
    // Receive loop (background thread)
    // -------------------------------------------------------

    private void receiveLoop() {
        try {
            while (connected) {
                Object obj = in.readObject();
                if (obj instanceof GamePacket.GameStatePacket state) {
                    latestState = state;
                    if (onStateReceived != null) onStateReceived.accept(state);
                } else if (obj instanceof GamePacket.LobbyPacket lobby) {
                    if (lobby.type == GamePacket.LobbyPacket.Type.ASSIGN_INDEX) {
                        assignedIndex = lobby.assignedIndex;
                    }
                    if (onLobbyReceived != null) onLobbyReceived.accept(lobby);
                }
            }
        } catch (EOFException | SocketException ignored) {
            // server closed cleanly
        } catch (Exception e) {
            System.err.println("[Client] Receive error: " + e.getMessage());
        } finally {
            connected = false;
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    // -------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------

    public void setOnStateReceived(Consumer<GamePacket.GameStatePacket> cb) { onStateReceived = cb; }
    public void setOnLobbyReceived(Consumer<GamePacket.LobbyPacket>     cb) { onLobbyReceived = cb; }
    public void setOnDisconnect(Runnable cb)                                { onDisconnect    = cb; }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public boolean isConnected()           { return connected; }
    public int     getAssignedIndex()      { return assignedIndex; }
    public GamePacket.GameStatePacket getLatestState() { return latestState; }
}
