<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

if (current_profile() !== null) {
    echo json_encode(['ok' => true, 'redirect' => 'recipes.php'], JSON_UNESCAPED_UNICODE);
    exit;
}

$token = trim($_POST['device_token'] ?? $_GET['device_token'] ?? '');
if ($token === '') {
    echo json_encode(['ok' => false], JSON_UNESCAPED_UNICODE);
    exit;
}

$result = login_profile_by_device_token($db, $token);
if ($result === null) {
    echo json_encode(['ok' => false], JSON_UNESCAPED_UNICODE);
    exit;
}

login_profile($result['profile']);
echo json_encode([
    'ok'           => true,
    'redirect'     => 'recipes.php',
    'device_token' => $result['device_token'],
], JSON_UNESCAPED_UNICODE);
