package io.orangebuffalo.simpleaccounting.tests.infra.utils

import com.nhaarman.mockitokotlin2.spy
import io.orangebuffalo.simpleaccounting.infra.TimeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Setups common mocks for the tests to avoid creation of extra Spring Contexts
 * and so speedup the tests.
 */
@Configuration
class TestsMocksConfiguration {

    /**
     * See Spring limitation described in https://github.com/spring-projects/spring-framework/issues/33934#issuecomment-2512136161.
     * Waiting for https://github.com/spring-projects/spring-framework/issues/33925 to be available - we then
     * can convert this workaround with primary bean into a meta-annotation.
     */
    @Bean
    @Primary
    fun testTimeService(prodTimeService: TimeService) = spy(prodTimeService)
}
