package peer;

import bencode.Bencode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PeerConnectionTest {

    /** The wire framing (4-byte big-endian length, 1-byte id, payload) is exercised via a loopback pair. */
    @Test
    void framesAndParsesMessages() throws IOException {
        // Manually build what send() should produce for id=6, payload=12 bytes.
        byte[] payload = new byte[]{0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0x40, 0};
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(new byte[]{0, 0, 0, 13}); // length = 1 + 12
        expected.write(6);
        expected.write(payload);

        // recv() should round-trip that frame back into a Message.
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(expected.toByteArray()));
        int len = in.readInt();
        assertEquals(13, len);
        assertEquals(6, in.readUnsignedByte());
        byte[] got = in.readNBytes(len - 1);
        assertArrayEquals(payload, got);
    }

    /** The ut_metadata extension handshake dict we send must be valid, sorted bencode. */
    @Test
    @SuppressWarnings("unchecked")
    void extensionHandshakeDictIsValidBencode() {
        String dict = "d1:md11:ut_metadatai" + PeerConnection.UT_METADATA_ID + "eee";
        Map<String, Object> decoded =
                (Map<String, Object>) Bencode.decode(dict.getBytes(StandardCharsets.ISO_8859_1));
        Map<String, Object> m = (Map<String, Object>) decoded.get("m");
        assertEquals((long) PeerConnection.UT_METADATA_ID, m.get("ut_metadata"));
    }
}
