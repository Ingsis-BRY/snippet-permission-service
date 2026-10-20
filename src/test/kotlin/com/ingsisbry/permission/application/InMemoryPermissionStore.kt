package com.ingsisbry.permission.application

import com.ingsisbry.permission.application.port.out.command.PermissionRepository
import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import com.ingsisbry.permission.application.query.list.SnippetRelationship
import com.ingsisbry.permission.domain.PermissionRole
import com.ingsisbry.permission.domain.SnippetPermission
import java.util.UUID

class InMemoryPermissionStore :
    PermissionRepository,
    PermissionQueryRepository {
    private val permissions = mutableMapOf<UUID, SnippetPermission>()

    fun all(): List<SnippetPermission> = permissions.values.toList()

    override fun save(permission: SnippetPermission): SnippetPermission {
        permissions[permission.id] = permission
        return permission
    }

    override fun delete(permission: SnippetPermission) {
        permissions.remove(permission.id)
    }

    override fun deleteAllBySnippetId(snippetId: UUID) {
        permissions.values.removeIf { it.snippetId == snippetId }
    }

    override fun findBySnippetIdAndUserId(
        snippetId: UUID,
        userId: String,
    ): SnippetPermission? =
        permissions.values.find { it.snippetId == snippetId && it.userId == userId }

    override fun existsBySnippetId(snippetId: UUID): Boolean =
        permissions.values.any { it.snippetId == snippetId }

    override fun findSnippetIds(
        userId: String,
        relationship: SnippetRelationship,
    ): List<UUID> =
        permissions.values
            .filter { it.userId == userId }
            .filter { matches(it.role, relationship) }
            .map { it.snippetId }

    private fun matches(
        role: PermissionRole,
        relationship: SnippetRelationship,
    ): Boolean =
        when (relationship) {
            SnippetRelationship.OWNER -> role == PermissionRole.OWNER
            SnippetRelationship.SHARED -> role == PermissionRole.READER
            SnippetRelationship.ALL -> true
        }
}
