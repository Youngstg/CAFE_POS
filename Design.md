# DESIGN.md — SuKopi POS (Visual & UI Spec)

Spesifikasi tampilan untuk memperbarui UI aplikasi Android (Kotlin + Jetpack Compose). Hanya membahas **layout, warna, font, bentuk, komponen, dan tampilan tiap layar**.

> Nilai hex adalah estimasi dari screenshot referensi. Sesuaikan bila ada file Figma asli.

---

## 1. Gaya Visual

- Bersih, ringan, banyak ruang kosong.
- Latar abu sangat muda, kartu putih dengan border tipis (flat, tanpa shadow berat).
- **Satu warna aksen**: oranye-merah, dipakai hanya untuk state aktif, tombol utama, dan penekanan.
- Foto produk besar dengan sudut membulat sebagai pusat perhatian.
- Elemen interaktif berbentuk **pill** (sangat bulat); kartu berbentuk rounded rectangle.
- Badge kecil (stok, diskon, status) ditumpuk di atas konten.

---

## 2. Warna

### Brand
| Token | Hex | Penggunaan |
|---|---|---|
| `primary` | `#F04A23` | Tombol utama, item sidebar aktif, tab aktif, ikon aktif |
| `primaryPressed` | `#D63C18` | State pressed |
| `primaryContainer` | `#FDEDE8` | Latar chip/kartu terpilih |
| `onPrimary` | `#FFFFFF` | Teks/ikon di atas primary |

### Neutral
| Token | Hex | Penggunaan |
|---|---|---|
| `background` | `#F4F4F5` | Latar halaman |
| `surface` | `#FFFFFF` | Kartu, sidebar, panel, dialog |
| `surfaceVariant` | `#F8F8F9` | Item di dalam panel, hover baris tabel |
| `outline` | `#E7E7EA` | Border kartu, divider |
| `outlineStrong` | `#D4D4D8` | Border chip tidak terpilih |
| `textPrimary` | `#18181B` | Judul, nama, harga |
| `textSecondary` | `#71717A` | Subjudul, deskripsi, label |
| `textTertiary` | `#A1A1AA` | Placeholder, label grup sidebar, caption |
| `scrim` | `#000000` 45% | Overlay di belakang dialog |

### Semantic
| Token | Hex | Latar (container) | Penggunaan |
|---|---|---|---|
| `info` | `#2563EB` | `#E8F0FE` | Stok tinggi, status New Order |
| `warning` | `#EA7A0B` | `#FFF1E0` | Stok menipis, status Preparing |
| `danger` | `#E5391B` | `#FDECEA` | Label diskon, qty `x1`, blok catatan |
| `success` | `#16A34A` | `#E7F6EC` | Status Ready/Completed |
| `purple` | `#7C3AED` | – | Ikon "Pickup Order" |
| `badgeRed` | `#EF4444` | – | Counter bulat di sidebar |

### Badge stok (di atas foto produk)
| Kondisi | Teks | Latar |
|---|---|---|
| Stok tinggi (> 50) | `info` | `#E8F0FE` |
| Stok sedang (20–50) | `textPrimary` | `surface` |
| Stok rendah (< 20) | `danger` / `warning` | container merah/oranye |
| Habis | `textTertiary` | `outline` + overlay "Sold out" |

### Dark mode (opsional)
| Token | Hex |
|---|---|
| background | `#0F0F11` |
| surface | `#18181B` |
| surfaceVariant | `#202024` |
| outline | `#2E2E33` |
| textPrimary | `#F4F4F5` |
| textSecondary | `#A1A1AA` |
| primary | `#FF6A45` |
| primaryContainer | `#3A1A12` |

---

## 3. Tipografi

Font: **Inter** (fallback Plus Jakarta Sans / Roboto).

| Style | Size | Weight | Line height | Penggunaan |
|---|---|---|---|---|
| `headlineMedium` | 20sp | SemiBold 600 | 28sp | Judul halaman |
| `titleLarge` | 18sp | SemiBold | 24sp | Judul dialog, ID order |
| `titleMedium` | 14sp | SemiBold | 20sp | Nama produk, nama pelanggan |
| `bodyLarge` | 14sp | Regular | 20sp | Teks utama |
| `bodyMedium` | 12sp | Regular | 16sp | Subjudul, deskripsi |
| `labelLarge` | 12sp | Medium 500 | 16sp | Tombol, chip, tab, harga |
| `labelMedium` | 11sp | Medium | 14sp | Label form, header tabel |
| `labelSmall` | 10sp | Medium | 12sp | Badge, caption, label grup sidebar |

Untuk tablet, ukuran boleh dinaikkan 1–2sp.

---

## 4. Spacing, Radius, Elevasi

**Spacing (grid 4dp):** 2 · 4 · 8 · 12 · 16 · 20 · 24 · 32

| Area | Nilai |
|---|---|
| Padding halaman | 24dp |
| Gap antar kartu grid | 12dp |
| Padding dalam kartu | 8–12dp |
| Padding dalam panel | 16dp |

**Radius**
| Token | Nilai | Penggunaan |
|---|---|---|
| XS | 6dp | Badge |
| S | 10dp | Chip option, kartu item, input |
| M | 14dp | Kartu produk, kategori, kartu order |
| L | 20dp | Panel utama, dialog |
| Full | pill | Tombol, sidebar aktif, search, tab, segmented |
| Foto produk | 12dp | Gambar di kartu |

**Elevasi**
| Level | Spec |
|---|---|
| Kartu | Putih, border 1dp `outline`, tanpa shadow |
| Kartu terpilih | `primaryContainer` + border 1dp `primary` |
| Dialog / bottom sheet | Shadow lembut `0 8 24 rgba(0,0,0,.12)` |

**Ikon:** gaya outline 1.5dp, ujung rounded, 20dp (sidebar 18dp, chip 16dp, badge 12dp). Rekomendasi: Lucide / Phosphor / Material Symbols Rounded.

**Motion:** tap 100ms (scale 0.97) · pindah chip/tab 200ms · dialog masuk 250ms (fade + slide-up 16dp) · keluar 180ms · item masuk keranjang 200ms.

---

## 5. Layout Global

### Breakpoint
| Lebar | Layout |
|---|---|
| ≥ 840dp (utama) | Sidebar penuh 200dp + konten + panel kanan |
| 600–839dp | Navigation rail (ikon + label) |
| < 600dp | Bottom navigation, panel kanan jadi bottom sheet |

### Kerangka (Expanded)
```
┌──────────┬────────────────────────────────────┬──────────────┐
│ Sidebar  │ Header: judul + tanggal | search + filter          │
│ 200dp    ├────────────────────────────────────┬──────────────┤
│          │ Konten utama (fleksibel)           │ Panel kanan  │
│          │                                    │ 320dp        │
└──────────┴────────────────────────────────────┴──────────────┘
```
- Sidebar: `surface`, border kanan 1dp `outline`.
- Konten: padding 24dp.
- Panel kanan: `surface`, radius 20dp, margin 12dp.
- Grid produk: 3 kolom (Expanded), 2–3 (Medium), 2 (Compact). Gap 12dp.

---

## 6. Komponen

### Sidebar
- Atas: logo maskot cangkir 28dp + "SuKopi - POS" (`titleMedium`), lalu divider tipis.
- Label grup "Menu" dan "Other": `labelSmall`, `textTertiary`.
- Item: tinggi 40dp, pill, ikon 18dp + label `labelLarge`, gap 10dp, padding horizontal 12dp.
  - Normal: ikon & teks `textSecondary`, latar transparan.
  - **Aktif:** latar `primary`, ikon & teks putih.
  - Pressed: `primaryContainer`.
- Badge counter (mis. Order List): lingkaran 16dp `badgeRed`, angka putih `labelSmall`, di kanan item.
- Grup "Other" (Settings, Help) di paling bawah.
- Urutan menu: Order, Order List, Report, Menu, Inventory, Barista.

### Page Header
- Kiri: judul `headlineMedium` + subjudul `bodyMedium` `textSecondary`.
- Kanan: **Search bar** (lebar ~220dp, tinggi 40dp, pill, border `outline`, ikon search kiri, placeholder "Search...") + **tombol filter** (lingkaran 40dp, ikon sliders horizontal).

### Kartu Kategori
- 5 kartu sejajar rata lebar, tinggi ~64dp, radius 14dp.
- Isi: ikon 16dp, nama `labelLarge`, jumlah ("6 items") `labelSmall` `textTertiary`.
- Normal: `surface` + border `outline`.
- **Terpilih:** `primaryContainer` + border `primary`, ikon & teks `primary`.

### Kartu Produk
- `surface`, radius 14dp, border `outline`, padding 8dp.
- Foto rasio 1:1, radius 12dp, crop.
- Badge stok pill di kiri atas foto (offset 8dp), teks `labelSmall`, format "53 Stocks".
- Nama `titleMedium`, 1 baris, ellipsis.
- Baris harga: kiri "USD 2.15" `labelLarge`; kanan opsional "30% Off" `labelSmall` warna `danger`.
- Pressed: scale 0.98. Stok habis: overlay putih 60% + "Sold out".

### Segmented Control
- Tinggi 36dp, pill, border `outline`.
- Segmen aktif: `primaryContainer` + border `primary` + teks `primary`. Tidak aktif: teks `textSecondary`.

### Option Chip
- Tinggi 36dp, padding horizontal 12dp, teks `labelLarge`.
- Bentuk: pill (Type, Size, Indoor/Outdoor) atau radius 10dp (Sugar, Milk).
- Normal: `surface` + border `outlineStrong`.
- **Terpilih:** `primaryContainer` + border `primary` + teks `primary`.
- Boleh memakai ikon di kiri (es, api, kursi, pohon) atau tambahan harga di label ("Large (+$4.00)").

### Add-on Chip
- Grid 2 kolom, tinggi 36dp, radius 10dp.
- Nama di kiri, harga di kanan ("+$0.30", `textSecondary`).
- Terpilih: border `primary` + `primaryContainer`.

### Status Badge (pill kecil: titik 6dp + teks `labelSmall`)
| Status | Teks/titik | Latar |
|---|---|---|
| New Order | `info` | `#E8F0FE` |
| Preparing | `warning` | `#FFF1E0` |
| Ready | `success` | `#E7F6EC` |
| Completed | `textSecondary` | `outline` |
| Cancelled | `danger` | `#FDECEA` |

### Tag Tipe Order (ikon + label `labelSmall`)
- Pickup Order: ikon box, `purple`.
- Delivery Order: ikon truk, `info`.
- Dine In: ikon garpu-pisau, `success`, plus nomor meja/jam.

### Kartu Order (list kiri di Order List)
- Radius 14dp, padding 12dp, border `outline`. Terpilih: `primaryContainer` + border `primary`.
- Baris 1: nama (`titleMedium`) kiri, jam + ikon jam (`labelSmall`, `textTertiary`) kanan.
- Baris 2: tag tipe order.
- Baris 3: status badge kiri, total harga kanan (`titleMedium`).

### Tabs
- Pill tinggi 36dp. Aktif: `primary` + teks putih + angka badge ("Active 5"). Tidak aktif: `surface` + teks `textSecondary`.

### Blok Catatan Pelanggan
- Latar `#FDECEA`, radius 10dp, padding 12dp.
- Judul "Customer Notes" dengan ikon info (`danger`, `labelLarge`), isi `bodyMedium` `textPrimary`.

### Tabel Produk
- Header: tinggi 40dp, `surface`, teks `labelMedium` `textSecondary`. Kolom: checkbox · Product · Category · SKU · Stock.
- Baris: tinggi 64dp, divider 1dp `outline`, hover `surfaceVariant`.
  - Produk: thumbnail 36dp radius 10dp + nama `titleMedium` + sub-label `labelSmall` `textTertiary`.
  - Category & SKU: `bodyMedium` `textSecondary`.
  - Stock: angka; warna `warning` bila rendah.
- Toolbar: dropdown "Category: All", "Sort: Name", teks "19 products".
- Di phone: berubah jadi list kartu.

### Dropdown Pill
- Tinggi 32dp, border `outline`, label kecil + nilai + chevron. Menu rounded 12dp.

### Tombol
| Tipe | Spec |
|---|---|
| Primary | `primary`, teks putih, tinggi 48dp, pill, `labelLarge` |
| Secondary | `surface`, border `outlineStrong`, teks `textPrimary` |
| Tonal | `primaryContainer`, teks `primary` |
| Icon | Lingkaran 40dp, border `outline`, ikon 20dp |
| Destructive | Outline `danger`, teks `danger` |
| Disabled | Opacity 40% |

---

## 7. Tampilan Layar

### 7.1 Order (Kasir)
Tiga area: Sidebar · Konten · Panel keranjang.

**Konten:** header ("Let's do your best today 🚀" + tanggal "March 5, 2026" + search + filter) → baris 5 kartu kategori → grid produk 3 kolom.

**Panel keranjang (320dp)**, dari atas ke bawah:
1. Label "Customer Name" + ikon pensil di kanan; di bawahnya ID kecil (`#12132312`).
2. Segmented `Dine in` | `Take away`.
3. Teks "Where will you eat :" lalu 2 chip `Indoor` | `Outdoor`.
4. Teks "Your order :".
5. Area daftar item. Kosong: ikon tas belanja 32dp + "No items yet" (`textTertiary`), di tengah.
6. Footer saat ada item: subtotal, diskon, pajak, total (`titleLarge`), tombol primary full-width 48dp.

**Baris item keranjang:** thumbnail 48dp (radius 10dp), nama `titleMedium`, ringkasan opsi `labelSmall` `textSecondary` ("Hot · Large · Less · Oat Milk"), harga, stepper `− 1 +` (tombol 28dp bulat).

### 7.2 Dialog Kustomisasi Produk
- Tablet: dialog modal ~360dp, tinggi hingga 90% layar, scroll, radius 20dp, scrim 45%. Phone: bottom sheet full-height.
- Susunan:
  1. Foto hero tinggi ~160dp full-width, sudut atas radius 20dp, tombol close (X) bulat putih di kanan atas.
  2. Nama `titleLarge` + deskripsi `bodyMedium` `textSecondary`.
  3. **Type:** `Iced` | `Hot` (2 kolom, pill, ikon).
  4. **Size:** `Regular` | `Large (+$4.00)`.
  5. **Sugar Level:** `Normal` | `Less` | `No Sugar`.
  6. **Milk:** `Fresh Milk` · `Oat Milk` · `Soy Milk` · `Almond Milk` · `No Milk`.
  7. **Add-ons:** grid 2 kolom (Extra Shot, Whipped Cream, Caramel Drizzle, Hazelnut Syrup, Vanilla Syrup).
  8. Footer menempel di bawah: stepper qty + tombol primary "Add to order — USD x.xx".
- Judul section: `labelMedium` `textSecondary`; jarak antar section 16dp.
- Default terpilih: Iced, Regular, Normal, Fresh Milk.

### 7.3 Order List (Master–Detail)
**Kiri (~320dp):** Tabs `Active (5)` | `Completed (3)`, lalu daftar kartu order.

**Kanan (fleksibel), satu kartu detail:**
- Header: ID `#ORD-20260305-001` (`titleLarge`) + status badge di kanan.
- Baris meta: ikon jam + jam, tag tipe order, chip sumber ("via GrabFood").
- Blok pelanggan: nama `titleMedium` + "Customer : DANA".
- "Order Items (3)": daftar kartu item, masing-masing thumbnail 40dp, nama, rincian opsi (`labelSmall`), harga, qty "x1" merah (`danger`, SemiBold).
- Blok Customer Notes (merah muda).
- Footer: ringkasan harga + tombol aksi (primary dan secondary).
- Tidak ada order terpilih: ilustrasi + teks "Select an order to see details".

### 7.4 Menu (Katalog)
Header ("Menu" + "Manage your menu catalog" + search + filter) → toolbar (dropdown Category, Sort, "19 products", tombol "+ Add Product" di kanan) → tabel produk dalam kartu putih radius 14dp.

Form edit produk: foto di atas, lalu field bertumpuk (nama, deskripsi, kategori, SKU, harga, diskon), kemudian grup opsi. Field: tinggi 48dp, radius 10dp, border `outline`, fokus border `primary`.

### 7.5 Report
- Filter periode: segmented (Today / 7D / 30D / Custom).
- Baris 4 kartu KPI (radius 14dp): label `labelMedium`, angka `headlineMedium`, delta kecil (`success` / `danger`).
- Grafik garis penjualan (garis `primary`, area fill 10%), bar chart per kategori, daftar Top Products (thumbnail + nama + jumlah terjual).

### 7.6 Inventory
- Tabel seperti Menu, kolom: Item, SKU, Stok, Satuan, Min. stok, Status (badge: In stock = `success`, Low = `warning`, Out = `danger`).
- Banner `warning` container di atas bila ada stok menipis.

### 7.7 Barista
- Tiga kolom (New · Preparing · Ready), header kolom dengan badge jumlah.
- Kartu tiket: nomor order besar, nama pelanggan, daftar item dengan opsi (font +2sp), timer di pojok (`warning` / `danger` jika lama), blok catatan.

### 7.8 Settings & Help
- Daftar section dalam kartu putih radius 14dp; tiap baris: ikon 20dp, judul `titleMedium`, deskripsi `bodyMedium`, chevron atau switch (aktif `primary`).

---

## 8. State Tampilan

| State | Tampilan |
|---|---|
| Loading | Skeleton shimmer abu pada kartu/baris (bukan spinner penuh layar) |
| Kosong | Ikon + teks `textTertiary` di tengah |
| Error | Banner `danger` container + tombol "Retry" |
| Sukses | Snackbar bawah, 3 detik |
| Fokus (D-pad/keyboard) | Outline `primary` 2dp |
| Disabled | Opacity 40% |

---

## 9. Aksesibilitas Visual
- Kontras teks ≥ 4.5:1 (cek `primary` di atas `primaryContainer` dan teks badge kecil).
- Target sentuh ≥ 48dp.
- Jangan mengandalkan warna saja (stok rendah: tambahkan ikon/teks).
- Layout tahan font scale hingga 1.3×.

---

## 10. Kode Theme (Compose)

**Color.kt**
```kotlin
val Primary = Color(0xFFF04A23)
val PrimaryPressed = Color(0xFFD63C18)
val PrimaryContainer = Color(0xFFFDEDE8)
val OnPrimary = Color(0xFFFFFFFF)

val Background = Color(0xFFF4F4F5)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF8F8F9)
val Outline = Color(0xFFE7E7EA)
val OutlineStrong = Color(0xFFD4D4D8)

val TextPrimary = Color(0xFF18181B)
val TextSecondary = Color(0xFF71717A)
val TextTertiary = Color(0xFFA1A1AA)

val Info = Color(0xFF2563EB);    val InfoContainer = Color(0xFFE8F0FE)
val Warning = Color(0xFFEA7A0B); val WarningContainer = Color(0xFFFFF1E0)
val Danger = Color(0xFFE5391B);  val DangerContainer = Color(0xFFFDECEA)
val Success = Color(0xFF16A34A); val SuccessContainer = Color(0xFFE7F6EC)
val Purple = Color(0xFF7C3AED)
```

**Warna tambahan di luar Material (CompositionLocal)**
```kotlin
@Immutable
data class SukopiColors(
    val info: Color, val infoContainer: Color,
    val warning: Color, val warningContainer: Color,
    val danger: Color, val dangerContainer: Color,
    val success: Color, val successContainer: Color,
    val textTertiary: Color, val outlineStrong: Color, val purple: Color,
)
val LocalSukopiColors = staticCompositionLocalOf<SukopiColors> { error("not provided") }
object SukopiTheme {
    val colors: SukopiColors @Composable get() = LocalSukopiColors.current
}
```

**Theme.kt**
```kotlin
private val LightColors = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer, onPrimaryContainer = Primary,
    background = Background, onBackground = TextPrimary,
    surface = Surface, onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant, onSurfaceVariant = TextSecondary,
    outline = Outline, error = Danger,
)

@Composable
fun SukopiTheme(content: @Composable () -> Unit) {
    val extended = SukopiColors(
        Info, InfoContainer, Warning, WarningContainer,
        Danger, DangerContainer, Success, SuccessContainer,
        TextTertiary, OutlineStrong, Purple,
    )
    CompositionLocalProvider(LocalSukopiColors provides extended) {
        MaterialTheme(colorScheme = LightColors, typography = SukopiTypography, shapes = SukopiShapes, content = content)
    }
}
```

**Type.kt**
```kotlin
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val SukopiTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge      = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.1.sp),
    labelSmall     = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium,   fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.2.sp),
)
```

**Shape.kt & Dimens.kt**
```kotlin
val SukopiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(14.dp),
    large      = RoundedCornerShape(20.dp),
)
val PillShape = RoundedCornerShape(percent = 50)

object Dimens {
    val SidebarWidth = 200.dp
    val CartPanelWidth = 320.dp
    val PagePadding = 24.dp
    val CardGap = 12.dp
    val ChipHeight = 36.dp
    val SearchHeight = 40.dp
}
```

### Contoh komponen

**Kartu produk**
```kotlin
@Composable
fun ProductCard(product: ProductUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        enabled = product.stock > 0,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(8.dp)) {
            Box {
                AsyncImage(
                    model = product.imageUrl, contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                )
                StockBadge(product.stock, Modifier.padding(8.dp).align(Alignment.TopStart))
            }
            Spacer(Modifier.height(8.dp))
            Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(product.priceLabel, style = MaterialTheme.typography.labelLarge)
                product.discountPercent?.let {
                    Text("$it% Off", style = MaterialTheme.typography.labelSmall, color = SukopiTheme.colors.danger)
                }
            }
        }
    }
}
```

**Option chip**
```kotlin
@Composable
fun OptionChip(
    label: String, selected: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, shape: Shape = PillShape,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val border = if (selected) MaterialTheme.colorScheme.primary else SukopiTheme.colors.outlineStrong
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dimens.ChipHeight),
        shape = shape, color = container, contentColor = content,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            leadingIcon?.invoke()
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

**Item sidebar**
```kotlin
@Composable
fun SidebarItem(icon: ImageVector, label: String, selected: Boolean, badgeCount: Int = 0, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(PillShape).background(bg)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        if (badgeCount > 0) CountBadge(badgeCount)
    }
}
```

**Scaffold adaptif**
```kotlin
@Composable
fun SukopiScaffold(windowSize: WindowSizeClass, content: @Composable () -> Unit) {
    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> Row { SukopiSidebar(); content() }
        WindowWidthSizeClass.Medium   -> Row { SukopiNavRail(); content() }
        else -> Scaffold(bottomBar = { SukopiBottomBar() }) { content() }
    }
}
```

---

## 11. Aset
- Foto produk: rasio 1:1, minimal 600×600px, WebP.
- Placeholder/error gambar: latar `surfaceVariant` + ikon cangkir.
- Ikon: vector drawable / `ImageVector`, basis 24dp.
- Font: `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` di `res/font`.

---

## 12. Catatan
1. Ikuti token di dokumen ini; hindari warna/ukuran di luar token.
2. Pakai Material 3 sebagai basis, tetapi override warna, bentuk, dan tipografi agar tidak tampil sebagai default M3.
3. Bila mock dan dokumen berbeda, **mock visual yang menang**.
4. Layar tanpa referensi visual (Report, Inventory, Barista, Settings) mengikuti pola komponen yang sama, tanpa gaya baru.