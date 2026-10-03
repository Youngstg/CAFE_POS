package com.coffeeos.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coffeeos.erp.core.data.local.MenuEntity
import com.coffeeos.erp.core.domain.menu.AddOn
import com.coffeeos.erp.core.domain.menu.DEFAULT_ADD_ONS
import com.coffeeos.erp.core.domain.menu.DrinkSize
import com.coffeeos.erp.core.domain.menu.IceLevel
import com.coffeeos.erp.core.domain.menu.MilkOption
import com.coffeeos.erp.core.domain.menu.SelectedModifiers
import com.coffeeos.erp.core.domain.menu.SugarLevel
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.theme.Dimens
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme
import kotlin.math.abs

/**
 * Dialog Kustomisasi Produk SuKopi POS (DESIGN.md §7.2):
 * - Modal ~360dp, tinggi hingga 90% layar, scroll, radius 20dp, scrim 45%.
 * - Hero photo ~160dp dengan rounded top 20dp + tombol close (X) bulat putih.
 * - Section: Type (Iced/Hot), Size, Sugar Level, Milk, Add-ons (2 kolom).
 * - Footer menempel: Stepper qty + Tombol primary "Add to order — Rp xxx".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductCustomizerDialog(
    menu: MenuEntity,
    initialModifiers: SelectedModifiers = SelectedModifiers(),
    onConfirm: (SelectedModifiers, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(initialModifiers.ice) }
    var size by remember { mutableStateOf(initialModifiers.size) }
    var sugar by remember { mutableStateOf(initialModifiers.sugar) }
    var milk by remember { mutableStateOf(initialModifiers.milk) }
    var selectedAddOns by remember { mutableStateOf(initialModifiers.addOns) }
    var notes by remember { mutableStateOf(initialModifiers.notes) }
    var quantity by remember { mutableIntStateOf(1) }

    val basePrice = menu.price
    val extraPrice = size.extraPrice + milk.extraPrice + selectedAddOns.sumOf { it.extraPrice }
    val unitPrice = basePrice + extraPrice
    val totalPrice = unitPrice * quantity

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Konten scrollable
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Hero Image / Banner (160dp tinggi, top corner 20dp)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFFD63C18)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = menu.name.firstOrNull()?.uppercase() ?: "☕",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            // Tombol close (X) bulat putih di kanan atas
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Info Produk
                        Column(
                            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = menu.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Kategori: ${menu.category} • Dasar: ${menu.price.toRupiah()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 2. Type (Iced | Hot)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Type",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OptionChip(
                                        label = "Iced",
                                        selected = type != IceLevel.HOT,
                                        onClick = { type = IceLevel.NORMAL },
                                        shape = PillShape,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.AcUnit,
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OptionChip(
                                        label = "Hot",
                                        selected = type == IceLevel.HOT,
                                        onClick = { type = IceLevel.HOT },
                                        shape = PillShape,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.LocalFireDepartment,
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // 3. Size (Regular | Large)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Size",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DrinkSize.entries.forEach { opt ->
                                        OptionChip(
                                            label = opt.label,
                                            selected = size == opt,
                                            onClick = { size = opt },
                                            shape = PillShape,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // 4. Sugar Level (Normal | Less | No Sugar)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Sugar Level",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SugarLevel.entries.forEach { opt ->
                                        OptionChip(
                                            label = opt.label,
                                            selected = sugar == opt,
                                            onClick = { sugar = opt },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // 5. Milk (Fresh Milk · Oat Milk · Soy Milk · Almond Milk · No Milk)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Milk",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MilkOption.entries.forEach { opt ->
                                        OptionChip(
                                            label = opt.label,
                                            selected = milk == opt,
                                            onClick = { milk = opt },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }

                            // 6. Add-ons (Grid 2 kolom)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Add-ons",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DEFAULT_ADD_ONS.chunked(2).forEach { rowItems ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { addon ->
                                                val isChecked = selectedAddOns.any { it.id == addon.id }
                                                OptionChip(
                                                    label = addon.name,
                                                    trailingText = "+${addon.extraPrice.toRupiah()}",
                                                    selected = isChecked,
                                                    onClick = {
                                                        selectedAddOns = if (isChecked) {
                                                            selectedAddOns.filterNot { it.id == addon.id }
                                                        } else {
                                                            selectedAddOns + addon
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (rowItems.size == 1) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            // 7. Catatan Pelanggan
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Customer Notes (opsional)") },
                                placeholder = { Text("misal: jangan terlalu manis, sajikan hangat") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 8. Sticky Footer: Stepper Qty + Primary Button "Add to order — Rp xxx"
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stepper Qty (- 1 +)
                            Surface(
                                shape = PillShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "$quantity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { quantity++ },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Tombol Primary Full Height 48dp Pill
                            Button(
                                onClick = {
                                    val mods = SelectedModifiers(
                                        ice = type,
                                        sugar = sugar,
                                        size = size,
                                        milk = milk,
                                        addOns = selectedAddOns,
                                        notes = notes
                                    )
                                    onConfirm(mods, quantity)
                                },
                                shape = PillShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Add to order — ${totalPrice.toRupiah()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
