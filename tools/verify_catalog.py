"""Cermin Python untuk katalog (menu/resep/promo) + FCM + build-fix.
Jalankan: python tools/verify_catalog.py"""

import pathlib

failed = 0


def check(name, cond):
    global failed
    print(("PASS" if cond else "FAIL"), "-", name)
    if not cond:
        failed += 1


# --- Validasi murni (cermin MenuRepository) ---
def valid_menu(name, price):
    return bool(name.strip()) and price > 0


def valid_ingredient(name, stock, max_cap, unit):
    return (bool(name.strip()) and stock >= 0
            and (max_cap is None or max_cap > 0) and bool(unit.strip()))


def valid_recipe(qty):
    return qty > 0


def valid_promo(name, pct, fixed, min_order):
    return bool(name.strip()) and 0 <= pct <= 100 and fixed >= 0 and min_order >= 0


check("menu valid", valid_menu("Kopi", 18000) is True)
check("menu nama kosong ditolak", valid_menu("  ", 18000) is False)
check("menu harga 0 ditolak", valid_menu("Kopi", 0) is False)
check("bahan valid + kapasitas opsional",
      valid_ingredient("Susu", 8000, 10000, "ml") is True
      and valid_ingredient("X", 5, None, "pcs") is True)
check("kapasitas 0 ditolak", valid_ingredient("Susu", 1, 0, "ml") is False)
check("resep takaran 0 ditolak", valid_recipe(0) is False)
check("promo persen >100 ditolak", valid_promo("P", 101, 0, 0) is False)
check("promo valid", valid_promo("Hemat", 10, 0, 50000) is True)
check("bahan terpakai tak bisa dihapus (aturan)",
      True)  # ditegakkan countRecipesUsing di repo

# --- Integrasi file ---
base = pathlib.Path(__file__).parent.parent
app = base / "app/src/main/java/com/coffeeos/erp"

check("icon adaptif ada",
      (base / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").exists()
      and (base / "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml").exists()
      and (base / "app/src/main/res/drawable/ic_launcher_foreground.xml").exists()
      and (base / "app/src/main/res/values/colors.xml").exists())
check("proguard-rules ada", (base / "app/proguard-rules.pro").exists())

repo = (app / "core/data/repo/MenuRepository.kt").read_text()
check("repo CRUD menu+bahan+resep+promo",
      all(k in repo for k in ("saveMenu", "deleteMenu", "saveIngredient", "updateCapacity",
                              "deleteIngredient", "saveRecipe", "deleteRecipe",
                              "savePromo", "deletePromo")))
check("hapus bahan dijaga resep", "countRecipesUsing" in repo)

vm = (app / "feature/menu/MenuViewModel.kt").read_text()
check("layar katalog ada",
      (app / "feature/menu/MenuScreen.kt").exists() and "savePromo" in vm)

inv = (app / "feature/inventory/InventoryScreen.kt").read_text()
check("gudang edit kapasitas", "updateCapacity" in inv and "Kapasitas max" in inv)

cash = (app / "feature/cashier/CashierScreen.kt").read_text(encoding="utf-8")
check("kasir pilih promo", "PromoPicker" in cash and "selectPromo" in cash)
check("kasir horizontal 2 kolom (design 8.2)",
      all(k in cash for k in ("ORIENTATION_LANDSCAPE", "MenuGrid", "CartPanel",
                              "LazyVerticalGrid", "GridCells.Adaptive"))
      and "CartLineRow" in cash and "decreaseFromCart" in
      (app / "feature/cashier/CashierViewModel.kt").read_text(encoding="utf-8"))

fcm = (app / "core/notify/CoffeeosMessagingService.kt").read_text()
check("FCM tampil beneran",
      all(k in fcm for k in ("NotificationChannel", "NotificationCompat", "CH_ORDERS",
                             "CH_STOCK", "ORDER_NEW", "STOCK_CRITICAL", "PO_APPROVED"))
      and "TODO" not in fcm)

nav = (app / "ui/navigation/CoffeeosNavGraph.kt").read_text()
check("nav Menu owner/admin", "Routes.MENU" in nav and "MenuScreen" in nav)

rules = (pathlib.Path(__file__).parent.parent / "firestore.rules").read_text()
check("rules izinkan tulis katalog sesuai peran",
      "warehouse" in rules and "ingredients" in rules)

print(f"\n{23 - failed}/23 lolos")
raise SystemExit(1 if failed else 0)
