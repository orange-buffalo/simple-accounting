package io.orangebuffalo.accounting.simpleaccounting.web

import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.KotlinBodySpec
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

fun WebTestClient.ResponseSpec.expectThatJsonBody(
    consumer: JsonAssert.ConfigurableJsonAssert.() -> Unit
): KotlinBodySpec<String> =
    expectBody<String>()
        .consumeWith { body ->
            val responseJson = body.responseBody
            assertThat(responseJson).isNotBlank()

            val jsonAssert = assertThatJson(responseJson)
            jsonAssert.consumer()
        }

@Component
class DbHelper(private val jdbcTemplate: JdbcTemplate) {

    fun getNextId() = jdbcTemplate
        .queryForObject("select nextval('hibernate_sequence')", Long::class.java)!! + 1

}
