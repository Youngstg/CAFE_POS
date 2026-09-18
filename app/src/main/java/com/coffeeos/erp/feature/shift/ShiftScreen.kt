package com.coffeeos.erp.feature.shift

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.ui.components.BadgeKind
import com.coffeeos.erp.ui.components.StatusBadge
import com.coffeeos.erp.ui.theme.SMALL_DIFF_THRESHOLD
import com.coffeeos.erp.ui.theme.status

/**
 * Shift (design.md §8.3): selisih berwarna semantik (pas/kecil/besar),
 * alasan terkunci eksplisit + jumlah pending + tombol sync — bukan disabled bisu.
 */
@Composable
fun ShiftScreen(
    outletId: String,
    uid: String,
    displayName: String,
    vm: ShiftViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val shifts by vm.shifts(outletId).collectAsState()
    var modal by remember { mutableStateOf("500000") }
    var counted by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Shift Kasir — $displayName", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = modal, onValueChange = { modal = it },
                label = { Text("Modal awal") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { vm.open(outletId, uid, displayName, modal.toLongOrNull() ?: 0) }) {
                Text("Buka")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = counted, onValueChange = { counted = it },
                label = { Text("Kas fisik") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { vm.close(outletId, counted.toLongOrNull() ?: 0) }) {
                Text("Tutup")
            }
        }
        ui.message?.let {
            StatusBadge(
                kind = if (it.startsWith("Shift PAS") || it.contains("dibuka")) BadgeKind.SAFE
                else BadgeKind.STOP,
                text = it
            )
        }
        Text("Riwayat", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shifts, key = { it.id }) { s ->
                Row(
                    Modifier.fillMaxWidth().animateItemPlacement(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${s.id} • modal Rp${s.modalAwal}", fontWeight = FontWeight.Medium)
                        if (s.isClosed) {
                            val diff = s.difference ?: 0L
                            Text(
                                "Selisih Rp$diff",
                                color = when {
                                    diff == 0L -> MaterialTheme.status.safe
                                    kotlin.math.abs(diff) <= SMALL_DIFF_THRESHOLD -> MaterialTheme.status.warning
                                    else -> MaterialTheme.status.stop
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    StatusBadge(
                        kind = if (!s.isClosed) BadgeKind.INFO
                        else {
                            val diff = kotlin.math.abs(s.difference ?: 0L)
                            when {
                                diff == 0L -> BadgeKind.SAFE
                                diff <= SMALL_DIFF_THRESHOLD -> BadgeKind.WARNING
                                else -> BadgeKind.STOP
                            }
                        },
                        text = if (s.isClosed) "TUTUP" else "AKTIF"
                    )
                }
            }
        }
    }
}
