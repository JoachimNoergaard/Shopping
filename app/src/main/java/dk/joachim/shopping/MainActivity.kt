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

    // Validate the last list ID still refers to an existing list
    val lastListId = remember {
        if (!onboardingDone) return@remember null
        val id = ShoppingRepository.getLastListId()
        if (id != null && ShoppingRepository.lists.value.any { it.id == id }) id else null
    }

    val navController = rememberNavController()

    // Navigate to the last list immediately after first composition (only when onboarding is done)
    LaunchedEffect(Unit) {
        if (lastListId != null) {
            navController.navigate("list/$lastListId")
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
                onNavigateBack = { navController.popBackStack() }
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
