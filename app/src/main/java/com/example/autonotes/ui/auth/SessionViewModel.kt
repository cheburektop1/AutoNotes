package com.example.autonotes.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autonotes.auth.AuthRepository
import com.example.autonotes.auth.SessionState
import com.example.autonotes.auth.toSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    auth: AuthRepository
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = auth.sessionStatus
        .map { it.toSessionState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.LOADING)
}