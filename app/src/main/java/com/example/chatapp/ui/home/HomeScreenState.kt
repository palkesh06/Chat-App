package com.example.chatapp.ui.home

import com.example.chatapp.ui.signIn.User

data class HomeScreenState(
    val currentLoggedInUser : User? = null,
    val chatList : List<Chat> = emptyList()
)
