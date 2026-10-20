package com.ingsisbry.permission.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionRoleTest {
    @Test
    fun `owner can perform every action`() {
        SnippetAction.entries.forEach { action ->
            assertTrue(PermissionRole.OWNER.allows(action))
        }
    }

    @Test
    fun `reader can only read`() {
        assertTrue(PermissionRole.READER.allows(SnippetAction.READ))
        assertFalse(PermissionRole.READER.allows(SnippetAction.WRITE))
        assertFalse(PermissionRole.READER.allows(SnippetAction.SHARE))
        assertFalse(PermissionRole.READER.allows(SnippetAction.DELETE))
    }
}
