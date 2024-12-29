package com.example.chatapp.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatapp.ui.chat.ChatScreen
import com.example.chatapp.ui.home.HomeScreen
import com.example.chatapp.ui.signIn.GoogleAuthUiClient
import com.example.chatapp.ui.signIn.SignInScreen
import com.example.chatapp.ui.signIn.SignInViewModel
import com.example.chatapp.ui.splash.SplashScreen
import com.example.chatapp.ui.theme.ChatAppTheme
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatAppTheme {
                Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->

                        val navController = rememberNavController()

                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            enterTransition = { slideInHorizontally() },
                            exitTransition = { slideOutHorizontally() }
                        ) {
                            composable("splash") {
                                LaunchedEffect(key1 = Unit) {
                                    delay(500)
                                    if (googleAuthUiClient.getUserData() != null) {
                                        navController.navigate("home") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("sign_in") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                                SplashScreen()
                            }

                            composable("home") {
                                HomeScreen(navController)
                            }

                            composable("sign_in") {
                                val viewmodel = hiltViewModel<SignInViewModel>()
                                val state = viewmodel.state.collectAsStateWithLifecycle().value
                                val isConnected =
                                    viewmodel.isConnected.collectAsStateWithLifecycle().value
                                val launcher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                                    onResult = { result ->
                                        if (result.resultCode == RESULT_OK) {
                                            lifecycleScope.launch {
                                                val singInResult =
                                                    googleAuthUiClient.signInWithIntent(
                                                        intent = result.data ?: return@launch
                                                    )
                                                viewmodel.onSignInResult(singInResult)
                                            }

                                        }
                                    }
                                )

                                LaunchedEffect(key1 = state.isSignInSuccessful) {
                                    if (state.isSignInSuccessful) {
                                        Toast.makeText(
                                            applicationContext,
                                            "Sign in successful",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        viewmodel.addUserToFireStore(googleAuthUiClient.getUserData())
                                        navController.navigate("home") {
                                            popUpTo("sign_in") { inclusive = true }
                                        }
                                    }
                                }

                                SignInScreen(
                                    state = state,
                                    onSignInClick = {
                                        if (isConnected) {
                                            lifecycleScope.launch {
                                                val signInIntent = googleAuthUiClient.signIn()
                                                launcher.launch(
                                                    IntentSenderRequest.Builder(
                                                        signInIntent ?: return@launch
                                                    ).build()
                                                )
                                            }
                                        } else {
                                            Toast.makeText(
                                                applicationContext,
                                                "No internet connection, Please check your connection and try again",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                )
                            }

                            composable(
                                route = "chat/{chatId}",
                                arguments = listOf(
                                    navArgument("chatId") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val chatId = backStackEntry.arguments?.getString("chatId").orEmpty()


                                // Pass the deserialized objects to ChatScreen
                                ChatScreen(chatId, navController)
                            }

                        }
                    }
                }
            }
        }
    }
}