package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate

object ProductCatalog {
    val units: List<String> = listOf("pcs", "pack", "g", "kg", "ml", "L", "bunch", "bottle", "jar", "can")

    // Approximate refrigerated storage guidance; package labeling should take precedence.
    // Values are intentionally conservative defaults, not food-safety guarantees.
    private val fridgeDays = mapOf(
        "greek yogurt" to 3, "plain yogurt" to 3, "yogurt" to 3, "yoghurt" to 3,
        "cherry tomatoes" to 5, "tomatoes" to 5, "tomato" to 5,
        "whole milk" to 5, "skim milk" to 5, "milk" to 5,
        "eggs" to 21, "cheese" to 7, "cheddar" to 21, "butter" to 30,
        "sour cream" to 7, "cottage cheese" to 5, "heavy cream" to 5, "cream" to 5,
        "broccoli" to 4, "cauliflower" to 4, "spinach" to 3, "lettuce" to 5, "kale" to 5,
        "carrots" to 14, "carrot" to 14, "celery" to 14, "mushrooms" to 5, "mushroom" to 5,
        "strawberries" to 3, "raspberries" to 3, "blackberries" to 3, "blueberries" to 5,
        "grapes" to 7, "apples" to 30, "apple" to 30, "oranges" to 21, "orange" to 21,
        "pears" to 7, "pear" to 7, "banana" to 7, "bananas" to 7, "avocado" to 3,
        "cucumber" to 7, "bell pepper" to 7, "zucchini" to 5, "courgette" to 5,
        "potatoes" to 21, "potato" to 21, "onions" to 30, "onion" to 30, "garlic" to 30,
        "raw chicken" to 1, "chicken breast" to 1, "chicken" to 1,
        "fresh fish" to 1, "salmon" to 1, "fish" to 1, "seafood" to 1,
        "ground beef" to 2, "beef" to 3, "pork" to 3,
        "deli meat" to 3, "leftovers" to 3, "cooked rice" to 4,
        "tofu" to 3, "hummus" to 7, "bread" to 5, "fresh herbs" to 3,
    )

    private val preparedFoodTerms = setOf("sauce", "paste", "soup", "juice", "powder", "frozen", "canned", "dried")

    fun estimatedFridgeDays(name: String): Int? {
        val normalized = normalize(name)
        if (normalized.isEmpty() || normalized.split(" ").any { it in preparedFoodTerms }) return null
        return fridgeDays[normalized] ?: fridgeDays.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (product, _) -> " $normalized ".contains(" $product ") }
            ?.value
    }

    fun suggestedExpiryDate(name: String, from: LocalDate = LocalDate.now()): LocalDate? =
        estimatedFridgeDays(name)?.let { from.plusDays(it.toLong()) }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
}
