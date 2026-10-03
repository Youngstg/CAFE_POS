@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.coffeeos.erp.feature.shift

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.ShiftEntity
import com.coffeeos.erp.core.util.toDateTimeString
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.components.BadgeKind
import com.coffeeos.erp.ui.components.StatusBadge
import com.coffeeos.erp.ui.components.animateItem
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.SMALL_DIFF_THRESHOLD
import com.coffeeos.erp.ui.theme.status

/**
 * Shift (design.md §8.3):
 * Perbaikan:
 * - Indikator shift aktif yang jelas (warna hijau + waktu mulai + omzet berjalan)
 * - Form buka dan tutup shift terpisah berdasarkan kondisi aktual
 * - Riwayat shift dengan format Rupiah dan selisih berwarna semantik
 */
@Composable
fun ShiftScreen(
    outletId: String,
    uid: String,
    displayName: String,
    vm: ShiftViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val shifts by remember(outletId) { vm.shifts(outletId) }.collectAsState()
    var modal by remember { mutableStateOf("500000") }
    var counted by remember { mutableStateOf("") }

    val activeShift = shifts.firstOrNull { !it.isClosed }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Shift Kasir",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Panel Status Shift Aktif
        AnimatedVisibility(visible = activeShift != null, enter = fadeIn(), exit = fadeOut()) {
            activeShift?.let { shift ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.status.safeContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(MaterialTheme.status.safe)
                            )
                            Text(
                                "Shift Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.status.safe
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.status.safe.copy(alpha = 0.3f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Mulai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(shift.openedAt.toDateTimeString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Modal Awal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(shift.modalAwal.toRupiah(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                        Text(
                            "ID: ${shift.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Form buka/tutup shift bergantian (bukan sekaligus)
        if (activeShift == null) {
            // Form Buka Shift
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Buka Shift Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = modal,
                        onValueChange = { if (it.all(Char::isDigit)) modal = it },
                        label = { Text("Modal awal (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Jumlah kas tunai di laci") }
                    )
                    Button(
                        onClick = { vm.open(outletId, uid, displayName, modal.toLongOrNull() ?: 0) },
                        enabled = !ui.busy && modal.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange)
                    ) {
                        Text(
                            if (ui.busy) "Membuka shift..."
                            else "Buka Shift — Modal ${(modal.toLongOrNull() ?: 0L).toRupiah()}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Form Tutup Shift
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tutup Shift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Hitung dan masukkan total uang tunai di laci sekarang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = counted,
                        onValueChange = { if (it.all(Char::isDigit)) counted = it },
                        label = { Text("Kas fisik dihitung (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Jumlah kas di laci setelah shift ini") }
                    )
                    Button(
                        onClick = { vm.close(outletId, counted.toLongOrNull() ?: 0) },
                        enabled = !ui.busy && counted.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (ui.busy) "Menutup shift..." else "Tutup & Rekonsiliasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Pesan status
        ui.message?.let {
            StatusBadge(
                kind = when {
                    it.startsWith("Shift PAS") || it.contains("dibuka") -> BadgeKind.SAFE
                    it.contains("Selisih") -> BadgeKind.WARNING
                    else -> BadgeKind.INFO
                },
                text = it,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()
        Text("Riwayat Shift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shifts, key = { it.id }) { s ->
                ShiftHistoryRow(s, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ShiftHistoryRow(shift: ShiftEntity, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(shift.id, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        shift.openedAt.toDateTimeString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    kind = if (!shift.isClosed) BadgeKind.INFO
                    else {
                        val diff = kotlin.math.abs(shift.difference ?: 0L)
                        when {
                            diff == 0L -> BadgeKind.SAFE
                            diff <= SMALL_DIFF_THRESHOLD -> BadgeKind.WARNING
                            else -> BadgeKind.STOP
                        }
                    },
                    text = if (shift.isClosed) "TUTUP" else "AKTIF"
                )
            }
            if (shift.isClosed) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Modal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shift.modalAwal.toRupiah(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Omzet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(shift.paidTotal.toRupiah(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    val diff = shift.difference ?: 0L
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Selisih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            diff.toRupiah(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                diff == 0L -> MaterialTheme.status.safe
                                kotlin.math.abs(diff) <= SMALL_DIFF_THRESHOLD -> MaterialTheme.status.warning
                                else -> MaterialTheme.status.stop
                            }
                        )
                    }
                }
            }
        }
    }
}
