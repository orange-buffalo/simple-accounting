package io.orangebuffalo.accounting.simpleaccounting.web.api.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/users")
class UsersApiController {

//    @GetMapping
//    fun getUsers(
//            @RequestParam(name = "page", defaultValue = "1") page: Int,
//            @RequestParam(name = "page_size", defaultValue = "10") pageSize: Int,
//            @RequestParam(name = "sort", defaultValue = "userName.asc") sortBy: String
//    ): Flux<ApiUser> {
//
////        val sortParts = sortBy.split(".")
////        if (sortParts.size > 2) {
////            throw ApiValidationException("$sortBy is not a valid sort expression")
////        }
////        val propertyName = sortParts[0]
////        val sortDirection = if (sortParts.size > 1) sortParts[1] else "asc"
////
////        validatePropertyExists(ApiUser::class, propertyName)
//
//
//    }


}

//fun getProperty(klass: KClass<out Any>, propertyName: String): KProperty1<out Any, *> {
//   return klass.declaredMemberProperties.filter { it.name == propertyName }.getOrNull(0)
//            ?: throw ApiValidationException("Unknown property $propertyName")
//}
//
//fun validatePropertyExists(klass: KClass<Any>, propertyName: String) {
//    getProperty(klass, propertyName)
//}
//
//inline fun <reified R: Any?> readProperty(instance: Any, propertyName: String): R {
//    val property = getProperty(instance.javaClass.kotlin, propertyName)
//
//    val propertyValue = property.get(instance)
//    if (propertyValue is R) {
//        return propertyValue
//    }
//
//    throw ApiValidationException("Invalid property type for $propertyName")
//}

data class ApiUser(
        var userName: String,
        var id: Long,
        var version: Int)