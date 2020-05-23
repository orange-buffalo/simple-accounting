package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.*
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

@SimpleAccountingIntegrationTest
@DisplayName("Profile API ")
class ProfileApiControllerIT(
    @Autowired val client: WebTestClient
) {

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
        )
    )
}
