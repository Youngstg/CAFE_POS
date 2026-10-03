@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.menu

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.util.toRupiah
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.components.animateItem
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import com.coffeeos.erp.ui.theme.status

/** Kategori baku — bebas extend di sini tanpa ganti DB. */
val PRESET_CATEGORIES = listOf(
    "Kopi", "Non-Kopi", "Minuman Dingin", "Minuman Panas",
    "Makanan Ringan", "Makanan Berat", "Dessert", "Promo", "Lainnya"
)

/**
 * Manajemen menu owner: daftar + form tambah/edit + editor resep + promo.
 * Perbaikan:
 * - Edit menu (nama/harga/kategori) — bukan hanya tambah/hapus
 * - Dropdown kategori (bukan free text) dengan opsi preset
 * - Bahan resep yang sudah dipakai di-highlight
 * - Form tersimpan di tab terpisah (tab-based layout)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MenuScreen(outletId: String, vm: MenuViewModel = hiltViewModel()) {
    val menus by remember(outletId) { vm.menus(outletId) }.collectAsState()
    val ingredients by remember(outletId) { vm.ingredients(outletId) }.collectAsState()
    val recipes by vm.recipes.collectAsState()
    val selectedMenuId by vm.selectedMenu.collectAsState()
    val ui by vm.ui.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Daftar Menu", "Tambah / Edit", "Resep BOM")

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label) })
            }
        }
        AnimatedContent(
            targetState = tabIndex,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "menuTab"
        ) { tab ->
            when (tab) {
                0 -> MenuListTab(
                    menus = menus,
                    selectedId = selectedMenuId,
                    onSelect = { vm.selectMenu(it); tabIndex = 2 },
                    onEdit = { vm.selectMenu(it); tabIndex = 1 },
                    onDelete = { vm.deleteMenu(it) },
                    message = ui.message
                )
                1 -> MenuFormTab(
                    outletId = outletId,
                    editingMenu = menus.firstOrNull { it.id == selectedMenuId },
                    onSave = { id, name, price, cat ->
                        vm.saveMenu(outletId, id, name, price, cat)
                        tabIndex = 0
                    }
                )
                else -> RecipeTab(
                    selectedMenuId = selectedMenuId,
                    menus = menus,
                    ingredients = ingredients,
                    existingRecipes = recipes,
                    onSelectMenu = { vm.selectMenu(it) },
                    onAddRecipe = { menuId, ingId, qty -> vm.saveRecipe(menuId, ingId, qty) },
                    onDeleteRecipe = { menuId, ingId -> vm.deleteRecipe(menuId, ingId) }
                )
            }
        }
    }
}

@Composable
private fun MenuListTab(
    menus: List<MenuEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    message: String?,
) {
    var deleteConfirm by remember { mutableStateOf<String?>(null) }

    deleteConfirm?.let { menuId ->
        val menu = menus.firstOrNull { it.id == menuId }
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Hapus Menu?") },
            text = { Text("\"${menu?.name}\" akan dihapus dari sistem. Resepnya juga ikut terhapus.") },
            confirmButton = {
                Button(onClick = { onDelete(menuId); deleteConfirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("Batal") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        message?.let {
            Text(it, color = if (it.contains("Tersimpan")) MaterialTheme.status.safe else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        if (menus.isEmpty()) {
            EmptyState(glyph = "🍽️", title = "Belum ada menu", hint = "Tap tab 'Tambah / Edit' untuk membuat menu pertama.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(menus, key = { it.id }) { menu ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (menu.id == selectedId)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (menu.id == selectedId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(menu.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${menu.category} · ${menu.price.toRupiah()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!menu.isAvailable) {
                                    Text("⛔ Stok habis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.status.stop)
                                }
                            }
                            Row {
                                IconButton(onClick = { onEdit(menu.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onSelect(menu.id) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Resep", tint = MaterialTheme.status.safe)
                                }
                                IconButton(onClick = { deleteConfirm = menu.id }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuFormTab(
    outletId: String,
    editingMenu: MenuEntity?,
    onSave: (String?, String, Long, String) -> Unit,
) {
    var name by remember(editingMenu?.id) { mutableStateOf(editingMenu?.name ?: "") }
    var priceText by remember(editingMenu?.id) { mutableStateOf(editingMenu?.price?.toString() ?: "") }
    var category by remember(editingMenu?.id) { mutableStateOf(editingMenu?.category ?: PRESET_CATEGORIES.first()) }
    var categoryDropdown by remember { mutableStateOf(false) }

    val isEdit = editingMenu != null
    val priceVal = priceText.toLongOrNull() ?: 0L

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isEdit) "Edit Menu: ${editingMenu?.name}" else "Tambah Menu Baru",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Menu") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { if (it.all(Char::isDigit)) priceText = it },
            label = { Text("Harga (Rp)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { if (priceVal > 0) Text("= ${priceVal.toRupiah()}") }
        )

        // Dropdown kategori
        Box {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it }, // Tetap izinkan ketik bebas
                label = { Text("Kategori") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { categoryDropdown = true }) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih kategori")
                    }
                }
            )
            DropdownMenu(
                expanded = categoryDropdown,
                onDismissRequest = { categoryDropdown = false }
            ) {
                PRESET_CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { category = cat; categoryDropdown = false }
                    )
                }
            }
        }

        Button(
            onClick = { onSave(editingMenu?.id, name.trim(), priceVal, category.trim().ifBlank { "Umum" }) },
            enabled = name.isNotBlank() && priceVal > 0,
            shape = PillShape,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
        ) {
            Text(
                if (isEdit) "Simpan Perubahan" else "Tambahkan ke Menu",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeTab(
    selectedMenuId: String?,
    menus: List<MenuEntity>,
    ingredients: List<com.coffeeos.erp.core.data.local.IngredientEntity>,
    existingRecipes: List<com.coffeeos.erp.core.data.local.RecipeEntity>,
    onSelectMenu: (String) -> Unit,
    onAddRecipe: (String, String, Double) -> Unit,
    onDeleteRecipe: (String, String) -> Unit,
) {
    val selectedMenu = menus.firstOrNull { it.id == selectedMenuId }
    val usedIngredientIds = existingRecipes.map { it.ingredientId }.toSet()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Editor Resep BOM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Pilih menu
        if (selectedMenu == null) {
            Text("Pilih menu di tab 'Daftar Menu' → tombol ＋", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        Text(
            "Menu: ${selectedMenu.name}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text("Resep saat ini:", style = MaterialTheme.typography.labelMedium)

        if (existingRecipes.isEmpty()) {
            Text("Belum ada bahan (jual bebas tanpa deduct stok).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                existingRecipes.forEach { r ->
                    val ing = ingredients.firstOrNull { it.id == r.ingredientId }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "✓ ${ing?.name ?: r.ingredientId} — ${r.qtyPerPortion} ${ing?.unit ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(
                                onClick = { onDeleteRecipe(selectedMenuId!!, r.ingredientId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Text("Tambah Bahan:", style = MaterialTheme.typography.labelMedium)

        // Daftar ingredient: sudah dipakai = highlight hijau, belum = normal
        var qtyInput by remember { mutableStateOf("") }
        var selectedIng by remember { mutableStateOf<com.coffeeos.erp.core.data.local.IngredientEntity?>(null) }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ingredients.forEach { ing ->
                val isUsed = ing.id in usedIngredientIds
                FilterChip(
                    selected = selectedIng?.id == ing.id,
                    onClick = { selectedIng = if (selectedIng?.id == ing.id) null else ing },
                    label = { Text(if (isUsed) "✓ ${ing.name}" else ing.name) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isUsed)
                            MaterialTheme.status.safeContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        selectedIng?.let { ing ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it },
                    label = { Text("Qty per porsi (${ing.unit})") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(
                    onClick = {
                        val qty = qtyInput.toDoubleOrNull() ?: return@Button
                        onAddRecipe(selectedMenuId!!, ing.id, qty)
                        qtyInput = ""
                    },
                    enabled = qtyInput.toDoubleOrNull() != null && (qtyInput.toDoubleOrNull() ?: 0.0) > 0,
                    shape = PillShape,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
                ) {
                    Text(if (ing.id in usedIngredientIds) "Update" else "Tambah", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
