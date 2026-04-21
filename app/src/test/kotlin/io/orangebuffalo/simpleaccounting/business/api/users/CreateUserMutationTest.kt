package io.orangebuffalo.simpleaccounting.business.api.users

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
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

@DisplayName("createUser mutation")
class CreateUserMutationTest(
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
            client.graphqlMutation { createUserMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateUser)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphqlMutation { createUserMutation() }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateUser)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphqlMutation { createUserMutation() }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateUser)
        }
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("userName") { value -> createUserMutation(userName = value) },
            sizeConstraintTestCases("userName", maxLength = 255) { value -> createUserMutation(userName = value) },
            requiredFieldRejectedTestCases("admin") {
                createUserMutation()
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client.buildInputValidationRequest(testCase)
                .from(preconditions.farnsworth)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.CreateUser)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should create a new regular user`() {
            client.graphqlMutation {
                createUserMutation(userName = "Leela", admin = false)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateUser to buildJsonObject {
                        put("id", JsonValues.ANY_NUMBER)
                        put("userName", "Leela")
                        put("admin", false)
                        put("activated", false)
                    }
                )

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.userName == "Leela" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    PlatformUser(
                        userName = "Leela",
                        isAdmin = false,
                        activated = false,
                        passwordHash = "",
                    ),
                    PlatformUser::passwordHash,
                )
        }

        @Test
        fun `should create a new admin user`() {
            client.graphqlMutation {
                createUserMutation(userName = "Wernstrom", admin = true)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateUser to buildJsonObject {
                        put("id", JsonValues.ANY_NUMBER)
                        put("userName", "Wernstrom")
                        put("admin", true)
                        put("activated", false)
                    }
                )

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.userName == "Wernstrom" }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    PlatformUser(
                        userName = "Wernstrom",
                        isAdmin = true,
                        activated = false,
                        passwordHash = "",
                    ),
                    PlatformUser::passwordHash,
                )
        }

        @Test
        fun `should return USER_ALREADY_EXISTS error when username is taken`() {
            client.graphqlMutation {
                createUserMutation(userName = "Fry")
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "User with name 'Fry' already exists",
                    errorCode = "USER_ALREADY_EXISTS",
                    path = DgsConstants.MUTATION.CreateUser,
                )
        }
    }

    private fun MutationProjection.createUserMutation(
        userName: String = "Hermes",
        admin: Boolean = false,
    ) = createUser(userName = userName, admin = admin) {
        id
        this.userName
        this.admin
        activated
    }
}
