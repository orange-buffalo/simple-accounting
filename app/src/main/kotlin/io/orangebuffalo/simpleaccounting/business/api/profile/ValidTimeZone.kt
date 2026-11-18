package io.orangebuffalo.simpleaccounting.business.api.profile

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import io.orangebuffalo.simpleaccounting.business.users.isSupportedTimeZone
import kotlin.reflect.KClass
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.infra.graphql.ValidationDirectiveMapping
import org.springframework.stereotype.Component

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TimeZoneValidator::class])
annotation class ValidTimeZone(
    val message: String = "must be a supported IANA time zone",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class TimeZoneValidator : ConstraintValidator<ValidTimeZone, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value == null || isSupportedTimeZone(value)
}

@Component
class TimeZoneValidationDirective : ValidationDirectiveMapping(
    annotationClass = ValidTimeZone::class,
    directiveName = "timeZone",
    directiveDescription = "Validates a supported IANA time zone identifier",
    errorCode = ValidationErrorCode.MustBeValidTimeZone,
    runtimeValidator = { path, value, _ ->
        if (value is String && !isSupportedTimeZone(value)) {
            ValidationErrorDetails(
                path = path,
                error = ValidationErrorCode.MustBeValidTimeZone,
                message = "must be a supported IANA time zone",
            )
        } else null
    },
)
