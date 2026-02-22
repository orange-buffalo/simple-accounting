package io.orangebuffalo.simpleaccounting.business.documents

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorage
import io.orangebuffalo.simpleaccounting.business.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.consumeToString
import io.orangebuffalo.simpleaccounting.tests.infra.utils.toDataBuffers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

class DocumentsServiceTest(
    @Autowired private val documentsService: DocumentsService,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
) : SaIntegrationTestBase() {

    @Test
    @WithMockFryUser
    fun `should delegate to storage for status verification`() {
        // trigger preconditions to be prepared - should be removed when JWT token client is used
        preconditions.fry

        val storageStatus = runBlocking {
            documentsService.getCurrentUserStorageStatus()
        }
        assertThat(storageStatus.active).isTrue()
    }

    @Test
    @WithMockFryUser
    fun `should return content for download`() {
        testDocumentsStorage.mock.stub {
            onBlocking {
                getDocumentContent(preconditions.fryWorkspace, preconditions.document.storageLocation!!)
            } doReturn "test-content".toDataBuffers()
        }

        val contentResponse = runBlocking {
            documentsService.getContent(DocumentDownloadMetadata(preconditions.document.id!!))
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

    private val preconditions by lazyPreconditions {
        object {
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
}
