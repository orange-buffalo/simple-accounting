package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import io.orangebuffalo.accounting.simpleaccounting.services.business.PlatformUserService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.PushNotificationService
import io.orangebuffalo.accounting.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.storage.DocumentStorage
import io.orangebuffalo.accounting.simpleaccounting.services.storage.StorageProviderResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.withContext
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.channels.Channels
import java.nio.channels.Pipe
import java.security.SecureRandom
import java.time.Instant
import java.util.*

private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
private val SCOPES = listOf(DriveScopes.DRIVE_FILE)
private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
private const val TOKEN_LENGTH = 20
private const val AUTH_EVENT_NAME = "storage.google-drive.auth"
typealias DriveFile = com.google.api.services.drive.model.File

//todo move to a separate module
@Service
class GoogleDriveDocumentStorageService(
    googleDriveProperties: GoogleDriveProperties,
    private val userService: PlatformUserService,
    private val timeService: TimeService,
    private val repository: GoogleDriveStorageIntegrationRepository,
    private val pushNotificationService: PushNotificationService
) : DocumentStorage {

    override suspend fun saveDocument(file: FilePart, workspace: Workspace): StorageProviderResponse {
        val driveService = getDriveService(workspace.owner)
        //todo error handling to help user to fix the problem
            ?: throw IllegalStateException("Credentials are not defined for ${workspace.owner.userName}")

        val integration = withDbContext {
            repository.findByUser(workspace.owner)
                ?: throw IllegalStateException("Integration is not defined for ${workspace.owner.userName}")
        }

        return withContext(Dispatchers.IO) {
            val workspaceFolder = getWorkspaceFolder(driveService, integration, workspace)

            val fileMetadata = DriveFile().apply {
                name = file.filename()
                parents = listOf(workspaceFolder.id)
            }

            val newFile = uploadFileToDrive(file, driveService, fileMetadata)

            StorageProviderResponse(
                newFile.id,
                newFile.getSize()
            )
        }
    }

    private suspend fun uploadFileToDrive(
        file: FilePart,
        driveService: Drive,
        fileMetadata: DriveFile
    ): DriveFile {

        val uploadedFileToDrivePipe = Pipe.open()

        GlobalScope.launch(Dispatchers.IO) {
            val pipeInput = uploadedFileToDrivePipe.sink()
            DataBufferUtils.write(file.content(), pipeInput)
                .doOnNext(DataBufferUtils.releaseConsumer())
                .doFinally { pipeInput.close() }
                .awaitLast()
        }

        return Channels.newInputStream(uploadedFileToDrivePipe.source()).use { fileInputStream ->
            val content = InputStreamContent(null, fileInputStream)
            driveService.files().create(fileMetadata, content)
                .setFields("id,size")
                .execute()
        }
    }

    //todo it can happen that parallel file uploads of the same user create multiple workspace folders: need cleanup
    private fun getWorkspaceFolder(
        driveService: Drive,
        integration: GoogleDriveStorageIntegration,
        workspace: Workspace
    ): DriveFile {

        val workspaceFolders = driveService.files().list()
            .setQ("'${integration.folderId}' in parents and name = '${workspace.id}'")
            .execute()

        return if (workspaceFolders.files.isEmpty()) {
            val fileMetadata = DriveFile().apply {
                name = "${workspace.id}"
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(integration.folderId)
            }

            driveService.files().create(fileMetadata)
                .setFields("id")
                .execute()
        } else {
            workspaceFolders.files[0]
        }
    }

    override fun getId(): String = "google-drive"

    override suspend fun getDocumentContent(workspace: Workspace, storageLocation: String): Resource {
        //todo
        throw java.lang.IllegalArgumentException()
    }

    private val flow: GoogleAuthorizationCodeFlow
    private val redirectUrl: String = "${googleDriveProperties.redirectUrlBase}$AUTH_CALLBACK_PATH"
    private val random = SecureRandom()

    init {
        val clientSecrets = googleDriveProperties.credentialsFile.inputStream.use { inputStream ->
            GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))
        }

        flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            //todo investigate best practices for secure storage
            .setDataStoreFactory(FileDataStoreFactory(File(googleDriveProperties.dataStoreDirectory)))
            .setAccessType("offline")
            .build()
    }

    suspend fun buildAuthorizationUrl(): String? {
        val currentUser = userService.getCurrentUser()

        val integration = withDbContext {
            repository.findByUser(currentUser)
                ?: GoogleDriveStorageIntegration(currentUser)
        }

        if (getRootFolder(integration) != null) {
            return null
        }

        val authStateTokenBytes = ByteArray(TOKEN_LENGTH)
        random.nextBytes(authStateTokenBytes)

        integration.apply {
            authStateToken = "${currentUser.id}:${String(Base64.getEncoder().encode(authStateTokenBytes))}"
            timeAuthFailed = null
            timeAuthRequested = timeService.currentTime()
            timeAuthSucceeded = null
        }

        withDbContext {
            repository.save(integration)
        }

        return flow.newAuthorizationUrl()
            .setRedirectUri(redirectUrl)
            .setState(integration.authStateToken)
            .build()
    }

    private suspend fun getRootFolder(integration: GoogleDriveStorageIntegration): DriveFile? =
        withContext(Dispatchers.IO) {
            if (integration.folderId != null) {
                val drive = getDriveService(integration.user)
                if (drive != null) {
                    try {
                        return@withContext drive.files().get(integration.folderId!!)
                            .setFields("name, trashed")
                            .execute()
                            ?.let {
                                // if root folder is deleted, ignore it
                                if (it.trashed) null else it
                            }
                    } catch (e: IOException) {
                        // if failed, do nothing
                    }
                }
            }
            null
        }

    private suspend fun getDriveService(user: PlatformUser): Drive? = withContext(Dispatchers.IO) {
        getCredential(user)?.let {
            Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, it)
                .setApplicationName("simple-accounting")
                .build()
        }
    }

    private suspend fun getCredential(user: PlatformUser): Credential? = withContext(Dispatchers.IO) {
        val credential = flow.loadCredential(user.id.toString())
        if (credential != null && (credential.refreshToken != null
                    || credential.expiresInSeconds == null
                    || credential.expiresInSeconds > 60)
        ) {
            credential
        } else {
            null
        }
    }

    suspend fun onAuthSuccess(code: String, authStateToken: String) {
        val integration = withDbContext {
            repository.findByAuthStateToken(authStateToken)
                ?: throw IllegalArgumentException("$authStateToken is not known")
        }

        //todo if it shares threads with default, what's the point?
        withContext(Dispatchers.IO) {
            val response = flow.newTokenRequest(code).setRedirectUri(redirectUrl).execute()
            flow.createAndStoreCredential(response, integration.user.id.toString())
        }

        integration.timeAuthSucceeded = timeService.currentTime()
        integration.timeAuthFailed = null
        integration.authStateToken = null

        val rootFolder = ensureRootFolder(integration)

        withDbContext {
            repository.save(integration)
        }

        pushNotificationService.sendPushNotification(
            eventName = AUTH_EVENT_NAME,
            user = integration.user,
            data = mapIntegrationStatus(integration, rootFolder.name)
        )
    }

    private suspend fun ensureRootFolder(integration: GoogleDriveStorageIntegration): DriveFile =
        withContext(Dispatchers.IO) {
            val maybeRootFolder = getRootFolder(integration)
            if (maybeRootFolder != null) {
                maybeRootFolder
            } else {
                val drive = getDriveService(integration.user)
                    ?: throw IllegalStateException("Credentials expected ant this time")

                val fileMetadata = DriveFile().apply {
                    name = "simple-accounting"
                    mimeType = "application/vnd.google-apps.folder"
                }

                drive.files().create(fileMetadata)
                    .setFields("id, name")
                    .execute()
                    .also { driveFolder -> integration.folderId = driveFolder.id }
            }
        }

    suspend fun onAuthFailure(authStateToken: String) {
        val integration = withDbContext {
            repository.findByAuthStateToken(authStateToken)
                ?: throw IllegalArgumentException("$authStateToken is not known")
        }

        integration.timeAuthFailed = timeService.currentTime()
        integration.authStateToken = null
        withDbContext {
            repository.save(integration)
        }

        val maybeRootFolder = getRootFolder(integration)

        pushNotificationService.sendPushNotification(
            eventName = AUTH_EVENT_NAME,
            user = integration.user,
            data = mapIntegrationStatus(integration, maybeRootFolder?.name)
        )
    }

    suspend fun getCurrentUserIntegrationStatus(): GoogleDriveStorageIntegrationStatus {
        val integration = withDbContext {
            val currentUser = userService.getCurrentUser()
            repository.findByUser(currentUser)
        }

        return integration?.let { mapIntegrationStatus(it, getRootFolder(it)?.name) }
            ?: GoogleDriveStorageIntegrationStatus()
    }

    private fun mapIntegrationStatus(integration: GoogleDriveStorageIntegration, rootFolderName: String?) =
        GoogleDriveStorageIntegrationStatus(
            timeAuthFailed = integration.timeAuthFailed,
            timeAuthRequested = integration.timeAuthRequested,
            timeAuthSucceeded = integration.timeAuthSucceeded,
            // if folder is not fetched, consider it is not defined
            folderId = rootFolderName?.let { integration.folderId },
            folderName = rootFolderName
        )
}

data class GoogleDriveStorageIntegrationStatus(
    val timeAuthRequested: Instant? = null,
    val timeAuthSucceeded: Instant? = null,
    val timeAuthFailed: Instant? = null,
    val folderId: String? = null,
    val folderName: String? = null
)
