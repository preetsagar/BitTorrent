package magnet;

import org.junit.jupiter.api.Test;
import torrent.Torrent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MagnetTest {

    @Test
    void parsesXtDnTr() {
        Magnet m = Magnet.parse("magnet:?xt=urn:btih:ad42ce8109f54c99613ce38f9b4d87e70f24a165"
                + "&dn=magnet1.gif&tr=http%3A%2F%2Fbittorrent-test-tracker.codecrafters.io%2Fannounce");

        assertEquals("ad42ce8109f54c99613ce38f9b4d87e70f24a165", m.infoHashHex());
        assertEquals("magnet1.gif", m.name());
        assertEquals("http://bittorrent-test-tracker.codecrafters.io/announce", m.tracker());
        assertEquals(20, m.infoHash().length);
    }

    @Test
    void infoHashBytesRoundTripToHex() {
        Magnet m = Magnet.parse("magnet:?xt=urn:btih:3f994a835e090238873498636b98a3e78d1c34ca&dn=x&tr=y");
        assertArrayEquals(m.infoHash(),
                hexToBytes("3f994a835e090238873498636b98a3e78d1c34ca"));
        assertEquals(m.infoHashHex(), Torrent.hex(m.infoHash()));
    }

    private static byte[] hexToBytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }
}
