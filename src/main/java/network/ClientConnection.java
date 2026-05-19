package network;

import java.io.*;
import java.net.Socket;

/**
 * ClientConnection — server-side handler for ONE connected client.
 *
 * Lives on its own thread; reads PlayerInputPackets and stores the latest one.
 * GameServer polls getLatestInput() every simulation tick.
 */
public class ClientConnection implements Runnable {

    private final Socket   socket;
    private final int      playerIndex;
    private final Runnable onDisconnect;
    private final java.util.function.Consumer<GamePacket.LobbyPacket> onLobbyReceived;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    private volatile GamePacket.PlayerInputPacket latestInput = null;
    private volatile boolean connected = true;

    public ClientConnection(Socket socket, int playerIndex, Runnable onDisconnect,
                            java.util.function.Consumer<GamePacket.LobbyPacket> onLobbyReceived) {
        this.socket          = socket;
        this.playerIndex     = playerIndex;
        this.onDisconnect    = onDisconnect;
        this.onLobbyReceived = onLobbyReceived;
    }

    /** Must be called (on the accept thread) before start(). */
    public void initStreams() throws IOException {
        // ObjectOutputStream FIRST — both sides must open out before in
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (connected) {
                Object obj = in.readObject();
                if (obj instanceof GamePacket.PlayerInputPacket pkt) {
                    latestInput = pkt;
                } else if (obj instanceof GamePacket.LobbyPacket lp) {
                    if (onLobbyReceived != null) {
                        onLobbyReceived.accept(lp);
                    }
                }
            }
        } catch (EOFException | java.net.SocketException ignored) {
            // normal disconnect
        } catch (Exception e) {
            System.err.println("[Server] Client " + playerIndex + " error: " + e.getMessage());
        } finally {
            connected = false;
            close();
            if (onDisconnect != null) onDisconnect.run();
        }
    }

    // -------------------------------------------------------
    // Send helpers  (called from the server's game-loop thread)
    // -------------------------------------------------------

    public void sendState(GamePacket.GameStatePacket state) {
        send(state);
    }

    public void sendLobby(GamePacket.LobbyPacket pkt) {
        send(pkt);
    }

    private synchronized void send(Object obj) {
        if (!connected) return;
        try {
            out.writeObject(obj);
            out.reset();   // prevent stale object-graph caching
            out.flush();
        } catch (IOException e) {
            connected = false;
        }
    }

    public void close() {
        connected = false;
        try { socket.close(); } catch (IOException ignored) {}
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public GamePacket.PlayerInputPacket getLatestInput() { return latestInput; }
    public int     getPlayerIndex()                      { return playerIndex; }
    public boolean isConnected()                         { return connected; }
}
