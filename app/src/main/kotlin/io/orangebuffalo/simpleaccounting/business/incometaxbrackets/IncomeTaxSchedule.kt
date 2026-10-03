package io.orangebuffalo.simpleaccounting.business.incometaxbrackets

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Table("income_tax_schedule")
data class IncomeTaxSchedule(
    val countryCode: String,
    val jurisdiction: String,
    val taxpayer: String,
    val taxPeriodLabel: String,
    val periodStart: LocalDate,
    val periodEndExclusive: LocalDate,
    val currency: String,
    val basis: String,
    val limitations: String,
    @field:MappedCollection(idColumn = "schedule_id")
    val brackets: Set<IncomeTaxBracket>,
    @field:MappedCollection(idColumn = "schedule_id")
    val sources: Set<IncomeTaxScheduleSource>,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()

@Table("income_tax_bracket")
data class IncomeTaxBracket(
    val threshold: BigDecimal,
    val rate: BigDecimal,
)

@Table("income_tax_schedule_source")
data class IncomeTaxScheduleSource(
    val title: String,
    val url: String,
    val accessedOn: LocalDate,
)
