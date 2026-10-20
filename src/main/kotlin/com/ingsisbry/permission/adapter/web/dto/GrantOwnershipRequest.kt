package com.ingsisbry.permission.adapter.web.dto

import java.util.UUID

data class GrantOwnershipRequest(
    val snippetId: UUID,
    val userId: String,
)
