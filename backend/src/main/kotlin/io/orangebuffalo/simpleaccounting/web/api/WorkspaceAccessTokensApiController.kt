package io.orangebuffalo.simpleaccounting.web.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceAccessTokenService
import io.orangebuffalo.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/workspace-access-tokens")
class WorkspaceAccessTokensApiController(
    private val accessTokenService: WorkspaceAccessTokenService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @GetMapping
    suspend fun getAccessTokens(@PathVariable workspaceId: Long): ApiPage<WorkspaceAccessTokenDto> {
        workspaceService.validateWorkspaceAccess(workspaceId, WorkspaceAccessMode.ADMIN)
        return filteringApiExecutor.executeFiltering(workspaceId)
    }

    @PostMapping
    suspend fun createToken(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createTokenRequest: CreateWorkspaceAccessTokenDto
    ): WorkspaceAccessTokenDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        return mapToDto(
            accessTokenService.createAccessToken(
                workspace,
                createTokenRequest.validTill
            )
        )
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder
        .executor<WorkspaceAccessToken, WorkspaceAccessTokenDto> {
            query(Tables.WORKSPACE_ACCESS_TOKEN) {
                addDefaultSorting { root.id.desc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }
            mapper { mapToDto(this) }
        }
}

private fun mapToDto(token: WorkspaceAccessToken) =
    WorkspaceAccessTokenDto(
        validTill = token.validTill,
        revoked = token.revoked,
        token = token.token,
        id = token.id!!,
        version = token.version!!
    )

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WorkspaceAccessTokenDto(
    val validTill: Instant,
    val revoked: Boolean,
    val token: String,
    val id: Long,
    val version: Int
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateWorkspaceAccessTokenDto(
    val validTill: Instant
)
