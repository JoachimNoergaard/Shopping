package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.CatalogItem
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.UserCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogItemsUiState(
    val items: List<CatalogItem> = emptyList(),
    val categories: List<UserCategory> = emptyList(),
    val showAddDialog: Boolean = false,
    val addName: String = "",
    val addCategory: UserCategory? = null,
    val editingItem: CatalogItem? = null,
    val editName: String = "",
    val editCategory: UserCategory? = null,
)

class CatalogItemsViewModel : ViewModel() {

    private val repository = ShoppingRepository

    private val _categories = MutableStateFlow<List<UserCategory>>(emptyList())

    private data class DialogState(
        val mode: Mode = Mode.NONE,
        val item: CatalogItem? = null,
        val name: String = "",
        val category: UserCategory? = null,
    ) {
        enum class Mode { NONE, ADD, EDIT }
    }
    private val _dialog = MutableStateFlow(DialogState())

    init {
        viewModelScope.launch {
            _categories.value = repository.loadCategories()
        }
        viewModelScope.launch {
            repository.loadCatalogItems()
        }
    }

    val uiState = combine(repository.catalogItems, _dialog, _categories) { items, dialog, categories ->
        CatalogItemsUiState(
            items = items.sortedWith(
                compareBy(
                    { categories.firstOrNull { c -> c.id == it.category }?.orderIndex ?: Int.MAX_VALUE },
                    { it.name.lowercase() }
                )
            ),
            categories = categories,
            showAddDialog = dialog.mode == DialogState.Mode.ADD,
            addName = if (dialog.mode == DialogState.Mode.ADD) dialog.name else "",
            addCategory = if (dialog.mode == DialogState.Mode.ADD) dialog.category else null,
            editingItem = if (dialog.mode == DialogState.Mode.EDIT) dialog.item else null,
            editName = if (dialog.mode == DialogState.Mode.EDIT) dialog.name else "",
            editCategory = if (dialog.mode == DialogState.Mode.EDIT) dialog.category else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogItemsUiState())

    // ── Add ────────────────────────────────────────────────────────────────

    fun showAddDialog() = _dialog.update { DialogState(mode = DialogState.Mode.ADD) }
    fun dismissAddDialog() = _dialog.update { DialogState() }
    fun updateAddName(name: String) = _dialog.update { it.copy(name = name) }
    fun updateAddCategory(category: UserCategory) = _dialog.update { it.copy(category = category) }

    fun addItem() {
        val dialog = _dialog.value
        if (dialog.name.isBlank()) return
        _dialog.update { DialogState() }
        viewModelScope.launch {
            repository.createCatalogItem(
                name = dialog.name,
                category = dialog.category?.id ?: "",
            )
        }
    }

    // ── Edit ───────────────────────────────────────────────────────────────

    fun startEdit(item: CatalogItem) {
        val category = _categories.value.firstOrNull { it.id == item.category }
        _dialog.update { DialogState(mode = DialogState.Mode.EDIT, item = item, name = item.name, category = category) }
    }

    fun dismissEdit() = _dialog.update { DialogState() }
    fun updateEditName(name: String) = _dialog.update { it.copy(name = name) }
    fun updateEditCategory(category: UserCategory) = _dialog.update { it.copy(category = category) }

    fun saveEdit() {
        val dialog = _dialog.value
        val item = dialog.item ?: return
        if (dialog.name.isBlank()) return
        _dialog.update { DialogState() }
        viewModelScope.launch {
            repository.updateCatalogItem(
                id = item.id,
                name = dialog.name,
                category = dialog.category?.id ?: item.category,
            )
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteCatalogItem(id) }
    }
}
