package io.orangebuffalo.simpleaccounting.business.users

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * See [ProfileApi] for the test subject.
 */
class ProfileRestApiTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    /**
     * [ProfileApi.getProfile]
     */
    @Nested
    @DisplayName("GET /api/profile")
    inner class GetProfile {
        private val preconditions by lazyPreconditions {
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
        private val preconditions by lazyPreconditions {
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
        private val preconditions by lazyPreconditions {
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
}


