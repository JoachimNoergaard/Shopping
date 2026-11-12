package dk.joachim.shopping.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

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
    // Comma-separated shop IDs; a legacy single shop ID uses the same format.
    val supermarket: String? = null,
    val comment: String? = null,
)

/** Shop IDs are UUIDs, so commas safely separate multiple selections in the existing API field. */
fun String?.toShopIds(): List<String> =
    this?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct().orEmpty()

fun List<String>.toShopSelection(): String? =
    filter { it.isNotBlank() }.distinct().joinToString(",").ifEmpty { null }

val WEEKDAYS = listOf("Ma", "Ti", "On", "To", "Fr", "Lø", "Sø")
val SUPERMARKETS = listOf("Rema", "Føtex", "Netto", "Bilka", "Lidl", "Meny", "Løvbjerg", "Harald Nyborg", "Biltema", "Anden")

/** Converts a weekday abbreviation (e.g. "Lø") to the next date with that weekday (ISO 8601, e.g. "2026-03-07"). */
fun weekdayNameToNextDate(dayName: String): String {
    val index = WEEKDAYS.indexOf(dayName)
    if (index == -1) return dayName
    val targetDow = DayOfWeek.of(index + 1) // ISO: 1=Mon..7=Sun
    val today = LocalDate.now()
    val daysUntil = ((targetDow.value - today.dayOfWeek.value) + 7) % 7
    val days = if (daysUntil == 0) 7L else daysUntil.toLong()
    return today.plusDays(days).toString()
}

/** Converts a stored date (e.g. "2026-03-07") back to a weekday abbreviation (e.g. "Lø") for display.
 *  Falls back gracefully for legacy values that are already weekday abbreviations. */
fun dateToWeekdayAbbr(value: String): String {
    return try {
        val dow = LocalDate.parse(value).dayOfWeek.value // 1=Mon..7=Sun
        WEEKDAYS.getOrElse(dow - 1) { value }
    } catch (e: Exception) {
        value
    }
}

/** Returns true if the stored weekday value is a future date (strictly after today). */
fun isFutureWeekday(dateStr: String): Boolean = try {
    LocalDate.parse(dateStr).isAfter(LocalDate.now())
} catch (e: Exception) {
    false
}
