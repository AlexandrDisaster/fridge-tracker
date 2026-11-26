package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate

object ProductCatalog {
    val units: List<String> = listOf("pcs", "pack", "g", "kg", "ml", "L", "bunch", "bottle", "jar", "can")

    // Approximate refrigerated storage guidance; package labeling should take precedence.
    private val fridgeDays = mapOf(
        "yogurt" to 3, "yoghurt" to 3,
        "tomatoes" to 5, "tomato" to 5,
        "milk" to 5, "eggs" to 21,
        "broccoli" to 4, "spinach" to 3, "lettuce" to 5,
        "carrots" to 14, "mushrooms" to 5,
        "strawberries" to 3, "blueberries" to 5,
        "raw chicken" to 1, "fresh fish" to 1,
    )

    fun estimatedFridgeDays(name: String): Int? = fridgeDays[name.trim().lowercase()]

    fun suggestedExpiryDate(name: String, from: LocalDate = LocalDate.now()): LocalDate? =
        estimatedFridgeDays(name)?.let { from.plusDays(it.toLong()) }
}
