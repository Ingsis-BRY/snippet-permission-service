package com.ingsisbry.permission.adapter.web

import com.ingsisbry.permission.adapter.persistence.permission.JpaSnippetPermissionRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.util.UUID
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class PermissionControllerIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: JpaSnippetPermissionRepository

    private val snippetId = UUID.randomUUID()
    private val owner = "auth0|owner"
    private val reader = "auth0|reader"

    @BeforeEach
    fun cleanDatabase() {
        repository.deleteAll()
    }

    /** A mock Auth0 access token for [subject] with the given scopes. */
    private fun token(
        subject: String,
        vararg scopes: String = arrayOf(READ, WRITE),
    ): RequestPostProcessor =
        jwt()
            .jwt { it.subject(subject) }
            .authorities(scopes.map { SimpleGrantedAuthority("SCOPE_$it") })

    private fun grantOwnership(userId: String = owner): ResultActionsDsl =
        mockMvc.post("/permissions/owner") {
            with(token(userId))
            contentType = MediaType.APPLICATION_JSON
            content = """{"snippetId":"$snippetId"}"""
        }

    private fun share(
        requester: String = owner,
        target: String = reader,
    ): ResultActionsDsl =
        mockMvc.post("/permissions/share") {
            with(token(requester))
            contentType = MediaType.APPLICATION_JSON
            content = """{"snippetId":"$snippetId","targetUserId":"$target"}"""
        }

    private fun check(
        userId: String,
        action: String,
    ): ResultActionsDsl =
        mockMvc.get("/permissions/check") {
            with(token(userId))
            param("snippetId", snippetId.toString())
            param("action", action)
        }

    @Test
    fun `granting ownership makes the caller the owner`() {
        grantOwnership().andExpect {
            status { isCreated() }
            jsonPath("\$.snippetId") { value(snippetId.toString()) }
            jsonPath("\$.userId") { value(owner) }
            jsonPath("\$.role") { value("OWNER") }
        }
    }

    @Test
    fun `granting ownership twice returns conflict`() {
        grantOwnership()

        grantOwnership("auth0|someone-else").andExpect { status { isConflict() } }
    }

    @Test
    fun `granting ownership with a blank subject returns bad request`() {
        grantOwnership(" ").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `shared user can read but cannot write`() {
        grantOwnership()

        share().andExpect {
            status { isCreated() }
            jsonPath("\$.role") { value("READER") }
        }

        check(reader, "READ").andExpect { jsonPath("\$.allowed") { value(true) } }
        check(reader, "WRITE").andExpect { jsonPath("\$.allowed") { value(false) } }
    }

    @Test
    fun `sharing without permission returns forbidden`() {
        grantOwnership()

        share(requester = "auth0|stranger").andExpect { status { isForbidden() } }
    }

    @Test
    fun `sharing with the owner returns bad request`() {
        grantOwnership()

        share(target = owner).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `sharing twice with the same user returns conflict`() {
        grantOwnership()
        share()

        share().andExpect { status { isConflict() } }
    }

    @Test
    fun `revoking a share removes read access`() {
        grantOwnership()
        share()

        mockMvc
            .delete("/permissions/share/$snippetId/$reader") {
                with(token(owner))
            }.andExpect { status { isNoContent() } }

        check(reader, "READ").andExpect { jsonPath("\$.allowed") { value(false) } }
    }

    @Test
    fun `revoking a share that does not exist returns not found`() {
        grantOwnership()

        mockMvc
            .delete("/permissions/share/$snippetId/$reader") {
                with(token(owner))
            }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `checking an unknown action returns bad request`() {
        check(owner, "EXECUTE").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `accessible snippets are those of the caller, filtered by relationship`() {
        grantOwnership()
        share()

        mockMvc
            .get("/permissions/snippets") {
                with(token(reader))
                param("relationship", "SHARED")
            }.andExpect {
                status { isOk() }
                jsonPath("\$.snippetIds[0]") { value(snippetId.toString()) }
            }

        mockMvc
            .get("/permissions/snippets") {
                with(token(reader))
                param("relationship", "OWNER")
            }.andExpect { jsonPath("\$.snippetIds") { isEmpty() } }
    }

    @Test
    fun `owner can delete the snippet permissions but a reader cannot`() {
        grantOwnership()
        share()

        mockMvc
            .delete("/permissions/snippets/$snippetId") {
                with(token(reader))
            }.andExpect { status { isForbidden() } }

        mockMvc
            .delete("/permissions/snippets/$snippetId") {
                with(token(owner))
            }.andExpect { status { isNoContent() } }

        check(owner, "READ").andExpect { jsonPath("\$.allowed") { value(false) } }
    }

    @Test
    fun `requests without a token are rejected with 401`() {
        mockMvc
            .get("/permissions/check") {
                param("snippetId", snippetId.toString())
                param("action", "READ")
            }.andExpect { status { isUnauthorized() } }

        mockMvc
            .post("/permissions/owner") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"snippetId":"$snippetId"}"""
            }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `a token without the write scope cannot change permissions`() {
        mockMvc
            .post("/permissions/owner") {
                with(token(owner, READ))
                contentType = MediaType.APPLICATION_JSON
                content = """{"snippetId":"$snippetId"}"""
            }.andExpect { status { isForbidden() } }

        repository.findAll().let { assert(it.isEmpty()) }
    }

    @Test
    fun `a token without the read scope cannot read permissions`() {
        mockMvc
            .get("/permissions/snippets") {
                with(token(owner, WRITE))
            }.andExpect { status { isForbidden() } }
    }

    private companion object {
        const val READ = "read:snippets"
        const val WRITE = "write:snippets"
    }
}
