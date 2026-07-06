<?php
declare(strict_types=1);

session_start();

require_once dirname(__DIR__) . '/common.php';

const COURSE_TYPE_SUGGESTIONS = [
    'Forret',
    'Hovedret',
    'Tilbehør',
    'Dessert',
    'Kage',
    'Bagværk',
];

function h(?string $value): string
{
    return htmlspecialchars($value ?? '', ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function current_profile(): ?array
{
    if (empty($_SESSION['profile_id'])) {
        return null;
    }
    return [
        'id'    => (string) $_SESSION['profile_id'],
        'name'  => (string) ($_SESSION['profile_name'] ?? ''),
        'email' => (string) ($_SESSION['profile_email'] ?? ''),
    ];
}

function require_login(?string $redirectAfter = null): array
{
    $profile = current_profile();
    if ($profile === null) {
        header('Location: ' . login_url($redirectAfter));
        exit;
    }
    return $profile;
}

function login_url(?string $next = null): string
{
    if ($next === null || $next === '') {
        return 'index.php';
    }
    return 'index.php?next=' . urlencode(safe_redirect_target($next));
}

function safe_redirect_target(?string $next): string
{
    if ($next === null || $next === '') {
        return 'recipes.php';
    }
    if (preg_match('#^https?://#i', $next) || str_contains($next, '..')) {
        return 'recipes.php';
    }
    return ltrim($next, '/');
}

function recipe_owned_by_profile(array $recipe, array $profile): bool
{
    return ($recipe['profileId'] ?? '') === $profile['id'];
}

function current_request_path(): string
{
    $script = basename($_SERVER['SCRIPT_NAME'] ?? 'index.php');
    $query = $_SERVER['QUERY_STRING'] ?? '';
    return $query !== '' ? $script . '?' . $query : $script;
}

function login_profile(array $row): void
{
    $_SESSION['profile_id']    = $row['id'];
    $_SESSION['profile_name']  = $row['name'] ?? '';
    $_SESSION['profile_email'] = $row['email'] ?? '';
}

function logout_profile(): void
{
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $params['path'], $params['domain'], (bool) $params['secure'], (bool) $params['httponly']);
    }
    session_destroy();
}

function flash_set(string $key, string $message): void
{
    $_SESSION['flash'][$key] = $message;
}

function flash_get(string $key): ?string
{
    if (!isset($_SESSION['flash'][$key])) {
        return null;
    }
    $message = (string) $_SESSION['flash'][$key];
    unset($_SESSION['flash'][$key]);
    return $message;
}

function render_header(string $title, ?array $profile = null): void
{
    $fullTitle = $title . ' · CookingShark';
    echo '<!DOCTYPE html><html lang="da"><head>';
    echo '<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">';
    echo '<title>' . h($fullTitle) . '</title>';
    echo '<link rel="stylesheet" href="assets/app.css">';
    echo '</head><body>';
    echo '<header class="site-header"><div class="container header-inner">';
    $brandHref = $profile !== null ? 'recipes.php' : 'index.php';
    echo '<a class="brand" href="' . h($brandHref) . '"><img class="brand-icon" src="assets/shopingsharkIcon.png" alt="">CookingShark</a>';
    if ($profile !== null) {
        echo '<nav class="header-nav">';
        echo '<span class="user-label">' . h($profile['name'] !== '' ? $profile['name'] : $profile['email']) . '</span>';
        echo '<a href="logout.php" class="btn btn-ghost btn-sm" data-logout-link>Log ud</a>';
        echo '</nav>';
    } else {
        echo '<nav class="header-nav">';
        echo '<a href="index.php" class="btn btn-ghost btn-sm">Log ind</a>';
        echo '</nav>';
    }
    echo '</div></header><main class="container main-content">';
}

function render_footer(bool $includeEditorJs = false): void
{
    echo '</main><footer class="site-footer"><div class="container">';
    echo '<p>Opskrifter du gemmer her synkroniseres automatisk til ShoppingShark-appen.</p>';
    echo '</div></footer>';
    echo '<script src="assets/device-storage.js"></script>';
    if ($includeEditorJs) {
        echo '<script src="assets/editor.js"></script>';
    }
    echo '</body></html>';
}

/**
 * @return array{0:?string,1:string} [base64 or null to skip, error message]
 */
function uploaded_image_as_jpeg_base64(?array $file): array
{
    if ($file === null || ($file['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_NO_FILE) {
        return [null, ''];
    }
    if (($file['error'] ?? UPLOAD_ERR_OK) !== UPLOAD_ERR_OK) {
        return [null, 'Kunne ikke uploade billedet.'];
    }
    $tmp = $file['tmp_name'] ?? '';
    if ($tmp === '' || !is_uploaded_file($tmp)) {
        return [null, 'Ugyldigt upload.'];
    }
    $bytes = file_get_contents($tmp);
    if ($bytes === false) {
        return [null, 'Kunne ikke læse billedet.'];
    }
    if (function_exists('imagecreatefromstring') && function_exists('imagejpeg')) {
        $img = @imagecreatefromstring($bytes);
        if ($img !== false) {
            ob_start();
            imagejpeg($img, null, 88);
            imagedestroy($img);
            $jpeg = ob_get_clean();
            if ($jpeg !== false && $jpeg !== '') {
                return [base64_encode($jpeg), ''];
            }
        }
    }
    return [base64_encode($bytes), ''];
}

/** @return array<string, mixed> */
function recipe_body_from_post(array $post, ?array $existing = null): array
{
    $ingredientSections = [];
    foreach ($post['ing_sec'] ?? [] as $section) {
        if (!is_array($section)) {
            continue;
        }
        $ingredients = [];
        foreach ($section['ing'] ?? [] as $ing) {
            if (!is_array($ing)) {
                continue;
            }
            $name = trim((string) ($ing['name'] ?? ''));
            $quantity = trim((string) ($ing['quantity'] ?? ''));
            $unit = trim((string) ($ing['unit'] ?? ''));
            if ($name === '' && $quantity === '' && $unit === '') {
                continue;
            }
            $ingredients[] = ['name' => $name, 'quantity' => $quantity, 'unit' => $unit];
        }
        $title = trim((string) ($section['title'] ?? ''));
        if ($title !== '' || $ingredients !== []) {
            $ingredientSections[] = ['title' => $title, 'ingredients' => $ingredients];
        }
    }

    $instructionSections = [];
    foreach ($post['inst_sec'] ?? [] as $section) {
        if (!is_array($section)) {
            continue;
        }
        $steps = [];
        foreach ($section['step'] ?? [] as $step) {
            $step = trim((string) $step);
            if ($step !== '') {
                $steps[] = $step;
            }
        }
        $title = trim((string) ($section['title'] ?? ''));
        if ($title !== '' || $steps !== []) {
            $instructionSections[] = ['title' => $title, 'steps' => $steps];
        }
    }

    $linked = [];
    foreach ($post['linked_recipe_ids'] ?? [] as $id) {
        $id = trim((string) $id);
        if ($id !== '') {
            $linked[] = $id;
        }
    }

    return [
        'name'                => trim((string) ($post['name'] ?? '')),
        'description'         => trim((string) ($post['description'] ?? '')),
        'rating'              => max(0, min(5, (int) ($existing['rating'] ?? 0))),
        'servings'            => max(0, (int) ($post['servings'] ?? 0)),
        'nutritionFacts'      => (string) ($existing['nutritionFacts'] ?? ''),
        'prepTimeMinutes'     => max(0, (int) ($post['prep_time_minutes'] ?? 0)),
        'totalTimeMinutes'    => max(0, (int) ($post['total_time_minutes'] ?? 0)),
        'durability'          => (string) ($existing['durability'] ?? ''),
        'courseType'          => trim((string) ($post['course_type'] ?? '')),
        'ingredientSections'  => $ingredientSections,
        'instructionSections' => $instructionSections,
        'tips'                => trim((string) ($post['tips'] ?? '')),
        'linkedRecipeIds'     => $linked,
        'createdAt'           => $existing['createdAt'] ?? nowMs(),
    ];
}

const KNOWN_INGREDIENT_UNITS = [
    'dl', 'ml', 'cl', 'l', 'g', 'kg',
    'tsk', 'spsk', 'stk',
    'fed', 'bundt', 'dåse', 'dåser',
    'pk', 'pakke', 'pakker', 'pose', 'poser',
    'glas', 'skive', 'skiver', 'knivspids', 'nip',
    'tb', 'tbsp', 'tsp', 'cup', 'cups', 'oz', 'lb', 'lbs',
];

function capitalize_ingredient_first_letter(string $value): string
{
    if ($value === '') {
        return $value;
    }
    if (preg_match('/\p{L}/u', $value, $match, PREG_OFFSET_CAPTURE)) {
        $index = $match[0][1];
        $char = mb_substr($value, $index, 1);
        if (mb_strtolower($char) === $char) {
            return mb_substr($value, 0, $index)
                . mb_convert_case($char, MB_CASE_TITLE)
                . mb_substr($value, $index + 1);
        }
    }
    return $value;
}

/** @return array{name:string,quantity:string,unit:string}|null */
function parse_ingredient_line(string $line): ?array
{
    $trimmed = trim($line);
    if ($trimmed === '') {
        return null;
    }

    $quantity = '';
    $afterQty = $trimmed;
    if (preg_match('/^([\d.,\/]+\s*[½⅓¼⅔¾]?|[½⅓¼⅔¾])/u', $trimmed, $qtyMatch)) {
        $quantity = trim($qtyMatch[0]);
        $afterQty = trim(substr($trimmed, strlen($qtyMatch[0])));
    }

    $unit = '';
    $name = $afterQty;
    $unitsPattern = implode('|', array_map(static fn (string $u) => preg_quote($u, '/'), KNOWN_INGREDIENT_UNITS));
    if (preg_match('/^(' . $unitsPattern . ')\.?\b\s*/iu', $afterQty, $unitMatch)) {
        $unit = trim($unitMatch[1]);
        $name = trim(substr($afterQty, strlen($unitMatch[0])));
    }

    if ($name === '' && $quantity === '') {
        return null;
    }

    return [
        'name'     => capitalize_ingredient_first_letter($name),
        'quantity' => $quantity,
        'unit'     => $unit,
    ];
}

/** @return list<array{title:string,ingredients:list<array{name:string,quantity:string,unit:string}>}> */
function parse_ingredients_text(string $text): array
{
    $sections = [];
    $currentTitle = '';
    $currentIngredients = [];

    foreach (preg_split('/\R/u', $text) as $line) {
        $trimmed = trim($line);
        if (str_starts_with($trimmed, '#')) {
            if ($currentIngredients !== []) {
                $sections[] = ['title' => $currentTitle, 'ingredients' => $currentIngredients];
            }
            $currentTitle = trim(substr($trimmed, 1));
            $currentIngredients = [];
            continue;
        }
        $parsed = parse_ingredient_line($trimmed);
        if ($parsed !== null) {
            $currentIngredients[] = $parsed;
        }
    }

    if ($currentIngredients !== []) {
        $sections[] = ['title' => $currentTitle, 'ingredients' => $currentIngredients];
    }

    return $sections;
}

/** @return list<array{title:string,steps:list<string>}> */
function parse_instructions_text(string $text): array
{
    $sections = [];
    $currentTitle = '';
    $currentSteps = [];

    foreach (preg_split('/\R/u', $text) as $line) {
        $trimmed = trim($line);
        if (str_starts_with($trimmed, '#')) {
            if ($currentSteps !== []) {
                $sections[] = ['title' => $currentTitle, 'steps' => $currentSteps];
            }
            $currentTitle = trim(substr($trimmed, 1));
            $currentSteps = [];
            continue;
        }
        if ($trimmed !== '') {
            $currentSteps[] = $trimmed;
        }
    }

    if ($currentSteps !== []) {
        $sections[] = ['title' => $currentTitle, 'steps' => $currentSteps];
    }

    return $sections;
}

function course_type_suggestions(PDO $db, string $profileId): array
{
    $types = COURSE_TYPE_SUGGESTIONS;
    $seen = array_map('mb_strtolower', $types);
    foreach (get_recipes_for_profile($db, $profileId) as $recipe) {
        $courseType = trim((string) ($recipe['courseType'] ?? ''));
        if ($courseType !== '' && !in_array(mb_strtolower($courseType), $seen, true)) {
            $types[] = $courseType;
            $seen[] = mb_strtolower($courseType);
        }
    }
    return $types;
}

/** Course types present in recipes, sorted like the Android app. */
function distinct_recipe_course_types(array $recipes): array
{
    $preferredOrder = array_flip(array_map('mb_strtolower', COURSE_TYPE_SUGGESTIONS));
    $seen = [];
    $types = [];
    foreach ($recipes as $recipe) {
        $type = trim((string) ($recipe['courseType'] ?? ''));
        if ($type === '') {
            continue;
        }
        $key = mb_strtolower($type);
        if (isset($seen[$key])) {
            continue;
        }
        $seen[$key] = true;
        $types[] = $type;
    }
    usort($types, static function (string $a, string $b) use ($preferredOrder): int {
        $ia = $preferredOrder[mb_strtolower($a)] ?? PHP_INT_MAX;
        $ib = $preferredOrder[mb_strtolower($b)] ?? PHP_INT_MAX;
        if ($ia !== $ib) {
            return $ia <=> $ib;
        }
        return strcasecmp($a, $b);
    });
    return $types;
}
