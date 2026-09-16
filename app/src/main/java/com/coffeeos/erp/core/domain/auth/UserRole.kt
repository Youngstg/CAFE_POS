package com.coffeeos.erp.core.domain.auth

/** RBAC — dipertahankan dari konsep lama (Akun_Role_POS.md). */
enum class UserRole {
    OWNER,
    ADMIN_OUTLET,
    CASHIER,
    KITCHEN,
    WAREHOUSE,
}
