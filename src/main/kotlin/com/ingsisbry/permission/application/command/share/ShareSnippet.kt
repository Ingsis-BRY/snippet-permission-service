package com.ingsisbry.permission.application.command.share

import com.ingsisbry.permission.domain.SnippetPermission

fun interface ShareSnippet {
    fun execute(command: ShareSnippetCommand): SnippetPermission
}
