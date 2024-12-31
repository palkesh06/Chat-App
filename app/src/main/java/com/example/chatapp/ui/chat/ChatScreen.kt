package com.example.chatapp.ui.chat

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.chatapp.common.utils.getFormattedTimestamp
import com.example.chatapp.common.utils.getVideoThumbnail
import com.example.chatapp.common.utils.isVideoUrl
import com.example.chatapp.ui.home.UserData
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    navController: NavController = rememberNavController()
) {
    val viewmodel = hiltViewModel<ChatScreenViewModel>()
    val focusManager = LocalFocusManager.current
    val state = viewmodel.state.collectAsStateWithLifecycle().value

    var showUrlInWholeScreen by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatId) {
        viewmodel.getChatDetailWithChatId(chatId)
    }

    BackHandler {
        focusManager.clearFocus()
        navController.navigateUp()
        viewmodel.updateUserStatus(chatId, state.currentLoggedInUser?.userId.toString(), false)
    }

    val partnerUser = state.chat?.let {
        if (state.currentLoggedInUser?.userId != state.chat.user1?.userId) {
            state.chat.user1 as UserData
        } else {
            state.chat.user2 as UserData
        }
    }

    // Observe partner user after fetching chat details
    LaunchedEffect(partnerUser) {
        if (partnerUser != null) {
            viewmodel.observePartnerUser(chatId, partnerUser.userId)
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
                                text = if (state.status == "") "offline" else state.status,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            navController.navigateUp()
                            viewmodel.updateUserStatus(
                                chatId,
                                state.currentLoggedInUser?.userId.toString(),
                                false
                            )
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
                partnerUser = partnerUser,
                chatId = chatId,
                viewModel = viewmodel,
                focusManager = focusManager
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            focusManager.clearFocus() // Clear focus on any tap outside
                        }
                    )
                }
                .padding(innerPadding)
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    )
                }

                state.error != null -> {
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
                                .fillMaxSize(),
                            contentPadding = PaddingValues(5.dp),
                        ) {
                            items(messageCollection.messages) { message ->
                                ChatBubble(
                                    message = message,
                                    isCurrentUser = state.currentLoggedInUser?.userId == message.sender.userId,
                                    onImageClick = { url ->
                                        showUrlInWholeScreen = url
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        showUrlInWholeScreen?.let { mediaUrl ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showUrlInWholeScreen = null }
                    .padding(16.dp)
            ) {
                if (isVideoUrl(mediaUrl)) {
                    val context = LocalContext.current
                    val player = remember {
                        ExoPlayer.Builder(context).build()
                    }

                    val mediaItem = MediaItem.fromUri(mediaUrl)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()

                    BackHandler {
                        player.release()
                        showUrlInWholeScreen = null
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = true // Show video controls
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                } else {
                    BackHandler {
                        showUrlInWholeScreen = null
                    }
                    Image(
                        painter = rememberAsyncImagePainter(model = mediaUrl),
                        contentDescription = "Full-Screen Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape)
                    )
                }
            }
        }
    }
}


@SuppressLint("ProduceStateDoesNotAssignValue")
@Composable
fun ChatBubble(message: Messages, isCurrentUser: Boolean, onImageClick: (String) -> Unit) {
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
            Column(
                modifier = Modifier.wrapContentSize()
            ) {
                // Display media URLs (Images)
                message.mediaUrl.forEach { mediaUrl ->
                    if (isVideoUrl(mediaUrl)) {
                        var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
                        var isLoading by remember { mutableStateOf(true) }

                        LaunchedEffect(mediaUrl) {
                            isLoading = true
                            thumbnail = getVideoThumbnail(mediaUrl)
                            isLoading = false
                        }

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onImageClick(mediaUrl) } // Handle video click
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                thumbnail?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: Text(
                                    text = "Error",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(model = mediaUrl),
                            contentDescription = "Media",
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(top = 5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .height(200.dp) // Adjust the height as needed
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onImageClick(mediaUrl)
                                }
                        )
                    }
                }
                // Main message text
                Text(
                    text = message.text.toString(),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Timestamp
                Text(
                    text = getFormattedTimestamp(message.timestamp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarContent(
    state: ChatScreenState,
    partnerUser: UserData?,
    chatId: String,
    viewModel: ChatScreenViewModel,
    focusManager: FocusManager
) {
    val focusRequester = remember { FocusRequester() }
    val selectedMediaUris = remember { mutableStateListOf<Uri>() }
    val showDuplicateRemovalSnackbar = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        uris?.let {
            val initialUris = selectedMediaUris.size
            val newUris = uris.filter { uri -> !selectedMediaUris.contains(uri) }
            selectedMediaUris.addAll(newUris)

            if (selectedMediaUris.size != (uris.size + initialUris)) {
                showDuplicateRemovalSnackbar.value = true
            }
        }
    }

    Column {
        LazyRow {
            items(selectedMediaUris) { uri ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Selected media",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)) // Ensure image corners match border
                    )

                    // Close button with a slightly improved design
                    IconButton(
                        onClick = { selectedMediaUris.remove(uri) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp) // Control the size of the icon
                            .background(
                                Color.White,
                                shape = CircleShape
                            ) // Circular background for the icon
                            .padding(4.dp) // Padding inside the icon
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove File",
                            tint = Color.Black // Icon color
                        )
                    }
                }
            }
        }
        LaunchedEffect(showDuplicateRemovalSnackbar.value) {
            if (showDuplicateRemovalSnackbar.value) {
                delay(2000) // Wait for 2000ms
                showDuplicateRemovalSnackbar.value = false
            }
        }
        if (showDuplicateRemovalSnackbar.value == true) {
            Snackbar(
                content = { Text("Removed already selected files.") },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable(
                    onClick = {
                        focusManager.clearFocus()
                    }
                )
        ) {
            if (state.isTyping) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Keyboard,
                        contentDescription = "Typing Indicator",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${partnerUser?.username} is typing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
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
                    var isTyping by remember { mutableStateOf(false) }
                    TextField(
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && !isTyping) {
                                    isTyping = true
                                    viewModel.updateUserIsTyping(
                                        state.currentLoggedInUser?.userId.toString(),
                                        chatId,
                                        true
                                    )
                                } else if (!focusState.isFocused && isTyping) {
                                    isTyping = false
                                    viewModel.updateUserIsTyping(
                                        state.currentLoggedInUser?.userId.toString(),
                                        chatId,
                                        false
                                    )
                                }
                            },
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text(text = "Enter your message") },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                    )

                    IconButton(
                        onClick = {
                            launcher.launch(arrayOf("image/*", "video/*"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Select File"
                        )

                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    var isSending by remember { mutableStateOf(false) }

                    // Send button
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            state.currentLoggedInUser?.let { user ->
                                isSending = true
                                viewModel.handleMediaAndSendMessage(
                                    selectedMediaUris = selectedMediaUris,
                                    context = context,
                                    result = { urls ->
                                        viewModel.sendMessage(
                                            chatId = chatId,
                                            sender = user,
                                            message = Messages(
                                                text = message,
                                                timestamp = Timestamp.now(),
                                                sender = user,
                                                mediaUrl = urls
                                            )
                                        )
                                        selectedMediaUris.clear()
                                        message = ""
                                        isSending = false
                                    }
                                )
                            }
                        },
                        enabled = message.isNotBlank() || selectedMediaUris.isNotEmpty()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }

            }
        }
    }
}

