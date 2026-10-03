package com.coffeeos.erp.core.domain.menu

/**
 * Pilihan modifikasi minuman cafe (DESIGN.md §7.2):
 * - Type: Iced | Hot
 * - Size: Regular | Large
 * - Sugar Level: Normal | Less | No Sugar
 * - Milk: Fresh Milk | Oat Milk | Soy Milk | Almond Milk | No Milk
 * - Add-ons: Extra Shot, Whipped Cream, Caramel Drizzle, Sirup, dll
 */
enum class IceLevel(val label: String) {
    NORMAL("Iced"),
    LESS("Less Ice"),
    NO_ICE("No Ice"),
    HOT("Hot")
}

enum class SugarLevel(val label: String) {
    NORMAL("Normal Sugar"),
    LESS("Less Sugar"),
    NO_SUGAR("No Sugar")
}

enum class DrinkSize(val label: String, val extraPrice: Long = 0L) {
    REGULAR("Regular", 0L),
    LARGE("Large (+Rp 4.000)", 4_000L)
}

enum class MilkOption(val label: String, val extraPrice: Long = 0L) {
    FRESH_MILK("Fresh Milk", 0L),
    OAT_MILK("Oat Milk (+Rp 6.000)", 6_000L),
    SOY_MILK("Soy Milk (+Rp 5.000)", 5_000L),
    ALMOND_MILK("Almond Milk (+Rp 6.000)", 6_000L),
    NO_MILK("No Milk", 0L)
}

data class AddOn(
    val id: String,
    val name: String,
    val extraPrice: Long
)

val DEFAULT_ADD_ONS = listOf(
    AddOn("extra_shot", "Extra Shot", 5_000L),
    AddOn("whipped_cream", "Whipped Cream", 4_000L),
    AddOn("caramel_drizzle", "Caramel Drizzle", 4_000L),
    AddOn("hazelnut_syrup", "Hazelnut Syrup", 4_000L),
    AddOn("vanilla_syrup", "Vanilla Syrup", 4_000L),
    AddOn("grass_jelly", "Grass Jelly", 3_000L),
)

data class SelectedModifiers(
    val ice: IceLevel = IceLevel.NORMAL,
    val sugar: SugarLevel = SugarLevel.NORMAL,
    val size: DrinkSize = DrinkSize.REGULAR,
    val milk: MilkOption = MilkOption.FRESH_MILK,
    val addOns: List<AddOn> = emptyList(),
    val notes: String = ""
) {
    val totalExtraPrice: Long get() = size.extraPrice + milk.extraPrice + addOns.sumOf { it.extraPrice }

    fun toSummary(): String = buildString {
        append(if (ice == IceLevel.HOT) "Hot" else "Iced")
        append(" · ")
        append(size.label.substringBefore(" ("))
        append(" · ")
        append(sugar.label.substringBefore(" Sugar"))
        if (milk != MilkOption.NO_MILK) {
            append(" · ")
            append(milk.label.substringBefore(" ("))
        }
        if (addOns.isNotEmpty()) {
            append(" · +")
            append(addOns.joinToString(", ") { it.name })
        }
        if (notes.isNotBlank()) {
            append(" (")
            append(notes)
            append(")")
        }
    }
}
