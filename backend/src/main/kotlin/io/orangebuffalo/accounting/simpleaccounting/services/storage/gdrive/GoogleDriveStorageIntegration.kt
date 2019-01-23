package io.orangebuffalo.accounting.simpleaccounting.services.storage.gdrive

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.AbstractEntity
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import javax.persistence.*

@Entity
class GoogleDriveStorageIntegration(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "gdrive_storage_integration_user_fk"))
    val user: PlatformUser,

    @field:Column var folderId: String? = null

) : AbstractEntity()