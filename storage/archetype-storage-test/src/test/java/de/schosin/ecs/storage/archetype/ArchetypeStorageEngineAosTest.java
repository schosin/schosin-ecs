package de.schosin.ecs.storage.archetype;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SuiteDisplayName;

import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.Variant;
import de.schosin.ecs.storage.testsuite.StorageEngineTestSuite;

@SuiteDisplayName("ArchetypeStorage (Array of Structs)")
class ArchetypeStorageEngineAosTest extends StorageEngineTestSuite {

    @BeforeSuite
    static void initializeSuite() {
        System.setProperty(ArchetypeStorageConfig.PROPERTY_VARIANT, Variant.ArrayOfStructs.name());
    }

    @AfterSuite
    static void cleanUp() {
        System.clearProperty(ArchetypeStorageConfig.PROPERTY_VARIANT);
    }

}
