package com.example.chatapp.ui.signIn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.data.repo.ConnectivityRepository
import com.example.chatapp.data.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository,
    connectivityRepository: ConnectivityRepository
) : ViewModel() {

    val isConnected = connectivityRepository.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    private var _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state

    fun onSignInResult(result: SignInResult) {
        _state.update {
            it.copy(
                isSignInSuccessful = result.data != null,
                signInError = result.errorMessage
            )
        }
    }


    fun addUserToFireStore(data: User?) {
        val userDataMap = mapOf(
            "userId" to data?.userId,
            "email" to data?.email,
            "username" to data?.username,
            "profilePictureUrl" to data?.profilePictureUrl,
            "bio" to data?.bio
        )
        userRepository.addUserToFireStore(userDataMap)
    }

}