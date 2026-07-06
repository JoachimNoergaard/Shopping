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
$loginRequestId = '';
$loginNext = safe_redirect_target($_GET['next'] ?? $_POST['next'] ?? $_SESSION['login_next'] ?? null);

if (isset($_GET['reset'])) {
    unset($_SESSION['pending_login_email'], $_SESSION['pending_login_request_id'], $_SESSION['login_next']);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    if ($action === 'send_link') {
        $email = trim($_POST['email'] ?? '');
        $rememberDevice = !empty($_POST['remember_device']);
        if ($email === '') {
            $error = 'Indtast din e-mail.';
        } else {
            $result = send_web_login_link_for_email($db, $email, $loginNext, $rememberDevice);
            if ($result === 'not_found') {
                $error = 'Ingen konto fundet med den e-mail. Opret først en profil i ShoppingShark-appen.';
            } elseif (!is_array($result)) {
                $error = 'Kunne ikke sende mail: ' . $result;
            } else {
                $_SESSION['pending_login_email'] = $email;
                $_SESSION['pending_login_request_id'] = $result['request_id'];
                $_SESSION['login_next'] = $loginNext;
                flash_set('success', 'Vi har sendt et login-link til ' . $email . '. Klik på linket i mailen for at logge ind.');
                header('Location: index.php');
                exit;
            }
        }
    }
} elseif (isset($_SESSION['pending_login_request_id'])) {
    $step = 'pending';
    $email = (string) ($_SESSION['pending_login_email'] ?? '');
    $loginRequestId = (string) $_SESSION['pending_login_request_id'];
    $loginNext = safe_redirect_target($_SESSION['login_next'] ?? null);
}

$success = flash_get('success');
$flashError = flash_get('error');
if ($flashError !== null) {
    $error = $flashError;
}

render_header('Log ind');
?>
<div class="card login-card">
    <h1>Opskrifter på web</h1>
    <p class="subtitle">Log ind med samme e-mail som i ShoppingShark-appen. Vi sender et login-link til din indbakke.</p>

    <?php if ($success !== null): ?>
        <div class="alert alert-success"><?= h($success) ?></div>
    <?php endif; ?>
    <?php if ($error !== ''): ?>
        <div class="alert alert-error"><?= h($error) ?></div>
    <?php endif; ?>

    <?php if ($step === 'email'): ?>
        <div id="login-auto" data-redirect="<?= h($loginNext) ?>" hidden></div>
        <form method="post" class="form-grid" id="login-email-form">
            <input type="hidden" name="action" value="send_link">
            <?php if ($loginNext !== 'recipes.php'): ?>
                <input type="hidden" name="next" value="<?= h($loginNext) ?>">
            <?php endif; ?>
            <div>
                <label for="email">E-mail</label>
                <input type="email" id="email" name="email" required autocomplete="email" value="<?= h($email) ?>">
            </div>
            <label class="checkbox-row">
                <input type="checkbox" id="remember-device" name="remember_device" value="1">
                <span>Husk mig på denne enhed</span>
            </label>
            <button type="submit" class="btn btn-primary">Send login-link</button>
        </form>
        <script src="assets/login-form.js" defer></script>
    <?php else: ?>
        <div id="login-pending"
             class="login-pending"
             data-request-id="<?= h($loginRequestId) ?>"
             data-redirect="<?= h($loginNext) ?>">
            <p>Vi har sendt et login-link til <strong><?= h($email) ?></strong>.</p>
            <p>Klik på linket i mailen for at logge ind. Denne side opdateres automatisk, når du har klikket på linket.</p>
            <p class="login-pending-status" aria-live="polite">Venter på godkendelse…</p>
        </div>
        <p class="field-hint" style="margin-top:1rem">
            <a href="index.php?reset=1<?= $loginNext !== 'recipes.php' ? '&amp;next=' . urlencode($loginNext) : '' ?>">Brug en anden e-mail</a>
        </p>
        <script src="assets/login-pending.js" defer></script>
    <?php endif; ?>
</div>
<?php
render_footer();
