package com.example.autonotes.auth

import io.github.jan.supabase.auth.status.SessionStatus

enum class SessionState { LOADING, SIGNED_IN, SIGNED_OUT }

fun SessionStatus.toSessionState(): SessionState = when (this) {
    is SessionStatus.Authenticated -> SessionState.SIGNED_IN
    is SessionStatus.RefreshFailure -> SessionState.SIGNED_IN
    is SessionStatus.NotAuthenticated -> SessionState.SIGNED_OUT
    else -> SessionState.LOADING
}