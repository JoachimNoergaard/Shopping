package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class GroceryItem(
    val id: String,
    val name: String,
    val quantity: String,
    // Stores the UserCategory.id. May be empty or an old enum name for legacy items.
    val category: String = "",
    val isChecked: Boolean = false,
    val checkedAt: Long? = null,
    val weekday: String? = null,
    val price: String? = null,
    val supermarket: String? = null,
    val comment: String? = null,
)

val WEEKDAYS = listOf("Ma", "Ti", "On", "To", "Fr", "Lø", "Sø")
val SUPERMARKETS = listOf("Rema", "Føtex", "Netto", "Bilka", "Lidl", "Meny", "Løvbjerg", "Harald Nyborg", "Biltema", "Anden")
