package com.coffeeos.erp.feature.supply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.domain.supply.PoCalculation
import com.coffeeos.erp.ui.components.EmptyState
import com.coffeeos.erp.ui.theme.status

/**
 * Supply (design.md §8.6): FAB tambah supplier, PO Status Stepper per baris,
 * pesan idempotency informatif (bukan error generik).
 */
@Composable
fun SupplyScreen(
    outletId: String,
    actorId: String,
    canApprove: Boolean,
    vm: SupplyViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    val suppliers by vm.suppliers(outletId).collectAsState()
    val pos by vm.pos(outletId).collectAsState()
    var supName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (supName.isNotBlank()) {
                    vm.addSupplier(outletId, supName, "", ""); supName = ""
                }
            }) { Icon(Icons.Filled.Add, contentDescription = "Tambah supplier") }
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Supply — Supplier & PO", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = supName, onValueChange = { supName = it },
                label = { Text("Nama supplier baru (lalu tap ＋)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            ui.message?.let { Text(it) }
            Text("PO — DRAFT dibuat gudang, APPROVED oleh owner", style = MaterialTheme.typography.bodySmall)
            TextButton(
                onClick = {
                    val sup = suppliers.firstOrNull() ?: return@TextButton
                    vm.createDraft(
                        outletId, sup.id,
                        listOf(PoCalculation.PoItem("ing-susu", 10_000.0, 15)),
                        actorId
                    )
                },
                enabled = suppliers.isNotEmpty()
            ) { Text("Buat Draft PO Susu 10L (demo)") }
            if (pos.isEmpty()) {
                EmptyState(glyph = "📦", title = "Belum ada PO", hint = "Buat draft pertama dari supplier.")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pos, key = { it.id }) { po ->
                    Card(Modifier.fillMaxWidth().animateItem()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${po.id} • Rp${po.total}", fontWeight = FontWeight.Medium)
                                if (po.status == "DRAFT" && canApprove) {
                                    TextButton(onClick = { vm.approve(po.id) }) { Text("Approve") }
                                }
                            }
                            PoStepper(status = po.status)
                        }
                    }
                }
                items(suppliers) { sup ->
                    Text(
                        "${sup.name} • ${sup.phone}",
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    )
                }
            }
        }
    }
}

/** Stepper DRAFT → APPROVED → RECEIVED, terisi sesuai progres (design.md §5). */
@Composable
private fun PoStepper(status: String) {
    val steps = listOf("DRAFT", "APPROVED", "RECEIVED")
    val reached = when (status) {
        "APPROVED" -> 1
        "RECEIVED" -> 2
        else -> 0
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { i, step ->
            val done = i <= reached
            Text(
                "${if (done) "●" else "○"} $step",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                color = if (done) MaterialTheme.status.safe
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
