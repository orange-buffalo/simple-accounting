package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import io.orangebuffalo.simpleaccounting.business.security.remeberme.RefreshTokensService
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class CreateAccessTokenByCredentialsMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @MockitoBean
    lateinit var refreshTokensService: RefreshTokensService

    @MockitoSpyBean
    lateinit var jwtService: JwtService

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
        doReturn("jwtTokenForFry").whenever(jwtService).buildJwtToken(argThat {
            userName == preconditions.fry.userName
        }, isNull())

        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "qwerty") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.CreateAccessTokenByCredentials to buildJsonObject {
                    put("accessToken", "jwtTokenForFry")
                }
            )
    }

    @Test
    fun `should return a JWT token for valid admin login credentials`() {
        whenever(passwordEncoder.matches("\$&#@(@", preconditions.farnsworth.passwordHash)) doReturn true
        doReturn("jwtTokenForFarnsworth").whenever(jwtService).buildJwtToken(argThat {
            userName == preconditions.farnsworth.userName
        }, isNull())

        client
            .graphqlMutation { loginMutation(preconditions.farnsworth.userName, "\$&#@(@") }
            .fromAnonymous()
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.CreateAccessTokenByCredentials to buildJsonObject {
                    put("accessToken", "jwtTokenForFarnsworth")
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                path = DgsConstants.MUTATION.CreateAccessTokenByCredentials
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
                    .contains("Path=/api")
                    .contains("HttpOnly")
                    .contains("SameSite=Strict")
            }
    }

    @Test
    fun `should allow authenticated users to call the mutation`() {
        whenever(passwordEncoder.matches("qwerty", preconditions.fry.passwordHash)) doReturn true
        doReturn("jwtTokenForFry").whenever(jwtService).buildJwtToken(argThat {
            userName == preconditions.fry.userName
        }, isNull())

        client
            .graphqlMutation { loginMutation(preconditions.fry.userName, "qwerty") }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.MUTATION.CreateAccessTokenByCredentials to buildJsonObject {
                    put("accessToken", "jwtTokenForFry")
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
