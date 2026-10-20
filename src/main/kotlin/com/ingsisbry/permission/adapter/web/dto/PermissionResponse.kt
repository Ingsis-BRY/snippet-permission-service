package com.ingsisbry.permission.adapter.web.dto

import com.ingsisbry.permission.domain.PermissionRole
import com.ingsisbry.permission.domain.SnippetPermission
import java.time.Instant
import java.util.UUID

data class PermissionResponse(
    val id: UUID,
    val snippetId: UUID,
    val userId: String,
    val role: PermissionRole,
    val createdAt: Instant,
) {
    companion object {
        fun from(permission: SnippetPermission): PermissionResponse =
            PermissionResponse(
                id = permission.id,
                snippetId = permission.snippetId,
                userId = permission.userId,
                role = permission.role,
                createdAt = permission.createdAt,
            )
    }
}
