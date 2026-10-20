package com.ingsisbry.permission.application.query.list

import java.util.UUID

fun interface ListAccessibleSnippets {
    fun execute(query: ListAccessibleSnippetsQuery): List<UUID>
}
