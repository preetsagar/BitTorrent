import bencode.Bencode;
import client.Client;
import com.google.gson.Gson;
import peer.PeerConnection;
import torrent.Torrent;
import tracker.Tracker;

import java.nio.file.Files;
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
            case "peers" -> {
                Torrent t = Torrent.parse(Path.of(args[1]));
                for (Tracker.Peer p : Tracker.discoverPeers(t)) {
                    System.out.println(p);
                }
            }
            case "handshake" -> {
                Torrent t = Torrent.parse(Path.of(args[1]));
                String[] hp = args[2].split(":");
                try (PeerConnection conn = PeerConnection.connect(hp[0], Integer.parseInt(hp[1]))) {
                    byte[] peerId = conn.handshake(t.infoHash);
                    System.out.println("Peer ID: " + Torrent.hex(peerId));
                }
            }
            case "download_piece" -> {
                // download_piece -o <output> <torrent> <pieceIndex>
                String output = args[2];
                Torrent t = Torrent.parse(Path.of(args[3]));
                int index = Integer.parseInt(args[4]);
                Client client = new Client(t);
                byte[] piece;
                try (PeerConnection conn = client.connectToReadyPeer()) {
                    piece = client.downloadPiece(conn, index);
                }
                Files.write(Path.of(output), piece);
                System.out.println("Piece " + index + " downloaded to " + output + ".");
            }
            case "download" -> {
                // download -o <output> <torrent>
                String output = args[2];
                Torrent t = Torrent.parse(Path.of(args[3]));
                byte[] file = new Client(t).downloadAll();
                Files.write(Path.of(output), file);
                System.out.println("Downloaded " + args[3] + " to " + output + ".");
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
