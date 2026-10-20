package com.ingsisbry.permission.adapter.persistence.permission

import com.ingsisbry.permission.domain.PermissionRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaSnippetPermissionRepository : JpaRepository<SnippetPermissionEntity, UUID> {
    fun findBySnippetIdAndUserId(
        snippetId: UUID,
        userId: String,
    ): SnippetPermissionEntity?

    fun existsBySnippetId(snippetId: UUID): Boolean

    fun findAllByUserId(userId: String): List<SnippetPermissionEntity>

    fun findAllByUserIdAndRole(
        userId: String,
        role: PermissionRole,
    ): List<SnippetPermissionEntity>

    fun deleteAllBySnippetId(snippetId: UUID)
}
