package aji.downmatica.util;

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

    public static boolean isValidFileName(String name) {
        if (name == null || name.trim().isEmpty() || name.equals(".") || name.equals("..")) {
            return false;
        }
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            if (name.matches(".*[\\\\/:*?\"<>|].*")) {
                return false;
            }
            String upper = name.toUpperCase();
            String base = upper.contains(".") ? upper.substring(0, upper.lastIndexOf('.')) : upper;
            if (base.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
                return false;
            }
            if (name.endsWith(" ") || name.endsWith(".")) {
                return false;
            }
        }
        if (name.indexOf('/') >= 0 || name.indexOf('\0') >= 0) {
            return false;
        }
        if (os.contains("mac")) {
            if (name.startsWith(".")) return false;
        }
        return name.length() <= 255;
    }
}
