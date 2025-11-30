package io.orangebuffalo.simpleaccounting.business.api

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.errors.BusinessErrorCode

/**
 * Error codes for the changePassword mutation.
 */
@GraphQLDescription("Possible business error codes for the changePassword operation.")
enum class ChangePasswordErrorCode : BusinessErrorCode {
    @GraphQLDescription("The provided current password does not match the user's actual password.")
    CurrentPasswordMismatch,

    @GraphQLDescription("Cannot change password for a transient user (e.g., shared workspace token user).")
    TransientUser,
}
