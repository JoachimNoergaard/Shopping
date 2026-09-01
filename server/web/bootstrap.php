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

function web_wants_json(): bool
{
    if (($_SERVER['HTTP_X_REQUESTED_WITH'] ?? '') === 'XMLHttpRequest') {
        return true;
    }

    $accept = $_SERVER['HTTP_ACCEPT'] ?? '';
    return str_contains($accept, 'application/json');
}

function web_json_response(array $data, int $status = 200): never
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function respond_grocery_list_item_result(array $result, string $redirectUrl): ?string
{
    $error = $result['error'] ?? null;
    if ($error !== null && $error !== '') {
        if (web_wants_json()) {
            web_json_response(['error' => $error], 400);
        }
        return $error;
    }

    $quantity = $result['quantity'] ?? null;
    if ($quantity !== null) {
        if (web_wants_json()) {
            web_json_response([
                'quantity' => $quantity,
                'itemId'   => $result['itemId'] ?? null,
            ]);
        }
        header('Location: ' . $redirectUrl);
        exit;
    }

    $redirect = $result['redirect'] ?? null;
    if ($redirect !== null) {
        $flash = $result['flash'] ?? null;
        if ($flash !== null) {
            flash_set('success', $flash);
        }
        header('Location: ' . $redirect);
        exit;
    }

    return null;
}

function render_grocery_qty_script(string $postUrl, ?string $shareToken = null): void
{
    ?>
    <script>
    (function () {
        var postUrl = <?= json_encode($postUrl, JSON_UNESCAPED_UNICODE) ?>;
        var shareToken = <?= json_encode($shareToken, JSON_UNESCAPED_UNICODE) ?>;

        document.querySelectorAll('[data-qty-adjust]').forEach(function (button) {
            button.addEventListener('click', function () {
                var itemId = button.getAttribute('data-item-id');
                var delta = button.getAttribute('data-delta');
                var row = button.closest('.grocery-item');
                var qtyEl = row ? row.querySelector('.grocery-item-quantity') : null;
                if (!itemId || !qtyEl || !delta) {
                    return;
                }

                var buttons = row.querySelectorAll('[data-qty-adjust]');
                buttons.forEach(function (btn) { btn.disabled = true; });

                var body = new FormData();
                body.append('action', 'adjust_quantity');
                body.append('item_id', itemId);
                body.append('delta', delta);
                if (shareToken) {
                    body.append('token', shareToken);
                }

                fetch(postUrl, {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest',
                    },
                    body: body,
                })
                    .then(function (response) {
                        return response.json().then(function (data) {
                            if (!response.ok) {
                                throw new Error(data.error || 'Kunne ikke opdatere mængden.');
                            }
                            return data;
                        });
                    })
                    .then(function (data) {
                        if (data.quantity) {
                            qtyEl.textContent = data.quantity;
                        }
                    })
                    .catch(function (error) {
                        console.error(error);
                    })
                    .finally(function () {
                        buttons.forEach(function (btn) { btn.disabled = false; });
                    });
            });
        });
    })();
    </script>
    <?php
}

function render_grocery_item_row(array $item, ?string $shareToken = null): void
{
    $checked = !empty($item['isChecked']);
    $quantity = trim((string) ($item['quantity'] ?? ''));
    $displayQuantity = $quantity !== '' ? $quantity : '1';
    ?>
    <li class="grocery-item<?= $checked ? ' is-checked' : '' ?>" data-item-id="<?= h($item['id']) ?>">
        <form method="post" class="grocery-item-toggle">
            <?php if ($shareToken !== null): ?>
                <input type="hidden" name="token" value="<?= h($shareToken) ?>">
            <?php endif; ?>
            <input type="hidden" name="action" value="toggle_item">
            <input type="hidden" name="item_id" value="<?= h($item['id']) ?>">
            <button type="submit" class="grocery-check-btn" aria-label="<?= $checked ? 'Fjern afkrydsning' : 'Afkræds' ?>">
                <span class="grocery-check-box<?= $checked ? ' is-checked' : '' ?>" aria-hidden="true"></span>
            </button>
        </form>
        <div class="grocery-item-body">
            <span class="grocery-item-name"><?= h($item['name']) ?></span>
            <?php if ($checked): ?>
                <?php if ($displayQuantity !== '1'): ?>
                    <span class="grocery-item-quantity"><?= h($displayQuantity) ?></span>
                <?php endif; ?>
            <?php else: ?>
                <div class="grocery-item-qty-controls">
                    <button
                        type="button"
                        class="grocery-qty-btn"
                        data-qty-adjust
                        data-item-id="<?= h($item['id']) ?>"
                        data-delta="-1"
                        aria-label="Reducer mængde"
                    >−</button>
                    <span class="grocery-item-quantity"><?= h($displayQuantity) ?></span>
                    <button
                        type="button"
                        class="grocery-qty-btn"
                        data-qty-adjust
                        data-item-id="<?= h($item['id']) ?>"
                        data-delta="1"
                        aria-label="Forøg mængde"
                    >+</button>
                </div>
            <?php endif; ?>
        </div>
        <form method="post" class="grocery-item-delete">
            <?php if ($shareToken !== null): ?>
                <input type="hidden" name="token" value="<?= h($shareToken) ?>">
            <?php endif; ?>
            <input type="hidden" name="action" value="delete_item">
            <input type="hidden" name="item_id" value="<?= h($item['id']) ?>">
            <button type="submit" class="btn btn-ghost btn-sm grocery-delete-btn" aria-label="Slet vare">×</button>
        </form>
    </li>
    <?php
}

/** @param list<array{title:string,items:list<array>}> $uncheckedGroups */
/** @param list<array{title:string,items:list<array>}> $checkedGroups */
function render_grocery_list_sections(array $uncheckedGroups, array $checkedGroups, ?string $shareToken = null): void
{
    if ($uncheckedGroups === [] && $checkedGroups === []) {
        echo '<div class="card empty-state"><p>Listen er tom. Tilføj varer ovenfor.</p></div>';
        return;
    }

    foreach ($uncheckedGroups as $group) {
        echo '<section class="card grocery-category-section">';
        echo '<h2 class="grocery-category-title">' . h($group['title']) . '</h2>';
        echo '<ul class="grocery-items">';
        foreach ($group['items'] as $item) {
            render_grocery_item_row($item, $shareToken);
        }
        echo '</ul></section>';
    }

    if ($checkedGroups === []) {
        return;
    }

    echo '<section class="card grocery-category-section grocery-checked-section">';
    echo '<h2 class="grocery-category-title">Afkrydsede</h2>';
    echo '<ul class="grocery-items">';
    foreach ($checkedGroups as $group) {
        foreach ($group['items'] as $item) {
            render_grocery_item_row($item, $shareToken);
        }
    }
    echo '</ul></section>';
}

function render_add_grocery_item_form(
    array $categories,
    array $catalogItems = [],
    ?string $shareToken = null,
): void {
    $formAction = $shareToken !== null
        ? 'list-share.php?token=' . urlencode($shareToken)
        : '';
    ?>
    <div class="card add-item-card">
        <form method="post" class="add-item-form"<?= $formAction !== '' ? ' action="' . h($formAction) . '"' : '' ?>>
            <input type="hidden" name="action" value="add_item">
            <?php if ($shareToken !== null): ?>
                <input type="hidden" name="token" value="<?= h($shareToken) ?>">
            <?php endif; ?>
            <div class="add-item-primary">
                <div class="add-item-name-wrap">
                    <label class="visually-hidden" for="item-name">Tilføj vare</label>
                    <input
                        type="text"
                        id="item-name"
                        name="name"
                        required
                        autocomplete="off"
                        placeholder="Tilføj vare…"
                    >
                    <?php if ($catalogItems !== []): ?>
                        <ul class="add-item-suggestions" id="item-suggestions" hidden></ul>
                    <?php endif; ?>
                </div>
                <button type="button" class="add-item-close" aria-label="Luk tilføj vare">×</button>
            </div>
            <div class="add-item-details">
                <div class="add-item-field">
                    <label for="item-quantity">Mængde</label>
                    <input type="text" id="item-quantity" name="quantity" placeholder="1">
                </div>
                <?php if ($categories !== []): ?>
                    <div class="add-item-field">
                        <label for="item-category">Kategori</label>
                        <select id="item-category" name="category" required>
                            <option value="" selected disabled>Vælg kategori</option>
                            <?php foreach ($categories as $cat): ?>
                                <option value="<?= h($cat['id']) ?>">
                                    <?= h($cat['name']) ?>
                                </option>
                            <?php endforeach; ?>
                        </select>
                    </div>
                <?php endif; ?>
                <button type="submit" class="btn btn-primary add-item-submit">Tilføj</button>
            </div>
        </form>
    </div>
    <script>
    (function () {
        var card = document.querySelector('.add-item-card');
        var form = document.querySelector('.add-item-form');
        if (!form || !card) return;

        var closeBtn = form.querySelector('.add-item-close');
        var nameInput = form.querySelector('#item-name');
        var quantityInput = form.querySelector('#item-quantity');

        function isExpanded() {
            if (form.classList.contains('is-collapsed')) return false;
            return form.contains(document.activeElement)
                || (nameInput && nameInput.value.trim() !== '')
                || (quantityInput && quantityInput.value.trim() !== '')
                || (categorySelect && categorySelect.value !== '');
        }

        function collapseAddItem() {
            form.classList.add('is-collapsed');
            if (nameInput) nameInput.value = '';
            if (quantityInput) quantityInput.value = '';
            if (categorySelect) categorySelect.value = '';
            if (nameInput) nameInput.blur();
            hideSuggestions();
        }

        var suggestionsEl = document.getElementById('item-suggestions');
        var categorySelect = document.getElementById('item-category');

        function hideSuggestions() {
            if (!suggestionsEl) return;
            suggestionsEl.hidden = true;
            suggestionsEl.innerHTML = '';
        }

        <?php if ($catalogItems !== []): ?>
        var catalog = <?= json_encode(array_map(static fn ($item) => [
            'name' => $item['name'],
            'category' => $item['category'] ?? '',
        ], $catalogItems), JSON_UNESCAPED_UNICODE) ?>;
        var minSuggestionChars = 2;
        var maxSuggestions = 6;

        function applyCatalogCategory() {
            if (!nameInput || !categorySelect) return;
            var value = nameInput.value.trim().toLowerCase();
            if (value === '') return;
            for (var i = 0; i < catalog.length; i++) {
                if (catalog[i].name.toLowerCase() === value && catalog[i].category) {
                    categorySelect.value = catalog[i].category;
                    return;
                }
            }
        }

        function updateSuggestions() {
            if (!suggestionsEl || !nameInput) return;
            var query = nameInput.value.trim().toLowerCase();
            if (query.length < minSuggestionChars) {
                hideSuggestions();
                return;
            }

            var matches = [];
            for (var i = 0; i < catalog.length; i++) {
                if (catalog[i].name.toLowerCase().indexOf(query) !== -1) {
                    matches.push(catalog[i]);
                    if (matches.length >= maxSuggestions) break;
                }
            }

            if (matches.length === 0) {
                hideSuggestions();
                return;
            }

            suggestionsEl.innerHTML = '';
            matches.forEach(function (item) {
                var option = document.createElement('li');
                option.className = 'add-item-suggestion';
                option.textContent = item.name;
                option.addEventListener('mousedown', function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    nameInput.value = item.name;
                    if (categorySelect && item.category) {
                        categorySelect.value = item.category;
                    }
                    form.classList.remove('is-collapsed');
                    hideSuggestions();
                });
                suggestionsEl.appendChild(option);
            });
            suggestionsEl.hidden = false;
        }

        if (nameInput) {
            nameInput.addEventListener('input', updateSuggestions);
            nameInput.addEventListener('change', applyCatalogCategory);
            nameInput.addEventListener('blur', function () {
                window.setTimeout(hideSuggestions, 120);
                applyCatalogCategory();
            });
        }
        <?php endif; ?>

        if (closeBtn) {
            closeBtn.addEventListener('click', collapseAddItem);
        }

        if (nameInput) {
            nameInput.addEventListener('focus', function () {
                form.classList.remove('is-collapsed');
                <?php if ($catalogItems !== []): ?>
                updateSuggestions();
                <?php endif; ?>
            });
        }

        document.addEventListener('mousedown', function (event) {
            if (card.contains(event.target)) return;
            if (event.target.closest && event.target.closest('.add-item-suggestions')) return;
            if (!isExpanded()) return;
            collapseAddItem();
        });
    })();
    </script>
    <?php
}

function render_header(string $title, ?array $profile = null): void
{
    $fullTitle = $title . ' · ShoppingShark';
    echo '<!DOCTYPE html><html lang="da"><head>';
    echo '<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">';
    echo '<title>' . h($fullTitle) . '</title>';
    echo '<link rel="stylesheet" href="assets/app.css">';
    echo '</head><body>';
    echo '<header class="site-header"><div class="container header-inner">';
    $brandHref = $profile !== null ? 'recipes.php' : 'index.php';
    echo '<a class="brand" href="' . h($brandHref) . '"><img class="brand-icon" src="assets/shopingsharkIcon.png" alt="">ShoppingShark</a>';
    if ($profile !== null) {
        echo '<nav class="header-nav">';
        echo '<div class="header-links">';
        echo '<a href="recipes.php" class="header-link">Opskrifter</a>';
        echo '<a href="lists.php" class="header-link">Indkøbslister</a>';
        echo '<a href="catalog.php" class="header-link">Varer</a>';
        echo '</div>';
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
    echo '<p>Indkøbslister og opskrifter du gemmer her synkroniseres automatisk til ShoppingShark-appen.</p>';
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
        'isPinned'            => !empty($existing['isPinned'] ?? false),
        'pinnedAt'            => isset($existing['pinnedAt']) ? $existing['pinnedAt'] : null,
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
