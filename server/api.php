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

require_once __DIR__ . '/common.php';

// Parse path once — used for web portal dispatch and API routing.
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME'] ?? ''), '/');
$uri       = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$path      = ltrim(substr($uri, strlen($scriptDir)), '/');
$segments  = array_values(array_filter(explode('/', $path)));

// When .htaccess rewrites /web/ to api.php, serve the HTML portal from here.
if (!empty($segments) && $segments[0] === 'web') {
    $webRel = implode('/', array_slice($segments, 1));
    if ($webRel === '' || $webRel === 'index.php') {
        $webFile = __DIR__ . '/web/index.php';
    } else {
        $webFile = __DIR__ . '/web/' . $webRel;
    }
    $realWeb = realpath($webFile);
    $webRoot = realpath(__DIR__ . '/web');
    if ($realWeb !== false && $webRoot !== false && strpos($realWeb, $webRoot) === 0 && is_file($realWeb)) {
        require $realWeb;
        exit;
    }
}

// ── Headers ────────────────────────────────────────────────────────────────
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ── Diagnostics ───────────────────────────────────────────────────────────
// Surface fatal PHP errors as a JSON 500 response. Without this, shared hosts
// with display_errors=off return an empty body and the client only sees an
// opaque 500 with no clue what failed. Safe to leave on — only logs the error
// text, no stack traces, and doesn't suppress anything.
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');
register_shutdown_function(function () {
    $err = error_get_last();
    if ($err === null) return;
    $fatalTypes = E_ERROR | E_CORE_ERROR | E_COMPILE_ERROR | E_USER_ERROR | E_PARSE | E_RECOVERABLE_ERROR;
    if (($err['type'] & $fatalTypes) === 0) return;
    if (!headers_sent()) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
    }
    echo json_encode([
        'error'  => 'Fatal PHP error',
        'detail' => $err['message'] . ' in ' . basename($err['file']) . ':' . $err['line'],
    ], JSON_UNESCAPED_UNICODE);
});
set_exception_handler(function (\Throwable $e) {
    if (!headers_sent()) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
    }
    echo json_encode([
        'error'  => 'Uncaught exception',
        'detail' => get_class($e) . ': ' . $e->getMessage() . ' in ' . basename($e->getFile()) . ':' . $e->getLine(),
    ], JSON_UNESCAPED_UNICODE);
});

// ── Router ─────────────────────────────────────────────────────────────────

$method = $_SERVER['REQUEST_METHOD'];

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

// ── POST /profile/by-email/send-code  body: {"email":"..."} ───────────────
//
// Emails the activation code to the user if the address is registered.
// Returns 204 on successful send, 404 if no profile uses that email, 500 if
// the PHPMailer transport itself fails. The 500 body includes a "detail" field
// with the underlying PHPMailer ErrorInfo for debugging.
if ($method === 'POST' && count($segments) === 3 && $segments[0] === 'profile' && $segments[1] === 'by-email' && $segments[2] === 'send-code') {
    $email = trim($body['email'] ?? '');
    if ($email === '') json_out(['error' => 'Missing email'], 400);

    $result = send_login_code_for_email($db, $email);
    if ($result === 'not_found') not_found();
    if ($result !== true) {
        json_out(['error' => 'Failed to send email', 'detail' => $result], 500);
    }

    http_response_code(204);
    exit;
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
    try {
        json_out(upsert_recipe($db, $profileId, $id, $body));
    } catch (InvalidArgumentException $e) {
        json_out(['error' => $e->getMessage()], 400);
    } catch (RuntimeException $e) {
        json_out(['error' => $e->getMessage()], 500);
    }
}

// ── DELETE /profile/{profileId}/recipes/{id} ──────────────────────────────
if ($method === 'DELETE' && count($segments) === 4 && $segments[0] === 'profile' && $segments[2] === 'recipes') {
    [, $profileId, , $id] = $segments;
    delete_recipe($db, $profileId, $id);
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
    $recipeServings = json_encode($body['recipeServings'] ?? new \stdClass());
    $db->prepare('
        INSERT INTO menu_plans (id, profile_id, name, description, servings, recipe_progress, recipe_servings, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
            name             = VALUES(name),
            description      = VALUES(description),
            servings         = VALUES(servings),
            recipe_progress  = VALUES(recipe_progress),
            recipe_servings  = VALUES(recipe_servings),
            updated_at       = VALUES(updated_at)
    ')->execute([$id, $profileId, $body['name'] ?? '', $body['description'] ?? '', (int) ($body['servings'] ?? 0), $recipeProgress, $recipeServings, $body['createdAt'] ?? $now, $now]);

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
