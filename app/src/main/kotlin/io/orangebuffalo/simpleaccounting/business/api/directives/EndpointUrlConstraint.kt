package io.orangebuffalo.simpleaccounting.business.api.directives

import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.infra.graphql.ValidationDirectiveMapping
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.stereotype.Component
import java.net.URI
import kotlin.reflect.KClass

private const val ENDPOINT_URL_MESSAGE = "must be a valid http or https URL"

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EndpointUrlValidator::class])
annotation class EndpointUrl(
    val message: String = ENDPOINT_URL_MESSAGE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class EndpointUrlValidator : ConstraintValidator<EndpointUrl, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean = isValidEndpointUrl(value)
}

@Component
class EndpointUrlValidationDirective : ValidationDirectiveMapping(
    annotationClass = EndpointUrl::class,
    directiveName = "endpointUrl",
    directiveDescription = "Validates that the value is a well-formed http or https URL",
    errorCode = ValidationErrorCode.MustBeValidEndpointUrl,
    runtimeValidator = { path, value, _ ->
        val stringValue = value as? String
        if (stringValue != null && !isValidEndpointUrl(stringValue)) {
            ValidationErrorDetails(
                path = path,
                error = ValidationErrorCode.MustBeValidEndpointUrl,
                message = ENDPOINT_URL_MESSAGE,
            )
        } else {
            null
        }
    }
)

/**
 * Blank values are considered valid here: the presence of a value is a separate concern,
 * reported by the `notBlank` constraint.
 */
private fun isValidEndpointUrl(value: String?): Boolean {
    if (value == null || value.isBlank()) return true

    val uri = try {
        URI(value)
    } catch (_: java.net.URISyntaxException) {
        return false
    }

    if (!uri.isAbsolute) return false
    return uri.host != null && uri.scheme.lowercase() in setOf("http", "https")
}
