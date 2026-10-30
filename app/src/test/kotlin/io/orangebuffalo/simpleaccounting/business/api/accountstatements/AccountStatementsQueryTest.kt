package io.orangebuffalo.simpleaccounting.business.api.accountstatements

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlRawQuery
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
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

        @Test
        fun `should reject administrators`() {
            val farnsworth = preconditions { farnsworth() }
            query().from(farnsworth).executeAndVerifyEntityNotFoundError(path = "workspace", locationColumn = 13)
        }

        @Test
        fun `should allow the workspace owner`() {
            preconditions { accountStatementRecord(integration = data.integration, externalId = "Slurm") }
            query().from(data.fry).executeAndVerifyResponse(expected("Slurm"))
        }

        @Test
        fun `should allow a token for the requested workspace`() {
            val token = preconditions {
                accountStatementRecord(integration = data.integration, externalId = "Slurm")
                workspaceAccessToken(workspace = data.workspace, validTill = MOCK_TIME.plusSeconds(10000))
            }
            query().usingSharedWorkspaceToken(token.token).executeAndVerifyResponse(expected("Slurm"))
        }

        @Test
        fun `should not allow a token for another workspace`() {
            val token = preconditions { workspaceAccessToken(validTill = MOCK_TIME.plusSeconds(10000)) }
            query().usingSharedWorkspaceToken(token.token)
                .executeAndVerifyEntityNotFoundError(path = "workspace", locationColumn = 13)
        }

        @Test
        fun `should allow a user with a saved shared workspace`() {
            val zoidberg = preconditions {
                accountStatementRecord(integration = data.integration, externalId = "Slurm")
                zoidberg().also { user ->
                    savedWorkspaceAccessToken(
                        owner = user,
                        workspaceAccessToken = workspaceAccessToken(
                            workspace = data.workspace,
                            validTill = MOCK_TIME.plusSeconds(10000),
                        ),
                    )
                }
            }
            query().from(zoidberg).executeAndVerifyResponse(expected("Slurm"))
        }

        @ParameterizedTest(name = "should reject {0} workspace token")
        @CsvSource("expired,-1,false", "revoked,10000,true")
        fun `should reject invalid workspace tokens`(state: String, validForSeconds: Long, revoked: Boolean) {
            val token = preconditions {
                workspaceAccessToken(
                    workspace = data.workspace,
                    token = "Slurm-$state",
                    validTill = MOCK_TIME.plusSeconds(validForSeconds),
                    revoked = revoked,
                )
            }
            query().usingSharedWorkspaceToken(token.token)
                .executeAndVerifyEntityNotFoundError(path = "workspace", locationColumn = 13)
        }
    }

    @Nested
    inner class Transactions {
        @ParameterizedTest
        @ValueSource(strings = ["owner", "workspace-token", "saved-share"])
        fun `should expose the linked invoice to authorized workspace readers`(actor: String) {
            val setup = preconditions {
                object {
                    val invoice = invoice(customer = customer(workspace = data.workspace), invoiceNumber = "PE-3025-42")
                    val token = workspaceAccessToken(workspace = data.workspace, validTill = MOCK_TIME.plusSeconds(10000))
                    val zoidberg = zoidberg().also { savedWorkspaceAccessToken(owner = it, workspaceAccessToken = token) }
                    init {
                        accountStatementRecord(integration = data.integration, linkedInvoice = invoice)
                    }
                }
            }
            val request = query(selection = "linkedInvoice { id invoiceNumber }")
            when (actor) {
                "workspace-token" -> request.usingSharedWorkspaceToken(setup.token.token)
                "saved-share" -> request.from(setup.zoidberg)
                else -> request.from(data.fry)
            }.executeAndVerifyResponse("workspace" to buildJsonObject {
                put("accountStatements", buildJsonObject {
                    put("totalCount", 1)
                    putJsonArray("edges") { add(buildJsonObject { put("node", buildJsonObject {
                        put("linkedInvoice", buildJsonObject {
                            put("id", setup.invoice.id)
                            put("invoiceNumber", "PE-3025-42")
                        })
                    }) }) }
                })
            })
        }

        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `should not expose absent or cross-workspace invoice links`(crossWorkspace: Boolean) {
            preconditions {
                accountStatementRecord(integration = data.integration, linkedInvoice = if (crossWorkspace) invoice() else null)
            }
            query(selection = "linkedInvoice { id invoiceNumber }").from(data.fry)
                .executeAndVerifyResponse("workspace" to buildJsonObject {
                    put("accountStatements", buildJsonObject {
                        put("totalCount", 1)
                        putJsonArray("edges") { add(buildJsonObject { put("node", buildJsonObject {
                            put("linkedInvoice", JsonNull)
                        }) }) }
                    })
                })
        }

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
        fun `should page tied timestamps deterministically`() {
            val records = preconditions {
                listOf(
                    accountStatementRecord(integration = data.integration, externalId = "Slurm", occurredAt = MOCK_TIME.plusNanos(123000)),
                    accountStatementRecord(integration = data.integration, externalId = "Robot-oil", occurredAt = MOCK_TIME.plusNanos(123000)),
                )
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

        @Test
        fun `should exclude records from other workspaces`() {
            preconditions {
                accountStatementRecord(integration = data.integration, externalId = "Slurm")
                accountStatementRecord(externalId = "Robot-oil")
            }
            query().from(data.fry).executeAndVerifyResponse(expected("Slurm"))
        }
    }

    @Nested
    inner class Filters {
        @Nested
        inner class FreeSearch {
            @ParameterizedTest(name = "should search {0} case insensitively")
            @CsvSource(
                "accountId,mArS", "externalId,sLuRm", "type,dEbIt",
                "partyName,pHiLiP", "additionalData,pLaNeT",
            )
            fun `should filter by every supported text field`(field: String, text: String) {
                preconditions {
                    accountStatementRecord(
                        integration = data.integration,
                        accountId = "Mars-account",
                        externalId = "Slurm",
                        type = StatementType.DEBIT,
                        partyName = "Philip Fry",
                        additionalData = "Reference: Planet Express",
                    ).also {
                        accountStatementRecord(
                            integration = data.integration,
                            externalId = "Robot-oil",
                            accountId = "Moon-account",
                            type = StatementType.CREDIT,
                            occurredAt = MOCK_TIME.minusSeconds(1),
                        )
                    }
                }
                query(arguments = ", freeSearchText: ${JsonPrimitive(text)}").from(data.fry)
                    .executeAndVerifyResponse(expected("Slurm"))
            }

            @ParameterizedTest
            @ValueSource(strings = ["_", "%", "\\"])
            fun `should match SQL wildcard and escape characters literally`(text: String) {
                preconditions {
                    accountStatementRecord(integration = data.integration, externalId = "Slurm${text}supplies")
                    accountStatementRecord(integration = data.integration, externalId = "SlurmXsupplies")
                }
                query(arguments = ", freeSearchText: ${JsonPrimitive(text)}").from(data.fry)
                    .executeAndVerifyResponse(expected("Slurm${text}supplies"))
            }

            @Test
            fun `should return no records when text does not match`() {
                preconditions { accountStatementRecord(integration = data.integration, externalId = "Slurm") }
                query(arguments = ", freeSearchText: \"Omicron\"").from(data.fry).executeAndVerifyResponse(expected())
            }

            @ParameterizedTest
            @NullSource
            @ValueSource(strings = ["", "  "])
            fun `should not restrict records for null empty or blank search text`(text: String?) {
                preconditions { accountStatementRecord(integration = data.integration, externalId = "Slurm") }
                query(arguments = ", freeSearchText: ${text?.let { JsonPrimitive(it) } ?: JsonNull}")
                    .from(data.fry).executeAndVerifyResponse(expected("Slurm"))
            }
        }

        @Nested
        inner class Provider {
            @Test
            fun `should return records for the selected provider`() {
                preconditions { accountStatementRecord(integration = data.integration, externalId = "Slurm") }
                query(arguments = ", providerIn: [WISE]").from(data.fry).executeAndVerifyResponse(expected("Slurm"))
            }

            @ParameterizedTest
            @ValueSource(strings = ["null", "[]"])
            fun `should not restrict records for null or empty provider list`(value: String) {
                preconditions { accountStatementRecord(integration = data.integration, externalId = "Slurm") }
                query(arguments = ", providerIn: $value").from(data.fry).executeAndVerifyResponse(expected("Slurm"))
            }
        }

        @Nested
        inner class TimestampBounds {
            private fun createRecords() = preconditions {
                accountStatementRecord(integration = data.integration, externalId = "Slurm-before", occurredAt = Instant.parse("1999-03-27T12:59:59.999999Z"))
                accountStatementRecord(integration = data.integration, externalId = "Slurm-start", occurredAt = Instant.parse("1999-03-27T13:00:00Z"))
                accountStatementRecord(integration = data.integration, externalId = "Slurm-end", occurredAt = Instant.parse("1999-03-28T13:59:59.999999Z"))
                accountStatementRecord(integration = data.integration, externalId = "Slurm-after", occurredAt = Instant.parse("1999-03-28T14:00:00Z"))
            }

            @Test
            fun `should include the lower bound when only from is provided`() {
                createRecords()
                query(arguments = ", occurredAtFrom: \"1999-03-27T13:00:00Z\"").from(data.fry)
                    .executeAndVerifyResponse(expected("Slurm-after", "Slurm-end", "Slurm-start"))
            }

            @Test
            fun `should exclude the upper bound when only before is provided`() {
                createRecords()
                query(arguments = ", occurredAtBefore: \"1999-03-28T14:00:00Z\"").from(data.fry)
                    .executeAndVerifyResponse(expected("Slurm-end", "Slurm-start", "Slurm-before"))
            }

            @Test
            fun `should apply both timestamp bounds`() {
                createRecords()
                query(arguments = """, occurredAtFrom: "1999-03-27T13:00:00Z", occurredAtBefore: "1999-03-28T14:00:00Z"""")
                    .from(data.fry).executeAndVerifyResponse(expected("Slurm-end", "Slurm-start"))
            }

            @Test
            fun `should not restrict records for explicitly null timestamp bounds`() {
                createRecords()
                query(arguments = ", occurredAtFrom: null, occurredAtBefore: null").from(data.fry)
                    .executeAndVerifyResponse(expected("Slurm-after", "Slurm-end", "Slurm-start", "Slurm-before"))
            }
        }
    }
}
