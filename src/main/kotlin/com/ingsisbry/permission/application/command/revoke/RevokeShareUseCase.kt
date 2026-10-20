package com.ingsisbry.permission.application.command.revoke

import com.ingsisbry.permission.application.common.authorization.PermissionAuthorizer
import com.ingsisbry.permission.application.common.exception.PermissionNotFoundException
import com.ingsisbry.permission.application.port.out.command.PermissionRepository
import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import com.ingsisbry.permission.domain.PermissionRole
import com.ingsisbry.permission.domain.SnippetAction
import org.springframework.stereotype.Service

@Service
class RevokeShareUseCase(
    private val permissionRepository: PermissionRepository,
    private val permissionQueryRepository: PermissionQueryRepository,
    private val permissionAuthorizer: PermissionAuthorizer,
) : RevokeShare {
    override fun execute(command: RevokeShareCommand) {
        permissionAuthorizer.authorize(
            snippetId = command.snippetId,
            userId = command.requesterId,
            action = SnippetAction.SHARE,
        )

        val permission =
            permissionQueryRepository.findBySnippetIdAndUserId(
                command.snippetId,
                command.targetUserId,
            ) ?: throw PermissionNotFoundException(
                "Snippet ${command.snippetId} is not shared with ${command.targetUserId}",
            )

        require(permission.role != PermissionRole.OWNER) {
            "The owner permission cannot be revoked"
        }

        permissionRepository.delete(permission)
    }
}
