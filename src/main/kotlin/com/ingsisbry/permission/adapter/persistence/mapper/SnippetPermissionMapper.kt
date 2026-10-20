package com.ingsisbry.permission.adapter.persistence.mapper

import com.ingsisbry.permission.adapter.persistence.permission.SnippetPermissionEntity
import com.ingsisbry.permission.domain.SnippetPermission

object SnippetPermissionMapper {
    fun toEntity(permission: SnippetPermission): SnippetPermissionEntity =
        SnippetPermissionEntity(
            id = permission.id,
            snippetId = permission.snippetId,
            userId = permission.userId,
            role = permission.role,
            createdAt = permission.createdAt,
        )

    fun toDomain(entity: SnippetPermissionEntity): SnippetPermission =
        SnippetPermission(
            id = entity.id,
            snippetId = entity.snippetId,
            userId = entity.userId,
            role = entity.role,
            createdAt = entity.createdAt,
        )
}
