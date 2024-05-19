package io.orangebuffalo.simpleaccounting.domain.invoices

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
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
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(
                                    workspace = targetWorkspace,
                                    name = "name xxx"
                                ),
                                status = InvoiceStatus.DRAFT,
                                dueDate = MOCK_DATE.plusDays(100),
                                notes = "notes",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(
                                    workspace = targetWorkspace,
                                    name = "name"
                                ),
                                status = InvoiceStatus.DRAFT,
                                dueDate = MOCK_DATE.plusDays(100),
                                notes = "notesYYy",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(
                                    workspace = targetWorkspace,
                                    name = "name"
                                ),
                                status = InvoiceStatus.DRAFT,
                                dueDate = MOCK_DATE.plusDays(100),
                                notes = "notes",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(
                                    workspace = targetWorkspace,
                                    name = "name"
                                ),
                                status = InvoiceStatus.DRAFT,
                                dueDate = MOCK_DATE.plusDays(100),
                                notes = "notes yyy notes",
                                title = "xXx title"
                            )
                        }
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                status = InvoiceStatus.DRAFT,
                                dueDate = MOCK_DATE.plusDays(100),
                                title = "draft",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInSent)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                title = "cancelled",
                                status = InvoiceStatus.CANCELLED,
                                dueDate = MOCK_DATE.minusDays(100),
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInSent)
                        skippedOn(statusInPaid)
                        skippedOn(statusInDraft)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dueDate = MOCK_DATE.plusDays(100),
                                title = "paid",
                                status = InvoiceStatus.PAID,
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInSent)
                        skippedOn(statusInDraft)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dueDate = MOCK_DATE.plusDays(100),
                                title = "sent",
                                status = InvoiceStatus.SENT,
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                        skippedOn(statusInCancelled)
                        skippedOn(statusInPaid)
                        skippedOn(statusInDraft)
                        skippedOn(statusInOverdue)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dueDate = MOCK_DATE.plusDays(100),
                                title = "overdue",
                                status = InvoiceStatus.OVERDUE
                            )
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
                        goes {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dateIssued = MOCK_DATE.plusDays(1),
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dateIssued = MOCK_DATE,
                                timeRecorded = MOCK_TIME.minusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dateIssued = MOCK_DATE,
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dateIssued = MOCK_DATE,
                                timeRecorded = MOCK_TIME.plusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.invoice(
                                customer = entitiesFactory.customer(workspace = targetWorkspace),
                                dateIssued = MOCK_DATE.minusDays(1),
                                timeRecorded = MOCK_TIME,
                            )
                        }
                    }
                }
            }
    }
}
