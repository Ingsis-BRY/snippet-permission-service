package com.ingsisbry.permission.application.command.grant

import java.util.UUID

data class GrantOwnershipCommand(
    val snippetId: UUID,
    val userId: String,
)
