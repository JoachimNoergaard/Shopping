package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.GroceryList
import dk.joachim.shopping.data.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroceryListsUiState(
    val lists: List<GroceryList> = emptyList(),
    val isLoading: Boolean = true,
    val showAddListDialog: Boolean = false,
    val newListName: String = "",
    val navigateToListId: String? = null,
    val pendingDeleteList: GroceryList? = null,
    val currentProfileId: String = ""
)

class GroceryListsViewModel : ViewModel() {

    private val repository = ShoppingRepository

    // True until the first server sync completes (success or failure).
    // Prevents the unfiltered local cache from flashing on screen before
    // the server has applied the list_members filter.
    private val _isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repository.syncAllLists()
            _isLoading.value = false
        }
        viewModelScope.launch { repository.loadCatalogItems() }
        viewModelScope.launch { repository.loadShops() }
    }

    private data class ExtraState(
        val showAddListDialog: Boolean = false,
        val newListName: String = "",
        val navigateToListId: String? = null,
        val pendingDeleteList: GroceryList? = null
    )

    private val _extra = MutableStateFlow(ExtraState())

    val uiState = combine(repository.lists, _extra, _isLoading) { lists, extra, isLoading ->
        GroceryListsUiState(
            lists = lists,
            isLoading = isLoading,
            showAddListDialog = extra.showAddListDialog,
            newListName = extra.newListName,
            navigateToListId = extra.navigateToListId,
            pendingDeleteList = extra.pendingDeleteList,
            currentProfileId = repository.getOrCreateProfileId()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GroceryListsUiState())

    // Add new list
    fun showAddListDialog() = _extra.update { it.copy(showAddListDialog = true) }
    fun dismissAddListDialog() = _extra.update { it.copy(showAddListDialog = false, newListName = "") }
    fun updateNewListName(name: String) = _extra.update { it.copy(newListName = name) }

    fun addList() {
        val name = _extra.value.newListName.trim()
        if (name.isBlank()) return
        repository.addList(name)
        _extra.update { ExtraState() }
    }

    fun consumeNavigation() = _extra.update { it.copy(navigateToListId = null) }

    fun requestDeleteList(list: GroceryList) = _extra.update { it.copy(pendingDeleteList = list) }

    fun dismissDeleteDialog() = _extra.update { it.copy(pendingDeleteList = null) }

    fun confirmDeleteList() {
        val list = _extra.value.pendingDeleteList ?: return
        _extra.update { it.copy(pendingDeleteList = null) }
        val profileId = repository.getOrCreateProfileId()
        if (list.ownerId == profileId) {
            repository.deleteList(list.id)
        } else {
            repository.leaveList(list.id)
        }
    }
}
