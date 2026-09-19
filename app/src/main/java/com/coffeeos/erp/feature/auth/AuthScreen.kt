package com.coffeeos.erp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * 3 jalur masuk:
 * - Demo (offline, evaluator): owner/admin/kasir/dapur/gudang, PIN 123456.
 * - Email (Firebase, butuh online sekali): role/tenant dari Custom Claims.
 * - PIN cepat (offline): setelah login email + buat PIN.
 * Setelah login email tanpa PIN, ditawari buat PIN dulu (opsional).
 */
@Composable
fun AuthScreen(vm: AuthViewModel = hiltViewModel(), onLoggedIn: () -> Unit) {
    val ui by vm.ui.collectAsState()

    if (ui.session != null && !ui.promptPinSetup) { onLoggedIn(); return }
    if (ui.session != null && ui.promptPinSetup) {
        PinSetupGate(
            onSave = { vm.setupPin(it) },
            onSkip = { vm.skipPinSetup() },
            saved = ui.pinSaved,
            error = ui.error,
            onDone = onLoggedIn
        )
        return
    }

    var mode by remember { mutableStateOf(0) } // 0 demo, 1 email, 2 pin cepat
    var username by remember { mutableStateOf("kasir") }
    var pin by remember { mutableStateOf("123456") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CoffeeOS ERP", style = MaterialTheme.typography.headlineMedium)
        // Tiga jalur sebagai card terpisah (design.md §8.1) — jelas, bukan tab.
        LoginPathCard(
            selected = mode == 0,
            onClick = { mode = 0 },
            title = "Demo PIN",
            desc = "Tanpa Firebase • owner/admin/kasir/dapur/gudang"
        )
        LoginPathCard(
            selected = mode == 1,
            onClick = { mode = 1 },
            title = "Email Firebase",
            desc = "Online sekali • role dari Custom Claims"
        )
        LoginPathCard(
            selected = mode == 2,
            onClick = { mode = 2 },
            title = "PIN Cepat",
            desc = "Offline • butuh sesi + PIN terdaftar"
        )
        when (mode) {
            0 -> {
                Text("Demo offline: owner/admin/kasir/dapur/gudang")
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it },
                    label = { Text("PIN (123456)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Button(
                    onClick = { vm.loginDemo(username, pin) },
                    enabled = !ui.loading, modifier = Modifier.fillMaxWidth()
                ) { Text(if (ui.loading) "Masuk..." else "Masuk Demo") }
            }
            1 -> {
                Text("Firebase (online sekali). Role dari Custom Claims.")
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Button(
                    onClick = { vm.loginEmail(email, password) },
                    enabled = !ui.loading, modifier = Modifier.fillMaxWidth()
                ) { Text(if (ui.loading) "Masuk..." else "Masuk Email") }
            }
            else -> {
                Text("PIN cepat (butuh sesi + PIN terdaftar).")
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it },
                    label = { Text("PIN") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Button(
                    onClick = { vm.loginQuickPin(pin) },
                    enabled = !ui.loading, modifier = Modifier.fillMaxWidth()
                ) { Text(if (ui.loading) "Masuk..." else "Masuk PIN") }
            }
        }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun LoginPathCard(selected: Boolean, onClick: () -> Unit, title: String, desc: String) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PinSetupGate(
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
    saved: Boolean,
    error: String?,
    onDone: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    if (saved) { onDone(); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Buat PIN cepat?", style = MaterialTheme.typography.titleLarge)
        Text("PIN dipakai kasir masuk offline tanpa password panjang. Bisa dilewati.")
        OutlinedTextField(
            value = pin, onValueChange = { pin = it },
            label = { Text("PIN minimal 4 digit") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { onSave(pin) }, modifier = Modifier.fillMaxWidth()) { Text("Simpan PIN") }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Lewati") }
    }
}
