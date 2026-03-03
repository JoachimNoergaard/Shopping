package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val activationCode: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
)


class ProfileViewModel : ViewModel() {

    private val repository = ShoppingRepository

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.loadProfile()
            _uiState.update {
                it.copy(
                    name = profile.name,
                    email = profile.email,
                    activationCode = profile.activationCode,
                    isLoading = false
                )
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name, saveSuccess = false) }
    fun updateEmail(email: String) = _uiState.update { it.copy(email = email, saveSuccess = false) }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            repository.saveProfile(_uiState.value.name, _uiState.value.email, _uiState.value.activationCode)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }
}
