package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("editStandaloneDocument mutation")
class EditStandaloneDocumentMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val fryDocument = document(workspace = fryWorkspace, name = "Slurm receipt")
            val replacementDocument = document(workspace = fryWorkspace, name = "Robot oil receipt")
            val standaloneDocument = standaloneDocument(
                workspace = fryWorkspace,
                document = fryDocument,
                title = "Old delivery receipt",
            )
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidberg = zoidberg()
            val zoidbergWorkspace = workspace(owner = zoidberg)
            val zoidbergDocument = document(workspace = zoidbergWorkspace, name = "Claw polish receipt")
            val zoidbergStandaloneDocument = standaloneDocument(
                workspace = zoidbergWorkspace,
                document = zoidbergDocument,
                title = "Zoidberg receipt",
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { editStandaloneDocumentMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditStandaloneDocument)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { editStandaloneDocumentMutation() }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditStandaloneDocument)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { editStandaloneDocumentMutation() }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditStandaloneDocument)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value -> editStandaloneDocumentMutation(title = value) },
            sizeConstraintTestCases("title", maxLength = 255) { value -> editStandaloneDocumentMutation(title = value) },
            requiredFieldRejectedTestCases("documentId") { editStandaloneDocumentMutation() },
            requiredFieldRejectedTestCases("id") { editStandaloneDocumentMutation() },
            requiredFieldRejectedTestCases("title") { editStandaloneDocumentMutation() },
            requiredFieldRejectedTestCases("workspaceId") { editStandaloneDocumentMutation() },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditStandaloneDocument)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should update standalone document`() {
            client
                .graphqlMutation {
                    editStandaloneDocumentMutation(
                        title = "Updated robot oil receipt",
                        documentId = preconditions.replacementDocument.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditStandaloneDocument to buildJsonObject {
                        put("id", preconditions.standaloneDocument.id!!)
                        put("title", "Updated robot oil receipt")
                        put("documentId", preconditions.replacementDocument.id!!)
                    }
                )

            aggregateTemplate.findSingle<StandaloneDocument>(preconditions.standaloneDocument.id!!)
                .shouldBeEntityWithFields(
                    StandaloneDocument(
                        title = "Updated robot oil receipt",
                        documentId = preconditions.replacementDocument.id!!,
                    )
                )
        }

        @Test
        fun `should return entity not found error for non-existent standalone document`() {
            client
                .graphqlMutation { editStandaloneDocumentMutation(id = "missing-id") }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditStandaloneDocument)
        }

        @Test
        fun `should return entity not found error for standalone document in another user workspace`() {
            client
                .graphqlMutation {
                    editStandaloneDocumentMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        id = preconditions.zoidbergStandaloneDocument.id!!,
                        documentId = preconditions.zoidbergDocument.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditStandaloneDocument)
        }

        @Test
        fun `should return entity not found error for document in another workspace`() {
            client
                .graphqlMutation { editStandaloneDocumentMutation(documentId = preconditions.zoidbergDocument.id!!) }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditStandaloneDocument)
        }
    }

    private fun MutationProjection.editStandaloneDocumentMutation(
        workspaceId: String = preconditions.fryWorkspace.id!!,
        id: String = preconditions.standaloneDocument.id!!,
        title: String = "Delivery receipt",
        documentId: String = preconditions.fryDocument.id!!,
    ): MutationProjection = editStandaloneDocument(
        workspaceId = workspaceId,
        id = id,
        title = title,
        documentId = documentId,
    ) {
        this.id
        this.title
        this.documentId
    }
}
