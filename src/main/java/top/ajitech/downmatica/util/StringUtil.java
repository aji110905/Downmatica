package top.ajitech.downmatica.util;

public final class StringUtil {
    private StringUtil() {

    }

    public static boolean hasText(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
