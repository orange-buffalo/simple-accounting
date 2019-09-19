package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessTokenService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QWorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.WorkspaceAccessToken
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiControllersExtensions
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApi
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.PageableApiDescriptor
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import javax.validation.Valid

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/workspace-access-tokens")
class WorkspaceAccessTokensApiController(
    private val extensions: ApiControllersExtensions,
    private val accessTokenService: WorkspaceAccessTokenService
) {

    @GetMapping
    @PageableApi(WorkspaceAccessTokenPageableApiDescriptor::class)
    fun getAccessTokens(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<WorkspaceAccessToken>> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        accessTokenService.getAccessTokens(workspace, pageRequest.page, pageRequest.predicate)
    }

    @PostMapping
    fun createToken(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid createTokenRequest: CreateWorkspaceAccessTokenDto
    ): Mono<WorkspaceAccessTokenDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.ADMIN)
        mapToDto(accessTokenService.createAccessToken(workspace, createTokenRequest.validTill))
    }
}

private fun mapToDto(token: WorkspaceAccessToken) = WorkspaceAccessTokenDto(
    validTill = token.validTill,
    revoked = token.revoked,
    token = token.token,
    id = token.id!!,
    version = token.version
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

@Component
class WorkspaceAccessTokenPageableApiDescriptor : PageableApiDescriptor<WorkspaceAccessToken, QWorkspaceAccessToken> {

    override suspend fun mapEntityToDto(entity: WorkspaceAccessToken) = mapToDto(entity)

}