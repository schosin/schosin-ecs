package de.schosin.ecs.storage.testsuite;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Storage Engine Example Suite")
@SelectPackages("de.schosin.ecs.storage.testsuite")
public abstract class StorageEngineTestSuite {
}
