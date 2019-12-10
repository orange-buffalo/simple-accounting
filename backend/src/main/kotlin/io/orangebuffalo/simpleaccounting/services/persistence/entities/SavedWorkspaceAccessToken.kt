package io.orangebuffalo.simpleaccounting.services.persistence.entities

import javax.persistence.Entity
import javax.persistence.ForeignKey
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class SavedWorkspaceAccessToken(

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "saved_ws_access_token_ws_access_token_fk"))
    var workspaceAccessToken: WorkspaceAccessToken,

    @field:ManyToOne(optional = false)
    @field:JoinColumn(nullable = false, foreignKey = ForeignKey(name = "saved_ws_access_token_owner_fk"))
    var owner: PlatformUser

) : AbstractEntity()
