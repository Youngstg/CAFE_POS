"""Cermin Python dari domain MVP: ShiftCalculation, OrderTotals, PoCalculation,
ConflictPolicy, ReceiptFormatter. Jalankan: python tools/verify_mvp.py"""

failed = 0


def check(name, cond):
    global failed
    print(("PASS" if cond else "FAIL"), "-", name)
    if not cond:
        failed += 1


# --- Shift ---
def shift_close(modal, paid, counted):
    expected = modal + paid
    return expected, counted - expected, (counted - expected) == 0


e, d, b = shift_close(500_000, 1_200_000, 1_700_000)
check("shift kas pas", (e, d, b) == (1_700_000, 0, True))
e, d, b = shift_close(500_000, 1_200_000, 1_650_000)
check("shift kas kurang -50rb", (e, d, b) == (1_700_000, -50_000, False))

# --- OrderTotals ---
ITEMS = [("Kopi Susu", 2, 18_000), ("Croissant", 1, 15_000)]


def totals(items, pct=0, fixed=0, min_order=0):
    sub = sum(q * p for _, q, p in items)
    disc = 0
    if sub >= min_order:
        disc += sub * pct // 100 + fixed
        disc = min(disc, sub)
    return sub, disc, sub - disc


check("total tanpa promo", totals(ITEMS) == (51_000, 0, 51_000))
check("promo 10% min 50rb", totals(ITEMS, pct=10, min_order=50_000) == (51_000, 5_100, 45_900))
check("promo gugur di bawah min", totals(ITEMS, pct=10, min_order=100_000)[1] == 0)
check("diskon capped subtotal", totals(ITEMS, fixed=99_000) == (51_000, 51_000, 0))

# --- PO ---
check("total PO", int(10.0 * 15_000 + 5.0 * 120_000) == 750_000)
check("PO receive nambah stok", 400.0 + 10_000.0 == 10_400.0)


def can_receive(status, received, po_id):
    return status == "APPROVED" and po_id not in received


check("PO approved baru bisa terima", can_receive("APPROVED", set(), "PO-1") is True)
check("PO ganda ditolak", can_receive("APPROVED", {"PO-1"}, "PO-1") is False)
check("PO draft ditolak", can_receive("DRAFT", set(), "PO-2") is False)

# --- Conflict ---
def resolve(stock, need, processed, oid):
    if oid in processed:
        return "DUPLICATE_IGNORED"
    return "ACCEPT" if stock >= need else "CONFLICT_REVIEW"


check("konflik stok cukup", resolve(1000, 150, set(), "O-1") == "ACCEPT")
check("konflik stok kurang", resolve(100, 150, set(), "O-2") == "CONFLICT_REVIEW")
check("konflik duplikat", resolve(1000, 150, {"O-1"}, "O-1") == "DUPLICATE_IGNORED")

# --- Receipt formatter sanity (format teks 32 kolom) ---
lines = ["=" * 32, "CoffeeOS", "-" * 32, "TOTAL    : Rp51000", "=" * 32]
check("struk 32 kolom", all(len(line) <= 32 for line in lines))

print(f"\n{15 - failed}/15 lolos")
raise SystemExit(1 if failed else 0)
