package dk.joachim.shopping.data.network

import dk.joachim.shopping.data.CatalogItem
import dk.joachim.shopping.data.GroceryItem
import dk.joachim.shopping.data.GroceryList
import dk.joachim.shopping.data.Profile
import dk.joachim.shopping.data.ReorderRequest
import dk.joachim.shopping.data.Shop
import dk.joachim.shopping.data.UserCategory
import retrofit2.HttpException

/**
 * Wraps all API calls in runCatching so network errors are silently ignored —
 * the app always works from local state and degrades gracefully without internet.
 */
object RemoteDataSource {

    private val api: ShoppingApi by lazy { buildShoppingApi() }

    suspend fun getAllLists(profileId: String): List<ApiList>? =
        runCatching { api.getLists(profileId) }.getOrNull()

    suspend fun addListMember(listId: String, profileId: String): Boolean =
        runCatching { api.addListMember(listId, AddMemberRequest(profileId)) }.isSuccess

    suspend fun removeListMember(listId: String, profileId: String): Boolean =
        runCatching { api.removeListMember(listId, profileId) }.isSuccess

    suspend fun getList(id: String): ApiList? =
        runCatching { api.getList(id) }.getOrNull()

    suspend fun createList(list: GroceryList): Boolean =
        runCatching {
            api.createList(CreateListRequest(list.id, list.name, list.ownerId, list.createdAt))
        }.isSuccess

    suspend fun deleteList(id: String): Boolean =
        runCatching { api.deleteList(id) }.isSuccess

    suspend fun upsertItem(listId: String, item: GroceryItem): Boolean =
        runCatching { api.upsertItem(listId, item.id, item) }.isSuccess

    suspend fun deleteItem(listId: String, itemId: String): Boolean =
        runCatching { api.deleteItem(listId, itemId) }.isSuccess

    /**
     * Returns the profile for [email] if it exists on the server, null if the server
     * confirms it doesn't exist (HTTP 404), or throws for any other error (5xx, network
     * failure, etc.) so callers can't silently bypass the email-verification step.
     */
    suspend fun findProfileByEmail(email: String): Profile? {
        val result = runCatching { api.findProfileByEmail(email) }
        val ex = result.exceptionOrNull()
        if (ex is HttpException && ex.code() == 404) return null
        return result.getOrThrow()
    }

    suspend fun getProfile(id: String): Profile? =
        runCatching { api.getProfile(id) }.getOrNull()

    suspend fun upsertProfile(profile: Profile): Profile? =
        runCatching { api.upsertProfile(profile.id, profile) }.getOrNull()

    suspend fun getCategories(profileId: String): List<UserCategory>? =
        runCatching { api.getCategories(profileId) }.getOrNull()

    suspend fun createCategory(profileId: String, category: UserCategory): UserCategory? =
        runCatching { api.createCategory(profileId, category) }.getOrNull()

    suspend fun updateCategory(category: UserCategory): UserCategory? =
        runCatching { api.updateCategory(category.profileId, category.id, category) }.getOrNull()

    suspend fun deleteCategory(profileId: String, id: String): Boolean =
        runCatching { api.deleteCategory(profileId, id) }.isSuccess

    suspend fun reorderCategories(profileId: String, ids: List<String>): Boolean =
        runCatching { api.reorderCategories(profileId, ReorderRequest(ids)) }.isSuccess

    suspend fun getShops(profileId: String): List<Shop>? =
        runCatching { api.getShops(profileId) }.getOrNull()

    suspend fun createShop(profileId: String, shop: Shop): Shop? =
        runCatching { api.createShop(profileId, shop) }.getOrNull()

    suspend fun updateShop(shop: Shop): Shop? =
        runCatching { api.updateShop(shop.profileId, shop.id, shop) }.getOrNull()

    suspend fun deleteShop(profileId: String, id: String): Boolean =
        runCatching { api.deleteShop(profileId, id) }.isSuccess

    suspend fun reorderShops(profileId: String, ids: List<String>): Boolean =
        runCatching { api.reorderShops(profileId, ReorderRequest(ids)) }.isSuccess

    suspend fun getCatalogItems(profileId: String): List<CatalogItem>? =
        runCatching { api.getCatalogItems(profileId) }.getOrNull()

    suspend fun upsertCatalogItem(item: CatalogItem): CatalogItem? =
        runCatching { api.upsertCatalogItem(item.profileId, item.id, item) }.getOrNull()

    suspend fun deleteCatalogItem(profileId: String, id: String): Boolean =
        runCatching { api.deleteCatalogItem(profileId, id) }.isSuccess
}
