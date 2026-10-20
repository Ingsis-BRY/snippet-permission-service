package com.ingsisbry.permission.adapter.persistence.permission

import com.ingsisbry.permission.domain.PermissionRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "snippet_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["snippet_id", "user_id"])],
    indexes = [Index(columnList = "user_id")],
)
class SnippetPermissionEntity(
    @Id
    val id: UUID,
    @Column(name = "snippet_id", nullable = false)
    val snippetId: UUID,
    @Column(name = "user_id", nullable = false)
    val userId: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: PermissionRole,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
)
