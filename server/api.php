<?php
/**
 * Shopping List REST API — MySQL edition
 * Place this file (and .htaccess) inside a directory on your PHP server,
 * e.g. /var/www/html/shopping/api/
 *
 * Endpoints
 * ---------
 * GET    /lists                                     List all lists (with items)
 * POST   /lists                                     Create a list
 * GET    /lists/{id}                                Get one list with items
 * PATCH  /lists/{id}                                Rename a list
 * DELETE /lists/{id}                                Delete list + items
 * PUT    /lists/{id}/items/{itemId}                 Create or update an item
 * DELETE /lists/{id}/items/{itemId}                 Delete an item
 *
 * GET    /profile/{profileId}/recipes               List all recipes
 * PUT    /profile/{profileId}/recipes/{id}          Create or update a recipe
 * DELETE /profile/{profileId}/recipes/{id}          Delete a recipe
 *
 * Recipe images are stored as JPEG files under recipe_images/{recipeId}.jpg;
 * the recipes row holds image_url (relative path). GET JSON includes absolute imageUrl.
 * GET    /profile/{profileId}/menu-plans            List all menu plans (with recipeIds)
 * PUT    /profile/{profileId}/menu-plans/{id}       Create or update a menu plan
 * DELETE /profile/{profileId}/menu-plans/{id}       Delete a menu plan
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

$db->exec('CREATE TABLE IF NOT EXISTS recipes (
    id                   VARCHAR(36)  NOT NULL,
    profile_id           VARCHAR(36)  NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT         NOT NULL,
    rating               INT          NOT NULL DEFAULT 0,
    servings             INT          NOT NULL DEFAULT 0,
    nutrition_facts      TEXT         NOT NULL,
    prep_time_minutes    INT          NOT NULL DEFAULT 0,
    total_time_minutes   INT          NOT NULL DEFAULT 0,
    durability           VARCHAR(255) NOT NULL DEFAULT \'\',
    course_type          VARCHAR(100) NOT NULL DEFAULT \'\',
    ingredient_sections  TEXT         NOT NULL,
    instruction_sections TEXT         NOT NULL,
    tips                 TEXT         NOT NULL,
    image_url            VARCHAR(768) NULL,
    created_at           BIGINT       NOT NULL,
    updated_at           BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_recipes_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

// Migration: add new recipe columns for databases created before they existed
foreach ([
    "ALTER TABLE recipes ADD COLUMN rating INT NOT NULL DEFAULT 0",
    "ALTER TABLE recipes ADD COLUMN servings INT NOT NULL DEFAULT 0",
    "ALTER TABLE recipes ADD COLUMN nutrition_facts TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE recipes ADD COLUMN prep_time_minutes INT NOT NULL DEFAULT 0",
    "ALTER TABLE recipes ADD COLUMN total_time_minutes INT NOT NULL DEFAULT 0",
    "ALTER TABLE recipes ADD COLUMN durability VARCHAR(255) NOT NULL DEFAULT ''",
    "ALTER TABLE recipes ADD COLUMN course_type VARCHAR(100) NOT NULL DEFAULT ''",
    "ALTER TABLE recipes ADD COLUMN ingredient_sections TEXT NOT NULL DEFAULT '[]'",
    "ALTER TABLE recipes ADD COLUMN instruction_sections TEXT NOT NULL DEFAULT '[]'",
    "ALTER TABLE recipes ADD COLUMN tips TEXT NOT NULL DEFAULT ''",
] as $sql) {
    try { $db->exec($sql); } catch (PDOException $e) { /* already exists */ }
}

try { $db->exec('ALTER TABLE recipes ADD COLUMN image_jpeg MEDIUMBLOB NULL'); } catch (PDOException $e) { /* already exists */ }
try { $db->exec('ALTER TABLE recipes ADD COLUMN image_url VARCHAR(768) NULL'); } catch (PDOException $e) { /* already exists */ }

// ── Recipe images (files on disk) ─────────────────────────────────────────
define('RECIPE_IMAGES_SUBDIR', 'recipe_images');

function recipe_images_fs_dir(): string
{
    return __DIR__ . '/' . RECIPE_IMAGES_SUBDIR;
}

function ensure_recipe_images_dir(): void
{
    $d = recipe_images_fs_dir();
    if (!is_dir($d)) {
        mkdir($d, 0755, true);
    }
}

/** Web path prefix containing api.php (e.g. /shopping). */
function public_api_dir_url_prefix(): string
{
    $dir = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '/')), '/');
    if ($dir === '' || $dir === '/') {
        return '';
    }
    return $dir;
}

function absolute_recipe_image_url(?string $relativePath): ?string
{
    if ($relativePath === null || $relativePath === '') {
        return null;
    }
    if (preg_match('#^https?://#i', $relativePath)) {
        return $relativePath;
    }
    $https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (!empty($_SERVER['HTTP_X_FORWARDED_PROTO']) && $_SERVER['HTTP_X_FORWARDED_PROTO'] === 'https');
    $proto = $https ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $base = public_api_dir_url_prefix();

    return $proto . '://' . $host . $base . '/' . ltrim(str_replace('\\', '/', $relativePath), '/');
}

/** Delete stored file for a recipe if it exists; clear DB column optional (caller updates row). */
function delete_recipe_image_file(PDO $db, string $recipeId, string $profileId): void
{
    $stmt = $db->prepare('SELECT image_url FROM recipes WHERE id = ? AND profile_id = ?');
    $stmt->execute([$recipeId, $profileId]);
    $row = $stmt->fetch();
    if (!$row || empty($row['image_url'])) {
        return;
    }
    $rel = $row['image_url'];
    if (preg_match('#^https?://#i', $rel)) {
        return;
    }
    $full = __DIR__ . '/' . ltrim(str_replace('\\', '/', $rel), '/');
    if (is_file($full)) {
        @unlink($full);
    }
}

function migrate_legacy_recipe_blobs_to_files(PDO $db): void
{
    try {
        $stmt = $db->query('SELECT id, image_jpeg FROM recipes WHERE image_jpeg IS NOT NULL AND LENGTH(image_jpeg) > 0');
        $rows = $stmt ? $stmt->fetchAll() : [];
    } catch (PDOException $e) {
        return;
    }
    ensure_recipe_images_dir();
    foreach ($rows as $r) {
        $id = $r['id'];
        $rel = RECIPE_IMAGES_SUBDIR . '/' . $id . '.jpg';
        $full = __DIR__ . '/' . $rel;
        if (@file_put_contents($full, $r['image_jpeg']) !== false) {
            $db->prepare('UPDATE recipes SET image_url = ?, image_jpeg = NULL WHERE id = ?')->execute([$rel, $id]);
        }
    }
    try {
        $db->exec('ALTER TABLE recipes DROP COLUMN image_jpeg');
    } catch (PDOException $e) { /* already dropped */ }
}

ensure_recipe_images_dir();
migrate_legacy_recipe_blobs_to_files($db);

$db->exec('CREATE TABLE IF NOT EXISTS menu_plans (
    id          VARCHAR(36)  NOT NULL,
    profile_id  VARCHAR(36)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    servings    INT          NOT NULL DEFAULT 0,
    created_at  BIGINT       NOT NULL,
    updated_at  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_menu_plans_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

try { $db->exec('ALTER TABLE menu_plans ADD COLUMN recipe_progress TEXT NOT NULL DEFAULT \'{}\''); } catch (PDOException $e) { /* already exists */ }
try { $db->exec('ALTER TABLE menu_plans ADD COLUMN servings INT NOT NULL DEFAULT 0'); } catch (PDOException $e) { /* already exists */ }

$db->exec('CREATE TABLE IF NOT EXISTS menu_plan_recipes (
    menu_plan_id VARCHAR(36) NOT NULL,
    recipe_id    VARCHAR(36) NOT NULL,
    sort_order   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (menu_plan_id, recipe_id),
    FOREIGN KEY (menu_plan_id) REFERENCES menu_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
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

function menuPlanToJson(PDO $db, array $row): array
{
    $stmt = $db->prepare('SELECT recipe_id FROM menu_plan_recipes WHERE menu_plan_id = ? ORDER BY sort_order ASC');
    $stmt->execute([$row['id']]);
    $progress = json_decode($row['recipe_progress'] ?? '{}', true) ?: [];
    return [
        'id'             => $row['id'],
        'profileId'      => $row['profile_id'],
        'name'           => $row['name'],
        'description'    => $row['description'] ?? '',
        'servings'       => (int) ($row['servings'] ?? 0),
        'recipeIds'      => $stmt->fetchAll(PDO::FETCH_COLUMN),
        'recipeProgress' => (object) $progress,
        'createdAt'      => (int) $row['created_at'],
    ];
}

function recipeToJson(array $row): array
{
    $out = [
        'id'                  => $row['id'],
        'profileId'           => $row['profile_id'],
        'name'                => $row['name'],
        'description'         => $row['description'] ?? '',
        'rating'              => (int) ($row['rating'] ?? 0),
        'servings'            => (int) ($row['servings'] ?? 0),
        'nutritionFacts'      => $row['nutrition_facts'] ?? '',
        'prepTimeMinutes'     => (int) ($row['prep_time_minutes'] ?? 0),
        'totalTimeMinutes'    => (int) ($row['total_time_minutes'] ?? 0),
        'durability'          => $row['durability'] ?? '',
        'courseType'           => $row['course_type'] ?? '',
        'ingredientSections'  => json_decode($row['ingredient_sections'] ?? '[]', true) ?: [],
        'instructionSections' => json_decode($row['instruction_sections'] ?? '[]', true) ?: [],
        'tips'                => $row['tips'] ?? '',
        'createdAt'           => (int) $row['created_at'],
    ];
    $abs = absolute_recipe_image_url($row['image_url'] ?? null);
    if ($abs !== null && $abs !== '') {
        $out['imageUrl'] = $abs;
    }
    return $out;
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

// ── GET /profile/{profileId}/recipes ───────────────────────────────────────
if ($method === 'GET' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'recipes') {
    $stmt = $db->prepare('SELECT * FROM recipes WHERE profile_id = ? ORDER BY created_at DESC');
    $stmt->execute([$segments[1]]);
    json_out(array_map('recipeToJson', $stmt->fetchAll()));
}

// ── PUT /profile/{profileId}/recipes/{id} ─────────────────────────────────
if ($method === 'PUT' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'recipes') {
    [, $profileId, , $id] = $segments;
    $now = nowMs();
    $db->prepare('
        INSERT INTO recipes (id, profile_id, name, description, rating, servings, nutrition_facts,
            prep_time_minutes, total_time_minutes, durability, course_type,
            ingredient_sections, instruction_sections, tips, created_at, updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
            name                 = VALUES(name),
            description          = VALUES(description),
            rating               = VALUES(rating),
            servings             = VALUES(servings),
            nutrition_facts      = VALUES(nutrition_facts),
            prep_time_minutes    = VALUES(prep_time_minutes),
            total_time_minutes   = VALUES(total_time_minutes),
            durability           = VALUES(durability),
            course_type          = VALUES(course_type),
            ingredient_sections  = VALUES(ingredient_sections),
            instruction_sections = VALUES(instruction_sections),
            tips                 = VALUES(tips),
            updated_at           = VALUES(updated_at)
    ')->execute([
        $id,
        $profileId,
        $body['name'] ?? '',
        $body['description'] ?? '',
        (int) ($body['rating'] ?? 0),
        (int) ($body['servings'] ?? 0),
        $body['nutritionFacts'] ?? '',
        (int) ($body['prepTimeMinutes'] ?? 0),
        (int) ($body['totalTimeMinutes'] ?? 0),
        $body['durability'] ?? '',
        $body['courseType'] ?? '',
        json_encode($body['ingredientSections'] ?? [], JSON_UNESCAPED_UNICODE),
        json_encode($body['instructionSections'] ?? [], JSON_UNESCAPED_UNICODE),
        $body['tips'] ?? '',
        $body['createdAt'] ?? $now,
        $now,
    ]);
    if (array_key_exists('imageBase64', $body)) {
        ensure_recipe_images_dir();
        $raw = $body['imageBase64'];
        if ($raw === null || $raw === '') {
            delete_recipe_image_file($db, $id, $profileId);
            $db->prepare('UPDATE recipes SET image_url = NULL WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
        } else {
            $bin = base64_decode((string) $raw, true);
            if ($bin === false) {
                json_out(['error' => 'invalid imageBase64'], 400);
            }
            $rel = RECIPE_IMAGES_SUBDIR . '/' . $id . '.jpg';
            $full = __DIR__ . '/' . $rel;
            if (file_put_contents($full, $bin) === false) {
                json_out(['error' => 'failed to store recipe image'], 500);
            }
            $db->prepare('UPDATE recipes SET image_url = ? WHERE id = ? AND profile_id = ?')->execute([$rel, $id, $profileId]);
        }
    }
    $stmt = $db->prepare('SELECT * FROM recipes WHERE id = ?');
    $stmt->execute([$id]);
    json_out(recipeToJson($stmt->fetch()));
}

// ── DELETE /profile/{profileId}/recipes/{id} ──────────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'recipes') {
    [, $profileId, , $id] = $segments;
    delete_recipe_image_file($db, $id, $profileId);
    $db->prepare('DELETE FROM recipes WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
    http_response_code(204);
    exit;
}

// ── GET /profile/{profileId}/menu-plans ───────────────────────────────────
if ($method === 'GET' && count($segments) === 3 && $segments[0] === 'profile' && $segments[2] === 'menu-plans') {
    $stmt = $db->prepare('SELECT * FROM menu_plans WHERE profile_id = ? ORDER BY created_at DESC');
    $stmt->execute([$segments[1]]);
    json_out(array_map(fn($r) => menuPlanToJson($db, $r), $stmt->fetchAll()));
}

// ── PUT /profile/{profileId}/menu-plans/{id} ──────────────────────────────
if ($method === 'PUT' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'menu-plans') {
    [, $profileId, , $id] = $segments;
    $now = nowMs();
    $recipeProgress = json_encode($body['recipeProgress'] ?? new \stdClass());
    $db->prepare('
        INSERT INTO menu_plans (id, profile_id, name, description, servings, recipe_progress, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
            name             = VALUES(name),
            description      = VALUES(description),
            servings         = VALUES(servings),
            recipe_progress  = VALUES(recipe_progress),
            updated_at       = VALUES(updated_at)
    ')->execute([$id, $profileId, $body['name'] ?? '', $body['description'] ?? '', (int) ($body['servings'] ?? 0), $recipeProgress, $body['createdAt'] ?? $now, $now]);

    $db->prepare('DELETE FROM menu_plan_recipes WHERE menu_plan_id = ?')->execute([$id]);
    $recipeIds = $body['recipeIds'] ?? [];
    $insertStmt = $db->prepare('INSERT INTO menu_plan_recipes (menu_plan_id, recipe_id, sort_order) VALUES (?,?,?)');
    foreach ($recipeIds as $i => $recipeId) {
        $insertStmt->execute([$id, $recipeId, $i]);
    }

    $stmt = $db->prepare('SELECT * FROM menu_plans WHERE id = ?');
    $stmt->execute([$id]);
    json_out(menuPlanToJson($db, $stmt->fetch()));
}

// ── DELETE /profile/{profileId}/menu-plans/{id} ───────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'menu-plans') {
    [, $profileId, , $id] = $segments;
    $db->prepare('DELETE FROM menu_plans WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
    http_response_code(204);
    exit;
}

not_found();
