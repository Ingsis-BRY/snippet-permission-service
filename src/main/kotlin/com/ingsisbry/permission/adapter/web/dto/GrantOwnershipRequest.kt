package com.ingsisbry.permission.adapter.web.dto

import java.util.UUID

/** The owner is the caller (the token's `sub`), so the body only names the snippet. */
data class GrantOwnershipRequest(
    val snippetId: UUID,
)
