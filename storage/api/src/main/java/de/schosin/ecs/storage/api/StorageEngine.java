package de.schosin.ecs.storage.api;

import java.util.ServiceLoader;

public interface StorageEngine extends ComponentStorage {

    /**
     * Sets the world this storage engine is used for.
     * 
     * @param world instance of world
     */
    default void setWorld(StorageWorld world) {
    }

    static StorageEngine load() {
        var providers = ServiceLoader.load(StorageEngine.class).stream().toList();
        if (providers.isEmpty()) {
            throw new StorageEngineException("No implementation of StorageEngine found. Make sure to include a storage engine implementation.");
        }

        if (providers.size() > 1) {
            throw new StorageEngineException("Multiple implementations of StorageEngine found (%d). Define storage engine using World.Builder or remove additional storage engines from classpath."
                    .formatted(providers.size()));
        }

        var provider = providers.get(0);
        return provider.get();
    }

    static StorageEngine load(Class<? extends StorageEngine> clazz) {
        var provider = ServiceLoader.load(StorageEngine.class).stream()
                .filter(p -> p.type() == clazz)
                .findFirst()
                .orElseThrow(() -> new StorageEngineException("Implementation '%s' of StorageEngine not found. Make sure the implementation is available via ServiceLoader."
                        .formatted(clazz.getName())));

        return provider.get();
    }

}
