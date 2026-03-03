package dk.joachim.shopping.data

import kotlinx.serialization.Serializable

@Serializable
data class Shop(
    val id: String,
    val profileId: String,
    val name: String,
    val backgroundColor: String = "#42A5F5",
    val foregroundColor: String = "#FFFFFF",
    val orderIndex: Int = 0,
)

val SHOP_BACKGROUND_COLORS = listOf(
    "#EF5350", "#FF7043", "#FFCA28", "#FDD835",
    "#66BB6A", "#26A69A", "#29B6F6", "#42A5F5",
    "#5C6BC0", "#AB47BC", "#EC407A", "#8D6E63",
    "#78909C", "#546E7A", "#424242", "#F5F5F5",
)

val SHOP_FOREGROUND_COLORS = listOf("#FFFFFF", "#1A1A1A")
