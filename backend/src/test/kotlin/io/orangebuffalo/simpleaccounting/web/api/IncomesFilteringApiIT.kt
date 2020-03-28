package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.MOCK_DATE
import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

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
                        { income -> income.category?.id },
                        { income -> income.dateReceived },
                        { income -> income.timeRecorded }
                    )
                }

                defaultEntityProvider {
                    Prototypes.income(
                        workspace = workspace
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                filtering {
                    entity {
                        configure { income ->
                            income.category = Prototypes.category(
                                workspace = workspace,
                                name = "name xxx"
                            ).let { save(it) }
                            income.notes = "notes"
                            income.title = "title"
                        }
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { income ->
                            income.category = Prototypes.category(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            income.notes = "notesYYy"
                            income.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        configure { income ->
                            // no category
                            income.notes = "notesYYy"
                            income.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        configure { income ->
                            income.category = Prototypes.category(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            income.notes = "notes"
                            income.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { income ->
                           // no category
                            income.notes = "notes"
                            income.title = "title"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { income ->
                            income.category = Prototypes.category(
                                workspace = workspace,
                                name = "name"
                            ).let { save(it) }
                            income.notes = "notes yyy notes"
                            income.title = "xXx title"
                        }
                    }

                    entity {
                        configure { income ->
                            // no category
                            income.notes = "notes yyy notes"
                            income.title = "xXx title"
                        }
                    }
                }

                sorting {
                    default {
                        goes { income ->
                            income.dateReceived = MOCK_DATE.plusDays(1)
                            income.timeRecorded = MOCK_TIME
                        }

                        goes { income ->
                            income.dateReceived = MOCK_DATE
                            income.timeRecorded = MOCK_TIME.minusMillis(1)
                        }

                        goes { income ->
                            income.dateReceived = MOCK_DATE
                            income.timeRecorded = MOCK_TIME
                        }

                        goes { income ->
                            income.dateReceived = MOCK_DATE
                            income.timeRecorded = MOCK_TIME.plusMillis(1)
                        }

                        goes { income ->
                            income.dateReceived = MOCK_DATE.minusDays(1)
                            income.timeRecorded = MOCK_TIME
                        }
                    }
                }
            }
    }
}
