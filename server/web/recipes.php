<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$profile = require_login();

$recipes = get_recipes_for_profile($db, $profile['id']);
usort($recipes, static function (array $a, array $b): int {
    return strcasecmp((string) ($a['name'] ?? ''), (string) ($b['name'] ?? ''));
});
$courseTypes = distinct_recipe_course_types($recipes);
$success = flash_get('success');

render_header('Mine opskrifter', $profile);
?>
<div class="page-header-row">
    <h1 class="page-title">Mine opskrifter</h1>
    <div class="page-toolbar">
        <a href="recipe.php?new=1" class="btn btn-primary">+ Ny opskrift</a>
        <a href="recipe-import.php" class="btn btn-ghost">Importér opskrift</a>
    </div>
</div>

<?php if ($success !== null): ?>
    <div class="alert alert-success"><?= h($success) ?></div>
<?php endif; ?>

<?php if ($recipes === []): ?>
    <div class="card empty-state">
        <p>Du har ingen opskrifter endnu.</p>
        <p><a href="recipe.php?new=1" class="btn btn-primary">Opret din første opskrift</a></p>
    </div>
<?php else: ?>
    <div class="recipe-list-controls card">
        <label class="search-label" for="recipe-search">Søg i opskrifter</label>
        <input type="search" id="recipe-search" class="recipe-search-input" placeholder="Søg efter navn…" autocomplete="off">

        <?php if ($courseTypes !== []): ?>
            <div class="course-type-filters" id="course-type-filters" role="group" aria-label="Filtrer efter rettype">
                <button type="button" class="filter-chip is-active" data-course-type="">Alle</button>
                <?php foreach ($courseTypes as $type): ?>
                    <button type="button" class="filter-chip" data-course-type="<?= h($type) ?>"><?= h($type) ?></button>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
    </div>

    <p id="recipe-list-empty-filtered" class="card empty-state" hidden>Ingen opskrifter matcher din søgning.</p>

    <ul class="recipe-list" id="recipe-list">
        <?php foreach ($recipes as $recipe): ?>
            <li class="recipe-item"
                data-name="<?= h($recipe['name']) ?>"
                data-course-type="<?= h($recipe['courseType'] ?? '') ?>">
                <a class="recipe-item-main" href="recipe.php?id=<?= h($recipe['id']) ?>">
                    <?php if (!empty($recipe['imageUrl'])): ?>
                        <img class="recipe-thumb" src="<?= h($recipe['imageUrl']) ?>" alt="">
                    <?php else: ?>
                        <div class="recipe-thumb placeholder" aria-hidden="true">🍽</div>
                    <?php endif; ?>
                    <div class="recipe-meta">
                        <h2><?= h($recipe['name']) ?></h2>
                        <p>
                            <?php
                            $bits = [];
                            if (($recipe['courseType'] ?? '') !== '') {
                                $bits[] = $recipe['courseType'];
                            }
                            if (($recipe['servings'] ?? 0) > 0) {
                                $bits[] = $recipe['servings'] . ' pers.';
                            }
                            if (($recipe['totalTimeMinutes'] ?? 0) > 0) {
                                $bits[] = $recipe['totalTimeMinutes'] . ' min';
                            }
                            echo h($bits !== [] ? implode(' · ', $bits) : 'Ingen detaljer');
                            ?>
                        </p>
                    </div>
                </a>
                <div class="recipe-actions">
                    <a class="btn btn-ghost btn-sm" href="recipe.php?id=<?= h($recipe['id']) ?>&amp;edit=1">Rediger</a>
                </div>
            </li>
        <?php endforeach; ?>
    </ul>
    <script src="assets/recipes-list.js" defer></script>
<?php endif; ?>

<?php
render_footer();
