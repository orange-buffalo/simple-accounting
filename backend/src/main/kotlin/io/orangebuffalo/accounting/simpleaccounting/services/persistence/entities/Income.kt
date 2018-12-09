package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import java.time.Instant
import java.time.LocalDate
import javax.persistence.*

@Entity
class Income(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "income_category_fk"))
    var category: Category,

    @field:Column(nullable = false)
    var title: String,

    @field:Column(nullable = false)
    val timeRecorded: Instant,

    @field:Column(nullable = false)
    var dateReceived: LocalDate,

    @field:Column(nullable = false, length = 3)
    var currency: String,

    @field:Column(nullable = false)
    var originalAmount: Long,

    @field:Column(nullable = false)
    var amountInDefaultCurrency: Long,

    @field:Column(nullable = false)
    var reportedAmountInDefaultCurrency: Long,

    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "income_attachments",
        foreignKey = ForeignKey(name = "income_attachments_income_fk"),
        inverseForeignKey = ForeignKey(name = "income_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var attachments: Set<Document> = setOf(),

    @field:Column(length = 1024)
    var notes: String? = null

) : AbstractEntity()