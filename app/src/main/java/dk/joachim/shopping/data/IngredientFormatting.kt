package dk.joachim.shopping.data

import java.util.Locale

/**
 * Uppercases the first letter character in the string; leaves the rest unchanged.
 * Used for recipe ingredient names (editing, display, and persistence).
 */
fun String.capitalizeIngredientFirstLetter(): String {
    if (isEmpty()) return this
    val i = indexOfFirst { it.isLetter() }
    if (i < 0) return this
    val ch = this[i]
    if (!ch.isLowerCase()) return this
    return substring(0, i) + ch.titlecase(Locale.getDefault()) + substring(i + 1)
}

/** Ensures each ingredient name has a leading capital letter (e.g. after load from storage). */
fun Recipe.withIngredientNamesFirstLetterCapitalized(): Recipe =
    copy(
        ingredientSections = ingredientSections.map { section ->
            section.copy(
                ingredients = section.ingredients.map { ing ->
                    ing.copy(name = ing.name.capitalizeIngredientFirstLetter())
                }
            )
        }
    )
