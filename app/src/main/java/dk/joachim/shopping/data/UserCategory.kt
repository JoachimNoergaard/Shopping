package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class UserCategory(
    val id: String,
    val profileId: String,
    val name: String,
    val orderIndex: Int,
)

@Serializable
data class ReorderRequest(val ids: List<String>)
