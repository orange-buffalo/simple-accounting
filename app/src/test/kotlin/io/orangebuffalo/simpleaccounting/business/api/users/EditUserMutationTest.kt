package io.orangebuffalo.simpleaccounting.business.api.users

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
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

@DisplayName("editUser mutation")
class EditUserMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
            val fryWorkspace = workspace(owner = fry)
            val workspaceAccessToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphqlMutation { editUserMutation(id = preconditions.fry.id!!) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditUser)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphqlMutation { editUserMutation(id = preconditions.fry.id!!) }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditUser)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphqlMutation { editUserMutation(id = preconditions.fry.id!!) }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.EditUser)
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("userName") { value ->
                editUserMutation(id = preconditions.fry.id!!, userName = value)
            },
            sizeConstraintTestCases("userName", maxLength = 255) { value ->
                editUserMutation(id = preconditions.fry.id!!, userName = value)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client.buildInputValidationRequest(testCase)
                .from(preconditions.farnsworth)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.EditUser)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should update the username`() {
            client.graphqlMutation {
                editUserMutation(id = preconditions.fry.id!!, userName = "Philip J. Fry")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.EditUser to buildJsonObject {
                        put("id", preconditions.fry.id!!)
                        put("userName", "Philip J. Fry")
                        put("admin", false)
                        put("activated", true)
                    }
                )

            aggregateTemplate.findSingle<PlatformUser>(preconditions.fry.id!!)
                .shouldBeEntityWithFields(
                    PlatformUser(
                        userName = "Philip J. Fry",
                        isAdmin = false,
                        activated = true,
                        passwordHash = "",
                    ),
                    PlatformUser::passwordHash,
                )
        }

        @Test
        fun `should return entity not found error for non-existent user`() {
            client.graphqlMutation {
                editUserMutation(id = Long.MAX_VALUE, userName = "Ghost")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.EditUser)
        }

        @Test
        fun `should return USER_ALREADY_EXISTS error when username is taken`() {
            client.graphqlMutation {
                editUserMutation(id = preconditions.fry.id!!, userName = "Farnsworth")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "User with name 'Farnsworth' already exists",
                    errorCode = "USER_ALREADY_EXISTS",
                    path = DgsConstants.MUTATION.EditUser,
                )
        }
    }

    private fun MutationProjection.editUserMutation(
        id: Long,
        userName: String = "Hermes",
    ) = editUser(id = id, userName = userName) {
        this.id
        this.userName
        this.admin
        activated
    }
}
