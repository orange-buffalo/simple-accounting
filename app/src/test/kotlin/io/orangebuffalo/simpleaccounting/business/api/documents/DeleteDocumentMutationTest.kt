package io.orangebuffalo.simpleaccounting.business.api.documents

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.GraphqlClientRequestExecutor
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQuery
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

private const val DELETE_DOCUMENT_MUTATION = "deleteDocument"

@DisplayName("deleteDocument mutation")
class DeleteDocumentMutationTest(
    @Autowired private val client: ApiTestClient,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val zoidberg = zoidberg()
            val fryWorkspace = workspace(owner = fry)
            val zoidbergWorkspace = workspace(owner = zoidberg)
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val deliveryReceipt = document(
                workspace = fryWorkspace,
                name = "Delivery to Mars receipt",
                storageLocation = "delivery-to-mars-receipt",
            )
            val zoidbergReceipt = document(
                workspace = zoidbergWorkspace,
                name = "Zoidberg receipt",
                storageLocation = "zoidberg-receipt",
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .deleteDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    documentId = preconditions.deliveryReceipt.id!!,
                )
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DELETE_DOCUMENT_MUTATION)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .deleteDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    documentId = preconditions.deliveryReceipt.id!!,
                )
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DELETE_DOCUMENT_MUTATION)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should delete unused document from database and storage`() {
            val testData = preconditions {
                object {
                    val document = document(
                        workspace = preconditions.fryWorkspace,
                        name = "Slurm supplies receipt",
                        storageLocation = "slurm-supplies-receipt",
                    )
                }
            }
            testDocumentsStorage.mockDocumentContent("slurm-supplies-receipt", "Slurm supplies".toByteArray())

            client
                .deleteDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    documentId = testData.document.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DELETE_DOCUMENT_MUTATION to JsonPrimitive(true)
                )

            aggregateTemplate.findById(testData.document.id!!, Document::class.java).shouldBe(null)
            testDocumentsStorage.hasUploadedContent("slurm-supplies-receipt").shouldBe(false)
        }

        @Test
        fun `should return DOCUMENT_IS_USED error when document is attached`() {
            val testData = preconditions {
                object {
                    val document = document(
                        workspace = preconditions.fryWorkspace,
                        name = "Planet Express equipment receipt",
                        storageLocation = "planet-express-equipment-receipt",
                    ).also { document ->
                        expense(
                            workspace = preconditions.fryWorkspace,
                            title = "Planet Express equipment",
                            attachments = setOf(document),
                        )
                    }
                }
            }
            testDocumentsStorage.mockDocumentContent(
                "planet-express-equipment-receipt",
                "Planet Express equipment".toByteArray(),
            )

            client
                .deleteDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    documentId = testData.document.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyBusinessError(
                    message = "Document ${testData.document.id} is used and cannot be deleted",
                    errorCode = "DOCUMENT_IS_USED",
                    path = DELETE_DOCUMENT_MUTATION,
                )

            aggregateTemplate.findSingle<Document>(testData.document.id!!).id.shouldBe(testData.document.id)
            testDocumentsStorage.hasUploadedContent("planet-express-equipment-receipt").shouldBe(true)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace belongs to another user`() {
            client
                .deleteDocumentMutation(
                    workspaceId = preconditions.zoidbergWorkspace.id!!,
                    documentId = preconditions.zoidbergReceipt.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DELETE_DOCUMENT_MUTATION)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when document belongs to another workspace`() {
            client
                .deleteDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    documentId = preconditions.zoidbergReceipt.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DELETE_DOCUMENT_MUTATION)
        }
    }

    private fun ApiTestClient.deleteDocumentMutation(
        workspaceId: String,
        documentId: String,
    ): GraphqlClientRequestExecutor = graphqlRawQuery(
        """
            mutation {
              deleteDocument(workspaceId: "$workspaceId", documentId: "$documentId")
            }
        """.trimIndent()
    )
}
