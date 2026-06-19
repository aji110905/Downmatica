package aji.downmatica.util;

public final class FileUtil {
    private FileUtil() {

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
