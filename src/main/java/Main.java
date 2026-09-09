import bencode.Bencode;
import com.google.gson.Gson;
import torrent.Torrent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        String command = args[0];
        switch (command) {
            case "decode" -> {
                Object decoded = Bencode.decode(args[1]);
                System.out.println(gson.toJson(jsonReady(decoded)));
            }
            case "info" -> {
                Torrent t = Torrent.parse(Path.of(args[1]));
                System.out.println("Tracker URL: " + t.announce);
                System.out.println("Length: " + t.length);
                System.out.println("Info Hash: " + t.infoHashHex());
                System.out.println("Piece Length: " + t.pieceLength);
                System.out.println("Piece Hashes:");
                for (byte[] h : t.pieceHashes) {
                    System.out.println(Torrent.hex(h));
                }
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }

    /** Recursively turn bencode byte[] strings into Java Strings so Gson emits them as JSON strings. */
    @SuppressWarnings("unchecked")
    private static Object jsonReady(Object o) {
        if (o instanceof byte[] b) {
            return new String(b, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (o instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object e : list) {
                out.add(jsonReady(e));
            }
            return out;
        }
        if (o instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<String, Object>) map).entrySet()) {
                out.put(e.getKey().toString(), jsonReady(e.getValue()));
            }
            return out;
        }
        return o;
    }
}
