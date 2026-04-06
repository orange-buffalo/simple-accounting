package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.workspaces.SavedWorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

class WorkspacesMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val farnsworth = farnsworth()
            val zoidberg = zoidberg()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val expiredWorkspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.minusMillis(1),
                token = "expiredToken",
            )
        }
    }

    @Nested
    @DisplayName("createWorkspace mutation")
    inner class CreateWorkspaceMutation {

        @Nested
        @DisplayName("Authorization")
        inner class Authorization {

            @Test
            fun `should return NOT_AUTHORIZED error for anonymous requests`() {
                client
                    .graphqlMutation { createWorkspaceMutation() }
                    .fromAnonymous()
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspace)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for admin user`() {
                client
                    .graphqlMutation { createWorkspaceMutation() }
                    .from(preconditions.farnsworth)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspace)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for workspace access token`() {
                client
                    .graphqlMutation { createWorkspaceMutation() }
                    .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateWorkspace)
            }
        }

        @Nested
        @DisplayName("Inputs Validation")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class InputsValidation {
            fun testCases() = listOf(
                mustNotBeBlankTestCases("name") { value ->
                    createWorkspaceMutation(name = value)
                },
                sizeConstraintTestCases("name", maxLength = 255) { value ->
                    createWorkspaceMutation(name = value)
                },
                mustNotBeBlankTestCases("defaultCurrency") { value ->
                    createWorkspaceMutation(defaultCurrency = value)
                },
                sizeConstraintTestCases("defaultCurrency", maxLength = 3) { value ->
                    createWorkspaceMutation(defaultCurrency = value)
                },
            ).flatten()

            @ParameterizedTest(name = "{0}")
            @MethodSource("testCases")
            fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
                client
                    .buildInputValidationRequest(testCase)
                    .from(preconditions.fry)
                    .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateWorkspace)
            }
        }

        @Nested
        @DisplayName("Business Flow")
        inner class BusinessFlow {

            @Test
            fun `should create a new workspace`() {
                client
                    .graphqlMutation {
                        createWorkspaceMutation(
                            name = "Robot Arms Apts",
                            defaultCurrency = "EUR",
                        )
                    }
                    .from(preconditions.fry)
                    .executeAndVerifySuccessResponse(
                        DgsConstants.MUTATION.CreateWorkspace to buildJsonObject {
                            put("id", JsonValues.ANY_NUMBER)
                            put("name", "Robot Arms Apts")
                            put("defaultCurrency", "EUR")
                        }
                    )

                aggregateTemplate.findAll<Workspace>()
                    .filter { it.ownerId == preconditions.fry.id && it.name == "Robot Arms Apts" }
                    .shouldBeSingle()
                    .shouldBeEntityWithFields(
                        Workspace(
                            name = "Robot Arms Apts",
                            defaultCurrency = "EUR",
                            ownerId = preconditions.fry.id!!,
                        )
                    )
            }
        }
    }

    @Nested
    @DisplayName("editWorkspace mutation")
    inner class EditWorkspaceMutation {

        @Nested
        @DisplayName("Authorization")
        inner class Authorization {

            @Test
            fun `should return NOT_AUTHORIZED error for anonymous requests`() {
                client
                    .graphqlMutation {
                        editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
                    }
                    .fromAnonymous()
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditWorkspace)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for admin user`() {
                client
                    .graphqlMutation {
                        editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
                    }
                    .from(preconditions.farnsworth)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditWorkspace)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for workspace access token`() {
                client
                    .graphqlMutation {
                        editWorkspaceMutation(id = preconditions.fryWorkspace.id!!)
                    }
                    .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditWorkspace)
            }
        }

        @Nested
        @DisplayName("Inputs Validation")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class InputsValidation {
            fun testCases() = listOf(
                mustNotBeBlankTestCases("name") { value ->
                    editWorkspaceMutation(id = preconditions.fryWorkspace.id!!, name = value)
                },
                sizeConstraintTestCases("name", maxLength = 255) { value ->
                    editWorkspaceMutation(id = preconditions.fryWorkspace.id!!, name = value)
                },
            ).flatten()

            @ParameterizedTest(name = "{0}")
            @MethodSource("testCases")
            fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
                client
                    .buildInputValidationRequest(testCase)
                    .from(preconditions.fry)
                    .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditWorkspace)
            }
        }

        @Nested
        @DisplayName("Business Flow")
        inner class BusinessFlow {

            @Test
            fun `should update the workspace name`() {
                client
                    .graphqlMutation {
                        editWorkspaceMutation(
                            id = preconditions.fryWorkspace.id!!,
                            name = "New New York Express",
                        )
                    }
                    .from(preconditions.fry)
                    .executeAndVerifySuccessResponse(
                        DgsConstants.MUTATION.EditWorkspace to buildJsonObject {
                            put("id", preconditions.fryWorkspace.id!!.toInt())
                            put("name", "New New York Express")
                            put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                        }
                    )

                aggregateTemplate.findSingle<Workspace>(preconditions.fryWorkspace.id!!)
                    .shouldBeEntityWithFields(
                        Workspace(
                            name = "New New York Express",
                            defaultCurrency = preconditions.fryWorkspace.defaultCurrency,
                            ownerId = preconditions.fry.id!!,
                        )
                    )
            }

            @Test
            fun `should return entity not found error for workspace of another user`() {
                val zoidbergWorkspace = preconditions { workspace(owner = preconditions.zoidberg) }

                client
                    .graphqlMutation {
                        editWorkspaceMutation(id = zoidbergWorkspace.id!!)
                    }
                    .from(preconditions.fry)
                    .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditWorkspace)
            }

            @Test
            fun `should return entity not found error for non-existent workspace`() {
                client
                    .graphqlMutation {
                        editWorkspaceMutation(id = Long.MAX_VALUE)
                    }
                    .from(preconditions.fry)
                    .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditWorkspace)
            }
        }
    }

    private fun MutationProjection.createWorkspaceMutation(
        name: String = "Planet Express",
        defaultCurrency: String = "USD",
    ): MutationProjection = createWorkspace(
        name = name,
        defaultCurrency = defaultCurrency,
    ) {
        this.id
        this.name
        this.defaultCurrency
    }

    private fun MutationProjection.editWorkspaceMutation(
        id: Long,
        name: String = "Planet Express",
    ): MutationProjection = editWorkspace(
        id = id,
        name = name,
    ) {
        this.id
        this.name
        this.defaultCurrency
    }

    @Nested
    @DisplayName("saveSharedWorkspace mutation")
    inner class SaveSharedWorkspaceMutation {

        @Nested
        @DisplayName("Authorization")
        inner class Authorization {

            @Test
            fun `should return NOT_AUTHORIZED error for anonymous requests`() {
                client
                    .graphqlMutation { saveSharedWorkspaceMutation() }
                    .fromAnonymous()
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.SaveSharedWorkspace)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for admin user`() {
                client
                    .graphqlMutation { saveSharedWorkspaceMutation() }
                    .from(preconditions.farnsworth)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.SaveSharedWorkspace)
            }

            @Test
            fun `should return NOT_AUTHORIZED error for workspace access token`() {
                client
                    .graphqlMutation { saveSharedWorkspaceMutation() }
                    .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                    .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.SaveSharedWorkspace)
            }
        }

        @Nested
        @DisplayName("Inputs Validation")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class InputsValidation {
            fun testCases() = mustNotBeBlankTestCases("token", boundarySetup = ::setupBoundaryData) { value ->
                saveSharedWorkspaceMutation(token = value)
            }

            private fun setupBoundaryData() {
                preconditions {
                    workspaceAccessToken(
                        workspace = preconditions.fryWorkspace,
                        token = "a",
                        validTill = MOCK_TIME.plusSeconds(10000),
                    )
                }
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("testCases")
            fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
                client
                    .buildInputValidationRequest(testCase)
                    .from(preconditions.fry)
                    .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.SaveSharedWorkspace)
            }
        }

        @Nested
        @DisplayName("Business Flow")
        inner class BusinessFlow {

            @Test
            fun `should save shared workspace and return its details`() {
                client
                    .graphqlMutation {
                        saveSharedWorkspaceMutation(token = preconditions.workspaceAccessToken.token)
                    }
                    .from(preconditions.zoidberg)
                    .executeAndVerifySuccessResponse(
                        DgsConstants.MUTATION.SaveSharedWorkspace to buildJsonObject {
                            put("id", preconditions.fryWorkspace.id!!.toInt())
                            put("name", preconditions.fryWorkspace.name)
                            put("defaultCurrency", preconditions.fryWorkspace.defaultCurrency)
                        }
                    )

                aggregateTemplate.findAll<SavedWorkspaceAccessToken>()
                    .filter { it.ownerId == preconditions.zoidberg.id }
                    .shouldBeSingle()
                    .shouldBeEntityWithFields(
                        SavedWorkspaceAccessToken(
                            ownerId = preconditions.zoidberg.id!!,
                            workspaceAccessTokenId = preconditions.workspaceAccessToken.id!!,
                        )
                    )
            }

            @Test
            fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error for expired token`() {
                client
                    .graphqlMutation {
                        saveSharedWorkspaceMutation(token = preconditions.expiredWorkspaceAccessToken.token)
                    }
                    .from(preconditions.zoidberg)
                    .executeAndVerifyBusinessError(
                        message = "Token ${preconditions.expiredWorkspaceAccessToken.token} is not valid",
                        errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                        path = DgsConstants.MUTATION.SaveSharedWorkspace,
                    )
            }

            @Test
            fun `should return INVALID_WORKSPACE_ACCESS_TOKEN error for unknown token`() {
                client
                    .graphqlMutation { saveSharedWorkspaceMutation(token = "unknownToken") }
                    .from(preconditions.zoidberg)
                    .executeAndVerifyBusinessError(
                        message = "Token unknownToken is not valid",
                        errorCode = "INVALID_WORKSPACE_ACCESS_TOKEN",
                        path = DgsConstants.MUTATION.SaveSharedWorkspace,
                    )
            }
        }
    }

    private fun MutationProjection.saveSharedWorkspaceMutation(
        token: String = "validToken",
    ): MutationProjection = saveSharedWorkspace(token = token) {
        this.id
        this.name
        this.defaultCurrency
    }
}
