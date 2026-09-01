<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$token = trim($_GET['token'] ?? $_POST['token'] ?? '');
$error = '';

if ($token === '') {
    http_response_code(404);
    render_header('Ikke fundet');
    echo '<div class="card"><h1>Liste ikke fundet</h1><p>Delingslinket er ugyldigt eller udløbet.</p></div>';
    render_footer();
    exit;
}

$list = get_list_by_share_token($db, $token);
if ($list === null) {
    http_response_code(404);
    render_header('Ikke fundet');
    echo '<div class="card"><h1>Liste ikke fundet</h1><p>Delingslinket er ugyldigt eller deling er slået fra.</p></div>';
    render_footer();
    exit;
}

$listId = $list['id'];
$categories = get_categories_for_profile($db, $list['ownerId']);
$catalogItems = get_catalog_for_profile($db, $list['ownerId']);
$defaultCategoryId = $categories[0]['id'] ?? '';
$redirectUrl = 'list-share.php?token=' . urlencode($token);

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $result = process_grocery_list_item_post($db, $listId, $list, $defaultCategoryId, $redirectUrl);
    if ($result['error'] !== null) {
        $error = $result['error'];
    } elseif ($result['redirect'] !== null) {
        if ($result['flash'] !== null) {
            flash_set('success', $result['flash']);
        }
        header('Location: ' . $result['redirect']);
        exit;
    }

    $list = get_list_by_share_token($db, $token);
    if ($list === null) {
        http_response_code(404);
        render_header('Ikke fundet');
        echo '<div class="card"><h1>Liste ikke fundet</h1><p>Delingslinket er ugyldigt eller deling er slået fra.</p></div>';
        render_footer();
        exit;
    }
}

$uncheckedGroups = group_list_items_by_category($list['items'], $categories, false);
$checkedGroups = group_list_items_by_category($list['items'], $categories, true);
$checkedCount = count(array_filter($list['items'], static fn ($item) => !empty($item['isChecked'])));
$success = flash_get('success');

render_header($list['name']);
?>
<div class="page-header-row">
    <h1 class="page-title"><?= h($list['name']) ?></h1>
    <div class="page-toolbar">
        <?php if ($checkedCount > 0): ?>
            <form method="post" class="inline-form">
                <input type="hidden" name="token" value="<?= h($token) ?>">
                <input type="hidden" name="action" value="clear_checked">
                <button type="submit" class="btn btn-ghost btn-sm">Ryd afkrydsede</button>
            </form>
        <?php endif; ?>
    </div>
</div>

<p class="field-hint shared-list-banner">Delt indkøbsliste — alle med linket kan se og redigere varer.</p>

<?php if ($success !== null): ?>
    <div class="alert alert-success"><?= h($success) ?></div>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <div class="alert alert-error"><?= h($error) ?></div>
<?php endif; ?>

<?php render_add_grocery_item_form($categories, $defaultCategoryId, $catalogItems); ?>

<?php render_grocery_list_sections($uncheckedGroups, $checkedGroups); ?>

<?php
render_footer();
