package aji.downmatica.network;

import aji.downmatica.entry.Schematic;

import java.util.ArrayList;
import java.util.Collection;

public final class SchematicAcquirers {
    private static final ArrayList<SchematicAcquirer> acquirers = new ArrayList<>();

    private SchematicAcquirers(){

    }

    public static void register(SchematicAcquirer acquirer){
        acquirers.add(acquirer);
    }

    public static Collection<Schematic> getAllSchematics(){
        ArrayList<Schematic> schematics = new ArrayList<>();
        for(SchematicAcquirer acquirer : acquirers){
            schematics.addAll(acquirer.getSchematics());
        }
        return schematics;
    }
}
