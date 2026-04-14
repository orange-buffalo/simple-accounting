package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsClient
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import kotlinx.serialization.json.*
import net.javacrumbs.jsonunit.core.Configuration
import net.javacrumbs.jsonunit.kotest.equalJson
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
        body.shouldNotBeBlank()
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
        data.shouldNotBeNull()
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

fun ApiTestClient.graphql(querySpec: QueryProjection.() -> QueryProjection): GraphqlClientRequestExecutor =
    buildGraphqlRequest { DgsClient.buildQuery(_projection = querySpec) }

fun ApiTestClient.graphqlMutation(mutationSpec: MutationProjection.() -> MutationProjection): GraphqlClientRequestExecutor =
    buildGraphqlRequest { DgsClient.buildMutation(_projection = mutationSpec) }

fun ApiTestClient.graphqlRawQuery(query: String): GraphqlClientRequestExecutor = this
    .post()
    .uri("/api/graphql")
    .sendJson {
        put("query", query)
    }
    .let {
        GraphqlClientRequestExecutor(it)
    }

/**
 * Builds a [GraphqlClientRequestExecutor] for the given input validation test case.
 * Dispatches to the appropriate request builder based on the test case type:
 * - [GraphqlMutationValidationErrorTestCase] and [GraphqlMutationValidBoundaryTestCase] use DGS mutation projections
 * - [GraphqlMutationRejectedInputTestCase] uses a raw GraphQL query (for null inputs)
 */
fun ApiTestClient.buildInputValidationRequest(testCase: GraphqlMutationInputTestCase): GraphqlClientRequestExecutor =
    when (testCase) {
        is GraphqlMutationValidationErrorTestCase -> graphqlMutation(testCase.mutation)
        is GraphqlMutationRejectedInputTestCase -> graphqlRawQuery(testCase.rawQueryBuilder())
        is GraphqlMutationValidBoundaryTestCase -> graphqlMutation(testCase.mutation)
    }

private fun ApiTestClient.buildGraphqlRequest(queryBuilder: () -> String): GraphqlClientRequestExecutor = this
    .post()
    .uri("/api/graphql")
    .sendJson {
        val query = queryBuilder()
            .lines()
            // DGS adds __typename to every object, which we cannot control and do not need;
            // to simplify testing we remove it here (so we do not need to expect it in assertions)
            .filter { line -> line.trim() != ("__typename") }
            .joinToString("\n")
        put("query", query)
    }
    .let {
        GraphqlClientRequestExecutor(it)
    }

/**
 * Handles GraphQL requests execution and assertions.
 */
class GraphqlClientRequestExecutor(
    private var requestSpec: WebTestClient.RequestHeadersSpec<*>,
) {
    fun from(platformUser: PlatformUser): GraphqlClientRequestExecutor {
        requestSpec = requestSpec.from(platformUser)
        return this
    }

    fun usingSharedWorkspaceToken(workspaceToken: String): GraphqlClientRequestExecutor {
        requestSpec = requestSpec.usingSharedWorkspaceToken(workspaceToken)
        return this
    }

    fun fromAnonymous(): GraphqlClientRequestExecutor {
        requestSpec = requestSpec.fromAnonymous()
        return this
    }

    fun cookie(name: String, value: String): GraphqlClientRequestExecutor {
        requestSpec = requestSpec.cookie(name, value)
        return this
    }

    fun executeAndVerifySuccessResponse(
        vararg dataItems: Pair<String, JsonObject>,
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBodyEqualTo {
                putJsonObject("data") {
                    dataItems.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            }
    }

    fun executeAndVerifyResponse(
        vararg dataItems: Pair<String, JsonElement>,
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBodyEqualTo {
                putJsonObject("data") {
                    dataItems.forEach { (key, value) ->
                        put(key, value)
                    }
                }
            }
    }

    fun executeAndVerifySingleError(
        message: String,
        errorType: String,
        locationColumn: Int,
        locationLine: Int,
        path: String,
    ) = executeAndVerifySingleError(
        message = message,
        errorType = errorType,
        locationColumn = locationColumn,
        locationLine = locationLine,
        paths = listOf(path),
    )

    fun executeAndVerifySingleError(
        message: String,
        errorType: String,
        locationColumn: Int,
        locationLine: Int,
        paths: List<String>,
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBodyEqualTo {
                putJsonArray("errors") {
                    add(buildJsonObject {
                        put("message", message)
                        put("extensions", buildJsonObject {
                            put("errorType", errorType)
                        })
                        putJsonArray("locations") {
                            add(buildJsonObject {
                                put("column", locationColumn)
                                put("line", locationLine)
                            })
                        }
                        putJsonArray("path") {
                            paths.forEach { add(it) }
                        }
                    })
                }
            }
    }

    fun executeAndVerifyValidationError(
        violationPath: String,
        error: String,
        message: String,
        path: String,
        params: Map<String, String>? = null,
        locationColumn: Int = 3,
        locationLine: Int = 2
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBodyEqualTo {
                putJsonArray("errors") {
                    add(buildJsonObject {
                        put("message", "Validation failed")
                        put("extensions", buildJsonObject {
                            put("errorType", "FIELD_VALIDATION_FAILURE")
                            putJsonArray("validationErrors") {
                                add(buildJsonObject {
                                    put("path", violationPath)
                                    put("error", error)
                                    put("message", message)
                                    params?.let {
                                        putJsonArray("params") {
                                            it.forEach { (key, value) ->
                                                add(buildJsonObject {
                                                    put("name", key)
                                                    put("value", value)
                                                })
                                            }
                                        }
                                    }
                                })
                            }
                        })
                        putJsonArray("locations") {
                            add(buildJsonObject {
                                put("column", locationColumn)
                                put("line", locationLine)
                            })
                        }
                        putJsonArray("path") {
                            add(path)
                        }
                    })
                }
            }
    }

    fun executeAndVerifyBusinessError(
        message: String,
        errorCode: String,
        path: String,
        locationColumn: Int = 3,
        locationLine: Int = 2,
        additionalExtensions: Map<String, Any>? = null
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBodyEqualTo {
                putJsonArray("errors") {
                    add(buildJsonObject {
                        put("message", message)
                        put("extensions", buildJsonObject {
                            put("errorType", "BUSINESS_ERROR")
                            put("errorCode", errorCode)
                            additionalExtensions?.forEach { (key, value) ->
                                when (value) {
                                    is String -> put(key, value)
                                    is Number -> put(key, value.toLong())
                                    is Boolean -> put(key, value)
                                    else -> put(key, value.toString())
                                }
                            }
                        })
                        putJsonArray("locations") {
                            add(buildJsonObject {
                                put("column", locationColumn)
                                put("line", locationLine)
                            })
                        }
                        putJsonArray("path") {
                            add(path)
                        }
                    })
                }
            }
    }

    fun executeAndVerifyNotAuthorized(
        path: String,
        locationColumn: Int = 3,
        locationLine: Int = 2,
        ignoreExtraData: Boolean = false,
    ) {
        if (ignoreExtraData) {
            verifyErrorInResponse(
                message = "User is not authenticated",
                errorType = "NOT_AUTHORIZED",
                locationColumn = locationColumn,
                locationLine = locationLine,
                path = path,
            )
        } else {
            executeAndVerifySingleError(
                message = "User is not authenticated",
                errorType = "NOT_AUTHORIZED",
                locationColumn = locationColumn,
                locationLine = locationLine,
                path = path,
            )
        }
    }

    fun executeAndVerifyNotAuthorized(
        paths: List<String>,
        locationColumn: Int = 3,
        locationLine: Int = 2
    ) {
        executeAndVerifySingleError(
            message = "User is not authenticated",
            errorType = "NOT_AUTHORIZED",
            locationColumn = locationColumn,
            locationLine = locationLine,
            paths = paths,
        )
    }

    private fun verifyErrorInResponse(
        message: String,
        errorType: String,
        locationColumn: Int,
        locationLine: Int,
        path: String,
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                val json = Json.parseToJsonElement(this).jsonObject
                val errors = json["errors"]?.jsonArray.shouldNotBeNull()
                errors.shouldNotBeEmpty()
                val error = errors[0].jsonObject
                withClue("Expected message to be '$message'") {
                    error["message"]?.jsonPrimitive?.content.shouldBe(message)
                }
                val extensions = error["extensions"]?.jsonObject.shouldNotBeNull()
                withClue("Expected errorType to be $errorType") {
                    extensions["errorType"]?.jsonPrimitive?.content.shouldBe(errorType)
                }
                val locations = error["locations"]?.jsonArray.shouldNotBeNull()
                locations.shouldNotBeEmpty()
                val location = locations[0].jsonObject
                withClue("Expected location column") {
                    location["column"]?.jsonPrimitive?.int.shouldBe(locationColumn)
                }
                withClue("Expected location line") {
                    location["line"]?.jsonPrimitive?.int.shouldBe(locationLine)
                }
                val pathArray = error["path"]?.jsonArray.shouldNotBeNull()
                pathArray.shouldNotBeEmpty()
                withClue("Expected path") {
                    pathArray[0].jsonPrimitive.content.shouldBe(path)
                }
            }
    }

    fun executeAndVerifyEntityNotFoundError(
        path: String,
        locationColumn: Int = 3,
        locationLine: Int = 2,
    ) {
        requestSpec
            .exchange()
            .expectStatus().isOk
            .expectThatJsonBody {
                val json = Json.parseToJsonElement(this).jsonObject
                val errors = json["errors"]?.jsonArray.shouldNotBeNull()
                errors.shouldNotBeEmpty()
                val error = errors[0].jsonObject
                val extensions = error["extensions"]?.jsonObject.shouldNotBeNull()
                withClue("Expected errorType to be ENTITY_NOT_FOUND") {
                    extensions["errorType"]?.jsonPrimitive?.content.shouldBe("ENTITY_NOT_FOUND")
                }
                val locations = error["locations"]?.jsonArray.shouldNotBeNull()
                locations.shouldNotBeEmpty()
                val location = locations[0].jsonObject
                withClue("Expected location column") {
                    location["column"]?.jsonPrimitive?.int.shouldBe(locationColumn)
                }
                withClue("Expected location line") {
                    location["line"]?.jsonPrimitive?.int.shouldBe(locationLine)
                }
                val pathArray = error["path"]?.jsonArray.shouldNotBeNull()
                pathArray.shouldNotBeEmpty()
                withClue("Expected path") {
                    pathArray[0].jsonPrimitive.content.shouldBe(path)
                }
            }
    }

    /**
     * Executes the request and verifies the response based on the [GraphqlMutationInputTestCase] type:
     * - [GraphqlMutationValidationErrorTestCase] — verifies exact `FIELD_VALIDATION_FAILURE` error structure
     * - [GraphqlMutationRejectedInputTestCase] — verifies GraphQL `ValidationError` for the null field
     * - [GraphqlMutationValidBoundaryTestCase] — verifies fully successful execution with no errors
     */
    fun executeAndVerifyInputValidation(
        testCase: GraphqlMutationInputTestCase,
        path: String,
    ) {
        when (testCase) {
            is GraphqlMutationValidationErrorTestCase -> executeAndVerifyValidationError(
                violationPath = testCase.violationPath,
                error = testCase.error,
                message = testCase.message,
                path = path,
                params = testCase.params,
            )
            is GraphqlMutationRejectedInputTestCase -> {
                requestSpec
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .consumeWith { body ->
                        val json = Json.parseToJsonElement(body.responseBody!!).jsonObject
                        val errors = withClue("Expected errors in response for null ${testCase.fieldName} but got none: ${body.responseBody}") {
                            json["errors"]?.jsonArray.shouldNotBeNull()
                        }
                        errors.shouldNotBeEmpty()
                        val errorMessages = errors.map { it.jsonObject["message"]?.jsonPrimitive?.content.orEmpty() }
                        withClue("At least one error message should mention the field name '${testCase.fieldName}'") {
                            errorMessages.any { it.contains(testCase.fieldName, ignoreCase = true) }.shouldBeTrue()
                        }
                    }
            }
            is GraphqlMutationValidBoundaryTestCase -> {
                testCase.setup()
                requestSpec
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .consumeWith { body ->
                        val json = Json.parseToJsonElement(body.responseBody!!).jsonObject
                        withClue("Expected no errors at all but got: ${json["errors"]}") {
                            json.containsKey("errors").shouldBeFalse()
                        }
                    }
            }
        }
    }

    fun execute(): WebTestClient.ResponseSpec = requestSpec.exchange()
}
