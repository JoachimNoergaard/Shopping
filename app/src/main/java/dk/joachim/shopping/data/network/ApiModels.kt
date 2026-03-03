package dk.joachim.shopping.data.network

import dk.joachim.shopping.data.GroceryItem
import dk.joachim.shopping.data.GroceryList
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
