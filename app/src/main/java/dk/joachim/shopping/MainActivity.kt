package dk.joachim.shopping

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dk.joachim.shopping.data.PendingTimerNavigation
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.ShoppingRepository.LAST_MAIN_SECTION_COOKING
import dk.joachim.shopping.data.ShoppingRepository.LAST_MAIN_SECTION_GROCERY_HOME
import dk.joachim.shopping.data.ShoppingRepository.LAST_MAIN_SECTION_GROCERY_LIST
import dk.joachim.shopping.ui.screens.CatalogItemsScreen
import dk.joachim.shopping.ui.screens.CategoriesScreen
import dk.joachim.shopping.ui.screens.GroceryListScreen
import dk.joachim.shopping.ui.screens.GroceryListsScreen
import dk.joachim.shopping.ui.screens.OnboardingScreen
import dk.joachim.shopping.ui.screens.ProfileScreen
import dk.joachim.shopping.ui.screens.ShopsScreen
import dk.joachim.shopping.ui.theme.ShoppingTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TIMER_RECIPE_ID = "dk.joachim.shopping.EXTRA_TIMER_RECIPE_ID"
        const val EXTRA_TIMER_MENU_PLANS = "dk.joachim.shopping.EXTRA_TIMER_MENU_PLANS"
    }

    private val requestPostNotificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — timers still work in-app if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliverTimerNotificationIntent(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPostNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        setContent {
            ShoppingTheme {
                ShoppingNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverTimerNotificationIntent(intent)
    }

    private fun deliverTimerNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val recipeId = intent.getStringExtra(EXTRA_TIMER_RECIPE_ID)
        val menuPlans = intent.getBooleanExtra(EXTRA_TIMER_MENU_PLANS, false)
        val nav = when {
            recipeId != null ->
                PendingTimerNavigation(recipeId = recipeId, menuPlansOverview = false)
            menuPlans ->
                PendingTimerNavigation(recipeId = null, menuPlansOverview = true)
            else -> null
        }
        if (nav != null) {
            ShoppingRepository.postPendingTimerNavigation(nav)
            intent.removeExtra(EXTRA_TIMER_RECIPE_ID)
            intent.removeExtra(EXTRA_TIMER_MENU_PLANS)
        }
    }
}

@Composable
private fun ShoppingNavHost() {
    val onboardingDone = remember { ShoppingRepository.isOnboardingDone() }

    val navController = rememberNavController()
    val pendingTimerNav by ShoppingRepository.pendingTimerNavigation.collectAsStateWithLifecycle()

    LaunchedEffect(pendingTimerNav) {
        if (pendingTimerNav == null) return@LaunchedEffect
        if (!onboardingDone) {
            ShoppingRepository.clearPendingTimerNavigation()
            return@LaunchedEffect
        }
        val route = navController.currentDestination?.route ?: return@LaunchedEffect
        if (route != "lists") {
            if (!navController.popBackStack("lists", false)) {
                navController.navigate("lists") {
                    launchSingleTop = true
                }
            }
        }
    }

    // Cold start: only deep-link into a list when last session was that list (not Madlavning / Indkøb overview).
    LaunchedEffect(Unit) {
        if (!onboardingDone) return@LaunchedEffect
        if (ShoppingRepository.pendingTimerNavigation.value != null) return@LaunchedEffect
        val section = ShoppingRepository.getLastMainSection()
        val lastListId = ShoppingRepository.getLastListId()?.takeIf { id ->
            ShoppingRepository.lists.value.any { it.id == id }
        }
        when (section) {
            LAST_MAIN_SECTION_GROCERY_LIST -> if (lastListId != null) {
                navController.navigate("list/$lastListId") {
                    launchSingleTop = true
                }
            }
            LAST_MAIN_SECTION_COOKING, LAST_MAIN_SECTION_GROCERY_HOME -> Unit
            null -> if (lastListId != null) {
                // Legacy installs: no section key yet — keep opening the last list once.
                navController.navigate("list/$lastListId") {
                    launchSingleTop = true
                }
                ShoppingRepository.saveLastMainSection(LAST_MAIN_SECTION_GROCERY_LIST)
            }
            else -> Unit
        }
    }

    val startDestination = if (onboardingDone) "lists" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onDone = {
                    navController.navigate("lists") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("lists") {
            GroceryListsScreen(
                onNavigateToList = { listId ->
                    ShoppingRepository.saveLastListId(listId)
                    ShoppingRepository.saveLastMainSection(LAST_MAIN_SECTION_GROCERY_LIST)
                    navController.navigate("list/$listId")
                },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToCategories = { navController.navigate("categories") },
                onNavigateToCatalog = { navController.navigate("catalog") },
                onNavigateToShops = { navController.navigate("shops") }
            )
        }
        composable("list/{listId}") {
            GroceryListScreen(
                onNavigateBack = {
                    ShoppingRepository.saveLastMainSection(LAST_MAIN_SECTION_GROCERY_HOME)
                    navController.popBackStack()
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("categories") {
            CategoriesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("catalog") {
            CatalogItemsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("shops") {
            ShopsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
