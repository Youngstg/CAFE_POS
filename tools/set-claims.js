/**
 * Set Custom Claims user (role + tenant + outlet).
 * Pakai: GOOGLE_APPLICATION_CREDENTIALS=/path/serviceAccount.json node tools/set-claims.js <email> <ROLE> <tenantId> <outletId> [displayName]
 * Roles: OWNER | ADMIN_OUTLET | CASHIER | KITCHEN | WAREHOUSE
 * Butuh: npm i firebase-admin (di folder tools/ atau global).
 */
// eslint-disable-next-line @typescript-eslint/no-require-imports
const admin = require("firebase-admin");

async function main() {
  const [email, role, tenantId, outletId, displayName] = process.argv.slice(2);
  const roles = ["OWNER", "ADMIN_OUTLET", "CASHIER", "KITCHEN", "WAREHOUSE"];
  if (!email || !roles.includes(role) || !tenantId || !outletId) {
    console.error("Pakai: node tools/set-claims.js <email> <ROLE> <tenantId> <outletId> [displayName]");
    process.exit(1);
  }
  admin.initializeApp({ credential: admin.credential.applicationDefault() });
  const user = await admin.auth().getUserByEmail(email);
  await admin.auth().setCustomUserClaims(user.uid, {
    role,
    tenantId,
    outletId,
    ...(displayName ? { displayName } : {}),
  });
  const check = await admin.auth().getUser(user.uid);
  console.log("OK:", check.email, JSON.stringify(check.customClaims));
  // Penting: user harus login ulang / refresh token agar claims baru terbaca aplikasi.
}

main().catch((e) => { console.error(e.message); process.exit(1); });
