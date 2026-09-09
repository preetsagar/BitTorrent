package peer;

import client.PeerId;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** A TCP connection to a single BitTorrent peer. */
public final class PeerConnection implements AutoCloseable {
    private static final String PROTOCOL = "BitTorrent protocol";

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;

    private PeerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = socket.getOutputStream();
    }

    public static PeerConnection connect(String host, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port));
        return new PeerConnection(s);
    }

    /** Sends our handshake and returns the peer's 20-byte peer id. */
    public byte[] handshake(byte[] infoHash) throws IOException {
        byte[] msg = new byte[68];
        msg[0] = 19;
        System.arraycopy(PROTOCOL.getBytes(StandardCharsets.US_ASCII), 0, msg, 1, 19);
        // bytes 20..27 reserved, left as zero
        System.arraycopy(infoHash, 0, msg, 28, 20);
        System.arraycopy(PeerId.BYTES, 0, msg, 48, 20);
        out.write(msg);
        out.flush();

        byte[] resp = new byte[68];
        in.readFully(resp);
        byte[] peerId = new byte[20];
        System.arraycopy(resp, 48, peerId, 0, 20);
        return peerId;
    }

    public DataInputStream in() {
        return in;
    }

    public OutputStream out() {
        return out;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
