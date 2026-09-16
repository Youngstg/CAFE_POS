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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Katalog owner/admin: kelola menu + resep BOM per menu + promo.
 * Semua tersimpan offline-first dan tersinkron antar HP.
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
    var qty by remember { mutableStateOf("") }
    var promoName by remember { mutableStateOf("") }
    var promoPct by remember { mutableStateOf("10") }
    var promoMin by remember { mutableStateOf("50000") }

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
            Button(onClick = {
                vm.saveMenu(outletId, null, menuName, menuPrice.toLongOrNull() ?: 0)
                menuName = ""; menuPrice = ""
            }) { Text("＋ Tambah Menu") }
            ui.message?.let { Text(it) }
        }
        items(menus) { menu ->
            Card(
                onClick = { vm.selectMenu(if (selected == menu.id) null else menu.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(menu.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Rp${menu.price} • " +
                                if (menu.isAvailable) "Jual" else "Mati (stok)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { vm.deleteMenu(menu.id) }) { Text("Hapus") }
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
                ingredients.forEach { ing ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${ing.name} (${ing.unit})", modifier = Modifier.weight(1f))
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
        items(promos) { promo ->
            Row(
                Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = promo.active,
                        onCheckedChange = {
                            vm.savePromo(
                                outletId, promo.id, promo.name, promo.percentOff,
                                promo.fixedDiscount, promo.minOrder, it
                            )
                        }
                    )
                    TextButton(onClick = { vm.deletePromo(promo.id) }) { Text("Hapus") }
                }
            }
        }
    }
}
