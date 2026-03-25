package dk.joachim.shopping.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.ContentPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.joachim.shopping.R
import dk.joachim.shopping.data.GroceryList
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.ShoppingRepository.LAST_MAIN_SECTION_COOKING
import dk.joachim.shopping.data.ShoppingRepository.LAST_MAIN_SECTION_GROCERY_HOME

@Suppress("LongParameterList", "LongMethod", "FunctionNaming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListsScreen(
    viewModel: GroceryListsViewModel = viewModel(),
    onNavigateToList: (String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToShops: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember {
        mutableIntStateOf(
            if (ShoppingRepository.getLastMainSection() == LAST_MAIN_SECTION_COOKING) 1 else 0
        )
    }

    val cookingViewModel: CookingViewModel = viewModel()
    val cookingUiState by cookingViewModel.uiState.collectAsStateWithLifecycle()
    val isRecipeFullScreen = cookingUiState.viewingRecipe != null || cookingUiState.editingRecipe != null

    // Navigate after a successful list join
    LaunchedEffect(uiState.navigateToListId) {
        uiState.navigateToListId?.let { id ->
            viewModel.consumeNavigation()
            onNavigateToList(id)
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            cookingViewModel.syncMenuPlansFromServer()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!(selectedTab == 1 && isRecipeFullScreen)) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedTab == 1) "CookingShark"
                                else stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) Color.White else Color(LOGO_COLOR)
                        )
                    },
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.ic_shark_shopper),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                color = if (isSystemInDarkTheme()) Color.White else Color(LOGO_COLOR),
                                blendMode = BlendMode.SrcIn,
                            ),
                            modifier = Modifier
                                .padding(start = 16.dp, end = 8.dp)
                                .width(50.dp)
                                .height(52.dp),
                        )
                    },
                    actions = {
                        if (selectedTab != 1) {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Mere"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.profil)) },
                                        onClick = { showMenu = false; onNavigateToProfile() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.categories)) },
                                        onClick = { showMenu = false; onNavigateToCategories() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.goods)) },
                                        onClick = { showMenu = false; onNavigateToCatalog() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.shops)) },
                                        onClick = { showMenu = false; onNavigateToShops() }
                                    )
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
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = viewModel::showAddListDialog,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New list")
                }
            }
        },
        bottomBar = {
            if (!(selectedTab == 1 && isRecipeFullScreen)) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            ShoppingRepository.saveLastMainSection(LAST_MAIN_SECTION_GROCERY_HOME)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Indkøb"
                            )
                        },
                        label = { Text("Indkøb") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            ShoppingRepository.saveLastMainSection(LAST_MAIN_SECTION_COOKING)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "Madlavning"
                            )
                        },
                        label = { Text("Madlavning") }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (selectedTab == 1) {
            CookingScreen(viewModel = cookingViewModel, paddingValues = paddingValues)
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.lists.isEmpty()) {
            EmptyListsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 80.dp
                )
            ) {
                itemsIndexed(uiState.lists, key = { _, it -> it.id }) { index, list ->
                    ListCard(
                        list = list,
                        isFirst = index == 0,
                        isLast = index == uiState.lists.lastIndex,
                        onClick = { onNavigateToList(list.id) },
                        onEdit = { viewModel.openEditListDialog(list) }
                    )
                }
            }
        }
    }

    if (uiState.showAddListDialog) {
        AddListDialog(
            name = uiState.newListName,
            onNameChange = viewModel::updateNewListName,
            onConfirm = viewModel::addList,
            onDismiss = viewModel::dismissAddListDialog
        )
    }

    uiState.editingList?.let { list ->
        val isOwner = list.ownerId == uiState.currentProfileId
        EditListDialog(
            name = uiState.editListName,
            isOwner = isOwner,
            originalName = list.name,
            onNameChange = viewModel::updateEditListName,
            onSave = viewModel::saveEditedListName,
            onDeleteRequest = viewModel::requestDeleteFromEditDialog,
            onDismiss = viewModel::dismissEditListDialog
        )
    }

    uiState.pendingDeleteList?.let { list ->
        val isOwner = list.ownerId == uiState.currentProfileId
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = {
                Text(
                    if (isOwner) stringResource(R.string.delete_list)
                    else stringResource(R.string.leave_list)
                )
            },
            text = {
                Text(
                    if (isOwner)
                        "\"${list.name}\" slettes for alle. Dette kan ikke fortrydes."
                    else
                        "Du forlader \"${list.name}\". Listen slettes ikke for andre."
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteList,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isOwner) "Slet" else "Forlad")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Annuller")
                }
            }
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun ListCard(
    list: GroceryList,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val totalItems = list.items.size
    val remaining = list.items.count { !it.isChecked }

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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        totalItems == 0 -> "Ingen varer"
                        remaining == 0 -> "$totalItems varer • Færdig ✓"
                        else -> "$totalItems varer • $remaining tilbage"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining == 0 && totalItems > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rediger ${list.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
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
}

@Suppress("FunctionNaming")
@Composable
private fun EmptyListsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shark_shopper),
            contentDescription = null,
            tint = if (isSystemInDarkTheme()) Color.White else Color(LOGO_COLOR),
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ingen lister endnu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tryk + for at oprette din første liste",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun EditListDialog(
    name: String,
    isOwner: Boolean,
    originalName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDismiss: () -> Unit,
) {
    val trimmed = name.trim()
    val canSave = isOwner && trimmed.isNotBlank() && trimmed != originalName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rediger liste") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Listenavn") },
                    readOnly = !isOwner,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isOwner) {
                    Text(
                        text = "Kun ejeren af listen kan ændre navnet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onDeleteRequest,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 0.dp,
                        vertical = ContentPadding.calculateTopPadding()
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwner) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(if (isOwner) "Slet liste" else "Forlad liste …")
                    }
                }
            }
        },
        confirmButton = {
            if (isOwner) {
                Button(onClick = onSave, enabled = canSave) {
                    Text("Gem")
                }
            } else {
                Box {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun AddListDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny indkøbsliste") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Listenavn") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text("Opret")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}

const val LOGO_COLOR = 0xFF461264
