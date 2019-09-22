package io.orangebuffalo.accounting.simpleaccounting

import org.springframework.security.test.context.support.WithMockUser

@Retention(AnnotationRetention.RUNTIME)
@WithMockUser(roles = ["USER"], username = "Fry")
annotation class WithMockFryUser

@Retention(AnnotationRetention.RUNTIME)
@WithMockUser(roles = ["USER"], username = "Zoidberg")
annotation class WithMockZoidbergUser

@Retention(AnnotationRetention.RUNTIME)
@WithMockUser(roles = ["USER"], username = "Farnsworth")
annotation class WithMockFarnsworthUser

@Retention(AnnotationRetention.RUNTIME)
@WithMockUser(roles = ["USER"], username = "Roberto")
annotation class WithMockRobertoUser

@Retention(AnnotationRetention.RUNTIME)
@WithMockUser(roles = ["USER"], username = "MafiaBot")
annotation class WithMockMafiaBotUser