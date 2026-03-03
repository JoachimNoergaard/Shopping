package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class GroceryList(
    val id: String,
    val name: String,
    val ownerId: String = "",
    val items: List<GroceryItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
