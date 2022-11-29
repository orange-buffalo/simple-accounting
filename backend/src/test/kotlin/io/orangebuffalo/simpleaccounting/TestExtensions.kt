package io.orangebuffalo.simpleaccounting

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import net.javacrumbs.jsonunit.assertj.JsonAssertions.json
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.test.StepVerifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun WebTestClient.ResponseSpec.expectThatJsonBody(
    spec: JsonAssert.ConfigurableJsonAssert.() -> Unit
) = expectBody<String>()
    .consumeWith { body ->
        val responseJson = body.responseBody
        assertThat(responseJson).isNotBlank()

        val jsonAssert = assertThatJson(responseJson!!)
        jsonAssert.spec()
    }

fun WebTestClient.RequestHeadersSpec<*>.verifyUnauthorized(): WebTestClient.ResponseSpec =
    exchange().expectStatus().isUnauthorized

fun WebTestClient.RequestHeadersSpec<*>.verifyNotFound(errorMessage: String) = exchange()
    .expectStatus().isNotFound
    .expectBody<String>().isEqualTo(errorMessage)

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndJsonBody(
    spec: JsonAssert.ConfigurableJsonAssert.() -> Unit
) = exchange()
    .expectStatus().isOk
    .expectThatJsonBody(spec)

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndBody(
    spec: (body: String) -> Unit
) = exchange()
    .expectStatus().isOk
    .expectBody<String>()
    .consumeWith { response ->
        val body = response.responseBody
        assertThat(body).isNotBlank()
        spec(body!!)
    }

fun WebTestClient.RequestBodySpec.sendJson(json: String): WebTestClient.RequestHeadersSpec<*> =
    contentType(MediaType.APPLICATION_JSON).bodyValue(json)

val MOCK_TIME: Instant = ZonedDateTime.of(1999, 3, 28, 18, 1, 2, 42000000, ZoneId.of("America/New_York")).toInstant()
const val MOCK_TIME_VALUE = "1999-03-28T23:01:02.042Z"

fun mockCurrentTime(timeService: TimeService) {
    whenever(timeService.currentTime()) doReturn MOCK_TIME
}

fun mockCurrentDate(timeService: TimeService) {
    whenever(timeService.currentDate()) doReturn MOCK_DATE
}

val MOCK_DATE: LocalDate = LocalDate.of(1999, 3, 28)
const val MOCK_DATE_VALUE = "1999-03-28"

fun <T> StepVerifier.Step<T>.assertNextJson(
    consumer: JsonAssert.ConfigurableJsonAssert.() -> Unit
): StepVerifier.Step<T> {
    return assertNext { data ->
        assertThat(data).isNotNull
        assertThatJson(data!!).consumer()
    }
}

fun <T> StepVerifier.Step<T>.assertNextJsonIs(jsonObject: String): StepVerifier.Step<T> {
    return assertNextJson {
        isEqualTo(json(jsonObject))
    }
}
