package com.ingsisbry.permission.application.common.exception

class PermissionAlreadyExistsException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
