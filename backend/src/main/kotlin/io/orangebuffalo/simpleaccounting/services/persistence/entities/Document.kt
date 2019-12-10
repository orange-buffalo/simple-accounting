package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import javax.persistence.*

@Entity
class Document(

    @field:Column(nullable = false) var name: String,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "document_workspace_fk"))
    val workspace: Workspace,

    @field:Column(nullable = false) val timeUploaded: Instant,

    @field:Column(nullable = false) var storageProviderId: String,

    @field:Column(length = 2048) var storageProviderLocation: String?,

    val sizeInBytes: Long?

) : AbstractEntity()
