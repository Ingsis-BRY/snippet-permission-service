package com.ingsisbry.permission.adapter.web.dto

import java.util.UUID

data class ShareSnippetRequest(
    val snippetId: UUID,
    val targetUserId: String,
)
