<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$requestId = trim($_GET['request_id'] ?? '');
if ($requestId === '') {
    echo json_encode(['status' => 'expired'], JSON_UNESCAPED_UNICODE);
    exit;
}

function web_login_status_response(array $payload): void
{
    unset($_SESSION['pending_login_email'], $_SESSION['pending_login_request_id'], $_SESSION['login_next']);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE);
    exit;
}

$profile = current_profile();
if ($profile !== null) {
    $pendingEmail = (string) ($_SESSION['pending_login_email'] ?? '');
    if ($pendingEmail === '' || strcasecmp($profile['email'], $pendingEmail) === 0) {
        $redirect = safe_redirect_target($_SESSION['login_next'] ?? null);
        $response = [
            'status'    => 'completed',
            'redirect'  => $redirect,
            'logged_in' => true,
        ];
        if (!empty($_SESSION['pending_device_token'])) {
            $response['device_token'] = (string) $_SESSION['pending_device_token'];
            unset($_SESSION['pending_device_token']);
        }
        web_login_status_response($response);
    }
}

$status = get_web_login_request_status($db, $requestId);

if ($status['status'] === 'completed') {
    $established = try_establish_web_login_from_request($db, $requestId);
    if ($established !== null) {
        web_login_status_response([
            'status'       => 'completed',
            'redirect'     => $established['redirect'],
            'logged_in'    => true,
            'device_token' => $established['device_token'] ?? null,
        ]);
    }

    web_login_status_response([
        'status'    => 'completed',
        'redirect'  => $status['redirect'] ?? 'recipes.php',
        'logged_in' => false,
    ]);
}

echo json_encode($status, JSON_UNESCAPED_UNICODE);
