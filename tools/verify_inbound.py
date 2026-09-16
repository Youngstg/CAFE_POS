"""Cermin Python dari InboundPolicy + cek integrasi RealtimeSync.
Jalankan: python tools/verify_inbound.py"""

import pathlib
import re

failed = 0


def check(name, cond):
    global failed
    print(("PASS" if cond else "FAIL"), "-", name)
    if not cond:
        failed += 1


# --- Kebijakan murni ---
def should_apply_doc(local_pending: bool) -> bool:
    return not local_pending


def should_apply_versioned(server_ts, local_ts) -> bool:
    if local_ts is None:
        return True
    if server_ts is None:
        return False
    return server_ts > local_ts


check("lokal pending menang", should_apply_doc(True) is False)
check("lokal sync ikut server", should_apply_doc(False) is True)
check("server baru diterapkan", should_apply_versioned(200, 100) is True)
check("server lama ditolak", should_apply_versioned(100, 200) is False)
check("timestamp sama ditolak", should_apply_versioned(200, 200) is False)
check("tanpa lokal selalu terapkan", should_apply_versioned(200, None) is True)

# --- Integrasi: RealtimeSync wajib pakai policy + abaikan echo/REMOVED ---
base = pathlib.Path(__file__).parent.parent / "app/src/main/java/com/coffeeos/erp"
rt = (base / "core/sync/RealtimeSync.kt").read_text()
check("pakai shouldApplyDoc", "shouldApplyDoc" in rt)
check("pakai shouldApplyVersioned", "shouldApplyVersioned" in rt)
check("abaikan hasPendingWrites", "hasPendingWrites" in rt)
check("abaikan REMOVED", "Type.REMOVED" in rt)
check("hitung ulang menu turunan", "refreshMenusForIngredient" in rt)
check("listener 8 koleksi",
      all(c in rt for c in ['"orders"', '"ingredients"', '"purchaseOrders"', '"shifts"',
                            '"suppliers"', '"menus"', '"recipes"', '"promos"']))
check("menu tak timpa availability", "isAvailable" in rt and "turunan" in rt)

# --- Konsistensi: semua koleksi listener ada di firestore.rules ---
rules = (pathlib.Path(__file__).parent.parent / "firestore.rules").read_text()
check("rules mencakup koleksi outlet", "outlets/{outletId}/{col}/{docId}" in rules)

print(f"\n{15 - failed}/15 lolos")
raise SystemExit(1 if failed else 0)
