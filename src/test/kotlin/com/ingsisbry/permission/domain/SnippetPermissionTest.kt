package com.ingsisbry.permission.domain

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SnippetPermissionTest {
    @Test
    fun `a permission cannot belong to a blank user`() {
        assertFailsWith<IllegalArgumentException> {
            SnippetPermission.owner(UUID.randomUUID(), UUID.randomUUID(), "  ", Instant.now())
        }
    }
}
