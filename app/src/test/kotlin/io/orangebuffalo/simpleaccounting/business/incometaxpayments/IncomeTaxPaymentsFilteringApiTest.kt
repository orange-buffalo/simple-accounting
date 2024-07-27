package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.api.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.tests.infra.api.generateFilteringApiTests

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
