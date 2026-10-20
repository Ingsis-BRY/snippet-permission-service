package com.ingsisbry.permission.application.query.list

data class ListAccessibleSnippetsQuery(
    val userId: String,
    val relationship: SnippetRelationship = SnippetRelationship.ALL,
)
