package top.ajitech.downmatica.util;

import net.minecraft.client.Minecraft;

public final class GuiUtil {
    public static int getStringWidth(String string) {
        if (!StringUtil.hasText(string)) {
            return 0;
        }
        return Minecraft.getInstance().font.width(string);
    }

    public static int getFontHeight() {
        return Minecraft.getInstance().font.lineHeight - 1;//不知道为什么定义的是9个像素，而实际上是8个像素
    }
}
