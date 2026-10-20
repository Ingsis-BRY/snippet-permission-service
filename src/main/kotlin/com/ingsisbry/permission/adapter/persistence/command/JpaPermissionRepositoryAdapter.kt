package com.ingsisbry.permission.adapter.persistence.command

import com.ingsisbry.permission.adapter.persistence.mapper.SnippetPermissionMapper
import com.ingsisbry.permission.adapter.persistence.permission.JpaSnippetPermissionRepository
import com.ingsisbry.permission.application.common.exception.PermissionAlreadyExistsException
import com.ingsisbry.permission.application.port.out.command.PermissionRepository
import com.ingsisbry.permission.domain.SnippetPermission
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class JpaPermissionRepositoryAdapter(
    private val repository: JpaSnippetPermissionRepository,
) : PermissionRepository {
    @Transactional
    override fun save(permission: SnippetPermission): SnippetPermission {
        try {
            repository.saveAndFlush(SnippetPermissionMapper.toEntity(permission))
        } catch (exception: DataIntegrityViolationException) {
            throw PermissionAlreadyExistsException(
                "User ${permission.userId} already has a permission on ${permission.snippetId}",
                exception,
            )
        }

        return permission
    }

    @Transactional
    override fun delete(permission: SnippetPermission) {
        repository.deleteById(permission.id)
    }

    @Transactional
    override fun deleteAllBySnippetId(snippetId: UUID) {
        repository.deleteAllBySnippetId(snippetId)
    }
}
