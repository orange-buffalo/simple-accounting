package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.MOCK_DATE
import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeTaxPayment
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class IncomeTaxPaymentsFilteringApiIT : AbstractFilteringApiTest() {

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

                defaultEntityProvider {
                    Prototypes.incomeTaxPayment(
                        workspace = workspace
                    )
                }

                sorting {
                    default {
                        goes { incomeTaxPayment ->
                            incomeTaxPayment.datePaid = MOCK_DATE.plusDays(1)
                            incomeTaxPayment.timeRecorded = MOCK_TIME
                        }

                        goes { incomeTaxPayment ->
                            incomeTaxPayment.datePaid = MOCK_DATE
                            incomeTaxPayment.timeRecorded = MOCK_TIME.minusMillis(1)
                        }

                        goes { incomeTaxPayment ->
                            incomeTaxPayment.datePaid = MOCK_DATE
                            incomeTaxPayment.timeRecorded = MOCK_TIME
                        }

                        goes { incomeTaxPayment ->
                            incomeTaxPayment.datePaid = MOCK_DATE
                            incomeTaxPayment.timeRecorded = MOCK_TIME.plusMillis(1)
                        }

                        goes { incomeTaxPayment ->
                            incomeTaxPayment.datePaid = MOCK_DATE.minusDays(1)
                            incomeTaxPayment.timeRecorded = MOCK_TIME
                        }
                    }
                }
            }
    }
}
