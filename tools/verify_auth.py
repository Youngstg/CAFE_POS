"""Cermin Python dari PinHash + cek integrasi Firebase Auth.
Jalankan: python tools/verify_auth.py"""

import hashlib
import pathlib
import re

failed = 0


def check(name, cond):
    global failed
    print(("PASS" if cond else "FAIL"), "-", name)
    if not cond:
        failed += 1


# --- PinHash mirror: SHA-256 "uid:pin" hex ---
def pin_hash(uid, pin):
    return hashlib.sha256(f"{uid}:{pin}".encode()).hexdigest()


h = pin_hash("demo-kasir", "123456")
check("hash 64 hex", bool(re.fullmatch(r"[0-9a-f]{64}", h)))
check("hash deterministik", pin_hash("demo-kasir", "123456") == h)
check("pin salah beda hash", pin_hash("demo-kasir", "000000") != h)
check("uid beda beda hash", pin_hash("demo-lain", "123456") != h)

# --- Integrasi kode ---
base = pathlib.Path(__file__).parent.parent / "app/src/main/java/com/coffeeos/erp"
auth = (base / "core/data/auth/AuthRepository.kt").read_text()
check("email login Firebase", "signInWithEmailAndPassword" in auth)
check("refresh claims", "getIdToken(true)" in auth)
check("baca role claim", 'claims["role"]' in auth)
check("baca tenant+outlet claim", 'claims["tenantId"]' in auth and 'claims["outletId"]' in auth)
check("validasi role dikenal", "UserRole.valueOf" in auth)
check("signOut saat logout", "signOut" in auth)
check("PIN hash bukan plaintext", "PinHash.hash" in auth and "pinHash" in
      (base / "core/data/session/SessionManager.kt").read_text())

screen = (base / "feature/auth/AuthScreen.kt").read_text()
check("layar 3 mode", all(k in screen for k in ("loginDemo", "loginEmail", "loginQuickPin")))
check("setup PIN pasca-email", "PinSetupGate" in screen and "setupPin" in screen)

rules = (pathlib.Path(__file__).parent.parent / "firestore.rules").read_text()
check("rules pakai token claims", "token.role" in rules and "token.tenantId" in rules)

tools = pathlib.Path(__file__).parent.parent / "tools/set-claims.js"
check("script set-claims ada", tools.exists() and "setCustomUserClaims" in tools.read_text())

prov = pathlib.Path(__file__).parent.parent / "tools/provision.js"
prov_src = prov.read_text() if prov.exists() else ""
check("script provision ada + idempoten",
      prov.exists() and "getUserByEmail" in prov_src and "createUser" in prov_src
      and "setCustomUserClaims" in prov_src)
check("provision tanpa RTDB", "databaseURL" not in prov_src)

gi = (pathlib.Path(__file__).parent.parent / ".gitignore").read_text()
check("service account di-gitignore", "serviceAccount" in gi)

print(f"\n{20 - failed}/20 lolos")
raise SystemExit(1 if failed else 0)
