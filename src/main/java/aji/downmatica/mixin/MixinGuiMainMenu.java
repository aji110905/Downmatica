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
    @Shadow
    protected abstract int getButtonWidth();

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void initGui(CallbackInfo ci) {
        int width = getButtonWidth();
        int x = 12 + width + 20;
        int y = 30 + 22 + 22;
        ButtonGeneric buttonGeneric = new ButtonGeneric(x, y, width, 20, getButtonText(), (IGuiIcon) null);
        addButton(buttonGeneric, (button, mouseButton) -> GuiBase.openGui(new DownloadGui(this)));
    }

    @Inject(
            method = "getButtonWidth",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void getButtonWidth(CallbackInfoReturnable<Integer> cir, @Local(name = "width") int width) {
        cir.setReturnValue(Math.max(width, getStringWidth(getButtonText()) + 30));
    }

    @Unique
    private String getButtonText() {
        return StringUtils.translate("downmatica.gui.title");
    }
}
