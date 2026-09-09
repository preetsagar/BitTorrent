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

    /** A peer wire protocol message (after the length prefix has been stripped). */
    public record Message(int id, byte[] payload) {
    }

    public static final int CHOKE = 0;
    public static final int UNCHOKE = 1;
    public static final int INTERESTED = 2;
    public static final int BITFIELD = 5;
    public static final int REQUEST = 6;
    public static final int PIECE = 7;
    public static final int EXTENDED = 20;

    public void send(int id, byte[] payload) throws IOException {
        int len = 1 + payload.length;
        byte[] frame = new byte[4 + len];
        frame[0] = (byte) (len >>> 24);
        frame[1] = (byte) (len >>> 16);
        frame[2] = (byte) (len >>> 8);
        frame[3] = (byte) len;
        frame[4] = (byte) id;
        System.arraycopy(payload, 0, frame, 5, payload.length);
        out.write(frame);
        out.flush();
    }

    /** Reads the next message, transparently skipping keep-alives. */
    public Message recv() throws IOException {
        while (true) {
            int len = in.readInt();
            if (len == 0) {
                continue; // keep-alive
            }
            int id = in.readUnsignedByte();
            byte[] payload = new byte[len - 1];
            in.readFully(payload);
            return new Message(id, payload);
        }
    }

    /** Reads messages until one with {@code expectedId} arrives. */
    public Message recvExpecting(int expectedId) throws IOException {
        while (true) {
            Message m = recv();
            if (m.id() == expectedId) {
                return m;
            }
        }
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
