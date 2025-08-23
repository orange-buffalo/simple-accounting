package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.extensions.get
import graphql.introspection.Introspection
import graphql.schema.GraphQLFieldDefinition
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipalOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.coroutines.EmptyCoroutineContext

const val REQUIRED_AUTH_DIRECTIVE_NAME = "auth"
private val log = KotlinLogging.logger { }

@GraphQLDirective(
    name = REQUIRED_AUTH_DIRECTIVE_NAME,
    description = "Defines the authorization requirements for the request. " +
            "If a request context does not satisfy the requirements, " +
            "it will be rejected with an error code `NOT_AUTHORIZED`.",
    locations = [
        Introspection.DirectiveLocation.FIELD_DEFINITION,
    ]
)
annotation class RequiredAuth(
    @param:GraphQLDescription(
        "The required authorization to execute the request."
    )
    val type: AuthType,
) {

    @GraphQLDescription(
        "Defines the type of authorization required to execute the request. " +
                "This is used in conjunction with the `@auth` directive."
    )
    enum class AuthType {
        @GraphQLDescription(
            "Allows a request to be executed by an anonymous user, " +
                    "i.e. not authenticated at all. With this restriction, any " +
                    "authenticated user is allowed to execute the request too. "
        )
        ANONYMOUS,

        @GraphQLDescription(
            "Allows a request to be executed by any authenticated user."
        )
        AUTHENTICATED,

        @GraphQLDescription(
            "Requires a request to be executed by a regular user, " +
                    "i.e. authenticated and not an admin user."
        )
        REGULAR_USER,

        @GraphQLDescription(
            "Requires a request to be executed by an admin user, " +
                    "i.e. authenticated and has admin privileges."
        )
        ADMIN_USER,
    }
}

class RequiredAuthDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val directive = environment.directive
        check(directive.name == REQUIRED_AUTH_DIRECTIVE_NAME) {
            "RequiredAuthDirectiveWiring can only be applied to the $REQUIRED_AUTH_DIRECTIVE_NAME directive"
        }
        val authType = directive.arguments[0].argumentValue.value as RequiredAuth.AuthType
        val originalDataFetcher = environment.getDataFetcher()
        if (authType == RequiredAuth.AuthType.ANONYMOUS) {
            environment.setDataFetcher { env ->
                log.trace { "This operation supports anonymous access, not checking the authorization" }
                originalDataFetcher.get(env)
            }
        } else {
            environment.setDataFetcher { env ->
                log.trace { "This operation requires authenticated context, verifying" }
                // coroutine context is required to get the security context
                val coroutineScope = env.graphQlContext.get<CoroutineScope>()
                    ?: CoroutineScope(EmptyCoroutineContext)
                val principal = runBlocking(coroutineScope.coroutineContext) {
                    getCurrentPrincipalOrNull()
                }
                if (principal == null) {
                    throw SaGrapQlException(
                        message = "User is not authenticated",
                        errorType = SaGrapQlErrorType.NOT_AUTHORIZED,
                    )
                }
                originalDataFetcher.get(env)
            }
        }
        return environment.element
    }
}
