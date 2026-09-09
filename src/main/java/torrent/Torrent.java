package torrent;

import bencode.Bencode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A parsed .torrent file (single-file torrents only, which is all this challenge uses). */
public final class Torrent {
    public final String announce;
    public final String name;
    public final long length;
    public final long pieceLength;
    public final byte[] infoHash;      // 20 raw bytes
    public final List<byte[]> pieceHashes; // each 20 raw bytes

    private Torrent(String announce, String name, long length, long pieceLength,
                    byte[] infoHash, List<byte[]> pieceHashes) {
        this.announce = announce;
        this.name = name;
        this.length = length;
        this.pieceLength = pieceLength;
        this.infoHash = infoHash;
        this.pieceHashes = pieceHashes;
    }

    public static Torrent parse(Path file) throws IOException {
        return parse(Files.readAllBytes(file));
    }

    @SuppressWarnings("unchecked")
    public static Torrent parse(byte[] data) {
        Map<String, Object> root = (Map<String, Object>) Bencode.decode(data);
        Map<String, Object> info = (Map<String, Object>) root.get("info");

        String announce = Bencode.asString(root.get("announce"));
        String name = Bencode.asString(info.get("name"));
        long length = (Long) info.get("length");
        long pieceLength = (Long) info.get("piece length");
        byte[] infoHash = sha1(Bencode.rawValueOfKey(data, "info"));

        byte[] pieces = (byte[]) info.get("pieces");
        List<byte[]> pieceHashes = new ArrayList<>();
        for (int i = 0; i < pieces.length; i += 20) {
            byte[] h = new byte[20];
            System.arraycopy(pieces, i, h, 0, 20);
            pieceHashes.add(h);
        }

        return new Torrent(announce, name, length, pieceLength, infoHash, pieceHashes);
    }

    public String infoHashHex() {
        return hex(infoHash);
    }

    public static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public static byte[] sha1(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
