package in.org.projecteka.hdaf.link.link;

import java.util.Base64;

public class TokenUtils {
    public static String readUserId(String authorizationHeader) {
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(authorizationHeader));
    }
}
