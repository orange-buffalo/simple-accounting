package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DocumentsStorageStatusQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                documentsStorage = "noop",
            )
            val zoidberg = platformUser(
                userName = "Zoidberg",
                documentsStorage = null,
            )
            val fryWorkspace = workspace(owner = fry)
            val workspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Test
    fun `should return error when accessed anonymously`() {
        client
            .graphql { documentsStorageStatusQuery() }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.QUERY.DocumentsStorageStatus,
            )
    }

    @Test
    fun `should prohibit access with workspace token`() {
        client
            .graphql { documentsStorageStatusQuery() }
            .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.QUERY.DocumentsStorageStatus,
            )
    }

    @Test
    fun `should return active storage status when storage is configured`() {
        client
            .graphql { documentsStorageStatusQuery() }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.QUERY.DocumentsStorageStatus to buildJsonObject {
                    put("active", true)
                }
            )
    }

    @Test
    fun `should return inactive storage status when storage is not configured`() {
        client
            .graphql { documentsStorageStatusQuery() }
            .from(preconditions.zoidberg)
            .executeAndVerifySuccessResponse(
                DgsConstants.QUERY.DocumentsStorageStatus to buildJsonObject {
                    put("active", false)
                }
            )
    }

    private fun QueryProjection.documentsStorageStatusQuery(): QueryProjection =
        documentsStorageStatus {
            active
        }
}
