package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.GraphQLError
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters
import graphql.language.BooleanValue
import graphql.language.Field
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.OperationDefinition
import graphql.language.SelectionSet
import graphql.language.SourceLocation
import graphql.language.StringValue
import graphql.language.Value
import graphql.language.VariableReference
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.validation.ValidationError
import graphql.validation.ValidationErrorType
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import org.springframework.stereotype.Component

internal const val SA_VALIDATION_ERRORS_EXTENSION = "saValidationErrors"
internal const val SA_VALIDATION_QUERY_PATH_EXTENSION = "saValidationQueryPath"

/**
 * Performs Simple Accounting input validation before graphql-java coerces variables.
 *
 * graphql-java aborts variable coercion on the first null supplied for a non-null value. By validating raw
 * variables against the selected operation and schema during the validation phase, we can report all null fields
 * in a single response and stop execution before the fail-fast coercion path runs.
 */
@Component
class SaGraphQLInputValidationInstrumentation(
    validationDirectiveMappings: List<ValidationDirectiveMapping>,
) : SimplePerformantInstrumentation() {

    private val validationDirectiveMappingsByName = validationDirectiveMappings.associateBy { it.directiveName }

    override fun beginValidation(
        parameters: InstrumentationValidationParameters,
        state: InstrumentationState?,
    ): InstrumentationContext<List<ValidationError>> = object : InstrumentationContext<List<ValidationError>> {
        override fun onDispatched() = Unit

        override fun onCompleted(result: List<ValidationError>, t: Throwable?) {
            if (t != null) return

            val operation = findOperation(parameters) ?: return
            val rootType = when (operation.operation) {
                OperationDefinition.Operation.MUTATION -> parameters.schema.mutationType
                OperationDefinition.Operation.QUERY -> parameters.schema.queryType
                OperationDefinition.Operation.SUBSCRIPTION -> parameters.schema.subscriptionType
                else -> null
            } ?: return

            val validationErrors = validateSelectionSet(
                selectionSet = operation.selectionSet,
                parentType = rootType,
                variables = parameters.variables,
                variableLocations = operation.variableDefinitions.associate { it.name to it.sourceLocation },
            )
            if (validationErrors.isEmpty()) return

            @Suppress("UNCHECKED_CAST")
            (result as? MutableList<ValidationError>)?.add(buildValidationError(validationErrors))
        }
    }

    private fun findOperation(parameters: InstrumentationValidationParameters): OperationDefinition? {
        val operations = parameters.document.getDefinitionsOfType(OperationDefinition::class.java)
        val operationName = parameters.operation
        return if (operationName != null) {
            operations.firstOrNull { it.name == operationName }
        } else {
            operations.singleOrNull()
        }
    }

    private fun validateSelectionSet(
        selectionSet: SelectionSet,
        parentType: GraphQLObjectType,
        variables: Map<String, Any>,
        variableLocations: Map<String, SourceLocation>,
    ): List<PreCoercionValidationErrorDetails> = selectionSet
        .getSelectionsOfType(Field::class.java)
        .flatMap { field -> validateField(field, parentType, variables, variableLocations) }

    private fun validateField(
        field: Field,
        parentType: GraphQLFieldsContainer,
        variables: Map<String, Any>,
        variableLocations: Map<String, SourceLocation>,
    ): List<PreCoercionValidationErrorDetails> {
        val fieldDefinition = parentType.getFieldDefinition(field.name) ?: return emptyList()
        val providedArguments = field.arguments.associateBy { it.name }

        return fieldDefinition.arguments.flatMap { argumentDefinition ->
            val argument = providedArguments[argumentDefinition.name]
            val value = argument?.value
            val sourceLocation = when (value) {
                is VariableReference -> variableLocations[value.name] ?: value.sourceLocation
                else -> field.sourceLocation
            }
            val queryPath = listOf(field.name)

            val resolvedValue = resolveValue(value, variables)
            val directiveErrors = validateAppliedDirectives(argumentDefinition, resolvedValue, sourceLocation, queryPath)
            if (directiveErrors.isNotEmpty()) {
                directiveErrors
            } else if (argumentDefinition.isRequired() && resolvedValue == null) {
                listOf(
                    PreCoercionValidationErrorDetails(
                        validationError = ValidationErrorDetails(
                            path = argumentDefinition.name,
                            error = ValidationErrorCode.MustNotBeNull,
                            message = "must not be null",
                        ),
                        sourceLocation = sourceLocation,
                        queryPath = queryPath,
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    private fun resolveValue(value: Value<*>?, variables: Map<String, Any>): Any? = when (value) {
        null -> null
        is NullValue -> null
        is VariableReference -> variables[value.name]
        is StringValue -> value.value
        is BooleanValue -> value.isValue
        is IntValue -> value.value.toLong()
        is FloatValue -> value.value
        else -> null
    }

    private fun validateAppliedDirectives(
        argumentDefinition: GraphQLArgument,
        value: Any?,
        sourceLocation: SourceLocation,
        queryPath: List<String>,
    ): List<PreCoercionValidationErrorDetails> = argumentDefinition.appliedDirectives.mapNotNull { directive ->
        val mapping = validationDirectiveMappingsByName[directive.name] ?: return@mapNotNull null
        val validationError = mapping.runtimeValidator(argumentDefinition.name, value, directive) ?: return@mapNotNull null

        PreCoercionValidationErrorDetails(validationError, sourceLocation, queryPath)
    }

    private fun GraphQLArgument.isRequired(): Boolean = type is GraphQLNonNull && !hasSetDefaultValue()

    private fun buildValidationError(validationErrors: List<PreCoercionValidationErrorDetails>): ValidationError =
        ValidationError.newValidationError()
            .validationErrorType(ValidationErrorType.NullValueForNonNullArgument)
            .description("Validation failed")
            .sourceLocation(validationErrors.first().sourceLocation)
            .extensions(
                mapOf(
                    SA_VALIDATION_ERRORS_EXTENSION to validationErrors.map { it.validationError },
                    SA_VALIDATION_QUERY_PATH_EXTENSION to validationErrors.first().queryPath,
                )
            )
            .build()

    private data class PreCoercionValidationErrorDetails(
        val validationError: ValidationErrorDetails,
        val sourceLocation: SourceLocation,
        val queryPath: List<String>,
    )
}

internal fun GraphQLError.getSaValidationErrors(): List<ValidationErrorDetails> {
    val rawValidationErrors = extensions?.get(SA_VALIDATION_ERRORS_EXTENSION) ?: return emptyList()

    @Suppress("UNCHECKED_CAST")
    return rawValidationErrors as? List<ValidationErrorDetails> ?: emptyList()
}

internal fun GraphQLError.getSaValidationQueryPath(): List<String> {
    val rawQueryPath = extensions?.get(SA_VALIDATION_QUERY_PATH_EXTENSION) ?: return emptyList()

    @Suppress("UNCHECKED_CAST")
    return rawQueryPath as? List<String> ?: emptyList()
}
