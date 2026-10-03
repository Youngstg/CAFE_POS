package com.coffeeos.erp.feature.queueboard

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.local.OrderEntity
import com.coffeeos.erp.ui.theme.EnergyOrange
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Status Antrean Pelanggan (Pick-Up / Queue Board TV).
 * Dipasang pada Smart TV / Tablet Monitor di atas Bar Cafe.
 * Menampilkan pesanan yang SEDANG DISIAPKAN dan SIAP DIAMBIL dengan font besar high-contrast.
 */
@Composable
fun QueueBoardScreen(
    outletId: String,
    cafeName: String = "CoffeeOS",
    onBack: (() -> Unit)? = null,
    vm: QueueBoardViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(outletId) {
        vm.setOutletId(outletId, cafeName)
    }

    // Audio chime saat order berubah ke READY
    DisposableEffect(Unit) {
        val toneGen = try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (_: Exception) {
            null
        }
        onDispose {
            toneGen?.release()
        }
    }

    LaunchedEffect(Unit) {
        vm.readyCallEvent.collect {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
                delay(500)
                toneGen.release()
            } catch (_: Exception) { /* Tone fallback */ }
        }
    }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1115) // Deep Slate Black untuk TV Monitor
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header Bar TV
            QueueHeader(
                cafeName = uiState.cafeName,
                currentTime = currentTime,
                onBack = onBack
            )

            // Main Display: 2 Kolom Besar (Preparing vs Ready)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kolom Kiri: Sedang Disiapkan
                PreparingColumn(
                    orders = uiState.preparingOrders,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Kolom Kanan: Siap Diambil / Pick Up (Aksen Emerald Glow)
                ReadyColumn(
                    orders = uiState.readyOrders,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Running Ticker Footer
            QueueFooter()
        }
    }
}

@Composable
private fun QueueHeader(
    cafeName: String,
    currentTime: String,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161920))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(EnergyOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocalCafe, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(
                    text = cafeName.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "STATUS ANTREAN PESANAN (PICK-UP BOARD)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    letterSpacing = 1.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF232730))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PreparingColumn(orders: List<OrderEntity>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181B22))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header Kolom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "SEDANG DISIAPKAN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800),
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF262C36))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${orders.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tidak ada antrean disiapkan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF555D6B)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(orders) { order ->
                        PreparingCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreparingCard(order: OrderEntity) {
    val seq = order.orderSeq.toString().padStart(3, '0')
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222731))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "#$seq",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            val nameOrTable = order.customerName.ifBlank { "Take Away" }
            Text(
                text = nameOrTable,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFB74D),
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (order.status == "COOKING") "Sedang Dibuat" else "Dalam Antrean",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ReadyColumn(orders: List<OrderEntity>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13221B)) // Deep Emerald Dark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header Kolom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "SIAP DIAMBIL / PICK UP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A2C))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${orders.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada pesanan yang siap diambil",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF385244)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(orders) { order ->
                        ReadyCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyCard(order: OrderEntity) {
    val seq = order.orderSeq.toString().padStart(3, '0')
    // Efek Pulsing Glow Hijau
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E676),
        targetValue = Color(0xFF00E676).copy(alpha = 0.3f),
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "readyGlow"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3828)),
        modifier = Modifier.border(2.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "#$seq",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E676)
            )
            val nameOrTable = order.customerName.ifBlank { "Pelanggan" }
            Text(
                text = nameOrTable,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Silakan Ambil di Bar 🛎️",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB9F6CA),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QueueFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161920))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📢 Harap perhatikan nomor antrean Anda • Ambil pesanan di Pick-Up Bar saat nomor menyala hijau",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
    }
}
