package bencode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal bencode decoder.
 *
 * Decoded value types:
 *   string  -> byte[] (callers that want text decode as UTF-8; see {@link #asString})
 *   integer -> Long
 *   list    -> List<Object>
 *   dict    -> Map<String, Object> (insertion order == file order == sorted)
 */
public final class Bencode {
    private final byte[] data;
    private int pos;

    private Bencode(byte[] data) {
        this.data = data;
    }

    public static Object decode(byte[] data) {
        Bencode b = new Bencode(data);
        Object value = b.parse();
        return value;
    }

    public static Object decode(String s) {
        return decode(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    public static String asString(Object o) {
        return new String((byte[]) o, StandardCharsets.UTF_8);
    }

    private Object parse() {
        byte c = data[pos];
        if (c == 'i') {
            return parseInteger();
        } else if (c == 'l') {
            return parseList();
        } else if (c == 'd') {
            return parseDict();
        } else if (c >= '0' && c <= '9') {
            return parseString();
        }
        throw new IllegalArgumentException("Unexpected bencode token '" + (char) c + "' at " + pos);
    }

    private byte[] parseString() {
        int colon = pos;
        while (data[colon] != ':') {
            colon++;
        }
        int len = Integer.parseInt(new String(data, pos, colon - pos, StandardCharsets.US_ASCII));
        int start = colon + 1;
        byte[] out = new byte[len];
        System.arraycopy(data, start, out, 0, len);
        pos = start + len;
        return out;
    }

    private Long parseInteger() {
        int end = pos + 1;
        while (data[end] != 'e') {
            end++;
        }
        long value = Long.parseLong(new String(data, pos + 1, end - pos - 1, StandardCharsets.US_ASCII));
        pos = end + 1;
        return value;
    }

    private List<Object> parseList() {
        pos++; // 'l'
        List<Object> list = new ArrayList<>();
        while (data[pos] != 'e') {
            list.add(parse());
        }
        pos++; // 'e'
        return list;
    }

    private Map<String, Object> parseDict() {
        pos++; // 'd'
        Map<String, Object> map = new LinkedHashMap<>();
        while (data[pos] != 'e') {
            String key = new String(parseString(), StandardCharsets.UTF_8);
            map.put(key, parse());
        }
        pos++; // 'e'
        return map;
    }
}
