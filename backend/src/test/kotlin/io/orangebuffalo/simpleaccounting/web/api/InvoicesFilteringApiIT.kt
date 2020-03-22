package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.MOCK_DATE
import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Invoice
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

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
                        { invoice -> invoice.customer.id },
                        { invoice -> invoice.dateIssued },
                        { invoice -> invoice.timeRecorded }
                    )
                }

                defaultEntityProvider {
                    val customer = save(Prototypes.customer(workspace = workspace))
                    Prototypes.invoice(
                        customer = customer,
                        dateCancelled = null,
                        datePaid = null,
                        dateSent = null,
                        dueDate = MOCK_DATE.plusDays(100)
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                val statusApiField = "status"
                val statusEqDraft = "$statusApiField[eq]=DRAFT"
                val statusEqCancelled = "$statusApiField[eq]=CANCELLED"
                val statusEqPaid = "$statusApiField[eq]=PAID"
                val statusEqSent = "$statusApiField[eq]=SENT"
                val statusEqOverdue = "$statusApiField[eq]=OVERDUE"

                filtering {
                    entity {
                        configure { invoice ->
                            invoice.customer = Prototypes.customer(
                                workspace = workspace,
                                name = "name xxx"
                            ).let { save(it) }
                            invoice.notes = "notes"
                            invoice.title = "title"
                        }
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.customer = Prototypes.customer(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            invoice.notes = "notesYYy"
                            invoice.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.customer = Prototypes.customer(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            invoice.notes = "notes"
                            invoice.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.customer = Prototypes.customer(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            invoice.notes = "notes yyy notes"
                            invoice.title = "xXx title"
                        }
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "draft"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "cancelled"
                            invoice.dateSent = MOCK_DATE
                            invoice.datePaid = MOCK_DATE
                            invoice.dueDate = MOCK_DATE.minusDays(100)
                            invoice.dateCancelled = MOCK_DATE
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqDraft)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "paid"
                            invoice.datePaid = MOCK_DATE
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqSent)
                        skippedOn(statusEqDraft)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "sent"
                            invoice.dateSent = MOCK_DATE
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqDraft)
                        skippedOn(statusEqOverdue)
                    }

                    entity {
                        configure { invoice ->
                            invoice.title = "overdue"
                            invoice.dateSent = MOCK_DATE
                            invoice.dueDate = MOCK_DATE.minusDays(1)
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusEqCancelled)
                        skippedOn(statusEqPaid)
                        skippedOn(statusEqDraft)
                        skippedOn(statusEqSent)
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
