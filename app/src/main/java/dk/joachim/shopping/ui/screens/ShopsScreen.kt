package dk.joachim.shopping.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.joachim.shopping.data.SHOP_BACKGROUND_COLORS
import dk.joachim.shopping.data.Shop
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.setValue

private fun String.toColor(): Color = try {
    val c = AndroidColor.parseColor(this)
    Color(c)
} catch (e: Exception) {
    Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsScreen(
    viewModel: ShopsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Butikker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Tilbage")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAddDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tilføj butik")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.shops.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ingen butikker endnu.\nTryk + for at tilføje.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            else -> ReorderableShopList(
                shops = uiState.shops,
                onReorder = viewModel::reorder,
                onEdit = viewModel::startEdit,
                onDelete = viewModel::deleteShop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (uiState.showAddDialog) {
        ShopFormDialog(
            title = "Ny butik",
            name = uiState.dialogName,
            bgColor = uiState.dialogBgColor,
            fgColor = uiState.dialogFgColor,
            confirmLabel = "Tilføj",
            onNameChange = viewModel::updateDialogName,
            onBgColorChange = viewModel::updateDialogBgColor,
            onFgColorChange = viewModel::updateDialogFgColor,
            onConfirm = viewModel::addShop,
            onDismiss = viewModel::dismissDialog,
        )
    }

    if (uiState.editingShop != null) {
        ShopFormDialog(
            title = "Rediger butik",
            name = uiState.dialogName,
            bgColor = uiState.dialogBgColor,
            fgColor = uiState.dialogFgColor,
            confirmLabel = "Gem",
            onNameChange = viewModel::updateDialogName,
            onBgColorChange = viewModel::updateDialogBgColor,
            onFgColorChange = viewModel::updateDialogFgColor,
            onConfirm = viewModel::saveEdit,
            onDismiss = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun ReorderableShopList(
    shops: List<Shop>,
    onReorder: (Int, Int) -> Unit,
    onEdit: (Shop) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggingIndex by remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var draggingOffsetY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shops.forEachIndexed { index, shop ->
            val isDragging = index == draggingIndex
            val draggedTo = draggingIndex?.let { from ->
                (from + (draggingOffsetY / itemHeightPx).roundToInt()).coerceIn(0, shops.lastIndex)
            }
            val shiftY = when {
                draggingIndex == null || isDragging -> 0f
                draggingIndex!! < index && draggedTo != null && index <= draggedTo -> -itemHeightPx
                draggingIndex!! > index && draggedTo != null && index >= draggedTo -> itemHeightPx
                else -> 0f
            }
            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) draggingOffsetY else shiftY
                        shadowElevation = if (isDragging) 8f else 0f
                    }
            ) {
                ShopRow(
                    shop = shop,
                    onEdit = { onEdit(shop) },
                    onDelete = { onDelete(shop.id) },
                    dragHandleModifier = Modifier.pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = index; draggingOffsetY = 0f },
                            onDrag = { _, delta -> draggingOffsetY += delta.y },
                            onDragEnd = {
                                val from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                val to = (from + (draggingOffsetY / itemHeightPx).roundToInt())
                                    .coerceIn(0, shops.lastIndex)
                                if (from != to) onReorder(from, to)
                                draggingIndex = null; draggingOffsetY = 0f
                            },
                            onDragCancel = { draggingIndex = null; draggingOffsetY = 0f }
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.size(80.dp))
    }
}

@Composable
private fun ShopRow(
    shop: Shop,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Træk for at sortere",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = dragHandleModifier.size(24.dp)
            )

            // Preview tag
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = shop.backgroundColor.toColor(),
            ) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = shop.foregroundColor.toColor(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rediger ${shop.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Slet ${shop.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ShopFormDialog(
    title: String,
    name: String,
    bgColor: String,
    fgColor: String,
    confirmLabel: String,
    onNameChange: (String) -> Unit,
    onBgColorChange: (String) -> Unit,
    onFgColorChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Navn") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live preview
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eksempel:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = bgColor.toColor(),
                    ) {
                        Text(
                            text = name.ifBlank { "Butik" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = fgColor.toColor(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Background color palette
                Text("Baggrundsfarve", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(SHOP_BACKGROUND_COLORS) { color ->
                        val selected = color == bgColor
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color.toColor())
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { onBgColorChange(color) }
                        ) {
                            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = fgColor.toColor(), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Foreground color
                Text("Tekstfarve", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(SHOP_BACKGROUND_COLORS) { color ->
                        val selected = color == fgColor
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color.toColor())
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { onFgColorChange(color) }
                        ) {
                            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = bgColor.toColor(), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}
