package com.coffeeos.erp.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.ui.components.OptionChip
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme

/**
 * Layar Autentikasi SuKopi POS (DESIGN.md §1 & §2):
 * - Visual bersih, latar abu sangat muda (#F4F4F5), kartu surface flat tanpa shadow berat.
 * - Tombol utama dan state aktif dengan warna aksen SuKopi (#F04A23).
 */
@Composable
fun AuthScreen(
    vm: AuthViewModel = hiltViewModel(),
    onLoggedIn: () -> Unit,
    onOpenSelfOrder: () -> Unit = {},
    onOpenQueueBoard: () -> Unit = {},
    onOpenCustomerDisplay: () -> Unit = {},
) {
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
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Branding SuKopi (DESIGN.md §6 Logo maskot cangkir 28dp)
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                "SuKopi - POS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Coffee Shop Point of Sale & ERP System",
                style = MaterialTheme.typography.bodyMedium,
                color = SukopiTheme.colors.textSecondary
            )

            Spacer(Modifier.height(8.dp))

            // Tiga mode sebagai pill chips
            Row(
                Modifier.fillMaxWidth().widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptionChip("Demo", mode == 0, { mode = 0; pin = "" }, Modifier.weight(1f))
                OptionChip("Email", mode == 1, { mode = 1; pin = "" }, Modifier.weight(1f))
                OptionChip("PIN Cepat", mode == 2, { mode = 2; username = "" }, Modifier.weight(1f))
            }

            // Kartu Form Login (Flat, surface, 14dp, 1dp outline border)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (mode) {
                        0 -> DemoLoginForm(
                            username = username,
                            pin = pin,
                            onUsername = { username = it },
                            onPin = { if (it.length <= 6) pin = it },
                            onLogin = { vm.loginDemo(username, pin) },
                            loading = ui.loading
                        )
                        1 -> EmailLoginForm(
                            email = email,
                            password = password,
                            onEmail = { email = it },
                            onPassword = { password = it },
                            onLogin = { vm.loginEmail(email, password) },
                            loading = ui.loading
                        )
                        else -> QuickPinForm(
                            pin = pin,
                            onPin = { if (it.length <= 6) pin = it },
                            onLogin = { vm.loginQuickPin(pin) },
                            loading = ui.loading
                        )
                    }
                }
            }

            AnimatedVisibility(visible = ui.error != null, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SukopiTheme.colors.dangerContainer,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
                ) {
                    Text(
                        ui.error ?: "",
                        color = SukopiTheme.colors.danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Akses Cepat Mode Layar Display Cafe (Tanpa Login Staf)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Mode Display Mandiri",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Jalankan perangkat sebagai display tanpa login akun staf:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SukopiTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenSelfOrder,
                            shape = PillShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱 Pesan Meja", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = onOpenQueueBoard,
                            shape = PillShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📺 TV Antrean", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenCustomerDisplay,
                        shape = PillShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖥️ Layar Kasir Pelanggan (CFD)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoLoginForm(
    username: String,
    pin: String,
    onUsername: (String) -> Unit,
    onPin: (String) -> Unit,
    onLogin: () -> Unit,
    loading: Boolean,
) {
    Text("Mode Demo (Evaluasi)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Pilih role: owner · admin · kasir · dapur · gudang\nPIN: 123456",
        style = MaterialTheme.typography.bodySmall,
        color = SukopiTheme.colors.textSecondary
    )
    OutlinedTextField(
        value = username,
        onValueChange = onUsername,
        label = { Text("Username") },
        placeholder = { Text("Contoh: kasir") },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    PinNumpad(pin = pin, onPin = onPin, label = "PIN Demo (123456)")
    Button(
        onClick = onLogin,
        enabled = !loading && username.isNotBlank() && pin.isNotBlank(),
        shape = PillShape,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(if (loading) "Memproses..." else "Masuk Demo", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmailLoginForm(
    email: String,
    password: String,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onLogin: () -> Unit,
    loading: Boolean,
) {
    Text("Login Email Firebase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Membutuhkan koneksi internet sekali untuk otentikasi awal.",
        style = MaterialTheme.typography.bodySmall,
        color = SukopiTheme.colors.textSecondary
    )
    OutlinedTextField(
        value = email, onValueChange = onEmail,
        label = { Text("Email") },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    OutlinedTextField(
        value = password, onValueChange = onPassword,
        label = { Text("Password") },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
    Button(
        onClick = onLogin,
        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
        shape = PillShape,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(if (loading) "Memproses..." else "Masuk dengan Email", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QuickPinForm(
    pin: String,
    onPin: (String) -> Unit,
    onLogin: () -> Unit,
    loading: Boolean,
) {
    Text("PIN Cepat (Offline)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Masukkan PIN yang telah didaftarkan pada perangkat ini.",
        style = MaterialTheme.typography.bodySmall,
        color = SukopiTheme.colors.textSecondary
    )
    PinNumpad(pin = pin, onPin = onPin, label = "Masukkan PIN")
    Button(
        onClick = onLogin,
        enabled = !loading && pin.isNotBlank(),
        shape = PillShape,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(if (loading) "Memverifikasi..." else "Masuk dengan PIN", style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Numpad PIN visual 4×3 (POS-style).
 */
@Composable
fun PinNumpad(
    pin: String,
    onPin: (String) -> Unit,
    label: String,
    maxLength: Int = 6,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SukopiTheme.colors.textSecondary)

        // Indikator PIN (titik-titik bulat)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(maxLength) { i ->
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary
                            else SukopiTheme.colors.outlineStrong
                        )
                )
            }
        }

        // Grid numpad 3 kolom
        val keys = listOf("1","2","3","4","5","6","7","8","9","⌫","0","✓")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 280.dp)) {
            keys.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        NumpadKey(
                            label = key,
                            onClick = {
                                when (key) {
                                    "⌫" -> if (pin.isNotEmpty()) onPin(pin.dropLast(1))
                                    "✓" -> { /* Confirm dikerjakan oleh parent */ }
                                    else -> if (pin.length < maxLength) onPin(pin + key)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            isPrimary = key == "✓"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.4f),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔑", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text("Buat PIN Cepat?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "PIN dipakai kasir untuk masuk offline secara instan.",
            style = MaterialTheme.typography.bodyMedium,
            color = SukopiTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PinNumpad(pin = pin, onPin = { pin = it }, label = "Buat PIN (min. 4 digit)")
        Spacer(Modifier.height(16.dp))
        error?.let {
            Text(it, color = SukopiTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = { onSave(pin) },
            enabled = pin.length >= 4,
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("Simpan PIN", style = MaterialTheme.typography.labelLarge) }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Lewati") }
    }
}
