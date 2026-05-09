package io.orangebuffalo.simpleaccounting.business.common.pesistence

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AbstractEntityIdCallbackTest(
    @Autowired private val documentsRepository: DocumentsRepository,
) : SaIntegrationTestBase() {

    @Test
    fun `should assign random string id before inserting abstract entity`() {
        val preconditions = preconditions {
            object {
                val workspace = workspace(owner = fry())
            }
        }

        val document = documentsRepository.save(
            Document(
                name = "Slurm receipt",
                workspaceId = preconditions.workspace.id!!,
                timeUploaded = MOCK_TIME,
                storageId = "planet-express-storage",
                storageLocation = "delivery-to-mars",
                sizeInBytes = 42,
                mimeType = "application/pdf",
            )
        )

        val documentId = document.id!!
        documentId.shouldMatch(Regex("[0-9A-Za-z]{10}"))
        documentsRepository.findById(documentId).get().id.shouldBe(documentId)
    }
}
