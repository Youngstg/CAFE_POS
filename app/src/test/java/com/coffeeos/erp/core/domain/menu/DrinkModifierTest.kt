package com.coffeeos.erp.core.domain.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkModifierTest {

    @Test
    fun `default modifier memiliki ice dan sugar normal serta tanpa addon`() {
        val mods = SelectedModifiers()
        assertEquals(IceLevel.NORMAL, mods.ice)
        assertEquals(SugarLevel.NORMAL, mods.sugar)
        assertTrue(mods.addOns.isEmpty())
        assertEquals(0L, mods.totalExtraPrice)
        assertEquals("", mods.notes)
    }

    @Test
    fun `total extra price menghitung akumulasi seluruh addon berbayar`() {
        val addOns = listOf(
            AddOn("extra_shot", "Extra Espresso Shot", 5_000L),
            AddOn("oatmilk", "Ganti Oat Milk", 6_000L),
            AddOn("caramel_syrup", "Sirup Karamel", 4_000L)
        )
        val mods = SelectedModifiers(addOns = addOns)
        assertEquals(15_000L, mods.totalExtraPrice)
    }

    @Test
    fun `summary tanpa addon hanya menampilkan es dan gula`() {
        val mods = SelectedModifiers(
            ice = IceLevel.LESS,
            sugar = SugarLevel.LESS
        )
        // Format Design.md: "Iced · Regular · Less · Fresh Milk"
        val summary = mods.toSummary()
        assertTrue(summary.contains("Iced"))
        assertTrue(summary.contains("Regular"))
        assertTrue(summary.contains("Less"))
        assertTrue(summary.contains("Fresh Milk"))
    }

    @Test
    fun `summary dengan addon dan catatan menyertakan semua detail`() {
        val mods = SelectedModifiers(
            ice = IceLevel.HOT,
            sugar = SugarLevel.NO_SUGAR,
            size = DrinkSize.LARGE,
            milk = MilkOption.OAT_MILK,
            addOns = listOf(
                AddOn("extra_shot", "Extra Shot", 5_000L),
                AddOn("caramel_drizzle", "Caramel Drizzle", 4_000L)
            ),
            notes = "jangan terlalu panas"
        )
        val summary = mods.toSummary()
        assertTrue(summary.contains("Hot"))
        assertTrue(summary.contains("Large"))
        assertTrue(summary.contains("No"))
        assertTrue(summary.contains("Oat Milk"))
        assertTrue(summary.contains("+Extra Shot, Caramel Drizzle"))
        assertTrue(summary.contains("(jangan terlalu panas)"))
    }

    @Test
    fun `default add-on list memiliki 6 pilihan populer cafe`() {
        assertEquals(6, DEFAULT_ADD_ONS.size)
        assertTrue(DEFAULT_ADD_ONS.any { it.name == "Extra Shot" && it.extraPrice == 5_000L })
        assertTrue(DEFAULT_ADD_ONS.any { it.name == "Whipped Cream" && it.extraPrice == 4_000L })
        assertTrue(DEFAULT_ADD_ONS.any { it.name == "Caramel Drizzle" && it.extraPrice == 4_000L })
    }
}
