package io.orangebuffalo.simpleaccounting.business.incomes

import io.orangebuffalo.simpleaccounting.tests.infra.api.legacy.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.tests.infra.api.legacy.generateFilteringApiTests
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME

class IncomesFilteringApiTest : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<Income> {

                baseUrl = "incomes"

                entityMatcher {
                    responseFields("title", "notes", "category", "dateReceived", "createdAt")
                    entityFields(
                        { income -> income.title },
                        { income -> income.notes },
                        { income -> income.categoryId },
                        { income -> income.dateReceived },
                        { income -> income.createdAt }
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                filtering {
                    entity {
                        createEntity {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name xxx"
                                ),
                                notes = "notes",
                                title = "title",
                            )
                        }
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name"
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
                            entitiesFactory.income(
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
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name"
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
                            entitiesFactory.income(
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
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                category = entitiesFactory.category(
                                    workspace = targetWorkspace,
                                    name = "name"
                                ),
                                notes = "notes yyy notes",
                                title = "xXx title",
                            )
                        }
                    }

                    entity {
                        createEntity {
                            // no category
                            entitiesFactory.income(
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
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE.plusDays(1),
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE,
                                createdAt = MOCK_TIME.minusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE,
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE,
                                createdAt = MOCK_TIME.plusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE.minusDays(1),
                            )
                        }
                    }
                }
            }
    }
}
