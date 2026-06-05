package aji.downmatica.network;

import aji.downmatica.entry.Schematic;

import java.util.Collection;

@FunctionalInterface
public interface SchematicAcquirer {
    Collection<Schematic> getSchematics();
}

