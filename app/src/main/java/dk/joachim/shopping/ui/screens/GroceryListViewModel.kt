package dk.joachim.shopping.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.CatalogItem
import dk.joachim.shopping.data.GroceryItem
import dk.joachim.shopping.data.GroceryList
import dk.joachim.shopping.data.Shop
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.UserCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroceryListUiState(
    val list: GroceryList? = null,
    val showAddItemDialog: Boolean = false,
    val newItemName: String = "",
    val newItemQuantity: String = "",
    val newItemCategory: UserCategory? = null,
    val userCategories: List<UserCategory> = emptyList(),
    val catalogItems: List<CatalogItem> = emptyList(),
    val shops: List<Shop> = emptyList(),
    val itemAddedCount: Int = 0,
    val initialSyncDone: Boolean = false,
)

class GroceryListViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])
    private val repository = ShoppingRepository

    private val _userCategories = MutableStateFlow<List<UserCategory>>(emptyList())
    private val _initialSyncDone = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _userCategories.value = repository.loadCategories()
        }
        viewModelScope.launch {
            repository.purgeExpiredCheckedItems(listId)
        }
        viewModelScope.launch {
            repository.syncList(listId)
            _initialSyncDone.value = true
            while (true) {
                delay(POLL_INTERVAL_MS)
                repository.syncList(listId)
            }
        }
    }

    private data class DialogState(
        val show: Boolean = false,
        val name: String = "",
        val quantity: String = "1",
        val category: UserCategory? = null,
        val itemAddedCount: Int = 0,
    )
    private val _dialog = MutableStateFlow(DialogState())

    val uiState = combine(
        repository.lists,
        _dialog,
        _userCategories,
        repository.catalogItems,
        combine(repository.shops, _initialSyncDone) { s, d -> s to d },
    ) { lists, dialog, categories, catalog, shopsAndSync ->
        val (shops, syncDone) = shopsAndSync
        GroceryListUiState(
            list = lists.find { it.id == listId },
            showAddItemDialog = dialog.show,
            newItemName = dialog.name,
            newItemQuantity = dialog.quantity,
            newItemCategory = dialog.category,
            userCategories = categories,
            catalogItems = catalog,
            shops = shops,
            itemAddedCount = dialog.itemAddedCount,
            initialSyncDone = syncDone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GroceryListUiState())

    // ── Item operations ────────────────────────────────────────────────────

    fun toggleItem(itemId: String) = repository.toggleItem(listId, itemId)
    fun deleteItem(itemId: String) = repository.deleteItem(listId, itemId)
    fun updateItemWeekday(itemId: String, weekday: String?) = repository.updateItemWeekday(listId, itemId, weekday)
    fun updateItemPrice(itemId: String, price: String?) = repository.updateItemPrice(listId, itemId, price)
    fun updateItemSupermarket(itemId: String, supermarket: String?) = repository.updateItemSupermarket(listId, itemId, supermarket)
    fun adjustItemQuantity(itemId: String, delta: Int) = repository.adjustItemQuantity(listId, itemId, delta)
    fun setItemQuantity(itemId: String, quantity: String) = repository.setItemQuantity(listId, itemId, quantity)
    fun updateItemComment(itemId: String, comment: String?) = repository.updateItemComment(listId, itemId, comment)
    fun updateItemName(itemId: String, name: String) = repository.updateItemName(listId, itemId, name)

    // ── Add-item dialog ────────────────────────────────────────────────────

    fun showAddItemDialog() = _dialog.update { it.copy(show = true) }

    fun dismissAddItemDialog() = _dialog.update { DialogState() }

    fun updateNewItemName(name: String) = _dialog.update { it.copy(name = name) }
    fun updateNewItemQuantity(quantity: String) = _dialog.update { it.copy(quantity = quantity) }
    fun updateNewItemCategory(category: UserCategory) = _dialog.update { it.copy(category = category) }

    fun fillFromSuggestion(item: GroceryItem) {
        val category = _userCategories.value.firstOrNull { it.id == item.category }
        _dialog.update { it.copy(name = item.name, category = category) }
    }

    fun fillFromCatalogSuggestion(item: CatalogItem) {
        val category = _userCategories.value.firstOrNull { it.id == item.category }
        _dialog.update { it.copy(name = item.name, category = category) }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
    }

    fun addItem() {
        val dialog = _dialog.value
        if (dialog.name.isBlank()) return
        // Allow adding without a category when none exist yet
        if (dialog.category == null && _userCategories.value.isNotEmpty()) return
        repository.addOrMergeItem(listId, dialog.name, dialog.quantity, dialog.category?.id ?: "")
        _dialog.update { it.copy(name = "", quantity = "1", category = null, itemAddedCount = it.itemAddedCount + 1) }
    }

    fun deleteCheckedItems() {
        val checkedIds = uiState.value.list?.items
            ?.filter { it.isChecked }
            ?.map { it.id }
            ?: return
        checkedIds.forEach { repository.deleteItem(listId, it) }
    }
}
