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
    val createdAt: Long = System.currentTimeMillis(),
)
