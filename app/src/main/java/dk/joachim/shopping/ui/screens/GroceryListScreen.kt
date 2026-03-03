package dk.joachim.shopping.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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

private fun String.toColor(): Color = try {
    Color(this.toColorInt())
} catch (e: Exception) {
    Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListScreen(
    viewModel: GroceryListViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val list = uiState.list
    val items = list?.items ?: emptyList()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
                            imageVector = Icons.Default.ArrowBack,
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
                                            if (url != null) {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
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

    // Build an order map: categoryId → position. Unknown IDs sort to the end.
    val categoryOrder = userCategories.mapIndexed { i, c -> c.id to i }.toMap()
    val categoryMap = userCategories.associateBy { it.id }
    val grouped: Map<String, List<GroceryItem>> = unchecked
        .sortedWith(compareBy(
            { categoryOrder[it.category] ?: Int.MAX_VALUE },
            { it.name }
        ))
        .groupBy { it.category }

    var checkedSectionExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Active items grouped by category ──────────────────────────────
        grouped.forEach { (categoryId, categoryItems) ->
            val userCategory = categoryMap[categoryId]
            item(key = "header_$categoryId") {
                CategoryHeader(
                    name = userCategory?.name ?: if (categoryId.isBlank()) "Uden kategori" else "Andet",
                )
            }
            items(categoryItems, key = { it.id }) { item ->
                SwipableGroceryItem(
                    item = item,
                    shops = shops,
                    onToggle = { onToggle(item.id) },
                    onDelete = { onDelete(item.id) },
                    onUpdateName = { onUpdateName(item.id, it) },
                    onUpdateWeekday = { onUpdateWeekday(item.id, it) },
                    onUpdatePrice = { onUpdatePrice(item.id, it) },
                    onUpdateSupermarket = { onUpdateSupermarket(item.id, it) },
                    onAdjustQuantity = { onAdjustQuantity(item.id, it) },
                    onSetQuantity = { onSetQuantity(item.id, it) },
                    onUpdateComment = { onUpdateComment(item.id, it) },
                )
            }
            item(key = "spacer_$categoryId") {
                Spacer(modifier = Modifier.height(2.dp))
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
                items(checked, key = { it.id }) { item ->
                    SwipableGroceryItem(
                        item = item,
                        shops = shops,
                        onToggle = { onToggle(item.id) },
                        onDelete = { onDelete(item.id) },
                        onUpdateName = { onUpdateName(item.id, it) },
                        onUpdateWeekday = { onUpdateWeekday(item.id, it) },
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
            .padding(top = 12.dp, bottom = 4.dp),
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
                text = "Completed ($count)",
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
private fun CategoryHeader(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipableGroceryItem(
    item: GroceryItem,
    shops: List<Shop>,
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onToggle()
            }
            false // always snap back — item stays in list
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val triggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val color by animateColorAsState(
                targetValue = if (triggered)
                    if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                val iconAlpha by animateFloatAsState(
                    targetValue = if (triggered) 1f else 0f,
                    label = "swipe_icon_alpha"
                )
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.Close else Icons.Default.CheckCircle,
                    contentDescription = if (item.isChecked) "Uncheck" else "Complete",
                    tint = if (item.isChecked)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = iconAlpha)
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = iconAlpha),
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    ) {
        GroceryItemCard(
            item = item,
            shops = shops,
            onToggle = onToggle,
            onDelete = onDelete,
            onUpdateName = onUpdateName,
            onUpdateWeekday = onUpdateWeekday,
            onUpdatePrice = onUpdatePrice,
            onUpdateSupermarket = onUpdateSupermarket,
            onAdjustQuantity = onAdjustQuantity,
            onSetQuantity = onSetQuantity,
            onUpdateComment = onUpdateComment,
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryItemCard(
    item: GroceryItem,
    shops: List<Shop>,
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

    val contentAlpha = if (item.isChecked) 0.4f else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!item.isChecked) {
                        showQuantityControls = !showQuantityControls
                        if (showQuantityControls) interactionTick++
                    }
                }
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
                            label = shop?.name ?: shopId,
                            alpha = contentAlpha,
                            backgroundColor = bgColor?.copy(alpha = contentAlpha * 0.9f),
                            contentColor = fgColor.copy(alpha = contentAlpha),
                        )
                    }
                    item.weekday?.let { day ->
                        DetailPill(
                            icon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(10.dp)) },
                            label = day,
                            alpha = contentAlpha,
                            backgroundColor = backgroundColor,
                            textStyle = MaterialTheme.typography.labelMedium,
                        )
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
                            icon = { Icon(Icons.Default.Notes, null, modifier = Modifier.size(10.dp)) },
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
    var supermarketDropdownExpanded by remember { mutableStateOf(false) }

    // Keep quantity text in sync when +/- buttons update it via the item
    LaunchedEffect(item.quantity) { quantityText = item.quantity }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rediger vare") },
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
                        if (value.isNotBlank()) onUpdateName(value)
                    },
                    label = { Text("Navn") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    onValueChange = { value ->
                        quantityText = value
                        onSetQuantity(value)
                    },
                    label = { Text("Mængde") },
                    leadingIcon = {
                        IconButton(
                            onClick = { onAdjustQuantity(-1) },
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
                        IconButton(onClick = { onAdjustQuantity(1) }) {
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
                        text = "Dag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEKDAYS.forEach { day ->
                        val selected = item.weekday == day
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onUpdateWeekday(if (selected) null else day) }
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
                        onUpdateComment(value.ifBlank { null })
                    },
                    label = { Text("Kommentar") },
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
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
                        val selectedShop = shops.firstOrNull { it.id == item.supermarket }
                        OutlinedTextField(
                            value = selectedShop?.name ?: (item.supermarket ?: ""),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Butik") },
                            leadingIcon = {
                                Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            if (item.supermarket != null) {
                                DropdownMenuItem(
                                    text = { Text("— Ryd", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        onUpdateSupermarket(null)
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
                                        onUpdateSupermarket(shop.id)
                                        supermarketDropdownExpanded = false
                                    },
                                    trailingIcon = if (item.supermarket == shop.id) ({
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
                        onUpdatePrice(value.ifBlank { null })
                    },
                    label = { Text("Pris") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp))
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
            TextButton(onClick = onDismiss) { Text("Færdig") }
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
            tint =if (isSystemInDarkTheme()) Color.White else Color(0xFF461264),
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
    data class Suggestion(val name: String, val categoryId: String, val fromCatalog: Boolean, val source: Any)
    val suggestions = remember(name, catalogItems, existingItems) {
        if (name.isBlank()) emptyList()
        else {
            val catalogMatches = catalogItems
                .filter { it.name.contains(name.trim(), ignoreCase = true) }
                .map { Suggestion(it.name, it.category, true, it) }
            val catalogNames = catalogMatches.map { it.name.lowercase() }.toSet()
            val listMatches = existingItems
                .filter { it.name.contains(name.trim(), ignoreCase = true) && it.name.lowercase() !in catalogNames }
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
                    onExpandedChange = { categoryExpanded = if (userCategories.isNotEmpty()) it else false }
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
