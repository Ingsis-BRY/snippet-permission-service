package com.ingsisbry.permission.application.command.grant

import com.ingsisbry.permission.domain.SnippetPermission

fun interface GrantOwnership {
    fun execute(command: GrantOwnershipCommand): SnippetPermission
}
