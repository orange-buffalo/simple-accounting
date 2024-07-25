package io.orangebuffalo.simpleaccounting.business.expenses

import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class ExpensesFilteringApiTest : AbstractFilteringApiTest() {

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
                        { expense -> expense.categoryId },
                        { expense -> expense.datePaid },
                        { expense -> expense.timeRecorded }
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                filtering {
                    entity {
                        createEntity {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name xxx",
                                ),
                                notes = "notes",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name",
                                ),
                                notes = "notesYYy",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        createEntity {
                            // no category
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = null,
                                notes = "notesYYy",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name",
                                ),
                                notes = "notes",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        createEntity {
                            // no category
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = null,
                                notes = "notes",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name",
                                ),
                                notes = "notes yyy notes",
                                title = "xXx title",
                            )
                        }
                    }

                    entity {
                        createEntity {
                            // no category
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                category = null,
                                notes = "notes yyy notes",
                                title = "xXx title",
                            )
                        }
                    }
                }

                sorting {
                    default {
                        goes {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE.plusDays(1),
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                timeRecorded = MOCK_TIME.minusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                timeRecorded = MOCK_TIME.plusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.expense(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE.minusDays(1),
                                timeRecorded = MOCK_TIME,
                            )
                        }
                    }
                }
            }
    }
}
