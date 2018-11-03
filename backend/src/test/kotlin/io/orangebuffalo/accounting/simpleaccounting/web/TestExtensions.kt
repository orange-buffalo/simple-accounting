package io.orangebuffalo.accounting.simpleaccounting.web

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.KotlinBodySpec
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.ZoneId
import java.time.ZonedDateTime

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

val MOCK_TIME: ZonedDateTime = ZonedDateTime.of(1999, 3, 28, 18, 1, 2, 42000000, ZoneId.of("America/New_York"))
const val MOCK_TIME_VALUE = "1999-03-28T18:01:02.042-05:00"

fun mockCurrentTime(timeService: TimeService) {
    whenever(timeService.currentTime()) doReturn MOCK_TIME
}