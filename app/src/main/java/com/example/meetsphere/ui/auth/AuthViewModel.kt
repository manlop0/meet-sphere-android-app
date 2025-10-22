package com.example.meetsphere.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetsphere.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1000)
                authRepository.currentUserFlow.collect { user ->
                    _uiState.value =
                        _uiState.value.copy(
                            isAuthenticated = user != null,
                            isAuthCheckComplete = true,
                        )
                }
            }
        }

        fun onEmailChange(email: String) {
            _uiState.value = _uiState.value.copy(email = email)
        }

        fun onPasswordChange(password: String) {
            _uiState.value = _uiState.value.copy(password = password)
        }

        fun onUsernameChange(username: String) {
            _uiState.value = _uiState.value.copy(username = username)
        }

        fun signIn() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val result = authRepository.signInWithEmailAndPassword(_uiState.value.email, _uiState.value.password)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isAuthenticated = true, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message, isLoading = false)
                }
            }
        }

        fun signUp() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val result =
                    authRepository.signUpWithEmailAndPassword(
                        _uiState.value.email,
                        _uiState.value.password,
                        _uiState.value.username,
                    )
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isAuthenticated = true, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message, isLoading = false)
                }
            }
        }

        fun signOut() {
            authRepository.signOut()
        }
    }

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isAuthCheckComplete: Boolean = false,
)
