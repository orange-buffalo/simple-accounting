package io.orangebuffalo.simpleaccounting.business.api.standalonedocuments

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("createStandaloneDocument mutation")
class CreateStandaloneDocumentMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val fryDocument = document(workspace = fryWorkspace, name = "Slurm receipt")
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidberg = zoidberg()
            val zoidbergWorkspace = workspace(owner = zoidberg)
            val zoidbergDocument = document(workspace = zoidbergWorkspace, name = "Claw polish receipt")
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {

        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { createStandaloneDocumentMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateStandaloneDocument)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { createStandaloneDocumentMutation() }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateStandaloneDocument)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { createStandaloneDocumentMutation() }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateStandaloneDocument)
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("title") { value -> createStandaloneDocumentMutation(title = value) },
            sizeConstraintTestCases("title", maxLength = 255) { value -> createStandaloneDocumentMutation(title = value) },
            requiredFieldRejectedTestCases("documentId") { createStandaloneDocumentMutation() },
            requiredFieldRejectedTestCases("title") { createStandaloneDocumentMutation() },
            requiredFieldRejectedTestCases("workspaceId") { createStandaloneDocumentMutation() },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateStandaloneDocument)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {

        @Test
        fun `should create a new standalone document`() {
            client
                .graphqlMutation {
                    createStandaloneDocumentMutation(title = "Slurm supplies receipt")
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateStandaloneDocument to buildJsonObject {
                        put("title", "Slurm supplies receipt")
                        put("documentId", preconditions.fryDocument.id!!)
                    }
                )

            aggregateTemplate.findAll<StandaloneDocument>()
                .filter { it.title == "Slurm supplies receipt" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    StandaloneDocument(
                        title = "Slurm supplies receipt",
                        documentId = preconditions.fryDocument.id!!,
                    )
                )
        }

        @Test
        fun `should return entity not found error for document in another workspace`() {
            client
                .graphqlMutation {
                    createStandaloneDocumentMutation(documentId = preconditions.zoidbergDocument.id!!)
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateStandaloneDocument)
        }

        @Test
        fun `should return entity not found error for another user workspace`() {
            client
                .graphqlMutation {
                    createStandaloneDocumentMutation(
                        workspaceId = preconditions.zoidbergWorkspace.id!!,
                        documentId = preconditions.zoidbergDocument.id!!,
                    )
                }
                .from(preconditions.fry)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateStandaloneDocument)
        }
    }

    private fun MutationProjection.createStandaloneDocumentMutation(
        workspaceId: String = preconditions.fryWorkspace.id!!,
        title: String = "Delivery receipt",
        documentId: String = preconditions.fryDocument.id!!,
    ): MutationProjection = createStandaloneDocument(
        workspaceId = workspaceId,
        title = title,
        documentId = documentId,
    ) {
        this.title
        this.documentId
    }
}
