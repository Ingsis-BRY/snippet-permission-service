package com.ingsisbry.permission.application.command.revoke

import java.util.UUID

data class RevokeShareCommand(
    val snippetId: UUID,
    val requesterId: String,
    val targetUserId: String,
)
