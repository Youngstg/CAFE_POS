package com.coffeeos.erp.feature.cfd

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.coffeeos.erp.R
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.coffeeos.erp.core.data.repo.CartLine
import com.coffeeos.erp.core.data.repo.CustomerDisplayState
import com.coffeeos.erp.core.util.toRupiah
import com.coffeeos.erp.ui.components.QrCodeImage
import com.coffeeos.erp.ui.theme.EnergyOrange
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Customer-Facing Display (CFD): Layar kedua di meja kasir yang menghadap ke pelanggan.
 * Menampilkan ringkasan belanja live, promo cafe, dan QRIS dinamis untuk scan bayar langsung.
 */
@Composable
fun CustomerDisplayScreen(
    onBack: (() -> Unit)? = null,
    vm: CustomerDisplayViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121417) // Sleek Dark Modern Background
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header Bar
            CfdHeader(
                cafeName = state.cafeName,
                outletName = state.outletName,
                currentTime = currentTime,
                onBack = onBack
            )

            // Main Content Area
            if (state.isPaymentSuccess) {
                PaymentSuccessView(
                    orderId = state.lastOrderId ?: "",
                    total = state.total,
                    customerName = state.customerName,
                    onDone = { vm.dismissSuccess() }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Live Cart Items & Totals (60% width)
                    Card(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228))
                    ) {
                        LiveCartSection(state = state)
                    }

                    // Right Column: QRIS Payment or Promotional Showcase (40% width)
                    Card(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228))
                    ) {
                        if (state.paymentMethod == "QRIS" && state.total > 0) {
                            QrisDisplaySection(state = state)
                        } else {
                            PromoSlideSection()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CfdHeader(
    cafeName: String,
    outletName: String,
    currentTime: String,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181B20))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            }
            Image(
                painter = painterResource(id = R.drawable.logo_sukopi),
                contentDescription = "Logo SuKopi",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(
                    text = cafeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$outletName • Layar Pelanggan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2E35))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🟢 Kasir Buka",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = currentTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun LiveCartSection(state: CustomerDisplayState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ringkasan Pesanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val typeLabel = if (state.orderType == "TAKE_AWAY") "Take Away (Bawa Pulang)" else "Dine In (Minum di Sini)"
                Text(
                    text = if (state.customerName.isNotBlank()) "${state.customerName} • $typeLabel" else typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = EnergyOrange
                )
            }
            Text(
                text = "${state.items.sumOf { it.qty }} Item",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF2E333D))
        Spacer(Modifier.height(8.dp))

        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("☕", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Menunggu pesanan dari kasir...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items) { item ->
                    CartItemRow(item = item)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFF2E333D))
        Spacer(Modifier.height(12.dp))

        // Totals breakdown
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(state.subtotal.toRupiah(), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
            if (state.discount > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Diskon Promo", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                    Text("-${state.discount.toRupiah()}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                }
            }
            if (state.tax > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pajak Restoran (PB1 10%)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(state.tax.toRupiah(), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF272D36))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL BAYAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = state.total.toRupiah(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EnergyOrange
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(item: CartLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF262A32))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.qty}× ${item.name}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            if (!item.variant.isNullOrBlank()) {
                Text(
                    text = item.variant,
                    style = MaterialTheme.typography.labelSmall,
                    color = EnergyOrange
                )
            }
        }
        Text(
            text = (item.qty.toLong() * item.unitPrice).toRupiah(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun QrisDisplaySection(state: CustomerDisplayState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.QrCode, contentDescription = null, tint = EnergyOrange, modifier = Modifier.size(28.dp))
            Text(
                text = "Scan QRIS untuk Bayar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        // Kotak QR Code Besar
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            QrCodeImage(
                content = state.qrisPayload.ifBlank { "00020101021226600016ID.COFFEEOS.POS" },
                size = 216
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = state.total.toRupiah(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = EnergyOrange
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Mendukung Semua Pembayaran Digital:",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(6.dp))

        // Badge E-Wallet & Bank
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("BCA", "Mandiri", "GoPay", "OVO", "DANA", "ShopeePay").forEach { app ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2A2E35))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(app, fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PromoSlideSection() {
    val slides = listOf(
        PromoSlide(
            imageRes = R.drawable.banner_cfd,
            title = "Freshly Brewed Daily",
            subtitle = "Nikmati sajian kopi racikan barista dan freshly baked pastry hangat setiap hari."
        ),
        PromoSlide(
            imageRes = R.drawable.menu_kopsus,
            title = "Kumpulkan 10 Stempel Digital",
            subtitle = "Setiap pembelian 1 minuman = 1 stempel!\nDapatkan 1 Kopi Pilihan GRATIS pada stempel ke-10."
        ),
        PromoSlide(
            imageRes = R.drawable.logo_sukopi,
            title = "Pesan Mandiri Dari Meja",
            subtitle = "Malas antre di kasir? Cukup scan kode QR di meja Anda dan pesan langsung dari smartphone!"
        )
    )

    var currentSlideIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L) // Ganti slide setiap 5 detik
            currentSlideIndex = (currentSlideIndex + 1) % slides.size
        }
    }

    val currentSlide = slides[currentSlideIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "promoSlide"
        ) { slide ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (slide.imageRes != null) {
                    Image(
                        painter = painterResource(id = slide.imageRes),
                        contentDescription = slide.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Text(slide.icon, fontSize = 64.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = slide.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Slide Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            slides.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentSlideIndex) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (index == currentSlideIndex) EnergyOrange else Color(0xFF3B4048))
                )
            }
        }
    }
}

private data class PromoSlide(val imageRes: Int? = null, val icon: String = "☕", val title: String, val subtitle: String)

@Composable
private fun PaymentSuccessView(
    orderId: String,
    total: Long,
    customerName: String,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Pembayaran Berhasil!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = total.toRupiah(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = EnergyOrange
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (customerName.isNotBlank()) "Terima kasih, Kak $customerName!" else "Terima kasih atas pesanan Anda!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                Text(
                    text = "Pesanan sedang disiapkan di Bar ☕",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = EnergyOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selesai")
                }
            }
        }
    }
}
