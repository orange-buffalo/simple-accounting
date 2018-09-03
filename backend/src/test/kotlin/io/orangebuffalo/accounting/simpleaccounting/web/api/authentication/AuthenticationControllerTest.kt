package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AuthenticationControllerTest {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var entityManager: EntityManager

    lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient.bindToApplicationContext(applicationContext).build()
    }

    @Test
    fun `Should return a JWT token for valid login credentials`() {
        client.post().uri("/api/login")
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "user1",
                        password = "pass"
                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").exists()
    }

}