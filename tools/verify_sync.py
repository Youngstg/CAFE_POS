"""Cermin Python dari FirestoreSync + payload ORDER_UPSERT.
Jalankan: python tools/verify_sync.py"""

import re

failed = 0


def check(name, cond):
    global failed
    print(("PASS" if cond else "FAIL"), "-", name)
    if not cond:
        failed += 1


# --- 1. Format payload ORDER_UPSERT (dibangun di OrderRepository.checkout) ---
def build_order_payload(order_id, total, needs: dict):
    items = ",".join(
        '{"ingredientId":"%s","qty":%s}' % (k, v) for k, v in needs.items()
    )
    return '{"orderId":"%s","total":%d,"needs":[%s]}' % (order_id, total, items)


def parse_needs(payload):
    return [
        (m.group(1), float(m.group(2)))
        for m in re.finditer(r'"ingredientId"\s*:\s*"([^"]+)"\s*,\s*"qty"\s*:\s*([0-9.]+)', payload)
    ]


p = build_order_payload("O-ABC123", 51000, {"ing-susu": 300.0, "ing-kopi": 36.0})
needs = dict(parse_needs(p))
check("payload needs round-trip", needs == {"ing-susu": 300.0, "ing-kopi": 36.0})
check("payload bawa orderId+total",
      '"orderId":"O-ABC123"' in p and '"total":51000' in p)

# --- 2. Keputusan transaction FIFO (cermin blok ORDER_UPSERT) ---
def txn_decide(server_stocks: dict, needs: dict):
    """True = kalah -> CONFLICT_NEED_REVIEW; False = sukses + decrement."""
    loser = any(server_stocks.get(k, 0.0) < v for k, v in needs.items())
    if loser:
        return True, server_stocks
    return False, {k: server_stocks[k] - needs.get(k, 0.0) for k in server_stocks}


loser, _ = txn_decide({"ing-susu": 8000.0, "ing-kopi": 4000.0}, needs)
check("stok server cukup -> terima", loser is False)
_, after = txn_decide({"ing-susu": 8000.0, "ing-kopi": 4000.0}, needs)
check("decrement benar", after == {"ing-susu": 7700.0, "ing-kopi": 3964.0})
loser, _ = txn_decide({"ing-susu": 100.0, "ing-kopi": 4000.0}, needs)
check("stok kurang 1 bahan -> konflik", loser is True)

# --- 3. FIFO: createdAt terkecil diproses dulu ---
queue = [("m3", 300), ("m1", 100), ("m2", 200)]
order = [mid for mid, _ in sorted(queue, key=lambda x: x[1])]
check("FIFO sesuai createdAt", order == ["m1", "m2", "m3"])

# --- 4. Idempotency: mutationId ganda tidak diproses ulang ---
seen, processed = set(), []
for mid in ["a", "b", "a", "c"]:
    if mid in seen:
        continue
    seen.add(mid)
    processed.append(mid)
check("duplikat mutationId diabaikan", processed == ["a", "b", "c"])

# --- 5. Semua kind yang di-enqueue repo ditangani FirestoreSync ---
import pathlib
sync_src = pathlib.Path(__file__).parent.parent.joinpath(
    "app/src/main/java/com/coffeeos/erp/core/sync/FirestoreSync.kt").read_text()
repo_dir = pathlib.Path(__file__).parent.parent.joinpath(
    "app/src/main/java/com/coffeeos/erp/core/data/repo")
enqueued = set()
for f in repo_dir.glob("*.kt"):
    enqueued.update(re.findall(r'"([A-Z_]+)"', f.read_text()))
enqueued = {k for k in enqueued if "_" in k or k.startswith("ORDER") or k.startswith("PO")
            or k.startswith("SHIFT") or k.startswith("SUPPLIER") or k.startswith("CONFLICT")
            or k.startswith("STOCK")}
handled = set(re.findall(r'"([A-Z_]+)"', sync_src))
missing = {k for k in enqueued if k not in handled and k not in ("QUEUED", "PAID")}
check("semua kind ditangani worker (kurang: %s)" % (missing or "-"), not missing)

print(f"\n{9 - failed}/9 lolos")
raise SystemExit(1 if failed else 0)
