<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

logout_profile();

$deviceToken = trim($_GET['device_token'] ?? '');
if ($deviceToken !== '') {
    revoke_web_device_token($db, $deviceToken);
}

header('Location: index.php');
exit;
