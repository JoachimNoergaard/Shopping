package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class CatalogItem(
    val id: String,
    val profileId: String,
    val name: String,
    val category: String = "",
)
