package com.ingsisbry.permission.application.query.list

import com.ingsisbry.permission.application.port.out.query.PermissionQueryRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ListAccessibleSnippetsUseCase(
    private val permissionQueryRepository: PermissionQueryRepository,
) : ListAccessibleSnippets {
    override fun execute(query: ListAccessibleSnippetsQuery): List<UUID> =
        permissionQueryRepository.findSnippetIds(query.userId, query.relationship)
}
