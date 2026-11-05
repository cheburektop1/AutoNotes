package com.example.autonotes.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autonotes.auth.AuthRepository
import com.example.autonotes.auth.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isRegister: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun toggleMode() {
        _state.update { it.copy(isRegister = !it.isRegister, error = null) }
    }

    fun submit(email: String, password: String, passwordRepeat: String) {
        val trimmed = email.trim()
        val isRegister = _state.value.isRegister

        val problem = when {
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Введите корректный адрес почты"
            password.length < 8 -> "Пароль не короче 8 символов"
            isRegister && password != passwordRepeat -> "Пароли не совпадают"
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                if (isRegister) auth.signUp(trimmed, password) else auth.signIn(trimmed, password)
                // При успехе экран сменится сам: его переключает статус сессии
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}