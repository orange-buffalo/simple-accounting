package io.orangebuffalo.simpleaccounting.infra.utils

import io.orangebuffalo.simpleaccounting.services.business.TimeService
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Configuration

/**
 * Setups common mocks for the tests to avoid creation of extra Spring Contexts
 * and so speedup the tests.
 */
@Configuration
class TestsMocksConfiguration {

    @SpyBean
    lateinit var timeService: TimeService
}
