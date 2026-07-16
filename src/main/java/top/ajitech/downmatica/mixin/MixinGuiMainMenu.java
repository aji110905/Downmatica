package top.ajitech.downmatica.mixin;

import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import top.ajitech.downmatica.core.ConfigHandler;
import top.ajitech.downmatica.gui.DownloadGui;
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

@Restriction(require = @Condition("litematica"))
@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiBase {
    @Shadow
    protected abstract int getButtonWidth();

    @Inject(method = "initGui", at = @At("RETURN"), remap = false)
    private void initGui(CallbackInfo ci) {
        if (!ConfigHandler.INSTANCE.litematicaMainMenuButton.getBooleanValue()) {
            return;
        }
        int height = 20;
        int width = getButtonWidth();
        int x = 12 + width + 20;//12为原方法中按钮的左边界，20为按钮之间的空隙
        int y = 30 + (2 + height) * 2;//30为原方法中按钮的顶边界，2为按钮之间的空隙
        addButton(
                new ButtonGeneric(x, y, width, height, getButtonText(), (IGuiIcon) null),
                (button, mouseButton) -> GuiBase.openGui(new DownloadGui(this, null))
        );
    }

    @Inject(
            method = "getButtonWidth",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void getButtonWidth(CallbackInfoReturnable<Integer> cir, @Local int width) {
        if (!ConfigHandler.INSTANCE.litematicaMainMenuButton.getBooleanValue()) {
            return;
        }
        cir.setReturnValue(Math.max(width, getStringWidth(getButtonText()) + 30));//30表示按钮左右留白
    }

    @Unique
    private String getButtonText() {
        return StringUtils.translate("downmatica.gui.litematica_main_menu.button.download_schematic.display");
    }
}
