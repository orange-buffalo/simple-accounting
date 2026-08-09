package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import com.expediagroup.graphql.generator.extensions.get
import graphql.schema.GraphQLFieldDefinition
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlErrorType
import io.orangebuffalo.simpleaccounting.business.api.errors.SaGrapQlException
import io.orangebuffalo.simpleaccounting.business.security.SecurityPrincipal
import io.orangebuffalo.simpleaccounting.business.security.SaUserRoles
import io.orangebuffalo.simpleaccounting.business.security.runWithAuthentication
import io.orangebuffalo.simpleaccounting.infra.graphql.SUBSCRIPTION_AUTHENTICATION_KEY
import mu.KotlinLogging
import org.springframework.security.core.Authentication

private val log = KotlinLogging.logger { }

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
                val authentication = env.graphQlContext
                    .get<Authentication?>(SUBSCRIPTION_AUTHENTICATION_KEY)
                runWithAuthentication(authentication) {
                    originalDataFetcher.get(env)
                }
            }
        } else {
            environment.setDataFetcher { env ->
                log.trace { "This operation requires authenticated context, verifying" }
                val authentication = env.graphQlContext
                    .get<Authentication?>(SUBSCRIPTION_AUTHENTICATION_KEY)
                val principal = authentication?.principal as? SecurityPrincipal
                var authCheckSucceeded = true
                if (principal == null) {
                    authCheckSucceeded = false
                } else {
                    if (authType == RequiredAuth.AuthType.AUTHENTICATED_ACTOR) {
                        log.trace { "This operation requires authenticated actor, principal is found, continue with data fetching" }
                    } else if (authType == RequiredAuth.AuthType.AUTHENTICATED_USER) {
                        // any authenticated user is allowed
                        if (principal.isTransient) {
                            authCheckSucceeded = false
                            log.trace {
                                "This operation requires authenticated user, but principal is transient"
                            }
                        }
                    } else if (authType == RequiredAuth.AuthType.REGULAR_USER) {
                        // only regular users are allowed
                        if (principal.isTransient || !principal.roles.contains(SaUserRoles.USER)) {
                            authCheckSucceeded = false
                            log.trace { "This operation requires regular user, but principal is transient or has no USER role" }
                        }
                    } else if (authType == RequiredAuth.AuthType.ADMIN_USER) {
                        // only admin users are allowed
                        if (principal.isTransient || !principal.roles.contains(SaUserRoles.ADMIN)) {
                            authCheckSucceeded = false
                            log.trace { "This operation requires admin user, but principal is transient or has no ADMIN role" }
                        }
                    }
                }
                if (authCheckSucceeded) {
                    return@setDataFetcher runWithAuthentication(authentication) {
                        originalDataFetcher.get(env)
                    }
                }
                throw SaGrapQlException(
                    message = "User is not authenticated",
                    errorType = SaGrapQlErrorType.NOT_AUTHORIZED,
                )
            }
        }
        return environment.element
    }
}
