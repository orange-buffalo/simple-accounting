package io.orangebuffalo.simpleaccounting.infra.graphql

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory

class GraphQlOperationLoggingInstrumentationTest {
    @Test
    fun `should log operation names without falling back to queries containing credentials`() {
        val logger = LoggerFactory.getLogger(GraphQlOperationLoggingInstrumentation::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            for (operation in listOf(null, "verifyWiseToken")) {
                appender.list.clear()
                val parameters = mock<InstrumentationExecutionParameters>()
                whenever(parameters.operation).thenReturn(operation)
                whenever(parameters.query).thenReturn("""mutation { verifyWiseIntegrationToken(token: "bender-secret-token", workspaceId: "planet") { error } }""")
                val context = GraphQlOperationLoggingInstrumentation().beginExecution(parameters, null)
                context.onCompleted(null, null)
                appender.list.map { it.formattedMessage.substringBefore(" (") }.shouldBe(listOf(
                    "GraphQL operation started: ${operation ?: "anonymous"}",
                    "GraphQL operation completed: ${operation ?: "anonymous"}",
                ))
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
