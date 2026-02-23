package com.sy.firebaseauthtest.data

import com.google.firebase.Timestamp


data class Todo(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val userid: String = "",
    val createdAt: Timestamp = Timestamp.now()
)