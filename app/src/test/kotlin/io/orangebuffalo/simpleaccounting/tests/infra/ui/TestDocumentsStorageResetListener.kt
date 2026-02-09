package io.orangebuffalo.simpleaccounting.tests.infra.ui

import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

/**
 * Test execution listener that automatically resets the [TestDocumentsStorage] before each test.
 * This ensures that tests start with a clean storage state without needing to manually call reset().
 */
class TestDocumentsStorageResetListener : TestExecutionListener {
    override fun beforeTestMethod(testContext: TestContext) {
        testContext.applicationContext
            .getBean(TestDocumentsStorage::class.java)
            .reset()
    }
}
