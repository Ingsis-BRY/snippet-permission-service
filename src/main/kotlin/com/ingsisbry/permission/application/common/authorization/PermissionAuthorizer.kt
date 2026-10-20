package com.ingsisbry.permission.application.common.authorization

import com.ingsisbry.permission.application.common.exception.PermissionDeniedException
import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import com.ingsisbry.permission.domain.SnippetAction
import com.ingsisbry.permission.domain.SnippetPermission
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PermissionAuthorizer(
    private val permissionQueryRepository: PermissionQueryRepository,
) {
    fun authorize(
        snippetId: UUID,
        userId: String,
        action: SnippetAction,
    ): SnippetPermission {
        val permission = permissionQueryRepository.findBySnippetIdAndUserId(snippetId, userId)

        if (permission == null || !permission.allows(action)) {
            throw PermissionDeniedException("User $userId cannot $action snippet $snippetId")
        }

        return permission
    }
}
