package com.ingsisbry.permission.application.command.grant

import com.ingsisbry.permission.application.common.exception.PermissionAlreadyExistsException
import com.ingsisbry.permission.application.port.out.command.PermissionRepository
import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import com.ingsisbry.permission.domain.SnippetPermission
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class GrantOwnershipUseCase(
    private val permissionRepository: PermissionRepository,
    private val permissionQueryRepository: PermissionQueryRepository,
    private val clock: Clock,
) : GrantOwnership {
    override fun execute(command: GrantOwnershipCommand): SnippetPermission {
        if (permissionQueryRepository.existsBySnippetId(command.snippetId)) {
            throw PermissionAlreadyExistsException(
                "Snippet ${command.snippetId} already has an owner",
            )
        }

        val permission =
            SnippetPermission.owner(
                id = UUID.randomUUID(),
                snippetId = command.snippetId,
                userId = command.userId,
                createdAt = Instant.now(clock),
            )

        return permissionRepository.save(permission)
    }
}
