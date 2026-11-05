package com.example.autonotes.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("Назад") }

            Text("Аккаунт", style = MaterialTheme.typography.headlineMedium)
            Text(
                viewModel.email,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant
            )

            state.error?.let { Text(it, color = scheme.error) }

            Button(
                onClick = viewModel::signOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) { Text("Выйти") }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = scheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Удаление аккаунта", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Аккаунт и все ваши данные в облаке будут удалены безвозвратно.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { password = ""; showDelete = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Удалить аккаунт и данные", color = scheme.error) }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.loading) showDelete = false },
            title = { Text("Удалить аккаунт?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Введите пароль для подтверждения. Это действие нельзя отменить.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.error?.let { Text(it, color = scheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAccount(password) },
                    enabled = password.isNotEmpty() && !state.loading
                ) { Text(if (state.loading) "Удаление…" else "Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }, enabled = !state.loading) {
                    Text("Отмена")
                }
            }
        )
    }
}