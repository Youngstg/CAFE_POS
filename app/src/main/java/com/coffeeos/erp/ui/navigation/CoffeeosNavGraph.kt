package com.coffeeos.erp.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coffeeos.erp.core.domain.auth.UserRole
import com.coffeeos.erp.feature.auth.AuthScreen
import com.coffeeos.erp.feature.auth.AuthViewModel
import com.coffeeos.erp.feature.cashier.CashierScreen
import com.coffeeos.erp.feature.inventory.InventoryScreen
import com.coffeeos.erp.feature.kitchen.KitchenScreen
import com.coffeeos.erp.feature.menu.MenuScreen
import com.coffeeos.erp.feature.owner.OwnerScreen
import com.coffeeos.erp.feature.shift.ShiftScreen
import com.coffeeos.erp.feature.supply.SupplyScreen

/**
 * Navigasi role-based 1 APK (design.md §7): bottom NavigationBar + ikon,
 * sesedikit mungkin tab untuk Kasir & Dapur.
 * - Kasir: Kasir + Shift. - Dapur: KDS. - Gudang: Stok + Supply.
 * - Admin/Owner: Owner + Menu + Supply + Stok + Shift.
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
                CashierScreen(
                    outletId = outletId,
                    cashierName = session.displayName,
                    onOpenShift = {
                        navController.navigate(Routes.SHIFT) { launchSingleTop = true }
                    }
                )
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
            composable(Routes.MENU) { MenuScreen(outletId = outletId) }
        }
        RoleTabBar(
            role = session.role,
            navController = navController,
            onLogout = { authVm.logout() }
        )
    }
}

private data class Tab(val label: String, val route: String, val icon: ImageVector)

@Composable
private fun RoleTabBar(role: UserRole, navController: NavController, onLogout: () -> Unit) {
    val tabs = when (role) {
        UserRole.CASHIER -> listOf(
            Tab("Kasir", Routes.CASHIER, Icons.Filled.PointOfSale),
            Tab("Shift", Routes.SHIFT, Icons.Filled.Schedule)
        )
        UserRole.KITCHEN -> listOf(Tab("Dapur", Routes.KITCHEN, Icons.Filled.Restaurant))
        UserRole.WAREHOUSE -> listOf(
            Tab("Stok", Routes.INVENTORY, Icons.Filled.Inventory),
            Tab("Supply", Routes.SUPPLY, Icons.Filled.LocalShipping)
        )
        UserRole.ADMIN_OUTLET, UserRole.OWNER -> listOf(
            Tab("Owner", Routes.OWNER, Icons.Filled.Dashboard),
            Tab("Menu", Routes.MENU, Icons.Filled.RestaurantMenu),
            Tab("Supply", Routes.SUPPLY, Icons.Filled.LocalShipping),
            Tab("Stok", Routes.INVENTORY, Icons.Filled.Inventory),
            Tab("Shift", Routes.SHIFT, Icons.Filled.Schedule)
        )
    }
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    navController.navigate(tab.route) { launchSingleTop = true }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Filled.Logout, contentDescription = "Keluar") },
            label = { Text("Keluar") }
        )
    }
}
