package com.coffeeos.erp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coffeeos.erp.core.domain.auth.UserRole
import com.coffeeos.erp.feature.auth.AuthScreen
import com.coffeeos.erp.feature.auth.AuthViewModel
import com.coffeeos.erp.feature.cashier.CashierScreen
import com.coffeeos.erp.feature.inventory.InventoryScreen
import com.coffeeos.erp.feature.kitchen.KitchenScreen
import com.coffeeos.erp.feature.owner.OwnerScreen
import com.coffeeos.erp.feature.shift.ShiftScreen
import com.coffeeos.erp.feature.supply.SupplyScreen

/**
 * Navigasi role-based 1 APK:
 * - Kasir: Kasir + Shift. - Dapur: KDS. - Gudang: Inventory + Supply.
 * - Admin/Owner: Owner + Supply(approve) + Inventory + Shift.
 */
@Composable
fun CoffeeosNavGraph(authVm: AuthViewModel = hiltViewModel()) {
    val authUi by authVm.ui.collectAsState()
    val session = authUi.session
    val navController = rememberNavController()

    if (session == null) {
        AuthScreen(onLoggedIn = {
            // session terisi via StateFlow -> recompose otomatis ke Home.
        })
        return
    }

    val outletId = session.outletId
    val home = when (session.role) {
        UserRole.CASHIER -> Routes.CASHIER
        UserRole.KITCHEN -> Routes.KITCHEN
        UserRole.WAREHOUSE -> Routes.INVENTORY
        UserRole.ADMIN_OUTLET, UserRole.OWNER -> Routes.OWNER
    }

    Column(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = home, modifier = Modifier.weight(1f)) {
            composable(Routes.CASHIER) {
                CashierScreen(outletId = outletId, cashierName = session.displayName)
            }
            composable(Routes.KITCHEN) { KitchenScreen(outletId = outletId) }
            composable(Routes.INVENTORY) {
                InventoryScreen(outletId = outletId, actorId = session.uid)
            }
            composable(Routes.OWNER) { OwnerScreen(outletId = outletId) }
            composable(Routes.SHIFT) {
                ShiftScreen(outletId = outletId, uid = session.uid, displayName = session.displayName)
            }
            composable(Routes.SUPPLY) {
                SupplyScreen(
                    outletId = outletId, actorId = session.uid,
                    canApprove = session.role == UserRole.OWNER || session.role == UserRole.ADMIN_OUTLET
                )
            }
        }
        RoleTabBar(
            role = session.role,
            onNavigate = { navController.navigate(it) { launchSingleTop = true } },
            onLogout = { authVm.logout() }
        )
    }
}

@Composable
private fun RoleTabBar(role: UserRole, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    val tabs = when (role) {
        UserRole.CASHIER -> listOf("Kasir" to Routes.CASHIER, "Shift" to Routes.SHIFT)
        UserRole.KITCHEN -> listOf("Dapur" to Routes.KITCHEN)
        UserRole.WAREHOUSE -> listOf("Stok" to Routes.INVENTORY, "Supply" to Routes.SUPPLY)
        UserRole.ADMIN_OUTLET, UserRole.OWNER ->
            listOf("Owner" to Routes.OWNER, "Supply" to Routes.SUPPLY, "Stok" to Routes.INVENTORY, "Shift" to Routes.SHIFT)
    }
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEach { (label, route) ->
            Button(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
    }
}
