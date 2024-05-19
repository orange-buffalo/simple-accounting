package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.domain.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class UsersFilteringApiIT : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<PlatformUser> {

                baseUrl = "users"
                workspaceBasedUrl = false
                executeAsAdmin = true

                entityMatcher {
                    responseFields("userName", "admin")
                    entityFields(
                        { user -> user.userName },
                        { user -> user.isAdmin },
                    )
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                filtering {
                    entity {
                        createEntity {
                            entitiesFactory.platformUser(
                                isAdmin = true,
                                userName = "name@xxx.com",
                            )
                        }
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.platformUser(
                                isAdmin = false,
                                userName = "name@yyy.com",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.platformUser(
                                userName = "user-YyY",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.platformUser(
                                userName = "another user",
                            )
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }
                }

                sorting {
                    default {
                        goes {
                            entitiesFactory.platformUser(
                                userName = "aUser",
                                isAdmin = false,
                            )
                        }

                        goes {
                            entitiesFactory.platformUser(
                                userName = "bUser",
                                isAdmin = true,
                            )
                        }

                        goes {
                            entitiesFactory.platformUser(
                                userName = "cUser",
                                isAdmin = false,
                            )
                        }
                    }
                }
            }
    }
}
