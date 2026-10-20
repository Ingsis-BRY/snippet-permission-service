package com.ingsisbry.permission.application.command.revoke

fun interface RevokeShare {
    fun execute(command: RevokeShareCommand)
}
