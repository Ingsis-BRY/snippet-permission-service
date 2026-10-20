package com.ingsisbry.permission.application.port.out.query

import com.ingsisbry.permission.application.query.list.SnippetRelationship
import com.ingsisbry.permission.domain.SnippetPermission
import java.util.UUID

interface PermissionQueryRepository {
    fun findBySnippetIdAndUserId(
        snippetId: UUID,
        userId: String,
    ): SnippetPermission?

    fun existsBySnippetId(snippetId: UUID): Boolean

    fun findSnippetIds(
        userId: String,
        relationship: SnippetRelationship,
    ): List<UUID>
}
