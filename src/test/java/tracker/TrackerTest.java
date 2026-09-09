package tracker;

import bencode.Bencode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackerTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesCompactPeersFromFixture() throws Exception {
        byte[] raw = Files.readAllBytes(Path.of("src/test/resources/response/response.txt"));
        Map<String, Object> body = (Map<String, Object>) Bencode.decode(raw);
        List<Tracker.Peer> peers = Tracker.parseCompactPeers((byte[]) body.get("peers"));

        assertEquals(9, peers.size());
        assertEquals("188.119.61.177:6881", peers.get(0).toString());
        assertEquals("216.195.129.27:60000", peers.get(8).toString());
    }

    @Test
    void urlEncodesRawInfoHashBytewise() {
        byte[] hash = new byte[]{(byte) 0xd6, (byte) 0x9f, (byte) 0x91, (byte) 0xe6, 'A'};
        assertEquals("%d6%9f%91%e6A", Tracker.urlEncode(hash));
    }
}
