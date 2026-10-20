package com.ingsisbry.permission.application.command.share

import java.util.UUID

data class ShareSnippetCommand(
    val snippetId: UUID,
    val requesterId: String,
    val targetUserId: String,
)
