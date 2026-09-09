import bencode.Bencode;
import client.Client;
import com.google.gson.Gson;
import magnet.Magnet;
import magnet.MagnetClient;
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
            case "info" -> printTorrentInfo(Torrent.parse(Path.of(args[1])));
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
            case "magnet_parse" -> {
                Magnet m = Magnet.parse(args[1]);
                System.out.println("Tracker URL: " + m.tracker());
                System.out.println("Info Hash: " + m.infoHashHex());
            }
            case "magnet_handshake" -> {
                Magnet m = Magnet.parse(args[1]);
                try (MagnetClient mc = MagnetClient.open(m)) {
                    System.out.println("Peer ID: " + Torrent.hex(mc.peerId));
                    System.out.println("Peer Metadata Extension ID: " + mc.peerUtMetadataId);
                }
            }
            case "magnet_info" -> {
                Magnet m = Magnet.parse(args[1]);
                try (MagnetClient mc = MagnetClient.open(m)) {
                    printTorrentInfo(mc.fetchMetadata());
                }
            }
            case "magnet_download_piece" -> {
                // magnet_download_piece -o <output> <magnet> <pieceIndex>
                String output = args[2];
                Magnet m = Magnet.parse(args[3]);
                int index = Integer.parseInt(args[4]);
                byte[] piece;
                try (MagnetClient mc = MagnetClient.open(m)) {
                    Torrent t = mc.fetchMetadata();
                    Client client = new Client(t);
                    client.prepare(mc.conn);
                    piece = client.downloadPiece(mc.conn, index);
                }
                Files.write(Path.of(output), piece);
                System.out.println("Piece " + index + " downloaded to " + output + ".");
            }
            case "magnet_download" -> {
                // magnet_download -o <output> <magnet>
                String output = args[2];
                Magnet m = Magnet.parse(args[3]);
                byte[] file;
                try (MagnetClient mc = MagnetClient.open(m)) {
                    Client client = new Client(mc.fetchMetadata());
                    client.prepare(mc.conn);
                    file = client.downloadAllOver(mc.conn);
                }
                Files.write(Path.of(output), file);
                System.out.println("Downloaded " + args[3] + " to " + output + ".");
            }
            default -> System.out.println("Unknown command: " + command);
        }
    }

    private static void printTorrentInfo(Torrent t) {
        System.out.println("Tracker URL: " + t.announce);
        System.out.println("Length: " + t.length);
        System.out.println("Info Hash: " + t.infoHashHex());
        System.out.println("Piece Length: " + t.pieceLength);
        System.out.println("Piece Hashes:");
        for (byte[] h : t.pieceHashes) {
            System.out.println(Torrent.hex(h));
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
