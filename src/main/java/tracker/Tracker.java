package tracker;

import bencode.Bencode;
import client.PeerId;
import torrent.Torrent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Talks to an HTTP BitTorrent tracker. */
public final class Tracker {

    /** A peer address from the compact tracker response. */
    public record Peer(String ip, int port) {
        @Override
        public String toString() {
            return ip + ":" + port;
        }
    }

    public static List<Peer> discoverPeers(Torrent torrent) throws IOException, InterruptedException {
        return discoverPeers(torrent.announce, torrent.infoHash, torrent.length);
    }

    /**
     * Queries the tracker for peers. For magnet links the file length is unknown, so pass
     * {@code left = 1} as a placeholder (the tracker only needs it to be > 0).
     */
    public static List<Peer> discoverPeers(String announce, byte[] infoHash, long left)
            throws IOException, InterruptedException {
        String url = announce
                + "?info_hash=" + urlEncode(infoHash)
                + "&peer_id=" + urlEncode(PeerId.BYTES)
                + "&port=6881"
                + "&uploaded=0"
                + "&downloaded=0"
                + "&left=" + left
                + "&compact=1";

        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) Bencode.decode(response.body());
        if (body.containsKey("failure reason")) {
            throw new IOException("Tracker error: " + Bencode.asString(body.get("failure reason")));
        }
        return parseCompactPeers((byte[]) body.get("peers"));
    }

    static List<Peer> parseCompactPeers(byte[] peers) {
        List<Peer> out = new ArrayList<>();
        for (int i = 0; i + 6 <= peers.length; i += 6) {
            String ip = (peers[i] & 0xFF) + "." + (peers[i + 1] & 0xFF) + "."
                    + (peers[i + 2] & 0xFF) + "." + (peers[i + 3] & 0xFF);
            int port = ((peers[i + 4] & 0xFF) << 8) | (peers[i + 5] & 0xFF);
            out.add(new Peer(ip, port));
        }
        return out;
    }

    /** Percent-encodes every byte that isn't an RFC 3986 unreserved character. */
    static String urlEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%')
                        .append(Character.forDigit((c >> 4) & 0xF, 16))
                        .append(Character.forDigit(c & 0xF, 16));
            }
        }
        return sb.toString();
    }
}
