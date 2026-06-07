package aji.downmatica.mixin;

import aji.downmatica.gui.DownloadGui;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiBase {
    @Unique
    private static final int LEFT_MARGIN = 12;
    @Unique
    private static final int TOP_MARGIN = 30;
    @Unique
    private static final int BUTTON_HORIZONTAL_SPACING = 10;
    @Unique
    private static final int BUTTON_VERTICAL_SPACING = 2;
    @Unique
    private static final int BUTTON_HEIGHT = 20;

    @Shadow
    protected abstract int getButtonWidth();

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void initGui(CallbackInfo ci) {
        final int width = getButtonWidth();
        final int x = LEFT_MARGIN + width + BUTTON_HORIZONTAL_SPACING * 2;
        final int y = TOP_MARGIN + (BUTTON_VERTICAL_SPACING + BUTTON_HEIGHT) * 2;
        addButton(
                new ButtonGeneric(x, y, width, BUTTON_HEIGHT, getButtonText(), (IGuiIcon) null),
                (button, mouseButton) -> GuiBase.openGui(new DownloadGui(this))
        );
    }

    @Inject(
            method = "getButtonWidth",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void getButtonWidth(CallbackInfoReturnable<Integer> cir, @Local(name = "width") int width) {
        //30表示按钮左右留白（原方法无常量，沿用字面量）
        cir.setReturnValue(Math.max(width, getStringWidth(getButtonText()) + 30));
    }

    @Unique
    private String getButtonText() {
        return StringUtils.translate("downmatica.gui.title");
    }
}
