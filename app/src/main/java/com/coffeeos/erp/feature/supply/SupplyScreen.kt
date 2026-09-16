package com.coffeeos.erp.feature.supply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.domain.supply.PoCalculation

/** Supply: supplier + PO DRAFT -> APPROVED (owner) -> RECEIVED (gudang). */
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

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Supply — Supplier & PO", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = supName, onValueChange = { supName = it },
                label = { Text("Nama supplier baru") }, modifier = Modifier.weight(1f), singleLine = true
            )
            Button(onClick = { vm.addSupplier(outletId, supName, "", ""); supName = "" }) {
                Text("＋")
            }
        }
        ui.message?.let { Text(it) }
        Text("PO — DRAFT dibuat gudang, APPROVED oleh owner", style = MaterialTheme.typography.bodySmall)
        // Shortcut demo: draft 1 PO susu 10 unit dari supplier pertama.
        Button(
            onClick = {
                val sup = suppliers.firstOrNull() ?: return@Button
                vm.createDraft(
                    outletId, sup.id,
                    listOf(PoCalculation.PoItem("ing-susu", 10_000.0, 15)),
                    actorId
                )
            },
            enabled = suppliers.isNotEmpty()
        ) { Text("Buat Draft PO Susu 10L (demo)") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pos) { po ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${po.id} • Rp${po.total} • ${po.status}")
                        if (po.status == "DRAFT" && canApprove) {
                            Button(onClick = { vm.approve(po.id) }) { Text("Approve") }
                        }
                    }
                }
            }
            items(suppliers) { sup ->
                Text("${sup.name} • ${sup.phone}", modifier = Modifier.fillMaxWidth().padding(4.dp))
            }
        }
    }
}
