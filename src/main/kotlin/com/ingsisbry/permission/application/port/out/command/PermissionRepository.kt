package com.ingsisbry.permission.application.port.out.command

import com.ingsisbry.permission.domain.SnippetPermission
import java.util.UUID

interface PermissionRepository {
    fun save(permission: SnippetPermission): SnippetPermission

    fun delete(permission: SnippetPermission)

    fun deleteAllBySnippetId(snippetId: UUID)
}
