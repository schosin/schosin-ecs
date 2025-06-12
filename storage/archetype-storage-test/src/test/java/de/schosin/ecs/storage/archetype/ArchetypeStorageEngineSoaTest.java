package de.schosin.ecs.storage.archetype;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SuiteDisplayName;

import de.schosin.ecs.storage.archetype.ArchetypeStorageConfig.Variant;
import de.schosin.ecs.storage.testsuite.StorageEngineTestSuite;

@SuiteDisplayName("ArchetypeStorage (Struct of Arrays)")
class ArchetypeStorageEngineSoaTest extends StorageEngineTestSuite {

    @BeforeSuite
    static void initializeSuite() {
        System.setProperty(ArchetypeStorageConfig.PROPERTY_VARIANT, Variant.StructOfArrays.name());
    }

    @AfterSuite
    static void cleanUp() {
        System.clearProperty(ArchetypeStorageConfig.PROPERTY_VARIANT);
    }

}
