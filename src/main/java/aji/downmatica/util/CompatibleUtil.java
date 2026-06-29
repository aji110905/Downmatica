package aji.downmatica.util;

//#if MC < 260100
import net.minecraft.Util;
//#else
//$$ import net.minecraft.util.Util;
//#endif

import java.net.URI;

public final class CompatibleUtil {
    private CompatibleUtil() {
    }

    public static void openUri(URI uri) {
        Util.getPlatform().openUri(uri);
    }
}
