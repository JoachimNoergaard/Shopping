package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.UserCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<UserCategory> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val newName: String = "",
)

class CategoriesViewModel : ViewModel() {

    private val repository = ShoppingRepository

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = repository.loadCategories()
            _uiState.update { it.copy(categories = categories, isLoading = false) }
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, newName = "") }
    fun dismissAddDialog() = _uiState.update { it.copy(showAddDialog = false) }
    fun updateNewName(name: String) = _uiState.update { it.copy(newName = name) }

    fun addCategory() {
        val state = _uiState.value
        if (state.newName.isBlank()) return
        viewModelScope.launch {
            val created = repository.createCategory(state.newName.trim())
            if (created != null) {
                _uiState.update { it.copy(
                    categories = it.categories + created,
                    showAddDialog = false,
                ) }
            }
        }
    }

    fun deleteCategory(id: String) {
        _uiState.update { it.copy(categories = it.categories.filter { c -> c.id != id }) }
        viewModelScope.launch { repository.deleteCategory(id) }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.categories.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val reindexed = list.mapIndexed { i, c -> c.copy(orderIndex = i) }
        _uiState.update { it.copy(categories = reindexed) }
        viewModelScope.launch { repository.reorderCategories(reindexed.map { it.id }) }
    }
}
