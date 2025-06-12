package de.schosin.ecs.storage.archetype;

import de.schosin.ecs.storage.api.StorageEngineException;

/**
 * Configuration object for archetype storage.
 * 
 * <p>
 * <table border="1">
 *  <tr valign="top">
 *      <td>classIdCount</td>
 *      <td>
 *      Id space to reserve for regular components. <br/>
 *      <br/>
 *      Match to used components for slightly better performance
 *      </td>
 *  </tr>
 *  <tr valign="top">
 *      <td>relationCount</td>
 *      <td>
 *      Id space to reserve for every relationship component for component relations. <br/>
 *      <br/>
 *      Match to the largest number of target component types any relationship component can have. <br/>
 *      For example, if a relationship {@code Important} (e.g. an enum) will be used with the target
 *      components {@code Position}, {@code Health} and {@code Score}, set this parameter to 3 if no other
 *      relationship component is assigned to more than 3 target components.
 *      </td>
 *  </tr>
 *  <tr valign="top">
 *      <td>variant</td>
 *      <td>
 *      Actual memory layout of components. <br/>
 *      <br/>
 *      See {@link #DEFAULT_VARIANT} for suggestion on which to start with
 *      </td>
 *  </tr>
 * </table>
 * </p>
 */
public record ArchetypeStorageConfig(int classIdCount, int relationCount, Variant variant) {

    public ArchetypeStorageConfig {
        if (classIdCount < 1) {
            throw new StorageEngineException("classIdCount must be positive, but was: " + classIdCount);
        }
        if (relationCount < 1) {
            throw new StorageEngineException("relationCount must be positive, but was: " + relationCount);
        }
        if (variant == null) {
            throw new StorageEngineException("variant must not be null");
        }
    }

    public enum Variant {
        ArrayOfStructs, StructOfArrays
    }

    public static final String PROPERTY_CLASS_ID_COUNT = "storage.archetype.classIdCount";
    public static final String PROPERTY_RELATION_COUNT = "storage.archetype.relationCount";
    public static final String PROPERTY_VARIANT = "storage.archetype.variant";
    
    public static final int DEFAULT_CLASS_ID_COUNT = 50;
    public static final int DEFAULT_RELATION_COUNT = 10;
    public static final Variant DEFAULT_VARIANT = Variant.StructOfArrays;

    public static final ArchetypeStorageConfig STRUCT_OF_ARRAYS = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, Variant.StructOfArrays);
    public static final ArchetypeStorageConfig ARRAY_OF_STRUCTS = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, Variant.ArrayOfStructs);

    public static final ArchetypeStorageConfig DEFAULT = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, DEFAULT_VARIANT);

}
