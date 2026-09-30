package io.orangebuffalo.simpleaccounting.business.api.workspaces

import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.infra.graphql.ValidationDirectiveMapping
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.util.Locale
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

private const val COUNTRY_CODE_MESSAGE = "must be a valid ISO 3166-1 alpha-2 country code"
private val countryCodes = Locale.getISOCountries().toSet()

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CountryCodeValidator::class])
annotation class CountryCode(
    val message: String = COUNTRY_CODE_MESSAGE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class CountryCodeValidator : ConstraintValidator<CountryCode, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean =
        value.isNullOrBlank() || value in countryCodes
}

@Component
class CountryCodeValidationDirective : ValidationDirectiveMapping(
    annotationClass = CountryCode::class,
    directiveName = "countryCode",
    directiveDescription = "Validates an ISO 3166-1 alpha-2 country code",
    errorCode = ValidationErrorCode.MustBeValidCountryCode,
    runtimeValidator = { path, value, _ ->
        if (value is String && value.isNotBlank() && value !in countryCodes) {
            ValidationErrorDetails(path, ValidationErrorCode.MustBeValidCountryCode, COUNTRY_CODE_MESSAGE)
        } else {
            null
        }
    },
)
