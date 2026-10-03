package com.coffeeos.erp.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coffeeos.erp.core.data.session.SessionManager
import com.coffeeos.erp.core.domain.auth.UserRole
import com.coffeeos.erp.feature.auth.AuthScreen
import com.coffeeos.erp.feature.auth.AuthViewModel
import com.coffeeos.erp.feature.cashier.CashierScreen
import com.coffeeos.erp.feature.cfd.CustomerDisplayScreen
import com.coffeeos.erp.feature.inventory.InventoryScreen
import com.coffeeos.erp.feature.kitchen.KitchenScreen
import com.coffeeos.erp.feature.menu.MenuScreen
import com.coffeeos.erp.feature.owner.OwnerScreen
import com.coffeeos.erp.feature.queueboard.QueueBoardScreen
import com.coffeeos.erp.feature.selforder.SelfOrderScreen
import com.coffeeos.erp.feature.shift.ShiftScreen
import com.coffeeos.erp.feature.supply.SupplyScreen
import com.coffeeos.erp.ui.components.SidebarItem
import com.coffeeos.erp.ui.theme.Dimens
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme

/**
 * Navigasi Adaptif SuKopi POS (DESIGN.md §5 & §6):
 * - >= 840dp: Sidebar penuh 200dp (surface, border kanan 1dp outline) + konten utama
 * - 600–839dp: Navigation Rail (ikon + label)
 * - < 600dp: Bottom navigation
 */
@Composable
fun CoffeeosNavGraph(authVm: AuthViewModel = hiltViewModel()) {
    val authUi by authVm.ui.collectAsState()
    val session = authUi.session
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = screenWidth >= 840 || (isLandscape && screenWidth >= 720)
    val isMedium = !isExpanded && screenWidth >= 600

    // Mode Display Cafe tanpa Login Staf (Self-Order Meja, TV Antrean, CFD Kasir)
    if (session == null) {
        var unauthScreen by remember { mutableStateOf<String?>(null) }
        val defaultOutletId = "OUTLET_01"

        when (unauthScreen) {
            Routes.SELF_ORDER -> {
                SelfOrderScreen(
                    outletId = defaultOutletId,
                    onBack = { unauthScreen = null }
                )
            }
            Routes.QUEUE_BOARD -> {
                QueueBoardScreen(
                    outletId = defaultOutletId,
                    cafeName = "SuKopi",
                    onBack = { unauthScreen = null }
                )
            }
            Routes.CUSTOMER_DISPLAY -> {
                CustomerDisplayScreen(
                    onBack = { unauthScreen = null }
                )
            }
            else -> {
                AuthScreen(
                    onLoggedIn = {
                        // session terisi via StateFlow -> recompose otomatis ke Home.
                    },
                    onOpenSelfOrder = { unauthScreen = Routes.SELF_ORDER },
                    onOpenQueueBoard = { unauthScreen = Routes.QUEUE_BOARD },
                    onOpenCustomerDisplay = { unauthScreen = Routes.CUSTOMER_DISPLAY }
                )
            }
        }
        return
    }

    val outletId = session.outletId
    val home = when (session.role) {
        UserRole.CASHIER -> Routes.CASHIER
        UserRole.KITCHEN -> Routes.KITCHEN
        UserRole.WAREHOUSE -> Routes.INVENTORY
        UserRole.ADMIN_OUTLET, UserRole.OWNER -> Routes.OWNER
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: home

    val navTabs = remember(session.role) {
        getTabsForRole(session.role)
    }

    if (isExpanded) {
        // Layout Tablet / Desktop Landscape: Sidebar 200dp + Konten
        Row(Modifier.fillMaxSize()) {
            SukopiSidebar(
                tabs = navTabs,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onLogout = { authVm.logout() },
                modifier = Modifier
                    .width(Dimens.SidebarWidth)
                    .fillMaxHeight()
            )

            Box(Modifier.weight(1f).fillMaxHeight()) {
                AppNavHost(
                    navController = navController,
                    startDestination = home,
                    outletId = outletId,
                    session = session
                )
            }
        }
    } else if (isMedium) {
        // Layout Medium: Navigation Rail
        Row(Modifier.fillMaxSize()) {
            SukopiNavRail(
                tabs = navTabs,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onLogout = { authVm.logout() },
                modifier = Modifier.fillMaxHeight()
            )

            Box(Modifier.weight(1f).fillMaxHeight()) {
                AppNavHost(
                    navController = navController,
                    startDestination = home,
                    outletId = outletId,
                    session = session
                )
            }
        }
    } else {
        // Layout Compact: Bottom Navigation
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AppNavHost(
                    navController = navController,
                    startDestination = home,
                    outletId = outletId,
                    session = session
                )
            }

            SukopiBottomBar(
                tabs = navTabs,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                },
                onLogout = { authVm.logout() }
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    startDestination: String,
    outletId: String,
    session: SessionManager.Session,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.CASHIER) {
            CashierScreen(
                outletId = outletId,
                cashierName = session.displayName,
                cafeName = "SuKopi",
                outletName = "Outlet Utama",
                onOpenShift = {
                    navController.navigate(Routes.SHIFT) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.KITCHEN) { KitchenScreen(outletId = outletId) }
        composable(Routes.INVENTORY) {
            InventoryScreen(outletId = outletId, uid = session.uid)
        }
        composable(Routes.OWNER) { OwnerScreen(outletId = outletId) }
        composable(Routes.SHIFT) {
            ShiftScreen(outletId = outletId, uid = session.uid, displayName = session.displayName)
        }
        composable(Routes.SUPPLY) {
            SupplyScreen(
                outletId = outletId,
                uid = session.uid,
                isOwner = session.role == UserRole.OWNER || session.role == UserRole.ADMIN_OUTLET
            )
        }
        composable(Routes.MENU) { MenuScreen(outletId = outletId) }
        composable(Routes.SELF_ORDER) {
            SelfOrderScreen(outletId = outletId, onBack = { navController.popBackStack() })
        }
        composable(Routes.QUEUE_BOARD) {
            QueueBoardScreen(outletId = outletId, cafeName = "SuKopi", onBack = { navController.popBackStack() })
        }
        composable(Routes.CUSTOMER_DISPLAY) {
            CustomerDisplayScreen(onBack = { navController.popBackStack() })
        }
    }
}

data class NavTabItem(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val isOther: Boolean = false,
    val badgeCount: Int = 0,
)

private fun getTabsForRole(role: UserRole): List<NavTabItem> = when (role) {
    UserRole.CASHIER -> listOf(
        NavTabItem("Order", Routes.CASHIER, Icons.Filled.PointOfSale),
        NavTabItem("Order List", Routes.KITCHEN, Icons.Filled.Restaurant),
        NavTabItem("Shift", Routes.SHIFT, Icons.Filled.Schedule),
        NavTabItem("Layar QR", Routes.SELF_ORDER, Icons.Filled.QrCode, isOther = true),
        NavTabItem("Antrean TV", Routes.QUEUE_BOARD, Icons.Filled.NotificationsActive, isOther = true)
    )
    UserRole.KITCHEN -> listOf(
        NavTabItem("Barista", Routes.KITCHEN, Icons.Filled.Restaurant),
        NavTabItem("Antrean TV", Routes.QUEUE_BOARD, Icons.Filled.NotificationsActive, isOther = true)
    )
    UserRole.WAREHOUSE -> listOf(
        NavTabItem("Inventory", Routes.INVENTORY, Icons.Filled.Inventory),
        NavTabItem("Supply", Routes.SUPPLY, Icons.Filled.LocalShipping)
    )
    UserRole.ADMIN_OUTLET, UserRole.OWNER -> listOf(
        NavTabItem("Order", Routes.CASHIER, Icons.Filled.PointOfSale),
        NavTabItem("Order List", Routes.KITCHEN, Icons.Filled.Restaurant),
        NavTabItem("Report", Routes.OWNER, Icons.Filled.Assessment),
        NavTabItem("Menu", Routes.MENU, Icons.Filled.RestaurantMenu),
        NavTabItem("Inventory", Routes.INVENTORY, Icons.Filled.Inventory),
        NavTabItem("Shift", Routes.SHIFT, Icons.Filled.Schedule),
        NavTabItem("Supply", Routes.SUPPLY, Icons.Filled.LocalShipping),
        NavTabItem("Layar QR", Routes.SELF_ORDER, Icons.Filled.QrCode, isOther = true),
        NavTabItem("Antrean TV", Routes.QUEUE_BOARD, Icons.Filled.NotificationsActive, isOther = true)
    )
}

/**
 * Sidebar SuKopi POS (DESIGN.md §5 & §6):
 * - Lebar 200dp, surface, border kanan 1dp outline.
 * - Atas: logo maskot cangkir 28dp + "SuKopi - POS" (titleMedium), divider tipis.
 * - Label grup "Menu" dan "Other": labelSmall textTertiary.
 * - Item: tinggi 40dp, pill, ikon 18dp + label labelLarge, gap 10dp, padding horizontal 12dp.
 * - Aktif: latar primary, ikon & teks putih.
 */
@Composable
fun SukopiSidebar(
    tabs: List<NavTabItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mainTabs = tabs.filter { !it.isOther }
    val otherTabs = tabs.filter { it.isOther }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Logo Maskot Cangkir 28dp + "SuKopi - POS"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "SuKopi - POS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Spacer(Modifier.height(16.dp))

            // Grup "Menu"
            Text(
                text = "Menu",
                style = MaterialTheme.typography.labelSmall,
                color = SukopiTheme.colors.textTertiary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {
                mainTabs.forEach { tab ->
                    SidebarItem(
                        icon = tab.icon,
                        label = tab.label,
                        selected = currentRoute == tab.route,
                        badgeCount = tab.badgeCount,
                        onClick = { onNavigate(tab.route) }
                    )
                }
            }

            // Grup "Other" di paling bawah
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Other",
                    style = MaterialTheme.typography.labelSmall,
                    color = SukopiTheme.colors.textTertiary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )

                otherTabs.forEach { tab ->
                    SidebarItem(
                        icon = tab.icon,
                        label = tab.label,
                        selected = currentRoute == tab.route,
                        badgeCount = tab.badgeCount,
                        onClick = { onNavigate(tab.route) }
                    )
                }

                SidebarItem(
                    icon = Icons.Default.Logout,
                    label = "Keluar",
                    selected = false,
                    onClick = onLogout
                )
            }
        }
    }
}

/** Navigation Rail untuk layar medium (tablet portrait / 600-839dp) */
@Composable
fun SukopiNavRail(
    tabs: List<NavTabItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))

        tabs.forEach { tab ->
            NavigationRailItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = SukopiTheme.colors.textSecondary,
                    unselectedTextColor = SukopiTheme.colors.textSecondary
                )
            )
        }

        Spacer(Modifier.weight(1f))

        NavigationRailItem(
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Default.Logout, contentDescription = "Keluar") },
            label = { Text("Keluar", style = MaterialTheme.typography.labelSmall) },
            colors = NavigationRailItemDefaults.colors(
                unselectedIconColor = SukopiTheme.colors.danger,
                unselectedTextColor = SukopiTheme.colors.danger
            )
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** Bottom Bar untuk layar compact (< 600dp) */
@Composable
fun SukopiBottomBar(
    tabs: List<NavTabItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val displayTabs = tabs.take(4) // Maksimal 4 tab utama di layar compact
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            displayTabs.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onNavigate(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = SukopiTheme.colors.textSecondary,
                        unselectedTextColor = SukopiTheme.colors.textSecondary
                    )
                )
            }
            NavigationBarItem(
                selected = false,
                onClick = onLogout,
                icon = { Icon(Icons.Default.Logout, contentDescription = "Keluar") },
                label = { Text("Keluar", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = SukopiTheme.colors.textSecondary,
                    unselectedTextColor = SukopiTheme.colors.textSecondary
                )
            )
        }
    }
}
