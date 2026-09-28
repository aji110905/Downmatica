package top.ajitech.downmatica.util;

//#if MC < 260300
import net.minecraft.Util;
//#else
//$$ import org.lwjgl.sdl.SDLMisc;
//#endif
import java.net.URI;

public final class CompatibleUtil {
    private CompatibleUtil() {
    }

    public static void openUri(URI uri) {
        //#if MC < 260300
        Util.getPlatform().openUri(uri);
        //#else
        //$$ SDLMisc.SDL_OpenURL(uri.toString());
        //#endif
    }
}
