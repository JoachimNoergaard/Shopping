<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

if (current_profile() === null) {
    header('Location: index.php');
    exit;
}

$requestId = trim($_GET['rid'] ?? '');
$redirect = safe_redirect_target($_GET['redirect'] ?? null);
$deviceToken = (string) ($_SESSION['pending_device_token'] ?? '');
unset($_SESSION['pending_device_token']);

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
?>
<!DOCTYPE html>
<html lang="da">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Logger ind · CookingShark</title>
</head>
<body>
<script src="assets/device-storage.js"></script>
<script>
(function () {
    var requestId = <?= json_encode($requestId, JSON_UNESCAPED_UNICODE) ?>;
    var redirect = <?= json_encode($redirect, JSON_UNESCAPED_UNICODE) ?>;
    var deviceToken = <?= json_encode($deviceToken, JSON_UNESCAPED_UNICODE) ?>;

    if (requestId) {
        try {
            localStorage.setItem('cookingshark_web_login_request', requestId);
        } catch (e) {}
    }

    if (deviceToken && window.CookingSharkDeviceStorage) {
        window.CookingSharkDeviceStorage.saveDeviceToken(deviceToken);
    } else if (deviceToken) {
        try {
            localStorage.setItem('cookingshark_web_device_token', deviceToken);
        } catch (e) {}
    }

    if (deviceToken) {
        window.location.replace(redirect + '#device_token=' + encodeURIComponent(deviceToken));
    } else {
        window.location.replace(redirect);
    }
})();
</script>
<p>Logger ind…</p>
</body>
</html>
