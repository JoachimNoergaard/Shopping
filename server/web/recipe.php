<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$profile = current_profile();
$isNew = isset($_GET['new']);
$isEdit = $isNew || isset($_GET['edit']);
$id = $isNew ? newId() : trim($_GET['id'] ?? '');
$error = '';

if ($isNew || $isEdit) {
    $profile = require_login(current_request_path());
}

if (!$isNew && $id === '') {
    header('Location: ' . ($profile !== null ? 'recipes.php' : 'index.php'));
    exit;
}

$existing = null;
if (!$isNew) {
    if ($isEdit) {
        $existing = get_recipe_for_profile($db, $profile['id'], $id);
    } else {
        $existing = get_recipe_by_id($db, $id);
    }
    if ($existing === null) {
        http_response_code(404);
        render_header('Ikke fundet', $profile);
        echo '<div class="card"><h1>Opskrift ikke fundet</h1>';
        if ($profile !== null) {
            echo '<p><a href="recipes.php">Tilbage til listen</a></p>';
        }
        echo '</div>';
        render_footer();
        exit;
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $profile = require_login();
    $action = $_POST['action'] ?? 'save';
    $id = trim($_POST['id'] ?? $id);

    if ($action === 'delete') {
        if (!get_recipe_for_profile($db, $profile['id'], $id)) {
            http_response_code(403);
            render_header('Ikke tilladt', $profile);
            echo '<div class="card"><h1>Du kan ikke slette denne opskrift.</h1></div>';
            render_footer();
            exit;
        }
        delete_recipe($db, $profile['id'], $id);
        flash_set('success', 'Opskriften er slettet.');
        header('Location: recipes.php');
        exit;
    }

    if (!$isEdit && !$isNew) {
        header('Location: recipe.php?id=' . urlencode($id));
        exit;
    }

    if ($isNew) {
        $existing = null;
    } elseif ($existing === null || !recipe_owned_by_profile($existing, $profile)) {
        $existing = get_recipe_for_profile($db, $profile['id'], $id);
        if ($existing === null) {
            http_response_code(403);
            render_header('Ikke tilladt', $profile);
            echo '<div class="card"><h1>Du kan ikke redigere denne opskrift.</h1></div>';
            render_footer();
            exit;
        }
    }

    $body = recipe_body_from_post($_POST, $existing);
    if ($body['name'] === '') {
        $error = 'Opskriften skal have et navn.';
        $existing = array_merge($existing ?? ['id' => $id, 'profileId' => $profile['id']], $body);
        $existing['id'] = $id;
    } else {
        if (!empty($_POST['clear_image'])) {
            $body['imageBase64'] = '';
        } else {
            [$imageBase64, $uploadError] = uploaded_image_as_jpeg_base64($_FILES['image'] ?? null);
            if ($uploadError !== '') {
                $error = $uploadError;
            } elseif ($imageBase64 !== null) {
                $body['imageBase64'] = $imageBase64;
            }
        }

        if ($error === '') {
            try {
                upsert_recipe($db, $profile['id'], $id, $body);
                flash_set('success', $isNew ? 'Opskriften er oprettet og vil snart vises i appen.' : 'Opskriften er gemt.');
                header('Location: recipe.php?id=' . urlencode($id));
                exit;
            } catch (Throwable $e) {
                $error = 'Kunne ikke gemme: ' . $e->getMessage();
            }
        }
        $existing = array_merge($existing ?? ['id' => $id, 'profileId' => $profile['id']], $body);
        $existing['id'] = $id;
    }
}

if (!$isNew && !$isEdit) {
    $linkedIds = $existing['linkedRecipeIds'] ?? [];
    $recipeById = get_recipes_by_ids($db, $linkedIds);
    $canEdit = $profile !== null && recipe_owned_by_profile($existing, $profile);
    $success = flash_get('success');
    render_header($existing['name'], $profile);
    ?>
    <?php if ($success !== null): ?>
        <div class="alert alert-success"><?= h($success) ?></div>
    <?php endif; ?>

    <?php if ($profile !== null): ?>
        <a href="recipes.php" class="back-link">← Tilbage til opskrifter</a>
    <?php endif; ?>

    <article class="card recipe-view">
        <?php if ($canEdit): ?>
            <div class="recipe-view-toolbar">
                <a href="recipe.php?id=<?= h($existing['id']) ?>&amp;edit=1" class="btn btn-primary btn-sm">Rediger</a>
            </div>
        <?php endif; ?>
        <div class="recipe-view-hero">
            <?php if (!empty($existing['imageUrl'])): ?>
                <img class="recipe-view-image" src="<?= h($existing['imageUrl']) ?>" alt="">
            <?php endif; ?>
            <div class="recipe-view-intro">
                <h1 class="recipe-view-title"><?= h($existing['name']) ?></h1>
                <?php
                $meta = [];
                if (($existing['courseType'] ?? '') !== '') {
                    $meta[] = $existing['courseType'];
                }
                if (($existing['servings'] ?? 0) > 0) {
                    $meta[] = $existing['servings'] . ' pers.';
                }
                if (($existing['prepTimeMinutes'] ?? 0) > 0) {
                    $meta[] = 'Forberedelse ' . $existing['prepTimeMinutes'] . ' min';
                }
                if (($existing['totalTimeMinutes'] ?? 0) > 0) {
                    $meta[] = 'Total ' . $existing['totalTimeMinutes'] . ' min';
                }
                if (($existing['rating'] ?? 0) > 0) {
                    $meta[] = str_repeat('★', (int) $existing['rating']) . str_repeat('☆', 5 - (int) $existing['rating']);
                }
                if ($meta !== []): ?>
                    <p class="recipe-view-meta"><?= h(implode(' · ', $meta)) ?></p>
                <?php endif; ?>
                <?php if (($existing['description'] ?? '') !== ''): ?>
                    <p class="recipe-view-description"><?= nl2br(h($existing['description'])) ?></p>
                <?php endif; ?>
            </div>
        </div>

        <?php
        $extra = [];
        if (($existing['durability'] ?? '') !== '') {
            $extra[] = 'Holdbarhed: ' . $existing['durability'];
        }
        if (($existing['nutritionFacts'] ?? '') !== '') {
            $extra[] = 'Næring: ' . $existing['nutritionFacts'];
        }
        if ($extra !== []): ?>
            <p class="recipe-view-extra"><?= h(implode(' · ', $extra)) ?></p>
        <?php endif; ?>

        <?php foreach ($existing['ingredientSections'] ?? [] as $section): ?>
            <?php
            $ings = $section['ingredients'] ?? [];
            if ($ings === []) {
                continue;
            }
            ?>
            <section class="recipe-view-section">
                <?php if (($section['title'] ?? '') !== ''): ?>
                    <h2><?= h($section['title']) ?></h2>
                <?php else: ?>
                    <h2>Ingredienser</h2>
                <?php endif; ?>
                <ul class="recipe-ingredient-list">
                    <?php foreach ($ings as $ing): ?>
                        <?php
                        $name = trim((string) ($ing['name'] ?? ''));
                        if ($name === '') {
                            continue;
                        }
                        $qty = trim((string) ($ing['quantity'] ?? ''));
                        $unit = trim((string) ($ing['unit'] ?? ''));
                        $amount = trim($qty . ' ' . $unit);
                        ?>
                        <li>
                            <?php if ($amount !== ''): ?>
                                <span class="ingredient-amount"><?= h($amount) ?></span>
                            <?php endif; ?>
                            <?= h($name) ?>
                        </li>
                    <?php endforeach; ?>
                </ul>
            </section>
        <?php endforeach; ?>

        <?php foreach ($existing['instructionSections'] ?? [] as $section): ?>
            <?php
            $steps = array_values(array_filter(
                array_map('trim', $section['steps'] ?? []),
                fn ($s) => $s !== ''
            ));
            if ($steps === []) {
                continue;
            }
            ?>
            <section class="recipe-view-section">
                <?php if (($section['title'] ?? '') !== ''): ?>
                    <h2><?= h($section['title']) ?></h2>
                <?php else: ?>
                    <h2>Fremgangsmåde</h2>
                <?php endif; ?>
                <ol class="recipe-step-list">
                    <?php foreach ($steps as $step): ?>
                        <li><?= nl2br(h($step)) ?></li>
                    <?php endforeach; ?>
                </ol>
            </section>
        <?php endforeach; ?>

        <?php if (($existing['tips'] ?? '') !== ''): ?>
            <section class="recipe-view-section">
                <h2>Tips</h2>
                <p><?= nl2br(h($existing['tips'])) ?></p>
            </section>
        <?php endif; ?>

        <?php
        $linked = array_values(array_filter(
            $existing['linkedRecipeIds'] ?? [],
            fn ($lid) => isset($recipeById[$lid])
        ));
        if ($linked !== []): ?>
            <section class="recipe-view-section">
                <h2>Se også</h2>
                <ul class="recipe-linked-list">
                    <?php foreach ($linked as $lid): ?>
                        <li><a href="recipe.php?id=<?= h($lid) ?>"><?= h($recipeById[$lid]['name']) ?></a></li>
                    <?php endforeach; ?>
                </ul>
            </section>
        <?php endif; ?>
    </article>
    <?php
    render_footer();
    exit;
}

if ($existing === null) {
    $existing = [
        'id'                  => $id,
        'profileId'           => $profile['id'],
        'name'                => '',
        'description'         => '',
        'rating'              => 0,
        'servings'            => 4,
        'nutritionFacts'      => '',
        'prepTimeMinutes'     => 0,
        'totalTimeMinutes'    => 0,
        'durability'          => '',
        'courseType'          => '',
        'ingredientSections'  => [
            ['title' => '', 'ingredients' => [['name' => '', 'quantity' => '', 'unit' => '']]],
        ],
        'instructionSections' => [
            ['title' => '', 'steps' => ['']],
        ],
        'tips'                => '',
        'linkedRecipeIds'     => [],
        'imageUrl'            => null,
        'createdAt'           => nowMs(),
    ];
}

$allRecipes = get_recipes_for_profile($db, $profile['id']);
$otherRecipes = array_values(array_filter($allRecipes, fn ($r) => $r['id'] !== $existing['id']));

$ingSections = $existing['ingredientSections'] ?? [];
if ($ingSections === []) {
    $ingSections = [['title' => '', 'ingredients' => [['name' => '', 'quantity' => '', 'unit' => '']]]];
}
$instSections = $existing['instructionSections'] ?? [];
if ($instSections === []) {
    $instSections = [['title' => '', 'steps' => ['']]];
}

$courseTypes = COURSE_TYPE_SUGGESTIONS;
foreach ($allRecipes as $r) {
    $ct = trim((string) ($r['courseType'] ?? ''));
    if ($ct !== '' && !in_array($ct, $courseTypes, true)) {
        $courseTypes[] = $ct;
    }
}

$pageTitle = $isNew ? 'Ny opskrift' : 'Rediger opskrift';
$success = flash_get('success');
render_header($pageTitle, $profile);
?>
<h1 class="page-title"><?= h($pageTitle) ?></h1>

<?php if ($success !== null): ?>
    <div class="alert alert-success"><?= h($success) ?></div>
<?php endif; ?>
<?php if ($error !== ''): ?>
    <div class="alert alert-error"><?= h($error) ?></div>
<?php endif; ?>

<form method="post" enctype="multipart/form-data" class="card form-grid">
    <input type="hidden" name="action" value="save">
    <input type="hidden" name="id" value="<?= h($existing['id']) ?>">

    <div class="form-grid two-col">
        <div>
            <label for="name">Navn *</label>
            <input type="text" id="name" name="name" required value="<?= h($existing['name'] ?? '') ?>">
        </div>
        <div>
            <label for="course_type">Rettype</label>
            <input type="text" id="course_type" name="course_type" list="course-types" value="<?= h($existing['courseType'] ?? '') ?>">
            <datalist id="course-types">
                <?php foreach ($courseTypes as $type): ?>
                    <option value="<?= h($type) ?>">
                <?php endforeach; ?>
            </datalist>
        </div>
    </div>

    <div>
        <label for="description">Beskrivelse</label>
        <textarea id="description" name="description"><?= h($existing['description'] ?? '') ?></textarea>
    </div>

    <div class="form-grid two-col">
        <div>
            <label for="servings">Antal personer</label>
            <input type="number" id="servings" name="servings" min="0" value="<?= (int) ($existing['servings'] ?? 0) ?>">
        </div>
        <div>
            <label for="prep_time_minutes">Forberedelse (min)</label>
            <input type="number" id="prep_time_minutes" name="prep_time_minutes" min="0" value="<?= (int) ($existing['prepTimeMinutes'] ?? 0) ?>">
        </div>
        <div>
            <label for="total_time_minutes">Total tid (min)</label>
            <input type="number" id="total_time_minutes" name="total_time_minutes" min="0" value="<?= (int) ($existing['totalTimeMinutes'] ?? 0) ?>">
        </div>
    </div>

    <div>
        <label for="image">Billede</label>
        <?php if (!empty($existing['imageUrl'])): ?>
            <img class="image-preview" src="<?= h($existing['imageUrl']) ?>" alt="Nuværende billede">
            <label style="font-weight:400;margin-top:0.5rem">
                <input type="checkbox" name="clear_image" value="1"> Fjern billede
            </label>
        <?php endif; ?>
        <input type="file" id="image" name="image" accept="image/*">
        <p class="field-hint">Valgfrit. Gemmes som JPEG på serveren — samme som i appen.</p>
    </div>

    <div>
        <h2>Ingredienser</h2>
        <div id="ingredient-sections">
            <?php foreach ($ingSections as $si => $section): ?>
                <?php
                $ings = $section['ingredients'] ?? [];
                if ($ings === []) {
                    $ings = [['name' => '', 'quantity' => '', 'unit' => '']];
                }
                ?>
                <div class="section-block" data-ingredient-section>
                    <h3>Ingredienssektion <?= $si + 1 ?></h3>
                    <div>
                        <label>Sektionstitel (valgfri)</label>
                        <input type="text" name="ing_sec[<?= $si ?>][title]" value="<?= h($section['title'] ?? '') ?>">
                    </div>
                    <div class="row-list" data-ingredient-list>
                        <?php foreach ($ings as $ii => $ing): ?>
                            <div class="ingredient-row" data-row>
                                <div>
                                    <label>Ingrediens</label>
                                    <input type="text" name="ing_sec[<?= $si ?>][ing][<?= $ii ?>][name]" value="<?= h($ing['name'] ?? '') ?>">
                                </div>
                                <div>
                                    <label>Mængde</label>
                                    <input type="text" name="ing_sec[<?= $si ?>][ing][<?= $ii ?>][quantity]" value="<?= h($ing['quantity'] ?? '') ?>">
                                </div>
                                <div>
                                    <label>Enhed</label>
                                    <input type="text" name="ing_sec[<?= $si ?>][ing][<?= $ii ?>][unit]" value="<?= h($ing['unit'] ?? '') ?>">
                                </div>
                                <button type="button" class="btn btn-ghost btn-sm" data-remove-row>Fjern</button>
                            </div>
                        <?php endforeach; ?>
                    </div>
                    <div class="toolbar">
                        <button type="button" class="btn btn-ghost btn-sm" data-add-ingredient>+ Ingrediens</button>
                        <button type="button" class="btn btn-danger btn-sm" data-remove-section>Fjern sektion</button>
                    </div>
                </div>
            <?php endforeach; ?>
        </div>
        <button type="button" class="btn btn-ghost" id="add-ingredient-section">+ Ingredienssektion</button>
    </div>

    <div>
        <h2>Fremgangsmåde</h2>
        <div id="instruction-sections">
            <?php foreach ($instSections as $si => $section): ?>
                <?php
                $steps = $section['steps'] ?? [];
                if ($steps === []) {
                    $steps = [''];
                }
                ?>
                <div class="section-block" data-instruction-section>
                    <h3>Trinsektion <?= $si + 1 ?></h3>
                    <div>
                        <label>Sektionstitel (valgfri)</label>
                        <input type="text" name="inst_sec[<?= $si ?>][title]" value="<?= h($section['title'] ?? '') ?>">
                    </div>
                    <div class="row-list" data-step-list>
                        <?php foreach ($steps as $ii => $step): ?>
                            <div class="step-row" data-row>
                                <div>
                                    <label>Trin <?= $ii + 1 ?></label>
                                    <textarea name="inst_sec[<?= $si ?>][step][<?= $ii ?>]" rows="2"><?= h($step) ?></textarea>
                                </div>
                                <button type="button" class="btn btn-ghost btn-sm" data-remove-row>Fjern</button>
                            </div>
                        <?php endforeach; ?>
                    </div>
                    <div class="toolbar">
                        <button type="button" class="btn btn-ghost btn-sm" data-add-step>+ Trin</button>
                        <button type="button" class="btn btn-danger btn-sm" data-remove-section>Fjern sektion</button>
                    </div>
                </div>
            <?php endforeach; ?>
        </div>
        <button type="button" class="btn btn-ghost" id="add-instruction-section">+ Trinsektion</button>
    </div>

    <div>
        <label for="tips">Tips</label>
        <textarea id="tips" name="tips"><?= h($existing['tips'] ?? '') ?></textarea>
    </div>

    <?php if ($otherRecipes !== []): ?>
        <div>
            <label>Relaterede opskrifter</label>
            <select name="linked_recipe_ids[]" multiple size="<?= min(6, count($otherRecipes)) ?>">
                <?php foreach ($otherRecipes as $other): ?>
                    <option value="<?= h($other['id']) ?>" <?= in_array($other['id'], $existing['linkedRecipeIds'] ?? [], true) ? 'selected' : '' ?>>
                        <?= h($other['name']) ?>
                    </option>
                <?php endforeach; ?>
            </select>
            <p class="field-hint">Hold Ctrl/Cmd for at vælge flere.</p>
        </div>
    <?php endif; ?>

    <div class="form-actions">
        <button type="submit" class="btn btn-primary">Gem opskrift</button>
        <a href="<?= $isNew ? 'recipes.php' : 'recipe.php?id=' . urlencode($existing['id']) ?>" class="btn btn-ghost">Annuller</a>
    </div>

    <?php if (!$isNew): ?>
        <hr class="form-divider">
        <div class="recipe-edit-delete">
            <button type="submit" name="action" value="delete" class="btn btn-danger" formnovalidate
                onclick="return confirm('Slet denne opskrift permanent? Den fjernes også fra appen ved næste synkronisering.');">
                Slet opskrift
            </button>
        </div>
    <?php endif; ?>
</form>

<template id="tpl-ingredient-row">
    <div class="ingredient-row" data-row>
        <div>
            <label>Ingrediens</label>
            <input type="text" name="ing_sec[__SI__][ing][__II__][name]">
        </div>
        <div>
            <label>Mængde</label>
            <input type="text" name="ing_sec[__SI__][ing][__II__][quantity]">
        </div>
        <div>
            <label>Enhed</label>
            <input type="text" name="ing_sec[__SI__][ing][__II__][unit]">
        </div>
        <button type="button" class="btn btn-ghost btn-sm" data-remove-row>Fjern</button>
    </div>
</template>

<template id="tpl-ingredient-section">
    <div class="section-block" data-ingredient-section>
        <h3>Ny ingredienssektion</h3>
        <div>
            <label>Sektionstitel (valgfri)</label>
            <input type="text" name="ing_sec[__SI__][title]">
        </div>
        <div class="row-list" data-ingredient-list></div>
        <div class="toolbar">
            <button type="button" class="btn btn-ghost btn-sm" data-add-ingredient>+ Ingrediens</button>
            <button type="button" class="btn btn-danger btn-sm" data-remove-section>Fjern sektion</button>
        </div>
    </div>
</template>

<template id="tpl-step-row">
    <div class="step-row" data-row>
        <div>
            <label>Trin</label>
            <textarea name="inst_sec[__SI__][step][__II__]" rows="2"></textarea>
        </div>
        <button type="button" class="btn btn-ghost btn-sm" data-remove-row>Fjern</button>
    </div>
</template>

<template id="tpl-instruction-section">
    <div class="section-block" data-instruction-section>
        <h3>Ny trinsektion</h3>
        <div>
            <label>Sektionstitel (valgfri)</label>
            <input type="text" name="inst_sec[__SI__][title]">
        </div>
        <div class="row-list" data-step-list></div>
        <div class="toolbar">
            <button type="button" class="btn btn-ghost btn-sm" data-add-step>+ Trin</button>
            <button type="button" class="btn btn-danger btn-sm" data-remove-section>Fjern sektion</button>
        </div>
    </div>
</template>

<?php
render_footer(true);
