package top.ajitech.downmatica.compatible;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import top.ajitech.downmatica.gui.ConfigGui;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> new ConfigGui(parent, null) ;
    }
}
