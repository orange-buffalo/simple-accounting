package io.orangebuffalo.simpleaccounting.utils

import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.NginxContainer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Workaround for https://github.com/testcontainers/testcontainers-java/issues/3081
 */
class ReusableNetwork(
    private val name: String
) : Network {

    private val initialized = AtomicBoolean(false)
    private var id: String? = null

    override fun close() {
        // reusable network does not close
    }

    override fun getId(): String {
        if (!initialized.getAndSet(true)) {
            val networks = DockerClientFactory.instance().client().listNetworksCmd()
                .withNameFilter(name)
                .exec()
            id = if (networks.isNotEmpty()) {
                networks[0].id
            } else {
                DockerClientFactory.instance().client().createNetworkCmd()
                    .withName(name)
                    .withCheckDuplicate(true)
                    .exec()
                    .id
            }
        }
        return id ?: throw IllegalStateException()
    }

    override fun apply(base: Statement, description: Description): Statement =
        throw UnsupportedOperationException()
}

class KBrowserWebDriverContainer : BrowserWebDriverContainer<KBrowserWebDriverContainer>()
class KNginxContainer(dockerImageName: String) : NginxContainer<KNginxContainer>(dockerImageName)
