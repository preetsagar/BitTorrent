package peer;

import bencode.Bencode;
import client.PeerId;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

/** A TCP connection to a single BitTorrent peer. */
public final class PeerConnection implements AutoCloseable {
    private static final String PROTOCOL = "BitTorrent protocol";

    /** Reserved bytes advertising support for the extension protocol (BEP 10). */
    public static final byte[] EXTENSION_RESERVED = {0, 0, 0, 0, 0, 16, 0, 0};

    /** Our ut_metadata extension id sent in the extension handshake. */
    public static final int UT_METADATA_ID = 1;

    private final Socket socket;
    private final DataInputStream in;
    private final OutputStream out;
    private boolean peerSupportsExtensions;

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

    /** Sends our handshake with all-zero reserved bytes and returns the peer's 20-byte peer id. */
    public byte[] handshake(byte[] infoHash) throws IOException {
        return handshake(infoHash, new byte[8]);
    }

    /** Sends our handshake with the given reserved bytes and returns the peer's 20-byte peer id. */
    public byte[] handshake(byte[] infoHash, byte[] reserved) throws IOException {
        byte[] msg = new byte[68];
        msg[0] = 19;
        System.arraycopy(PROTOCOL.getBytes(StandardCharsets.US_ASCII), 0, msg, 1, 19);
        System.arraycopy(reserved, 0, msg, 20, 8);
        System.arraycopy(infoHash, 0, msg, 28, 20);
        System.arraycopy(PeerId.BYTES, 0, msg, 48, 20);
        out.write(msg);
        out.flush();

        byte[] resp = new byte[68];
        in.readFully(resp);
        peerSupportsExtensions = (resp[25] & 0x10) != 0;
        byte[] peerId = new byte[20];
        System.arraycopy(resp, 48, peerId, 0, 20);
        return peerId;
    }

    public boolean peerSupportsExtensions() {
        return peerSupportsExtensions;
    }

    /** Result of the BEP 10 extension handshake: the peer's ut_metadata id and the metadata size. */
    public record ExtensionHandshake(int utMetadataId, int metadataSize) {
    }

    /** Performs the extension handshake and returns what the peer advertised. */
    @SuppressWarnings("unchecked")
    public ExtensionHandshake extensionHandshake() throws IOException {
        String dict = "d1:md11:ut_metadatai" + UT_METADATA_ID + "eee";
        send(EXTENDED, prefixByte(0, dict.getBytes(StandardCharsets.ISO_8859_1)));

        Message m = recvExpecting(EXTENDED);
        Map<String, Object> handshake =
                (Map<String, Object>) Bencode.decodePrefix(m.payload(), 1).value();
        Map<String, Object> mDict = (Map<String, Object>) handshake.get("m");
        int utMetadataId = ((Long) mDict.get("ut_metadata")).intValue();
        int metadataSize = handshake.containsKey("metadata_size")
                ? ((Long) handshake.get("metadata_size")).intValue() : 0;
        return new ExtensionHandshake(utMetadataId, metadataSize);
    }

    /** Requests metadata piece 0 and returns the raw bencoded info dictionary. */
    public byte[] requestMetadata(int peerUtMetadataId) throws IOException {
        String request = "d8:msg_typei0e5:piecei0ee";
        send(EXTENDED, prefixByte(peerUtMetadataId, request.getBytes(StandardCharsets.ISO_8859_1)));

        Message m = recvExpecting(EXTENDED);
        // payload: [ext msg id][bencoded {msg_type:1, piece:0, total_size:N}][raw info dict...]
        Bencode.Decoded header = Bencode.decodePrefix(m.payload(), 1);
        return Arrays.copyOfRange(m.payload(), header.end(), m.payload().length);
    }

    private static byte[] prefixByte(int b, byte[] rest) {
        byte[] out = new byte[rest.length + 1];
        out[0] = (byte) b;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
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
