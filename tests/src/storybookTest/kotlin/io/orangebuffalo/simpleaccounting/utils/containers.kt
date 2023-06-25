package io.orangebuffalo.simpleaccounting.utils

import org.testcontainers.containers.NginxContainer

class KNginxContainer(dockerImageName: String) : NginxContainer<KNginxContainer>(dockerImageName)
