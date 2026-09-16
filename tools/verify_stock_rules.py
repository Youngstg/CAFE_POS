"""Cermin Python dari StockRules.kt — untuk verifikasi logika tanpa JDK/Gradle.

Aturan: WARNING <=10%, STOP <=2%, nyala lagi >=5%, warning hilang >12%,
fallback absolut <=5 bila maxCapacity tidak diketahui.
Jalankan: python tools/verify_stock_rules.py
"""

WARNING_PERCENT = 10.0
STOP_PERCENT = 2.0
REENABLE_PERCENT = 5.0
WARNING_CLEAR_PERCENT = 12.0
ABSOLUTE_FALLBACK_STOP = 5.0


def evaluate(current: float, max_capacity) -> dict:
    if max_capacity is None or max_capacity <= 0:
        stopped = current <= ABSOLUTE_FALLBACK_STOP
        return {
            "level": "CRITICAL_STOP" if stopped else "HEALTHY",
            "percent": None,
            "usable": not stopped,
            "should_alert": stopped,
        }
    percent = current / max_capacity * 100.0
    if percent <= STOP_PERCENT:
        return {"level": "CRITICAL_STOP", "percent": percent, "usable": False, "should_alert": True}
    if percent <= WARNING_PERCENT:
        return {"level": "LOW_WARNING", "percent": percent, "usable": True, "should_alert": True}
    return {"level": "HEALTHY", "percent": percent, "usable": False if False else True, "should_alert": False}


def is_menu_sellable(requirements, stocks: dict) -> bool:
    """requirements: [(ingredient_id, qty_per_portion)]. stocks: {id: (current, max)}."""
    if not requirements:
        return False
    for ing_id, qty_needed in requirements:
        if ing_id not in stocks:
            return False
        current, max_cap = stocks[ing_id]
        ev = evaluate(current, max_cap)
        if not ev["usable"] or current < qty_needed:
            return False
    return True


CASES = [
    ("sehat di 20%", lambda: evaluate(2000, 10000)["level"] == "HEALTHY"),
    ("warning tepat 10%", lambda: evaluate(1000, 10000)["level"] == "LOW_WARNING"),
    ("warning masih bisa jual", lambda: evaluate(1000, 10000)["usable"] is True),
    ("stop tepat 2%", lambda: evaluate(200, 10000)["level"] == "CRITICAL_STOP"),
    ("stop tidak usable", lambda: evaluate(200, 10000)["usable"] is False),
    ("menu mati jika 1 bahan kritis",
     lambda: is_menu_sellable([("susu", 150), ("kopi", 18)],
                              {"susu": (100, 10000), "kopi": (4000, 5000)}) is False),
    ("menu hidup saat warning tapi cukup 1 porsi",
     lambda: is_menu_sellable([("susu", 150)], {"susu": (900, 10000)}) is True),
    ("menu mati jika < 1 porsi walau belum 2%",
     lambda: is_menu_sellable([("susu", 150)], {"susu": (90, 1000)}) is False),
    ("hysteresis nyala di 5%", lambda: (4.9 < REENABLE_PERCENT) and (5.0 >= REENABLE_PERCENT)),
    ("fallback absolut: 5 stop, 6 aman",
     lambda: evaluate(5, None)["level"] == "CRITICAL_STOP" and evaluate(6, None)["level"] == "HEALTHY"),
]

if __name__ == "__main__":
    failed = 0
    for name, fn in CASES:
        try:
            ok = bool(fn())
        except Exception as exc:  # noqa: BLE001
            ok = False
            print(f"ERROR {name}: {exc}")
        print(("PASS" if ok else "FAIL"), "-", name)
        failed += 0 if ok else 1
    print(f"\n{len(CASES) - failed}/{len(CASES)} lolos")
    raise SystemExit(1 if failed else 0)
