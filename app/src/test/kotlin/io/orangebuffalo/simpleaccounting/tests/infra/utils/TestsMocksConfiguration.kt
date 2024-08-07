package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.orangebuffalo.simpleaccounting.infra.TimeService
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
