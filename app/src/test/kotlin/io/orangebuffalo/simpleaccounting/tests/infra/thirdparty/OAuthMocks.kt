package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import no.nav.security.mock.oauth2.MockOAuth2Server

val mockOAuthServer = MockOAuth2Server()
    .apply {
        start()
        Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
    }
