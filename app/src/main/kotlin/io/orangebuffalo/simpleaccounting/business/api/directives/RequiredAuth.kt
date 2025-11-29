package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLDirective
import graphql.introspection.Introspection

const val REQUIRED_AUTH_DIRECTIVE_NAME = "auth"

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
            "Allows a request to be executed by any authenticated actor, " +
                    "including by workspace access token."
        )
        AUTHENTICATED_ACTOR,

        @GraphQLDescription(
            "Allows a request to be executed by any authenticated user, " +
                    "be it admin or regular user, but not via workspace access token."
        )
        AUTHENTICATED_USER,

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
