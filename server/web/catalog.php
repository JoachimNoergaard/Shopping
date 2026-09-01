<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$profile = require_login();
$error = '';
$editingId = trim($_GET['edit'] ?? '');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    if ($action === 'add_item') {
        $name = trim($_POST['name'] ?? '');
        $category = trim($_POST['category'] ?? '');
        if ($name === '') {
            $error = 'Indtast et varenavn.';
        } else {
            save_catalog_item_for_profile($db, $profile['id'], $name, $category);
            flash_set('success', 'Varen er tilføjet.');
            header('Location: catalog.php');
            exit;
        }
    } elseif ($action === 'update_item') {
        $id = trim($_POST['id'] ?? '');
        $name = trim($_POST['name'] ?? '');
        $category = trim($_POST['category'] ?? '');
        if ($id === '' || $name === '') {
            $error = 'Indtast et varenavn.';
        } else {
            save_catalog_item_for_profile($db, $profile['id'], $name, $category, $id);
            flash_set('success', 'Varen er opdateret.');
            header('Location: catalog.php');
            exit;
        }
    } elseif ($action === 'delete_item') {
        $id = trim($_POST['id'] ?? '');
        if ($id !== '') {
            delete_catalog_item_for_profile($db, $profile['id'], $id);
            flash_set('success', 'Varen er slettet.');
        }
        header('Location: catalog.php');
        exit;
    }
}

$catalog = get_catalog_for_profile($db, $profile['id']);
$categories = get_categories_for_profile($db, $profile['id']);
$defaultCategoryId = $categories[0]['id'] ?? '';
$groups = group_catalog_by_category($catalog, $categories);
$success = flash_get('success');

$editingItem = null;
if ($editingId !== '') {
    foreach ($catalog as $item) {
        if ($item['id'] === $editingId) {
            $editingItem = $item;
            break;
        }
    }
}

render_header('Varer', $profile);
?>
<div class="page-header-row">
    <h1 class="page-title">Varer</h1>
</div>

<p class="page-intro">Dine gemte varer fra ShoppingShark-appen. De vises som forslag, når du tilføjer varer til indkøbslister.</p>

<?php if ($success !== null): ?>
    <div class="alert alert-success"><?= h($success) ?></div>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <div class="alert alert-error"><?= h($error) ?></div>
<?php endif; ?>

<div class="card">
    <h2><?= $editingItem !== null ? 'Rediger vare' : 'Tilføj vare' ?></h2>
    <form method="post" class="form-grid catalog-item-form">
        <input type="hidden" name="action" value="<?= $editingItem !== null ? 'update_item' : 'add_item' ?>">
        <?php if ($editingItem !== null): ?>
            <input type="hidden" name="id" value="<?= h($editingItem['id']) ?>">
        <?php endif; ?>
        <div>
            <label for="catalog-name">Vare</label>
            <input
                type="text"
                id="catalog-name"
                name="name"
                required
                autocomplete="off"
                value="<?= h($editingItem['name'] ?? '') ?>"
                placeholder="Fx mælk"
            >
        </div>
        <?php if ($categories !== []): ?>
            <div>
                <label for="catalog-category">Kategori</label>
                <select id="catalog-category" name="category">
                    <?php foreach ($categories as $cat): ?>
                        <?php
                        $selectedCategory = $editingItem['category'] ?? $defaultCategoryId;
                        ?>
                        <option value="<?= h($cat['id']) ?>" <?= $cat['id'] === $selectedCategory ? 'selected' : '' ?>>
                            <?= h($cat['name']) ?>
                        </option>
                    <?php endforeach; ?>
                </select>
            </div>
        <?php else: ?>
            <input type="hidden" name="category" value="">
        <?php endif; ?>
        <div class="catalog-item-form-actions">
            <button type="submit" class="btn btn-primary">
                <?= $editingItem !== null ? 'Gem' : 'Tilføj' ?>
            </button>
            <?php if ($editingItem !== null): ?>
                <a href="catalog.php" class="btn btn-ghost">Annuller</a>
            <?php endif; ?>
        </div>
    </form>
</div>

<?php if ($groups === []): ?>
    <div class="card empty-state">
        <p>Du har ingen gemte varer endnu.</p>
        <p>Tilføj varer her, eller brug ShoppingShark-appen på telefonen.</p>
    </div>
<?php else: ?>
    <?php foreach ($groups as $group): ?>
        <section class="card catalog-category-section">
            <h2 class="catalog-category-title"><?= h($group['title']) ?></h2>
            <ul class="catalog-items">
                <?php foreach ($group['items'] as $item): ?>
                    <li class="catalog-item">
                        <span class="catalog-item-name"><?= h($item['name']) ?></span>
                        <div class="catalog-item-actions">
                            <a href="catalog.php?edit=<?= h($item['id']) ?>" class="btn btn-ghost btn-sm">Rediger</a>
                            <form method="post" class="inline-form" onsubmit="return confirm('Slet denne vare fra listen?');">
                                <input type="hidden" name="action" value="delete_item">
                                <input type="hidden" name="id" value="<?= h($item['id']) ?>">
                                <button type="submit" class="btn btn-ghost btn-sm catalog-delete-btn" aria-label="Slet vare">×</button>
                            </form>
                        </div>
                    </li>
                <?php endforeach; ?>
            </ul>
        </section>
    <?php endforeach; ?>
<?php endif; ?>

<?php
render_footer();
