package com.ingsisbry.permission.application.command.delete

import com.ingsisbry.permission.application.common.authorization.PermissionAuthorizer
import com.ingsisbry.permission.application.port.out.command.PermissionRepository
import com.ingsisbry.permission.domain.SnippetAction
import org.springframework.stereotype.Service

@Service
class DeleteSnippetPermissionsUseCase(
    private val permissionRepository: PermissionRepository,
    private val permissionAuthorizer: PermissionAuthorizer,
) : DeleteSnippetPermissions {
    override fun execute(command: DeleteSnippetPermissionsCommand) {
        permissionAuthorizer.authorize(
            snippetId = command.snippetId,
            userId = command.requesterId,
            action = SnippetAction.DELETE,
        )

        permissionRepository.deleteAllBySnippetId(command.snippetId)
    }
}
