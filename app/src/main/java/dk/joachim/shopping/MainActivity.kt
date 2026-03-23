package dk.joachim.shopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShoppingTheme {
                ShoppingNavHost()
            }
        }
    }
}

@Composable
private fun ShoppingNavHost() {
    val onboardingDone = remember { ShoppingRepository.isOnboardingDone() }

    val navController = rememberNavController()

    // Cold start: only deep-link into a list when last session was that list (not Madlavning / Indkøb overview).
    LaunchedEffect(Unit) {
        if (!onboardingDone) return@LaunchedEffect
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
