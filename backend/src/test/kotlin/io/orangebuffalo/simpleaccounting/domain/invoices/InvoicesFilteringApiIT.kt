package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.MOCK_DATE
import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class InvoicesFilteringApiIT : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<Invoice> {

                baseUrl = "invoices"

                entityMatcher {
                    responseFields("title", "notes", "customer", "dateIssued", "timeRecorded")
                    entityFields(
                        { invoice -> invoice.title },
                        { invoice -> invoice.notes ?: "" },
                        { invoice -> invoice.customerId },
                        { invoice -> invoice.dateIssued },
                        { invoice -> invoice.timeRecorded }
                    )
                }

                defaultEntityProvider {
                    val customer = save(Prototypes.customer(workspace = workspace))
                    Prototypes.invoice(
                        customer = customer,
                        status = InvoiceStatus.DRAFT,
                        dueDate = MOCK_DATE.plusDays(100)
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                val statusApiField = "status"
                val statusInDraft = "$statusApiField[in]=DRAFT"
                val statusInCancelled = "$statusApiField[in]=CANCELLED"
                val statusInPaid = "$statusApiField[in]=PAID"
                val statusInSent = "$statusApiField[in]=SENT"
                val statusInOverdue = "$statusApiField[in]=OVERDUE"

                filtering {
                    entity {
                        configure { invoice ->
                            invoice.customerId = Prototypes.customer(
                                workspace = workspace,
                                name = "name xxx"
                            ).let { save(it).id!! }
                            invoice.notes = "notes"
                            invoice.title = "title"
                        }
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.customerId = Prototypes.customer(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it).id!! }
                            invoice.notes = "notesYYy"
                            invoice.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.customerId = Prototypes.customer(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it).id!! }
                            invoice.notes = "notes"
                            invoice.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.customerId = Prototypes.customer(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it).id!! }
                            invoice.notes = "notes yyy notes"
                            invoice.title = "xXx title"
                        }
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "draft"
                            invoice.status = InvoiceStatus.DRAFT
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "cancelled"
                            invoice.status = InvoiceStatus.CANCELLED
                            invoice.dueDate = MOCK_DATE.minusDays(100)
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInSent)
                        skippedOn(statusInPaid)
                        skippedOn(statusInDraft)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "paid"
                            invoice.status = InvoiceStatus.PAID
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInSent)
                        skippedOn(statusInDraft)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "sent"
                            invoice.status = InvoiceStatus.SENT
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInDraft)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "overdue"
                            invoice.status = InvoiceStatus.OVERDUE
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInDraft)
                        skippedOn(statusInSent)
                    }
                }

                sorting {
                    default {
                        goes { invoice ->
                            invoice.dateIssued = MOCK_DATE.plusDays(1)
                            invoice.timeRecorded = MOCK_TIME
                        }

                        goes { invoice ->
                            invoice.dateIssued = MOCK_DATE
                            invoice.timeRecorded = MOCK_TIME.minusMillis(1)
                        }

                        goes { invoice ->
                            invoice.dateIssued = MOCK_DATE
                            invoice.timeRecorded = MOCK_TIME
                        }

                        goes { invoice ->
                            invoice.dateIssued = MOCK_DATE
                            invoice.timeRecorded = MOCK_TIME.plusMillis(1)
                        }

                        goes { invoice ->
                            invoice.dateIssued = MOCK_DATE.minusDays(1)
                            invoice.timeRecorded = MOCK_TIME
                        }
                    }
                }
            }
    }
}
