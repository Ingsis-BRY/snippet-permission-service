package com.ingsisbry.permission.adapter.web

import com.ingsisbry.permission.adapter.persistence.permission.JpaSnippetPermissionRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
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
    private val owner = "owner"
    private val reader = "reader"

    @BeforeEach
    fun cleanDatabase() {
        repository.deleteAll()
    }

    private fun grantOwnership(userId: String = owner): ResultActionsDsl =
        mockMvc.post("/permissions/owner") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"snippetId":"$snippetId","userId":"$userId"}"""
        }

    private fun share(
        requester: String = owner,
        target: String = reader,
    ): ResultActionsDsl =
        mockMvc.post("/permissions/share") {
            header("X-User-Id", requester)
            contentType = MediaType.APPLICATION_JSON
            content = """{"snippetId":"$snippetId","targetUserId":"$target"}"""
        }

    private fun check(
        userId: String,
        action: String,
    ): ResultActionsDsl =
        mockMvc.get("/permissions/check") {
            param("snippetId", snippetId.toString())
            param("userId", userId)
            param("action", action)
        }

    @Test
    fun `granting ownership returns the created owner permission`() {
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

        grantOwnership("someone-else").andExpect { status { isConflict() } }
    }

    @Test
    fun `granting ownership to a blank user returns bad request`() {
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

        share(requester = "stranger").andExpect { status { isForbidden() } }
    }

    @Test
    fun `sharing without user header returns bad request`() {
        grantOwnership()

        mockMvc
            .post("/permissions/share") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"snippetId":"$snippetId","targetUserId":"$reader"}"""
            }.andExpect { status { isBadRequest() } }
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
                header("X-User-Id", owner)
            }.andExpect { status { isNoContent() } }

        check(reader, "READ").andExpect { jsonPath("\$.allowed") { value(false) } }
    }

    @Test
    fun `revoking a share that does not exist returns not found`() {
        grantOwnership()

        mockMvc
            .delete("/permissions/share/$snippetId/$reader") {
                header("X-User-Id", owner)
            }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `checking an unknown action returns bad request`() {
        check(owner, "EXECUTE").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `accessible snippets are filtered by relationship`() {
        grantOwnership()
        share()

        mockMvc
            .get("/permissions/snippets") {
                param("userId", reader)
                param("relationship", "SHARED")
            }.andExpect {
                status { isOk() }
                jsonPath("\$.snippetIds[0]") { value(snippetId.toString()) }
            }

        mockMvc
            .get("/permissions/snippets") {
                param("userId", reader)
                param("relationship", "OWNER")
            }.andExpect { jsonPath("\$.snippetIds") { isEmpty() } }
    }

    @Test
    fun `owner can delete the snippet permissions but a reader cannot`() {
        grantOwnership()
        share()

        mockMvc
            .delete("/permissions/snippets/$snippetId") {
                header("X-User-Id", reader)
            }.andExpect { status { isForbidden() } }

        mockMvc
            .delete("/permissions/snippets/$snippetId") {
                header("X-User-Id", owner)
            }.andExpect { status { isNoContent() } }

        check(owner, "READ").andExpect { jsonPath("\$.allowed") { value(false) } }
    }
}
