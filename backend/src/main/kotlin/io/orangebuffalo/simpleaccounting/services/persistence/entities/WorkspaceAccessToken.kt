package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.time.Instant
import javax.persistence.*

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(name = "workspace_access_token_token_uq", columnNames = ["token"])
    ]
)
class WorkspaceAccessToken(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "workspace_access_token_workspace_fk"))
    var workspace: Workspace,

    @field:Column(nullable = false)
    val timeCreated: Instant,

    @field:Column(nullable = false)
    val validTill: Instant,

    @field:Column(nullable = false)
    val revoked: Boolean,

    @field:Column(nullable = false)
    val token: String

) : LegacyAbstractEntity()
