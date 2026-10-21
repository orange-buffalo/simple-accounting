package io.orangebuffalo.simpleaccounting.business.integrations.wise

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

data class WiseIntegrationSaved(val integrationId: String)

@Component
class WiseStatementFetchTrigger(
    @param:Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    private val executor: AsyncTaskExecutor,
    private val job: WiseStatementFetchJob,
) {
    @TransactionalEventListener
    fun onIntegrationSaved(event: WiseIntegrationSaved) {
        executor.execute { job.fetchIntegration(event.integrationId) }
    }
}
