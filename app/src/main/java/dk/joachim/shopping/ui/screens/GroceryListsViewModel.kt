package dk.joachim.shopping.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
    val editingList: GroceryList? = null,
    val editListName: String = "",
    val editShareEnabled: Boolean = false,
    val editShareUrl: String? = null,
    val editShareLoading: Boolean = false,
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
        val pendingDeleteList: GroceryList? = null,
        val editingList: GroceryList? = null,
        val editListName: String = "",
        val editShareEnabled: Boolean = false,
        val editShareUrl: String? = null,
        val editShareLoading: Boolean = false,
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
            editingList = extra.editingList,
            editListName = extra.editListName,
            editShareEnabled = extra.editShareEnabled,
            editShareUrl = extra.editShareUrl,
            editShareLoading = extra.editShareLoading,
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

    fun openEditListDialog(list: GroceryList) {
        val profileId = repository.getOrCreateProfileId()
        val isOwner = list.ownerId == profileId
        _extra.update {
            it.copy(
                editingList = list,
                editListName = list.name,
                editShareEnabled = list.shareEnabled,
                editShareUrl = null,
                editShareLoading = isOwner,
            )
        }
        if (!isOwner) return

        viewModelScope.launch {
            val share = repository.getListShare(list.id)
            _extra.update { extra ->
                if (extra.editingList?.id != list.id) extra
                else extra.copy(
                    editShareEnabled = share?.shareEnabled ?: list.shareEnabled,
                    editShareUrl = share?.shareUrl,
                    editShareLoading = false,
                )
            }
        }
    }

    fun dismissEditListDialog() =
        _extra.update {
            it.copy(
                editingList = null,
                editListName = "",
                editShareEnabled = false,
                editShareUrl = null,
                editShareLoading = false,
            )
        }

    fun updateEditListName(name: String) = _extra.update { it.copy(editListName = name) }

    fun saveEditedListName() {
        val list = _extra.value.editingList ?: return
        if (list.ownerId != repository.getOrCreateProfileId()) return
        val name = _extra.value.editListName.trim()
        if (name.isBlank()) return
        if (name != list.name) {
            repository.renameList(list.id, name)
        }
        dismissEditListDialog()
    }

    fun setEditListShareEnabled(enabled: Boolean) {
        val list = _extra.value.editingList ?: return
        if (list.ownerId != repository.getOrCreateProfileId()) return
        if (enabled == _extra.value.editShareEnabled) return

        viewModelScope.launch {
            _extra.update { it.copy(editShareLoading = true) }
            val response = if (enabled) {
                repository.enableListShare(list.id)
            } else {
                repository.disableListShare(list.id)
            }
            _extra.update { extra ->
                if (extra.editingList?.id != list.id) extra
                else extra.copy(
                    editShareEnabled = response?.shareEnabled ?: false,
                    editShareUrl = response?.shareUrl,
                    editShareLoading = false,
                )
            }
        }
    }

    fun shareEditListLink(context: Context) {
        val list = _extra.value.editingList ?: return
        if (list.ownerId != repository.getOrCreateProfileId()) return

        viewModelScope.launch {
            var url = _extra.value.editShareUrl
            if (!_extra.value.editShareEnabled || url.isNullOrBlank()) {
                _extra.update { it.copy(editShareLoading = true) }
                val response = repository.enableListShare(list.id)
                url = response?.shareUrl
                _extra.update { extra ->
                    if (extra.editingList?.id != list.id) extra
                    else extra.copy(
                        editShareEnabled = response?.shareEnabled ?: false,
                        editShareUrl = url,
                        editShareLoading = false,
                    )
                }
            }
            if (url.isNullOrBlank()) {
                Toast.makeText(context, "Kunne ikke hente delingslink", Toast.LENGTH_SHORT).show()
                return@launch
            }
            shareListLink(context, list.name, url)
        }
    }

    fun copyEditListLink(context: Context) {
        val url = _extra.value.editShareUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "Intet delingslink endnu", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Delingslink", url))
        Toast.makeText(context, "Link kopieret", Toast.LENGTH_SHORT).show()
    }

    /** Opens the existing delete/leave confirmation; closes the edit dialog. */
    fun requestDeleteFromEditDialog() {
        val list = _extra.value.editingList ?: return
        _extra.update {
            it.copy(
                editingList = null,
                editListName = "",
                editShareEnabled = false,
                editShareUrl = null,
                editShareLoading = false,
                pendingDeleteList = list,
            )
        }
    }

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

    private fun shareListLink(context: Context, listName: String, url: String) {
        val text = "$listName\n$url"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, listName)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "Del indkøbsliste").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Ingen apps kan dele tekst", Toast.LENGTH_SHORT).show()
        }
    }
}
