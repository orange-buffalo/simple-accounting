package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class IncomeTaxPaymentsFilteringApiTest : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<IncomeTaxPayment> {

                baseUrl = "income-tax-payments"

                entityMatcher {
                    responseFields("datePaid", "timeRecorded")
                    entityFields(
                        { incomeTaxPayment -> incomeTaxPayment.datePaid },
                        { incomeTaxPayment -> incomeTaxPayment.timeRecorded }
                    )
                }

                sorting {
                    default {
                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE.plusDays(1),
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                timeRecorded = MOCK_TIME.minusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                timeRecorded = MOCK_TIME,
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                timeRecorded = MOCK_TIME.plusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
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
