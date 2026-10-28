package io.orangebuffalo.simpleaccounting.business.api.accountstatements

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQuery
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.util.Base64

class AccountStatementsQueryTest(@Autowired private val client: ApiTestClient) : SaIntegrationTestBase() {
    private val data by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val integration = wiseIntegrationSettings(workspace = workspace)
        }
    }

    private fun query(arguments: String = "", selection: String = "externalId") = client.graphqlRawQuery(
        """{
            workspace(id: "${data.workspace.id}") {
            accountStatements(first: 10 $arguments) { edges { node { $selection } } totalCount }
        } }"""
    )

    private fun expected(vararg externalIds: String) = "workspace" to buildJsonObject {
        put("accountStatements", buildJsonObject {
            putJsonArray("edges") {
                externalIds.forEach { id -> add(buildJsonObject {
                    put("node", buildJsonObject { put("externalId", id) })
                }) }
            }
            put("totalCount", externalIds.size)
        })
    }

    @Nested
    inner class Authorization {
        @Test
        fun `should reject anonymous requests`() {
            query().fromAnonymous().executeAndVerifyNotAuthorized(path = "workspace", locationColumn = 13)
        }

        @Test
        fun `should not expose another users workspace`() {
            val zoidberg = preconditions { zoidberg() }
            query().from(zoidberg).executeAndVerifyEntityNotFoundError(path = "workspace", locationColumn = 13)
        }
    }

    @Nested
    inner class Transactions {
        @Test
        fun `should expose all transaction information without losing fractional exchange rates`() {
            val record = preconditions {
                accountStatementRecord(
                    integration = data.integration, accountId = "Mars-account", accountCurrency = "EUR",
                    externalId = "Slurm-conversion", partyName = "Fry", type = StatementType.CONVERSION,
                    amountInCents = 1200, currency = "USD", feesInCents = 0, feesCurrency = "USD",
                    runningBalanceInCents = -125, runningBalanceCurrency = "USD", additionalData = "Description: Slurm",
                    originalAmountInCents = 1000, originalCurrency = "EUR", sourceAmountInCents = 1000,
                    sourceCurrency = "EUR", targetAmountInCents = 1200, targetCurrency = "USD",
                    exchangeRate = BigDecimal("1.234567"),
                )
            }
            query(selection = """id provider integrationId accountId accountCurrency externalId type occurredAt
                amountInCents currency partyName feesInCents feesCurrency runningBalanceInCents runningBalanceCurrency
                additionalData originalAmountInCents originalCurrency sourceAmountInCents sourceCurrency
                targetAmountInCents targetCurrency exchangeRate createdAt""")
                .from(data.fry).executeAndVerifyResponse("workspace" to buildJsonObject {
                    put("accountStatements", buildJsonObject {
                        put("totalCount", 1)
                        putJsonArray("edges") { add(buildJsonObject { put("node", buildJsonObject {
                            put("id", record.id)
                            put("provider", "WISE")
                            put("integrationId", data.integration.id)
                            put("accountId", "Mars-account")
                            put("accountCurrency", "EUR")
                            put("externalId", "Slurm-conversion")
                            put("type", "CONVERSION")
                            put("occurredAt", MOCK_TIME.toString())
                            put("amountInCents", 1200)
                            put("currency", "USD")
                            put("partyName", "Fry")
                            put("feesInCents", 0)
                            put("feesCurrency", "USD")
                            put("runningBalanceInCents", -125)
                            put("runningBalanceCurrency", "USD")
                            put("additionalData", "Description: Slurm")
                            put("originalAmountInCents", 1000)
                            put("originalCurrency", "EUR")
                            put("sourceAmountInCents", 1000)
                            put("sourceCurrency", "EUR")
                            put("targetAmountInCents", 1200)
                            put("targetCurrency", "USD")
                            put("exchangeRate", "1.234567")
                            put("createdAt", record.createdAt.toString())
                        }) }) }
                    })
                })
        }

        @Test
        fun `should page tied timestamps deterministically and isolate workspaces`() {
            val records = preconditions {
                listOf(
                    accountStatementRecord(integration = data.integration, externalId = "Slurm", occurredAt = MOCK_TIME.plusNanos(123000)),
                    accountStatementRecord(integration = data.integration, externalId = "Robot-oil", occurredAt = MOCK_TIME.plusNanos(123000)),
                ).also { accountStatementRecord() }
            }.sortedByDescending { it.id }
            val cursor = Base64.getEncoder().encodeToString("${MOCK_TIME.toEpochMilli()}.123:${records[0].id}".toByteArray())
            client.graphqlRawQuery("""{ workspace(id: "${data.workspace.id}") {
                accountStatements(first: 1) { edges { cursor node { externalId } } totalCount pageInfo { hasNextPage } }
            } }""").from(data.fry).executeAndVerifyResponse("workspace" to buildJsonObject {
                put("accountStatements", buildJsonObject {
                    put("totalCount", 2)
                    put("pageInfo", buildJsonObject { put("hasNextPage", true) })
                    putJsonArray("edges") { add(buildJsonObject {
                        put("cursor", cursor)
                        put("node", buildJsonObject { put("externalId", records[0].externalId) })
                    }) }
                })
            })
            query(arguments = ", after: \"$cursor\"").from(data.fry)
                .executeAndVerifyResponse("workspace" to buildJsonObject {
                    put("accountStatements", buildJsonObject {
                        put("totalCount", 2)
                        putJsonArray("edges") { add(buildJsonObject {
                            put("node", buildJsonObject { put("externalId", records[1].externalId) })
                        }) }
                    })
                })
        }
    }

    @Nested
    inner class Filters {
        @Test
        fun `should search every text field case insensitively with literal matching`() {
            val record = preconditions { accountStatementRecord(
                integration = data.integration, accountId = "Mars-account", externalId = "Slurm_100%",
                accountCurrency = "EUR", currency = "USD", partyName = "Philip Fry", additionalData = "Reference: Planet Express",
                feesCurrency = "GBP", runningBalanceCurrency = "UAH", originalCurrency = "CAD",
                sourceCurrency = "AUD", targetCurrency = "JPY",
            ).also { accountStatementRecord() } }
            listOf(record.id!!, data.integration.id!!, "wise", "mars", "eur", "slurm_100%", "debit", "usd",
                "PHILIP", "gbp", "uah", "planet", "cad", "aud", "jpy").forEach { term ->
                query(arguments = ", freeSearchText: ${JsonPrimitive(term)}").from(data.fry)
                    .executeAndVerifyResponse(expected("Slurm_100%"))
            }
            query(arguments = ", freeSearchText: \"missing\"").from(data.fry).executeAndVerifyResponse(expected())
        }

        @Test
        fun `should combine search provider and timestamp bounds and ignore cleared filters`() {
            preconditions {
                accountStatementRecord(integration = data.integration, externalId = "Slurm-start", occurredAt = Instant.parse("1999-03-27T13:00:00Z"))
                accountStatementRecord(integration = data.integration, externalId = "Slurm-end", occurredAt = Instant.parse("1999-03-28T13:59:59.999999Z"))
                accountStatementRecord(integration = data.integration, externalId = "Slurm-before", occurredAt = Instant.parse("1999-03-27T12:59:59.999999Z"))
                accountStatementRecord(integration = data.integration, externalId = "Slurm-after", occurredAt = Instant.parse("1999-03-28T14:00:00Z"))
                accountStatementRecord(integration = data.integration, externalId = "Robot-oil")
            }
            query(arguments = """, freeSearchText: "slurm", providerIn: [WISE], occurredAtFrom: "1999-03-27T13:00:00Z", occurredAtBefore: "1999-03-28T14:00:00Z"""")
                .from(data.fry).executeAndVerifyResponse(expected("Slurm-end", "Slurm-start"))
            query(arguments = ", freeSearchText: \"  \", providerIn: [], occurredAtFrom: null, occurredAtBefore: null")
                .from(data.fry).executeAndVerifyResponse(expected("Robot-oil", "Slurm-after", "Slurm-end", "Slurm-start", "Slurm-before"))
        }
    }
}
