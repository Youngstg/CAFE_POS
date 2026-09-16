package com.coffeeos.erp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Login PIN offline. Demo: kasir/kasir? username: owner/admin/kasir/dapur/gudang, PIN: 123456. */
@Composable
fun AuthScreen(vm: AuthViewModel = hiltViewModel(), onLoggedIn: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var username by remember { mutableStateOf("kasir") }
    var pin by remember { mutableStateOf("123456") }

    if (ui.session != null) { onLoggedIn(); return }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CoffeeOS ERP", style = MaterialTheme.typography.headlineMedium)
        Text("Login PIN (offline-ready). Demo: owner/admin/kasir/dapur/gudang")
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        OutlinedTextField(
            value = pin, onValueChange = { pin = it },
            label = { Text("PIN") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { vm.login(username, pin) },
            enabled = !ui.loading, modifier = Modifier.fillMaxWidth()
        ) { Text(if (ui.loading) "Masuk..." else "Masuk") }
    }
}
