package io.orangebuffalo.simpleaccounting.business.workspaces

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilderLegacy
import org.springframework.web.bind.annotation.*
import java.time.Instant
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/workspace-access-tokens")
class WorkspaceAccessTokensApiController(
    private val accessTokenService: WorkspaceAccessTokenService,
    private val workspaceService: WorkspaceService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilderLegacy
) {

    @GetMapping
    suspend fun getAccessTokens(@PathVariable workspaceId: Long): ApiPage<WorkspaceAccessTokenDto> =
        filteringApiExecutor.executeFiltering(workspaceId, WorkspaceAccessMode.ADMIN)

    @PostMapping
    suspend fun createAccessToken(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createTokenRequest: CreateWorkspaceAccessTokenDto
    ): WorkspaceAccessTokenDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        return accessTokenService
            .createAccessToken(
                workspace,
                createTokenRequest.validTill
            )
            .mapToDto()
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder
        .executor<WorkspaceAccessToken, WorkspaceAccessTokenDto> {
            query(Tables.WORKSPACE_ACCESS_TOKEN) {
                addDefaultSorting { root.id.desc() }
                workspaceFilter { workspaceId -> root.workspaceId.eq(workspaceId) }
            }
            mapper { mapToDto() }
        }
}

private fun WorkspaceAccessToken.mapToDto() = WorkspaceAccessTokenDto(
    validTill = validTill,
    revoked = revoked,
    token = token,
    id = id!!,
    version = version!!
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
