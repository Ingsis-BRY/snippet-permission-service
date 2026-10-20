package com.ingsisbry.permission.application.query.check

fun interface CheckPermission {
    fun execute(query: CheckPermissionQuery): Boolean
}
