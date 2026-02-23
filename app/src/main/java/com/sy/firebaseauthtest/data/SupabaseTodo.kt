package com.sy.firebaseauthtest.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTodo(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    val title: String = "",
    val done: Boolean = false,
    @SerialName("created_at")
    val createdAt: String = ""
)