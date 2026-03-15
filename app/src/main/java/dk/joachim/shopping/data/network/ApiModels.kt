package dk.joachim.shopping.data.network

import dk.joachim.shopping.data.CompletedStep
import dk.joachim.shopping.data.GroceryItem
import dk.joachim.shopping.data.GroceryList
import dk.joachim.shopping.data.IngredientSection
import dk.joachim.shopping.data.InstructionSection
import dk.joachim.shopping.data.MenuPlan
import dk.joachim.shopping.data.Recipe
import kotlinx.serialization.Serializable

/** Full list with items — returned by GET /lists and GET /lists/{id}. */
@Serializable
data class ApiList(
    val id: String,
    val name: String,
    val ownerId: String = "",
    val createdAt: Long,
    val items: List<GroceryItem> = emptyList(),
) {
    fun toGroceryList() = GroceryList(id = id, name = name, ownerId = ownerId, items = items, createdAt = createdAt)
}

@Serializable
data class CreateListRequest(val id: String, val name: String, val ownerId: String, val createdAt: Long)

@Serializable
data class PatchListRequest(val name: String)

@Serializable
data class AddMemberRequest(val profileId: String)

// ── Menu plans ────────────────────────────────────────────────────────────────

@Serializable
data class ApiMenuPlan(
    val id: String,
    val profileId: String,
    val name: String,
    val description: String = "",
    val servings: Int = 0,
    val recipeIds: List<String> = emptyList(),
    val recipeProgress: Map<String, List<CompletedStep>> = emptyMap(),
    val createdAt: Long,
) {
    fun toMenuPlan() = MenuPlan(
        id = id,
        profileId = profileId,
        name = name,
        description = description,
        servings = servings,
        recipeIds = recipeIds,
        recipeProgress = recipeProgress,
        createdAt = createdAt,
    )
}

@Serializable
data class UpsertMenuPlanRequest(
    val id: String,
    val profileId: String,
    val name: String,
    val description: String,
    val servings: Int = 0,
    val recipeIds: List<String>,
    val recipeProgress: Map<String, List<CompletedStep>> = emptyMap(),
    val createdAt: Long,
)

// ── Recipes ───────────────────────────────────────────────────────────────────

@Serializable
data class ApiRecipe(
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
    val createdAt: Long,
) {
    fun toRecipe() = Recipe(
        id = id,
        profileId = profileId,
        name = name,
        description = description,
        rating = rating,
        servings = servings,
        nutritionFacts = nutritionFacts,
        prepTimeMinutes = prepTimeMinutes,
        totalTimeMinutes = totalTimeMinutes,
        durability = durability,
        courseType = courseType,
        ingredientSections = ingredientSections,
        instructionSections = instructionSections,
        tips = tips,
        createdAt = createdAt,
    )
}

@Serializable
data class UpsertRecipeRequest(
    val id: String,
    val profileId: String,
    val name: String,
    val description: String,
    val rating: Int,
    val servings: Int,
    val nutritionFacts: String,
    val prepTimeMinutes: Int,
    val totalTimeMinutes: Int,
    val durability: String,
    val courseType: String,
    val ingredientSections: List<IngredientSection>,
    val instructionSections: List<InstructionSection>,
    val tips: String,
    val createdAt: Long,
)
