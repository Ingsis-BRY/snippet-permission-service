package com.ingsisbry.permission.application.command.delete

import java.util.UUID

data class DeleteSnippetPermissionsCommand(
    val snippetId: UUID,
    val requesterId: String,
)
