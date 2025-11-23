package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.schema.GraphQLFieldDefinition
import mu.KotlinLogging
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

private val log = KotlinLogging.logger { }

/**
 * Directive wiring for the @maxLength directive.
 * Validates that arguments annotated with @MaxLength do not exceed the specified length.
 */
class MaxLengthDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val directive = environment.directive
        check(directive.name == MAX_LENGTH_DIRECTIVE_NAME) {
            "MaxLengthDirectiveWiring can only be applied to the $MAX_LENGTH_DIRECTIVE_NAME directive"
        }
        
        val originalDataFetcher = environment.getDataFetcher()
        val fieldDefinition = environment.element
        val fieldName = fieldDefinition.name
        
        // Find the Kotlin function to access parameter annotations
        val kFunction = findKFunctionForField(fieldName)
        
        if (kFunction == null) {
            log.warn { "Could not find Kotlin function for field $fieldName, @MaxLength validation will be skipped" }
            return environment.element
        }
        
        environment.setDataFetcher { env ->
            log.trace { "Validating @MaxLength constraints for field ${fieldDefinition.name}" }
            
            val validationErrors = mutableListOf<FieldValidationError>()
            
            // Check each parameter for @MaxLength annotation
            kFunction.valueParameters.forEach { param ->
                val argName = param.name ?: return@forEach
                val maxLengthAnnotation = param.findAnnotation<MaxLength>()
                
                if (maxLengthAnnotation != null) {
                    val argValue = env.getArgument<String?>(argName)
                    if (argValue != null && argValue.length > maxLengthAnnotation.value) {
                        validationErrors.add(
                            FieldValidationError(
                                field = argName,
                                error = "SizeConstraintViolated",
                                message = "size must be between 0 and ${maxLengthAnnotation.value}",
                                params = mapOf("min" to "0", "max" to maxLengthAnnotation.value.toString())
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
