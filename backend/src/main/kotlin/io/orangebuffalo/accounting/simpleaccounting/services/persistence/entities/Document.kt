package io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities

import java.time.ZonedDateTime
import javax.persistence.*

@Entity
class Document(

    @field:Column(nullable = false) var name: String,

    @field:Column(length = 1024) var notes: String? = null,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "document_workspace_fk"))
    val workspace: Workspace,

    @field:Column(nullable = false) val timeUploaded: ZonedDateTime,

    @field:Column(nullable = false) var storageProviderId: String,

    @field:Column(length = 2048) var storageProviderLocation: String?

) : AbstractEntity()