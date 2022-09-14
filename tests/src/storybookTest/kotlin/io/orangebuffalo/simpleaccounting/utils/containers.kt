package io.orangebuffalo.simpleaccounting.utils

import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.NginxContainer

class KBrowserWebDriverContainer : BrowserWebDriverContainer<KBrowserWebDriverContainer>()
class KNginxContainer(dockerImageName: String) : NginxContainer<KNginxContainer>(dockerImageName)
