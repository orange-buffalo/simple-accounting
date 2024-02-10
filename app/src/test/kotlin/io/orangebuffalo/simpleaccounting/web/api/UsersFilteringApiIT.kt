package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
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

                defaultEntityProvider {
                    Prototypes.platformUser()
                }

                val freeSearchTextApiField = "freeSearchText"
                val freeSearchTextEqXXX = "$freeSearchTextApiField[eq]=XXX"
                val freeSearchTextEqYYY = "$freeSearchTextApiField[eq]=YYY"

                filtering {
                    entity {
                        configure { user ->
                            user.isAdmin = true
                            user.userName = "name@xxx.com"
                        }
                        skippedOn(freeSearchTextEqYYY)
                    }

                    entity {
                        configure { user ->
                            user.isAdmin = false
                            user.userName = "name@yyy.com"
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        configure { user ->
                            user.userName = "user-YyY"
                        }
                        skippedOn(freeSearchTextEqXXX)
                    }

                    entity {
                        configure { user ->
                            user.userName = "another user"
                        }
                        skippedOn(freeSearchTextEqXXX)
                        skippedOn(freeSearchTextEqYYY)
                    }
                }

                sorting {
                    default {
                        goes { user ->
                            user.userName = "aUser"
                            user.isAdmin = false
                        }

                        goes { user ->
                            user.userName = "bUser"
                            user.isAdmin = true
                        }

                        goes { user ->
                            user.userName = "cUser"
                            user.isAdmin = false
                        }
                    }
                }
            }
    }
}
