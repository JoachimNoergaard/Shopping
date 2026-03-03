package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.SHOP_BACKGROUND_COLORS
import dk.joachim.shopping.data.SHOP_FOREGROUND_COLORS
import dk.joachim.shopping.data.Shop
import dk.joachim.shopping.data.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopsUiState(
    val shops: List<Shop> = emptyList(),
    val isLoading: Boolean = true,
    val editingShop: Shop? = null,
    val showAddDialog: Boolean = false,
    val dialogName: String = "",
    val dialogBgColor: String = SHOP_BACKGROUND_COLORS.first(),
    val dialogFgColor: String = SHOP_FOREGROUND_COLORS.first(),
)

class ShopsViewModel : ViewModel() {

    private val repository = ShoppingRepository

    private data class DialogState(
        val mode: Mode = Mode.NONE,
        val shop: Shop? = null,
        val name: String = "",
        val bgColor: String = SHOP_BACKGROUND_COLORS.first(),
        val fgColor: String = SHOP_FOREGROUND_COLORS.first(),
    ) {
        enum class Mode { NONE, ADD, EDIT }
    }
    private val _dialog = MutableStateFlow(DialogState())
    private val _isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repository.loadShops()
            _isLoading.value = false
        }
    }

    val uiState = combine(repository.shops, _dialog, _isLoading) { shops, dialog, loading ->
        ShopsUiState(
            shops = shops,
            isLoading = loading,
            showAddDialog = dialog.mode == DialogState.Mode.ADD,
            editingShop = if (dialog.mode == DialogState.Mode.EDIT) dialog.shop else null,
            dialogName = dialog.name,
            dialogBgColor = dialog.bgColor,
            dialogFgColor = dialog.fgColor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ShopsUiState())

    // ── Add ────────────────────────────────────────────────────────────────

    fun showAddDialog() = _dialog.update { DialogState(mode = DialogState.Mode.ADD) }
    fun dismissDialog() = _dialog.update { DialogState() }
    fun updateDialogName(name: String) = _dialog.update { it.copy(name = name) }
    fun updateDialogBgColor(color: String) = _dialog.update { it.copy(bgColor = color) }
    fun updateDialogFgColor(color: String) = _dialog.update { it.copy(fgColor = color) }

    fun addShop() {
        val dialog = _dialog.value
        if (dialog.name.isBlank()) return
        _dialog.update { DialogState() }
        viewModelScope.launch {
            repository.createShop(dialog.name, dialog.bgColor, dialog.fgColor)
        }
    }

    // ── Edit ───────────────────────────────────────────────────────────────

    fun startEdit(shop: Shop) =
        _dialog.update { DialogState(mode = DialogState.Mode.EDIT, shop = shop, name = shop.name, bgColor = shop.backgroundColor, fgColor = shop.foregroundColor) }

    fun saveEdit() {
        val dialog = _dialog.value
        val shop = dialog.shop ?: return
        if (dialog.name.isBlank()) return
        val updated = shop.copy(name = dialog.name.trim(), backgroundColor = dialog.bgColor, foregroundColor = dialog.fgColor)
        _dialog.update { DialogState() }
        viewModelScope.launch { repository.updateShop(updated) }
    }

    // ── Delete + reorder ───────────────────────────────────────────────────

    fun deleteShop(id: String) = repository.deleteShop(id)

    fun reorder(fromIndex: Int, toIndex: Int) {
        val list = repository.shops.value.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val reindexed = list.mapIndexed { i, s -> s.copy(orderIndex = i) }
        // Optimistic local update via repository StateFlow
        viewModelScope.launch {
            repository.reorderShops(reindexed.map { it.id })
            repository.loadShops()
        }
    }
}
