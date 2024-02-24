package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.security.authentication.AuthenticationService
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPageRequest
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.NoOpSorting
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.jooq.impl.DSL.lower
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UsersApiController(
    private val userService: PlatformUserService,
    private val authenticationService: AuthenticationService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @GetMapping
    suspend fun getUsers(
        @ParameterObject request: UsersFilteringRequest
    ): ApiPage<PlatformUserDto> = filteringApiExecutor.executeFiltering(request)

    @PostMapping
    suspend fun createUser(@RequestBody @Valid user: CreateUserRequestDto): PlatformUserDto = userService
        .save(
            PlatformUser(
                userName = user.userName!!,
                passwordHash = "",
                isAdmin = user.admin!!,
                i18nSettings = I18nSettings(locale = "en_AU", language = "en")
            ).apply {
                authenticationService.setUserPassword(this, user.password!!)
            }
        )
        .mapToUserDto()

    private val filteringApiExecutor =
        filteringApiExecutorBuilder.executor<PlatformUser, PlatformUserDto, NoOpSorting, UsersFilteringRequest> {
            val platformUser = Tables.PLATFORM_USER
            query(platformUser) {
                addDefaultSorting { lower(root.userName).asc() }
                onFilter(UsersFilteringRequest::freeSearchText) { searchText ->
                    platformUser.userName.containsIgnoreCase(searchText)
                }
            }
            mapper { mapToUserDto() }
        }
}

class UsersFilteringRequest : ApiPageRequest<NoOpSorting>() {
    override var sortBy: NoOpSorting? = null

    @field:Parameter(name = "freeSearchText[eq]")
    var freeSearchText: String? = null
}

data class PlatformUserDto(
    var userName: String,
    var id: Long?,
    var version: Int,
    var admin: Boolean
)

data class CreateUserRequestDto(
    @field:NotBlank var userName: String?,
    @field:NotNull var admin: Boolean?,
    @field:NotBlank var password: String?
)

private fun PlatformUser.mapToUserDto() = PlatformUserDto(
    userName = userName,
    id = id,
    version = version!!,
    admin = isAdmin
)
