package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.ApiErrorMapping
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPage
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.ApiPageRequest
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.FilteringApiExecutorBuilder
import io.orangebuffalo.simpleaccounting.web.api.integration.filtering.NoOpSorting
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.jooq.impl.DSL.lower
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@Transactional
class UsersApi(
    private val userService: PlatformUsersService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @GetMapping
    suspend fun getUsers(
        @ParameterObject request: UsersFilteringRequest
    ): ApiPage<PlatformUserDto> = filteringApiExecutor.executeFiltering(request)

    @GetMapping("/{userId}")
    suspend fun getUser(
        @PathVariable("userId") userId: Long
    ): PlatformUserDto = userService.getUserByUserId(userId).mapToUserDto()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorMapping(UserCreationException.UserAlreadyExistsException::class, "UserAlreadyExists")
    suspend fun createUser(@RequestBody @Valid user: CreateUserRequestDto): PlatformUserDto = userService
        .createUser(
            userName = user.userName,
            isAdmin = user.admin,
        )
        .mapToUserDto()

    @PutMapping("/{userId}")
    @ApiErrorMapping(UserUpdateException.UserAlreadyExistsException::class, "UserAlreadyExists")
    suspend fun updateUser(
        @PathVariable userId: Long,
        @RequestBody @Valid request: UpdateUserRequestDto
    ): PlatformUserDto {
        val user = userService.getUserByUserId(userId)
        user.userName = request.userName
        return userService.updateUser(user).mapToUserDto()
    }

    private val filteringApiExecutor = filteringApiExecutorBuilder
        .executor<PlatformUser, PlatformUserDto, NoOpSorting, UsersFilteringRequest> {
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
    var admin: Boolean,
    var activated: Boolean,
)

data class CreateUserRequestDto(
    @field:NotBlank @field:Size(max = 255) var userName: String,
    var admin: Boolean,
)

data class UpdateUserRequestDto(
    @field:NotBlank @field:Size(max = 255) var userName: String,
)

private fun PlatformUser.mapToUserDto() = PlatformUserDto(
    userName = userName,
    id = id,
    version = version!!,
    admin = isAdmin,
    activated = activated,
)
