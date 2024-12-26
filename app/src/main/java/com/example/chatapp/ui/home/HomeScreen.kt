package com.example.chatapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.chatapp.ui.home.components.AddUserDialog
import com.google.gson.Gson

@Composable
fun HomeScreen(
    navController: NavController
) {
    val viewmodel = hiltViewModel<HomeScreenViewModel>()
    var showDialog by remember { mutableStateOf(false) }

    val state by viewmodel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = "This is add user button"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.padding(top = 100.dp)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (showDialog) {
                AddUserDialog(onDismiss = { showDialog = false }, onInvite = { email ->
                    viewmodel.inviteUserToChat(email)
                    showDialog = false
                })
            }
        }
        ChatListScreen(navController, state)
    }
}

@Composable
fun ChatListScreen(navController: NavController, state: HomeScreenState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(state.chatList) { chatItem ->
            var partnerUser = if (state.currentLoggedInUser?.userId != chatItem.user1?.userId) {
                chatItem.user1 as UserData
            } else {
                chatItem.user2 as UserData
            }
            ChatCard(
                chatItem = chatItem,
                partnerUser = partnerUser,
                onChatClicked = { chatId ->
                    navController.navigate("chat/${chatItem.chatId}")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChatCard(chatItem: Chat, partnerUser: UserData, onChatClicked: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClicked(chatItem.chatId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Profile Picture
            Image(
                painter = rememberAsyncImagePainter(partnerUser.profileUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name and Last Message
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = partnerUser.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chatItem.lastMessage?.content.toString(),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Timestamp
            Text(
                text = chatItem.lastMessage?.timestamp.toString(),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}