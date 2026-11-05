package com.example.autonotes.ui.auth

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

data class AccountUiState(
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    val email: String = auth.currentEmail ?: ""

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                auth.deleteAccount(password)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}