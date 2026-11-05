package com.example.autonotes.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    val currentUserId: String? get() = supabase.auth.currentUserOrNull()?.id
    val currentEmail: String? get() = supabase.auth.currentUserOrNull()?.email

    suspend fun signUp(userEmail: String, userPassword: String) {
        supabase.auth.signUpWith(Email) {
            email = userEmail
            password = userPassword
        }
    }

    suspend fun signIn(userEmail: String, userPassword: String) {
        supabase.auth.signInWith(Email) {
            email = userEmail
            password = userPassword
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // Нет сети: выходим хотя бы на этом устройстве
            supabase.auth.signOut(SignOutScope.LOCAL)
        }
    }

    /**
     * Удаляет аккаунт вместе с данными: проверяет пароль, стирает файлы
     * в хранилище, затем вызывает серверную функцию delete_my_account.
     */
    suspend fun deleteAccount(userPassword: String) {
        val userEmail = currentEmail ?: error("Нет активной сессии")
        signIn(userEmail, userPassword) // повторная проверка пароля
        val uid = currentUserId ?: error("Нет активной сессии")

        deleteAllUserFiles(uid)
        supabase.postgrest.rpc("delete_my_account")
        supabase.auth.signOut(SignOutScope.LOCAL)
    }

    /** Структура файлов: <uid>/<id записи>/<файл>. */
    private suspend fun deleteAllUserFiles(uid: String) {
        val bucket = supabase.storage.from(BUCKET)
        repeat(MAX_PAGES) {
            val folders = bucket.list(uid)
            if (folders.isEmpty()) return
            for (folder in folders) {
                val prefix = "$uid/${folder.name}"
                val files = bucket.list(prefix)
                if (files.isNotEmpty()) {
                    bucket.delete(files.map { "$prefix/${it.name}" })
                }
            }
        }
    }

    companion object {
        const val BUCKET = "recordings"
        private const val MAX_PAGES = 50
    }
}