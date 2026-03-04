package dk.joachim.shopping.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

    // Navigate after a successful list join
    LaunchedEffect(uiState.navigateToListId) {
        uiState.navigateToListId?.let { id ->
            viewModel.consumeNavigation()
            onNavigateToList(id)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
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
                onClick = viewModel::showAddListDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New list")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 80.dp
                )
            ) {
                items(uiState.lists, key = { it.id }) { list ->
                    ListCard(
                        list = list,
                        onClick = { onNavigateToList(list.id) },
                        onDelete = { viewModel.requestDeleteList(list) }
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val totalItems = list.items.size
    val remaining = list.items.count { !it.isChecked }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            Column(modifier = Modifier.weight(1f)) {
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
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Slet ${list.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
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
