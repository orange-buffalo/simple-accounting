package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.schema.GraphQLFieldDefinition
import io.orangebuffalo.simpleaccounting.business.api.ChangePasswordMutation
import mu.KotlinLogging
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

private val log = KotlinLogging.logger { }

/**
 * Directive wiring for the @validated directive.
 * This validates all arguments that have @notBlank or @maxLength annotations on the Kotlin function parameters.
 */
class ValidatedDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val directive = environment.directive
        check(directive.name == VALIDATED_DIRECTIVE_NAME) {
            "ValidatedDirectiveWiring can only be applied to the $VALIDATED_DIRECTIVE_NAME directive"
        }
        
        val originalDataFetcher = environment.getDataFetcher()
        val fieldDefinition = environment.element
        
        // Access Kotlin function using reflection on known mutation class
        // This is a workaround since the environment doesn't expose the KFunction directly
        val kFunction = ChangePasswordMutation::class.memberFunctions.find { it.name == fieldDefinition.name }
        
        println("DEBUG: KFunction found: ${kFunction?.name}, params: ${kFunction?.valueParameters?.map { it.name }}")
        
        environment.setDataFetcher { env ->
            log.trace { "Validating arguments for field ${fieldDefinition.name}" }
            
            // Collect all validation errors for this field
            val validationErrors = mutableListOf<FieldValidationError>()
            
            // Check each argument against Kotlin annotations
            kFunction?.valueParameters?.forEach { param ->
                val argName = param.name ?: return@forEach
                val argValue = env.getArgument<String?>(argName)
                
                println("DEBUG: Checking param=${param.name}, value='$argValue', annotations=${param.annotations.map { it.annotationClass.simpleName }}")
                
                // Check @NotBlank annotation
                val notBlankAnnotation = param.findAnnotation<NotBlank>()
                if (notBlankAnnotation != null) {
                    println("DEBUG: Found @NotBlank on $argName, isNullOrBlank=${argValue.isNullOrBlank()}")
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
                
                // Check @MaxLength annotation
                val maxLengthAnnotation = param.findAnnotation<MaxLength>()
                if (maxLengthAnnotation != null && argValue != null && argValue.length > maxLengthAnnotation.value) {
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
            
            println("DEBUG: Total validation errors: ${validationErrors.size}")
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
