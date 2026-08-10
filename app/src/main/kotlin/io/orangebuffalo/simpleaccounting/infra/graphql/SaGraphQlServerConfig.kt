package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import com.expediagroup.graphql.generator.extensions.print
import graphql.ErrorClassification
import graphql.ExecutionResultImpl
import graphql.GraphQLContext
import graphql.GraphQLError
import graphql.execution.instrumentation.Instrumentation
import graphql.schema.GraphQLSchema
import graphql.language.SourceLocation
import io.orangebuffalo.simpleaccounting.business.security.ProgrammaticAuthentication
import io.orangebuffalo.simpleaccounting.business.security.SpringSecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.jwt.JwtService
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.graphql.server.WebSocketGraphQlInterceptor
import org.springframework.graphql.server.WebSocketSessionInfo
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@PublishedApi
internal val APPLICATION_CONTEXT_KEY = ApplicationContext::class

internal const val SUBSCRIPTION_AUTHENTICATION_KEY = "sa-subscription-authentication"

@Configuration
class SaGraphQlServerConfig {

    @Bean
    fun graphQlSource(
        schema: GraphQLSchema,
        instrumentations: List<Instrumentation>,
        exceptionHandler: SaDataFetcherExceptionHandler,
    ): GraphQlSource = GraphQlSource.builder(schema)
        .instrumentation(instrumentations)
        .configureGraphQl { it.defaultDataFetcherExceptionHandler(exceptionHandler) }
        .build()

    @Bean
    fun executionGraphQlService(
        graphQlSource: GraphQlSource,
        dataLoaders: List<KotlinDataLoader<*, *>>,
    ): ExecutionGraphQlService = DefaultExecutionGraphQlService(graphQlSource).apply {
        addDataLoaderRegistrar { registry, graphQlContext ->
            dataLoaders.forEach { dataLoader ->
                registry.register(dataLoader.dataLoaderName, dataLoader.getDataLoader(graphQlContext))
            }
        }
    }

    @Bean
    fun saWebGraphQlInterceptor(
        applicationContext: ApplicationContext,
        jwtService: JwtService,
    ): WebGraphQlInterceptor = SaWebGraphQlInterceptor(applicationContext, jwtService)
}

private class SaWebGraphQlInterceptor(
    private val applicationContext: ApplicationContext,
    private val jwtService: JwtService,
) : WebSocketGraphQlInterceptor {

    override fun intercept(
        request: WebGraphQlRequest,
        chain: WebGraphQlInterceptor.Chain,
    ): Mono<WebGraphQlResponse> {
        val authentication = request.attributes[SUBSCRIPTION_AUTHENTICATION_KEY] as? Authentication
            ?: extractAuthentication(request.headers.getFirst(HttpHeaders.AUTHORIZATION)?.removeBearerPrefix())
        val httpRequestContext = GraphQlHttpRequestContext(
            refreshToken = request.cookies.getFirst("refreshToken")?.value,
        )

        request.configureExecutionInput { executionInput, builder ->
            builder.graphQLContext { contextBuilder ->
                contextBuilder.put(APPLICATION_CONTEXT_KEY, applicationContext)
                contextBuilder.put(GraphQlHttpRequestContext::class, httpRequestContext)
                authentication?.let {
                    contextBuilder.put(SUBSCRIPTION_AUTHENTICATION_KEY, it)
                }
            }.build()
        }
        return chain.next(request).map { response ->
            val errors = response.executionResult.errors.map(::GraphQlErrorWithoutClassification)
            val normalizedResponse = if (response.executionResult.getData<Any?>() == null && errors.isNotEmpty()) {
                val executionResult = ExecutionResultImpl.newExecutionResult()
                    .errors(errors)
                    .extensions(response.executionResult.extensions)
                    .build()
                WebGraphQlResponse(
                    DefaultExecutionGraphQlResponse(response.executionInput, executionResult)
                )
            } else {
                response.transform { builder -> builder.errors(errors) }
            }
            normalizedResponse.responseHeaders.putAll(response.responseHeaders)
            httpRequestContext.responseCookies.forEach { cookie ->
                normalizedResponse.responseHeaders.add(HttpHeaders.SET_COOKIE, cookie.toString())
            }
            normalizedResponse
        }
    }

    override fun handleConnectionInitialization(
        sessionInfo: WebSocketSessionInfo,
        connectionInitPayload: Map<String, Any>,
    ): Mono<Any> {
        extractAuthentication(connectionInitPayload["token"] as? String)?.let {
            sessionInfo.attributes[SUBSCRIPTION_AUTHENTICATION_KEY] = it
        }
        return Mono.empty()
    }

    private fun extractAuthentication(token: String?): Authentication? {
        if (token.isNullOrBlank()) return null
        return try {
            val userDetails = jwtService.validateTokenAndBuildUserDetails(token)
            if (userDetails is SpringSecurityPrincipal) ProgrammaticAuthentication(userDetails) else null
        } catch (e: BadCredentialsException) {
            logger.debug(e) { "Failed to validate JWT token from GraphQL request" }
            null
        }
    }

    private fun String.removeBearerPrefix(): String? =
        takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")?.trim()
}

private class GraphQlErrorWithoutClassification(
    private val delegate: GraphQLError,
) : GraphQLError {
    override fun getMessage(): String = delegate.message
    override fun getLocations(): List<SourceLocation>? = delegate.locations
    override fun getPath(): List<Any>? = delegate.path
    override fun getExtensions(): Map<String, Any>? = delegate.extensions
    override fun getErrorType(): ErrorClassification? = null
}

@RestController
class GraphQlSchemaController(
    private val schema: GraphQLSchema,
) {
    @GetMapping("/api/graphql/schema", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun schema(): String = schema.print()
}

inline fun <reified T : Any> GraphQLContext.getBean(): T =
    get<ApplicationContext>(APPLICATION_CONTEXT_KEY).getBean(T::class.java)
