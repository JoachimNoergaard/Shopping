package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, PROFILE, VERIFY_CODE }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val email: String = "",
    val verificationCode: String = "",
    val expectedActivationCode: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
)

class OnboardingViewModel : ViewModel() {

    private val repository = ShoppingRepository

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun goToProfileStep() {
        _uiState.update { it.copy(step = OnboardingStep.PROFILE) }
    }

    fun goBackToProfile() {
        _uiState.update { it.copy(step = OnboardingStep.PROFILE, verificationCode = "", error = null) }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name, error = null) }
    fun updateEmail(email: String) = _uiState.update { it.copy(email = email, error = null) }
    fun updateVerificationCode(code: String) {
        // Only allow digits, max 6 characters
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(verificationCode = filtered, error = null) }
    }

    fun submit() {
        val name = _uiState.value.name.trim()
        val email = _uiState.value.email.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Indtast venligst dit navn") }
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Indtast venligst en gyldig e-mail") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val existingProfile = repository.getProfileByEmail(email)
                if (existingProfile != null) {
                    // Existing account — require activation code verification before adopting
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            step = OnboardingStep.VERIFY_CODE,
                            expectedActivationCode = existingProfile.activationCode
                        )
                    }
                } else {
                    repository.completeOnboarding(name, email)
                    _uiState.update { it.copy(isSubmitting = false, isDone = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = "Noget gik galt. Prøv igen.") }
            }
        }
    }

    fun verifyCode() {
        val state = _uiState.value

        if (state.verificationCode != state.expectedActivationCode) {
            _uiState.update { it.copy(error = "Forkert aktiveringskode. Prøv igen.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.completeOnboarding(state.name, state.email)
                _uiState.update { it.copy(isSubmitting = false, isDone = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = "Noget gik galt. Prøv igen.") }
            }
        }
    }
}
