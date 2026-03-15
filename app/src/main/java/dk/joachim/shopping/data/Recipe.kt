package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
)

@Serializable
data class IngredientSection(
    val title: String = "",
    val ingredients: List<Ingredient> = emptyList(),
)

@Serializable
data class InstructionSection(
    val title: String = "",
    val steps: List<String> = emptyList(),
)

@Serializable
data class Recipe(
    val id: String,
    val profileId: String,
    val name: String,
    val description: String = "",
    val rating: Int = 0,
    val servings: Int = 0,
    val nutritionFacts: String = "",
    val prepTimeMinutes: Int = 0,
    val totalTimeMinutes: Int = 0,
    val durability: String = "",
    val courseType: String = "",
    val ingredientSections: List<IngredientSection> = emptyList(),
    val instructionSections: List<InstructionSection> = emptyList(),
    val tips: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
