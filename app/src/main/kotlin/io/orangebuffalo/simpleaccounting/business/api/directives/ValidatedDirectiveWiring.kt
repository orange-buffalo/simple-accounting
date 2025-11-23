package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.schema.GraphQLFieldDefinition
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

/**
 * Directive wiring for the @validated directive.
 * This validates all arguments that have @notBlank or @maxLength directives.
 */
class ValidatedDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val directive = environment.directive
        check(directive.name == VALIDATED_DIRECTIVE_NAME) {
            "ValidatedDirectiveWiring can only be applied to the $VALIDATED_DIRECTIVE_NAME directive"
        }
        
        val originalDataFetcher = environment.getDataFetcher()
        val fieldDefinition = environment.element
        
        environment.setDataFetcher { env ->
            log.trace { "Validating arguments for field ${fieldDefinition.name}" }
            
            // Collect all validation errors for this field
            val validationErrors = mutableListOf<FieldValidationError>()
            
            // Check each argument for validation directives
            fieldDefinition.arguments.forEach { argDef ->
                val argValue = env.getArgument<String?>(argDef.name)
                
                // Check @notBlank directive
                val hasNotBlankDirective = argDef.directives.any { it.name == NOT_BLANK_DIRECTIVE_NAME }
                if (hasNotBlankDirective && argValue.isNullOrBlank()) {
                    validationErrors.add(
                        FieldValidationError(
                            field = argDef.name,
                            error = "MustNotBeBlank",
                            message = "must not be blank"
                        )
                    )
                }
                
                // Check @maxLength directive
                val maxLengthDirective = argDef.directives.find { it.name == MAX_LENGTH_DIRECTIVE_NAME }
                if (maxLengthDirective != null && argValue != null) {
                    val maxLength = maxLengthDirective.arguments[0].argumentValue.value as Int
                    if (argValue.length > maxLength) {
                        validationErrors.add(
                            FieldValidationError(
                                field = argDef.name,
                                error = "SizeConstraintViolated",
                                message = "size must be between 0 and $maxLength",
                                params = mapOf("min" to "0", "max" to maxLength.toString())
                            )
                        )
                    }
                }
            }
            
            if (validationErrors.isNotEmpty()) {
                throw FieldValidationException(
                    message = "Validation failed",
                    validationErrors = validationErrors
                )
            }
            
            originalDataFetcher.get(env)
        }
        
        return environment.element
    }
}
