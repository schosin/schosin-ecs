package de.schosin.ecs.storage.archetype;

public record ArchetypeStorageConfig(int classIdCount, int relationCount) {

    public static final ArchetypeStorageConfig DEFAULT = new ArchetypeStorageConfig(50, 10);

}
