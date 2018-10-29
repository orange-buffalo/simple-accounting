package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import java.time.ZonedDateTime
import javax.persistence.*

@Entity
class Expense(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "expense_category_fk"))
    var category: Category,

    @field:Column(nullable = false)
    val dateRecorded: ZonedDateTime,

    @field:Column(nullable = false)
    var datePaid: ZonedDateTime,

    @field:Column(nullable = false, length = 3)
    var currency: String,

    @field:Column(nullable = false)
    var originalAmount: Long,

    @field:Column(nullable = false)
    var amountInDefaultCurrency: Long,

    @field:Column(nullable = false)
    var actualAmountInDefaultCurrency: Long,

    @field:ManyToMany(fetch = FetchType.EAGER)
    @field:JoinTable(
        name = "expense_attachments",
        foreignKey = ForeignKey(name = "expense_attachments_expense_fk"),
        inverseForeignKey = ForeignKey(name = "expense_attachments_document_fk"),
        inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    val attachments: MutableList<Document> = ArrayList(),

    @field:Column(nullable = false)
    var percentOnBusinessInBps: Int,

    @field:Column(length = 1024)
    var notes: String? = null

) : AbstractEntity()