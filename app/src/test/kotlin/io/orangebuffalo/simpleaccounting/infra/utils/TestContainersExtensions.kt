package io.orangebuffalo.simpleaccounting.infra.utils

import org.testcontainers.containers.NginxContainer

class KNginxContainer(dockerImageName: String) : NginxContainer<KNginxContainer>(dockerImageName)
