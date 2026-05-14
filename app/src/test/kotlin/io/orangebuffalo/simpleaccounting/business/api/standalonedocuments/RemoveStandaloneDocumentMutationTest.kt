package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
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

private const val REMOVE_STANDALONE_DOCUMENT_MUTATION = "removeStandaloneDocument"

@DisplayName("removeStandaloneDocument mutation")
class RemoveStandaloneDocumentMutationTest(
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
            val standaloneDocument = standaloneDocument(
                workspace = fryWorkspace,
                document = deliveryReceipt,
                title = "Delivery to Mars receipt",
            )
            val zoidbergReceipt = document(
                workspace = zoidbergWorkspace,
                name = "Zoidberg receipt",
                storageLocation = "zoidberg-receipt",
            )
            val zoidbergStandaloneDocument = standaloneDocument(
                workspace = zoidbergWorkspace,
                document = zoidbergReceipt,
                title = "Claw polish receipt",
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = preconditions.standaloneDocument.id!!,
                )
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = REMOVE_STANDALONE_DOCUMENT_MUTATION)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = preconditions.standaloneDocument.id!!,
                )
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = REMOVE_STANDALONE_DOCUMENT_MUTATION)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = preconditions.standaloneDocument.id!!,
                )
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = REMOVE_STANDALONE_DOCUMENT_MUTATION)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should remove standalone document and linked document by default`() {
            val testData = preconditions {
                object {
                    val document = document(
                        workspace = preconditions.fryWorkspace,
                        name = "Slurm supplies receipt",
                        storageLocation = "slurm-supplies-receipt",
                    )
                    val standaloneDocument = standaloneDocument(
                        workspace = preconditions.fryWorkspace,
                        document = document,
                        title = "Slurm supplies receipt",
                    )
                }
            }
            testDocumentsStorage.mockDocumentContent("slurm-supplies-receipt", "Slurm supplies".toByteArray())

            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = testData.standaloneDocument.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    REMOVE_STANDALONE_DOCUMENT_MUTATION to JsonPrimitive(true)
                )

            aggregateTemplate.findById(testData.standaloneDocument.id!!, StandaloneDocument::class.java).shouldBe(null)
            aggregateTemplate.findById(testData.document.id!!, Document::class.java).shouldBe(null)
            testDocumentsStorage.hasUploadedContent("slurm-supplies-receipt").shouldBe(false)
        }

        @Test
        fun `should keep linked document when remove document if unused is false`() {
            val testData = preconditions {
                object {
                    val document = document(
                        workspace = preconditions.fryWorkspace,
                        name = "Robot oil receipt",
                        storageLocation = "robot-oil-receipt",
                    )
                    val standaloneDocument = standaloneDocument(
                        workspace = preconditions.fryWorkspace,
                        document = document,
                        title = "Robot oil receipt",
                    )
                }
            }
            testDocumentsStorage.mockDocumentContent("robot-oil-receipt", "Robot oil".toByteArray())

            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = testData.standaloneDocument.id!!,
                    removeDocumentIfUnused = false,
                )
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    REMOVE_STANDALONE_DOCUMENT_MUTATION to JsonPrimitive(true)
                )

            aggregateTemplate.findById(testData.standaloneDocument.id!!, StandaloneDocument::class.java).shouldBe(null)
            aggregateTemplate.findSingle<Document>(testData.document.id!!).id.shouldBe(testData.document.id)
            testDocumentsStorage.hasUploadedContent("robot-oil-receipt").shouldBe(true)
        }

        @Test
        fun `should keep linked document when it is still used`() {
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
                    val standaloneDocument = standaloneDocument(
                        workspace = preconditions.fryWorkspace,
                        document = document,
                        title = "Planet Express equipment receipt",
                    )
                }
            }
            testDocumentsStorage.mockDocumentContent(
                "planet-express-equipment-receipt",
                "Planet Express equipment".toByteArray(),
            )

            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = testData.standaloneDocument.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    REMOVE_STANDALONE_DOCUMENT_MUTATION to JsonPrimitive(true)
                )

            aggregateTemplate.findById(testData.standaloneDocument.id!!, StandaloneDocument::class.java).shouldBe(null)
            aggregateTemplate.findSingle<Document>(testData.document.id!!).id.shouldBe(testData.document.id)
            testDocumentsStorage.hasUploadedContent("planet-express-equipment-receipt").shouldBe(true)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error for non-existent standalone document`() {
            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.fryWorkspace.id!!,
                    standaloneDocumentId = "missing-id",
                )
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = REMOVE_STANDALONE_DOCUMENT_MUTATION)
        }

        @Test
        fun `should return ENTITY_NOT_FOUND error when workspace belongs to another user`() {
            client
                .removeStandaloneDocumentMutation(
                    workspaceId = preconditions.zoidbergWorkspace.id!!,
                    standaloneDocumentId = preconditions.zoidbergStandaloneDocument.id!!,
                )
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = REMOVE_STANDALONE_DOCUMENT_MUTATION)
        }
    }

    private fun ApiTestClient.removeStandaloneDocumentMutation(
        workspaceId: String,
        standaloneDocumentId: String,
        removeDocumentIfUnused: Boolean? = null,
    ): GraphqlClientRequestExecutor {
        val removeDocumentIfUnusedArg = removeDocumentIfUnused
            ?.let { ", removeDocumentIfUnused: $it" }
            .orEmpty()
        return graphqlRawQuery(
            """
                mutation {
                  removeStandaloneDocument(
                    workspaceId: "$workspaceId",
                    standaloneDocumentId: "$standaloneDocumentId"$removeDocumentIfUnusedArg
                  )
                }
            """.trimIndent()
        )
    }
}
