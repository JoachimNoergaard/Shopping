# SharkShopper 🦈

A collaborative grocery shopping app for Android, built with Jetpack Compose and Material Design 3.

## Features

- **Multiple grocery lists** — create, share, and manage lists across devices
- **Real-time sync** — lists update every 5 seconds while open, so changes from other users appear instantly
- **Item details** — attach quantity, weekday, price, supermarket tag, and a free-text comment to any item
- **Category grouping** — items are grouped and sorted by your custom category order
- **Personal catalog** — previously used items are remembered and surfaced as autocomplete suggestions when adding new ones
- **Swipe to complete** — swipe an item right-to-left to toggle it checked/unchecked
- **Completed section** — checked items collapse into a "Completed" section and are automatically purged after 24 hours
- **Supermarket tags** — colour-coded shop pills on items show where something is on sale
- **MobilePay shortcut** — one-tap button in each list to open MobilePay
- **Supermarket flyer links** — quick links to weekly flyers for Rema 1000, Netto, Føtex, Bilka, and 365 Discount
- **Multi-device support** — link the same profile on a second device using a 6-digit activation code

## Screens

| Screen | Description |
|---|---|
| **Onboarding** | First-run flow: welcome splash, profile creation (name + email), and activation code entry for existing accounts |
| **Lists** | Home screen — all grocery lists with item counts and completion status |
| **List detail** | Items grouped by category; supports adding, editing, toggling, and deleting items |
| **Categories** | Manage and reorder personal item categories (affects grouping in lists) |
| **Catalog** | Personal item catalog grouped by category; items appear as autocomplete suggestions |
| **Shops** | Manage stores with custom colour-coded pill tags (drag to reorder) |
| **Profile** | Edit name and email; copy the activation code to link a second device |

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI** | Jetpack Compose + Material Design 3 |
| **Architecture** | MVVM — `ViewModel` + `StateFlow` + Compose `collectAsStateWithLifecycle` |
| **Navigation** | Navigation Compose |
| **Networking** | Retrofit 2 + OkHttp 4 + Kotlinx Serialization |
| **Backend** | PHP REST API (`server/api.php`) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |

## Project Structure

```
app/src/main/java/dk/joachim/shopping/
├── MainActivity.kt
├── ShoppingApplication.kt
├── data/
│   ├── GroceryList.kt          # List + item data models
│   ├── GroceryItem.kt
│   ├── CatalogItem.kt
│   ├── Shop.kt
│   ├── UserCategory.kt
│   ├── Profile.kt
│   ├── ShoppingRepository.kt   # Single source of truth; local + remote state
│   └── network/
│       ├── ShoppingApi.kt      # Retrofit interface
│       ├── RemoteDataSource.kt # API call implementations
│       ├── ApiModels.kt        # API-layer DTOs
│       └── NetworkConfig.kt    # Base URL + HTTP client
└── ui/
    ├── theme/                  # Color, Type, Theme
    └── screens/                # One Screen + ViewModel per feature
```

## Getting Started

1. Clone the repository
2. Open in Android Studio (Ladybug or newer recommended)
3. Build and run on a device or emulator running Android 8.0+

The app connects to the hosted PHP backend automatically on first launch and guides new users through the onboarding flow.
