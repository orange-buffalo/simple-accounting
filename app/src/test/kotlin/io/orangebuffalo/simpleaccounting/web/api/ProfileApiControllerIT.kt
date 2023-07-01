package io.orangebuffalo.simpleaccounting.web.api

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.stub
import io.orangebuffalo.simpleaccounting.*
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import io.orangebuffalo.simpleaccounting.domain.documents.storage.DocumentsStorageStatus
import io.orangebuffalo.simpleaccounting.infra.api.sendJson
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndJsonBody
import io.orangebuffalo.simpleaccounting.infra.api.verifyUnauthorized
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.database.TestData
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFarnsworthUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockFryUser
import io.orangebuffalo.simpleaccounting.infra.security.WithMockZoidbergUser
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Profile API ")
class ProfileApiControllerIT(
    @Autowired val client: WebTestClient
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
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Fry",
                            "documentsStorage": "google-drive",
                            "i18n": {
                                "locale": "en_AU",
                                "language": "en"
                            }
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockZoidbergUser
    fun `should return data for minimum profile`(testData: ProfileApiTestData) {
        client.get()
            .uri("/api/profile")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Zoidberg",
                            "i18n": {
                                "locale": "en_US",
                                "language": "en"
                            }
                        }"""
                    )
                )
            }
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
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Fry",
                            "i18n": {
                                "locale": "en_AU",
                                "language": "en"
                            }
                        }"""
                    )
                )
            }
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
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Zoidberg",
                            "documentsStorage": "new-storage",
                            "i18n": {
                                "locale": "el",
                                "language": "uk"
                            }
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockFarnsworthUser
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
}

class ProfileApiTestData : TestData {
    override fun generateData() = listOf(
        Prototypes.platformUser(
            userName = "Fry",
            documentsStorage = "google-drive",
            i18nSettings = I18nSettings(locale = "en_AU", language = "en")
        ),
        Prototypes.platformUser(
            userName = "Zoidberg",
            i18nSettings = I18nSettings(locale = "en_US", language = "en")
        ),
        Prototypes.platformUser(
            userName = "Farnsworth"
        )
    )
}
