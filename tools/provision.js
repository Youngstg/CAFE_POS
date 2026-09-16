/**
 * Provisioning sekali-jalan: bikin 5 user demo + set Custom Claims sekaligus.
 * Idempoten — aman dijalankan ulang (user ada -> password di-reset + claims di-set).
 *
 * Pakai:
 *   npm i firebase-admin
 *   node tools/provision.js <serviceAccount.json> [password] [tenantId] [outletId]
 *
 * Contoh:
 *   node tools/provision.js ./serviceAccountKey.json "Cafe1234!" tenant-1 outlet-1
 *
 * CATATAN:
 * - serviceAccount.json JANGAN di-commit (sudah di .gitignore).
 * - Properti URL basis data realtime TIDAK dibutuhkan: kita pakai Firestore
 *   (Auth Admin SDK), bukan Realtime Database. Abaikan snippet bawaan
 *   Firebase yang menyebut RTDB.
 * - Password demo sama untuk semua akun agar gampang diingat tim;
 *   ganti password akun owner di Console setelah demo/sidang.
 */
const admin = require("firebase-admin");

const USERS = [
  { user: "owner", email: "owner@cafepos.local", role: "OWNER", name: "Owner" },
  { user: "admin", email: "admin@cafepos.local", role: "ADMIN_OUTLET", name: "Admin Cabang" },
  { user: "kasir", email: "kasir@cafepos.local", role: "CASHIER", name: "Kasir 1" },
  { user: "dapur", email: "dapur@cafepos.local", role: "KITCHEN", name: "Dapur 1" },
  { user: "gudang", email: "gudang@cafepos.local", role: "WAREHOUSE", name: "Gudang 1" },
];

async function main() {
  const [keyPath, passwordArg, tenantIdArg, outletIdArg] = process.argv.slice(2);
  if (!keyPath) {
    console.error("Pakai: node tools/provision.js <serviceAccount.json> [password] [tenantId] [outletId]");
    process.exit(1);
  }
  const password = passwordArg || "Cafe1234!";
  const tenantId = tenantIdArg || "tenant-1";
  const outletId = outletIdArg || "outlet-1";

  // eslint-disable-next-line import/no-dynamic-require, global-require
  const serviceAccount = require(require("path").resolve(keyPath));
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

  for (const u of USERS) {
    let uid;
    try {
      const existing = await admin.auth().getUserByEmail(u.email);
      uid = existing.uid;
      await admin.auth().updateUser(uid, { password, displayName: u.name });
      console.log(`~= ${u.email} sudah ada -> password di-reset`);
    } catch (e) {
      if (e.code !== "auth/user-not-found") throw e;
      const created = await admin.auth().createUser({
        email: u.email,
        password,
        displayName: u.name,
        emailVerified: false,
        disabled: false,
      });
      uid = created.uid;
      console.log(`+  ${u.email} dibuat`);
    }
    await admin.auth().setCustomUserClaims(uid, {
      role: u.role,
      tenantId,
      outletId,
      displayName: u.name,
    });
    console.log(`   claims: role=${u.role} tenant=${tenantId} outlet=${outletId}`);
  }

  console.log("\nSELESAI. Login di HP pakai tab Email, contoh:");
  console.log(`  kasir@cafepos.local / ${password}`);
  console.log("User HARUS login ulang di HP setelah claims diubah.");
}

main().catch((e) => {
  console.error("GAGAL:", e.message);
  process.exit(1);
});
