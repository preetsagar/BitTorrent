package torrent;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorrentTest {
    private static final String TRACKER = "http://bittorrent-test-tracker.codecrafters.io/announce";

    private Torrent load(String name) throws Exception {
        return Torrent.parse(Path.of("src/test/resources/torrents", name));
    }

    @Test
    void parsesCodercat() throws Exception {
        Torrent t = load("codercat.gif.torrent");
        assertEquals(TRACKER, t.announce);
        assertEquals(2994120L, t.length);
        assertEquals("c77829d2a77d6516f88cd7a3de1a26abcbfab0db", t.infoHashHex());
    }

    @Test
    void parsesCongratulations() throws Exception {
        Torrent t = load("congratulations.gif.torrent");
        assertEquals(820892L, t.length);
        assertEquals("1cad4a486798d952614c394eb15e75bec587fd08", t.infoHashHex());
    }

    @Test
    void parsesItsworking() throws Exception {
        Torrent t = load("itsworking.gif.torrent");
        assertEquals(2549700L, t.length);
        assertEquals("70edcac2611a8829ebf467a6849f5d8408d9d8f4", t.infoHashHex());
    }

    @Test
    void pieceHashesAre20BytesAndCoverFile() throws Exception {
        Torrent t = load("codercat.gif.torrent");
        for (byte[] h : t.pieceHashes) {
            assertEquals(20, h.length);
            assertEquals(40, Torrent.hex(h).length());
        }
        long expectedPieces = (t.length + t.pieceLength - 1) / t.pieceLength;
        assertEquals(expectedPieces, t.pieceHashes.size());
        assertTrue(t.pieceLength > 0);
    }
}
