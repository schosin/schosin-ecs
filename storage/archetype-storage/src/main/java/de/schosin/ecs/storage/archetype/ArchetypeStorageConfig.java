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
 * </table>
 * </p>
 */
public record ArchetypeStorageConfig(int classIdCount, int relationCount, int creationBatchSize) {

    public static ArchetypeStorageConfig getConfig() {
        // System variable
        var classIdCountProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT);
        var relationCountProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT);
        var creationBatchSizeProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_CREATION_BATCH_SIZE);

        if (classIdCountProp != null || relationCountProp != null || creationBatchSizeProp != null) {
            var classIdCount = classIdCountProp != null ? Integer.parseInt(classIdCountProp) : ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT;
            var relationCount = relationCountProp != null ? Integer.parseInt(relationCountProp) : ArchetypeStorageConfig.DEFAULT_RELATION_COUNT;
            var creationBatchSize = creationBatchSizeProp != null ? Integer.parseInt(creationBatchSizeProp) : ArchetypeStorageConfig.DEFAULT_CREATION_BATCH_SIZE;

            return new ArchetypeStorageConfig(classIdCount, relationCount, creationBatchSize);
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
    }

    public static final String PROPERTY_CLASS_ID_COUNT = "storage.archetype.classIdCount";
    public static final String PROPERTY_RELATION_COUNT = "storage.archetype.relationCount";
    public static final String PROPERTY_CREATION_BATCH_SIZE = "storage.archetype.creationBatchSize";

    public static final int DEFAULT_CLASS_ID_COUNT = 50;
    public static final int DEFAULT_RELATION_COUNT = 10;
    public static final int DEFAULT_CREATION_BATCH_SIZE = 100;

    public static final ArchetypeStorageConfig DEFAULT = new ArchetypeStorageConfig(DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT, DEFAULT_CREATION_BATCH_SIZE);

}
