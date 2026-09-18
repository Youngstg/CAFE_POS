package com.coffeeos.erp.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.ui.components.ConfirmDialog
import com.coffeeos.erp.ui.components.EmptyState

/**
 * Katalog owner/admin (design.md §8.7): CRUD menu + editor resep dua panel
 * (bahan tersedia | komposisi) + promo dengan switch besar. Hapus destruktif
 * selalu konfirmasi dialog (§11).
 */
@Composable
fun MenuScreen(outletId: String, vm: MenuViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val menus by vm.menus(outletId).collectAsState()
    val ingredients by vm.ingredients(outletId).collectAsState()
    val promos by vm.promos(outletId).collectAsState()
    val selected by vm.selectedMenu.collectAsState()
    val recipes by vm.recipes.collectAsState()

    var menuName by remember { mutableStateOf("") }
    var menuPrice by remember { mutableStateOf("") }
    var menuCat by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var promoName by remember { mutableStateOf("") }
    var promoPct by remember { mutableStateOf("10") }
    var promoMin by remember { mutableStateOf("50000") }
    var confirmDeleteMenu by remember { mutableStateOf<String?>(null) }
    var confirmDeletePromo by remember { mutableStateOf<String?>(null) }

    confirmDeleteMenu?.let { menuId ->
        ConfirmDialog(
            title = "Hapus menu?",
            body = "Resep menu ini ikut terhapus di semua HP. Stok bahan tidak berubah.",
            confirmLabel = "Hapus",
            onConfirm = { vm.deleteMenu(menuId); confirmDeleteMenu = null },
            onDismiss = { confirmDeleteMenu = null }
        )
    }
    confirmDeletePromo?.let { promoId ->
        ConfirmDialog(
            title = "Hapus promo?",
            body = "Promo hilang dari kasir semua HP.",
            confirmLabel = "Hapus",
            onConfirm = { vm.deletePromo(promoId); confirmDeletePromo = null },
            onDismiss = { confirmDeletePromo = null }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Katalog — Menu & Resep", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = menuName, onValueChange = { menuName = it },
                    label = { Text("Nama menu") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = menuPrice, onValueChange = { menuPrice = it },
                    label = { Text("Harga") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            OutlinedTextField(
                value = menuCat, onValueChange = { menuCat = it },
                label = { Text("Kategori (mis. Minuman)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Button(onClick = {
                vm.saveMenu(outletId, null, menuName, menuPrice.toLongOrNull() ?: 0, menuCat)
                menuName = ""; menuPrice = ""; menuCat = ""
            }) { Text("＋ Tambah Menu") }
            ui.message?.let { Text(it) }
        }
        if (menus.isEmpty()) {
            item { EmptyState(glyph = "☕", title = "Belum ada menu", hint = "Tambah menu pertama di atas.") }
        }
        items(menus, key = { it.id }) { menu ->
            Card(
                onClick = { vm.selectMenu(if (selected == menu.id) null else menu.id) },
                modifier = Modifier.fillMaxWidth().animateItem()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(menu.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${menu.category} • Rp${menu.price} • " +
                                if (menu.isAvailable) "Jual" else "Mati (stok)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { confirmDeleteMenu = menu.id }) { Text("Hapus") }
                }
            }
        }
        selected?.let { menuId ->
            item {
                val ingNames = ingredients.associateBy({ it.id }, { it.name })
                Text(
                    "Resep: ${menus.firstOrNull { it.id == menuId }?.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (recipes.isEmpty()) {
                    Text("Belum ada bahan — tambah dari daftar bawah.", style = MaterialTheme.typography.bodySmall)
                }
                recipes.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${ingNames[r.ingredientId] ?: r.ingredientId} — ${r.qtyPerPortion}")
                        TextButton(onClick = { vm.deleteRecipe(menuId, r.ingredientId) }) {
                            Text("Hapus")
                        }
                    }
                }
                Text("Tambah bahan ke resep (takaran per porsi):", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it },
                    label = { Text("Takaran") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text("Kiri: bahan tersedia (tap ＋) — kanan: komposisi di atas.", style = MaterialTheme.typography.bodySmall)
                ingredients.forEach { ing ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${ing.name} (${ing.unit})")
                            TextButton(onClick = { vm.deleteIngredient(ing.id) }) {
                                Text("Hapus bahan", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = {
                            vm.saveRecipe(menuId, ing.id, qty.toDoubleOrNull() ?: 0.0)
                        }) { Text("＋") }
                    }
                }
            }
        }
        item {
            Text("Promo", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = promoName, onValueChange = { promoName = it },
                label = { Text("Nama promo") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = promoPct, onValueChange = { promoPct = it },
                    label = { Text("% (0-100)") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = promoMin, onValueChange = { promoMin = it },
                    label = { Text("Min. order") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Button(onClick = {
                vm.savePromo(
                    outletId, null, promoName,
                    promoPct.toIntOrNull() ?: 0, 0, promoMin.toLongOrNull() ?: 0, true
                )
                promoName = ""
            }) { Text("＋ Tambah Promo") }
        }
        items(promos, key = { it.id }) { promo ->
            Row(
                Modifier.fillMaxWidth().animateItem(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(promo.name)
                    Text(
                        "${promo.percentOff}% • min Rp${promo.minOrder} • " +
                            if (promo.active) "Aktif" else "Mati",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = promo.active,
                    onCheckedChange = {
                        vm.savePromo(
                            outletId, promo.id, promo.name, promo.percentOff,
                            promo.fixedDiscount, promo.minOrder, it
                        )
                    }
                )
                TextButton(onClick = { confirmDeletePromo = promo.id }) { Text("Hapus") }
            }
        }
    }
}
