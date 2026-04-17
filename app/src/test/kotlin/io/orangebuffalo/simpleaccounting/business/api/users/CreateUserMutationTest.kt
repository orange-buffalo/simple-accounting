package io.orangebuffalo.simpleaccounting.business.api.users

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
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

@DisplayName("createUser mutation")
class CreateUserMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
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
    }

    @Nested
    @DisplayName("Input Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("userName") { value -> createUserMutation(userName = value) },
            sizeConstraintTestCases("userName", maxLength = 255) { value -> createUserMutation(userName = value) },
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
