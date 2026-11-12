<?php
/** Offer metadata shared by the website and the Android API's existing item format. */

function grocery_shop_ids(?string $value): array
{
    return array_values(array_unique(array_filter(
        array_map('trim', explode(',', $value ?? '')),
        static fn ($id) => $id !== '',
    )));
}

function grocery_offer_date(?string $value): ?DateTimeImmutable
{
    if ($value === null || !preg_match('/^\d{4}-\d{2}-\d{2}$/D', $value)) {
        return null;
    }
    $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value, new DateTimeZone('Europe/Copenhagen'));
    return $date && $date->format('Y-m-d') === $value ? $date : null;
}

function grocery_offer_today(): string
{
    return (new DateTimeImmutable('today', new DateTimeZone('Europe/Copenhagen')))->format('Y-m-d');
}

/** Legacy weekday abbreviations remain active, matching Android's fallback behavior. */
function grocery_offer_is_future(array $item, ?string $today = null): bool
{
    $date = grocery_offer_date($item['weekday'] ?? null);
    return $date !== null && $date->format('Y-m-d') > ($today ?? grocery_offer_today());
}

function grocery_offer_date_label(?string $value): string
{
    $date = grocery_offer_date($value);
    if ($date === null) {
        return $value ?? '';
    }
    $days = [1 => 'Ma', 'Ti', 'On', 'To', 'Fr', 'Lø', 'Sø'];
    return $days[(int) $date->format('N')] . ' ' . $date->format('d/m/Y');
}

/** Validate browser input before persisting the same fields used by Android. */
function grocery_offer_fields_from_post(array $post, array $allowedShopIds, ?array $existing = null): array
{
    foreach (['price', 'weekday'] as $field) {
        if (isset($post[$field]) && !is_string($post[$field])) {
            throw new InvalidArgumentException('Indtast en gyldig pris og startdato.');
        }
    }
    $price = trim(is_string($post['price'] ?? null) ? $post['price'] : '');
    if ($price !== '' && (!preg_match('/^\d+(?:[.,]\d{1,2})?$/D', $price) || strlen($price) > 100)) {
        throw new InvalidArgumentException('Indtast en gyldig pris, fx 12,95.');
    }
    $weekday = trim(is_string($post['weekday'] ?? null) ? $post['weekday'] : '');
    if ($weekday !== '' && grocery_offer_date($weekday) === null) {
        throw new InvalidArgumentException('Vælg en gyldig startdato for tilbuddet.');
    }
    $postedIds = $post['shops'] ?? [];
    if (!is_array($postedIds)) {
        throw new InvalidArgumentException('Vælg gyldige butikker.');
    }
    // Keep tags from another list member selectable when editing their item.
    $allowed = array_merge($allowedShopIds, grocery_shop_ids($existing['supermarket'] ?? null));
    $ids = [];
    foreach ($postedIds as $id) {
        if (!is_string($id) || !in_array($id, $allowed, true)) {
            throw new InvalidArgumentException('Vælg gyldige butikker.');
        }
        $ids[] = $id;
    }
    $selection = implode(',', array_unique($ids));
    return [
        'price' => $price !== '' ? $price : null,
        'weekday' => $weekday !== '' ? $weekday : null,
        'supermarket' => $selection !== '' ? $selection : null,
    ];
}
