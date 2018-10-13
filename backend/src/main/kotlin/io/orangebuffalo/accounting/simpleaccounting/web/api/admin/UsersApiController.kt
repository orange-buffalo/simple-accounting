package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiDto
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.ApiPageRequest
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.mapping.ApiDtoMapperAdapter
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1/admin/users")
class UsersApiController {

    @GetMapping
    @ApiDto(ApiUser::class)
    fun getUsers(pageRequest: ApiPageRequest): Mono<Page<PlatformUser>> {
        return Mono.empty()
    }
}

data class ApiUser(
        var userName: String,
        var id: Long?,
        var version: Int)

@Component
class ApiUserPropertyMap
    : ApiDtoMapperAdapter<PlatformUser, ApiUser>(PlatformUser::class.java, ApiUser::class.java) {

    override fun map(source: PlatformUser): ApiUser = ApiUser(
            userName = source.userName,
            id = source.id,
            version = source.version)
}