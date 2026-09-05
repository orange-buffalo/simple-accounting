package io.orangebuffalo.simpleaccounting.business.api.directives

import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.infra.graphql.ValidationDirectiveMapping
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import kotlin.reflect.KClass

private const val ENDPOINT_URL_MESSAGE = "must be an https URL, or an http URL of a loopback host"

/**
 * Requires the value to be an absolute URL this application is willing to talk to, or to send
 * the users to. Plain http is only tolerated for loopback hosts, so that local development and
 * tests can use it while deployments cannot leak client credentials over an unencrypted channel.
 */
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
    directiveDescription = "Validates that the value is an https URL, or an http URL of a loopback host",
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
    val host = uri.host ?: return false

    return when (uri.scheme?.lowercase()) {
        "https" -> true
        "http" -> isLoopbackHost(host)
        else -> false
    }
}

private fun isLoopbackHost(host: String): Boolean {
    if (host.equals("localhost", ignoreCase = true)) return true
    val literalAddress = host.removeSurrounding("[", "]")
    return try {
        // only IP literals are resolved here, so no DNS lookup is performed
        InetAddress.ofLiteral(literalAddress).isLoopbackAddress
    } catch (_: IllegalArgumentException) {
        false
    }
}
