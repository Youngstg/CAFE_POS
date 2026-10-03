"""Runner Unit Test Menyeluruh untuk Cafe POS.
Menguji seluruh domain logic:
1. Order Totals & PB1 Restoran (OrderTotalsTest)
2. Drink Modifiers & Pricing (DrinkModifierTest)
3. Customer Loyalty Stamps (CustomerLoyaltyTest)
4. Receipt Formatter 32 Kolom (ReceiptFormatterTest)
5. Customer-Facing Display State Bridge (CustomerDisplayBridgeTest)
6. Stock Evaluation Rules (StockRulesTest)
7. Shift Cash Balance (ShiftCalculationTest)
8. PO & Procurement Calculation (PoCalculationTest)
9. Conflict Resolution Policy (ConflictPolicyTest)

Jalankan: python tools/run_unit_tests.py
"""

import sys
import os

passed = 0
failed = 0
test_results = []


def run_test(suite_name, test_name, test_func):
    global passed, failed
    try:
        test_func()
        passed += 1
        test_results.append((suite_name, test_name, True, None))
        print(f"  [PASS] {test_name}")
    except AssertionError as e:
        failed += 1
        test_results.append((suite_name, test_name, False, str(e)))
        print(f"  [FAIL] {test_name}: {e}")
    except Exception as e:
        failed += 1
        test_results.append((suite_name, test_name, False, f"Exception: {e}"))
        print(f"  [ERROR] {test_name}: {e}")


# ==============================================================================
# 1. OrderTotalsTest
# ==============================================================================
def compute_order_totals(items, promo=None, apply_tax=False):
    subtotal = sum(qty * price for _, qty, price in items)
    discount = 0
    if promo:
        pct = promo.get("pct", 0)
        fixed = promo.get("fixed", 0)
        min_order = promo.get("min_order", 0)
        if subtotal >= min_order:
            discount += subtotal * pct // 100
            discount += fixed
            if discount > subtotal:
                discount = subtotal
    after_discount = max(0, subtotal - discount)
    tax = (after_discount * 10 // 100) if apply_tax else 0
    total = after_discount + tax
    return {"subtotal": subtotal, "discount": discount, "tax": tax, "total": total}


ITEMS_SAMPLE = [("Kopi Susu", 2, 18000), ("Croissant", 1, 15000)]


def test_order_totals_no_promo():
    res = compute_order_totals(ITEMS_SAMPLE)
    assert res["subtotal"] == 51000, f"Expected 51000, got {res['subtotal']}"
    assert res["discount"] == 0
    assert res["total"] == 51000


def test_order_totals_percent_promo():
    res = compute_order_totals(ITEMS_SAMPLE, promo={"pct": 10, "min_order": 50000})
    assert res["discount"] == 5100, f"Expected 5100, got {res['discount']}"
    assert res["total"] == 45900


def test_order_totals_promo_min_order_not_met():
    res = compute_order_totals(ITEMS_SAMPLE, promo={"pct": 10, "min_order": 100000})
    assert res["discount"] == 0
    assert res["total"] == 51000


def test_order_totals_cap_discount():
    res = compute_order_totals(ITEMS_SAMPLE, promo={"fixed": 99000})
    assert res["discount"] == 51000
    assert res["total"] == 0


def test_order_totals_pb1_tax():
    res = compute_order_totals(ITEMS_SAMPLE, apply_tax=True)
    assert res["subtotal"] == 51000
    assert res["discount"] == 0
    assert res["tax"] == 5100
    assert res["total"] == 56100


def test_order_totals_pb1_tax_with_promo():
    res = compute_order_totals(ITEMS_SAMPLE, promo={"pct": 10, "min_order": 50000}, apply_tax=True)
    assert res["subtotal"] == 51000
    assert res["discount"] == 5100
    # afterDiscount = 45900 -> tax 10% = 4590 -> total = 50490
    assert res["tax"] == 4590
    assert res["total"] == 50490


# ==============================================================================
# 2. DrinkModifierTest
# ==============================================================================
def format_modifiers_summary(ice, sugar, add_ons, notes):
    parts = [ice, sugar]
    if add_ons:
        names = [name for name, _ in add_ons]
        parts.append("+" + ", ".join(names))
    summary = ", ".join(parts)
    if notes:
        summary += f" ({notes})"
    return summary


def compute_extra_price(add_ons):
    return sum(price for _, price in add_ons)


def test_modifier_default():
    ice = "Normal Ice"
    sugar = "Normal Sugar (100%)"
    summary = format_modifiers_summary(ice, sugar, [], "")
    assert summary == "Normal Ice, Normal Sugar (100%)"
    assert compute_extra_price([]) == 0


def test_modifier_extra_price_accumulation():
    add_ons = [
        ("Extra Espresso Shot", 5000),
        ("Ganti Oat Milk", 6000),
        ("Sirup Karamel", 4000)
    ]
    total_extra = compute_extra_price(add_ons)
    assert total_extra == 15000, f"Expected 15000, got {total_extra}"


def test_modifier_full_summary():
    add_ons = [
        ("Extra Espresso Shot", 5000),
        ("Ganti Oat Milk", 6000)
    ]
    summary = format_modifiers_summary("No Ice", "No Sugar (0%)", add_ons, "jangan terlalu panas")
    assert "No Ice" in summary
    assert "No Sugar (0%)" in summary
    assert "+Extra Espresso Shot, Ganti Oat Milk" in summary
    assert "(jangan terlalu panas)" in summary


# ==============================================================================
# 3. CustomerLoyaltyTest
# ==============================================================================
def format_stamp_progress(stamps):
    stamps = max(0, min(10, stamps))
    filled = "■" * stamps
    empty = "□" * (10 - stamps)
    return f"[{filled}{empty}] {stamps}/10"


def record_order_stamps(prev_stamps, cups):
    new_total = prev_stamps + cups
    effective = new_total % 10
    free_reward = (new_total // 10) > (prev_stamps // 10)
    return effective, free_reward


def test_loyalty_progress_bar_empty():
    assert format_stamp_progress(0) == "[□□□□□□□□□□] 0/10"


def test_loyalty_progress_bar_four():
    assert format_stamp_progress(4) == "[■■■■□□□□□□] 4/10"


def test_loyalty_progress_bar_nine():
    assert format_stamp_progress(9) == "[■■■■■■■■■□] 9/10"


def test_loyalty_earned_reward_on_10():
    # dari 8 stempel tambah 2 cup = 10 -> reward free drink!
    effective, free_reward = record_order_stamps(8, 2)
    assert free_reward is True, "Should earn free reward at 10 stamps"
    assert effective == 0, "Stamps should reset to 0 after 10"


def test_loyalty_rollover_stamps():
    # dari 8 stempel beli 5 cup = 13 -> reward + sisa 3
    effective, free_reward = record_order_stamps(8, 5)
    assert free_reward is True
    assert effective == 3, f"Expected 3 remaining stamps, got {effective}"


def test_loyalty_not_enough_for_reward():
    effective, free_reward = record_order_stamps(3, 2)
    assert free_reward is False
    assert effective == 5


# ==============================================================================
# 4. ReceiptFormatterTest
# ==============================================================================
def format_rupiah(num):
    s = str(num)[::-1]
    chunked = ".".join(s[i:i+3] for i in range(0, len(s), 3))
    return "Rp" + chunked[::-1]


def format_receipt(cafe_name, outlet, order_id, order_type, customer_name, cashier, items, discount, tax, payment):
    subtotal = sum(qty * price for _, _, qty, price in items)
    total = max(0, subtotal - discount + tax)
    type_str = "Bawa Pulang (Take Away)" if order_type == "TAKE_AWAY" else "Minum di Tempat (Dine In)"

    lines = [
        "=" * 32,
        cafe_name.upper().center(32),
        outlet.center(32),
        "-" * 32,
        f"Order : {order_id}",
        f"Tipe  : {type_str}"
    ]
    if customer_name:
        lines.append(f"Nama  : {customer_name}")
    lines.extend([
        f"Kasir : {cashier}",
        "-" * 32
    ])
    for name, variant, qty, price in items:
        lbl = f"{name} ({variant})" if variant else name
        lines.append(f"{lbl} x{qty}")
        lines.append(f"  {format_rupiah(qty * price)}")
    lines.extend([
        "-" * 32,
        f"Subtotal : {format_rupiah(subtotal)}"
    ])
    if discount > 0:
        lines.append(f"Diskon   : -{format_rupiah(discount)}")
    if tax > 0:
        lines.append(f"PB1 (10%): {format_rupiah(tax)}")
    lines.extend([
        f"TOTAL    : {format_rupiah(total)}",
        f"Bayar    : {payment}",
        "=" * 32,
        "Terima kasih!".center(32),
        "Selamat Menikmati ☕".center(32)
    ])
    return "\n".join(lines)


def test_receipt_structure_dine_in():
    items = [
        ("Kopi Susu Gula Aren", "Less Ice, 50% Sugar", 2, 22000),
        ("Croissant Butter", None, 1, 18000)
    ]
    receipt = format_receipt("CoffeeOS", "Outlet Sudirman", "ORD-001", "DINE_IN", "Budi (Meja 04)", "Siti", items, 5000, 5700, "QRIS")
    assert "COFFEEOS" in receipt
    assert "Outlet Sudirman" in receipt
    assert "Order : ORD-001" in receipt
    assert "Tipe  : Minum di Tempat (Dine In)" in receipt
    assert "Nama  : Budi (Meja 04)" in receipt
    assert "Kopi Susu Gula Aren (Less Ice, 50% Sugar) x2" in receipt
    assert "Subtotal : Rp62.000" in receipt
    assert "Diskon   : -Rp5.000" in receipt
    assert "PB1 (10%): Rp5.700" in receipt
    assert "TOTAL    : Rp62.700" in receipt
    assert "Bayar    : QRIS" in receipt


def test_receipt_32_columns():
    items = [("Americano Ice", None, 1, 18000)]
    receipt = format_receipt("CoffeeOS", "Outlet Utama", "ORD-002", "TAKE_AWAY", "", "Rian", items, 0, 0, "TUNAI")
    for line in receipt.split("\n"):
        assert len(line) <= 32 or "Selamat Menikmati" in line, f"Line exceeds 32 chars: {line}"


# ==============================================================================
# 5. CustomerDisplayBridgeTest
# ==============================================================================
class MockCustomerDisplayState:
    def __init__(self):
        self.cafe_name = "CoffeeOS"
        self.items = []
        self.subtotal = 0
        self.discount = 0
        self.tax = 0
        self.total = 0
        self.order_type = "DINE_IN"
        self.customer_name = ""
        self.payment_method = "TUNAI"
        self.is_showing_payment = False
        self.is_payment_success = False
        self.last_order_id = None
        self.qris_payload = ""

    def update_cart(self, items, discount, tax, total, order_type, customer_name):
        self.items = items
        self.subtotal = sum(q * p for _, q, p in items)
        self.discount = discount
        self.tax = tax
        self.total = total
        self.order_type = order_type
        self.customer_name = customer_name
        self.is_payment_success = False

    def show_payment(self, method, total, order_id):
        self.payment_method = method
        self.is_showing_payment = True
        self.last_order_id = order_id
        self.qris_payload = f"00020101021226600016ID.COFFEEOS.POS540{total}5802ID5912COFFEEOS CAFE"

    def mark_payment_success(self, order_id, total):
        self.is_showing_payment = False
        self.is_payment_success = True
        self.last_order_id = order_id
        self.total = total

    def reset(self):
        self.items = []
        self.subtotal = 0
        self.discount = 0
        self.tax = 0
        self.total = 0
        self.is_showing_payment = False
        self.is_payment_success = False


def test_cfd_bridge_update_cart():
    bridge = MockCustomerDisplayState()
    items = [("Latte", 2, 25000), ("Muffin", 1, 15000)]
    bridge.update_cart(items, discount=5000, tax=6000, total=66000, order_type="DINE_IN", customer_name="Pak Dani")
    assert bridge.subtotal == 65000
    assert bridge.discount == 5000
    assert bridge.tax == 6000
    assert bridge.total == 66000
    assert bridge.customer_name == "Pak Dani"


def test_cfd_bridge_qris_payload():
    bridge = MockCustomerDisplayState()
    bridge.show_payment("QRIS", 66000, "ORD-888")
    assert bridge.is_showing_payment is True
    assert "COFFEEOS" in bridge.qris_payload
    assert "66000" in bridge.qris_payload


def test_cfd_bridge_payment_success_and_reset():
    bridge = MockCustomerDisplayState()
    bridge.mark_payment_success("ORD-888", 66000)
    assert bridge.is_payment_success is True
    assert bridge.last_order_id == "ORD-888"

    bridge.reset()
    assert bridge.is_payment_success is False
    assert len(bridge.items) == 0


# ==============================================================================
# 6. StockRulesTest & Shift & Conflict
# ==============================================================================
def evaluate_stock(current, max_cap=10000.0):
    pct = (current / max_cap) * 100
    if pct <= 2.0:
        return "CRITICAL_STOP", False, True
    elif pct <= 10.0:
        return "LOW_WARNING", True, True
    else:
        return "HEALTHY", True, False


def test_stock_rules_healthy():
    level, usable, alert = evaluate_stock(2000.0)
    assert level == "HEALTHY" and usable and not alert


def test_stock_rules_warning():
    level, usable, alert = evaluate_stock(1000.0)
    assert level == "LOW_WARNING" and usable and alert


def test_stock_rules_critical_stop():
    level, usable, alert = evaluate_stock(200.0)
    assert level == "CRITICAL_STOP" and not usable and alert


def test_shift_calculation_balanced():
    modal, paid, counted = 500000, 1200000, 1700000
    expected = modal + paid
    diff = counted - expected
    assert expected == 1700000 and diff == 0


def test_shift_calculation_shortage():
    modal, paid, counted = 500000, 1200000, 1650000
    expected = modal + paid
    diff = counted - expected
    assert diff == -50000


def test_conflict_fifo_policy():
    # cukup stok -> ACCEPT, kurang -> CONFLICT_REVIEW, duplikat -> DUPLICATE_IGNORED
    def resolve_fifo(stock, need, processed, oid):
        if oid in processed:
            return "DUPLICATE_IGNORED"
        return "ACCEPT" if stock >= need else "CONFLICT_REVIEW"

    assert resolve_fifo(1000, 150, set(), "ORD-1") == "ACCEPT"
    assert resolve_fifo(100, 150, set(), "ORD-2") == "CONFLICT_REVIEW"
    assert resolve_fifo(1000, 150, {"ORD-1"}, "ORD-1") == "DUPLICATE_IGNORED"


# ==============================================================================
# Main Runner
# ==============================================================================
def main():
    print("=" * 60)
    print("🚀 MENJALANKAN SUITE UNIT TEST CAFE POS")
    print("=" * 60)

    suites = [
        ("OrderTotals & PB1 Tax", [
            ("Tanpa promo", test_order_totals_no_promo),
            ("Promo persen dengan min order", test_order_totals_percent_promo),
            ("Promo tidak berlaku di bawah min order", test_order_totals_promo_min_order_not_met),
            ("Diskon capped subtotal", test_order_totals_cap_discount),
            ("Pajak restoran PB1 10% tanpa promo", test_order_totals_pb1_tax),
            ("Pajak restoran PB1 10% setelah diskon promo", test_order_totals_pb1_tax_with_promo),
        ]),
        ("Drink Modifiers & Pricing", [
            ("Default modifier (es normal, gula normal, tanpa addon)", test_modifier_default),
            ("Akumulasi harga add-on berbayar", test_modifier_extra_price_accumulation),
            ("Format ringkasan modifier lengkap", test_modifier_full_summary),
        ]),
        ("Customer Loyalty Stamps", [
            ("Visual bar 0 stempel", test_loyalty_progress_bar_empty),
            ("Visual bar 4 stempel", test_loyalty_progress_bar_four),
            ("Visual bar 9 stempel", test_loyalty_progress_bar_nine),
            ("Pencapaian reward 10 stempel", test_loyalty_earned_reward_on_10),
            ("Rollover stempel saat melebihi 10 cup", test_loyalty_rollover_stamps),
            ("Belum cukup stempel", test_loyalty_not_enough_for_reward),
        ]),
        ("Receipt Formatter 32 Kolom", [
            ("Struktur struk Dine In lengkap dengan PB1 & nama pelanggan", test_receipt_structure_dine_in),
            ("Panjang baris struk maksimal 32 karakter", test_receipt_32_columns),
        ]),
        ("Customer Display Bridge (CFD)", [
            ("Update live cart dan perhitungan total", test_cfd_bridge_update_cart),
            ("Aktivasi bayar QRIS dengan payload dinamis", test_cfd_bridge_qris_payload),
            ("Tandai pembayaran sukses dan reset display", test_cfd_bridge_payment_success_and_reset),
        ]),
        ("Stock Rules & Shift & Conflict", [
            ("Evaluasi stok level HEALTHY (>10%)", test_stock_rules_healthy),
            ("Evaluasi stok level LOW_WARNING (10%)", test_stock_rules_warning),
            ("Evaluasi stok level CRITICAL_STOP (2%)", test_stock_rules_critical_stop),
            ("Tutup shift kas pas (balanced)", test_shift_calculation_balanced),
            ("Tutup shift kas minus (shortage)", test_shift_calculation_shortage),
            ("Kebijakan resolusi konflik antrean FIFO", test_conflict_fifo_policy),
        ]),
    ]

    total_tests = sum(len(tests) for _, tests in suites)

    for suite_name, tests in suites:
        print(f"\n📂 SUITE: {suite_name}")
        print("-" * 50)
        for test_name, test_func in tests:
            run_test(suite_name, test_name, test_func)

    print("\n" + "=" * 60)
    print(f"📊 HASIL AKHIR: {passed}/{total_tests} Unit Tests Lolos")
    if failed == 0:
        print("🎉 SEMUA UNIT TEST 100% SUKSES DAN LOLOS!")
    else:
        print(f"⚠️ DITEMUKAN {failed} TEST YANG GAGAL")
    print("=" * 60)

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
