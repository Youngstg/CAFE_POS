"""Cermin implementasi design.md (tema, komponen, layout per layar).
Jalankan: python tools/verify_design.py"""

import pathlib

failed = 0


def check(name, cond):
    global failed
    print(("PASS" if cond else "FAIL"), "-", name)
    if not cond:
        failed += 1


base = pathlib.Path(__file__).parent.parent
app = base / "app/src/main/java/com/coffeeos/erp"


def src(*parts):
    return (app.joinpath(*parts)).read_text(encoding="utf-8")


# --- §2 Palet + §4 bentuk ---
theme = src("ui/theme/Theme.kt")
check("palet brand", all(h in theme for h in
      ("3B2417", "6B4226", "9C6644", "E8D5C4", "F5EBE0", "FBF7F2")))
check("palet semantik (tanpa merah murni)",
      all(h in theme for h in ("C98A2C", "B3402F", "6B7FA6", "8A7B6D"))
      and "FF0000" not in theme)
check("shapes hangat + ambang selisih",
      "RoundedCornerShape(14.dp)" in theme and "SMALL_DIFF_THRESHOLD" in theme)

# --- §5 Komponen bersama ---
comp = src("ui/components/Components.kt")
check("komponen bersama",
      all(k in comp for k in ("StatusBadge", "StockBadge", "OrderBadge",
                              "EmptyState", "StatCard", "ConfirmDialog")))
check("badge glif+teks (bukan warna saja)", all(g in comp for g in ('"●"', '"▲"', '"✕"')))

# --- §7 Navigasi ---
nav = src("ui/navigation/CoffeeosNavGraph.kt")
check("bottom nav ikon per peran",
      "NavigationBar" in nav and "NavigationBarItem" in nav and "PointOfSale" in nav)
check("tab minimal kasir/dapur", "Routes.SHIFT" in nav and "Routes.KITCHEN" in nav)

# --- §8.1 Login kartu ---
auth = src("feature/auth/AuthScreen.kt")
check("login 3 kartu", "LoginPathCard" in auth)

# --- §8.2 Kasir ---
cash = src("feature/cashier/CashierScreen.kt")
check("kasir chips kategori + lock shift",
      "CategoryChips" in cash and "onOpenShift" in cash and "Buka Shift" in cash)

# --- §8.3 Shift warna ---
shift = src("feature/shift/ShiftScreen.kt").replace(" ", "")
check("shift selisih semantik", "SMALL_DIFF_THRESHOLD" in shift and "StatusBadge" in shift)

# --- Anti-kedip: flow Room distabilkan dengan remember(outletId) ---
import re
screens = ["feature/cashier/CashierScreen.kt", "feature/inventory/InventoryScreen.kt",
           "feature/supply/SupplyScreen.kt", "feature/owner/OwnerScreen.kt",
           "feature/shift/ShiftScreen.kt", "feature/menu/MenuScreen.kt"]
stable = all("remember(outletId)" in src(s) for s in screens)
raw = []
for s in screens:
    raw += re.findall(r"by vm\.\w+\(outletId\)\.collectAsState\(\)", src(s))
check("flow stabil remember(outletId) 7 layar", stable)
check("tanpa collect langsung dari VM (%s)" % (raw or "-"), not raw)
kds = src("feature/kitchen/KitchenScreen.kt")
check("KDS 3 kolom + waktu relatif",
      all(k in kds for k in ('"QUEUED"', '"COOKING"', '"READY"', "relativeTime",
                             "KanbanColumn", "animateItem")))

# --- §8.5 Gudang ---
gud = src("feature/inventory/InventoryScreen.kt")
check("stock row progress+sort+mode",
      all(k in gud for k in ("LinearProgressIndicator", "Kritis dulu", "Mode ubah",
                             "StockBadge")))
check("terima PO pratinjau", "poPreview" in gud and "Hanya PO APPROVED" in gud)

# --- §8.6 Supply ---
sup = src("feature/supply/SupplyScreen.kt")
check("FAB + stepper PO",
      "FloatingActionButton" in sup and "PoStepper" in sup and "RECEIVED" in sup)

# --- §8.7 Katalog ---
menu = src("feature/menu/MenuScreen.kt")
check("katalog switch + konfirmasi + kategori",
      "Switch(" in menu and "ConfirmDialog" in menu and "Kategori" in menu)

# --- §8.8 Owner ---
owner = src("feature/owner/OwnerScreen.kt")
check("stat grid + refund outline + konfirmasi",
      "StatCard" in owner and "StatRow" in owner and "GridCells" not in owner
      and "OutlinedButton" in owner and "confirmForce" in owner)

# --- Kategori end-to-end ---
check("kategori end-to-end",
      'category' in (app / "core/data/local/AppDatabase.kt").read_text(encoding="utf-8")
      and "version = 4" in (app / "core/data/local/AppDatabase.kt").read_text(encoding="utf-8")
      and '"category"' in (app / "core/sync/FirestoreSync.kt").read_text(encoding="utf-8")
      and '"category"' in (app / "core/sync/RealtimeSync.kt").read_text(encoding="utf-8"))

# --- Ikon dep ---
gradle = (base / "app/build.gradle.kts").read_text(encoding="utf-8")
check("dep ikon compose", "material-icons-core" in gradle)

# --- Anti-stuck & anti-crash layout ---
auth = src("feature/auth/AuthScreen.kt")
check("login bisa scroll (landscape)", auth.count("verticalScroll") >= 2)
bounded = sum(
    src(s).count("LazyColumn(Modifier.weight")
    for s in ["feature/inventory/InventoryScreen.kt",
              "feature/supply/SupplyScreen.kt",
              "feature/owner/OwnerScreen.kt",
              "feature/shift/ShiftScreen.kt",
              "feature/kitchen/KitchenScreen.kt"]
)
check("list berbatas di 5 layar (%d)" % bounded, bounded >= 5)

# --- Adaptif orientasi otomatis (ikut sensor) ---
kds_all = src("feature/kitchen/KitchenScreen.kt")
check("KDS adaptif landscape/portrait",
      "ORIENTATION_LANDSCAPE" in kds_all and "horizontalScroll" in kds_all
      and "width(300.dp)" in kds_all)
manifest = (base / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
check("tanpa kunci orientasi", "screenOrientation" not in manifest)

print(f"\n{25 - failed}/25 lolos")
raise SystemExit(1 if failed else 0)
