package io.orangebuffalo.simpleaccounting.infra.api

import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.JsonAssertions
import org.assertj.core.api.Assertions
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.test.StepVerifier

fun WebTestClient.ResponseSpec.expectThatJsonBody(
    spec: JsonAssert.ConfigurableJsonAssert.() -> Unit
) = expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<String>().consumeWith { body ->
        val responseJson = body.responseBody
        Assertions.assertThat(responseJson).isNotBlank()

        val jsonAssert = JsonAssertions.assertThatJson(responseJson!!)
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

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndJsonBody(
    jsonBody: String
) = verifyOkAndJsonBody {
    isEqualTo(jsonBody)
}

fun WebTestClient.RequestHeadersSpec<*>.verifyBadRequestAndJsonBody(
    spec: JsonAssert.ConfigurableJsonAssert.() -> Unit
) = exchange()
    .expectStatus().isBadRequest
    .expectThatJsonBody(spec)

fun WebTestClient.RequestHeadersSpec<*>.verifyBadRequestAndJsonBody(
    jsonBody: String
) = verifyBadRequestAndJsonBody {
    isEqualTo(jsonBody)
}

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndBody(
    spec: (body: String) -> Unit
) = exchange()
    .expectStatus().isOk
    .expectBody<String>()
    .consumeWith { response ->
        val body = response.responseBody
        Assertions.assertThat(body).isNotBlank()
        spec(body!!)
    }

fun WebTestClient.RequestHeadersSpec<*>.verifyOkNoContent() = exchange()
    .expectStatus().isNoContent

fun WebTestClient.RequestBodySpec.sendJson(json: String): WebTestClient.RequestHeadersSpec<*> =
    contentType(MediaType.APPLICATION_JSON).bodyValue(json)

fun <T> StepVerifier.Step<T>.assertNextJson(
    consumer: JsonAssert.ConfigurableJsonAssert.() -> Unit
): StepVerifier.Step<T> {
    return assertNext { data ->
        Assertions.assertThat(data).isNotNull
        JsonAssertions.assertThatJson(data!!).consumer()
    }
}

fun <T> StepVerifier.Step<T>.assertNextJsonIs(jsonObject: String): StepVerifier.Step<T> {
    return assertNextJson {
        isEqualTo(JsonAssertions.json(jsonObject))
    }
}
