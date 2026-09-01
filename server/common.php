<?php
/**
 * Shared database setup and helpers for api.php and the recipe web portal.
 */

if (!defined('DB_HOST')) {
    define('DB_HOST', 'localhost');
    define('DB_NAME', 'joachimd_shopping');
    define('DB_USER', 'joachimd_shopping');
}

if (!defined('MAIL_FROM_ADDRESS')) {
    define('MAIL_FROM_ADDRESS', 'noreply@joachim.dk');
    define('MAIL_FROM_NAME',    'ShoppingShark');
    define('MAIL_REPLY_TO',     'joachim@joachim.dk');
    define('MAIL_SMTP_HOST',     'mail.joachim.dk');
    define('MAIL_SMTP_PORT',     465);
    define('MAIL_SMTP_USER',     'noreply@joachim.dk');
    define('MAIL_SMTP_SECURE',   'ssl');
}

require_once __DIR__ . '/secrets.php';

/** @var PDO $db */
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

$db->exec('CREATE TABLE IF NOT EXISTS lists (
    id               VARCHAR(36)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    owner_id         VARCHAR(36)  NOT NULL DEFAULT \'\',
    share_enabled    TINYINT(1)   NOT NULL DEFAULT 0,
    share_token      VARCHAR(64)  NULL,
    share_token_hash CHAR(64)     NULL,
    created_at       BIGINT       NOT NULL,
    updated_at       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_lists_share_token (share_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

try {
    $db->exec('ALTER TABLE lists ADD COLUMN share_enabled TINYINT(1) NOT NULL DEFAULT 0');
} catch (PDOException $e) {
    // Column already exists.
}
try {
    $db->exec('ALTER TABLE lists ADD COLUMN share_token_hash CHAR(64) NULL');
} catch (PDOException $e) {
    // Column already exists.
}
try {
    $db->exec('ALTER TABLE lists ADD COLUMN share_token VARCHAR(64) NULL');
} catch (PDOException $e) {
    // Column already exists.
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
    linked_recipe_ids    TEXT         NOT NULL DEFAULT \'[]\',
    is_pinned            TINYINT(1)   NOT NULL DEFAULT 0,
    pinned_at            BIGINT       NULL,
    created_at           BIGINT       NOT NULL,
    updated_at           BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_recipes_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

try {
    $db->exec('ALTER TABLE recipes ADD COLUMN is_pinned TINYINT(1) NOT NULL DEFAULT 0');
} catch (PDOException $e) {
    // Column already exists.
}
try {
    $db->exec('ALTER TABLE recipes ADD COLUMN pinned_at BIGINT NULL');
} catch (PDOException $e) {
    // Column already exists.
}

$db->exec('CREATE TABLE IF NOT EXISTS web_login_requests (
    id           VARCHAR(36)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    token_hash   CHAR(64)     NOT NULL,
    next_url     VARCHAR(255) NOT NULL DEFAULT \'recipes.php\',
    remember_device TINYINT(1) NOT NULL DEFAULT 0,
    completed_at BIGINT       NULL,
    expires_at   BIGINT       NOT NULL,
    created_at   BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_web_login_token (token_hash),
    INDEX idx_web_login_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

try {
    $db->exec('ALTER TABLE web_login_requests ADD COLUMN remember_device TINYINT(1) NOT NULL DEFAULT 0');
} catch (PDOException $e) {
    // Column already exists.
}

$db->exec('CREATE TABLE IF NOT EXISTS web_device_tokens (
    id           VARCHAR(36)  NOT NULL,
    profile_id   VARCHAR(36)  NOT NULL,
    token_hash   CHAR(64)     NOT NULL,
    expires_at   BIGINT       NOT NULL,
    created_at   BIGINT       NOT NULL,
    last_used_at BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_web_device_token_hash (token_hash),
    INDEX idx_web_device_profile (profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

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

/** Web path prefix for the API directory (e.g. /shopping). */
function public_api_dir_url_prefix(): string
{
    $dir = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '/')), '/');
    if (preg_match('#/web$#', $dir)) {
        $dir = dirname($dir);
        $dir = $dir === '/' ? '' : $dir;
    }
    if ($dir === '' || $dir === '/') {
        return '';
    }
    return $dir;
}

/** Absolute base URL for the recipe web portal (trailing slash). */
function web_portal_base_url(): string
{
    if (defined('WEB_PORTAL_BASE_URL') && WEB_PORTAL_BASE_URL !== '') {
        return rtrim(WEB_PORTAL_BASE_URL, '/') . '/';
    }
    $https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (!empty($_SERVER['HTTP_X_FORWARDED_PROTO']) && $_SERVER['HTTP_X_FORWARDED_PROTO'] === 'https');
    $proto = $https ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $prefix = public_api_dir_url_prefix();

    return $proto . '://' . $host . $prefix . '/web/';
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

$db->exec('CREATE TABLE IF NOT EXISTS menu_plan_recipes (
    menu_plan_id VARCHAR(36) NOT NULL,
    recipe_id    VARCHAR(36) NOT NULL,
    sort_order   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (menu_plan_id, recipe_id),
    FOREIGN KEY (menu_plan_id) REFERENCES menu_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');

function nowMs(): int
{
    return (int) (microtime(true) * 1000);
}

function newId(): string
{
    return bin2hex(random_bytes(16));
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
        'id'           => $list['id'],
        'name'         => $list['name'],
        'ownerId'      => $list['owner_id'] ?? '',
        'shareEnabled' => !empty($list['share_enabled']),
        'createdAt'    => (int) $list['created_at'],
        'items'        => array_map('rowToItem', $stmt->fetchAll()),
    ];
}

function list_accessible_by_profile(PDO $db, string $listId, string $profileId): bool
{
    $stmt = $db->prepare('SELECT 1 FROM list_members WHERE list_id = ? AND profile_id = ? LIMIT 1');
    $stmt->execute([$listId, $profileId]);

    return (bool) $stmt->fetchColumn();
}

function list_owned_by_profile(array $list, array $profile): bool
{
    return ($list['ownerId'] ?? '') === $profile['id'];
}

/** @return list<array> */
function get_lists_for_profile(PDO $db, string $profileId): array
{
    $stmt = $db->prepare('
        SELECT l.* FROM lists l
        INNER JOIN list_members m ON m.list_id = l.id
        WHERE m.profile_id = ?
        ORDER BY l.updated_at DESC, l.created_at DESC
    ');
    $stmt->execute([$profileId]);

    return array_map(static fn ($row) => listWithItems($db, $row), $stmt->fetchAll());
}

function get_list_for_profile(PDO $db, string $profileId, string $listId): ?array
{
    if (!list_accessible_by_profile($db, $listId, $profileId)) {
        return null;
    }
    $stmt = $db->prepare('SELECT * FROM lists WHERE id = ? LIMIT 1');
    $stmt->execute([$listId]);
    $row = $stmt->fetch();

    return $row ? listWithItems($db, $row) : null;
}

function list_share_url(string $token): string
{
    return web_portal_base_url() . 'list-share.php?token=' . urlencode($token);
}

function get_list_by_share_token(PDO $db, string $token): ?array
{
    $token = trim($token);
    if ($token === '') {
        return null;
    }

    $stmt = $db->prepare('
        SELECT * FROM lists
        WHERE share_enabled = 1 AND share_token_hash = ?
        LIMIT 1
    ');
    $stmt->execute([hash_web_login_token($token)]);
    $row = $stmt->fetch();

    return $row ? listWithItems($db, $row) : null;
}

/** @return string|null Plain-text share token */
function enable_list_share(PDO $db, string $listId, string $profileId): ?string
{
    $stmt = $db->prepare('SELECT id FROM lists WHERE id = ? AND owner_id = ? LIMIT 1');
    $stmt->execute([$listId, $profileId]);
    if (!$stmt->fetch()) {
        return null;
    }

    $token = new_web_login_token();
    $db->prepare('
        UPDATE lists
        SET share_enabled = 1, share_token = ?, share_token_hash = ?, updated_at = ?
        WHERE id = ? AND owner_id = ?
    ')->execute([$token, hash_web_login_token($token), nowMs(), $listId, $profileId]);

    return $token;
}

function get_list_share_token_for_owner(PDO $db, string $listId, string $profileId): ?string
{
    $stmt = $db->prepare('
        SELECT share_token
        FROM lists
        WHERE id = ? AND owner_id = ? AND share_enabled = 1
        LIMIT 1
    ');
    $stmt->execute([$listId, $profileId]);
    $token = trim((string) ($stmt->fetchColumn() ?: ''));

    return $token !== '' ? $token : null;
}

/** Returns a copyable share URL for the owner, creating a stored token if missing. */
function get_list_share_link_for_owner(PDO $db, string $listId, string $profileId): ?string
{
    $stmt = $db->prepare('
        SELECT share_enabled, share_token
        FROM lists
        WHERE id = ? AND owner_id = ?
        LIMIT 1
    ');
    $stmt->execute([$listId, $profileId]);
    $row = $stmt->fetch();
    if (!$row || empty($row['share_enabled'])) {
        return null;
    }

    $token = trim((string) ($row['share_token'] ?? ''));
    if ($token === '') {
        $token = enable_list_share($db, $listId, $profileId) ?? '';
    }

    return $token !== '' ? list_share_url($token) : null;
}

function disable_list_share(PDO $db, string $listId, string $profileId): bool
{
    $stmt = $db->prepare('
        UPDATE lists
        SET share_enabled = 0, share_token = NULL, share_token_hash = NULL, updated_at = ?
        WHERE id = ? AND owner_id = ?
    ');
    $stmt->execute([nowMs(), $listId, $profileId]);

    return $stmt->rowCount() > 0;
}

/**
 * @return array{redirect:?string,error:?string,flash:?string,quantity:?string,itemId:?string}
 */
function process_grocery_list_item_post(
    PDO $db,
    string $listId,
    array $list,
    string $defaultCategoryId,
    string $redirectUrl,
    ?string $catalogProfileId = null,
): array {
    $action = $_POST['action'] ?? '';

    if ($action === 'add_item') {
        $name = trim($_POST['name'] ?? '');
        $quantity = trim($_POST['quantity'] ?? '');
        $category = trim($_POST['category'] ?? '');
        if ($name === '') {
            return ['redirect' => null, 'error' => 'Indtast et varenavn.', 'flash' => null];
        }
        if ($category === '') {
            return ['redirect' => null, 'error' => 'Vælg en kategori.', 'flash' => null];
        }
        upsert_list_item($db, $listId, newId(), [
            'name'      => $name,
            'quantity'  => $quantity !== '' ? $quantity : '1',
            'category'  => $category,
            'isChecked' => false,
            'checkedAt' => null,
        ]);
        if ($catalogProfileId !== null && $catalogProfileId !== '') {
            save_catalog_item_for_profile($db, $catalogProfileId, $name, $category);
        }

        return ['redirect' => $redirectUrl, 'error' => null, 'flash' => 'Varen er tilføjet.'];
    }

    if ($action === 'toggle_item') {
        $itemId = trim($_POST['item_id'] ?? '');
        foreach ($list['items'] as $candidate) {
            if ($candidate['id'] !== $itemId) {
                continue;
            }
            $checked = !empty($candidate['isChecked']);
            upsert_list_item($db, $listId, $itemId, [
                'name'        => $candidate['name'],
                'quantity'    => $candidate['quantity'],
                'category'    => $candidate['category'],
                'isChecked'   => !$checked,
                'checkedAt'   => !$checked ? nowMs() : null,
                'weekday'     => $candidate['weekday'],
                'price'       => $candidate['price'],
                'supermarket' => $candidate['supermarket'],
                'comment'     => $candidate['comment'],
            ]);
            break;
        }

        return ['redirect' => $redirectUrl, 'error' => null, 'flash' => null];
    }

    if ($action === 'delete_item') {
        $itemId = trim($_POST['item_id'] ?? '');
        if ($itemId !== '') {
            $db->prepare('DELETE FROM items WHERE id = ? AND list_id = ?')->execute([$itemId, $listId]);
            $db->prepare('UPDATE lists SET updated_at = ? WHERE id = ?')->execute([nowMs(), $listId]);
        }

        return ['redirect' => $redirectUrl, 'error' => null, 'flash' => null];
    }

    if ($action === 'adjust_quantity') {
        $itemId = trim($_POST['item_id'] ?? '');
        $delta = (int) ($_POST['delta'] ?? 0);
        if ($delta === 0) {
            return ['redirect' => null, 'error' => null, 'flash' => null, 'quantity' => null, 'itemId' => null];
        }
        foreach ($list['items'] as $candidate) {
            if ($candidate['id'] !== $itemId) {
                continue;
            }
            $newQuantity = adjust_item_quantity_string((string) ($candidate['quantity'] ?? ''), $delta);
            upsert_list_item($db, $listId, $itemId, [
                'name'        => $candidate['name'],
                'quantity'    => $newQuantity,
                'category'    => $candidate['category'],
                'isChecked'   => !empty($candidate['isChecked']),
                'checkedAt'   => $candidate['checkedAt'] ?? null,
                'weekday'     => $candidate['weekday'],
                'price'       => $candidate['price'],
                'supermarket' => $candidate['supermarket'],
                'comment'     => $candidate['comment'],
            ]);

            return [
                'redirect' => null,
                'error'    => null,
                'flash'    => null,
                'quantity' => $newQuantity,
                'itemId'   => $itemId,
            ];
        }

        return ['redirect' => null, 'error' => null, 'flash' => null, 'quantity' => null, 'itemId' => null];
    }

    if ($action === 'clear_checked') {
        $db->prepare('DELETE FROM items WHERE list_id = ? AND is_checked = 1')->execute([$listId]);
        $db->prepare('UPDATE lists SET updated_at = ? WHERE id = ?')->execute([nowMs(), $listId]);

        return ['redirect' => $redirectUrl, 'error' => null, 'flash' => 'Afkrydsede varer er fjernet.'];
    }

    return ['redirect' => null, 'error' => null, 'flash' => null];
}

/** @return list<array{id:string,name:string,orderIndex:int}> */
function get_categories_for_profile(PDO $db, string $profileId): array
{
    $stmt = $db->prepare('SELECT * FROM categories WHERE profile_id = ? ORDER BY order_index ASC, id ASC');
    $stmt->execute([$profileId]);

    return array_map(static fn ($row) => [
        'id'         => $row['id'],
        'name'       => $row['name'],
        'orderIndex' => (int) $row['order_index'],
    ], $stmt->fetchAll());
}

function category_name_by_id(array $categories, string $categoryId): string
{
    foreach ($categories as $cat) {
        if ($cat['id'] === $categoryId) {
            return $cat['name'];
        }
    }

    return $categoryId !== '' ? $categoryId : 'Andet';
}

/** @return list<array{title:string,items:list<array>}> */
function group_list_items_by_category(array $items, array $categories, bool $checkedOnly = false): array
{
    $filtered = array_values(array_filter(
        $items,
        static fn ($item) => !empty($item['isChecked']) === $checkedOnly,
    ));
    if ($filtered === []) {
        return [];
    }

    $byCategory = [];
    foreach ($filtered as $item) {
        $catId = (string) ($item['category'] ?? '');
        $byCategory[$catId][] = $item;
    }

    foreach ($byCategory as &$catItems) {
        usort(
            $catItems,
            static fn ($a, $b) => strcasecmp((string) ($a['name'] ?? ''), (string) ($b['name'] ?? '')),
        );
    }
    unset($catItems);

    $groups = [];
    $seen = [];
    foreach ($categories as $cat) {
        $catId = $cat['id'];
        if (empty($byCategory[$catId])) {
            continue;
        }
        $groups[] = ['title' => $cat['name'], 'items' => $byCategory[$catId]];
        $seen[$catId] = true;
    }
    foreach ($byCategory as $catId => $catItems) {
        if (isset($seen[$catId])) {
            continue;
        }
        $groups[] = [
            'title' => category_name_by_id($categories, (string) $catId),
            'items' => $catItems,
        ];
    }

    return $groups;
}

function adjust_item_quantity_string(string $quantity, int $delta): string
{
    $trimmed = trim($quantity);
    if ($trimmed === '') {
        return (string) max(1, 1 + $delta);
    }

    if (!preg_match('/^(\d+(?:[.,]\d+)?)\s*(.*)$/u', $trimmed, $match)) {
        return $trimmed;
    }

    $num = (float) str_replace(',', '.', $match[1]);
    $unit = trim($match[2]);
    $newNum = max(1.0, $num + $delta);
    $formatted = fmod($newNum, 1.0) === 0.0
        ? (string) (int) $newNum
        : rtrim(rtrim(number_format($newNum, 2, '.', ''), '0'), '.');

    return $unit === '' ? $formatted : $formatted . ' ' . $unit;
}

/** @return list<array{id:string,name:string,category:string}> */
function get_catalog_for_profile(PDO $db, string $profileId): array
{
    $stmt = $db->prepare('SELECT * FROM catalog_items WHERE profile_id = ? ORDER BY name ASC');
    $stmt->execute([$profileId]);

    return array_map(static fn ($row) => [
        'id'       => $row['id'],
        'name'     => $row['name'],
        'category' => $row['category'] ?? '',
    ], $stmt->fetchAll());
}

/** @return list<array{title:string,items:list<array{id:string,name:string,category:string}>}> */
function group_catalog_by_category(array $items, array $categories): array
{
    if ($items === []) {
        return [];
    }

    $byCategory = [];
    foreach ($items as $item) {
        $catId = (string) ($item['category'] ?? '');
        $byCategory[$catId][] = $item;
    }

    $groups = [];
    $seen = [];
    foreach ($categories as $cat) {
        $catId = $cat['id'];
        if (empty($byCategory[$catId])) {
            continue;
        }
        usort($byCategory[$catId], static fn ($a, $b) => strcasecmp($a['name'], $b['name']));
        $groups[] = ['title' => $cat['name'], 'items' => $byCategory[$catId]];
        $seen[$catId] = true;
    }
    foreach ($byCategory as $catId => $catItems) {
        if (isset($seen[$catId])) {
            continue;
        }
        usort($catItems, static fn ($a, $b) => strcasecmp($a['name'], $b['name']));
        $groups[] = [
            'title' => category_name_by_id($categories, (string) $catId),
            'items' => $catItems,
        ];
    }

    return $groups;
}

function save_catalog_item_for_profile(
    PDO $db,
    string $profileId,
    string $name,
    string $category,
    ?string $id = null,
): ?array {
    $name = trim($name);
    if ($name === '') {
        return null;
    }

    $now = nowMs();

    if ($id !== null && $id !== '') {
        $db->prepare('
            UPDATE catalog_items
            SET name = ?, category = ?, updated_at = ?
            WHERE id = ? AND profile_id = ?
        ')->execute([$name, $category, $now, $id, $profileId]);

        return ['id' => $id, 'name' => $name, 'category' => $category];
    }

    $stmt = $db->prepare('
        SELECT id FROM catalog_items
        WHERE profile_id = ? AND LOWER(name) = LOWER(?)
        LIMIT 1
    ');
    $stmt->execute([$profileId, $name]);
    $existingId = $stmt->fetchColumn();

    if ($existingId) {
        $db->prepare('
            UPDATE catalog_items
            SET category = ?, updated_at = ?
            WHERE id = ? AND profile_id = ?
        ')->execute([$category, $now, $existingId, $profileId]);

        return ['id' => (string) $existingId, 'name' => $name, 'category' => $category];
    }

    $newId = newId();
    $db->prepare('
        INSERT INTO catalog_items (id, profile_id, name, category, updated_at)
        VALUES (?,?,?,?,?)
    ')->execute([$newId, $profileId, $name, $category, $now]);

    return ['id' => $newId, 'name' => $name, 'category' => $category];
}

function delete_catalog_item_for_profile(PDO $db, string $profileId, string $id): bool
{
    $stmt = $db->prepare('DELETE FROM catalog_items WHERE id = ? AND profile_id = ?');
    $stmt->execute([$id, $profileId]);

    return $stmt->rowCount() > 0;
}

function create_list_for_profile(PDO $db, string $profileId, string $name): array
{
    $now = nowMs();
    $id = newId();
    $name = trim($name);
    $db->prepare('INSERT INTO lists (id, name, owner_id, created_at, updated_at) VALUES (?,?,?,?,?)')
        ->execute([$id, $name, $profileId, $now, $now]);
    $db->prepare('INSERT INTO list_members (list_id, profile_id, joined_at) VALUES (?,?,?)')
        ->execute([$id, $profileId, $now]);
    $stmt = $db->prepare('SELECT * FROM lists WHERE id = ? LIMIT 1');
    $stmt->execute([$id]);

    return listWithItems($db, $stmt->fetch());
}

function upsert_list_item(PDO $db, string $listId, string $itemId, array $body): array
{
    $now = nowMs();
    $db->prepare('
        INSERT INTO items
            (id, list_id, name, quantity, category, is_checked, checked_at, weekday, price, supermarket, comment, updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            quantity = VALUES(quantity),
            category = VALUES(category),
            is_checked = VALUES(is_checked),
            checked_at = VALUES(checked_at),
            weekday = VALUES(weekday),
            price = VALUES(price),
            supermarket = VALUES(supermarket),
            comment = VALUES(comment),
            updated_at = VALUES(updated_at)
    ')->execute([
        $itemId,
        $listId,
        $body['name'],
        $body['quantity'] ?? '',
        $body['category'] ?? '',
        !empty($body['isChecked']) ? 1 : 0,
        $body['checkedAt'] ?? null,
        $body['weekday'] ?? null,
        $body['price'] ?? null,
        $body['supermarket'] ?? null,
        $body['comment'] ?? null,
        $now,
    ]);
    $db->prepare('UPDATE lists SET updated_at = ? WHERE id = ?')->execute([$now, $listId]);
    $stmt = $db->prepare('SELECT * FROM items WHERE id = ? AND list_id = ? LIMIT 1');
    $stmt->execute([$itemId, $listId]);

    return rowToItem($stmt->fetch());
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

function send_activation_code_email(string $toEmail, string $toName, string $code): bool|string
{
    $base = __DIR__ . '/phpmailer';
    require_once $base . '/Exception.php';
    require_once $base . '/PHPMailer.php';
    require_once $base . '/SMTP.php';

    $mailer = new \PHPMailer\PHPMailer\PHPMailer(true);
    try {
        if (MAIL_SMTP_HOST !== '') {
            $mailer->isSMTP();
            $mailer->Host       = MAIL_SMTP_HOST;
            $mailer->Port       = (int) MAIL_SMTP_PORT;
            $mailer->SMTPAuth   = MAIL_SMTP_USER !== '';
            if ($mailer->SMTPAuth) {
                $mailer->Username = MAIL_SMTP_USER;
                $mailer->Password = MAIL_SMTP_PASS;
            }
            if (MAIL_SMTP_SECURE !== '') {
                $mailer->SMTPSecure = MAIL_SMTP_SECURE;
            }
        } else {
            $mailer->isMail();
            $mailer->UseSendmailOptions = false;
        }

        $mailer->CharSet  = 'UTF-8';
        $mailer->Encoding = '8bit';

        $mailer->setFrom(MAIL_FROM_ADDRESS, MAIL_FROM_NAME);
        if (MAIL_REPLY_TO !== '') {
            $mailer->addReplyTo(MAIL_REPLY_TO);
        }
        $mailer->addAddress($toEmail, $toName);

        $greetingName = $toName !== '' ? $toName : 'der';
        $mailer->Subject = 'Din aktiveringskode til ShoppingShark';
        $mailer->isHTML(false);
        $mailer->Body =
            "Hej $greetingName,\r\n\r\n" .
            "Din aktiveringskode til ShoppingShark er:\r\n\r\n" .
            "    $code\r\n\r\n" .
            "Indtast koden i appen eller på opskriftssiden for at logge ind.\r\n\r\n" .
            "Hvis du ikke har bedt om denne kode, kan du roligt ignorere mailen.\r\n\r\n" .
            "Venlig hilsen,\r\nShoppingShark";

        $mailer->send();
        return true;
    } catch (\PHPMailer\PHPMailer\Exception $e) {
        return $mailer->ErrorInfo !== '' ? $mailer->ErrorInfo : $e->getMessage();
    } catch (\Throwable $e) {
        return $e->getMessage();
    }
}

function menuPlanToJson(PDO $db, array $row): array
{
    $stmt = $db->prepare('SELECT recipe_id FROM menu_plan_recipes WHERE menu_plan_id = ? ORDER BY sort_order ASC');
    $stmt->execute([$row['id']]);
    $progress = json_decode($row['recipe_progress'] ?? '{}', true) ?: [];
    $recipeServings = json_decode($row['recipe_servings'] ?? '{}', true) ?: [];
    return [
        'id'             => $row['id'],
        'profileId'      => $row['profile_id'],
        'name'           => $row['name'],
        'description'    => $row['description'] ?? '',
        'servings'       => (int) ($row['servings'] ?? 0),
        'recipeIds'      => $stmt->fetchAll(PDO::FETCH_COLUMN),
        'recipeProgress' => (object) $progress,
        'recipeServings' => (object) $recipeServings,
        'createdAt'      => (int) $row['created_at'],
    ];
}

function sanitize_section_list($value): array
{
    if (!is_array($value)) return [];
    $out = [];
    foreach ($value as $entry) {
        if (is_array($entry) && (count($entry) === 0
                ? false
                : array_keys($entry) !== range(0, count($entry) - 1))
        ) {
            $out[] = $entry;
        }
    }
    return $out;
}

function recipeToJson(array $row): array
{
    $ingredientSections  = sanitize_section_list(
        json_decode($row['ingredient_sections'] ?? '[]', true)
    );
    $instructionSections = sanitize_section_list(
        json_decode($row['instruction_sections'] ?? '[]', true)
    );

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
        'courseType'          => $row['course_type'] ?? '',
        'ingredientSections'  => $ingredientSections,
        'instructionSections' => $instructionSections,
        'tips'                => $row['tips'] ?? '',
        'createdAt'           => (int) $row['created_at'],
        'linkedRecipeIds'     => json_decode($row['linked_recipe_ids'] ?? '[]', true) ?: [],
        'isPinned'            => !empty($row['is_pinned']),
        'pinnedAt'            => isset($row['pinned_at']) ? (int) $row['pinned_at'] : null,
    ];
    if ($out['pinnedAt'] === 0) {
        $out['pinnedAt'] = null;
    }
    $abs = absolute_recipe_image_url($row['image_url'] ?? null);
    if ($abs !== null && $abs !== '') {
        $out['imageUrl'] = $abs;
    }
    return $out;
}

/**
 * Create or update a recipe. $body matches the REST API JSON shape.
 * Pass imageBase64: null to skip image, "" to clear, or base64 JPEG to set.
 *
 * @return array Recipe JSON (recipeToJson shape)
 */
function upsert_recipe(PDO $db, string $profileId, string $id, array $body): array
{
    $now = nowMs();
    $isPinned = !empty($body['isPinned']);
    $pinnedAt = $isPinned
        ? (isset($body['pinnedAt']) ? (int) $body['pinnedAt'] : $now)
        : null;

    $db->prepare('
        INSERT INTO recipes (id, profile_id, name, description, rating, servings, nutrition_facts,
            prep_time_minutes, total_time_minutes, durability, course_type,
            ingredient_sections, instruction_sections, tips, linked_recipe_ids,
            is_pinned, pinned_at, created_at, updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
            linked_recipe_ids    = VALUES(linked_recipe_ids),
            is_pinned            = VALUES(is_pinned),
            pinned_at            = VALUES(pinned_at),
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
        json_encode($body['linkedRecipeIds'] ?? [], JSON_UNESCAPED_UNICODE),
        $isPinned ? 1 : 0,
        $pinnedAt,
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
                throw new InvalidArgumentException('invalid imageBase64');
            }
            $rel = RECIPE_IMAGES_SUBDIR . '/' . $id . '.jpg';
            $full = __DIR__ . '/' . $rel;
            if (file_put_contents($full, $bin) === false) {
                throw new RuntimeException('failed to store recipe image');
            }
            $db->prepare('UPDATE recipes SET image_url = ? WHERE id = ? AND profile_id = ?')->execute([$rel, $id, $profileId]);
        }
    }

    $stmt = $db->prepare('SELECT * FROM recipes WHERE id = ? AND profile_id = ?');
    $stmt->execute([$id, $profileId]);
    $row = $stmt->fetch();
    if (!$row) {
        throw new RuntimeException('recipe not found after upsert');
    }
    return recipeToJson($row);
}

function delete_recipe(PDO $db, string $profileId, string $id): void
{
    delete_recipe_image_file($db, $id, $profileId);
    $db->prepare('DELETE FROM recipes WHERE id = ? AND profile_id = ?')->execute([$id, $profileId]);
}

function get_recipes_for_profile(PDO $db, string $profileId): array
{
    $stmt = $db->prepare('SELECT * FROM recipes WHERE profile_id = ? ORDER BY created_at DESC');
    $stmt->execute([$profileId]);
    return array_map('recipeToJson', $stmt->fetchAll());
}

function get_recipe_for_profile(PDO $db, string $profileId, string $id): ?array
{
    $stmt = $db->prepare('SELECT * FROM recipes WHERE id = ? AND profile_id = ?');
    $stmt->execute([$id, $profileId]);
    $row = $stmt->fetch();
    return $row ? recipeToJson($row) : null;
}

function get_recipe_by_id(PDO $db, string $id): ?array
{
    $stmt = $db->prepare('SELECT * FROM recipes WHERE id = ?');
    $stmt->execute([$id]);
    $row = $stmt->fetch();
    return $row ? recipeToJson($row) : null;
}

/** @return array<string, array> Map of recipe id → recipe JSON */
function get_recipes_by_ids(PDO $db, array $ids): array
{
    $ids = array_values(array_filter(array_map('strval', $ids)));
    if ($ids === []) {
        return [];
    }
    $placeholders = implode(',', array_fill(0, count($ids), '?'));
    $stmt = $db->prepare("SELECT * FROM recipes WHERE id IN ($placeholders)");
    $stmt->execute($ids);
    $map = [];
    foreach ($stmt->fetchAll() as $row) {
        $json = recipeToJson($row);
        $map[$json['id']] = $json;
    }
    return $map;
}

function verify_profile_login(PDO $db, string $email, string $code): ?array
{
    $email = trim($email);
    $code = trim($code);
    if ($email === '' || $code === '') {
        return null;
    }
    $stmt = $db->prepare('SELECT * FROM profiles WHERE email = ? AND activation_code = ? LIMIT 1');
    $stmt->execute([$email, $code]);
    $row = $stmt->fetch();
    return $row ?: null;
}

function send_login_code_for_email(PDO $db, string $email): bool|string
{
    $email = trim($email);
    if ($email === '') {
        return 'Missing email';
    }
    $stmt = $db->prepare('SELECT activation_code, name FROM profiles WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $row = $stmt->fetch();
    if (!$row) {
        return 'not_found';
    }
    $code = (string) $row['activation_code'];
    if ($code === '') {
        return 'Profile has no activation code';
    }
    return send_activation_code_email($email, trim((string) $row['name']), $code);
}

function new_web_login_token(): string
{
    return bin2hex(random_bytes(32));
}

function hash_web_login_token(string $token): string
{
    return hash('sha256', $token);
}

function web_login_next_url(?string $next): string
{
    if ($next === null || $next === '') {
        return 'recipes.php';
    }
    if (preg_match('#^https?://#i', $next) || str_contains($next, '..') || str_contains($next, "\0")) {
        return 'recipes.php';
    }
    return ltrim($next, '/');
}

/**
 * @return array{request_id:string,token:string,name:string}|string
 */
function create_web_login_request(PDO $db, string $email, ?string $next = null, bool $rememberDevice = false): array|string
{
    $email = trim($email);
    if ($email === '') {
        return 'Missing email';
    }

    $stmt = $db->prepare('SELECT id, name FROM profiles WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $profile = $stmt->fetch();
    if (!$profile) {
        return 'not_found';
    }

    $now = time();
    $requestId = newId();
    $token = new_web_login_token();
    $expiresAt = $now + 900;

    $db->prepare('DELETE FROM web_login_requests WHERE email = ? AND completed_at IS NULL')->execute([$email]);

    $db->prepare('
        INSERT INTO web_login_requests (id, email, token_hash, next_url, remember_device, expires_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    ')->execute([
        $requestId,
        $email,
        hash_web_login_token($token),
        web_login_next_url($next),
        $rememberDevice ? 1 : 0,
        $expiresAt,
        $now,
    ]);

    return [
        'request_id' => $requestId,
        'token'      => $token,
        'name'       => trim((string) ($profile['name'] ?? '')),
    ];
}

function send_web_login_email(string $toEmail, string $toName, string $loginUrl): bool|string
{
    $base = __DIR__ . '/phpmailer';
    require_once $base . '/Exception.php';
    require_once $base . '/PHPMailer.php';
    require_once $base . '/SMTP.php';

    $mailer = new \PHPMailer\PHPMailer\PHPMailer(true);
    try {
        if (MAIL_SMTP_HOST !== '') {
            $mailer->isSMTP();
            $mailer->Host       = MAIL_SMTP_HOST;
            $mailer->Port       = (int) MAIL_SMTP_PORT;
            $mailer->SMTPAuth   = MAIL_SMTP_USER !== '';
            if ($mailer->SMTPAuth) {
                $mailer->Username = MAIL_SMTP_USER;
                $mailer->Password = MAIL_SMTP_PASS;
            }
            if (MAIL_SMTP_SECURE !== '') {
                $mailer->SMTPSecure = MAIL_SMTP_SECURE;
            }
        } else {
            $mailer->isMail();
            $mailer->UseSendmailOptions = false;
        }

        $mailer->CharSet  = 'UTF-8';
        $mailer->Encoding = '8bit';

        $mailer->setFrom(MAIL_FROM_ADDRESS, MAIL_FROM_NAME);
        if (MAIL_REPLY_TO !== '') {
            $mailer->addReplyTo(MAIL_REPLY_TO);
        }
        $mailer->addAddress($toEmail, $toName);

        $greetingName = $toName !== '' ? $toName : 'der';
        $safeUrl = htmlspecialchars($loginUrl, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $mailer->Subject = 'Log ind på CookingShark opskrifter';
        $mailer->isHTML(true);
        $mailer->Body =
            "<p>Hej " . htmlspecialchars($greetingName, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8') . ",</p>" .
            "<p>Klik på knappen for at logge ind på opskriftssiden:</p>" .
            "<p style=\"margin:1.5rem 0\"><a href=\"$safeUrl\" style=\"display:inline-block;padding:0.75rem 1.25rem;background:#1a6b7a;color:#fff;text-decoration:none;border-radius:8px;font-weight:600\">Log ind på opskrifter</a></p>" .
            "<p>Eller åbn dette link i din browser:<br><a href=\"$safeUrl\">$safeUrl</a></p>" .
            "<p>Linket udløber om 15 minutter. Hvis du ikke har bedt om at logge ind, kan du roligt ignorere mailen.</p>" .
            "<p>Venlig hilsen,<br>ShoppingShark</p>";
        $mailer->AltBody =
            "Hej $greetingName,\r\n\r\n" .
            "Klik på linket for at logge ind på opskriftssiden:\r\n\r\n" .
            "$loginUrl\r\n\r\n" .
            "Linket udløber om 15 minutter. Hvis du ikke har bedt om at logge ind, kan du roligt ignorere mailen.\r\n\r\n" .
            "Venlig hilsen,\r\nShoppingShark";

        $mailer->send();
        return true;
    } catch (\PHPMailer\PHPMailer\Exception $e) {
        return $mailer->ErrorInfo !== '' ? $mailer->ErrorInfo : $e->getMessage();
    } catch (\Throwable $e) {
        return $e->getMessage();
    }
}

/**
 * @return array{request_id:string,token:string,name:string}|string
 */
function send_web_login_link_for_email(PDO $db, string $email, ?string $next = null, bool $rememberDevice = false): array|string
{
    $created = create_web_login_request($db, $email, $next, $rememberDevice);
    if (!is_array($created)) {
        return $created;
    }

    $loginUrl = web_portal_base_url() . 'login.php?token=' . urlencode($created['token']);
    $sent = send_web_login_email($email, $created['name'], $loginUrl);
    if ($sent !== true) {
        return $sent;
    }

    return $created;
}

/**
 * @return array{status:string,redirect?:string,profile?:array}
 */
function get_web_login_request_status(PDO $db, string $requestId): array
{
    $stmt = $db->prepare('SELECT * FROM web_login_requests WHERE id = ? LIMIT 1');
    $stmt->execute([$requestId]);
    $row = $stmt->fetch();
    if (!$row) {
        return ['status' => 'expired'];
    }

    $now = time();
    if ($row['completed_at'] !== null) {
        return [
            'status'   => 'completed',
            'redirect' => web_login_next_url($row['next_url'] ?? null),
        ];
    }
    if ((int) $row['expires_at'] < $now) {
        return ['status' => 'expired'];
    }

    return ['status' => 'pending'];
}

/**
 * @return array{profile:array,redirect:string,device_token?:string}|null
 */
function complete_web_login_by_token(PDO $db, string $token): ?array
{
    $token = trim($token);
    if ($token === '') {
        return null;
    }

    $hash = hash_web_login_token($token);
    $stmt = $db->prepare('SELECT * FROM web_login_requests WHERE token_hash = ? LIMIT 1');
    $stmt->execute([$hash]);
    $request = $stmt->fetch();
    if (!$request) {
        return null;
    }

    $now = time();
    if ($request['completed_at'] !== null || (int) $request['expires_at'] < $now) {
        return null;
    }

    $stmt = $db->prepare('SELECT * FROM profiles WHERE email = ? LIMIT 1');
    $stmt->execute([$request['email']]);
    $profile = $stmt->fetch();
    if (!$profile) {
        return null;
    }

    $db->prepare('UPDATE web_login_requests SET completed_at = ? WHERE id = ?')
        ->execute([$now, $request['id']]);

    $result = [
        'profile'    => $profile,
        'redirect'   => web_login_next_url($request['next_url'] ?? null),
        'request_id' => (string) $request['id'],
    ];

    if (!empty($request['remember_device'])) {
        $deviceToken = create_web_device_token($db, (string) $profile['id']);
        if ($deviceToken !== null) {
            $result['device_token'] = $deviceToken;
        }
    }

    return $result;
}

/**
 * Log in the browser tab that is waiting for e-mail approval, once the link was
 * clicked elsewhere (often in a separate session started from the mail client).
 *
 * @return array{redirect:string,device_token?:string}|null
 */
function try_establish_web_login_from_request(PDO $db, string $requestId): ?array
{
    $requestId = trim($requestId);
    if ($requestId === '') {
        return null;
    }

    $pendingId = (string) ($_SESSION['pending_login_request_id'] ?? '');
    if ($pendingId === '' || $pendingId !== $requestId) {
        return null;
    }

    $stmt = $db->prepare('SELECT * FROM web_login_requests WHERE id = ? LIMIT 1');
    $stmt->execute([$requestId]);
    $request = $stmt->fetch();
    if (!$request || $request['completed_at'] === null) {
        return null;
    }

    $completedAt = (int) $request['completed_at'];
    if (time() - $completedAt > 900) {
        return null;
    }

    $stmt = $db->prepare('SELECT * FROM profiles WHERE email = ? LIMIT 1');
    $stmt->execute([$request['email']]);
    $profile = $stmt->fetch();
    if (!$profile) {
        return null;
    }

    login_profile($profile);

    $result = [
        'redirect' => web_login_next_url($request['next_url'] ?? null),
    ];

    if (!empty($request['remember_device'])) {
        $deviceToken = create_web_device_token($db, (string) $profile['id']);
        if ($deviceToken !== null) {
            $result['device_token'] = $deviceToken;
        }
    }

    unset($_SESSION['pending_login_email'], $_SESSION['pending_login_request_id'], $_SESSION['login_next']);

    return $result;
}

function create_web_device_token(PDO $db, string $profileId): ?string
{
    if ($profileId === '') {
        return null;
    }

    $token = new_web_login_token();
    $now = time();
    $expiresAt = $now + (365 * 24 * 60 * 60);

    $db->prepare('
        INSERT INTO web_device_tokens (id, profile_id, token_hash, expires_at, created_at, last_used_at)
        VALUES (?, ?, ?, ?, ?, ?)
    ')->execute([
        newId(),
        $profileId,
        hash_web_login_token($token),
        $expiresAt,
        $now,
        $now,
    ]);

    return $token;
}

/**
 * @return array{profile:array,device_token:string}|null
 */
function login_profile_by_device_token(PDO $db, string $token): ?array
{
    $token = trim($token);
    if ($token === '') {
        return null;
    }

    $hash = hash_web_login_token($token);
    $stmt = $db->prepare('SELECT * FROM web_device_tokens WHERE token_hash = ? LIMIT 1');
    $stmt->execute([$hash]);
    $row = $stmt->fetch();
    if (!$row) {
        return null;
    }

    $now = time();
    if ((int) $row['expires_at'] < $now) {
        $db->prepare('DELETE FROM web_device_tokens WHERE id = ?')->execute([$row['id']]);
        return null;
    }

    $stmt = $db->prepare('SELECT * FROM profiles WHERE id = ? LIMIT 1');
    $stmt->execute([$row['profile_id']]);
    $profile = $stmt->fetch();
    if (!$profile) {
        $db->prepare('DELETE FROM web_device_tokens WHERE id = ?')->execute([$row['id']]);
        return null;
    }

    $db->prepare('UPDATE web_device_tokens SET last_used_at = ? WHERE id = ?')->execute([$now, $row['id']]);

    return [
        'profile'      => $profile,
        'device_token' => $token,
    ];
}

function revoke_web_device_token(PDO $db, string $token): void
{
    $token = trim($token);
    if ($token === '') {
        return;
    }
    $db->prepare('DELETE FROM web_device_tokens WHERE token_hash = ?')->execute([hash_web_login_token($token)]);
}
