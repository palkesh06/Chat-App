package com.example.chatapp.ui.signIn

data class SignInResult(
    val data: User?,
    val errorMessage: String?
)

data class User(
    val userId: String = "",
    val email: String = "",
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val bio: String? = null
)