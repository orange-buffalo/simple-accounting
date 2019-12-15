package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.*
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
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
    fun `should update documents storage setting`(testData: ProfileApiTestData) {
        client.put()
            .uri("/api/profile")
            .sendJson(
                """{
                   "documentsStorage": "new-storage" 
                }"""
            )
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Zoidberg",
                            "documentsStorage": "new-storage",
                            "i18n": {
                                "locale": "en_US",
                                "language": "en"
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
