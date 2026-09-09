package bencode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BencodeTest {

    @Test
    void decodesString() {
        assertEquals("hello", Bencode.asString(Bencode.decode("5:hello")));
    }

    @Test
    void decodesInteger() {
        assertEquals(52L, Bencode.decode("i52e"));
        assertEquals(-52L, Bencode.decode("i-52e"));
        assertEquals(4294967300L, Bencode.decode("i4294967300e"));
    }

    @Test
    void decodesList() {
        assertEquals(List.of(), Bencode.decode("le"));
        List<?> l = (List<?>) Bencode.decode("l5:helloi52ee");
        assertEquals("hello", Bencode.asString(l.get(0)));
        assertEquals(52L, l.get(1));
    }

    @Test
    void decodesNestedList() {
        List<?> l = (List<?>) Bencode.decode("lli4eei5ee");
        assertEquals(5L, l.get(1));
        assertEquals(4L, ((List<?>) l.get(0)).get(0));
    }

    @Test
    void decodesDict() {
        assertEquals(Map.of(), Bencode.decode("de"));
        Map<?, ?> d = (Map<?, ?>) Bencode.decode("d3:foo3:bar5:helloi52ee");
        assertEquals("bar", Bencode.asString(d.get("foo")));
        assertEquals(52L, d.get("hello"));
    }

    @Test
    void decodePrefixReportsEndOfValue() {
        byte[] data = "d8:msg_typei1e5:piecei0ee<INFODICT>".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        Bencode.Decoded d = Bencode.decodePrefix(data, 0);
        assertEquals("<INFODICT>", new String(data, d.end(), data.length - d.end(),
                java.nio.charset.StandardCharsets.ISO_8859_1));
        assertEquals(1L, ((Map<?, ?>) d.value()).get("msg_type"));
    }

    @Test
    void nestedDict() {
        Map<?, ?> d = (Map<?, ?>) Bencode.decode(
                "d10:inner_dictd4:key16:value14:key2i42e8:list_keyl5:item15:item2i3eeee");
        Map<?, ?> inner = (Map<?, ?>) d.get("inner_dict");
        assertEquals("value1", Bencode.asString(inner.get("key1")));
        assertEquals(42L, inner.get("key2"));
        assertEquals(3L, ((List<?>) inner.get("list_key")).get(2));
    }
}
