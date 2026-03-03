<?php
/**
 * Shopping List REST API — MySQL edition
 * Place this file (and .htaccess) inside a directory on your PHP server,
 * e.g. /var/www/html/shopping/api/
 *
 * Endpoints
 * ---------
 * GET    /lists                          List all lists (with items)
 * POST   /lists                          Create a list
 * GET    /lists/{id}                     Get one list with items
 * PATCH  /lists/{id}                     Rename a list
 * DELETE /lists/{id}                     Delete list + items
 * PUT    /lists/{id}/items/{itemId}      Create or update an item
 * DELETE /lists/{id}/items/{itemId}      Delete an item
 */

// ── Database config — fill these in ───────────────────────────────────────
define('DB_HOST', 'localhost');
define('DB_NAME', 'joachimd_shopping');
define('DB_USER', 'joachimd_shopping');
define('DB_PASS', 'Erk$*XbB%q(8#ge^');

// ── Headers ────────────────────────────────────────────────────────────────
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ── Database connection ────────────────────────────────────────────────────
$db = new PDO(
    'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
    DB_USER,
    DB_PASS,
    [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]
);

// ── Schema (auto-created on first request) ─────────────────────────────────
$db->exec('CREATE TABLE IF NOT EXISTS lists (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    owner_id   VARCHAR(36)  NOT NULL DEFAULT \'\',
    created_at BIGINT       NOT NULL,
    updated_at BIGINT       NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

// Add owner_id column to existing tables that were created before this field existed
try {
    $db->exec("ALTER TABLE lists ADD COLUMN owner_id VARCHAR(36) NOT NULL DEFAULT ''");
} catch (PDOException $e) {
    // Column already exists — ignore
}

$db->exec('CREATE TABLE IF NOT EXISTS items (
    id          VARCHAR(36)  NOT NULL,
    list_id     VARCHAR(36)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    quantity    VARCHAR(100) NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    is_checked  TINYINT(1)   NOT NULL DEFAULT 0,
    checked_at  BIGINT,
    weekday     VARCHAR(10),
    price       VARCHAR(100),
    supermarket VARCHAR(100),
    comment     TEXT,
    updated_at  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

// Add comment column to existing tables that were created before this field existed
try {
    $db->exec('ALTER TABLE items ADD COLUMN comment TEXT');
} catch (PDOException $e) {
    // Column already exists — ignore
}

$db->exec('CREATE TABLE IF NOT EXISTS categories (
    id          VARCHAR(36)  NOT NULL,
    profile_id  VARCHAR(36)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    order_index INT          NOT NULL DEFAULT 0,
    updated_at  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

$db->exec('CREATE TABLE IF NOT EXISTS profiles (
    id              VARCHAR(36)  NOT NULL,
    name            VARCHAR(255) NOT NULL DEFAULT \'\',
    email           VARCHAR(255) NOT NULL DEFAULT \'\',
    activation_code VARCHAR(10)  NOT NULL DEFAULT \'\',
    updated_at      BIGINT       NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
// Migration: add email column for databases created before it was introduced
try {
    $db->exec("ALTER TABLE profiles ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT ''");
} catch (PDOException $e) { /* already exists, ignore */ }
try {
    $db->exec('ALTER TABLE profiles ADD INDEX idx_profiles_email (email)');
} catch (PDOException $e) { /* already exists, ignore */ }
// Migration: add activation_code column for databases created before it was introduced
try {
    $db->exec("ALTER TABLE profiles ADD COLUMN activation_code VARCHAR(10) NOT NULL DEFAULT ''");
} catch (PDOException $e) { /* already exists, ignore */ }

$db->exec('CREATE TABLE IF NOT EXISTS shops (
    id               VARCHAR(36)  NOT NULL,
    profile_id       VARCHAR(36)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    background_color VARCHAR(10)  NOT NULL DEFAULT \'#42A5F5\',
    foreground_color VARCHAR(10)  NOT NULL DEFAULT \'#FFFFFF\',
    order_index      INT          NOT NULL DEFAULT 0,
    updated_at       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_shops_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

$db->exec('CREATE TABLE IF NOT EXISTS catalog_items (
    id         VARCHAR(36)  NOT NULL,
    profile_id VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    category   VARCHAR(50)  NOT NULL DEFAULT \'\',
    updated_at BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_catalog_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

$db->exec('CREATE TABLE IF NOT EXISTS list_members (
    list_id    VARCHAR(36) NOT NULL,
    profile_id VARCHAR(36) NOT NULL,
    joined_at  BIGINT      NOT NULL,
    PRIMARY KEY (list_id, profile_id),
    FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
    INDEX idx_list_members_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

// ── Helpers ────────────────────────────────────────────────────────────────

function nowMs(): int
{
    return (int) (microtime(true) * 1000);
}

function rowToItem(array $row): array
{
    return [
        'id'          => $row['id'],
        'name'        => $row['name'],
        'quantity'    => $row['quantity'],
        'category'    => $row['category'],
        'isChecked'   => (bool) $row['is_checked'],
        'checkedAt'   => isset($row['checked_at']) ? (int) $row['checked_at'] : null,
        'weekday'     => $row['weekday'],
        'price'       => $row['price'],
        'supermarket' => $row['supermarket'],
        'comment'     => $row['comment'] ?? null,
    ];
}

function listWithItems(PDO $db, array $list): array
{
    $stmt = $db->prepare('SELECT * FROM items WHERE list_id = ? ORDER BY updated_at ASC, id ASC');
    $stmt->execute([$list['id']]);
    return [
        'id'        => $list['id'],
        'name'      => $list['name'],
        'ownerId'   => $list['owner_id'] ?? '',
        'createdAt' => (int) $list['created_at'],
        'items'     => array_map('rowToItem', $stmt->fetchAll()),
    ];
}

function json_out(mixed $data, int $status = 200): never
{
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function not_found(): never
{
    json_out(['error' => 'Not found'], 404);
}

// ── Router ─────────────────────────────────────────────────────────────────

$method = $_SERVER['REQUEST_METHOD'];

$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/');
$uri       = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$path      = ltrim(substr($uri, strlen($scriptDir)), '/');
$segments  = array_values(array_filter(explode('/', $path)));

$body = json_decode(file_get_contents('php://input'), true) ?? [];

// ── GET /lists ─────────────────────────────────────────────────────────────
if ($method === 'GET' && $segments === ['lists']) {
    $profileId = $_GET['profileId'] ?? '';
    if ($profileId !== '') {
        $stmt = $db->prepare('
            SELECT l.* FROM lists l
            INNER JOIN list_members m ON m.list_id = l.id
            WHERE m.profile_id = ?
            ORDER BY l.created_at DESC
        ');
        $stmt->execute([$profileId]);
    } else {
        $stmt = $db->query('SELECT * FROM lists ORDER BY created_at DESC');
    }
    $rows = $stmt->fetchAll();
    json_out(array_map(fn($r) => listWithItems($db, $r), $rows));
}

// ── POST /lists ────────────────────────────────────────────────────────────
if ($method === 'POST' && $segments === ['lists']) {
    $now     = nowMs();
    $id      = $body['id'] ?? bin2hex(random_bytes(16));
    $ownerId = $body['ownerId'] ?? '';
    $db->prepare('
        INSERT INTO lists (id, name, owner_id, created_at, updated_at) VALUES (?,?,?,?,?)
        ON DUPLICATE KEY UPDATE name = VALUES(name), owner_id = VALUES(owner_id), updated_at = VALUES(updated_at)
    ')->execute([$id, $body['name'], $ownerId, $body['createdAt'] ?? $now, $now]);
    // Register the creator as a member so the list appears in their filtered view
    if ($ownerId !== '') {
        $db->prepare('
            INSERT IGNORE INTO list_members (list_id, profile_id, joined_at) VALUES (?,?,?)
        ')->execute([$id, $ownerId, $now]);
    }
    json_out(['id' => $id, 'name' => $body['name'], 'ownerId' => $ownerId, 'createdAt' => $body['createdAt'] ?? $now, 'items' => []], 201);
}

// ── POST /lists/{id}/members ───────────────────────────────────────────────
if ($method === 'POST' && count($segments) === 3 && $segments[0] === 'lists' && $segments[2] === 'members') {
    $listId    = $segments[1];
    $profileId = $body['profileId'] ?? '';
    if ($profileId === '') json_out(['error' => 'Missing profileId'], 400);
    $check = $db->prepare('SELECT id FROM lists WHERE id = ?');
    $check->execute([$listId]);
    if (!$check->fetch()) not_found();
    $db->prepare('
        INSERT IGNORE INTO list_members (list_id, profile_id, joined_at) VALUES (?,?,?)
    ')->execute([$listId, $profileId, nowMs()]);
    json_out(['listId' => $listId, 'profileId' => $profileId], 201);
}

// ── DELETE /lists/{id}/members/{profileId} ────────────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'lists' && $segments[2] === 'members') {
    $listId    = $segments[1];
    $profileId = $segments[3];
    $db->prepare('DELETE FROM list_members WHERE list_id = ? AND profile_id = ?')
       ->execute([$listId, $profileId]);
    json_out(['listId' => $listId, 'profileId' => $profileId]);
}

// ── GET /lists/{id} ────────────────────────────────────────────────────────
if ($method === 'GET' && count($segments) === 2 && $segments[0] === 'lists') {
    $stmt = $db->prepare('SELECT * FROM lists WHERE id = ?');
    $stmt->execute([$segments[1]]);
    $row = $stmt->fetch();
    if (!$row) not_found();
    json_out(listWithItems($db, $row));
}

// ── PATCH /lists/{id} ─────────────────────────────────────────────────────
if ($method === 'PATCH' && count($segments) === 2 && $segments[0] === 'lists') {
    $db->prepare('UPDATE lists SET name = ?, updated_at = ? WHERE id = ?')
       ->execute([$body['name'], nowMs(), $segments[1]]);
    json_out(['id' => $segments[1], 'name' => $body['name']]);
}

// ── DELETE /lists/{id} ────────────────────────────────────────────────────
if ($method === 'DELETE' && count($segments) === 2 && $segments[0] === 'lists') {
    $db->prepare('DELETE FROM lists WHERE id = ?')->execute([$segments[1]]);
    http_response_code(204);
    exit;
}

// ── PUT /lists/{listId}/items/{itemId} ────────────────────────────────────
if ($method === 'PUT'
    && count($segments) === 4
    && $segments[0] === 'lists'
    && $segments[2] === 'items'
) {
    [, $listId, , $itemId] = $segments;
    $now = nowMs();

    // Verify the parent list exists
    $check = $db->prepare('SELECT id FROM lists WHERE id = ?');
    $check->execute([$listId]);
    if (!$check->fetch()) not_found();

    // Upsert the item (compatible with MySQL 5.7 and 8.x)
    $db->prepare('
        INSERT INTO items
            (id, list_id, name, quantity, category, is_checked, checked_at, weekday, price, supermarket, comment, updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
            name        = VALUES(name),
            quantity    = VALUES(quantity),
            category    = VALUES(category),
            is_checked  = VALUES(is_checked),
            checked_at  = VALUES(checked_at),
            weekday     = VALUES(weekday),
            price       = VALUES(price),
            supermarket = VALUES(supermarket),
            comment     = VALUES(comment),
            updated_at  = VALUES(updated_at)
    ')->execute([
        $itemId,
        $listId,
        $body['name'],
        $body['quantity'],
        $body['category'],
        !empty($body['isChecked']) ? 1 : 0,
        $body['checkedAt']   ?? null,
        $body['weekday']     ?? null,
        $body['price']       ?? null,
        $body['supermarket'] ?? null,
        $body['comment']     ?? null,
        $now,
    ]);

    // Bump the list's updated_at so polling clients notice a change
    $db->prepare('UPDATE lists SET updated_at = ? WHERE id = ?')->execute([$now, $listId]);

    json_out(rowToItem([
        'id'          => $itemId,
        'name'        => $body['name'],
        'quantity'    => $body['quantity'],
        'category'    => $body['category'],
        'is_checked'  => !empty($body['isChecked']) ? 1 : 0,
        'checked_at'  => $body['checkedAt']   ?? null,
        'weekday'     => $body['weekday']     ?? null,
        'price'       => $body['price']       ?? null,
        'supermarket' => $body['supermarket'] ?? null,
        'comment'     => $body['comment']     ?? null,
    ]));
}

// ── DELETE /lists/{listId}/items/{itemId} ─────────────────────────────────
if ($method === 'DELETE'
    && count($segments) === 4
    && $segments[0] === 'lists'
    && $segments[2] === 'items'
) {
    [, $listId, , $itemId] = $segments;
    $db->prepare('DELETE FROM items WHERE id = ? AND list_id = ?')->execute([$itemId, $listId]);
    $db->prepare('UPDATE lists SET updated_at = ? WHERE id = ?')->execute([nowMs(), $listId]);
    http_response_code(204);
    exit;
}

// ── GET /profile/{profileId}/categories ───────────────────────────────────
if ($method === 'GET' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'categories') {
    $stmt = $db->prepare('SELECT * FROM categories WHERE profile_id = ? ORDER BY order_index ASC, id ASC');
    $stmt->execute([$segments[1]]);
    json_out(array_map(fn($r) => [
        'id' => $r['id'], 'profileId' => $r['profile_id'],
        'name' => $r['name'], 'orderIndex' => (int) $r['order_index'],
    ], $stmt->fetchAll()));
}

// ── POST /profile/{profileId}/categories ──────────────────────────────────
if ($method === 'POST' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'categories') {
    $profileId = $segments[1];
    $id  = $body['id'] ?? bin2hex(random_bytes(16));
    $now = nowMs();
    // Place at end of list
    $max = $db->prepare('SELECT COALESCE(MAX(order_index), -1) FROM categories WHERE profile_id = ?');
    $max->execute([$profileId]);
    $orderIndex = (int) $max->fetchColumn() + 1;
    $db->prepare('INSERT INTO categories (id, profile_id, name, order_index, updated_at) VALUES (?,?,?,?,?)')
       ->execute([$id, $profileId, $body['name'] ?? '', $orderIndex, $now]);
    json_out(['id' => $id, 'profileId' => $profileId, 'name' => $body['name'] ?? '', 'orderIndex' => $orderIndex], 201);
}

// ── PUT /profile/{profileId}/categories/{id} ──────────────────────────────
if ($method === 'PUT' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'categories') {
    [, $profileId, , $id] = $segments;
    $now = nowMs();
    $db->prepare('UPDATE categories SET name = ?, order_index = ?, updated_at = ? WHERE id = ? AND profile_id = ?')
       ->execute([$body['name'] ?? '', $body['orderIndex'] ?? 0, $now, $id, $profileId]);
    json_out(['id' => $id, 'profileId' => $profileId, 'name' => $body['name'] ?? '', 'orderIndex' => $body['orderIndex'] ?? 0]);
}

// ── DELETE /profile/{profileId}/categories/{id} ───────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'categories') {
    [, $profileId, , $id] = $segments;
    $db->prepare('DELETE FROM categories WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
    http_response_code(204);
    exit;
}

// ── POST /profile/{profileId}/categories/reorder ──────────────────────────
if ($method === 'POST' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'categories' && $segments[3] === 'reorder') {
    $profileId = $segments[1];
    $ids = $body['ids'] ?? [];
    $stmt = $db->prepare('UPDATE categories SET order_index = ?, updated_at = ? WHERE id = ? AND profile_id = ?');
    $now = nowMs();
    foreach ($ids as $i => $id) {
        $stmt->execute([$i, $now, $id, $profileId]);
    }
    json_out(['ok' => true]);
}

// ── GET /profile/{profileId}/shops ────────────────────────────────────────
if ($method === 'GET' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'shops') {
    $stmt = $db->prepare('SELECT * FROM shops WHERE profile_id = ? ORDER BY order_index ASC, id ASC');
    $stmt->execute([$segments[1]]);
    json_out(array_map(fn($r) => [
        'id'              => $r['id'],
        'profileId'       => $r['profile_id'],
        'name'            => $r['name'],
        'backgroundColor' => $r['background_color'],
        'foregroundColor' => $r['foreground_color'],
        'orderIndex'      => (int) $r['order_index'],
    ], $stmt->fetchAll()));
}

// ── POST /profile/{profileId}/shops ───────────────────────────────────────
if ($method === 'POST' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'shops') {
    $profileId = $segments[1];
    $id  = $body['id'] ?? bin2hex(random_bytes(16));
    $now = nowMs();
    $max = $db->prepare('SELECT COALESCE(MAX(order_index), -1) FROM shops WHERE profile_id = ?');
    $max->execute([$profileId]);
    $orderIndex = (int) $max->fetchColumn() + 1;
    $db->prepare('INSERT INTO shops (id, profile_id, name, background_color, foreground_color, order_index, updated_at) VALUES (?,?,?,?,?,?,?)')
       ->execute([$id, $profileId, $body['name'] ?? '', $body['backgroundColor'] ?? '#42A5F5', $body['foregroundColor'] ?? '#FFFFFF', $orderIndex, $now]);
    json_out(['id' => $id, 'profileId' => $profileId, 'name' => $body['name'] ?? '', 'backgroundColor' => $body['backgroundColor'] ?? '#42A5F5', 'foregroundColor' => $body['foregroundColor'] ?? '#FFFFFF', 'orderIndex' => $orderIndex], 201);
}

// ── PUT /profile/{profileId}/shops/{id} ───────────────────────────────────
if ($method === 'PUT' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'shops') {
    [, $profileId, , $id] = $segments;
    $now = nowMs();
    $db->prepare('UPDATE shops SET name = ?, background_color = ?, foreground_color = ?, order_index = ?, updated_at = ? WHERE id = ? AND profile_id = ?')
       ->execute([$body['name'] ?? '', $body['backgroundColor'] ?? '#42A5F5', $body['foregroundColor'] ?? '#FFFFFF', $body['orderIndex'] ?? 0, $now, $id, $profileId]);
    json_out(['id' => $id, 'profileId' => $profileId, 'name' => $body['name'] ?? '', 'backgroundColor' => $body['backgroundColor'] ?? '#42A5F5', 'foregroundColor' => $body['foregroundColor'] ?? '#FFFFFF', 'orderIndex' => $body['orderIndex'] ?? 0]);
}

// ── DELETE /profile/{profileId}/shops/{id} ────────────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'shops') {
    [, $profileId, , $id] = $segments;
    $db->prepare('DELETE FROM shops WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
    http_response_code(204);
    exit;
}

// ── POST /profile/{profileId}/shops/reorder ───────────────────────────────
if ($method === 'POST' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'shops' && $segments[3] === 'reorder') {
    $profileId = $segments[1];
    $ids  = $body['ids'] ?? [];
    $stmt = $db->prepare('UPDATE shops SET order_index = ?, updated_at = ? WHERE id = ? AND profile_id = ?');
    $now  = nowMs();
    foreach ($ids as $i => $id) {
        $stmt->execute([$i, $now, $id, $profileId]);
    }
    json_out(['ok' => true]);
}

// ── GET /profile/{profileId}/catalog ──────────────────────────────────────
if ($method === 'GET' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'catalog') {
    $stmt = $db->prepare('SELECT * FROM catalog_items WHERE profile_id = ? ORDER BY name ASC');
    $stmt->execute([$segments[1]]);
    json_out(array_map(fn($r) => [
        'id'        => $r['id'],
        'profileId' => $r['profile_id'],
        'name'      => $r['name'],
        'category'  => $r['category'],
    ], $stmt->fetchAll()));
}

// ── PUT /profile/{profileId}/catalog/{id} ─────────────────────────────────
if ($method === 'PUT' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'catalog') {
    [, $profileId, , $id] = $segments;
    $now = nowMs();
    $db->prepare('
        INSERT INTO catalog_items (id, profile_id, name, category, updated_at) VALUES (?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
            name       = VALUES(name),
            category   = VALUES(category),
            updated_at = VALUES(updated_at)
    ')->execute([$id, $profileId, $body['name'] ?? '', $body['category'] ?? '', $now]);
    json_out(['id' => $id, 'profileId' => $profileId, 'name' => $body['name'] ?? '', 'category' => $body['category'] ?? '']);
}

// ── DELETE /profile/{profileId}/catalog/{id} ──────────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'catalog') {
    [, $profileId, , $id] = $segments;
    $db->prepare('DELETE FROM catalog_items WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
    http_response_code(204);
    exit;
}

// ── GET /profile/by-email?email=... ───────────────────────────────────────
if ($method === 'GET' && count($segments) === 2 && $segments[0] === 'profile' && $segments[1] === 'by-email') {
    $email = trim($_GET['email'] ?? '');
    if ($email === '') json_out(['error' => 'Missing email'], 400);
    $stmt = $db->prepare('SELECT * FROM profiles WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $row = $stmt->fetch();
    if (!$row) not_found();
    json_out(['id' => $row['id'], 'name' => $row['name'], 'email' => $row['email'], 'activationCode' => $row['activation_code']]);
}

// ── GET /profile/{id} ─────────────────────────────────────────────────────
if ($method === 'GET' && count($segments) === 2 && $segments[0] === 'profile') {
    $stmt = $db->prepare('SELECT * FROM profiles WHERE id = ?');
    $stmt->execute([$segments[1]]);
    $row = $stmt->fetch();
    if (!$row) {
        json_out(['id' => $segments[1], 'name' => '', 'email' => '', 'activationCode' => '']);
    }
    json_out(['id' => $row['id'], 'name' => $row['name'], 'email' => $row['email'], 'activationCode' => $row['activation_code']]);
}

// ── PUT /profile/{id} ─────────────────────────────────────────────────────
if ($method === 'PUT' && count($segments) === 2 && $segments[0] === 'profile') {
    $id             = $segments[1];
    $name           = $body['name']           ?? '';
    $email          = $body['email']          ?? '';
    $activationCode = $body['activationCode'] ?? '';
    $now            = nowMs();
    $db->prepare('
        INSERT INTO profiles (id, name, email, activation_code, updated_at) VALUES (?,?,?,?,?)
        ON DUPLICATE KEY UPDATE name = VALUES(name), email = VALUES(email), activation_code = VALUES(activation_code), updated_at = VALUES(updated_at)
    ')->execute([$id, $name, $email, $activationCode, $now]);
    json_out(['id' => $id, 'name' => $name, 'email' => $email, 'activationCode' => $activationCode]);
}

not_found();
