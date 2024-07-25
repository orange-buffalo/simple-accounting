package io.orangebuffalo.simpleaccounting.tests.infra.utils

import org.testcontainers.containers.NginxContainer

class KNginxContainer(dockerImageName: String) : NginxContainer<KNginxContainer>(dockerImageName)
