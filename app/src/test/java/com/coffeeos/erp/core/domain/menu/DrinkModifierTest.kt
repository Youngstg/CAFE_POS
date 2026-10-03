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
        assertEquals("Less Ice, Less Sugar (50%)", mods.toSummary())
    }

    @Test
    fun `summary dengan addon dan catatan menyertakan semua detail`() {
        val mods = SelectedModifiers(
            ice = IceLevel.NO_ICE,
            sugar = SugarLevel.NO_SUGAR,
            addOns = listOf(
                AddOn("extra_shot", "Extra Espresso Shot", 5_000L),
                AddOn("oatmilk", "Ganti Oat Milk", 6_000L)
            ),
            notes = "jangan terlalu panas"
        )
        val summary = mods.toSummary()
        assertTrue(summary.contains("No Ice"))
        assertTrue(summary.contains("No Sugar (0%)"))
        assertTrue(summary.contains("+Extra Espresso Shot, Ganti Oat Milk"))
        assertTrue(summary.contains("(jangan terlalu panas)"))
    }

    @Test
    fun `default add-on list memiliki 5 pilihan populer cafe`() {
        assertEquals(5, DEFAULT_ADD_ONS.size)
        assertTrue(DEFAULT_ADD_ONS.any { it.name == "Extra Espresso Shot" && it.extraPrice == 5_000L })
        assertTrue(DEFAULT_ADD_ONS.any { it.name == "Ganti Oat Milk" && it.extraPrice == 6_000L })
    }
}
