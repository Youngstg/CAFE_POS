package com.coffeeos.erp.ui.components

import androidx.annotation.DrawableRes
import com.coffeeos.erp.R

/**
 * Resolver cerdas untuk memetakan nama & ID menu ke aset fotografi produk kafe SuKopi.
 * Menghubungkan aset WebP/JPG berkualitas tinggi secara offline-first.
 */
object MenuImageResolver {

    @DrawableRes
    fun getDrawableForMenu(menuName: String, menuId: String = ""): Int? {
        val name = menuName.lowercase()
        val id = menuId.lowercase()

        return when {
            // Es Kopi Susu Aren (Signature SuKopi)
            id == "m-kopsus" || name.contains("kopi susu") || name.contains("kopsus") || name.contains("aren") -> {
                R.drawable.menu_kopsus
            }
            // Caffe Latte / Cappuccino Art
            id == "m-latte" || name.contains("latte") || name.contains("cappuccino") || name.contains("flat white") -> {
                R.drawable.menu_latte
            }
            // Butter Croissant
            id == "m-croissant" || name.contains("croissant") || name.contains("pastry") || name.contains("danish") -> {
                R.drawable.menu_croissant
            }
            // Matcha Latte
            id == "m-matcha" || name.contains("matcha") || name.contains("green tea") -> {
                R.drawable.menu_matcha
            }
            // Toast / Roti Bakar Kaya Butter
            id == "m-toast" || name.contains("toast") || name.contains("roti") || name.contains("srikaya") -> {
                R.drawable.menu_toast
            }
            // Americano / Long Black / Espresso
            id == "m-americano" || name.contains("americano") || name.contains("black") || name.contains("espresso") -> {
                R.drawable.menu_americano
            }
            else -> null
        }
    }
}
