package com.ingsisbry.permission.adapter.web

import com.ingsisbry.permission.adapter.web.dto.AccessibleSnippetsResponse
import com.ingsisbry.permission.adapter.web.dto.GrantOwnershipRequest
import com.ingsisbry.permission.adapter.web.dto.PermissionCheckResponse
import com.ingsisbry.permission.adapter.web.dto.PermissionResponse
import com.ingsisbry.permission.adapter.web.dto.ShareSnippetRequest
import com.ingsisbry.permission.application.command.delete.DeleteSnippetPermissions
import com.ingsisbry.permission.application.command.delete.DeleteSnippetPermissionsCommand
import com.ingsisbry.permission.application.command.grant.GrantOwnership
import com.ingsisbry.permission.application.command.grant.GrantOwnershipCommand
import com.ingsisbry.permission.application.command.revoke.RevokeShare
import com.ingsisbry.permission.application.command.revoke.RevokeShareCommand
import com.ingsisbry.permission.application.command.share.ShareSnippet
import com.ingsisbry.permission.application.command.share.ShareSnippetCommand
import com.ingsisbry.permission.application.query.check.CheckPermission
import com.ingsisbry.permission.application.query.check.CheckPermissionQuery
import com.ingsisbry.permission.application.query.list.ListAccessibleSnippets
import com.ingsisbry.permission.application.query.list.ListAccessibleSnippetsQuery
import com.ingsisbry.permission.application.query.list.SnippetRelationship
import com.ingsisbry.permission.domain.SnippetAction
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * The caller is always the user of the access token (its `sub` claim): nobody can act, or ask,
 * on behalf of another user.
 */
@RestController
@RequestMapping("/permissions")
class PermissionController(
    private val grantOwnership: GrantOwnership,
    private val shareSnippet: ShareSnippet,
    private val revokeShare: RevokeShare,
    private val deleteSnippetPermissions: DeleteSnippetPermissions,
    private val checkPermission: CheckPermission,
    private val listAccessibleSnippets: ListAccessibleSnippets,
) {
    @PostMapping("/owner")
    @ResponseStatus(HttpStatus.CREATED)
    fun grantOwnership(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: GrantOwnershipRequest,
    ): PermissionResponse =
        PermissionResponse.from(
            grantOwnership.execute(
                GrantOwnershipCommand(
                    snippetId = request.snippetId,
                    userId = jwt.userId,
                ),
            ),
        )

    @PostMapping("/share")
    @ResponseStatus(HttpStatus.CREATED)
    fun share(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: ShareSnippetRequest,
    ): PermissionResponse =
        PermissionResponse.from(
            shareSnippet.execute(
                ShareSnippetCommand(
                    snippetId = request.snippetId,
                    requesterId = jwt.userId,
                    targetUserId = request.targetUserId,
                ),
            ),
        )

    @DeleteMapping("/share/{snippetId}/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeShare(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable snippetId: UUID,
        @PathVariable targetUserId: String,
    ) {
        revokeShare.execute(
            RevokeShareCommand(
                snippetId = snippetId,
                requesterId = jwt.userId,
                targetUserId = targetUserId,
            ),
        )
    }

    @GetMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    fun check(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam snippetId: UUID,
        @RequestParam action: SnippetAction,
    ): PermissionCheckResponse =
        PermissionCheckResponse(
            allowed = checkPermission.execute(CheckPermissionQuery(snippetId, jwt.userId, action)),
        )

    @GetMapping("/snippets")
    @ResponseStatus(HttpStatus.OK)
    fun listAccessibleSnippets(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "ALL") relationship: SnippetRelationship,
    ): AccessibleSnippetsResponse =
        AccessibleSnippetsResponse(
            snippetIds =
                listAccessibleSnippets.execute(
                    ListAccessibleSnippetsQuery(jwt.userId, relationship),
                ),
        )

    @DeleteMapping("/snippets/{snippetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSnippetPermissions(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable snippetId: UUID,
    ) {
        deleteSnippetPermissions.execute(
            DeleteSnippetPermissionsCommand(
                snippetId = snippetId,
                requesterId = jwt.userId,
            ),
        )
    }
}

/** The caller's id. A token without `sub` gives a blank id, which the use cases reject. */
private val Jwt.userId: String
    get() = subject.orEmpty()
