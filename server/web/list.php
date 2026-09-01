<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$profile = require_login();
$id = trim($_GET['id'] ?? '');
$error = '';

if ($id === '') {
    header('Location: lists.php');
    exit;
}

$list = get_list_for_profile($db, $profile['id'], $id);
if ($list === null) {
    http_response_code(404);
    render_header('Ikke fundet', $profile);
    echo '<div class="card"><h1>Liste ikke fundet</h1><p><a href="lists.php">Tilbage til indkøbslister</a></p></div>';
    render_footer();
    exit;
}

$isOwner = list_owned_by_profile($list, $profile);
$categories = get_categories_for_profile($db, $profile['id']);
$catalogItems = get_catalog_for_profile($db, $profile['id']);
$defaultCategoryId = $categories[0]['id'] ?? '';
$redirectUrl = 'list.php?id=' . urlencode($id);

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    if (in_array($action, ['add_item', 'toggle_item', 'delete_item', 'clear_checked'], true)) {
        $result = process_grocery_list_item_post(
            $db,
            $id,
            $list,
            $defaultCategoryId,
            $redirectUrl,
            $profile['id'],
        );
        if ($result['error'] !== null) {
            $error = $result['error'];
        } elseif ($result['redirect'] !== null) {
            if ($result['flash'] !== null) {
                flash_set('success', $result['flash']);
            }
            header('Location: ' . $result['redirect']);
            exit;
        }
    } elseif ($action === 'rename' && $isOwner) {
        $name = trim($_POST['name'] ?? '');
        if ($name === '') {
            $error = 'Indtast et navn.';
        } else {
            $db->prepare('UPDATE lists SET name = ?, updated_at = ? WHERE id = ?')
                ->execute([$name, nowMs(), $id]);
            flash_set('success', 'Listen er omdøbt.');
            header('Location: ' . $redirectUrl);
            exit;
        }
    } elseif ($action === 'delete' && $isOwner) {
        $db->prepare('DELETE FROM lists WHERE id = ?')->execute([$id]);
        flash_set('success', 'Listen er slettet.');
        header('Location: lists.php');
        exit;
    } elseif ($action === 'leave' && !$isOwner) {
        $db->prepare('DELETE FROM list_members WHERE list_id = ? AND profile_id = ?')
            ->execute([$id, $profile['id']]);
        flash_set('success', 'Du har forladt listen.');
        header('Location: lists.php');
        exit;
    } elseif ($action === 'enable_share' && $isOwner) {
        if (enable_list_share($db, $id, $profile['id']) !== null) {
            flash_set('success', 'Deling er aktiveret.');
        }
        header('Location: ' . $redirectUrl);
        exit;
    } elseif ($action === 'regenerate_share' && $isOwner) {
        if (enable_list_share($db, $id, $profile['id']) !== null) {
            flash_set('success', 'Nyt delingslink er oprettet. Det gamle link virker ikke længere.');
        }
        header('Location: ' . $redirectUrl);
        exit;
    } elseif ($action === 'disable_share' && $isOwner) {
        disable_list_share($db, $id, $profile['id']);
        flash_set('success', 'Deling er deaktiveret.');
        header('Location: ' . $redirectUrl);
        exit;
    }

    $list = get_list_for_profile($db, $profile['id'], $id);
    if ($list === null) {
        header('Location: lists.php');
        exit;
    }
}

$uncheckedGroups = group_list_items_by_category($list['items'], $categories, false);
$checkedGroups = group_list_items_by_category($list['items'], $categories, true);
$checkedCount = count(array_filter($list['items'], static fn ($item) => !empty($item['isChecked'])));
$success = flash_get('success');
$shareEnabled = !empty($list['shareEnabled']);
$shareToken = $isOwner && $shareEnabled
    ? get_list_share_token_for_owner($db, $id, $profile['id'])
    : null;
$shareLink = $shareToken !== null ? list_share_url($shareToken) : null;

render_header($list['name'], $profile);
?>
<a href="lists.php" class="back-link">← Tilbage til indkøbslister</a>

<div class="page-header-row">
    <h1 class="page-title"><?= h($list['name']) ?></h1>
    <div class="page-toolbar">
        <?php if ($checkedCount > 0): ?>
            <form method="post" class="inline-form">
                <input type="hidden" name="action" value="clear_checked">
                <button type="submit" class="btn btn-ghost btn-sm">Ryd afkrydsede</button>
            </form>
        <?php endif; ?>
    </div>
</div>

<?php if ($success !== null): ?>
    <div class="alert alert-success"><?= h($success) ?></div>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <div class="alert alert-error"><?= h($error) ?></div>
<?php endif; ?>

<?php render_add_grocery_item_form($categories, $defaultCategoryId, $catalogItems); ?>

<?php render_grocery_list_sections($uncheckedGroups, $checkedGroups); ?>

<div class="card list-settings">
    <h2>Listeindstillinger</h2>
    <?php if ($isOwner): ?>
        <form method="post" class="form-grid inline-form">
            <input type="hidden" name="action" value="rename">
            <div>
                <label for="list-rename">Navn</label>
                <input type="text" id="list-rename" name="name" required value="<?= h($list['name']) ?>">
            </div>
            <button type="submit" class="btn btn-ghost">Gem navn</button>
        </form>

        <div class="list-share-settings">
            <h3>Del med link</h3>
            <?php if ($shareEnabled): ?>
                <?php if ($shareLink !== null): ?>
                    <div class="share-link-box">
                        <label for="share-link">Delingslink</label>
                        <div class="share-link-row">
                            <input type="text" id="share-link" class="share-link-input" readonly value="<?= h($shareLink) ?>">
                            <button type="button" class="btn btn-ghost btn-sm" data-copy-target="share-link">Kopiér</button>
                        </div>
                    </div>
                <?php else: ?>
                    <p class="field-hint">Deling er aktiv, men linket mangler. Opret et nyt link for at dele listen.</p>
                <?php endif; ?>
                <div class="share-link-actions">
                    <form method="post" class="inline-form">
                        <input type="hidden" name="action" value="regenerate_share">
                        <button type="submit" class="btn btn-ghost btn-sm">Nyt delingslink</button>
                    </form>
                    <form method="post" class="inline-form">
                        <input type="hidden" name="action" value="disable_share">
                        <button type="submit" class="btn btn-ghost btn-sm">Slå deling fra</button>
                    </form>
                </div>
            <?php else: ?>
                <p class="field-hint">Giv adgang til listen uden login via et hemmeligt link.</p>
                <form method="post" class="inline-form">
                    <input type="hidden" name="action" value="enable_share">
                    <button type="submit" class="btn btn-ghost btn-sm">Aktivér deling</button>
                </form>
            <?php endif; ?>
        </div>

        <form method="post" class="list-danger-action" onsubmit="return confirm('Slet denne liste permanent? Den fjernes også for andre medlemmer.');">
            <input type="hidden" name="action" value="delete">
            <button type="submit" class="btn btn-danger">Slet liste</button>
        </form>
    <?php else: ?>
        <p class="field-hint">Dette er en delt liste. Du kan forlade den, men ikke slette den.</p>
        <form method="post" class="list-danger-action" onsubmit="return confirm('Forlad denne liste?');">
            <input type="hidden" name="action" value="leave">
            <button type="submit" class="btn btn-ghost">Forlad liste</button>
        </form>
    <?php endif; ?>
</div>

<script>
document.querySelectorAll('[data-copy-target]').forEach(function (button) {
    button.addEventListener('click', function () {
        var input = document.getElementById(button.getAttribute('data-copy-target'));
        if (!input) {
            return;
        }
        input.select();
        input.setSelectionRange(0, input.value.length);
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(input.value).then(function () {
                button.textContent = 'Kopieret';
                setTimeout(function () { button.textContent = 'Kopiér'; }, 2000);
            });
        }
    });
});
</script>

<?php
render_footer();
