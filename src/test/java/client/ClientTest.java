package client;

import org.junit.jupiter.api.Test;
import torrent.Torrent;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientTest {

    private Client clientFor(String name) throws Exception {
        return new Client(Torrent.parse(Path.of("src/test/resources/torrents", name)));
    }

    @Test
    void pieceLengthHandlesLastShortPiece() throws Exception {
        Client codercat = clientFor("codercat.gif.torrent");
        assertEquals(262144, codercat.pieceLength(0));
        assertEquals(110536, codercat.pieceLength(11)); // 2994120 - 11*262144

        Client congrats = clientFor("congratulations.gif.torrent");
        assertEquals(262144, congrats.pieceLength(2));
        assertEquals(34460, congrats.pieceLength(3)); // 820892 - 3*262144

        Client itsworking = clientFor("itsworking.gif.torrent");
        assertEquals(190404, itsworking.pieceLength(9)); // 2549700 - 9*262144
    }
}
