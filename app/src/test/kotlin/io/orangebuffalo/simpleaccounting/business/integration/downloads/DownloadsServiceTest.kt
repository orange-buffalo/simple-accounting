package io.orangebuffalo.simpleaccounting.business.integration.downloads

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.business.security.getCurrentPrincipal
import io.orangebuffalo.simpleaccounting.business.security.runAs
import io.orangebuffalo.simpleaccounting.business.security.toSecurityPrincipal
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.consumeToString
import io.orangebuffalo.simpleaccounting.tests.infra.utils.toDataBuffers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

class DownloadsServiceTest(
    @Autowired private val downloadsService: DownloadsService,
    @Autowired private val testContentProvider: TestContentProvider,
) : SaIntegrationTestBase() {

    @Test
    fun `should generate download token`() {
        val token = runBlocking {
            runAs(preconditions.fry.toSecurityPrincipal()) {
                downloadsService.createDownloadToken(testContentProvider, TestContentMetadata("ref"))
            }
        }

        assertThat(token).isNotBlank().hasSizeGreaterThanOrEqualTo(20)
    }

    @Test
    fun `should fail with EntityNotFoundException if token is not known`() {
        assertThatThrownBy {
            runBlocking {
                downloadsService.getContentByToken("42")
            }
        }.isInstanceOf(EntityNotFoundException::class.java).hasMessage("Token 42 is not found")
    }

    @Test
    fun `should return content from content provider by generated token`() {
        val token = runBlocking {
            runAs(preconditions.fry.toSecurityPrincipal()) {
                downloadsService.createDownloadToken(testContentProvider, TestContentMetadata("ref"))
            }
        }
        val contentResponse = runBlocking {
            downloadsService.getContentByToken(token)
        }

        assertThat(contentResponse.sizeInBytes).isEqualTo(42)
        assertThat(contentResponse.content.consumeToString()).isEqualTo("test content")
        assertThat(contentResponse.contentType).isEqualTo("content type")
        assertThat(contentResponse.fileName).isEqualTo("file name")
    }

    data class TestContentMetadata(
        val reference: String
    )

    class TestContentProvider : DownloadableContentProvider<TestContentMetadata> {
        override fun getId() = "test-provider"

        override suspend fun getContent(metadata: TestContentMetadata): DownloadContentResponse {
            assertThat(getCurrentPrincipal().userName).isEqualTo("Fry")

            return DownloadContentResponse(
                fileName = "file name",
                contentType = "content type",
                content = "test content".toDataBuffers(),
                sizeInBytes = 42
            )
        }
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testContentProvider() = TestContentProvider()
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
        }
    }
}
