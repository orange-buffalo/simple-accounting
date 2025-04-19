package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

private val wireMockServer = ThirdPartyApisMocks.server

object GoogleDriveApiMocks {
    private const val GDRIVE_MOCKS_ROOT_PATH = "/google-drive-mocks"

    fun configProperties() = arrayOf(
        "simpleaccounting.documents.storage.google-drive.base-api-url=" +
                "http://localhost:${wireMockServer.port()}$GDRIVE_MOCKS_ROOT_PATH"
    )
}
