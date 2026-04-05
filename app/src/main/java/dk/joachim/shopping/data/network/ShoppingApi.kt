package dk.joachim.shopping.data.network

import dk.joachim.shopping.data.CatalogItem
import dk.joachim.shopping.data.GroceryItem
import dk.joachim.shopping.data.Profile
import dk.joachim.shopping.data.ReorderRequest
import dk.joachim.shopping.data.Shop
import dk.joachim.shopping.data.UserCategory
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ShoppingApi {

    @GET("lists")
    suspend fun getLists(@Query("profileId") profileId: String = ""): List<ApiList>

    @POST("lists")
    suspend fun createList(@Body request: CreateListRequest): ApiList

    @POST("lists/{id}/members")
    suspend fun addListMember(@Path("id") listId: String, @Body request: AddMemberRequest)

    @DELETE("lists/{id}/members/{profileId}")
    suspend fun removeListMember(@Path("id") listId: String, @Path("profileId") profileId: String)

    @GET("lists/{id}")
    suspend fun getList(@Path("id") id: String): ApiList

    @PATCH("lists/{id}")
    suspend fun patchList(@Path("id") id: String, @Body request: PatchListRequest)

    @DELETE("lists/{id}")
    suspend fun deleteList(@Path("id") id: String)

    @PUT("lists/{id}/items/{itemId}")
    suspend fun upsertItem(
        @Path("id") listId: String,
        @Path("itemId") itemId: String,
        @Body item: GroceryItem,
    ): GroceryItem

    @DELETE("lists/{id}/items/{itemId}")
    suspend fun deleteItem(
        @Path("id") listId: String,
        @Path("itemId") itemId: String,
    )

    @GET("profile/by-email")
    suspend fun findProfileByEmail(@Query("email") email: String): Profile

    @GET("profile/{id}")
    suspend fun getProfile(@Path("id") id: String): Profile

    @PUT("profile/{id}")
    suspend fun upsertProfile(@Path("id") id: String, @Body profile: Profile): Profile

    @GET("profile/{profileId}/categories")
    suspend fun getCategories(@Path("profileId") profileId: String): List<UserCategory>

    @POST("profile/{profileId}/categories")
    suspend fun createCategory(@Path("profileId") profileId: String, @Body category: UserCategory): UserCategory

    @PUT("profile/{profileId}/categories/{id}")
    suspend fun updateCategory(
        @Path("profileId") profileId: String,
        @Path("id") id: String,
        @Body category: UserCategory,
    ): UserCategory

    @DELETE("profile/{profileId}/categories/{id}")
    suspend fun deleteCategory(@Path("profileId") profileId: String, @Path("id") id: String)

    @POST("profile/{profileId}/categories/reorder")
    suspend fun reorderCategories(@Path("profileId") profileId: String, @Body request: ReorderRequest)

    @GET("profile/{profileId}/shops")
    suspend fun getShops(@Path("profileId") profileId: String): List<Shop>

    @POST("profile/{profileId}/shops")
    suspend fun createShop(@Path("profileId") profileId: String, @Body shop: Shop): Shop

    @PUT("profile/{profileId}/shops/{id}")
    suspend fun updateShop(
        @Path("profileId") profileId: String,
        @Path("id") id: String,
        @Body shop: Shop,
    ): Shop

    @DELETE("profile/{profileId}/shops/{id}")
    suspend fun deleteShop(@Path("profileId") profileId: String, @Path("id") id: String)

    @POST("profile/{profileId}/shops/reorder")
    suspend fun reorderShops(@Path("profileId") profileId: String, @Body request: ReorderRequest)

    @GET("profile/{profileId}/catalog")
    suspend fun getCatalogItems(@Path("profileId") profileId: String): List<CatalogItem>

    @PUT("profile/{profileId}/catalog/{id}")
    suspend fun upsertCatalogItem(
        @Path("profileId") profileId: String,
        @Path("id") id: String,
        @Body item: CatalogItem,
    ): CatalogItem

    @DELETE("profile/{profileId}/catalog/{id}")
    suspend fun deleteCatalogItem(@Path("profileId") profileId: String, @Path("id") id: String)

    // ── Menu plans ────────────────────────────────────────────────────────────

    @GET("profile/{profileId}/menu-plans")
    suspend fun getMenuPlans(@Path("profileId") profileId: String): List<ApiMenuPlan>

    @PUT("profile/{profileId}/menu-plans/{id}")
    suspend fun upsertMenuPlan(
        @Path("profileId") profileId: String,
        @Path("id") id: String,
        @Body plan: UpsertMenuPlanRequest,
    ): ApiMenuPlan

    @DELETE("profile/{profileId}/menu-plans/{id}")
    suspend fun deleteMenuPlan(@Path("profileId") profileId: String, @Path("id") id: String)

    // ── Recipes ───────────────────────────────────────────────────────────────

    @GET("profile/{profileId}/recipes")
    suspend fun getRecipes(@Path("profileId") profileId: String): List<ApiRecipe>

    @PUT("profile/{profileId}/recipes/{id}")
    suspend fun upsertRecipe(
        @Path("profileId") profileId: String,
        @Path("id") id: String,
        @Body recipe: UpsertRecipeRequest,
    ): ApiRecipe

    @DELETE("profile/{profileId}/recipes/{id}")
    suspend fun deleteRecipe(@Path("profileId") profileId: String, @Path("id") id: String)
}

fun buildShoppingApi(): ShoppingApi {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    return Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()
        )
        .build()
        .create(ShoppingApi::class.java)
}
