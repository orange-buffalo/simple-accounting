package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.javacrumbs.jsonunit.core.Configuration
import net.javacrumbs.jsonunit.core.Option
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean

class CreateAccessTokenByCredentialsMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var refreshTokensService: RefreshTokensService

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val inactiveUser = platformUser(
                userName = "Scruffy",
                activated = false,
            )
        }
    }

    @Test
    fun `should return a JWT token for valid user login credentials`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "qwerty") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "createAccessTokenByCredentials" to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )
    }

    @Test
    fun `should return a JWT token for valid admin login credentials`() {
        whenever(passwordEncoder.matches("\$&#@(@", preconditions.farnsworth.passwordHash)) doReturn true

        client
            .graphqlMutation { loginMutation(preconditions.farnsworth.userName, "\$&#@(@") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                "createAccessTokenByCredentials" to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )
    }

    @Test
    fun `should return BAD_CREDENTIALS error when user is unknown`() {
        client
            .graphqlMutation { loginMutation("Roberto", "qwerty") }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Invalid Credentials",
                errorCode = "BAD_CREDENTIALS",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should return BAD_CREDENTIALS error when password does not match`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn false

        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "qwerty") }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "Invalid Credentials",
                errorCode = "BAD_CREDENTIALS",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should return USER_NOT_ACTIVATED error when user is not activated`() {
        client
            .graphqlMutation { loginMutation(preconditions.inactiveUser.userName, "irrelevant") }
            .fromAnonymous()
            .executeAndVerifyBusinessError(
                message = "User is not activated",
                errorCode = "USER_NOT_ACTIVATED",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when userName is blank`() {
        client
            .graphqlMutation { loginMutation("  ", "qwerty") }
            .fromAnonymous()
            .executeAndVerifyValidationError(
                violationPath = "userName",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when userName is empty`() {
        client
            .graphqlMutation { loginMutation("", "qwerty") }
            .fromAnonymous()
            .executeAndVerifyValidationError(
                violationPath = "userName",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when password is blank`() {
        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "  ") }
            .fromAnonymous()
            .executeAndVerifyValidationError(
                violationPath = "password",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when password is empty`() {
        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "") }
            .fromAnonymous()
            .executeAndVerifyValidationError(
                violationPath = "password",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "createAccessTokenByCredentials"
            )
    }

    @Test
    fun `should not set refresh token cookie when issueRefreshTokenCookie is not provided`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "qwerty") }
            .fromAnonymous()
            .execute()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
    }

    @Test
    fun `should not set refresh token cookie when issueRefreshTokenCookie is false`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        client
            .graphqlMutation {
                createAccessTokenByCredentials(
                    issueRefreshTokenCookie = false,
                    password = "qwerty",
                    userName = preconditions.fry.userName
                ) { accessToken }
            }
            .fromAnonymous()
            .execute()
            .expectStatus().isOk
            .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE)
    }

    @Test
    fun `should set refresh token cookie when issueRefreshTokenCookie is true`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        runBlocking {
            whenever(refreshTokensService.generateRefreshToken(preconditions.fry.userName)) doReturn "refreshTokenForFry"
        }

        client
            .graphqlMutation {
                createAccessTokenByCredentials(
                    issueRefreshTokenCookie = true,
                    password = "qwerty",
                    userName = preconditions.fry.userName
                ) { accessToken }
            }
            .fromAnonymous()
            .execute()
            .expectStatus().isOk
            .expectHeader().value(HttpHeaders.SET_COOKIE) { cookie ->
                assertThat(cookie).contains("refreshToken=refreshTokenForFry")
                    .contains("Max-Age=2592000")
                    .contains("Path=/api/auth/token")
                    .contains("HttpOnly")
                    .contains("SameSite=Strict")
            }
    }

    @Test
    fun `should allow authenticated users to call the mutation`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true

        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "qwerty") }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                "createAccessTokenByCredentials" to buildJsonObject {
                    put("accessToken", "\${json-unit.any-string}")
                }
            )
    }

    private fun MutationProjection.loginMutation(
        userName: String,
        password: String
    ): MutationProjection = createAccessTokenByCredentials(
        password = password,
        userName = userName
    ) {
        accessToken
    }
}
