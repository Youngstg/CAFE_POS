@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.cashier

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.data.repo.CartLine
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.components.FirebaseConnectionDialog
import com.coffeeos.erp.core.sync.FirebaseHealthStatus
import com.coffeeos.erp.ui.components.CategoryCard
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.MiniKpi
import com.coffeeos.erp.ui.components.OptionChip
import com.coffeeos.erp.ui.components.ProductCard
import com.coffeeos.erp.ui.components.ProductCustomizerDialog
import com.coffeeos.erp.ui.components.animateItem
import com.coffeeos.erp.ui.theme.Dimens
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Layar Kasir SuKopi POS (DESIGN.md §7.1).
 * Mengimplementasikan tata letak Expanded (Sidebar + Konten + Panel Kanan 320dp)
 * dan tata letak responsif untuk layar medium/compact.
 */
@Composable
fun CashierScreen(
    outletId: String,
    cashierName: String,
    cafeName: String = "SuKopi",
    outletName: String = "Outlet Utama",
    onOpenShift: () -> Unit = {},
    vm: CashierViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val menus by remember(outletId) { vm.menus(outletId) }.collectAsState()
    val hasShift by vm.hasShift.collectAsState()
    val kpi by vm.kpi.collectAsState()
    val query by vm.query.collectAsState()
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = config.screenWidthDp >= 840 || (isLandscape && config.screenWidthDp >= 720)
    val context = LocalContext.current
    val firebaseHealthState by vm.firebaseHealthState.collectAsState()
    var showFirebaseDialog by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Semua") }
    var showReceiptPreview by remember { mutableStateOf(false) }

    LaunchedEffect(outletId) {
        vm.refreshPending()
        vm.checkShift(outletId)
        vm.startKpiPolling(outletId)
        vm.checkFirebaseHealth()
    }

    // Dialog status & test koneksi Firebase
    if (showFirebaseDialog) {
        FirebaseConnectionDialog(
            state = firebaseHealthState,
            onTestClick = { vm.checkFirebaseHealth() },
            onSyncClick = { vm.syncNow() },
            onDismiss = { showFirebaseDialog = false }
        )
    }

    // Dialog konfirmasi checkout
    if (ui.showCheckoutDialog) {
        CheckoutDialog(
            cart = ui.cart,
            selectedPayment = ui.selectedPayment,
            orderType = ui.orderType,
            customerName = ui.customerName,
            customerPhone = ui.customerPhone,
            applyTax = ui.applyTax,
            onPaymentSelect = { vm.selectPaymentMethod(it) },
            onOrderTypeChange = { vm.setOrderType(it) },
            onCustomerNameChange = { vm.setCustomerName(it) },
            onCustomerPhoneChange = { vm.setCustomerPhone(it) },
            onApplyTaxChange = { vm.setApplyTax(it) },
            onConfirm = { vm.pay(outletId, cashierName, cafeName, outletName) },
            onDismiss = { vm.dismissCheckout() }
        )
    }

    // Dialog Kustomisasi Minuman (DESIGN.md §7.2)
    ui.customizingMenu?.let { menu ->
        ProductCustomizerDialog(
            menu = menu,
            onConfirm = { mods, qty ->
                vm.addCustomizedToCart(menu, mods, qty)
            },
            onDismiss = { vm.dismissCustomizing() }
        )
    }

    // Dialog daftar pesanan ditahan (Hold Bill)
    if (ui.showHeldDialog) {
        HeldCartsDialog(
            heldCarts = ui.heldCarts,
            onResume = { vm.resumeHeldCart(it) },
            onDelete = { vm.deleteHeldCart(it) },
            onDismiss = { vm.setShowHeldDialog(false) }
        )
    }

    // Preview Struk Digital
    if (showReceiptPreview && ui.lastReceiptText != null) {
        ReceiptPreviewDialog(
            receiptText = ui.lastReceiptText ?: "",
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Struk Pesanan ${ui.lastOrderId ?: ""}")
                    putExtra(Intent.EXTRA_TEXT, ui.lastReceiptText)
                }
                context.startActivity(Intent.createChooser(intent, "Kirim Struk ke Pelanggan"))
            },
            onDismiss = { showReceiptPreview = false }
        )
    }

    // Kunci lembut bila shift belum dibuka
    if (hasShift == false) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("☕", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 48.sp))
            Spacer(Modifier.height(16.dp))
            Text("Shift belum dibuka", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Buka shift terlebih dahulu sebelum mulai melayani pesanan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpenShift,
                shape = PillShape,
                modifier = Modifier.widthIn(min = 220.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Buka Shift Sekarang", style = MaterialTheme.typography.labelLarge)
            }
        }
        return
    }

    val categories = remember(menus) { listOf("Semua") + menus.map { it.category }.distinct().sorted() }
    val visible = remember(menus, category, query) {
        menus.filter {
            (category == "Semua" || it.category == category) &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }

    // Layout Utama
    if (isExpanded) {
        Row(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Konten Utama (Fleksibel): Header + Kategori + Grid Produk
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Halaman (DESIGN.md §6 & §7.1)
                CashierHeader(
                    query = query,
                    onQuery = { vm.setQuery(it) },
                    pendingSync = ui.pendingSync,
                    healthStatus = firebaseHealthState.status,
                    onOpenFirebase = {
                        vm.checkFirebaseHealth()
                        showFirebaseDialog = true
                    }
                )

                // Baris Kartu Kategori
                CategoryCardsRow(
                    categories = categories,
                    menus = menus,
                    selected = category,
                    onSelect = { category = it }
                )

                // Grid Produk 3 Kolom
                MenuProductGrid(
                    menus = visible,
                    columns = 3,
                    onAdd = { vm.addToCart(it) },
                    onCustomize = { vm.startCustomizing(it) },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            // Panel Keranjang Kanan (DESIGN.md §5 & §7.1: 320dp)
            CartPanel(
                outletId = outletId,
                cashierName = cashierName,
                vm = vm,
                modifier = Modifier
                    .width(Dimens.CartPanelWidth)
                    .fillMaxHeight()
            )
        }
    } else {
        // Mode Compact / Portrait Phone
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CashierHeader(
                query = query,
                onQuery = { vm.setQuery(it) },
                pendingSync = ui.pendingSync,
                healthStatus = firebaseHealthState.status,
                onOpenFirebase = {
                    vm.checkFirebaseHealth()
                    showFirebaseDialog = true
                }
            )

            kpi?.let {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpi(it.revenue.toRupiah(), "Omzet shift", Modifier.widthIn(min = 130.dp), accentColor = SukopiTheme.colors.success)
                    MiniKpi("${it.openTickets}", "Tiket aktif", Modifier.widthIn(min = 100.dp), accentColor = MaterialTheme.colorScheme.primary)
                    MiniKpi(
                        "${it.critical}", "Stok STOP", Modifier.widthIn(min = 100.dp),
                        accentColor = if (it.critical > 0) SukopiTheme.colors.danger else SukopiTheme.colors.success
                    )
                }
            }

            CategoryCardsRow(
                categories = categories,
                menus = menus,
                selected = category,
                onSelect = { category = it }
            )

            MenuProductGrid(
                menus = visible,
                columns = 2,
                onAdd = { vm.addToCart(it) },
                onCustomize = { vm.startCustomizing(it) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            AnimatedVisibility(
                visible = ui.cart.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                CartPanel(
                    outletId = outletId,
                    cashierName = cashierName,
                    vm = vm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Header Halaman Kasir (DESIGN.md §6 Page Header & §7.1):
 * - Kiri: Judul "Let's do your best today 🚀" + Subjudul tanggal
 * - Kanan: Search bar ~220dp, tinggi 40dp, pill + Tombol filter (lingkaran 40dp)
 */
@Composable
private fun CashierHeader(
    query: String,
    onQuery: (String) -> Unit,
    pendingSync: Int,
    healthStatus: FirebaseHealthStatus,
    onOpenFirebase: () -> Unit,
) {
    val currentDateText = remember {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))
        } catch (_: Exception) {
            "Today"
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kiri: Judul dan Tanggal + Badge Indikator Cloud / Firebase
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Let's do your best today 🚀",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentDateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SukopiTheme.colors.textSecondary
                )

                // Badge Status Koneksi Cloud / Firebase interaktif
                Surface(
                    shape = PillShape,
                    color = when (healthStatus) {
                        FirebaseHealthStatus.ONLINE -> SukopiTheme.colors.success.copy(alpha = 0.12f)
                        FirebaseHealthStatus.CHECKING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        FirebaseHealthStatus.DEGRADED -> SukopiTheme.colors.warning.copy(alpha = 0.12f)
                        FirebaseHealthStatus.OFFLINE -> SukopiTheme.colors.danger.copy(alpha = 0.12f)
                        else -> SukopiTheme.colors.danger.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (healthStatus) {
                            FirebaseHealthStatus.ONLINE -> SukopiTheme.colors.success.copy(alpha = 0.35f)
                            FirebaseHealthStatus.CHECKING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            FirebaseHealthStatus.DEGRADED -> SukopiTheme.colors.warning.copy(alpha = 0.35f)
                            FirebaseHealthStatus.OFFLINE -> SukopiTheme.colors.danger.copy(alpha = 0.35f)
                            else -> SukopiTheme.colors.danger.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { onOpenFirebase() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (healthStatus == FirebaseHealthStatus.OFFLINE) Icons.Filled.WifiOff else Icons.Filled.Wifi,
                            contentDescription = "Status Firebase",
                            tint = when (healthStatus) {
                                FirebaseHealthStatus.ONLINE -> SukopiTheme.colors.success
                                FirebaseHealthStatus.CHECKING -> MaterialTheme.colorScheme.primary
                                FirebaseHealthStatus.DEGRADED -> SukopiTheme.colors.warning
                                FirebaseHealthStatus.OFFLINE -> SukopiTheme.colors.danger
                                else -> SukopiTheme.colors.danger
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when (healthStatus) {
                                FirebaseHealthStatus.ONLINE -> if (pendingSync > 0) "$pendingSync pending" else "Cloud Terhubung"
                                FirebaseHealthStatus.CHECKING -> "Mengecek Cloud..."
                                FirebaseHealthStatus.DEGRADED -> "Latensi Lambat"
                                FirebaseHealthStatus.OFFLINE -> "Offline Mode"
                                else -> "Offline Mode"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when (healthStatus) {
                                FirebaseHealthStatus.ONLINE -> if (pendingSync > 0) SukopiTheme.colors.danger else SukopiTheme.colors.success
                                FirebaseHealthStatus.CHECKING -> MaterialTheme.colorScheme.primary
                                FirebaseHealthStatus.DEGRADED -> SukopiTheme.colors.warning
                                FirebaseHealthStatus.OFFLINE -> SukopiTheme.colors.danger
                                else -> SukopiTheme.colors.danger
                            }
                        )
                    }
                }
            }
        }

        // Kanan: Search bar pill (~220dp, tinggi 40dp) + Tombol filter lingkaran 40dp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                placeholder = {
                    Text(
                        "Search...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SukopiTheme.colors.textTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = SukopiTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = PillShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .height(Dimens.SearchHeight)
            )

            // Tombol filter: lingkaran 40dp, border outline, ikon sliders horizontal
            Surface(
                onClick = {},
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Baris Kartu Kategori (DESIGN.md §6 Kartu Kategori & §7.1):
 * - 5 kartu sejajar (atau scrollable bila lebih), tinggi ~64dp, radius 14dp.
 * - Isi: ikon 16dp, nama labelLarge, jumlah ("6 items") labelSmall textTertiary.
 */
@Composable
private fun CategoryCardsRow(
    categories: List<String>,
    menus: List<MenuEntity>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { cat ->
            val count = if (cat == "Semua") menus.size else menus.count { it.category == cat }
            val icon = when {
                cat.contains("kopi", true) || cat.contains("coffee", true) -> Icons.Default.LocalCafe
                cat.contains("dingin", true) || cat.contains("cold", true) -> Icons.Default.Nightlife
                cat.contains("panas", true) || cat.contains("hot", true) -> Icons.Default.LocalFireDepartment
                cat.contains("makan", true) || cat.contains("food", true) -> Icons.Default.Restaurant
                cat.contains("promo", true) -> Icons.Default.LocalOffer
                cat.contains("snack", true) -> Icons.Default.Fastfood
                else -> Icons.Default.Coffee
            }
            CategoryCard(
                name = cat,
                itemCount = count,
                selected = selected == cat,
                icon = icon,
                onClick = { onSelect(cat) }
            )
        }
    }
}

/**
 * Grid Produk SuKopi (DESIGN.md §6 Kartu Produk & §7.1):
 * Menggunakan kartu produk bersih ber-radius 14dp, border 1dp outline, gambar 1:1 radius 12dp.
 */
@Composable
private fun MenuProductGrid(
    menus: List<MenuEntity>,
    columns: Int = 3,
    onAdd: (MenuEntity) -> Unit,
    onCustomize: (MenuEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (menus.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            EmptyState(
                glyph = "☕",
                title = "No products found",
                hint = "Coba kata kunci atau kategori lain."
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            items(menus, key = { it.id }) { menu ->
                val isDrink = menu.category.contains("kopi", ignoreCase = true) ||
                        menu.category.contains("minuman", ignoreCase = true) ||
                        menu.category.contains("coffee", ignoreCase = true) ||
                        menu.category.contains("tea", ignoreCase = true)

                ProductCard(
                    menu = menu,
                    stockCount = if (menu.isAvailable) 53 else 0,
                    onAdd = { onAdd(menu) },
                    onCustomize = if (isDrink) { { onCustomize(menu) } } else null,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

/**
 * Panel Keranjang SuKopi POS (DESIGN.md §5, §6, §7.1):
 * - Lebar 320dp, surface, radius 20dp, border 1dp outline.
 * - 1. Label "Customer Name" + ikon pensil; kecil ID (#12132312)
 * - 2. Segmented Dine in | Take away
 * - 3. "Where will you eat :" lalu 2 chip Indoor | Outdoor
 * - 4. "Your order :"
 * - 5. Daftar item dengan thumbnail 48dp (radius 10dp), stepper bulat 28dp
 * - 6. Footer: Subtotal, diskon, pajak PB1, total (titleLarge), tombol primary pill 48dp
 */
@Composable
private fun CartPanel(
    outletId: String,
    cashierName: String,
    vm: CashierViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsState()
    val subtotal = ui.cart.sumOf { it.qty.toLong() * it.unitPrice }
    val tax = if (ui.applyTax) (subtotal * 10 / 100) else 0L
    val total = subtotal + tax

    var whereToEat by remember { mutableStateOf("Indoor") }
    var showNameDialog by remember { mutableStateOf(false) }

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(ui.customerName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Customer Name / Table", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = { Text("misal: Budi / Meja 04") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.setCustomerName(tempName)
                        showNameDialog = false
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Batal") }
            }
        )
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Customer Name + ID
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (ui.customerName.isNotBlank()) ui.customerName else "Customer Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { showNameDialog = true },
                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Nama",
                                tint = SukopiTheme.colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "#ORD-${(subtotal % 100000).toString().padStart(6, '0')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SukopiTheme.colors.textTertiary
                    )
                }

                // Tombol aksi bill ditahan
                if (ui.heldCarts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { vm.setShowHeldDialog(true) },
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Hold (${ui.heldCarts.size})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 2. Segmented Control: Dine in | Take away (tinggi 36dp, pill, border outline)
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(Modifier.fillMaxSize()) {
                    val isDineIn = ui.orderType == "DINE_IN"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(PillShape)
                            .background(if (isDineIn) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { vm.setOrderType("DINE_IN") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Dine in",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isDineIn) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDineIn) MaterialTheme.colorScheme.primary else SukopiTheme.colors.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(PillShape)
                            .background(if (!isDineIn) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { vm.setOrderType("TAKE_AWAY") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Take away",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (!isDineIn) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isDineIn) MaterialTheme.colorScheme.primary else SukopiTheme.colors.textSecondary
                        )
                    }
                }
            }

            // 3. Where will you eat : Indoor | Outdoor
            if (ui.orderType == "DINE_IN") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Where will you eat :",
                        style = MaterialTheme.typography.labelMedium,
                        color = SukopiTheme.colors.textSecondary
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OptionChip(
                            label = "Indoor",
                            selected = whereToEat == "Indoor",
                            onClick = { whereToEat = "Indoor" },
                            modifier = Modifier.weight(1f)
                        )
                        OptionChip(
                            label = "Outdoor",
                            selected = whereToEat == "Outdoor",
                            onClick = { whereToEat = "Outdoor" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Section "Your order :"
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your order :",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (ui.cart.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = SukopiTheme.colors.danger,
                        modifier = Modifier.clickable { vm.clearCart() }
                    )
                }
            }

            // 5. Area Daftar Item
            if (ui.cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = SukopiTheme.colors.textTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No items yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SukopiTheme.colors.textTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ui.cart, key = { "${it.menuId}_${it.variant}" }) { line ->
                        CartLineRow(
                            line = line,
                            onIncrease = { vm.increaseInCart(line.menuId) },
                            onDecrease = { vm.decreaseFromCart(line.menuId) }
                        )
                    }
                }
            }

            // 6. Footer Rincian Harga & Tombol Checkout
            if (ui.cart.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Subtotal
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = SukopiTheme.colors.textSecondary)
                        Text(subtotal.toRupiah(), style = MaterialTheme.typography.bodyMedium)
                    }

                    // Pajak PB1
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PB1 Tax (10%)", style = MaterialTheme.typography.bodyMedium, color = SukopiTheme.colors.textSecondary)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = ui.applyTax,
                                onCheckedChange = { vm.setApplyTax(it) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        Text(tax.toRupiah(), style = MaterialTheme.typography.bodyMedium)
                    }

                    // Total
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            total.toRupiah(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Tombol Primary Full-width 48dp Pill
                Button(
                    onClick = { vm.requestCheckout() },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (ui.busy) "Processing..." else "Proceed to Payment — ${total.toRupiah()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Baris item keranjang (DESIGN.md §7.1):
 * Thumbnail 48dp (radius 10dp), nama titleMedium, ringkasan opsi labelSmall textSecondary,
 * harga, stepper bulat 28dp (- 1 +).
 */
@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail 48dp (radius 10dp)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = line.name.firstOrNull()?.uppercase() ?: "☕",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Info: Nama + Ringkasan Opsi + Harga
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = line.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!line.variant.isNullOrBlank()) {
                    Text(
                        text = line.variant,
                        style = MaterialTheme.typography.labelSmall,
                        color = SukopiTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = (line.unitPrice * line.qty).toRupiah(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Stepper Bulat 28dp (- 1 +)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "${line.qty}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 20.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/** Dialog konfirmasi checkout */
@Composable
private fun CheckoutDialog(
    cart: List<CartLine>,
    selectedPayment: String,
    orderType: String,
    customerName: String,
    customerPhone: String,
    applyTax: Boolean,
    onPaymentSelect: (String) -> Unit,
    onOrderTypeChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onApplyTaxChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val subtotal = cart.sumOf { it.qty.toLong() * it.unitPrice }
    val tax = if (applyTax) (subtotal * 10 / 100) else 0L
    val total = subtotal + tax
    val paymentMethods = listOf("TUNAI", "QRIS", "TRANSFER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Konfirmasi Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tipe Pesanan", style = MaterialTheme.typography.labelMedium, color = SukopiTheme.colors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptionChip(
                        label = "☕ Dine in",
                        selected = orderType == "DINE_IN",
                        onClick = { onOrderTypeChange("DINE_IN") },
                        modifier = Modifier.weight(1f)
                    )
                    OptionChip(
                        label = "🥡 Take away",
                        selected = orderType == "TAKE_AWAY",
                        onClick = { onOrderTypeChange("TAKE_AWAY") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Nama Pelanggan") },
                    placeholder = { Text("misal: Budi / Meja 05") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onCustomerPhoneChange,
                    label = { Text("No. WhatsApp (Struk & Loyalty)") },
                    placeholder = { Text("0812xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text("Ringkasan Item", style = MaterialTheme.typography.labelMedium, color = SukopiTheme.colors.textSecondary)
                cart.forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${line.qty}× ${line.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            if (!line.variant.isNullOrBlank()) {
                                Text(line.variant, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text((line.qty * line.unitPrice).toRupiah(), style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(subtotal.toRupiah(), style = MaterialTheme.typography.bodyMedium)
                }
                if (applyTax) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pajak PB1 (10%)", style = MaterialTheme.typography.bodyMedium)
                        Text(tax.toRupiah(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL BAYAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(total.toRupiah(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider()

                Text("Metode Pembayaran", style = MaterialTheme.typography.labelMedium, color = SukopiTheme.colors.textSecondary)
                paymentMethods.forEach { method ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedPayment == method) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .padding(4.dp)
                    ) {
                        RadioButton(
                            selected = selectedPayment == method,
                            onClick = { onPaymentSelect(method) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(method, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Bayar ${total.toRupiah()}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/** Dialog pesanan ditahan */
@Composable
private fun HeldCartsDialog(
    heldCarts: List<HeldCart>,
    onResume: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pesanan Ditahan (${heldCarts.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            if (heldCarts.isEmpty()) {
                Text("Tidak ada pesanan yang sedang ditahan.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                ) {
                    items(heldCarts, key = { it.id }) { held ->
                        val subtotal = held.items.sumOf { it.qty.toLong() * it.unitPrice }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(held.note, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (held.orderType == "TAKE_AWAY") "🥡 Take Away" else "☕ Dine In",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SukopiTheme.colors.textSecondary
                                    )
                                }
                                Text(
                                    "${held.items.size} item • Total: ${subtotal.toRupiah()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SukopiTheme.colors.textSecondary
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onDelete(held.id) }) {
                                        Text("Hapus", color = SukopiTheme.colors.danger)
                                    }
                                    Button(
                                        onClick = { onResume(held.id) },
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Buka Kembali")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

/** Dialog preview struk */
@Composable
private fun ReceiptPreviewDialog(
    receiptText: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Preview Struk Digital", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = receiptText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("📱 Kirim via WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
