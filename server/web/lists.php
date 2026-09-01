<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$profile = require_login();
$error = '';
$submittedName = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $name = trim($_POST['name'] ?? '');
    $submittedName = $name;
    if ($name === '') {
        $error = 'Indtast et navn til listen.';
    } else {
        $list = create_list_for_profile($db, $profile['id'], $name);
        flash_set('success', 'Listen er oprettet.');
        header('Location: list.php?id=' . urlencode($list['id']));
        exit;
    }
}

$lists = get_lists_for_profile($db, $profile['id']);
$success = flash_get('success');

render_header('Indkøbslister', $profile);
?>
<div class="page-header-row">
    <h1 class="page-title">Indkøbslister</h1>
    <div class="page-toolbar">
        <button type="button" class="btn btn-primary" id="open-create-list-dialog">Opret liste</button>
    </div>
</div>

<p class="page-intro">Dine indkøbslister fra ShoppingShark-appen. Ændringer her synkroniseres automatisk.</p>

<?php if ($success !== null): ?>
    <div class="alert alert-success"><?= h($success) ?></div>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <div class="alert alert-error"><?= h($error) ?></div>
<?php endif; ?>

<?php if ($lists === []): ?>
    <div class="card empty-state">
        <p>Du har ingen indkøbslister endnu.</p>
        <p><button type="button" class="btn btn-primary" data-open-create-list-dialog>Opret din første liste</button></p>
    </div>
<?php else: ?>
    <ul class="grocery-list-overview">
        <?php foreach ($lists as $list): ?>
            <?php
            $items = $list['items'] ?? [];
            $unchecked = count(array_filter($items, static fn ($item) => empty($item['isChecked'])));
            $total = count($items);
            $isOwner = list_owned_by_profile($list, $profile);
            ?>
            <li class="grocery-list-overview-item">
                <a class="grocery-list-overview-main" href="list.php?id=<?= h($list['id']) ?>">
                    <div class="grocery-list-overview-icon" aria-hidden="true">🛒</div>
                    <div class="grocery-list-overview-meta">
                        <h2><?= h($list['name']) ?></h2>
                        <p>
                            <?php if ($total === 0): ?>
                                Ingen varer endnu
                            <?php elseif ($unchecked === 0): ?>
                                Alle <?= $total ?> varer er afkrydsede
                            <?php else: ?>
                                <?= $unchecked ?> tilbage · <?= $total ?> varer i alt
                            <?php endif; ?>
                            <?php if (!$isOwner): ?>
                                · Delt liste
                            <?php endif; ?>
                        </p>
                    </div>
                </a>
            </li>
        <?php endforeach; ?>
    </ul>
<?php endif; ?>

<dialog class="app-dialog" id="create-list-dialog" aria-labelledby="create-list-dialog-title">
    <form method="post" class="dialog-form">
        <h2 id="create-list-dialog-title">Opret ny liste</h2>
        <div>
            <label for="list-name">Navn</label>
            <input
                type="text"
                id="list-name"
                name="name"
                required
                placeholder="Fx ugeindkøb"
                value="<?= h($submittedName) ?>"
                autocomplete="off"
            >
        </div>
        <div class="dialog-actions">
            <button type="button" class="btn btn-ghost" data-dialog-close>Annuller</button>
            <button type="submit" class="btn btn-primary">Opret</button>
        </div>
    </form>
</dialog>

<script>
(function () {
    var dialog = document.getElementById('create-list-dialog');
    var nameInput = document.getElementById('list-name');
    if (!dialog) return;

    function openDialog() {
        dialog.showModal();
        if (nameInput) {
            window.setTimeout(function () {
                nameInput.focus();
                nameInput.select();
            }, 0);
        }
    }

    function closeDialog() {
        dialog.close();
    }

    document.querySelectorAll('#open-create-list-dialog, [data-open-create-list-dialog]').forEach(function (button) {
        button.addEventListener('click', openDialog);
    });

    dialog.querySelectorAll('[data-dialog-close]').forEach(function (button) {
        button.addEventListener('click', closeDialog);
    });

    dialog.addEventListener('click', function (event) {
        if (event.target === dialog) {
            closeDialog();
        }
    });

    dialog.addEventListener('close', function () {
        if (nameInput && !<?= $error !== '' ? 'true' : 'false' ?>) {
            nameInput.value = '';
        }
    });

    <?php if ($error !== ''): ?>
    openDialog();
    <?php endif; ?>
})();
</script>

<?php
render_footer();
