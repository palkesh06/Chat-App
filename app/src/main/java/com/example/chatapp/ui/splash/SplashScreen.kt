package com.example.chatapp.ui.splash


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // Padding around the edges
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo or icon at the top
            Image(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp) // Logo size
            )

            Spacer(modifier = Modifier.height(24.dp)) // Spacing between logo and text

            // Text description with improved styling
            Text(
                text = "Get ready for an amazing experience!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(40.dp)) // Spacing between text and progress indicator

            // Circular progress indicator with customized size and color
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                color = Color.Gray,
                strokeWidth = 4.dp
            )
        }
    }
}