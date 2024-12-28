package com.example.chatapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.chatapp.ui.signIn.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val viewmodel = hiltViewModel<HomeScreenViewModel>()
    var showDialog by remember { mutableStateOf(false) }

    val state by viewmodel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            HomeTopBar(state.currentLoggedInUser?.username.toString())
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* Home Action */ }) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                    )
                }

                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat"
                    )
                    Text(text = "New Chat")
                }

                IconButton(onClick = { /* Profile Action */ }) {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (showDialog) {
                AddUserDialog(onDismiss = { showDialog = false }, onInvite = { email ->
                    viewmodel.inviteUserToChat(email)
                    showDialog = false
                })
            }
        }
        ChatListScreen(
            modifier = Modifier.padding(innerPadding),
            navController,
            state
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(username: String) {
    TopAppBar(
        title = {
            Text(
                text = "Hello" +
                        ", $username",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(onClick = { /* Search Action */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = Color.Black,
            navigationIconContentColor = Color.Black,
            actionIconContentColor = Color.Black
        )
    )
}

data class Story(val name: String, val imageUrl: String, val isAddStory: Boolean = false)

@Composable
fun ChatListScreen(
    modifier: Modifier,
    navController: NavController,
    state: HomeScreenState
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Stories Section
        StoriesSection(
            stories = listOf(
                Story("Add Story", "", isAddStory = true),
                Story("Terry", "https://example.com/terry.jpg"),
                Story("Craig", "https://example.com/craig.jpg"),
                Story("Roger", "https://example.com/roger.jpg"),
                Story("Nolan", "https://example.com/nolan.jpg")
            )
        )

        // Chats Header with Requests Button
        SectionHeaderWithButton(
            title = "Chats",
            buttonText = "Requests",
            onButtonClick = { /* Handle Requests button click */ }
        )


        // Chats Section
        ChatList(
            chats = state.chatList,
            currentUser = state.currentLoggedInUser,
            navController = navController
        )
    }
}

@Composable
fun StoriesSection(stories: List<Story>) {
    LazyRow(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stories) { story ->
            StoryItem(story)
        }
    }
}

@Composable
fun SectionHeaderWithButton(
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp), // Added space on top and bottom
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fontSize = 32.sp,
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f) // Pushes the button to the end
        )

        Button(
            onClick = onButtonClick,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Text(text = buttonText)
        }
    }
}

@Composable
fun ChatList(
    chats: List<Chat>,
    currentUser: User?,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val c = chats + chats + chats + chats + chats + chats
        items(c) { chat ->
            val partnerUser = if (currentUser?.userId != chat.user1?.userId) {
                chat.user1
            } else {
                chat.user2
            }

            if (partnerUser != null) {
                ChatCard(
                    chatItem = chat,
                    partnerUser = partnerUser,
                    onChatClicked = { chatId ->
                        navController.navigate("chat/${chat.chatId}")
                    }
                )
            }
        }
    }
}

@Composable
fun StoryItem(story: Story) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (story.isAddStory) Color.Gray else Color.LightGray,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (story.isAddStory) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Story", tint = Color.White)
                }

            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = story.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.clip(CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = story.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ChatCard(chatItem: Chat, partnerUser: UserData, onChatClicked: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClicked(chatItem.chatId) },
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
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