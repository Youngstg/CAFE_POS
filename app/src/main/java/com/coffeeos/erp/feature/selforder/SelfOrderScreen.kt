@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.coffeeos.erp.feature.selforder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.domain.menu.DEFAULT_ADD_ONS
import com.coffeeos.erp.core.domain.menu.IceLevel
import com.coffeeos.erp.core.domain.menu.SugarLevel
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.components.MenuImageResolver
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status

@Composable
fun SelfOrderScreen(
    outletId: String,
    initialTableNo: String = "01",
    onBack: () -> Unit = {},
    vm: SelfOrderViewModel = hiltViewModel()
) {
    val tableNo by vm.tableNo.collectAsState()
    val menus by remember(outletId) { vm.menus(outletId) }.collectAsState()
    val cart by vm.cart.collectAsState()
    val query by vm.query.collectAsState()
    val customizingMenu by vm.customizingMenu.collectAsState()
    val submittedOrderId by vm.submittedOrderId.collectAsState()
    val isSubmitting by vm.isSubmitting.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val stampResult by vm.stampResult.collectAsState()

    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCartSheet by remember { mutableStateOf(false) }
    var showTablePicker by remember { mutableStateOf(false) }
    var showQrCardDialog by remember { mutableStateOf(false) }

    remember(initialTableNo) {
        if (initialTableNo.isNotBlank()) vm.setTableNo(initialTableNo)
    }

    // Layar Sukses Pesanan
    if (submittedOrderId != null) {
        OrderSuccessView(
            orderId = submittedOrderId ?: "",
            tableNo = tableNo,
            stampResult = stampResult,
            onNewOrder = { vm.resetOrder() }
        )
        return
    }

    // Dialog Pemilih / Ganti Nomor Meja
    if (showTablePicker) {
        TablePickerDialog(
            currentTable = tableNo,
            onSelect = {
                vm.setTableNo(it)
                showTablePicker = false
            },
            onDismiss = { showTablePicker = false }
        )
    }

    // Dialog Tampilan Stand QR Meja untuk dicetak / dipajang di cafe
    if (showQrCardDialog) {
        TableQrStandDialog(
            tableNo = tableNo,
            onDismiss = { showQrCardDialog = false }
        )
    }

    // Bottom Sheet Kustomisasi Minuman
    customizingMenu?.let { menu ->
        DrinkCustomizerSheet(
            menu = menu,
            vm = vm,
            onDismiss = { vm.closeCustomizer() }
        )
    }

    // Bottom Sheet Keranjang & Checkout Pelanggan
    if (showCartSheet) {
        CustomerCartSheet(
            cart = cart,
            tableNo = tableNo,
            outletId = outletId,
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            vm = vm,
            onDismiss = { showCartSheet = false },
            onSubmit = {
                vm.submitOrder(outletId)
                showCartSheet = false
            }
        )
    }

    val categories = remember(menus) { listOf("Semua") + menus.map { it.category }.distinct().sorted() }
    val filteredMenus = remember(menus, selectedCategory, query) {
        menus.filter {
            (selectedCategory == "Semua" || it.category == selectedCategory) &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (cart.isNotEmpty()) 76.dp else 0.dp)
        ) {
            // Header Pelanggan
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                            Column {
                                Text("☕ CoffeeOS Cafe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Pesan Langsung dari Meja", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { showQrCardDialog = true }) {
                                Icon(Icons.Filled.QrCode, contentDescription = "Stand QR Meja", tint = EnergyOrange)
                            }
                            FilterChip(
                                selected = true,
                                onClick = { showTablePicker = true },
                                label = { Text("Meja #$tableNo ▾", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    // Kolom Pencarian
                    OutlinedTextField(
                        value = query,
                        onValueChange = { vm.setQuery(it) },
                        placeholder = { Text("Cari kopi, pastry, camilan...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Kategori Menu
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // Grid Daftar Menu
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMenus, key = { it.id }) { menu ->
                    CustomerMenuTile(
                        menu = menu,
                        onOrderClick = { vm.openCustomizer(menu) }
                    )
                }
            }
        }

        // Sticky Bottom Cart Bar
        AnimatedVisibility(
            visible = cart.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val totalQty = cart.sumOf { it.qty }
            val subtotal = cart.sumOf { it.qty * it.unitPrice }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "$totalQty Item dipesan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            subtotal.toRupiah(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EnergyOrange
                        )
                    }
                    Button(
                        onClick = { showCartSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                        shape = PillShape,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lihat Pesanan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Tile Menu untuk Pelanggan */
@Composable
private fun CustomerMenuTile(menu: MenuEntity, onOrderClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val imageRes = remember(menu.name, menu.id) {
                MenuImageResolver.getDrawableForMenu(menu.name, menu.id)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = menu.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("☕", fontSize = 42.sp)
                }
                if (!menu.isAvailable) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("HABIS", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(
                menu.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                menu.price.toRupiah(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EnergyOrange
            )
            Button(
                onClick = onOrderClick,
                enabled = menu.isAvailable,
                colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                shape = PillShape,
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text("Pesan", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Bottom Sheet Kustomisasi Minuman (Level Es, Gula, Add-ons) */
@Composable
private fun DrinkCustomizerSheet(
    menu: MenuEntity,
    vm: SelfOrderViewModel,
    onDismiss: () -> Unit
) {
    val mods by vm.selectedModifiers.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(menu.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Kustomisasi Minuman Anda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text((menu.price + mods.totalExtraPrice).toRupiah(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = EnergyOrange)
            }

            HorizontalDivider()

            // Level Es
            Text("Pilihan Es (Ice Level)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IceLevel.entries.forEach { ice ->
                    FilterChip(
                        selected = mods.ice == ice,
                        onClick = { vm.setIce(ice) },
                        label = { Text(ice.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Level Gula
            Text("Level Kemanisan (Sugar Level)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SugarLevel.entries.forEach { sugar ->
                    FilterChip(
                        selected = mods.sugar == sugar,
                        onClick = { vm.setSugar(sugar) },
                        label = { Text(sugar.label.replace("Sugar", "").trim(), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // Add-ons Berbayar
            Text("Tambah Topping & Ekstra (Add-ons)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            DEFAULT_ADD_ONS.forEach { addon ->
                val checked = mods.addOns.any { it.id == addon.id }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.toggleAddOn(addon) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { vm.toggleAddOn(addon) },
                            colors = CheckboxDefaults.colors(checkedColor = EnergyOrange)
                        )
                        Text(addon.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("+${addon.extraPrice.toRupiah()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EnergyOrange)
                }
            }

            // Catatan Khusus
            OutlinedTextField(
                value = mods.notes,
                onValueChange = { vm.setNotes(it) },
                label = { Text("Catatan untuk Barista (opsional)") },
                placeholder = { Text("misal: jangan terlalu pahit ya kak") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.addCustomizedToCart() },
                colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    "Tambah ke Pesanan — ${(menu.price + mods.totalExtraPrice).toRupiah()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/** Bottom Sheet Keranjang Pesanan Meja */
@Composable
private fun CustomerCartSheet(
    cart: List<com.coffeeos.erp.core.data.repo.CartLine>,
    tableNo: String,
    outletId: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    vm: SelfOrderViewModel,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val name by vm.customerName.collectAsState()
    val phone by vm.customerPhone.collectAsState()
    val paymentChoice by vm.paymentChoice.collectAsState()
    val subtotal = cart.sumOf { it.qty * it.unitPrice }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pesanan Meja #$tableNo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { vm.clearCart() }) { Text("Hapus Semua") }
            }

            // Daftar Item Cart
            cart.forEachIndexed { index, line ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        if (line.notes.isNotBlank()) {
                            Text(line.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${line.unitPrice.toRupiah()} × ${line.qty}", style = MaterialTheme.typography.bodySmall, color = EnergyOrange)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { vm.decreaseFromCart(index) },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                        }
                        Text("${line.qty}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                        IconButton(
                            onClick = { vm.increaseInCart(index) },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(EnergyOrange)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }

            // Input Data Pelanggan
            Text("Data Pemesan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { vm.setCustomerName(it) },
                label = { Text("Nama Panggilan Anda") },
                placeholder = { Text("misal: Budi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { vm.setCustomerPhone(it) },
                label = { Text("No. WhatsApp (Program Stempel Gratis 🎁)") },
                placeholder = { Text("0812xxxxxxxx (opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Pilihan Metode Bayar
            Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paymentChoice == "QRIS",
                    onClick = { vm.setPaymentChoice("QRIS") },
                    label = { Text("📱 QRIS (Scan)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = paymentChoice == "CASHIER",
                    onClick = { vm.setPaymentChoice("CASHIER") },
                    label = { Text("💵 Bayar di Kasir") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Rincian Total
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Tagihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtotal.toRupiah(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = EnergyOrange)
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && cart.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mengirim ke Dapur...")
                } else {
                    Text("Kirim Pesanan ke Dapur 🚀", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Layar Sukses Pesanan Pelanggan */
@Composable
private fun OrderSuccessView(
    orderId: String,
    tableNo: String,
    stampResult: com.coffeeos.erp.core.data.repo.CustomerRepository.StampResult?,
    onNewOrder: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.status.safe,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Pesanan Terkirim ke Dapur!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Nomor Pesanan: $orderId (Meja #$tableNo)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = EnergyOrange
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Barista kami sedang menyiapkan pesanan Anda dengan penuh cinta. Duduk santai, pesanan Anda akan segera diantar ke meja!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Progress Stempel Loyalitas jika ada
        stampResult?.let { stamp ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎁 Kartu Stempel Loyalitas Kopi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stamp.formatVisualBar(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EnergyOrange
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (stamp.freeRewardEarned) "🎉 Selamat! Anda berhak mendapatkan 1 KOPI GRATIS!"
                        else "Kumpulkan ${10 - stamp.currentStamps} lagi untuk klaim Kopi Gratis!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onNewOrder,
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Pesan Menu Lainnya", fontWeight = FontWeight.Bold)
        }
    }
}

/** Dialog Pemilih Nomor Meja */
@Composable
private fun TablePickerDialog(currentTable: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val tables = (1..20).map { it.toString().padStart(2, '0') }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Nomor Meja", fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 280.dp)
            ) {
                items(tables) { table ->
                    val selected = table == currentTable
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) EnergyOrange else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(table) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#$table",
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/** Dialog Preview Kartu Tenda QR Meja */
@Composable
private fun TableQrStandDialog(tableNo: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stand QR Meja #$tableNo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, EnergyOrange),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("☕ COFFEEOS CAFE", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Text("SCAN UNTUK PESAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EnergyOrange)
                        Box(
                            Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(110.dp), tint = Color.Black)
                        }
                        Text("MEJA #$tableNo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Text("Tanpa antre • Langsung bayar", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                Text("Pajang kartu ini di atas meja cafe agar pelanggan bisa scan & pesan langsung.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
