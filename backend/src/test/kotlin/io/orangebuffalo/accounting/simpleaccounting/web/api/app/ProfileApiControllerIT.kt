package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.junit.TestData
import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.sendJson
import io.orangebuffalo.accounting.simpleaccounting.verifyOkAndJsonBody
import io.orangebuffalo.accounting.simpleaccounting.verifyUnauthorized
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    @WithMockUser(roles = ["USER"], username = "Fry")
    fun `should return data for full profile`(testData: ProfileApiTestData) {
        client.get()
            .uri("/api/profile")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Fry",
                            "documentsStorage": "google-drive"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Zoidberg")
    fun `should return data for minimum profile`(testData: ProfileApiTestData) {
        client.get()
            .uri("/api/profile")
            .verifyOkAndJsonBody {
                inPath("$").isEqualTo(
                    json(
                        """{
                            "userName": "Zoidberg"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Fry")
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
                            "userName": "Fry"
                        }"""
                    )
                )
            }
    }

    @Test
    @WithMockUser(roles = ["USER"], username = "Zoidberg")
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
                            "documentsStorage": "new-storage"
                        }"""
                    )
                )
            }
    }
}

class ProfileApiTestData : TestData {
    override fun generateData() = listOf(
        Prototypes.platformUser(userName = "Fry", documentsStorage = "google-drive"),
        Prototypes.platformUser(userName = "Zoidberg")
    )
}
