package io.orangebuffalo.simpleaccounting.domain.documents

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.stub
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.Preconditions
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsInfra
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.utils.consumeToString
import io.orangebuffalo.simpleaccounting.infra.utils.toDataBuffers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SimpleAccountingIntegrationTest
class DocumentsServiceIT(
    @Autowired private val documentsService: DocumentsService,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
    @Autowired private val preconditionsInfra: PreconditionsInfra,
) {

    @Test
    @WithMockFryUser
    fun `should delegate to storage for status verification`() {
        setupPreconditions()

        val storageStatus = runBlocking {
            documentsService.getCurrentUserStorageStatus()
        }
        assertThat(storageStatus.active).isTrue()
    }

    @Test
    @WithMockFryUser
    fun `should return content for download`() {
        val testData = setupPreconditions()
        testDocumentsStorage.mock.stub {
            onBlocking {
                getDocumentContent(testData.fryWorkspace, testData.document.storageLocation!!)
            } doReturn "test-content".toDataBuffers()
        }

        val contentResponse = runBlocking {
            documentsService.getContent(DocumentDownloadMetadata(testData.document.id!!))
        }

        assertThat(contentResponse.fileName).isEqualTo("document.pdf")
        assertThat(contentResponse.sizeInBytes).isEqualTo(42)
        assertThat(contentResponse.content.consumeToString()).isEqualTo("test-content")
        // todo #108: verify content type
    }

    @TestConfiguration
    class DocumentsServiceConfig {
        @Bean
        fun mockStorage() = TestDocumentsStorage()
    }

    class TestDocumentsStorage(
        val mock: DocumentsStorage = mock(DocumentsStorage::class.java)
    ) : DocumentsStorage by mock {
        override suspend fun getCurrentUserStorageStatus() = DocumentsStorageStatus(true)
        override fun getId() = "mock-storage"
    }

    private fun setupPreconditions() = object : Preconditions(preconditionsInfra) {
        val fry = platformUser(
            userName = "Fry",
            documentsStorage = "mock-storage"
        )
        val fryWorkspace = workspace(owner = fry)
        val document = document(
            name = "document.pdf",
            storageLocation = "document-in-storage",
            workspace = fryWorkspace,
            storageId = "mock-storage",
            timeUploaded = MOCK_TIME,
            sizeInBytes = 42
        )
    }
}


