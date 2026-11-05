package io.orangebuffalo.simpleaccounting.business.integrations.wise

import io.orangebuffalo.simpleaccounting.business.integrations.AccountStatementRecord
import io.orangebuffalo.simpleaccounting.business.integrations.StatementType
import io.orangebuffalo.simpleaccounting.business.integrations.IntegrationProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.io.InputStream
import java.net.http.HttpClient
import java.time.Duration
import java.time.Instant
import java.time.DateTimeException
import java.util.Currency

@Component
internal class WiseApiClient(
    properties: WiseProperties,
) {
    private val logger = KotlinLogging.logger {}
    private val client = RestClient.builder().baseUrl(properties.apiBaseUrl)
        .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build())
            .apply { setReadTimeout(Duration.ofSeconds(20)) })
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    fun getAccounts(token: String): List<WiseAccount> =
        get<List<WiseProfile>>(path = "/2026Q4/profiles", token = token).flatMap { profile ->
            get<List<WiseBalance>>(path = "/2026Q4/profiles/${profile.id}/balances?types=STANDARD", token = token)
                .filter { it.type == "STANDARD" }.map { balance ->
                    WiseAccount(
                        profileId = profile.id, profileName = profile.fullName, profileType = profile.type,
                        accountId = balance.id, currency = balance.currency, name = balance.name, type = balance.type,
                        amountInCents = balanceAmount(balance = balance, profileId = profile.id, token = token),
                    )
                }
        }

    fun getStatement(settings: WiseIntegrationSettings, account: WiseIntegrationAccount, start: Instant, end: Instant): List<AccountStatementRecord> {
        val path = "/2026Q4/profiles/${account.profileId}/balance-statements/${account.accountId}/statement.json" +
            "?currency=${account.currency}&intervalStart=$start&intervalEnd=$end&type=COMPACT"
        val statement = get<WiseStatement>(path = path, token = settings.token)
        try {
            return statement.transactions.map { transaction ->
                require(transaction.referenceNumber.isNotBlank())
                require(transaction.amount.currency == account.currency)
                val type = when {
                    transaction.details?.type == "CONVERSION" -> StatementType.CONVERSION
                    transaction.type == "DEBIT" -> StatementType.DEBIT
                    transaction.type == "CREDIT" -> StatementType.CREDIT
                    else -> StatementType.OTHER
                }
                val amount = transaction.amount.minorUnits()
                require(when (transaction.type) {
                    "DEBIT" -> amount <= 0
                    "CREDIT" -> amount >= 0
                    else -> true
                })
                val details = transaction.details
                val original = transaction.exchangeDetails?.forAmount ?: details?.amount
                AccountStatementRecord(
                    workspaceId = settings.workspaceId, provider = IntegrationProvider.WISE, integrationId = settings.id!!,
                    accountId = account.accountId.toString(), accountCurrency = account.currency,
                    // Wise references can be shared by a refund and across currency statements.
                    externalId = json.encodeToString(listOf(account.profileId.toString(), account.currency, transaction.type, transaction.referenceNumber)),
                    type = type, occurredAt = Instant.parse(transaction.date),
                    amountInCents = if (amount < 0) Math.negateExact(amount) else amount, currency = transaction.amount.currency,
                    feesInCents = transaction.totalFees?.minorUnits(), feesCurrency = transaction.totalFees?.currency,
                    runningBalanceInCents = transaction.runningBalance?.minorUnits(), runningBalanceCurrency = transaction.runningBalance?.currency,
                    additionalData = details?.additionalData(),
                    partyName = details?.partyName(),
                    title = if (type == StatementType.CONVERSION) details?.description?.takeIf(String::isNotBlank) else null,
                    originalAmountInCents = original?.minorUnits(), originalCurrency = original?.currency,
                    sourceAmountInCents = details?.sourceAmount?.minorUnits(), sourceCurrency = details?.sourceAmount?.currency,
                    targetAmountInCents = details?.targetAmount?.minorUnits(), targetCurrency = details?.targetAmount?.currency,
                    exchangeRate = (transaction.exchangeDetails?.rate ?: details?.rate)?.content?.toBigDecimal(),
                )
            }
        } catch (_: IllegalArgumentException) {
            logFailure(endpoint = path, token = settings.token, detail = "Invalid statement data")
            throw WiseUnavailableException()
        } catch (_: ArithmeticException) {
            logFailure(endpoint = path, token = settings.token, detail = "Unrepresentable statement amount")
            throw WiseUnavailableException()
        } catch (_: DateTimeException) {
            logFailure(endpoint = path, token = settings.token, detail = "Invalid statement date")
            throw WiseUnavailableException()
        }
    }

    private fun balanceAmount(balance: WiseBalance, profileId: Long, token: String): Long = try {
        balance.amount.value.content.toBigDecimal()
            .movePointRight(Currency.getInstance(balance.currency).defaultFractionDigits).longValueExact()
    } catch (_: IllegalArgumentException) {
        logFailure(endpoint = "/2026Q4/profiles/$profileId/balances?types=STANDARD", token = token, detail = "Invalid balance amount or currency")
        throw WiseUnavailableException()
    } catch (_: ArithmeticException) {
        logFailure(endpoint = "/2026Q4/profiles/$profileId/balances?types=STANDARD", token = token, detail = "Unrepresentable balance amount")
        throw WiseUnavailableException()
    }

    fun getStatementPdf(
        settings: WiseIntegrationSettings,
        record: AccountStatementRecord,
        consume: (fileName: String, content: InputStream) -> Unit,
    ) {
        val identity = try {
            json.parseToJsonElement(record.externalId).jsonArray.map { it.jsonPrimitive.content }
                .also { require(it.size == 4 && it[1] == record.accountCurrency) }
        } catch (_: IllegalArgumentException) {
            logFailure(endpoint = "statement.pdf", token = settings.token, detail = "Invalid stored Wise statement identity")
            throw WiseUnavailableException()
        }
        val profileId = identity[0].toLongOrNull() ?: throw WiseUnavailableException()
        val accountId = record.accountId.toLongOrNull() ?: throw WiseUnavailableException()
        val start = record.occurredAt.minus(Duration.ofHours(1))
        val end = record.occurredAt.plus(Duration.ofHours(1))
        val fileName = "Wise statement $profileId $accountId ${record.accountCurrency} ${record.occurredAt}.pdf"
        getStatementPdf(
            settings = settings, profileId = profileId, accountId = accountId, currency = record.accountCurrency,
            start = start, end = end,
        ) { consume(fileName, it) }
    }

    fun getStatementPdf(
        settings: WiseIntegrationSettings,
        profileId: Long,
        accountId: Long,
        currency: String,
        start: Instant,
        end: Instant,
        consume: (InputStream) -> Unit,
    ) {
        val path = "/v1/profiles/$profileId/balance-statements/$accountId/statement.pdf" +
            "?currency=$currency&intervalStart=$start&intervalEnd=$end&type=COMPACT"
        downloadPdf(path = path, token = settings.token, consume = consume)
    }

    private fun downloadPdf(path: String, token: String, consume: (InputStream) -> Unit) {
        request(path = path, token = token) { request ->
            request.exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful) {
                    failResponse(
                        endpoint = path, token = token, status = response.statusCode.value(),
                        headers = response.headers, body = response.body.readNBytes(4096).toString(Charsets.UTF_8),
                    )
                }
                consume(response.body)
            }
        }
    }

    private inline fun <reified T> get(path: String, token: String): T {
        var body: String? = null
        var status: Int? = null
        try {
            val response = request(path = path, token = token) { it.retrieve().toEntity(String::class.java) }
            status = response.statusCode.value()
            body = response.body
            if (body.isNullOrBlank()) {
                logFailure(endpoint = path, token = token, detail = "HTTP $status; empty response body")
                throw WiseUnavailableException()
            }
            return json.decodeFromString<T>(body)
        } catch (_: SerializationException) {
            logFailure(endpoint = path, token = token, detail = "HTTP $status; invalid response body: $body")
            throw WiseUnavailableException()
        }
    }

    private fun <T> request(path: String, token: String, read: (RestClient.RequestHeadersSpec<*>) -> T): T {
        var endpoint = path
        try {
            return read(client.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .httpRequest { request -> endpoint = request.uri.toString() })
        } catch (e: RestClientResponseException) {
            failResponse(endpoint = endpoint, token = token, status = e.statusCode.value(), headers = e.responseHeaders, body = e.responseBodyAsString)
        } catch (e: RestClientException) {
            val causes = generateSequence<Throwable>(e) { it.cause }.take(8)
                .joinToString("; ") { "${it.javaClass.simpleName}: ${it.message}" }
            logFailure(endpoint = endpoint, token = token, detail = "No usable response received; $causes")
            throw WiseUnavailableException()
        }
    }

    private fun failResponse(endpoint: String, token: String, status: Int, headers: HttpHeaders?, body: String): Nothing {
        val approvalResult = when (headers?.getFirst("x-2fa-approval-result")) {
            "APPROVED" -> "APPROVED"
            "REJECTED" -> "REJECTED"
            null -> "absent"
            else -> "unrecognized"
        }
        val challenge = headers?.getFirst("x-2fa-approval")
        val challengePresent = !challenge.isNullOrBlank()
        val responseBody = body.let {
            if (!challenge.isNullOrBlank()) it.replace(oldValue = challenge, newValue = "[REDACTED]") else it
        }.ifBlank { "<empty>" }
        logFailure(endpoint = endpoint, token = token, detail = "HTTP $status; scaApprovalResult=$approvalResult; " +
            "scaChallengePresent=$challengePresent; response body: $responseBody")
        if (status in listOf(401, 403)) throw WiseInvalidTokenException()
        throw WiseUnavailableException()
    }

    private fun logFailure(endpoint: String, token: String, detail: String) {
        val diagnostic = "Wise GET $endpoint failed: $detail"
            .replace(token, "[REDACTED]")
            .replace(Regex("[\\r\\n\\t]"), " ")
        logger.warn { diagnostic.take(4096) }
    }
}

@Serializable
private data class WiseProfile(val id: Long, val fullName: String, val type: String)

@Serializable
private data class WiseBalance(val id: Long, val currency: String, val name: String? = null, val type: String, val amount: WiseAmount)

@Serializable
private data class WiseAmount(val value: JsonPrimitive)

@Serializable
private data class WiseStatement(val transactions: List<WiseStatementTransaction>)

@Serializable
private data class WiseStatementAmount(val value: JsonPrimitive, val currency: String) {
    fun minorUnits(): Long = value.content.toBigDecimal()
        .movePointRight(Currency.getInstance(currency).defaultFractionDigits.also { require(it >= 0) }).longValueExact()
}

@Serializable
private data class WiseStatementTransaction(
    val type: String,
    val date: String,
    val amount: WiseStatementAmount,
    val referenceNumber: String,
    val totalFees: WiseStatementAmount? = null,
    val runningBalance: WiseStatementAmount? = null,
    val details: WiseStatementDetails? = null,
    val exchangeDetails: WiseExchangeDetails? = null,
)

@Serializable
private data class WiseStatementDetails(
    val type: String? = null,
    val description: String? = null,
    val amount: WiseStatementAmount? = null,
    val senderName: String? = null,
    val senderAccount: String? = null,
    val paymentReference: String? = null,
    val category: String? = null,
    val merchant: WiseMerchant? = null,
    val sourceAmount: WiseStatementAmount? = null,
    val targetAmount: WiseStatementAmount? = null,
    val rate: JsonPrimitive? = null,
) {
    fun partyName(): String? = senderName?.takeIf(String::isNotBlank)
        ?: merchant?.name?.takeIf(String::isNotBlank)

    fun additionalData(): String? = listOfNotNull(
        type?.let { "Type: $it" }, description?.let { "Description: $it" },
        paymentReference?.let { "Payment reference: $it" }, senderName?.let { "Sender: $it" },
        senderAccount?.let { "Sender account: $it" }, category?.let { "Category: $it" },
        merchant?.name?.let { "Merchant: $it" }, merchant?.category?.let { "Merchant category: $it" },
        merchant?.let { listOfNotNull(it.firstLine, it.city, it.state, it.postCode, it.country)
            .filter(String::isNotBlank).joinToString(", ").ifBlank { null } }?.let { "Merchant address: $it" },
    ).joinToString("\n").ifBlank { null }
}

@Serializable
private data class WiseMerchant(
    val name: String? = null,
    val firstLine: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postCode: String? = null,
    val country: String? = null,
    val category: String? = null,
)

@Serializable
private data class WiseExchangeDetails(val forAmount: WiseStatementAmount? = null, val rate: JsonPrimitive? = null)
