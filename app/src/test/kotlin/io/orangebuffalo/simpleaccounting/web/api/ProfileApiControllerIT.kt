package io.orangebuffalo.simpleaccounting.web.api

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.api.*
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockZoidbergUser
import io.orangebuffalo.simpleaccounting.infra.security.WithSaMockUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import io.orangebuffalo.simpleaccounting.services.persistence.repos.PlatformUserRepository
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Profile API ")
class ProfileApiControllerIT(
    @Autowired val client: WebTestClient,
    @Autowired val testPasswordEncoder: PasswordEncoder,
    @Autowired val userRepository: PlatformUserRepository,
) {

    @MockBean
    private lateinit var documentsService: DocumentsService

    @Test
    fun `should return 401 for unauthorized requests`(testData: ProfileApiTestData) {
        client.get()
            .uri("/api/profile")
            .verifyUnauthorized()
    }

    @Test
    @WithMockFryUser
    fun `should return data for full profile`(testData: ProfileApiTestData) {
        client.get()
            .uri("/api/profile")
            .verifyOkAndJsonBody(
                """{
                    "userName": "Fry",
                    "documentsStorage": "google-drive",
                    "i18n": {
                        "locale": "en_AU",
                        "language": "en"
                    }
                }"""
            )
    }

    @Test
    @WithMockZoidbergUser
    fun `should return data for minimum profile`(testData: ProfileApiTestData) {
        client.get()
            .uri("/api/profile")
            .verifyOkAndJsonBody(
                """{
                    "userName": "Zoidberg",
                    "i18n": {
                        "locale": "en_US",
                        "language": "en"
                    }
                }"""
            )
    }

    @Test
    @WithMockFryUser
    fun `should clear documents storage setting`(testData: ProfileApiTestData) {
        client.put()
            .uri("/api/profile")
            .sendJson(
                """{
                    "i18n": {
                        "locale": "en_AU",
                        "language": "en"
                    }
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    "userName": "Fry",
                    "i18n": {
                        "locale": "en_AU",
                        "language": "en"
                    }
                }"""
            )
    }

    @Test
    @WithMockZoidbergUser
    fun `should update profile`(testData: ProfileApiTestData) {
        client.put()
            .uri("/api/profile")
            .sendJson(
                """{
                   "documentsStorage": "new-storage",
                    "i18n": {
                                "locale": "el",
                                "language": "uk"
                            }
                }"""
            )
            .verifyOkAndJsonBody(
                """{
                    "userName": "Zoidberg",
                    "documentsStorage": "new-storage",
                    "i18n": {
                        "locale": "el",
                        "language": "uk"
                    }
                }"""
            )
    }

    @Test
    @WithMockZoidbergUser
    fun `should delegate to Documents Service on storage request`(testData: ProfileApiTestData) {
        documentsService.stub {
            onBlocking { getCurrentUserStorageStatus() } doReturn DocumentsStorageStatus(true)
        }

        client.get()
            .uri("/api/profile/documents-storage")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "active": true
                        }"""
                    )
                )
            }
    }

    @Test
    fun `should return 401 for unauthorized requests to change password`(testData: ProfileApiTestData) {
        client.post()
            .uri("/api/profile/change-password")
            .sendJson(
                """{
                    "currentPassword": "${testData.fry.passwordHash}",
                    "newPassword": "new password"
                }"""
            )
            .verifyUnauthorized()
    }

    @Test
    @WithSaMockUser(transient = true, workspaceAccessToken = "wsToken")
    fun `should return 400 when changing password by a transient user`(testData: ProfileApiTestData) {
        client.post()
            .uri("/api/profile/change-password")
            .sendJson(
                """{
                    "currentPassword": "password",
                    "newPassword": "new password"
                }"""
            )
            .verifyBadRequestAndJsonBody(
                """{
                    "error": "TransientUser",
                    "message": "Cannot change password for transient user"
                }"""
            )
    }

    @Test
    @WithMockFarnsworthUser
    fun `should return 400 when password does not match`(testData: ProfileApiTestData) {
        whenever(testPasswordEncoder.matches("password", testData.farnsworth.passwordHash)) doReturn false

        client.post()
            .uri("/api/profile/change-password")
            .sendJson(
                """{
                    "currentPassword": "password",
                    "newPassword": "new password"
                }"""
            )
            .verifyBadRequestAndJsonBody(
                """{
                    "error": "CurrentPasswordMismatch",
                    "message": "Invalid current password"
                }"""
            )
    }

    @Test
    @WithMockZoidbergUser
    fun `should change password for regular user`(testData: ProfileApiTestData) {
        whenever(testPasswordEncoder.encode("new password")) doReturn "new password hash"

        client.post()
            .uri("/api/profile/change-password")
            .sendJson(
                """{
                    "currentPassword": "password",
                    "newPassword": "new password"
                }"""
            )
            .verifyOkNoContent()

        userRepository.findByUserName(testData.zoidberg.userName)
            ?.passwordHash
            .shouldBe("new password hash")
    }

    @Test
    @WithMockFarnsworthUser
    fun `should change password for admin user`(testData: ProfileApiTestData) {
        whenever(testPasswordEncoder.encode("new password")) doReturn "new password hash"

        client.post()
            .uri("/api/profile/change-password")
            .sendJson(
                """{
                    "currentPassword": "password",
                    "newPassword": "new password"
                }"""
            )
            .verifyOkNoContent()

        userRepository.findByUserName(testData.farnsworth.userName)
            ?.passwordHash
            .shouldBe("new password hash")
    }
}

class ProfileApiTestData : TestData {

    val fry = Prototypes.fry().apply {
        documentsStorage = "google-drive"
        i18nSettings.locale = "en_AU"
        i18nSettings.language = "en"
    }
    val zoidberg = Prototypes.platformUser(
        userName = "Zoidberg",
        i18nSettings = I18nSettings(locale = "en_US", language = "en")
    )
    val farnsworth = Prototypes.farnsworth()
}
