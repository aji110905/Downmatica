package aji.downmatica.entry;

import java.util.Collection;

@FunctionalInterface
public interface SchematicAcquirer {
    Collection<Schematic> getSchematics();
}

