package com.coffeeos.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeeos.erp.core.sync.FirebaseHealthState
import com.coffeeos.erp.ui.theme.EnergyOrange
import com.coffeeos.erp.ui.theme.PillShape
import com.coffeeos.erp.ui.theme.SukopiTheme

/**
 * Dialog Pengujian & Status Koneksi Firebase (Cloud Firestore & Auth).
 * Memenuhi spesifikasi Design.md dengan radius 20dp, outline 1dp, dan tombol pill.
 */
@Composable
fun FirebaseConnectionDialog(
    state: FirebaseHealthState,
    onTestClick: () -> Unit,
    onSyncClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onSyncClick,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sync Sekarang", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onTestClick,
                shape = PillShape,
                enabled = !state.isChecking,
                modifier = Modifier.height(44.dp)
            ) {
                if (state.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Ulang")
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isFirestoreConnected) SukopiTheme.colors.successContainer
                            else SukopiTheme.colors.warningContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isFirestoreConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = if (state.isFirestoreConnected) SukopiTheme.colors.success else SukopiTheme.colors.warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "Status Database Firebase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Google Cloud Firestore & Auth",
                        style = MaterialTheme.typography.bodySmall,
                        color = SukopiTheme.colors.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Utama Banner
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (state.isFirestoreConnected) SukopiTheme.colors.successContainer
                    else SukopiTheme.colors.warningContainer,
                    border = BorderStroke(
                        1.dp,
                        if (state.isFirestoreConnected) SukopiTheme.colors.success.copy(alpha = 0.4f)
                        else SukopiTheme.colors.warning.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isFirestoreConnected) SukopiTheme.colors.success
                                    else SukopiTheme.colors.warning
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.statusSummary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (state.isFirestoreConnected) SukopiTheme.colors.success
                            else SukopiTheme.colors.warning
                        )
                    }
                }

                // Detail Metrics
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricRow("Project ID", state.projectId)
                        MetricRow("Host Database", "Cloud Firestore (asia-southeast1)")
                        MetricRow("Arsitektur", "Offline-First (Room SQLite + Firestore)")
                        if (state.isOnline) {
                            MetricRow("Latensi Jaringan", "${state.latencyMs} ms")
                        }
                        MetricRow(
                            "Antrean Sync",
                            if (state.pendingMutationsCount > 0) "${state.pendingMutationsCount} mutasi tertunda"
                            else "Semua data tersinkron ✓"
                        )
                        MetricRow("Sesi Pengguna", state.authUser ?: "Demo Kasir")
                    }
                }
            }
        }
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = SukopiTheme.colors.textSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
