package io.orangebuffalo.simpleaccounting.business.api.errors

import graphql.ErrorClassification
import graphql.ErrorType

open class SaGrapQlException(
    message: String,
    val errorType: SaGrapQlErrorType,
    val errorClassification: ErrorClassification = ErrorType.ValidationError,
) : RuntimeException(message)
