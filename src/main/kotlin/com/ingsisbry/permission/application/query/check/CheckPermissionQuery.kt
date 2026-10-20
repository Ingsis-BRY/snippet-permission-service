package com.ingsisbry.permission.application.query.check

import com.ingsisbry.permission.domain.SnippetAction
import java.util.UUID

data class CheckPermissionQuery(
    val snippetId: UUID,
    val userId: String,
    val action: SnippetAction,
)
