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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Buka/tutup shift + riwayat + pesan selisih. Tutup diblokir bila antrean sync > 0. */
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
        Text("Shift Kasir — $displayName")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = modal, onValueChange = { modal = it },
                label = { Text("Modal awal") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { vm.open(outletId, uid, displayName, modal.toLongOrNull() ?: 0) }) {
                Text("Buka")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = counted, onValueChange = { counted = it },
                label = { Text("Kas fisik") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { vm.close(outletId, counted.toLongOrNull() ?: 0) }) {
                Text("Tutup")
            }
        }
        ui.message?.let { Text(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shifts) { s ->
                Text(
                    "${s.id} • modal Rp${s.modalAwal} • " +
                        if (s.isClosed) "TUTUP (selisih Rp${s.difference ?: 0})" else "AKTIF",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
