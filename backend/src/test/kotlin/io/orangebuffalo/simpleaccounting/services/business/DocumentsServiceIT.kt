package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.WithMockFryUser
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.services.storage.DocumentsStorageStatus
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SimpleAccountingIntegrationTest
class DocumentsServiceIT(
    @Autowired val documentsService: DocumentsService
) {

    @Test
    @WithMockFryUser
    fun `should delegate to storage for status verification`(testData: DocumentsServiceTestData) {
        val storageStatus = runBlocking {
            documentsService.getCurrentUserStorageStatus()
        }
        assertThat(storageStatus.active).isTrue()
    }

    @TestConfiguration
    class DocumentsServiceConfig {
        @Bean
        fun mockStorage() = TestDocumentsStorage()
    }

    class TestDocumentsStorage(
        private val mock: DocumentsStorage = mock(DocumentsStorage::class.java)
    ) : DocumentsStorage by mock {
        override suspend fun getCurrentUserStorageStatus() = DocumentsStorageStatus(true)
        override fun getId() = "mock-storage"
    }
}

class DocumentsServiceTestData : TestData {
    override fun generateData() = listOf(
        Prototypes.platformUser(
            userName = "Fry",
            documentsStorage = "mock-storage"
        )
    )
}
