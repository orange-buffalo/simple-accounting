package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.MOCK_DATE
import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

class ExpensesFilteringApiIT : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<Expense> {

                baseUrl = "expenses"

                entityMatcher {
                    responseFields("title", "notes", "category", "datePaid", "timeRecorded")
                    entityFields(
                        { expense -> expense.title },
                        { expense -> expense.notes },
                        { expense -> expense.category?.id },
                        { expense -> expense.datePaid },
                        { expense -> expense.timeRecorded }
                    )
                }

                defaultEntityProvider {
                    Prototypes.expense(
                        workspace = workspace
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                filtering {
                    entity {
                        configure { expense ->
                            expense.category = Prototypes.category(
                                workspace = workspace,
                                name = "name xxx"
                            ).let { save(it) }
                            expense.notes = "notes"
                            expense.title = "title"
                        }
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { expense ->
                            expense.category = Prototypes.category(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            expense.notes = "notesYYy"
                            expense.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        configure { expense ->
                            // no category
                            expense.notes = "notesYYy"
                            expense.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        configure { expense ->
                            expense.category = Prototypes.category(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            expense.notes = "notes"
                            expense.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { expense ->
                           // no category
                            expense.notes = "notes"
                            expense.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { expense ->
                            expense.category = Prototypes.category(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            expense.notes = "notes yyy notes"
                            expense.title = "xXx title"
                        }
                    }

                    entity {
                        configure { expense ->
                            // no category
                            expense.notes = "notes yyy notes"
                            expense.title = "xXx title"
                        }
                    }
                }

                sorting {
                    default {
                        goes { expense ->
                            expense.datePaid = MOCK_DATE.plusDays(1)
                            expense.timeRecorded = MOCK_TIME
                        }

                        goes { expense ->
                            expense.datePaid = MOCK_DATE
                            expense.timeRecorded = MOCK_TIME.minusMillis(1)
                        }

                        goes { expense ->
                            expense.datePaid = MOCK_DATE
                            expense.timeRecorded = MOCK_TIME
                        }

                        goes { expense ->
                            expense.datePaid = MOCK_DATE
                            expense.timeRecorded = MOCK_TIME.plusMillis(1)
                        }

                        goes { expense ->
                            expense.datePaid = MOCK_DATE.minusDays(1)
                            expense.timeRecorded = MOCK_TIME
                        }
                    }
                }
            }
    }
}
