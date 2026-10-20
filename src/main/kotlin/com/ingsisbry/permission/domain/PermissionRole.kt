package com.ingsisbry.permission.domain

enum class PermissionRole(
    private val allowedActions: Set<SnippetAction>,
) {
    OWNER(SnippetAction.entries.toSet()),
    READER(setOf(SnippetAction.READ)),
    ;

    fun allows(action: SnippetAction): Boolean = action in allowedActions
}
