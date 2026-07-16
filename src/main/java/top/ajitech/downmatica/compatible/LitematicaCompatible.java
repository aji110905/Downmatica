package top.ajitech.downmatica.compatible;

import net.fabricmc.loader.api.FabricLoader;

public final class LitematicaCompatible {
    private LitematicaCompatible() {
    }

    public static boolean isLitematicaInstalled() {
        return FabricLoader.getInstance().isModLoaded("litematica");
    }
}
