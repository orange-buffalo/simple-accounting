package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.security.JwtService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AuthenticationControllerTest {

    @MockBean
    lateinit var passwordEncoder: PasswordEncoder

    @MockBean
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `Should return a JWT token for valid login credentials`() {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            username == "Fry" && password == "qwertyHash"
        })) doReturn "jwtTokenForFry"

        client.post().uri("/api/login")
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "Fry",
                        password = "qwerty"
                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForFry")
    }

    @TestConfiguration
    class Config {
        @Bean
        fun testSetupRunner(
                transactionTemplate: TransactionTemplate,
                entityManager: EntityManager): ApplicationRunner = ApplicationRunner { _ ->

            transactionTemplate.execute {
                entityManager.persist(PlatformUser(
                        userName = "Fry",
                        passwordHash = "qwertyHash",
                        isAdmin = false
                ))
            }
        }

        @Bean
        fun webClient(applicationContext: ApplicationContext): WebTestClient {
            return WebTestClient.bindToApplicationContext(applicationContext).build()
        }
    }
}