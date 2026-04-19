package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.ExecutionResult
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class GraphQlOperationLoggingInstrumentation : Instrumentation {

    override fun beginExecution(
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState?,
    ): InstrumentationContext<ExecutionResult> {
        val operationName = parameters.operation ?: parameters.query?.take(80) ?: "unknown"
        logger.info { "GraphQL operation started: $operationName" }
        val startTime = System.currentTimeMillis()

        return object : InstrumentationContext<ExecutionResult> {
            override fun onDispatched() {}

            override fun onCompleted(result: ExecutionResult?, throwable: Throwable?) {
                val duration = System.currentTimeMillis() - startTime
                if (throwable != null) {
                    logger.info { "GraphQL operation failed: $operationName (${duration}ms)" }
                    logger.debug(throwable) { "GraphQL operation failure details: $operationName" }
                } else {
                    val errors = result?.errors
                    if (!errors.isNullOrEmpty()) {
                        logger.info { "GraphQL operation completed with errors: $operationName (${duration}ms, ${errors.size} error(s))" }
                        logger.debug { "GraphQL operation errors: ${errors.map { it.message }}" }
                    } else {
                        logger.info { "GraphQL operation completed: $operationName (${duration}ms)" }
                    }
                }
            }
        }
    }
}
