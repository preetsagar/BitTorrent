package magnet;

import peer.PeerConnection;
import torrent.Torrent;
import tracker.Tracker;

import java.io.IOException;
import java.util.List;

/**
 * Drives a magnet-link session: discover a peer, do the base + extension handshakes,
 * then (optionally) fetch the torrent metadata over ut_metadata.
 */
public final class MagnetClient implements AutoCloseable {
    public final Magnet magnet;
    public final PeerConnection conn;
    public final byte[] peerId;
    public final int peerUtMetadataId;

    private MagnetClient(Magnet magnet, PeerConnection conn, byte[] peerId, int peerUtMetadataId) {
        this.magnet = magnet;
        this.conn = conn;
        this.peerId = peerId;
        this.peerUtMetadataId = peerUtMetadataId;
    }

    public static MagnetClient open(Magnet magnet) throws IOException, InterruptedException {
        List<Tracker.Peer> peers = Tracker.discoverPeers(magnet.tracker(), magnet.infoHash(), 1);
        if (peers.isEmpty()) {
            throw new IOException("Tracker returned no peers");
        }
        Tracker.Peer p = peers.get(0);
        PeerConnection conn = PeerConnection.connect(p.ip(), p.port());
        byte[] peerId = conn.handshake(magnet.infoHash(), PeerConnection.EXTENSION_RESERVED);
        PeerConnection.ExtensionHandshake ext = conn.extensionHandshake();
        return new MagnetClient(magnet, conn, peerId, ext.utMetadataId());
    }

    /** Fetches the info dictionary via ut_metadata and returns a fully-populated Torrent. */
    public Torrent fetchMetadata() throws IOException {
        byte[] infoDict = conn.requestMetadata(peerUtMetadataId);
        return Torrent.fromInfoDict(magnet.tracker(), infoDict);
    }

    @Override
    public void close() throws IOException {
        conn.close();
    }
}
