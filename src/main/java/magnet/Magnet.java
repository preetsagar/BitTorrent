package magnet;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** A parsed magnet link: magnet:?xt=urn:btih:&lt;hex&gt;&dn=&lt;name&gt;&tr=&lt;tracker&gt; */
public record Magnet(byte[] infoHash, String infoHashHex, String name, String tracker) {

    public static Magnet parse(String link) {
        String query = link.substring(link.indexOf('?') + 1);
        String infoHashHex = null;
        String name = null;
        String tracker = null;
        for (String param : query.split("&")) {
            int eq = param.indexOf('=');
            String key = param.substring(0, eq);
            String value = param.substring(eq + 1);
            switch (key) {
                case "xt" -> infoHashHex = value.substring("urn:btih:".length());
                case "dn" -> name = URLDecoder.decode(value, StandardCharsets.UTF_8);
                case "tr" -> tracker = URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }

        byte[] infoHash = new byte[20];
        for (int i = 0; i < 20; i++) {
            infoHash[i] = (byte) Integer.parseInt(infoHashHex.substring(i * 2, i * 2 + 2), 16);
        }
        return new Magnet(infoHash, infoHashHex, name, tracker);
    }
}
