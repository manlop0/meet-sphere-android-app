package com.example.meetsphere.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashState {
    object Loading : SplashState()

    object UserLoggedIn : SplashState()

    object UserNotLoggedIn : SplashState()
}

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
        val splashState = _splashState.asStateFlow()

        init {
            checkUserLoggedInStatus()
        }

        private fun checkUserLoggedInStatus() {
            viewModelScope.launch {
                delay(1500)

                if (authRepository.getCurrentUser() != null) {
                    _splashState.value = SplashState.UserLoggedIn
                } else {
                    _splashState.value = SplashState.UserNotLoggedIn
                }
            }
        }
    }
