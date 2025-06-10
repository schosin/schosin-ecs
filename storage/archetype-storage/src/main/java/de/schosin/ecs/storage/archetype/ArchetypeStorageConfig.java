package de.schosin.ecs.storage.archetype;

public record ArchetypeStorageConfig(int classIdCount, int relationCount, Variant variant) {

    public enum Variant {
        ArrayOfStructs, StructOfArrays
    }

    public static final ArchetypeStorageConfig DEFAULT = new ArchetypeStorageConfig(50, 10, Variant.StructOfArrays);
    public static final ArchetypeStorageConfig ARRAY_OF_STRUCTS = new ArchetypeStorageConfig(50, 10, Variant.ArrayOfStructs);

}
