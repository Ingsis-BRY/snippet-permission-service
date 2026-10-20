package com.ingsisbry.permission.application.command.share

import com.ingsisbry.permission.application.common.authorization.PermissionAuthorizer
import com.ingsisbry.permission.application.common.exception.PermissionAlreadyExistsException
import com.ingsisbry.permission.application.port.out.command.PermissionRepository
import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import com.ingsisbry.permission.domain.SnippetAction
import com.ingsisbry.permission.domain.SnippetPermission
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ShareSnippetUseCase(
    private val permissionRepository: PermissionRepository,
    private val permissionQueryRepository: PermissionQueryRepository,
    private val permissionAuthorizer: PermissionAuthorizer,
    private val clock: Clock,
) : ShareSnippet {
    override fun execute(command: ShareSnippetCommand): SnippetPermission {
        permissionAuthorizer.authorize(
            snippetId = command.snippetId,
            userId = command.requesterId,
            action = SnippetAction.SHARE,
        )

        require(command.targetUserId != command.requesterId) {
            "A snippet cannot be shared with its owner"
        }

        val existing =
            permissionQueryRepository.findBySnippetIdAndUserId(
                command.snippetId,
                command.targetUserId,
            )

        if (existing != null) {
            throw PermissionAlreadyExistsException(
                "Snippet ${command.snippetId} is already shared with ${command.targetUserId}",
            )
        }

        val permission =
            SnippetPermission.reader(
                id = UUID.randomUUID(),
                snippetId = command.snippetId,
                userId = command.targetUserId,
                createdAt = Instant.now(clock),
            )

        return permissionRepository.save(permission)
    }
}
