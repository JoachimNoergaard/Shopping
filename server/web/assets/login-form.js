(function () {
    const storage = window.CookingSharkDeviceStorage;
    if (!storage) return;

    const form = document.getElementById('login-email-form');
    if (!form) return;

    const emailInput = document.getElementById('email');
    const rememberInput = document.getElementById('remember-device');
    const autoLoginRoot = document.getElementById('login-auto');
    const params = new URLSearchParams(window.location.search);

    if (params.has('reset')) {
        storage.clearStoredEmail();
        storage.clearDeviceToken();
    }

    if (emailInput && storage.readEmail()) {
        emailInput.value = storage.readEmail();
    }
    if (rememberInput) {
        rememberInput.checked = storage.readRemember();
    }

    form.addEventListener('submit', () => {
        const email = emailInput ? emailInput.value.trim() : '';
        const remember = rememberInput ? rememberInput.checked : false;
        storage.saveRememberPreference(email, remember);
        if (!remember) {
            storage.clearDeviceToken();
        }
    });

    if (autoLoginRoot && storage.readDeviceToken()) {
        storage.tryAutoLogin(autoLoginRoot.getAttribute('data-redirect') || 'recipes.php');
    }
})();
