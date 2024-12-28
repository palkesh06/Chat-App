package com.example.chatapp.ui.chat

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.chatapp.ui.home.Message
import com.example.chatapp.ui.home.UserData
import com.google.firebase.Timestamp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    navController: NavController = rememberNavController()
) {
    val viewmodel = hiltViewModel<ChatScreenViewModel>()

    LaunchedEffect(chatId) {
        viewmodel.getChatDetailWithChatId(chatId)
    }

    val state = viewmodel.state.collectAsStateWithLifecycle().value
    val partnerUser = state.chat?.let {
        if (state.currentLoggedInUser?.userId != state.chat.user1?.userId) {
            state.chat.user1 as UserData
        } else {
            state.chat.user2 as UserData
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile image
                        AsyncImage(
                            model = partnerUser?.profileUrl ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.Gray, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        // Username
                        Column {
                            Text(
                                text = partnerUser?.username ?: "Loading..",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = partnerUser?.status ?: "Loading",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigateUp()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {/* work on this show chat options*/ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBarContent(
                state = state,
                chatId = chatId,
                viewModel = viewmodel
            )
        }
    ) { innerPadding ->
        when {
            state.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
                // Disable user interaction while loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x80000000))
                )
            }

            state.error != null -> {
                // Show Error message
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            }

            else -> {
                // Messages section
                state.messages?.let { messageCollection ->
                    val listState = rememberLazyListState()

                    // Automatically scroll to the last message when the list changes
                    LaunchedEffect(messageCollection.messages.size) {
                        if (messageCollection.messages.isNotEmpty()) {
                            listState.animateScrollToItem(messageCollection.messages.size - 1)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding),
                        contentPadding = PaddingValues(8.dp),
                    ) {
                        items(messageCollection.messages) { message ->
                            ChatBubble(
                                message = message,
                                isCurrentUser = state.currentLoggedInUser?.userId == message.sender.userId
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ChatBubble(message: Messages, isCurrentUser: Boolean) {
    // Get the current theme colors
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarContent(
    state: ChatScreenState,
    chatId: String,
    viewModel: ChatScreenViewModel,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .imePadding()
            .padding(horizontal = 10.dp)
            .padding(top = 8.dp),

        ) {
        // Image select button
        IconButton(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            onClick = { /* Select Image */ }
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = "Select Image",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(25.dp)
            )
        ) {

            // Text input
            var message by remember { mutableStateOf("") }
            TextField(
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f),
                value = message,
                onValueChange = { message = it },
                placeholder = { Text(text = "Enter your message") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Select File"
                )

            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send button
            IconButton(
                onClick = {
                    state.currentLoggedInUser?.let {
                        viewModel.sendMessage(
                            chatId = chatId,
                            sender = state.currentLoggedInUser,
                            message = Messages(
                                text = message,
                                timestamp = Timestamp.now(),
                                sender = state.currentLoggedInUser
                            )
                        )
                    }
                    message = ""
                },
                enabled = message.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }

    }
}