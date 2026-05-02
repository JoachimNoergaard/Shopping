package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class CompletedStep(
    val sectionIndex: Int,
    val stepIndex: Int,
)

@Serializable
data class MenuPlan(
    val id: String,
    val profileId: String,
    val name: String,
    val description: String = "",
    val servings: Int = 4,
    val recipeIds: List<String> = emptyList(),
    val recipeProgress: Map<String, List<CompletedStep>> = emptyMap(),
    /** Per-recipe servings overrides. If a recipe id is absent the plan-wide [servings] is used. */
    val recipeServings: Map<String, Int> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
)
