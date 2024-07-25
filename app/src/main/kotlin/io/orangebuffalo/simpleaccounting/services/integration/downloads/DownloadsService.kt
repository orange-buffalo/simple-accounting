package io.orangebuffalo.simpleaccounting.services.integration.downloads

import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * Manages downloads by tokens, i.e. for content sharing or other purposes where security credentials cannot be
 * applied when accessing the content.
 */
@Service
class DownloadsService(
    private val downloadsRepository: DownloadsRepository,
    private val tokenGenerator: TokenGenerator,
    @Lazy private val contentProviders: List<DownloadableContentProvider<*>>,
    private val userService: PlatformUsersService
) {
    suspend fun <T : Any> createDownloadToken(contentProvider: DownloadableContentProvider<T>, metadata: T): String =
        tokenGenerator.generateToken(tokenLength = 30)
            .also { token ->
                downloadsRepository.storeDownloadRequest(
                    token = token,
                    providerId = contentProvider.getId(),
                    metadata = metadata,
                    userName = getCurrentPrincipal().userName
                )
            }

    suspend fun getContentByToken(token: String): DownloadContentResponse {
        val downloadRequest = downloadsRepository.getRequestByToken(token)
        val contentProvider = contentProviders.find { it.getId() == downloadRequest.providerId }
            ?: throw IllegalStateException("Cannot find provider ${downloadRequest.providerId}")
        val user = userService.getUserByUserName(downloadRequest.userName)
            ?: throw IllegalStateException("Cannot find user ${downloadRequest.userName}")

        return runAs(user.toSecurityPrincipal()) {
            @Suppress("UNCHECKED_CAST")
            (contentProvider as DownloadableContentProvider<Any>).getContent(downloadRequest.metadata)
        }
    }
}

