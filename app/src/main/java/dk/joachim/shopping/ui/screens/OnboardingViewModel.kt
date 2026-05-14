package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.network.RemoteDataSource.SendActivationCodeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, PROFILE, VERIFY_CODE, LOGIN }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val email: String = "",
    val verificationCode: String = "",
    val expectedActivationCode: String = "",
    /** Email entered on the dedicated [OnboardingStep.LOGIN] screen. */
    val loginEmail: String = "",
    /** 6-digit code entered on the dedicated [OnboardingStep.LOGIN] screen. */
    val loginCode: String = "",
    /** True once the server has emailed the activation code in the login flow. */
    val loginCodeSent: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Non-error confirmation message shown below the form (e.g. "code emailed"). */
    val info: String? = null,
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

    fun goToLoginStep() {
        _uiState.update { it.copy(step = OnboardingStep.LOGIN, error = null, info = null) }
    }

    fun goBackToWelcomeFromLogin() {
        _uiState.update {
            it.copy(
                step = OnboardingStep.WELCOME,
                loginCode = "",
                loginCodeSent = false,
                error = null,
                info = null,
            )
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name, error = null) }
    fun updateEmail(email: String) = _uiState.update { it.copy(email = email, error = null) }
    fun updateVerificationCode(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(verificationCode = filtered, error = null) }
    }

    fun updateLoginEmail(email: String) =
        _uiState.update { it.copy(loginEmail = email, error = null, info = null, loginCodeSent = false) }
    fun updateLoginCode(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(loginCode = filtered, error = null) }
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
                    // Existing account — email the code, then ask the user to enter it.
                    // Fire-and-forget: the user can still get the code from their other device
                    // even if the SMTP send happens to fail.
                    viewModelScope.launch { repository.requestActivationCodeEmail(email) }
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

    /**
     * Step 1 of explicit login: asks the server to e-mail the activation code for
     * [OnboardingUiState.loginEmail]. On success, reveals the code field in the UI.
     */
    fun sendLoginCode() {
        val email = _uiState.value.loginEmail.trim()
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Indtast venligst en gyldig e-mail", info = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, info = null) }
            when (val res = repository.requestActivationCodeEmail(email)) {
                SendActivationCodeResult.Sent -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        loginCodeSent = true,
                        info = "Vi har sendt en kode til $email. Tjek din indbakke (og evt. spam-mappen).",
                        error = null,
                    )
                }
                SendActivationCodeResult.EmailUnknown -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        loginCodeSent = false,
                        error = "Ingen konto fundet med den e-mail",
                        info = null,
                    )
                }
                is SendActivationCodeResult.Failed -> _uiState.update {
                    val base = "Kunne ikke sende mailen. Prøv igen."
                    val withDetail = res.detail
                        ?.takeIf { d -> d.isNotBlank() }
                        ?.let { d -> "$base\n($d)" }
                        ?: base
                    it.copy(
                        isSubmitting = false,
                        error = withDetail,
                        info = null,
                    )
                }
            }
        }
    }

    /**
     * Step 2 of explicit login: looks up the profile for [OnboardingUiState.loginEmail],
     * validates the user-entered code against the stored activation code, and adopts the
     * existing profile id locally so subsequent data lives under that account.
     */
    fun verifyLoginCode() {
        val state = _uiState.value
        val email = state.loginEmail.trim()
        val code = state.loginCode.trim()

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Indtast venligst en gyldig e-mail") }
            return
        }
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Indtast den 6-cifrede aktiveringskode") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val profile = repository.getProfileByEmail(email)
                when {
                    profile == null -> _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = "Ingen konto fundet med den e-mail",
                        )
                    }
                    profile.activationCode != code -> _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = "Forkert aktiveringskode. Prøv igen.",
                        )
                    }
                    else -> {
                        repository.completeOnboarding(profile.name.ifBlank { "Bruger" }, email)
                        _uiState.update { it.copy(isSubmitting = false, isDone = true) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = "Noget gik galt. Prøv igen.") }
            }
        }
    }
}
