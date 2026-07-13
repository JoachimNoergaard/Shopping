(function () {
    const root = document.getElementById('login-pending');
    if (!root) return;

    const requestId = root.getAttribute('data-request-id') || '';
    const redirect = root.getAttribute('data-redirect') || 'recipes.php';
    if (!requestId) return;

    const LOGIN_REQUEST_KEY = 'cookingshark_web_login_request';
    let stopped = false;

    function goToRecipes(target) {
        if (stopped) return;
        stopped = true;
        window.location.href = target || redirect;
    }

    function storeDeviceToken(token) {
        if (!token || !window.CookingSharkDeviceStorage) return;
        window.CookingSharkDeviceStorage.saveDeviceToken(token);
    }

    function handleCompleted(data) {
        storeDeviceToken(data.device_token);
        goToRecipes(data.redirect || redirect);
    }

    function poll() {
        if (stopped) return;
        fetch('login-status.php?request_id=' + encodeURIComponent(requestId), {
            credentials: 'same-origin',
            headers: { Accept: 'application/json' },
        })
            .then((response) => response.json())
            .then((data) => {
                if (data.status === 'completed' && data.logged_in !== false) {
                    handleCompleted(data);
                    return;
                }
                if (data.status === 'completed' && data.logged_in === false) {
                    const statusEl = root.querySelector('.login-pending-status');
                    if (statusEl) {
                        statusEl.textContent = 'Linket er brugt. Åbn login-linket på denne enhed for at fortsætte her.';
                    }
                    stopped = true;
                    return;
                }
                if (data.status === 'expired') {
                    stopped = true;
                    window.location.href = 'index.php?reset=1';
                    return;
                }
                window.setTimeout(poll, 1000);
            })
            .catch(() => {
                window.setTimeout(poll, 2000);
            });
    }

    window.addEventListener('storage', (event) => {
        if (event.key === LOGIN_REQUEST_KEY && event.newValue === requestId) {
            poll();
        }
    });

    try {
        if (localStorage.getItem(LOGIN_REQUEST_KEY) === requestId) {
            localStorage.removeItem(LOGIN_REQUEST_KEY);
            poll();
        }
    } catch (e) {}

    poll();
})();
