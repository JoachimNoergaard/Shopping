<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

logout_profile();
header('Location: index.php');
exit;
