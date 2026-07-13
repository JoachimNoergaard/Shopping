<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

if (current_profile() !== null) {
    header('Location: recipes.php');
    exit;
}

$token = trim($_GET['token'] ?? '');
if ($token === '') {
    flash_set('error', 'Ugyldigt login-link.');
    header('Location: index.php');
    exit;
}

$result = complete_web_login_by_token($db, $token);
if ($result === null) {
    flash_set('error', 'Login-linket er ugyldigt eller udløbet. Anmod om et nyt link.');
    header('Location: index.php');
    exit;
}

login_profile($result['profile']);

if (!empty($result['device_token'])) {
    $_SESSION['pending_device_token'] = $result['device_token'];
}

$params = http_build_query([
    'rid'      => $result['request_id'] ?? '',
    'redirect' => $result['redirect'],
], '', '&', PHP_QUERY_RFC3986);

header('Location: login-complete.php?' . $params);
exit;
