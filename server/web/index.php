<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';

if (current_profile() !== null) {
    header('Location: ' . safe_redirect_target($_GET['next'] ?? null));
    exit;
}

$error = '';
$step = 'email';
$email = '';
$loginNext = safe_redirect_target($_GET['next'] ?? $_POST['next'] ?? $_SESSION['login_next'] ?? null);

if (isset($_GET['reset'])) {
    unset($_SESSION['pending_login_email']);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    if ($action === 'send_code') {
        $email = trim($_POST['email'] ?? '');
        if ($email === '') {
            $error = 'Indtast din e-mail.';
        } else {
            $result = send_login_code_for_email($db, $email);
            if ($result === 'not_found') {
                $error = 'Ingen konto fundet med den e-mail. Opret først en profil i ShoppingShark-appen.';
            } elseif ($result !== true) {
                $error = 'Kunne ikke sende mail: ' . $result;
            } else {
                $_SESSION['pending_login_email'] = $email;
                $_SESSION['login_next'] = $loginNext;
                $step = 'code';
                flash_set('success', 'Vi har sendt en aktiveringskode til ' . $email . '.');
            }
        }
    } elseif ($action === 'verify_code') {
        $email = (string) ($_SESSION['pending_login_email'] ?? trim($_POST['email'] ?? ''));
        $code = trim($_POST['code'] ?? '');
        if ($email === '' || $code === '') {
            $error = 'Indtast både e-mail og kode.';
            $step = $email !== '' ? 'code' : 'email';
        } else {
            $row = verify_profile_login($db, $email, $code);
            if ($row === null) {
                $error = 'Forkert kode. Prøv igen eller anmod om en ny kode.';
                $step = 'code';
            } else {
                unset($_SESSION['pending_login_email']);
                login_profile($row);
                $next = safe_redirect_target($_POST['next'] ?? $_SESSION['login_next'] ?? null);
                unset($_SESSION['login_next']);
                header('Location: ' . $next);
                exit;
            }
        }
    }
} elseif (isset($_SESSION['pending_login_email'])) {
    $step = 'code';
    $email = (string) $_SESSION['pending_login_email'];
}

$success = flash_get('success');

render_header('Log ind');
?>
<div class="card login-card">
    <h1>Opskrifter på web</h1>
    <p class="subtitle">Log ind med samme e-mail og aktiveringskode som i ShoppingShark-appen.</p>

    <?php if ($success !== null): ?>
        <div class="alert alert-success"><?= h($success) ?></div>
    <?php endif; ?>
    <?php if ($error !== ''): ?>
        <div class="alert alert-error"><?= h($error) ?></div>
    <?php endif; ?>

    <?php if ($step === 'email'): ?>
        <form method="post" class="form-grid">
            <input type="hidden" name="action" value="send_code">
            <?php if ($loginNext !== 'recipes.php'): ?>
                <input type="hidden" name="next" value="<?= h($loginNext) ?>">
            <?php endif; ?>
            <div>
                <label for="email">E-mail</label>
                <input type="email" id="email" name="email" required autocomplete="email" value="<?= h($email) ?>">
            </div>
            <button type="submit" class="btn btn-primary">Send aktiveringskode</button>
        </form>
    <?php else: ?>
        <form method="post" class="form-grid">
            <input type="hidden" name="action" value="verify_code">
            <input type="hidden" name="email" value="<?= h($email) ?>">
            <?php if ($loginNext !== 'recipes.php'): ?>
                <input type="hidden" name="next" value="<?= h($loginNext) ?>">
            <?php endif; ?>
            <div>
                <label>E-mail</label>
                <input type="email" value="<?= h($email) ?>" disabled>
            </div>
            <div>
                <label for="code">Aktiveringskode</label>
                <input type="text" id="code" name="code" required inputmode="numeric" pattern="[0-9]{6}" maxlength="6" autocomplete="one-time-code" placeholder="6 cifre">
            </div>
            <button type="submit" class="btn btn-primary">Log ind</button>
        </form>
        <p class="field-hint" style="margin-top:1rem">
            <a href="index.php?reset=1<?= $loginNext !== 'recipes.php' ? '&amp;next=' . urlencode($loginNext) : '' ?>">Brug en anden e-mail</a>
        </p>
    <?php endif; ?>
</div>
<?php
render_footer();
