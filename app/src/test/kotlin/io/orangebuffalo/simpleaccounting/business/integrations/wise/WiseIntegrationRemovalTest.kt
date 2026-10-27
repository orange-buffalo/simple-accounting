package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.WiseApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
@TestExecutionListeners(listeners = [ThirdPartyApisMocksListener::class], mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class WiseIntegrationRemovalTest(
    @Autowired private val service: WiseIntegrationService,
    @Autowired private val fetcher: WiseStatementFetchService,
) : SaIntegrationTestBase() {
    @Test
    fun `should remove statements produced by an in-flight fetch and ignore queued fetches after removal`() {
        val data = preconditions { object {
            val fry = fry()
            val integration = wiseIntegrationSettings(
                workspace = workspace(owner = fry),
                token = WiseApiMocks.TOKEN,
                accounts = setOf(WiseIntegrationAccount(profileId = 101, accountId = 301, currency = "USD")),
            )
        } }
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            lateinit var removal: Future<*>
            lateinit var fetch: Future<*>
            WiseApiMocks.blockStatementResponse(transactions = """[{
                "type":"DEBIT","date":"1999-03-27T12:00:00Z","referenceNumber":"CARD-1",
                "amount":{"value":-1.25,"currency":"USD"}
            }]""").use { blocked ->
                fetch = executor.submit { fetcher.fetch(data.integration.id!!) }
                blocked.shouldBePending()
                val removalStarted = CountDownLatch(1)
                removal = executor.submit {
                    removalStarted.countDown()
                    runAs(data.fry.toSecurityPrincipal()) { service.remove(data.integration.workspaceId) }
                }
                removalStarted.await(30, TimeUnit.SECONDS).shouldBe(true)
                shouldThrow<TimeoutException> { removal.get(250, TimeUnit.MILLISECONDS) }
            }
            fetch.get(30, TimeUnit.SECONDS)
            removal.get(30, TimeUnit.SECONDS)
        }
        fetcher.fetch(data.integration.id!!)
        aggregateTemplate.findAll<WiseIntegrationSettings>().shouldBe(emptyList())
        aggregateTemplate.findAll<AccountStatementRecord>().shouldBe(emptyList())
    }
}
