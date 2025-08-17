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
 *      <td>creationBatchSize</td>
 *      <td>
 *      Maximum batch size when creating entities in batches. <br/>
 *      If a larger amount of entities is created, they will be split in batches according to this size. <br/>
 *      <br/>
 *      Tweaking this value can potentially improve performance if very large number of entities are created regularly.
 *      </td>
 *  </tr>
 *  <tr valign="top">
 *      <td>processAttempts</td>
 *      <td>
 *      Number of process attempts when the storage is processed. <br/>
 *      If deletions of entities or addition/removal of components cause further changes, these will be performed
 *      in another step. If after {@code processAttempts} steps there are additional changes left, 
 *      an error will be thrown to avoid infinite loops. <br /> 
 *      <br/>
 *      If user code requires more attempts, this value can be increased to accommodate that need.<br />
 *      See {@link #PROPERTY_PROCESS_ATTEMPTS}.
 *      </td>
 *  </tr>
 *  <tr valign="top">
 *      <td>creationFlushAttempts</td>
 *      <td>
 *      Number of flushes of changes to an entity being created.
 *      Relates to {@code processAttempts}, but limited to whenever an entity is created and only the changes
 *      to that created entity. <br/>
 *      If after {@code creationFlushAttempts} steps a created entity still has additional changes left,
 *      an error will be thrown to avoid infinite loops. <br /> 
 *      <br/>
 *      If user code requires more attempts, this value can be increased to accommodate that need.<br />
 *      See {@link #PROPERTY_PROCESS_ATTEMPTS}.
 *      </td>
 *  </tr>
 * </table>
 * </p>
 */
public record ArchetypeStorageConfig(int classIdCount, int relationCount, int creationFlushAttempts, int processAttempts) {

    public static ArchetypeStorageConfig getConfig() {
        // System variable
        var classIdCountProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_CLASS_ID_COUNT);
        var relationCountProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_RELATION_COUNT);
        var creationFlushAttemptsProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_CREATION_FLUSH_ATTEMPTS);
        var processAttemptsProp = System.getProperty(ArchetypeStorageConfig.PROPERTY_PROCESS_ATTEMPTS);

        if (classIdCountProp != null || relationCountProp != null || creationFlushAttemptsProp != null || processAttemptsProp != null) {
            var classIdCount = classIdCountProp != null ? Integer.parseInt(classIdCountProp) : ArchetypeStorageConfig.DEFAULT_CLASS_ID_COUNT;
            var relationCount = relationCountProp != null ? Integer.parseInt(relationCountProp) : ArchetypeStorageConfig.DEFAULT_RELATION_COUNT;
            var creationFlushAttempts = creationFlushAttemptsProp != null ? Integer.parseInt(creationFlushAttemptsProp) : ArchetypeStorageConfig.DEFAULT_CREATION_FLUSH_ATTEMPTS;
            var processAttempts = processAttemptsProp != null ? Integer.parseInt(processAttemptsProp) : ArchetypeStorageConfig.DEFAULT_PROCESS_ATTEMPTS;

            return new ArchetypeStorageConfig(classIdCount, relationCount, creationFlushAttempts, processAttempts);
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
        if (creationFlushAttempts < 0) {
            throw new StorageEngineException("creationFlushAttempts must be positive or zero, but was: " + creationFlushAttempts);
        }
        if (processAttempts < 1) {
            throw new StorageEngineException("processAttempts must be positive, but was: " + processAttempts);
        }
    }

    public static final String PROPERTY_CLASS_ID_COUNT = "storage.archetype.classIdCount";
    public static final String PROPERTY_RELATION_COUNT = "storage.archetype.relationCount";
    public static final String PROPERTY_CREATION_FLUSH_ATTEMPTS = "storage.archetype.creationFlushAttempts";
    public static final String PROPERTY_PROCESS_ATTEMPTS = "storage.archetype.processAttempts";

    public static final int DEFAULT_CLASS_ID_COUNT = 50;
    public static final int DEFAULT_RELATION_COUNT = 10;
    public static final int DEFAULT_CREATION_FLUSH_ATTEMPTS = 5;
    public static final int DEFAULT_PROCESS_ATTEMPTS = 5;

    public static final ArchetypeStorageConfig DEFAULT = new ArchetypeStorageConfig(
            DEFAULT_CLASS_ID_COUNT, DEFAULT_RELATION_COUNT,
            DEFAULT_CREATION_FLUSH_ATTEMPTS, DEFAULT_PROCESS_ATTEMPTS);

}
