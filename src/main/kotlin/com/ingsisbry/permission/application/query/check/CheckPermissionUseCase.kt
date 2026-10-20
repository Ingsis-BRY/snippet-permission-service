package com.ingsisbry.permission.application.query.check

import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import org.springframework.stereotype.Service

@Service
class CheckPermissionUseCase(
    private val permissionQueryRepository: PermissionQueryRepository,
) : CheckPermission {
    override fun execute(query: CheckPermissionQuery): Boolean =
        permissionQueryRepository
            .findBySnippetIdAndUserId(query.snippetId, query.userId)
            ?.allows(query.action)
            ?: false
}
