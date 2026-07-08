package top.ajitech.downmatica.core;

import top.ajitech.downmatica.DownmaticaMod;
import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class SchematicAcquirerManager {
    public static final SchematicAcquirerManager INSTANCE = new SchematicAcquirerManager();

    private final CopyOnWriteArrayList<SchematicAcquirer> acquirers = new CopyOnWriteArrayList<>();

    private SchematicAcquirerManager(){

    }

    /**
     * 此方法供内部调用，不推荐直接使用。
     * <p>
     * 未来版本中，该方法的签名或行为可能会在不预先通知的情况下发生变化。
     * <p>
     * 建议使用{@link SchematicAcquirer#register(SchematicAcquirer)}作为替代，
     * 该方法会保持向下兼容性，而此方法不会。
     */
    public void register(SchematicAcquirer acquirer) {
        acquirers.add(acquirer);
    }

    public Collection<Schematic> getAllSchematics(){
        ArrayList<Schematic> schematics = new ArrayList<>();
        for(SchematicAcquirer acquirer : acquirers){
            try {
                schematics.addAll(acquirer.getSchematics());
            } catch (Exception e) {
                DownmaticaMod.LOGGER.error("Failed to get schematics from acquirer {}", acquirer.getClass().getName(), e);
            }
        }
        return schematics;
    }
}
