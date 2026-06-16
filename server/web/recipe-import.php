<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

$profile = require_login();
$courseTypes = course_type_suggestions($db, $profile['id']);

$name = '';
$courseType = '';
$ingredients = '';
$instructions = '';
$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $name = trim($_POST['name'] ?? '');
    $courseType = trim($_POST['course_type'] ?? '');
    $ingredients = (string) ($_POST['ingredients'] ?? '');
    $instructions = (string) ($_POST['instructions'] ?? '');

    if ($name === '') {
        $error = 'Angiv et navn på opskriften.';
    } elseif ($courseType === '') {
        $error = 'Vælg type af ret.';
    } else {
        $id = newId();
        $body = [
            'name'                => $name,
            'description'         => '',
            'rating'              => 0,
            'servings'            => 0,
            'nutritionFacts'      => '',
            'prepTimeMinutes'     => 0,
            'totalTimeMinutes'    => 0,
            'durability'          => '',
            'courseType'          => $courseType,
            'ingredientSections'  => parse_ingredients_text($ingredients),
            'instructionSections' => parse_instructions_text($instructions),
            'tips'                => '',
            'linkedRecipeIds'     => [],
            'createdAt'           => nowMs(),
        ];

        try {
            upsert_recipe($db, $profile['id'], $id, $body);
            flash_set('success', 'Opskriften er importeret. Du kan nu rette detaljer og gemme.');
            header('Location: recipe.php?id=' . urlencode($id) . '&edit=1');
            exit;
        } catch (Throwable $e) {
            $error = 'Kunne ikke importere: ' . $e->getMessage();
        }
    }
}

render_header('Importér opskrift', $profile);
?>
<h1 class="page-title">Importér opskrift</h1>
<p class="page-intro">Indsæt ingredienser og fremgangsmåde som tekst — samme format som i appen.</p>

<?php if ($error !== ''): ?>
    <div class="alert alert-error"><?= h($error) ?></div>
<?php endif; ?>

<form method="post" class="card form-grid">
    <div class="form-grid two-col">
        <div>
            <label for="name">Navn *</label>
            <input type="text" id="name" name="name" required value="<?= h($name) ?>">
        </div>
        <div>
            <label for="course_type">Type af ret *</label>
            <input type="text" id="course_type" name="course_type" list="course-types" required value="<?= h($courseType) ?>">
            <datalist id="course-types">
                <?php foreach ($courseTypes as $type): ?>
                    <option value="<?= h($type) ?>">
                <?php endforeach; ?>
            </datalist>
        </div>
    </div>

    <div>
        <label for="ingredients">Ingredienser</label>
        <textarea id="ingredients" name="ingredients" rows="8" placeholder="Én ingrediens per linje&#10;f.eks. 2 dl mel"><?= h($ingredients) ?></textarea>
    </div>

    <div>
        <label for="instructions">Fremgangsmåde</label>
        <textarea id="instructions" name="instructions" rows="8" placeholder="Én linje per trin"><?= h($instructions) ?></textarea>
        <p class="field-hint">Tip: Linjer, der starter med # opretter ny sektion.</p>
    </div>

    <div class="form-actions">
        <button type="submit" class="btn btn-primary">Importér</button>
        <a href="recipes.php" class="btn btn-ghost">Annuller</a>
    </div>
</form>

<?php
render_footer();
