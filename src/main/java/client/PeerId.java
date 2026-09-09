package client;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/** Our client's peer id: 20 random ASCII bytes, stable for the lifetime of the process. */
public final class PeerId {
    public static final byte[] BYTES = generate();

    private PeerId() {
    }

    private static byte[] generate() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder("-CC0001-");
        while (sb.length() < 20) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
