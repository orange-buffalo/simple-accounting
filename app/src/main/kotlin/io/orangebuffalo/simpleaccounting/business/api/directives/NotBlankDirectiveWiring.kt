package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.schema.GraphQLFieldDefinition
import mu.KotlinLogging
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

private val log = KotlinLogging.logger { }

/**
 * Directive wiring for the @notBlank directive.
 * Validates that arguments annotated with @NotBlank are not null, empty, or contain only whitespace.
 */
class NotBlankDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val directive = environment.directive
        check(directive.name == NOT_BLANK_DIRECTIVE_NAME) {
            "NotBlankDirectiveWiring can only be applied to the $NOT_BLANK_DIRECTIVE_NAME directive"
        }
        
        val originalDataFetcher = environment.getDataFetcher()
        val fieldDefinition = environment.element
        val fieldName = fieldDefinition.name
        
        // Find the Kotlin function to access parameter annotations
        val kFunction = findKFunctionForField(fieldName)
        
        if (kFunction == null) {
            log.warn { "Could not find Kotlin function for field $fieldName, @NotBlank validation will be skipped" }
            return environment.element
        }
        
        environment.setDataFetcher { env ->
            log.trace { "Validating @NotBlank constraints for field ${fieldDefinition.name}" }
            
            val validationErrors = mutableListOf<FieldValidationError>()
            
            // Check each parameter for @NotBlank annotation
            kFunction.valueParameters.forEach { param ->
                val argName = param.name ?: return@forEach
                val notBlankAnnotation = param.findAnnotation<NotBlank>()
                
                if (notBlankAnnotation != null) {
                    val argValue = env.getArgument<String?>(argName)
                    if (argValue.isNullOrBlank()) {
                        validationErrors.add(
                            FieldValidationError(
                                field = argName,
                                error = "MustNotBeBlank",
                                message = "must not be blank"
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
