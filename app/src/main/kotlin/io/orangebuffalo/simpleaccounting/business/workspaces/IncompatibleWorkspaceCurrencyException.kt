package io.orangebuffalo.simpleaccounting.business.workspaces

class IncompatibleWorkspaceCurrencyException(residency: String, currency: String) :
    RuntimeException("Currency $currency is not supported for residency $residency")
