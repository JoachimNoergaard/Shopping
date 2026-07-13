(function () {
    const KEYS = {
        remember: 'cookingshark_web_remember',
        email: 'cookingshark_web_email',
        deviceToken: 'cookingshark_web_device_token',
    };

    function readRemember() {
        return localStorage.getItem(KEYS.remember) === '1';
    }

    function readEmail() {
        return localStorage.getItem(KEYS.email) || '';
    }

    function readDeviceToken() {
        return localStorage.getItem(KEYS.deviceToken) || '';
    }

    function saveRememberPreference(email, remember) {
        if (remember) {
            localStorage.setItem(KEYS.remember, '1');
            localStorage.setItem(KEYS.email, email);
        } else {
            localStorage.removeItem(KEYS.remember);
            localStorage.removeItem(KEYS.email);
        }
    }

    function saveDeviceToken(token) {
        if (!token) return;
        localStorage.setItem(KEYS.deviceToken, token);
        localStorage.setItem(KEYS.remember, '1');
    }

    function clearDeviceToken() {
        localStorage.removeItem(KEYS.deviceToken);
    }

    function clearStoredEmail() {
        localStorage.removeItem(KEYS.email);
        localStorage.removeItem(KEYS.remember);
    }

    function consumeHashDeviceToken() {
        const hash = window.location.hash.replace(/^#/, '');
        if (!hash) return null;
        const params = new URLSearchParams(hash);
        const token = params.get('device_token');
        if (!token) return null;
        saveDeviceToken(token);
        if (window.history.replaceState) {
            const cleanUrl = window.location.pathname + window.location.search;
            window.history.replaceState(null, '', cleanUrl);
        } else {
            window.location.hash = '';
        }
        return token;
    }

    function loginWithDeviceToken(token) {
        const body = new URLSearchParams();
        body.set('device_token', token);
        return fetch('device-login.php', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: body.toString(),
        }).then((response) => response.json());
    }

    function tryAutoLogin(redirect) {
        consumeHashDeviceToken();
        const token = readDeviceToken();
        if (!token) return Promise.resolve(false);
        return loginWithDeviceToken(token).then((data) => {
            if (!data.ok) {
                clearDeviceToken();
                return false;
            }
            if (data.device_token) {
                saveDeviceToken(data.device_token);
            }
            if (redirect) {
                window.location.href = data.redirect || redirect;
            }
            return true;
        }).catch(() => false);
    }

    function wireLogoutLink() {
        document.querySelectorAll('[data-logout-link]').forEach((link) => {
            if (link.dataset.wired) return;
            link.dataset.wired = '1';
            link.addEventListener('click', (event) => {
                const href = link.getAttribute('href') || 'logout.php';
                const token = readDeviceToken();
                if (token) {
                    event.preventDefault();
                    const url = new URL(href, window.location.href);
                    url.searchParams.set('device_token', token);
                    clearDeviceToken();
                    window.location.href = url.toString();
                }
            });
        });
    }

    window.CookingSharkDeviceStorage = {
        KEYS,
        readRemember,
        readEmail,
        readDeviceToken,
        saveRememberPreference,
        saveDeviceToken,
        clearDeviceToken,
        clearStoredEmail,
        consumeHashDeviceToken,
        loginWithDeviceToken,
        tryAutoLogin,
        wireLogoutLink,
    };

    consumeHashDeviceToken();
    wireLogoutLink();
})();
