package io.orangebuffalo.simpleaccounting.business.users

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * See [ProfileApi] for the test subject.
 */
@SimpleAccountingIntegrationTest
class ProfileApiTest(
    @Autowired private val client: ApiTestClient,
    @Autowired private val testPasswordEncoder: PasswordEncoder,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
    private val preconditionsFactory: PreconditionsFactory,
) {

    /**
     * [ProfileApi.getProfile]
     */
    @Nested
    @DisplayName("GET /api/profile")
    inner class GetProfile {
        private val preconditions by preconditionsFactory {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = "google-drive",
                    i18nSettings = I18nSettings(
                        locale = "en_AU",
                        language = "en",
                    )
                )
                val zoidberg = platformUser(
                    userName = "Zoidberg",
                    i18nSettings = I18nSettings(locale = "en_US", language = "en")
                )
            }
        }

        private fun request() = client.get()
            .uri("/api/profile")

        @Test
        fun `should return 401 for unauthorized requests`() {
            request().verifyUnauthorized()
        }

        @Test
        fun `should return data for full profile`() {
            request()
                .from(preconditions.fry)
                .verifyOkAndJsonBodyEqualTo {
                    put("userName", "Fry")
                    put("documentsStorage", "google-drive")
                    putJsonObject("i18n") {
                        put("locale", "en_AU")
                        put("language", "en")
                    }
                }
        }

        @Test
        fun `should return data for minimum profile`() {
            request()
                .from(preconditions.zoidberg)
                .verifyOkAndJsonBodyEqualTo {
                    put("userName", "Zoidberg")
                    putJsonObject("i18n") {
                        put("locale", "en_US")
                        put("language", "en")
                    }
                }
        }
    }

    /**
     * [ProfileApi.updateProfile]
     */
    @Nested
    @DisplayName("PUT /api/profile")
    inner class UpdateProfile {
        private val preconditions by preconditionsFactory {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = "google-drive",
                    i18nSettings = I18nSettings(
                        locale = "en_AU",
                        language = "en",
                    )
                )
            }
        }

        private fun request() = client.put()
            .uri("/api/profile")

        @Test
        fun `should return 401 for unauthorized requests`() {
            request()
                .sendJson {
                    putJsonObject("i18n") {
                        put("locale", "el")
                        put("language", "uk")
                    }
                }
                .verifyUnauthorized()
        }

        @Test
        fun `should update profile`() {
            request()
                .from(preconditions.fry)
                .sendJson {
                    put("documentsStorage", "new-storage")
                    putJsonObject("i18n") {
                        put("locale", "el")
                        put("language", "uk")
                    }
                }
                .verifyOkAndJsonBodyEqualTo {
                    put("userName", "Fry")
                    put("documentsStorage", "new-storage")
                    putJsonObject("i18n") {
                        put("locale", "el")
                        put("language", "uk")
                    }
                }
            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.id == preconditions.fry.id }
                .shouldBeSingle()
                .shouldBeEntityWithFields(
                    PlatformUser(
                        documentsStorage = "new-storage",
                        i18nSettings = I18nSettings(locale = "el", language = "uk"),
                        activated = preconditions.fry.activated,
                        userName = preconditions.fry.userName,
                        passwordHash = preconditions.fry.passwordHash,
                        isAdmin = preconditions.fry.isAdmin,
                    )
                )
        }

        @Test
        fun `should clear documents storage setting`() {
            request()
                .from(preconditions.fry)
                .sendJson {
                    putJsonObject("i18n") {
                        put("locale", "el")
                        put("language", "uk")
                    }
                }
                .verifyOkAndJsonBodyEqualTo {
                    put("userName", "Fry")
                    putJsonObject("i18n") {
                        put("locale", "el")
                        put("language", "uk")
                    }
                }

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.id == preconditions.fry.id }
                .shouldBeSingle()
                .should {
                    it.documentsStorage.shouldBeNull()
                }
        }

        @Nested
        inner class RequestsValidation : ApiRequestsValidationsTestBase() {
            override val requestExecutionSpec = { requestBody: String ->
                request()
                    .from(preconditions.fry)
                    .sendJson(requestBody)
            }

            override val requestBodySpec: ApiRequestsBodyConfiguration = {
                string("documentsStorage", maxLength = 255, mandatory = false)
                nested("i18n", mandatory = true) {
                    string("locale", maxLength = 36, mandatory = true)
                    string("language", maxLength = 36, mandatory = true)
                }
            }
        }
    }

    /**
     * [ProfileApi.getDocumentsStorageStatus]
     */
    @Nested
    @DisplayName("GET /api/profile/documents-storage")
    inner class GetDocumentsStorageStatus {
        private val preconditions by preconditionsFactory {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = "noop",
                )
                val zoidberg = platformUser(
                    userName = "Zoidberg",
                    documentsStorage = null,
                )
            }
        }

        private fun request() = client.get()
            .uri("/api/profile/documents-storage")

        @Test
        fun `should return 401 for unauthorized requests`() {
            request().verifyUnauthorized()
        }

        @Test
        fun `should return document storage state when set`() {
            request()
                .from(preconditions.fry)
                .verifyOkAndJsonBodyEqualTo {
                    put("active", true)
                }
        }

        @Test
        fun `should return document storage state when not set`() {
            request()
                .from(preconditions.zoidberg)
                .verifyOkAndJsonBodyEqualTo {
                    put("active", false)
                }
        }
    }

    /**
     * [ProfileApi.changePassword]
     */
    @Nested
    @DisplayName("POST /api/profile/change-password")
    inner class ChangePassword {
        private val preconditions by preconditionsFactory {
            object {
                val fry = fry()
                val workspaceAccessToken = workspaceAccessToken()
                val farnsworth = farnsworth()
            }
        }

        private fun request() = client.post()
            .uri("/api/profile/change-password")

        @Test
        fun `should return 401 for unauthorized requests`() {
            request()
                .sendJson {
                    put("currentPassword", preconditions.fry.passwordHash)
                    put("newPassword", "new-password")
                }
                .verifyUnauthorized()
        }

        @Test
        fun `should return 400 when changing password by a transient user`() {
            request()
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .sendJson {
                    put("currentPassword", preconditions.fry.passwordHash)
                    put("newPassword", "new-password")
                }
                .verifyBadRequestAndJsonBodyEqualTo {
                    put("error", "TransientUser")
                    put("message", "Cannot change password for transient user")
                }
        }

        @Test
        fun `should return 400 when password does not match`() {
            whenever(testPasswordEncoder.matches("password", preconditions.fry.passwordHash)) doReturn false

            request()
                .from(preconditions.fry)
                .sendJson {
                    put("currentPassword", "password")
                    put("newPassword", "new-password")
                }
                .verifyBadRequestAndJsonBodyEqualTo {
                    put("error", "CurrentPasswordMismatch")
                    put("message", "Invalid current password")
                }
        }

        @Test
        fun `should change password for regular user`() {
            whenever(testPasswordEncoder.encode("new-password")) doReturn "new password hash"

            request()
                .from(preconditions.fry)
                .sendJson {
                    put("currentPassword", preconditions.fry.passwordHash)
                    put("newPassword", "new-password")
                }
                .verifyOkNoContent()

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.id == preconditions.fry.id }
                .shouldBeSingle()
                .passwordHash.shouldBeEqual("new password hash")
        }

        @Test
        fun `should change password for admin user`() {
            whenever(testPasswordEncoder.encode("new-password")) doReturn "new password hash"

            request()
                .from(preconditions.farnsworth)
                .sendJson {
                    put("currentPassword", preconditions.farnsworth.passwordHash)
                    put("newPassword", "new-password")
                }
                .verifyOkNoContent()

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.id == preconditions.farnsworth.id }
                .shouldBeSingle()
                .passwordHash.shouldBeEqual("new password hash")
        }

        @Nested
        inner class RequestsValidation : ApiRequestsValidationsTestBase() {
            override val requestExecutionSpec = { requestBody: String ->
                request()
                    .from(preconditions.fry)
                    .sendJson(requestBody)
            }

            override val requestBodySpec: ApiRequestsBodyConfiguration = {
                string("currentPassword", mandatory = true, maxLength = 100)
                string("newPassword", mandatory = true, maxLength = 100)
            }

            override val successResponseStatus = HttpStatus.NO_CONTENT
        }
    }
}


