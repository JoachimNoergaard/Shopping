package dk.joachim.shopping.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.joachim.shopping.R
import dk.joachim.shopping.data.CatalogItem
import dk.joachim.shopping.data.GroceryItem
import dk.joachim.shopping.data.Shop
import dk.joachim.shopping.data.UserCategory
import dk.joachim.shopping.data.WEEKDAYS
import dk.joachim.shopping.data.dateToWeekdayAbbr
import dk.joachim.shopping.data.isFutureWeekday
import dk.joachim.shopping.data.weekdayNameToNextDate
import kotlinx.coroutines.flow.first

private fun String.toColor(): Color = try {
    Color(this.toColorInt())
} catch (_: Exception) {
    Color.Gray
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListScreen(
    viewModel: GroceryListViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val list = uiState.list
    val items = list?.items ?: emptyList()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = list?.name ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tilbage",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (list != null) {
                        IconButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("mobilepay://"))
                                )
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_mp_blue_mono),
                                contentDescription = "Åbn MobilePay",
                                tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        var overflowExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Mere"
                                )
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false }
                            ) {
                                listOf(
                                    "eTilbudsavis" to "https://etilbudsavis.dk/soeg",
                                    "Rema 1000" to "https://rema1000.dk/avis",
                                    "365 Discount" to "https://365discount.coop.dk/365avis/",
                                    "Netto" to "https://netto.dk/netto-avisen",
                                    "Føtex" to "https://www.foetex.dk/foetex-avis/",
                                    "Bilka" to "https://www.bilka.dk/bilkaavisen/",
                                ).forEach { (label, url) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            overflowExpanded = false
                                            try {
                                                val intent =
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        .addCategory(Intent.CATEGORY_BROWSABLE)
                                                context.startActivity(intent)
                                            } catch (_: ActivityNotFoundException) {
                                                Toast.makeText(
                                                    context,
                                                    "$label er ikke installeret",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }, floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddItemDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            GroceryItemList(
                items = items,
                userCategories = uiState.userCategories,
                shops = uiState.shops,
                itemAddedCount = uiState.itemAddedCount,
                initialSyncDone = uiState.initialSyncDone,
                onToggle = viewModel::toggleItem,
                onDelete = viewModel::deleteItem,
                onRemoveAllChecked = viewModel::deleteCheckedItems,
                onUpdateName = viewModel::updateItemName,
                onUpdateWeekday = viewModel::updateItemWeekday,
                onUpdatePrice = viewModel::updateItemPrice,
                onUpdateSupermarket = viewModel::updateItemSupermarket,
                onAdjustQuantity = viewModel::adjustItemQuantity,
                onSetQuantity = viewModel::setItemQuantity,
                onUpdateComment = viewModel::updateItemComment,
                contentPadding = paddingValues
            )
        }
    }

    if (uiState.showAddItemDialog) {
        AddGroceryItemDialog(
            name = uiState.newItemName,
            quantity = uiState.newItemQuantity,
            category = uiState.newItemCategory,
            userCategories = uiState.userCategories,
            existingItems = items,
            catalogItems = uiState.catalogItems,
            itemAddedCount = uiState.itemAddedCount,
            onNameChange = viewModel::updateNewItemName,
            onQuantityChange = viewModel::updateNewItemQuantity,
            onCategoryChange = viewModel::updateNewItemCategory,
            onFillFromSuggestion = viewModel::fillFromSuggestion,
            onFillFromCatalogSuggestion = viewModel::fillFromCatalogSuggestion,
            onConfirm = viewModel::addItem,
            onDismiss = viewModel::dismissAddItemDialog
        )
    }

}

@Composable
private fun GroceryItemList(
    items: List<GroceryItem>,
    userCategories: List<UserCategory>,
    shops: List<Shop>,
    itemAddedCount: Int,
    initialSyncDone: Boolean,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRemoveAllChecked: () -> Unit,
    onUpdateName: (String, String) -> Unit,
    onUpdateWeekday: (String, String?) -> Unit,
    onUpdatePrice: (String, String?) -> Unit,
    onUpdateSupermarket: (String, String?) -> Unit,
    onAdjustQuantity: (String, Int) -> Unit,
    onSetQuantity: (String, String) -> Unit,
    onUpdateComment: (String, String?) -> Unit,
    contentPadding: PaddingValues
) {
    val unchecked = items.filter { !it.isChecked }
    val checked = items.filter { it.isChecked }.sortedByDescending { it.checkedAt }

    val futureItems = unchecked.filter { it.weekday != null && isFutureWeekday(it.weekday) }
    val currentItems = unchecked.filter { it.weekday == null || !isFutureWeekday(it.weekday) }

    // Build an order map: categoryId → position. Unknown IDs sort to the end.
    val categoryOrder = userCategories.mapIndexed { i, c -> c.id to i }.toMap()
    val categoryMap = userCategories.associateBy { it.id }
    val grouped: Map<String, List<GroceryItem>> = currentItems
        .sortedWith(
            compareBy(
                { categoryOrder[it.category] ?: Int.MAX_VALUE },
                { it.name }
            ))
        .groupBy { it.category }

    var checkedSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var futureItemsExpanded by rememberSaveable { mutableStateOf(false) }
    var scrollToFutureItemId by remember { mutableStateOf<String?>(null) }

    val listState = remember { LazyListState() }
    LaunchedEffect(initialSyncDone) {
        if (initialSyncDone) listState.scrollToItem(0)
    }
    LaunchedEffect(itemAddedCount) {
        if (itemAddedCount > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(scrollToFutureItemId, futureItems) {
        val targetId = scrollToFutureItemId ?: return@LaunchedEffect
        val targetIndexInFuture = futureItems.indexOfFirst { it.id == targetId }
        if (targetIndexInFuture < 0) return@LaunchedEffect
        futureItemsExpanded = true
        val futureHeaderIndex = grouped.entries.sumOf { 2 + it.value.size }
        val targetIndex = futureHeaderIndex + 1 + targetIndexInFuture
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > targetIndex }
        listState.animateScrollToItem(targetIndex)
        scrollToFutureItemId = null
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
    ) {
        // ── Active items grouped by category ──────────────────────────────
        grouped.forEach { (categoryId, categoryItems) ->
            val userCategory = categoryMap[categoryId]
            item(key = "header_$categoryId") {
                CategoryHeader(
                    name = userCategory?.name
                        ?: if (categoryId.isBlank()) "Uden kategori" else "Andet",
                )
            }
            itemsIndexed(categoryItems, key = { _, it -> it.id }) { index, item ->
                GroceryItemCard(
                    item = item,
                    shops = shops,
                    isFirst = index == 0,
                    isLast = index == categoryItems.lastIndex,
                    onToggle = { onToggle(item.id) },
                    onDelete = { onDelete(item.id) },
                    onUpdateName = { onUpdateName(item.id, it) },
                    onUpdateWeekday = { weekday ->
                        onUpdateWeekday(item.id, weekday)
                        if (weekday != null && isFutureWeekday(weekday)) {
                            scrollToFutureItemId = item.id
                        }
                    },
                    onUpdatePrice = { onUpdatePrice(item.id, it) },
                    onUpdateSupermarket = { onUpdateSupermarket(item.id, it) },
                    onAdjustQuantity = { onAdjustQuantity(item.id, it) },
                    onSetQuantity = { onSetQuantity(item.id, it) },
                    onUpdateComment = { onUpdateComment(item.id, it) },
                )
            }
            item(key = "spacer_$categoryId") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── Future items section ──────────────────────────────────────────
        if (futureItems.isNotEmpty()) {
            item(key = "future_header") {
                FutureSectionHeader(
                    count = futureItems.size,
                    expanded = futureItemsExpanded,
                    onToggleExpanded = { futureItemsExpanded = !futureItemsExpanded }
                )
            }
            if (futureItemsExpanded) {
                itemsIndexed(futureItems, key = { _, it -> it.id }) { index, item ->
                    GroceryItemCard(
                        item = item,
                        shops = shops,
                        isFirst = index == 0,
                        isLast = index == futureItems.lastIndex,
                        onToggle = { onToggle(item.id) },
                        onDelete = { onDelete(item.id) },
                        onUpdateName = { onUpdateName(item.id, it) },
                        onUpdateWeekday = { onUpdateWeekday(item.id, it) },
                        onUpdatePrice = { onUpdatePrice(item.id, it) },
                        onUpdateSupermarket = { onUpdateSupermarket(item.id, it) },
                        onAdjustQuantity = { onAdjustQuantity(item.id, it) },
                        onSetQuantity = { onSetQuantity(item.id, it) },
                        onUpdateComment = { onUpdateComment(item.id, it) },
                        isFuture = true,
                    )
                }
            }
        }

        // ── Checked / completed section ───────────────────────────────────
        if (checked.isNotEmpty()) {
            item(key = "checked_header") {
                CompletedSectionHeader(
                    count = checked.size,
                    expanded = checkedSectionExpanded,
                    onToggleExpanded = { checkedSectionExpanded = !checkedSectionExpanded },
                    onRemoveAll = onRemoveAllChecked
                )
            }
            if (checkedSectionExpanded) {
                itemsIndexed(checked, key = { _, it -> it.id }) { index, item ->
                    GroceryItemCard(
                        item = item,
                        shops = shops,
                        isFirst = index == 0,
                        isLast = index == checked.lastIndex,
                        onToggle = { onToggle(item.id) },
                        onDelete = { onDelete(item.id) },
                        onUpdateName = { onUpdateName(item.id, it) },
                        onUpdateWeekday = { weekday ->
                            onUpdateWeekday(item.id, weekday)
                            if (weekday != null && isFutureWeekday(weekday)) {
                                scrollToFutureItemId = item.id
                            }
                        },
                        onUpdatePrice = { onUpdatePrice(item.id, it) },
                        onUpdateSupermarket = { onUpdateSupermarket(item.id, it) },
                        onAdjustQuantity = { onAdjustQuantity(item.id, it) },
                        onSetQuantity = { onSetQuantity(item.id, it) },
                        onUpdateComment = { onUpdateComment(item.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRemoveAll: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "completed_chevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        TextButton(
            onClick = onToggleExpanded,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Afkrydset ($count)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronRotation)
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        if (expanded) {
            TextButton(
                onClick = onRemoveAll,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Fjern alle",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FutureSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "future_chevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        TextButton(
            onClick = onToggleExpanded,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (expanded) "Skjul kommende varer" else "Du har $count kommende varer",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Skjul" else "Vis",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronRotation)
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun CategoryHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun GroceryItemCard(
    item: GroceryItem,
    shops: List<Shop>,
    isFirst: Boolean,
    isLast: Boolean,
    isFuture: Boolean = false,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateWeekday: (String?) -> Unit,
    onUpdatePrice: (String?) -> Unit,
    onUpdateSupermarket: (String?) -> Unit,
    onAdjustQuantity: (Int) -> Unit,
    onSetQuantity: (String) -> Unit,
    onUpdateComment: (String?) -> Unit,

    ) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showQuantityControls by rememberSaveable(item.id) { mutableStateOf(false) }
    var interactionTick by remember { mutableStateOf(0) }

    LaunchedEffect(interactionTick) {
        if (showQuantityControls) {
            kotlinx.coroutines.delay(2_000)
            showQuantityControls = false
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (item.isChecked)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface,
        label = "card_bg"
    )

    val haptic = LocalHapticFeedback.current
    val contentAlpha = if (item.isChecked) 0.4f else 1f

    val cornerRadius = 12.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) cornerRadius else 0.dp,
        topEnd = if (isFirst) cornerRadius else 0.dp,
        bottomStart = if (isLast) cornerRadius else 0.dp,
        bottomEnd = if (isLast) cornerRadius else 0.dp,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 0.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (!item.isChecked) {
                            showQuantityControls = !showQuantityControls
                            if (showQuantityControls) interactionTick++
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
                )
                .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.quantity == "1") item.name else "${item.name} x ${item.quantity}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    item.supermarket?.let { shopId ->
                        val shop = shops.firstOrNull { it.id == shopId }
                        val bgColor = shop?.backgroundColor?.toColor()
                        val fgColor = (shop?.foregroundColor?.toColor() ?: Color.White)
                        DetailPill(
                            icon = {},
                            label = shop?.name ?: "Special",
                            alpha = contentAlpha,
                            backgroundColor = bgColor?.copy(alpha = contentAlpha * 0.9f),
                            contentColor = fgColor.copy(alpha = contentAlpha),
                        )
                    }
                    item.weekday?.let { day ->
                        if (isFuture) {
                            DetailPill(
                                icon = {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        null,
                                        modifier = Modifier.size(10.dp)
                                    )
                                },
                                label = dateToWeekdayAbbr(day),
                                alpha = contentAlpha,
                                backgroundColor = backgroundColor,
                                textStyle = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    item.price?.let { price ->
                        DetailPill(
                            icon = { },
                            label = "$price kr.",
                            alpha = contentAlpha,
                            backgroundColor = backgroundColor,
                            textStyle = MaterialTheme.typography.labelMedium,
                        )
                    }
                    item.comment?.let { comment ->
                        DetailPill(
                            icon = {
                                Icon(
                                    Icons.Default.Notes,
                                    null,
                                    modifier = Modifier.size(10.dp)
                                )
                            },
                            label = comment,
                            alpha = contentAlpha,
                            backgroundColor = backgroundColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            if (!item.isChecked) {
                AnimatedVisibility(visible = showQuantityControls) {
                    Row {
                        IconButton(
                            onClick = { onAdjustQuantity(-1); interactionTick++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease quantity",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onAdjustQuantity(1); interactionTick++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase quantity",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${item.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (!item.isChecked) {
                IconButton(
                    onClick = { showDetailsDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Detaljer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (!isLast) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (showDetailsDialog) {
        ItemDetailsDialog(
            item = item,
            shops = shops,
            onUpdateName = onUpdateName,
            onAdjustQuantity = onAdjustQuantity,
            onSetQuantity = onSetQuantity,
            onUpdateWeekday = onUpdateWeekday,
            onUpdatePrice = onUpdatePrice,
            onUpdateSupermarket = onUpdateSupermarket,
            onUpdateComment = onUpdateComment,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailsDialog(
    item: GroceryItem,
    shops: List<Shop>,
    onUpdateName: (String) -> Unit,
    onAdjustQuantity: (Int) -> Unit,
    onSetQuantity: (String) -> Unit,
    onUpdateWeekday: (String?) -> Unit,
    onUpdatePrice: (String?) -> Unit,
    onUpdateSupermarket: (String?) -> Unit,
    onUpdateComment: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameText by remember { mutableStateOf(item.name) }
    var quantityText by remember { mutableStateOf(item.quantity) }
    var priceText by remember { mutableStateOf(item.price ?: "") }
    var commentText by remember { mutableStateOf(item.comment ?: "") }
    var pendingWeekday by remember { mutableStateOf(item.weekday) }
    var pendingSupermarket by remember { mutableStateOf(item.supermarket) }
    var supermarketDropdownExpanded by remember { mutableStateOf(false) }

    fun commit() {
        if (nameText.isNotBlank()) onUpdateName(nameText)
        onSetQuantity(quantityText)
        onUpdateWeekday(pendingWeekday)
        onUpdatePrice(priceText.ifBlank { null })
        onUpdateComment(commentText.ifBlank { null })
        onUpdateSupermarket(pendingSupermarket)
        onDismiss()
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = "Rediger vare"
                )
                IconButton(onClick = {
                    val encoded = Uri.encode(item.name)
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://etilbudsavis.dk/soeg/$encoded")
                    )
                    context.startActivity(intent)
                }) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_etilbudsavis),
                        contentDescription = "Søg på eTilbudsavis"
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {

                // ── Name ──────────────────────────────────────────────────
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { value ->
                        nameText = value
                    },
                    label = { Text("Navn") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (nameText.isNotEmpty()) {
                            IconButton(onClick = { nameText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Ryd navn")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )

                // ── Quantity stepper ──────────────────────────────────────
                val qty = quantityText.trimStart().takeWhile { it.isDigit() }.toIntOrNull() ?: 1
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { value -> quantityText = value },
                    label = { Text("Mængde") },
                    leadingIcon = {
                        IconButton(
                            onClick = { if (qty > 1) quantityText = (qty - 1).toString() },
                            enabled = qty > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease quantity",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { quantityText = (qty + 1).toString() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase quantity",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )

                // ── Weekday selector ──────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dag, hvor tilbuddet starter",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEKDAYS.forEach { day ->
                        val selected = pendingWeekday?.let { dateToWeekdayAbbr(it) } == day
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    pendingWeekday =
                                        if (selected) null else weekdayNameToNextDate(day)
                                }
                        ) {
                            Text(
                                text = day.take(1),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Comment ───────────────────────────────────────────────
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { value ->
                        commentText = value
                    },
                    label = { Text("Kommentar") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )

                // ── Supermarket ───────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = supermarketDropdownExpanded,
                    onExpandedChange = { supermarketDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedShop = shops.firstOrNull { it.id == pendingSupermarket }
                    OutlinedTextField(
                        value = selectedShop?.name ?: (pendingSupermarket ?: ""),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Butik") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Store,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = supermarketDropdownExpanded)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = supermarketDropdownExpanded,
                        onDismissRequest = { supermarketDropdownExpanded = false }
                    ) {
                        if (pendingSupermarket != null) {
                            DropdownMenuItem(
                                text = { Text("— Ryd", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    pendingSupermarket = null
                                    supermarketDropdownExpanded = false
                                }
                            )
                        }
                        shops.forEach { shop ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(shop.backgroundColor.toColor())
                                        )
                                        Text(shop.name)
                                    }
                                },
                                onClick = {
                                    pendingSupermarket = shop.id
                                    supermarketDropdownExpanded = false
                                },
                                trailingIcon = if (pendingSupermarket == shop.id) ({
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }) else null
                            )
                        }
                    }
                }

                // ── Price ─────────────────────────────────────────────────
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { value ->
                        priceText = value
                    },
                    label = { Text("Pris") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::commit) { Text("Færdig") }
        }
    )
}


@Composable
private fun DetailPill(
    icon: @Composable () -> Unit,
    label: String,
    alpha: Float,
    backgroundColor: Color? = null,
    contentColor: Color? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    val bgColor =
        backgroundColor ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alpha * 1f)
    val fgColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha)

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides fgColor) {
                icon()
            }
            Text(
                text = label,
                style = textStyle,
                color = fgColor,
                maxLines = 1
            )
        }
    }
}


@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shark_shopper),
            contentDescription = null,
            tint = if (isSystemInDarkTheme()) Color.White else Color(0xFF461264),
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Din liste er tom",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + for at tilføje en vare",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroceryItemDialog(
    name: String,
    quantity: String,
    category: UserCategory?,
    userCategories: List<UserCategory>,
    existingItems: List<GroceryItem>,
    catalogItems: List<CatalogItem>,
    itemAddedCount: Int,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCategoryChange: (UserCategory) -> Unit,
    onFillFromSuggestion: (GroceryItem) -> Unit,
    onFillFromCatalogSuggestion: (CatalogItem) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var suggestionsExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Fires on initial open (itemAddedCount == 0) and again after each successful add
    LaunchedEffect(itemAddedCount) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Catalog items matching the typed name are shown first.
    // Items from the current list that aren't in the catalog fill the remainder.
    data class Suggestion(
        val name: String,
        val categoryId: String,
        val fromCatalog: Boolean,
        val source: Any
    )

    val suggestions = remember(name, catalogItems, existingItems) {
        if (name.isBlank()) emptyList()
        else {
            val catalogMatches = catalogItems
                .filter { it.name.contains(name.trim(), ignoreCase = true) }
                .map { Suggestion(it.name, it.category, true, it) }
            val catalogNames = catalogMatches.map { it.name.lowercase() }.toSet()
            val listMatches = existingItems
                .filter {
                    it.name.contains(
                        name.trim(),
                        ignoreCase = true
                    ) && it.name.lowercase() !in catalogNames
                }
                .distinctBy { it.name }
                .map { Suggestion(it.name, it.category, false, it) }
            (catalogMatches + listMatches).take(6)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_items)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Name field with autocomplete ──────────────────────────
                ExposedDropdownMenuBox(
                    expanded = suggestionsExpanded && suggestions.isNotEmpty(),
                    onExpandedChange = { suggestionsExpanded = it }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            onNameChange(it)
                            suggestionsExpanded = true
                        },
                        label = { Text(stringResource(R.string.name)) },
                        trailingIcon = {
                            if (name.isNotEmpty()) {
                                IconButton(onClick = { onNameChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear name"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .focusRequester(focusRequester)
                    )
                    ExposedDropdownMenu(
                        expanded = suggestionsExpanded && suggestions.isNotEmpty(),
                        onDismissRequest = { suggestionsExpanded = false }
                    ) {
                        suggestions.forEach { suggestion ->
                            val categoryName = userCategories
                                .firstOrNull { it.id == suggestion.categoryId }?.name
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (categoryName != null) {
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (suggestion.fromCatalog) {
                                        onFillFromCatalogSuggestion(suggestion.source as CatalogItem)
                                    } else {
                                        onFillFromSuggestion(suggestion.source as GroceryItem)
                                    }
                                    suggestionsExpanded = false
                                }
                            )
                        }
                    }
                }

                // ── Quantity ──────────────────────────────────────────────
                val qty = quantity.toIntOrNull() ?: 1
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { v ->
                        val filtered = v.filter { it.isDigit() }
                        onQuantityChange(if (filtered.isEmpty()) "1" else filtered)
                    },
                    label = { Text(stringResource(R.string.quantity)) },
                    leadingIcon = {
                        IconButton(
                            onClick = { onQuantityChange((qty - 1).coerceAtLeast(1).toString()) },
                            enabled = qty > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease quantity",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { onQuantityChange((qty + 1).toString()) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase quantity",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Category ──────────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = {
                        categoryExpanded = if (userCategories.isNotEmpty()) it else false
                    }
                ) {
                    OutlinedTextField(
                        value = category?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category_required)) },
                        placeholder = {
                            Text(
                                if (userCategories.isEmpty()) "Opret kategorier under Indstillinger"
                                else stringResource(R.string.choose_category)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        userCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    onCategoryChange(cat)
                                    categoryExpanded = false
                                },
                                trailingIcon = if (cat.id == category?.id) ({
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }) else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.isNotBlank() && (category != null || userCategories.isEmpty())
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
