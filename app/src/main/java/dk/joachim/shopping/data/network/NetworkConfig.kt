package dk.joachim.shopping.data.network

object NetworkConfig {
    /**
     * Base URL of your PHP server API (must end with /).
     *   Android emulator → local machine : "http://10.0.2.2/shopping/api/"
     *   Real device on same Wi-Fi network: "http://192.168.x.x/shopping/api/"
     *   Production server                : "https://yourserver.com/shopping/api/"
     */
    const val BASE_URL = "https://joachim.dk/shopping/"

    /** Browser UI for creating and editing recipes (syncs with the app). */
    const val RECIPE_WEB_PORTAL_URL = BASE_URL + "web/"

    /** Public read-only recipe page (no login required). */
    fun recipeWebViewUrl(recipeId: String): String =
        RECIPE_WEB_PORTAL_URL + "recipe.php?id=" + recipeId
}
