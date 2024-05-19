package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class IncomesFilteringApiIT : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<Income> {

                baseUrl = "incomes"

                entityMatcher {
                    responseFields("title", "notes", "category", "dateReceived", "timeRecorded")
                    entityFields(
                        { income -> income.title },
                        { income -> income.notes },
                        { income -> income.categoryId },
                        { income -> income.dateReceived },
                        { income -> income.timeRecorded }
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
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE,
                                timeRecorded = MOCK_TIME.minusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE,
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE,
                                timeRecorded = MOCK_TIME.plusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.income(
                                workspace = targetWorkspace,
                                dateReceived = MOCK_DATE.minusDays(1),
                                timeRecorded = MOCK_TIME,
                            )
                        }
                    }
                }
            }
    }
}
