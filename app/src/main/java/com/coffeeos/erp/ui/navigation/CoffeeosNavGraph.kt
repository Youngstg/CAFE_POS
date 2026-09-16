package com.coffeeos.erp.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coffeeos.erp.core.domain.auth.UserRole

/**
 * Skeleton navigasi role-based.
 * TODO MVP-1: ganti [role] dengan hasil login Firebase (Custom Claims) dari Room/DataStore.
 */
@Composable
fun CoffeeosNavGraph() {
    val navController = rememberNavController()
    var role by remember { mutableStateOf<UserRole?>(null) }

    if (role == null) {
        AuthGate(onLoggedIn = { loggedInRole ->
            role = loggedInRole
            navController.navigate(
                when (loggedInRole) {
                    UserRole.CASHIER -> Routes.CASHIER
                    UserRole.KITCHEN -> Routes.KITCHEN
                    UserRole.ADMIN_OUTLET, UserRole.OWNER -> Routes.OWNER
                    UserRole.WAREHOUSE -> Routes.INVENTORY
                }
            ) { popUpTo(Routes.AUTH) { inclusive = true } }
        })
        return
    }

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) { /* diganti AuthGate di atas setelah login */ }
        composable(Routes.CASHIER) { RolePlaceholder("Kasir — Terminal POS (offline-first)") }
        composable(Routes.KITCHEN) { RolePlaceholder("Dapur — Kitchen Display realtime") }
        composable(Routes.INVENTORY) { RolePlaceholder("Gudang — Stok, Opname, Terima PO") }
        composable(Routes.OWNER) { RolePlaceholder("Owner — Dashboard + Approve PO + Konflik") }
    }
}

@Composable
private fun AuthGate(onLoggedIn: (UserRole) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("CoffeeOS ERP — pilih role untuk demo (ganti Firebase Auth di MVP-1)")
        UserRole.entries.forEach { role ->
            Button(onClick = { onLoggedIn(role) }) { Text(role.name) }
        }
    }
}

@Composable
private fun RolePlaceholder(title: String) {
    Column(Modifier.fillMaxSize().padding(24.dp)) { Text(title) }
}
