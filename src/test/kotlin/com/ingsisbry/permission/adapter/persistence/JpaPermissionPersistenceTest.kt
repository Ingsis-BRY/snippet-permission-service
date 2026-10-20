package com.ingsisbry.permission.adapter.persistence

import com.ingsisbry.permission.adapter.persistence.command.JpaPermissionRepositoryAdapter
import com.ingsisbry.permission.adapter.persistence.permission.JpaSnippetPermissionRepository
import com.ingsisbry.permission.adapter.persistence.query.JpaPermissionQueryRepositoryAdapter
import com.ingsisbry.permission.application.common.exception.PermissionAlreadyExistsException
import com.ingsisbry.permission.application.query.list.SnippetRelationship
import com.ingsisbry.permission.domain.SnippetPermission
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
class JpaPermissionPersistenceTest {
    @Autowired
    private lateinit var commandAdapter: JpaPermissionRepositoryAdapter

    @Autowired
    private lateinit var queryAdapter: JpaPermissionQueryRepositoryAdapter

    @Autowired
    private lateinit var repository: JpaSnippetPermissionRepository

    private val createdAt = Instant.parse("2026-10-20T12:00:00Z")

    @BeforeEach
    fun cleanDatabase() {
        repository.deleteAll()
    }

    private fun owner(
        snippetId: UUID,
        userId: String,
    ): SnippetPermission = SnippetPermission.owner(UUID.randomUUID(), snippetId, userId, createdAt)

    private fun reader(
        snippetId: UUID,
        userId: String,
    ): SnippetPermission = SnippetPermission.reader(UUID.randomUUID(), snippetId, userId, createdAt)

    @Test
    fun `saved permissions can be found by snippet and user`() {
        val permission = owner(UUID.randomUUID(), "owner")

        commandAdapter.save(permission)

        assertEquals(
            permission,
            queryAdapter.findBySnippetIdAndUserId(permission.snippetId, "owner"),
        )
        assertTrue(queryAdapter.existsBySnippetId(permission.snippetId))
    }

    @Test
    fun `a duplicated permission for the same snippet and user is rejected`() {
        val snippetId = UUID.randomUUID()
        commandAdapter.save(owner(snippetId, "owner"))

        assertFailsWith<PermissionAlreadyExistsException> {
            commandAdapter.save(reader(snippetId, "owner"))
        }
    }

    @Test
    fun `snippet ids are listed by relationship`() {
        val owned = UUID.randomUUID()
        val shared = UUID.randomUUID()
        commandAdapter.save(owner(owned, "user"))
        commandAdapter.save(owner(shared, "other"))
        commandAdapter.save(reader(shared, "user"))

        assertEquals(listOf(owned), queryAdapter.findSnippetIds("user", SnippetRelationship.OWNER))
        assertEquals(
            listOf(shared),
            queryAdapter.findSnippetIds("user", SnippetRelationship.SHARED),
        )
        assertEquals(
            setOf(owned, shared),
            queryAdapter.findSnippetIds("user", SnippetRelationship.ALL).toSet(),
        )
    }

    @Test
    fun `deleting by snippet only removes that snippet permissions`() {
        val deleted = UUID.randomUUID()
        val kept = UUID.randomUUID()
        commandAdapter.save(owner(deleted, "owner"))
        commandAdapter.save(reader(deleted, "reader"))
        commandAdapter.save(owner(kept, "owner"))

        commandAdapter.deleteAllBySnippetId(deleted)

        assertNull(queryAdapter.findBySnippetIdAndUserId(deleted, "owner"))
        assertNull(queryAdapter.findBySnippetIdAndUserId(deleted, "reader"))
        assertTrue(queryAdapter.existsBySnippetId(kept))
    }
}
