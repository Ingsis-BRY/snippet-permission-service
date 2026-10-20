package com.ingsisbry.permission.application

import com.ingsisbry.permission.application.command.delete.DeleteSnippetPermissionsCommand
import com.ingsisbry.permission.application.command.delete.DeleteSnippetPermissionsUseCase
import com.ingsisbry.permission.application.command.grant.GrantOwnershipCommand
import com.ingsisbry.permission.application.command.grant.GrantOwnershipUseCase
import com.ingsisbry.permission.application.command.revoke.RevokeShareCommand
import com.ingsisbry.permission.application.command.revoke.RevokeShareUseCase
import com.ingsisbry.permission.application.command.share.ShareSnippetCommand
import com.ingsisbry.permission.application.command.share.ShareSnippetUseCase
import com.ingsisbry.permission.application.common.authorization.PermissionAuthorizer
import com.ingsisbry.permission.application.common.exception.PermissionAlreadyExistsException
import com.ingsisbry.permission.application.common.exception.PermissionDeniedException
import com.ingsisbry.permission.application.common.exception.PermissionNotFoundException
import com.ingsisbry.permission.application.query.check.CheckPermissionQuery
import com.ingsisbry.permission.application.query.check.CheckPermissionUseCase
import com.ingsisbry.permission.application.query.list.ListAccessibleSnippetsQuery
import com.ingsisbry.permission.application.query.list.ListAccessibleSnippetsUseCase
import com.ingsisbry.permission.application.query.list.SnippetRelationship
import com.ingsisbry.permission.domain.PermissionRole
import com.ingsisbry.permission.domain.SnippetAction
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionUseCasesTest {
    private val store = InMemoryPermissionStore()
    private val clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)
    private val authorizer = PermissionAuthorizer(store)

    private val grantOwnership = GrantOwnershipUseCase(store, store, clock)
    private val shareSnippet = ShareSnippetUseCase(store, store, authorizer, clock)
    private val revokeShare = RevokeShareUseCase(store, store, authorizer)
    private val deleteSnippetPermissions = DeleteSnippetPermissionsUseCase(store, authorizer)
    private val checkPermission = CheckPermissionUseCase(store)
    private val listAccessibleSnippets = ListAccessibleSnippetsUseCase(store)

    private val snippetId = UUID.randomUUID()
    private val owner = "owner"
    private val reader = "reader"

    private fun createSnippet(id: UUID = snippetId) {
        grantOwnership.execute(GrantOwnershipCommand(id, owner))
    }

    private fun share(target: String = reader) {
        shareSnippet.execute(ShareSnippetCommand(snippetId, owner, target))
    }

    private fun canDo(
        userId: String,
        action: SnippetAction,
    ): Boolean = checkPermission.execute(CheckPermissionQuery(snippetId, userId, action))

    @Test
    fun `granting ownership registers the creator as owner`() {
        val permission = grantOwnership.execute(GrantOwnershipCommand(snippetId, owner))

        assertEquals(PermissionRole.OWNER, permission.role)
        assertEquals(Instant.now(clock), permission.createdAt)
        assertTrue(canDo(owner, SnippetAction.WRITE))
    }

    @Test
    fun `a snippet cannot have its ownership granted twice`() {
        createSnippet()

        assertFailsWith<PermissionAlreadyExistsException> {
            grantOwnership.execute(GrantOwnershipCommand(snippetId, "someone-else"))
        }
    }

    @Test
    fun `owner can share and the target only gets read access`() {
        createSnippet()

        share()

        assertTrue(canDo(reader, SnippetAction.READ))
        assertFalse(canDo(reader, SnippetAction.WRITE))
        assertFalse(canDo(reader, SnippetAction.SHARE))
    }

    @Test
    fun `reader cannot share the snippet`() {
        createSnippet()
        share()

        assertFailsWith<PermissionDeniedException> {
            shareSnippet.execute(ShareSnippetCommand(snippetId, reader, "third"))
        }
    }

    @Test
    fun `user without permission cannot share the snippet`() {
        createSnippet()

        assertFailsWith<PermissionDeniedException> {
            shareSnippet.execute(ShareSnippetCommand(snippetId, "stranger", reader))
        }
    }

    @Test
    fun `owner cannot share with themselves`() {
        createSnippet()

        assertFailsWith<IllegalArgumentException> { share(owner) }
    }

    @Test
    fun `sharing twice with the same user fails`() {
        createSnippet()
        share()

        assertFailsWith<PermissionAlreadyExistsException> { share() }
    }

    @Test
    fun `owner can revoke a share`() {
        createSnippet()
        share()

        revokeShare.execute(RevokeShareCommand(snippetId, owner, reader))

        assertFalse(canDo(reader, SnippetAction.READ))
    }

    @Test
    fun `revoking a share that does not exist fails`() {
        createSnippet()

        assertFailsWith<PermissionNotFoundException> {
            revokeShare.execute(RevokeShareCommand(snippetId, owner, reader))
        }
    }

    @Test
    fun `the owner permission cannot be revoked`() {
        createSnippet()

        assertFailsWith<IllegalArgumentException> {
            revokeShare.execute(RevokeShareCommand(snippetId, owner, owner))
        }
    }

    @Test
    fun `owner can delete every permission of a snippet`() {
        createSnippet()
        share()

        deleteSnippetPermissions.execute(DeleteSnippetPermissionsCommand(snippetId, owner))

        assertTrue(store.all().isEmpty())
    }

    @Test
    fun `reader cannot delete the snippet permissions`() {
        createSnippet()
        share()

        assertFailsWith<PermissionDeniedException> {
            deleteSnippetPermissions.execute(DeleteSnippetPermissionsCommand(snippetId, reader))
        }
    }

    @Test
    fun `check returns false for users without permission`() {
        createSnippet()

        assertFalse(canDo("stranger", SnippetAction.READ))
    }

    @Test
    fun `accessible snippets are listed by relationship`() {
        val ownSnippet = UUID.randomUUID()
        grantOwnership.execute(GrantOwnershipCommand(ownSnippet, reader))
        createSnippet()
        share()

        assertEquals(listOf(ownSnippet), list(SnippetRelationship.OWNER))
        assertEquals(listOf(snippetId), list(SnippetRelationship.SHARED))
        assertEquals(setOf(ownSnippet, snippetId), list(SnippetRelationship.ALL).toSet())
    }

    private fun list(relationship: SnippetRelationship): List<UUID> =
        listAccessibleSnippets.execute(ListAccessibleSnippetsQuery(reader, relationship))
}
