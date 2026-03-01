package io.orangebuffalo.simpleaccounting.business.incometaxpayments

import io.orangebuffalo.simpleaccounting.tests.infra.api.legacy.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.tests.infra.api.legacy.generateFilteringApiTests
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_DATE
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME

class IncomeTaxPaymentsFilteringApiTest : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<IncomeTaxPayment> {

                baseUrl = "income-tax-payments"

                entityMatcher {
                    responseFields("datePaid", "createdAt")
                    entityFields(
                        { incomeTaxPayment -> incomeTaxPayment.datePaid },
                        { incomeTaxPayment -> incomeTaxPayment.createdAt }
                    )
                }

                sorting {
                    default {
                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE.plusDays(1),
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                createdAt = MOCK_TIME.minusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE,
                                createdAt = MOCK_TIME.plusMillis(1),
                            )
                        }

                        goes {
                            entitiesFactory.incomeTaxPayment(
                                workspace = targetWorkspace,
                                datePaid = MOCK_DATE.minusDays(1),
                            )
                        }
                    }
                }
            }
    }
}
