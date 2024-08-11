package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.kotest.matchers.should
import io.kotest.matchers.string.shouldNotBeBlank
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import net.javacrumbs.jsonunit.core.Configuration
import net.javacrumbs.jsonunit.kotest.equalJson
import org.assertj.core.api.Assertions
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.test.StepVerifier

fun WebTestClient.ResponseSpec.expectThatJsonBody(
    spec: String.() -> Unit
) = expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<String>().consumeWith { body ->
        val responseJson = body.responseBody.shouldNotBeBlank()!!
        spec(responseJson)
    }

fun WebTestClient.ResponseSpec.expectThatJsonBodyEqualTo(
    configuration: Configuration = Configuration.empty(),
    spec: JsonObjectBuilder.() -> Unit
) = expectThatJsonBody {
    shouldBeEqualToJson(configuration, spec)
}

fun WebTestClient.RequestHeadersSpec<*>.verifyUnauthorized(): WebTestClient.ResponseSpec =
    exchange().expectStatus().isUnauthorized

fun WebTestClient.RequestHeadersSpec<*>.verifyNotFound(errorMessage: String) = exchange()
    .expectStatus().isNotFound
    .expectBody<String>().isEqualTo(errorMessage)

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndJsonBody(
    spec: String.() -> Unit
) = exchange()
    .expectStatus().isOk
    .expectThatJsonBody(spec)

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndJsonBodyEqualTo(
    spec: JsonObjectBuilder.() -> Unit
) {
    verifyOkAndJsonBody {
        shouldBeEqualToJson(spec = spec)
    }
}

fun WebTestClient.RequestHeadersSpec<*>.verifyCreatedAndJsonBodyEqualTo(
    spec: JsonObjectBuilder.() -> Unit
) {
    exchange()
        .expectStatus().isCreated
        .expectThatJsonBody {
            shouldBeEqualToJson(spec = spec)
        }
}

fun WebTestClient.RequestHeadersSpec<*>.verifyOkAndJsonBody(
    jsonBody: String
) = verifyOkAndJsonBody {
    shouldBeEqualToJson(jsonBody)
}

fun WebTestClient.RequestHeadersSpec<*>.verifyBadRequestAndJsonBody(
    spec: String.() -> Unit
) = exchange()
    .expectStatus().isBadRequest
    .expectThatJsonBody(spec)

fun WebTestClient.RequestHeadersSpec<*>.verifyBadRequestAndJsonBody(
    jsonBody: String
) = verifyBadRequestAndJsonBody {
    shouldBeEqualToJson(jsonBody)
}

fun WebTestClient.RequestHeadersSpec<*>.verifyBadRequestAndJsonBodyEqualTo(
    spec: JsonObjectBuilder.() -> Unit
) = verifyBadRequestAndJsonBody {
    shouldBeEqualToJson(spec = spec)
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

fun WebTestClient.RequestBodySpec.sendJson(spec: JsonObjectBuilder.() -> Unit): WebTestClient.RequestHeadersSpec<*> {
    val jsonElement = buildJsonObject {
        spec(this)
    }
    return sendJson(jsonElement.toString())
}

fun <T> StepVerifier.Step<T>.assertNextJsonIs(
    jsonObject: String
): StepVerifier.Step<T> {
    return assertNext { data ->
        Assertions.assertThat(data).isNotNull
        data.should(equalJson(jsonObject))
    }
}

fun String.shouldBeEqualToJson(
    configuration: Configuration = Configuration.empty(),
    spec: JsonObjectBuilder.() -> Unit
) {
    val jsonElement = buildJsonObject {
        spec(this)
    }
    this.should(equalJson(jsonElement.toString(), configuration))
}

fun String.shouldBeEqualToJson(expectedJson: String) {
    this.should(equalJson(expectedJson))
}
