package dk.joachim.shopping.data

import android.content.Context
import android.content.SharedPreferences
import dk.joachim.shopping.data.network.RemoteDataSource

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

private const val PREFS_NAME = "shopping_prefs"
private const val KEY_LISTS = "lists"
const val KEY_LAST_LIST_ID = "last_list_id"
private const val KEY_PROFILE_ID = "profile_id"
private const val KEY_ONBOARDING_DONE = "onboarding_done"
private const val KEY_CATEGORIES = "categories"
private const val KEY_PENDING_MUTATIONS = "pending_mutations"
// How long (ms) to suppress server sync after a local write, giving the server
// time to process the push before we pull again.
private const val SYNC_COOLDOWN_MS = 10_000L

@Serializable
private data class PendingMutation(
    val type: String,
    val listId: String,
    val itemId: String,
    val item: GroceryItem? = null,
) {
    companion object {
        const val UPSERT = "upsert"
        const val DELETE = "delete"
    }
}

object ShoppingRepository {

    // Background scope for fire-and-forget remote pushes.
    // SupervisorJob ensures one failing coroutine doesn't cancel the others.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var prefs: SharedPreferences

    // Tracks the last time a local mutation was made so syncList can hold off
    // long enough for the server to process the write before pulling again.
    private val lastMutationAt = AtomicLong(0L)
    private fun recordMutation() = lastMutationAt.set(System.currentTimeMillis())

    private val mutationQueueLock = Any()

    private val _lists = MutableStateFlow<List<GroceryList>>(emptyList())
    val lists: StateFlow<List<GroceryList>> = _lists.asStateFlow()

    private val _catalogItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val catalogItems: StateFlow<List<CatalogItem>> = _catalogItems.asStateFlow()

    private val _shops = MutableStateFlow<List<Shop>>(emptyList())
    val shops: StateFlow<List<Shop>> = _shops.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _lists.value = loadLists()
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private fun loadLists(): List<GroceryList> {
        val raw = prefs.getString(KEY_LISTS, null) ?: return sampleLists()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            sampleLists()
        }
    }

    private fun saveLists(lists: List<GroceryList>) {
        prefs.edit().putString(KEY_LISTS, json.encodeToString(lists)).apply()
    }

    // ── Offline mutation queue ──────────────────────────────────────────────

    private fun loadPendingMutations(): List<PendingMutation> {
        val raw = prefs.getString(KEY_PENDING_MUTATIONS, null) ?: return emptyList()
        return try { json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
    }

    private fun savePendingMutations(mutations: List<PendingMutation>) {
        prefs.edit().putString(KEY_PENDING_MUTATIONS, json.encodeToString(mutations)).apply()
    }

    private fun queueMutation(mutation: PendingMutation) {
        synchronized(mutationQueueLock) {
            val current = loadPendingMutations().toMutableList()
            current.removeAll { it.listId == mutation.listId && it.itemId == mutation.itemId }
            current.add(mutation)
            savePendingMutations(current)
        }
    }

    private fun removePendingMutation(listId: String, itemId: String) {
        synchronized(mutationQueueLock) {
            val current = loadPendingMutations()
            val filtered = current.filter { !(it.listId == listId && it.itemId == itemId) }
            if (filtered.size != current.size) savePendingMutations(filtered)
        }
    }

    /**
     * Pushes an item upsert to the server; queues the mutation for retry if the
     * push fails (e.g. device is offline).
     */
    private suspend fun pushItemOrQueue(listId: String, itemId: String) {
        val item = itemIn(listId, itemId) ?: return
        if (RemoteDataSource.upsertItem(listId, item)) {
            removePendingMutation(listId, itemId)
        } else {
            queueMutation(PendingMutation(PendingMutation.UPSERT, listId, itemId, item))
        }
    }

    private suspend fun pushDeleteOrQueue(listId: String, itemId: String) {
        if (RemoteDataSource.deleteItem(listId, itemId)) {
            removePendingMutation(listId, itemId)
        } else {
            queueMutation(PendingMutation(PendingMutation.DELETE, listId, itemId))
        }
    }

    /**
     * Attempts to push all queued offline mutations to the server.
     * Returns true when the queue is empty (all flushed or nothing pending).
     */
    private suspend fun flushPendingMutations(): Boolean {
        val pending = synchronized(mutationQueueLock) { loadPendingMutations() }
        if (pending.isEmpty()) return true

        val remaining = mutableListOf<PendingMutation>()
        for (mutation in pending) {
            val ok = when (mutation.type) {
                PendingMutation.UPSERT ->
                    mutation.item?.let { RemoteDataSource.upsertItem(mutation.listId, it) } ?: true
                PendingMutation.DELETE ->
                    RemoteDataSource.deleteItem(mutation.listId, mutation.itemId)
                else -> true
            }
            if (!ok) remaining.add(mutation)
        }

        synchronized(mutationQueueLock) { savePendingMutations(remaining) }
        return remaining.isEmpty()
    }

    fun getLastListId(): String? = prefs.getString(KEY_LAST_LIST_ID, null)

    fun saveLastListId(id: String) {
        prefs.edit().putString(KEY_LAST_LIST_ID, id).apply()
    }

    fun getOrCreateProfileId(): String {
        val existing = prefs.getString(KEY_PROFILE_ID, null)
        if (existing != null) return existing
        val new = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_PROFILE_ID, new).apply()
        return new
    }

    // ── Onboarding ─────────────────────────────────────────────────────────

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    private fun markOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    private fun adoptProfileId(newId: String) {
        prefs.edit().putString(KEY_PROFILE_ID, newId).apply()
    }

    /**
     * Returns the profile for [email] if it exists on the server, null if not found.
     * Throws for network / server errors so callers can handle them explicitly.
     */
    suspend fun getProfileByEmail(email: String): Profile? =
        RemoteDataSource.findProfileByEmail(email.trim())

    /**
     * Called when the user submits the onboarding form.
     * If the e-mail is already registered on the server, the server's profileId is reused
     * so the user gets access to their existing data. Otherwise the locally generated UUID
     * is kept and a new profile is created with a freshly generated activation code.
     */
    suspend fun completeOnboarding(name: String, email: String) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()

        val existing = RemoteDataSource.findProfileByEmail(trimmedEmail)
        val profileId = if (existing != null) {
            adoptProfileId(existing.id)
            existing.id
        } else {
            getOrCreateProfileId()
        }

        // Preserve existing activation code; generate one only for brand-new profiles.
        val activationCode = existing?.activationCode?.ifBlank { null } ?: generateActivationCode()
        RemoteDataSource.upsertProfile(
            Profile(id = profileId, name = trimmedName, email = trimmedEmail, activationCode = activationCode)
        )
        markOnboardingDone()
    }

    // ── Profile ────────────────────────────────────────────────────────────

    suspend fun loadProfile(): Profile {
        val id = getOrCreateProfileId()
        val profile = RemoteDataSource.getProfile(id) ?: Profile(id = id)
        // Backfill: if the server profile has no activation code yet, generate and persist one.
        return if (profile.activationCode.isBlank()) {
            val withCode = profile.copy(activationCode = generateActivationCode())
            RemoteDataSource.upsertProfile(withCode) ?: withCode
        } else {
            profile
        }
    }

    suspend fun saveProfile(name: String, email: String, activationCode: String): Profile? {
        val id = getOrCreateProfileId()
        return RemoteDataSource.upsertProfile(
            Profile(id = id, name = name, email = email, activationCode = activationCode)
        )
    }

    private fun generateActivationCode(): String = (100000..999999).random().toString()

    // ── Categories ─────────────────────────────────────────────────────────

    suspend fun loadCategories(): List<UserCategory> {
        val profileId = getOrCreateProfileId()
        val remote = RemoteDataSource.getCategories(profileId)
        if (remote == null) return loadCachedCategories()
        val result = if (remote.isEmpty()) createDefaultCategories(profileId) else remote
        saveCategories(result)
        return result
    }

    private fun loadCachedCategories(): List<UserCategory> {
        val raw = prefs.getString(KEY_CATEGORIES, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCategories(categories: List<UserCategory>) {
        prefs.edit().putString(KEY_CATEGORIES, json.encodeToString(categories)).apply()
    }

    private suspend fun createDefaultCategories(profileId: String): List<UserCategory> {
        val names = listOf(
            "🥨 Bagværk",
            "🥦 Frugt og grønt",
            "🧀 Kølevarer",
            "🥩 Kød og fisk",
            "🥛 Mejeri",
            "❄️ Frost",
            "🥫 Kolonial",
            "🍾 Drikkevarer",
            "🧽 Hygiejne og rengøring",
            "🍭 Chips og slik",
            "🪚 Byggemarked",
            "😊 Andet",
        )
        return names.mapIndexedNotNull { index, name ->
            val category = UserCategory(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                name = name,
                orderIndex = index,
            )
            RemoteDataSource.createCategory(profileId, category)
        }
    }

    suspend fun createCategory(name: String): UserCategory? {
        val profileId = getOrCreateProfileId()
        val category = UserCategory(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name,
            orderIndex = 0,
        )
        return RemoteDataSource.createCategory(profileId, category)
    }

    suspend fun updateCategory(category: UserCategory): UserCategory? =
        RemoteDataSource.updateCategory(category)

    suspend fun deleteCategory(id: String) {
        val profileId = getOrCreateProfileId()
        RemoteDataSource.deleteCategory(profileId, id)
    }

    suspend fun reorderCategories(orderedIds: List<String>) {
        val profileId = getOrCreateProfileId()
        RemoteDataSource.reorderCategories(profileId, orderedIds)
    }

    // ── Catalog ────────────────────────────────────────────────────────────

    suspend fun loadCatalogItems() {
        val profileId = getOrCreateProfileId()
        val remote = RemoteDataSource.getCatalogItems(profileId) ?: return
        _catalogItems.value = remote
    }

    /**
     * Saves an item to the cross-list catalog. If an item with the same name already
     * exists, its category is updated. Otherwise a new catalog entry is created.
     * Called automatically whenever an item is added to any shopping list.
     */
    private fun saveToCatalog(name: String, category: String) {
        val profileId = getOrCreateProfileId()
        scope.launch {
            val existing = _catalogItems.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
            val item = existing?.copy(category = category)
                ?: CatalogItem(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    name = name,
                    category = category,
                )
            val saved = RemoteDataSource.upsertCatalogItem(item) ?: return@launch
            _catalogItems.update { items ->
                if (items.any { it.id == saved.id }) items.map { if (it.id == saved.id) saved else it }
                else items + saved
            }
        }
    }

    suspend fun createCatalogItem(name: String, category: String) {
        val profileId = getOrCreateProfileId()
        val item = CatalogItem(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name.trim(),
            category = category,
        )
        _catalogItems.update { it + item }
        RemoteDataSource.upsertCatalogItem(item)
    }

    suspend fun updateCatalogItem(id: String, name: String, category: String) {
        val existing = _catalogItems.value.firstOrNull { it.id == id } ?: return
        val updated = existing.copy(name = name.trim(), category = category)
        _catalogItems.update { items -> items.map { if (it.id == id) updated else it } }
        RemoteDataSource.upsertCatalogItem(updated)
    }

    suspend fun deleteCatalogItem(id: String) {
        val profileId = getOrCreateProfileId()
        _catalogItems.update { it.filter { item -> item.id != id } }
        RemoteDataSource.deleteCatalogItem(profileId, id)
    }

    // ── Shops ──────────────────────────────────────────────────────────────

    suspend fun loadShops() {
        val profileId = getOrCreateProfileId()
        val remote = RemoteDataSource.getShops(profileId) ?: return
        _shops.value = remote
    }

    suspend fun createShop(name: String, backgroundColor: String, foregroundColor: String): Shop? {
        val profileId = getOrCreateProfileId()
        val shop = Shop(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            name = name.trim(),
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            orderIndex = _shops.value.size,
        )
        val saved = RemoteDataSource.createShop(profileId, shop) ?: return null
        _shops.update { it + saved }
        return saved
    }

    suspend fun updateShop(shop: Shop): Shop? {
        val saved = RemoteDataSource.updateShop(shop) ?: return null
        _shops.update { items -> items.map { if (it.id == saved.id) saved else it } }
        return saved
    }

    fun deleteShop(id: String) {
        _shops.update { it.filter { s -> s.id != id } }
        val profileId = getOrCreateProfileId()
        scope.launch { RemoteDataSource.deleteShop(profileId, id) }
    }

    suspend fun reorderShops(orderedIds: List<String>) {
        val profileId = getOrCreateProfileId()
        RemoteDataSource.reorderShops(profileId, orderedIds)
    }

    // ── Remote sync ────────────────────────────────────────────────────────

    /**
     * Called once when the lists overview opens.
     * If the server has data, it becomes the authoritative state.
     * If the server is empty and we have local data, push everything up so
     * the server is seeded for other users.
     */
    suspend fun syncAllLists() {
        flushPendingMutations()
        val profileId = getOrCreateProfileId()
        val remote = RemoteDataSource.getAllLists(profileId) ?: return   // network error — keep local

        // Only lists explicitly owned by this user are candidates for seeding back to the server.
        // Sample data (ownerId = "") and lists owned by others must never be seeded under this profile.
        val ownedLocally = _lists.value.filter { it.ownerId == profileId }

        if (remote.isEmpty() && ownedLocally.isNotEmpty()) {
            // Returning user who reinstalled — push their real lists back up to the server.
            ownedLocally.forEach { list ->
                RemoteDataSource.createList(list)
                list.items.forEach { item -> RemoteDataSource.upsertItem(list.id, item) }
            }
        } else {
            // Use the server as the source of truth (empty or not).
            // This also clears sample/stale data for brand-new users.
            _lists.value = remote.map { it.toGroceryList() }
            saveLists(_lists.value)
        }
    }

    /**
     * Called every 5 seconds while a list screen is open.
     * Replaces local items with whatever the server has — this is how changes
     * from other users appear.
     * Skipped while a recent local write is still propagating to the server.
     * Pending offline mutations are flushed before pulling; if some remain
     * (still offline), the pull is skipped to avoid overwriting local changes.
     */
    suspend fun syncList(listId: String) {
        if (System.currentTimeMillis() - lastMutationAt.get() < SYNC_COOLDOWN_MS) return
        if (!flushPendingMutations()) return
        val remote = RemoteDataSource.getList(listId) ?: return
        val updated = remote.toGroceryList()
        _lists.update { lists -> lists.map { if (it.id == listId) updated else it } }
        saveLists(_lists.value)
    }

    // ── Local-only helper ──────────────────────────────────────────────────

    private fun updateList(listId: String, transform: (GroceryList) -> GroceryList) {
        _lists.update { lists -> lists.map { if (it.id == listId) transform(it) else it } }
        saveLists(_lists.value)
    }

    private fun itemIn(listId: String, itemId: String): GroceryItem? =
        _lists.value.find { it.id == listId }?.items?.find { it.id == itemId }

    // ── List CRUD ──────────────────────────────────────────────────────────

    fun addList(name: String) {
        val list = GroceryList(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            ownerId = getOrCreateProfileId(),
        )
        _lists.update { it + list }
        saveLists(_lists.value)
        scope.launch { RemoteDataSource.createList(list) }
    }

    fun deleteList(id: String) {
        _lists.update { it.filter { l -> l.id != id } }
        saveLists(_lists.value)
        scope.launch { RemoteDataSource.deleteList(id) }
    }

    /** Removes the current user's membership from a list they don't own. */
    fun leaveList(id: String) {
        _lists.update { it.filter { l -> l.id != id } }
        saveLists(_lists.value)
        val profileId = getOrCreateProfileId()
        scope.launch { RemoteDataSource.removeListMember(id, profileId) }
    }

    /**
     * Fetches a list from the server by its ID and adds/updates it locally.
     * Returns true if the list was found, false if not found or network error.
     */
    suspend fun joinList(listId: String): Boolean {
        val trimmedId = listId.trim()
        val remote = RemoteDataSource.getList(trimmedId) ?: return false
        // Register this user as a member so the list shows up in their filtered view
        val profileId = getOrCreateProfileId()
        RemoteDataSource.addListMember(trimmedId, profileId)
        val list = remote.toGroceryList()
        _lists.update { existing ->
            if (existing.any { it.id == list.id }) {
                existing.map { if (it.id == list.id) list else it }
            } else {
                existing + list
            }
        }
        saveLists(_lists.value)
        return true
    }

    // ── Item operations ────────────────────────────────────────────────────

    fun toggleItem(listId: String, itemId: String) {
        recordMutation()
        updateList(listId) { list ->
            list.copy(items = list.items.map { item ->
                if (item.id == itemId) {
                    val nowChecked = !item.isChecked
                    item.copy(
                        isChecked = nowChecked,
                        checkedAt = if (nowChecked) System.currentTimeMillis() else null
                    )
                } else item
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    fun deleteItem(listId: String, itemId: String) {
        recordMutation()
        updateList(listId) { list -> list.copy(items = list.items.filter { it.id != itemId }) }
        scope.launch { pushDeleteOrQueue(listId, itemId) }
    }

    fun purgeExpiredCheckedItems(listId: String) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val expired = _lists.value
            .find { it.id == listId }
            ?.items
            ?.filter { it.isChecked && (it.checkedAt ?: 0L) < cutoff }
            ?: return
        expired.forEach { item ->
            updateList(listId) { list -> list.copy(items = list.items.filter { it.id != item.id }) }
            scope.launch { pushDeleteOrQueue(listId, item.id) }
        }
    }

    fun updateItemWeekday(listId: String, itemId: String, weekday: String?) {
        recordMutation()
        updateList(listId) { list ->
            list.copy(items = list.items.map { if (it.id == itemId) it.copy(weekday = weekday) else it })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    fun updateItemPrice(listId: String, itemId: String, price: String?) {
        recordMutation()
        updateList(listId) { list ->
            list.copy(items = list.items.map {
                if (it.id == itemId) it.copy(price = price?.ifBlank { null }) else it
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    fun updateItemSupermarket(listId: String, itemId: String, supermarket: String?) {
        recordMutation()
        updateList(listId) { list ->
            list.copy(items = list.items.map {
                if (it.id == itemId) it.copy(supermarket = supermarket?.ifBlank { null }) else it
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    fun setItemQuantity(listId: String, itemId: String, quantity: String) {
        recordMutation()
        val trimmed = quantity.trim().ifBlank { "1" }
        updateList(listId) { list ->
            list.copy(items = list.items.map {
                if (it.id == itemId) it.copy(quantity = trimmed) else it
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    fun updateItemComment(listId: String, itemId: String, comment: String?) {
        recordMutation()
        updateList(listId) { list ->
            list.copy(items = list.items.map {
                if (it.id == itemId) it.copy(comment = comment?.ifBlank { null }) else it
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    fun updateItemName(listId: String, itemId: String, newName: String) {
        recordMutation()
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val oldName = itemIn(listId, itemId)?.name ?: return
        if (oldName == trimmed) return
        updateList(listId) { list ->
            list.copy(items = list.items.map {
                if (it.id == itemId) it.copy(name = trimmed) else it
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
        scope.launch {
            val catalogItem = _catalogItems.value.firstOrNull { it.name.equals(oldName, ignoreCase = true) }
            if (catalogItem != null) {
                val updated = catalogItem.copy(name = trimmed)
                _catalogItems.update { items -> items.map { if (it.id == catalogItem.id) updated else it } }
                RemoteDataSource.upsertCatalogItem(updated)
            }
        }
    }

    fun adjustItemQuantity(listId: String, itemId: String, delta: Int) {
        recordMutation()
        updateList(listId) { list ->
            list.copy(items = list.items.map { item ->
                if (item.id == itemId) item.copy(quantity = adjustQuantityString(item.quantity, delta))
                else item
            })
        }
        scope.launch { pushItemOrQueue(listId, itemId) }
    }

    private fun adjustQuantityString(quantity: String, delta: Int): String {
        val pattern = Regex("^(\\d+(?:[.,]\\d+)?)\\s*(.*)")
        val match = pattern.find(quantity.trim()) ?: return quantity
        val num = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return quantity
        val unit = match.groupValues[2].trim()
        val newNum = (num + delta).coerceAtLeast(1.0)
        val formatted = if (newNum % 1.0 == 0.0) newNum.toLong().toString() else newNum.toString()
        return if (unit.isEmpty()) formatted else "$formatted $unit"
    }

    fun addOrMergeItem(listId: String, name: String, quantity: String, category: String) {
        recordMutation()
        val trimmedName = name.trim()
        val trimmedQty = quantity.trim().ifBlank { "1" }
        updateList(listId) { list ->
            val existing = list.items.firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
            if (existing != null) {
                list.copy(items = list.items.map { item ->
                    if (item.id != existing.id) item
                    else if (existing.isChecked) {
                        // Re-adding a completed item: reset it fully
                        item.copy(
                            quantity = trimmedQty,
                            category = category,
                            isChecked = false,
                            checkedAt = null,
                            weekday = null,
                            price = null,
                            supermarket = null,
                            comment = null,
                        )
                    } else {
                        item.copy(quantity = mergeQuantities(item.quantity, trimmedQty))
                    }
                })
            } else {
                list.copy(
                    items = list.items + GroceryItem(
                        id = UUID.randomUUID().toString(),
                        name = trimmedName,
                        quantity = trimmedQty,
                        category = category
                    )
                )
            }
        }
        scope.launch {
            val item = _lists.value.find { it.id == listId }
                ?.items?.firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
                ?: return@launch
            pushItemOrQueue(listId, item.id)
        }
        saveToCatalog(trimmedName, category)
    }

    private fun mergeQuantities(existing: String, added: String): String {
        val pattern = Regex("^(\\d+(?:[.,]\\d+)?)\\s*(.*)")
        val eMatch = pattern.find(existing.trim())
        val aMatch = pattern.find(added.trim())

        if (eMatch != null && aMatch != null) {
            val eNum = eMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
            val aNum = aMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
            val eUnit = eMatch.groupValues[2].trim()
            val aUnit = aMatch.groupValues[2].trim()

            if (eNum != null && aNum != null && eUnit.equals(aUnit, ignoreCase = true)) {
                val total = eNum + aNum
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                return if (eUnit.isEmpty()) formatted else "$formatted $eUnit"
            }
        }
        return "$existing + $added"
    }

    // ── Sample data (first launch only) ───────────────────────────────────

    private fun sampleLists() = listOf(
        GroceryList(
            id = UUID.randomUUID().toString(),
            name = "Ugentlig indkøb",
            items = sampleItems()
        )
    )

    private fun sampleItems() = listOf(
        GroceryItem(UUID.randomUUID().toString(), "Æbler", "6 stk"),
        GroceryItem(UUID.randomUUID().toString(), "Sødmælk", "1 L"),
        GroceryItem(UUID.randomUUID().toString(), "Kyllingebryst", "500 g"),
        GroceryItem(UUID.randomUUID().toString(), "Surdejsbrød", "1 stk"),
    )
}
