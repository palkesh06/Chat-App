package com.example.chatapp.ui.home

import com.example.chatapp.ui.signIn.User

data class HomeScreenState(
    val currentLoggedInUser : User? = null,
    val chatList : List<Chat> = emptyList(),
    var isAddingToStory : Boolean = false,
    val stories: List<Story> = listOf(Story("Add Story", ""))
)

data class Story(
    val name: String? = null,
    val imageUrl: String? = null
)
