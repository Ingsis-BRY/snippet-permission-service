package com.ingsisbry.permission.adapter.persistence.query

import com.ingsisbry.permission.adapter.persistence.mapper.SnippetPermissionMapper
import com.ingsisbry.permission.adapter.persistence.permission.JpaSnippetPermissionRepository
import com.ingsisbry.permission.adapter.persistence.permission.SnippetPermissionEntity
import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import com.ingsisbry.permission.application.query.list.SnippetRelationship
import com.ingsisbry.permission.domain.PermissionRole
import com.ingsisbry.permission.domain.SnippetPermission
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class JpaPermissionQueryRepositoryAdapter(
    private val repository: JpaSnippetPermissionRepository,
) : PermissionQueryRepository {
    @Transactional(readOnly = true)
    override fun findBySnippetIdAndUserId(
        snippetId: UUID,
        userId: String,
    ): SnippetPermission? =
        repository
            .findBySnippetIdAndUserId(snippetId, userId)
            ?.let(SnippetPermissionMapper::toDomain)

    @Transactional(readOnly = true)
    override fun existsBySnippetId(snippetId: UUID): Boolean =
        repository.existsBySnippetId(snippetId)

    @Transactional(readOnly = true)
    override fun findSnippetIds(
        userId: String,
        relationship: SnippetRelationship,
    ): List<UUID> {
        val permissions =
            when (relationship) {
                SnippetRelationship.OWNER -> findAllByRole(userId, PermissionRole.OWNER)
                SnippetRelationship.SHARED -> findAllByRole(userId, PermissionRole.READER)
                SnippetRelationship.ALL -> repository.findAllByUserId(userId)
            }

        return permissions.map { it.snippetId }
    }

    private fun findAllByRole(
        userId: String,
        role: PermissionRole,
    ): List<SnippetPermissionEntity> = repository.findAllByUserIdAndRole(userId, role)
}
