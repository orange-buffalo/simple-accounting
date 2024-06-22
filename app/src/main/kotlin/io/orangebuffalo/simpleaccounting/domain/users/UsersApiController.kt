package io.orangebuffalo.simpleaccounting.domain.users

import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.DefaultErrorHandler
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.HandleApiErrorsWith
import io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling.SaApiErrorDto
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
class UsersApiController(
    private val userService: PlatformUserService,
    filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) {

    @GetMapping
    suspend fun getUsers(
        @ParameterObject request: UsersFilteringRequest
    ): ApiPage<PlatformUserDto> = filteringApiExecutor.executeFiltering(request)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @HandleApiErrorsWith(UserApiBadRequestErrorHandler::class)
    suspend fun createUser(@RequestBody @Valid user: CreateUserRequestDto): PlatformUserDto = userService
        .createUser(
            userName = user.userName,
            isAdmin = user.admin,
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
    var admin: Boolean,
    var activated: Boolean,
)

data class CreateUserRequestDto(
    @field:NotBlank @field:Size(max = 255) var userName: String,
    var admin: Boolean,
)

private fun PlatformUser.mapToUserDto() = PlatformUserDto(
    userName = userName,
    id = id,
    version = version!!,
    admin = isAdmin,
    activated = activated,
)

class UserApiBadRequestErrorHandler : DefaultErrorHandler<UserApiErrors, UserApiBadRequestErrors>(
    responseType = UserApiBadRequestErrors::class,
    exceptionMappings = mapOf(
        UserCreationException.UserAlreadyExistsException::class to UserApiErrors.UserAlreadyExists
    )
)

enum class UserApiErrors {
    /**
     * Indicates that a user with requested username already exists.
     */
    UserAlreadyExists,
}

class UserApiBadRequestErrors(error: UserApiErrors, message: String?) :
    SaApiErrorDto<UserApiErrors>(error, message)

