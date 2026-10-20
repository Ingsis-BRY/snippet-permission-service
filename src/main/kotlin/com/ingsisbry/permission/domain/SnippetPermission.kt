package com.ingsisbry.permission.domain

import java.time.Instant
import java.util.UUID

data class SnippetPermission(
    val id: UUID,
    val snippetId: UUID,
    val userId: String,
    val role: PermissionRole,
    val createdAt: Instant,
) {
    init {
        require(userId.isNotBlank()) { "User id must not be blank" }
    }

    fun allows(action: SnippetAction): Boolean = role.allows(action)

    companion object {
        fun owner(
            id: UUID,
            snippetId: UUID,
            userId: String,
            createdAt: Instant,
        ): SnippetPermission =
            SnippetPermission(
                id = id,
                snippetId = snippetId,
                userId = userId,
                role = PermissionRole.OWNER,
                createdAt = createdAt,
            )

        fun reader(
            id: UUID,
            snippetId: UUID,
            userId: String,
            createdAt: Instant,
        ): SnippetPermission =
            SnippetPermission(
                id = id,
                snippetId = snippetId,
                userId = userId,
                role = PermissionRole.READER,
                createdAt = createdAt,
            )
    }
}
