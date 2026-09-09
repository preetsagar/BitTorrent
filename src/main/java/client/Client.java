package client;

import peer.PeerConnection;
import torrent.Torrent;
import tracker.Tracker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/** High-level operations: connect to a peer and download pieces / whole files. */
public final class Client {
    private static final int BLOCK_SIZE = 16 * 1024;

    private final Torrent torrent;

    public Client(Torrent torrent) {
        this.torrent = torrent;
    }

    /** Connects to the first advertised peer and completes handshake + choke negotiation. */
    public PeerConnection connectToReadyPeer() throws IOException, InterruptedException {
        List<Tracker.Peer> peers = Tracker.discoverPeers(torrent);
        if (peers.isEmpty()) {
            throw new IOException("Tracker returned no peers");
        }
        Tracker.Peer p = peers.get(0);
        PeerConnection conn = PeerConnection.connect(p.ip(), p.port());
        conn.handshake(torrent.infoHash);
        conn.recvExpecting(PeerConnection.BITFIELD);
        conn.send(PeerConnection.INTERESTED, new byte[0]);
        conn.recvExpecting(PeerConnection.UNCHOKE);
        return conn;
    }

    public long pieceLength(int index) {
        return Math.min(torrent.pieceLength, torrent.length - (long) index * torrent.pieceLength);
    }

    /** Downloads and verifies a single piece over an already-ready connection. */
    public byte[] downloadPiece(PeerConnection conn, int index) throws IOException {
        int pieceLen = Math.toIntExact(pieceLength(index));
        byte[] piece = new byte[pieceLen];

        int blocks = 0;
        for (int begin = 0; begin < pieceLen; begin += BLOCK_SIZE) {
            int length = Math.min(BLOCK_SIZE, pieceLen - begin);
            ByteBuffer req = ByteBuffer.allocate(12);
            req.putInt(index).putInt(begin).putInt(length);
            conn.send(PeerConnection.REQUEST, req.array());
            blocks++;
        }

        for (int i = 0; i < blocks; i++) {
            PeerConnection.Message m = conn.recvExpecting(PeerConnection.PIECE);
            ByteBuffer buf = ByteBuffer.wrap(m.payload());
            buf.getInt(); // piece index
            int begin = buf.getInt();
            buf.get(piece, begin, buf.remaining());
        }

        byte[] actual = Torrent.sha1(piece);
        if (!Arrays.equals(actual, torrent.pieceHashes.get(index))) {
            throw new IOException("Piece " + index + " hash mismatch");
        }
        return piece;
    }

    public byte[] downloadAll() throws IOException, InterruptedException {
        byte[] file = new byte[Math.toIntExact(torrent.length)];
        try (PeerConnection conn = connectToReadyPeer()) {
            int offset = 0;
            for (int i = 0; i < torrent.pieceHashes.size(); i++) {
                byte[] piece = downloadPiece(conn, i);
                System.arraycopy(piece, 0, file, offset, piece.length);
                offset += piece.length;
            }
        }
        return file;
    }
}
