package io.orangebuffalo.simpleaccounting.infra.rest.errorhandling

class ApiValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
