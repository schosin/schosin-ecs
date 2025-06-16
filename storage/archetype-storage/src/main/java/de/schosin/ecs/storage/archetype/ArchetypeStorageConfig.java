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
public record ArchetypeStorageConfig(int classIdCount, int relationCount, int creationBatchSize, Variant variant) {
    
    public static ArchetypeStorageConfig getConfig() {
        // System variable
        var classIdCountProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT);
        var relationCountProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT);
        var creationBatchSizeProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_CREATION_BATCH_SIZE);
        var variantProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_VARIANT);

        if (classIdCountProp != null || relationCountProp != null || creationBatchSizeProp != null || variantProp != null) {
            var classIdCount = classIdCountProp != null ? Integer.parseInt(classIdCountProp) : ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT;
            var relationCount = relationCountProp != null ? Integer.parseInt(relationCountProp) : ArchetypeStorageConfig.DEFAULT_RELATION_COUNT;
            var creationBatchSize = creationBatchSizeProp != null ? Integer.parseInt(creationBatchSizeProp) : ArchetypeStorageConfig.DEFAULT_CREATION_BATCH_SIZE;
            var variant = variantProp != null ? Variant.valueOf(variantProp) : ArchetypeStorageConfig.DEFAULT_VARIANT;

            return new ArchetypeStorageConfig(classIdCount, relationCount, creationBatchSize, variant);
        }

        // Default config
        return ArchetypeStorageConfig.DEFAULT;
    }

    public ArchetypeStorageConfig {
        if (classIdCount < 1) {
            throw new StorageEngineException("classIdCount must be positive, but was: " + classIdCount);
        }
        if (relationCount < 1) {
            throw new StorageEngineException("relationCount must be positive, but was: " + relationCount);
        }
        if (creationBatchSize < 1) {
            throw new StorageEngineException("creationBatchSize must be positive, but was: " + creationBatchSize);
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
    public static final String PROPERTY_CREATION_BATCH_SIZE = "storage.archetype.creationBatchSize";
    public static final String PROPERTY_VARIANT = "storage.archetype.variant";

    public static final int DEFAULT_CLASS_ID_COUNT = 50;
    public static final int DEFAULT_RELATION_COUNT = 10;
    public static final int DEFAULT_CREATION_BATCH_SIZE = 100;
    public static final Variant DEFAULT_VARIANT = Variant.StructOfArrays;

    public static final ArchetypeStorageConfig STRUCT_OF_ARRAYS = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, DEFAULT_CREATION_BATCH_SIZE, Variant.StructOfArrays);
    public static final ArchetypeStorageConfig ARRAY_OF_STRUCTS = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, DEFAULT_CREATION_BATCH_SIZE, Variant.ArrayOfStructs);

    public static final ArchetypeStorageConfig DEFAULT = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, DEFAULT_CREATION_BATCH_SIZE, DEFAULT_VARIANT);

}
