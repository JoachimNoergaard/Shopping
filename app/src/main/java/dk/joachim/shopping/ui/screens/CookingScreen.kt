package dk.joachim.shopping.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import dk.joachim.shopping.data.IngredientSection
import dk.joachim.shopping.data.InstructionSection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.joachim.shopping.data.CompletedStep
import dk.joachim.shopping.data.Ingredient
import dk.joachim.shopping.data.MenuPlan
import dk.joachim.shopping.data.Recipe
import dk.joachim.shopping.data.RecipeStepTimer
import dk.joachim.shopping.data.RecipeStepTimerEntry
import dk.joachim.shopping.data.capitalizeIngredientFirstLetter
import dk.joachim.shopping.data.parseInstructionMinutes

@Suppress("LongMethod", "FunctionNaming")
@Composable
fun CookingScreen(
    viewModel: CookingViewModel = viewModel(),
    paddingValues: PaddingValues,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.editingRecipe != null) {
        BackHandler(onBack = viewModel::dismissRecipeEditor)
        RecipeEditorScreen(
            recipe = uiState.editingRecipe!!,
            onRecipeChange = viewModel::updateEditingRecipe,
            onSave = viewModel::saveEditingRecipe,
            onDismiss = viewModel::dismissRecipeEditor,
            onAddIngredientSection = viewModel::addIngredientSection,
            onRemoveIngredientSection = viewModel::removeIngredientSection,
            onUpdateIngredientSectionTitle = viewModel::updateIngredientSectionTitle,
            onAddIngredient = viewModel::addIngredient,
            onRemoveIngredient = viewModel::removeIngredient,
            onUpdateIngredient = viewModel::updateIngredient,
            onReorderIngredients = viewModel::reorderIngredients,
            onAddInstructionSection = viewModel::addInstructionSection,
            onRemoveInstructionSection = viewModel::removeInstructionSection,
            onUpdateInstructionSectionTitle = viewModel::updateInstructionSectionTitle,
            onAddInstructionStep = viewModel::addInstructionStep,
            onRemoveInstructionStep = viewModel::removeInstructionStep,
            onUpdateInstructionStep = viewModel::updateInstructionStep,
            onReorderInstructions = viewModel::reorderInstructions,
        )
        return
    }

    if (uiState.viewingRecipe != null) {
        BackHandler(onBack = viewModel::dismissRecipeViewer)
        RecipeDetailScreen(
            recipe = uiState.viewingRecipe!!,
            completedSteps = uiState.completedSteps,
            showStepProgress = uiState.viewingMenuPlanId != null,
            onToggleStep = viewModel::toggleStepCompletion,
            planServings = uiState.viewingMenuPlanId?.let { pid ->
                uiState.menuPlans.firstOrNull { it.id == pid }?.servings
            } ?: 0,
            showAddToPlan = uiState.viewingMenuPlanId == null,
            menuPlans = if (uiState.viewingMenuPlanId == null)
                uiState.menuPlans.filter { uiState.viewingRecipe!!.id !in it.recipeIds }
            else emptyList(),
            onAddToPlan = { planId ->
                viewModel.addRecipeToMenuPlan(planId, uiState.viewingRecipe!!.id)
            },
            onCreatePlanAndAdd = { name ->
                viewModel.createMenuPlanAndAddRecipe(name, uiState.viewingRecipe!!.id)
            },
            onEdit = viewModel::startEditingFromViewer,
            onDismiss = viewModel::dismissRecipeViewer,
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Søg i opskrifter") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Ryd")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.searchQuery.isBlank() && uiState.menuPlans.isNotEmpty()) {
                        itemsIndexed(
                            uiState.menuPlans,
                            key = { _, it -> "plan_${it.id}" }) { _, plan ->
                            MenuPlanCard(
                                plan = plan,
                                recipes = uiState.recipesForPlan(plan),
                                isExpanded = uiState.expandedPlanId == plan.id,
                                onToggleExpand = { viewModel.togglePlanExpanded(plan.id) },
                                onEditPlan = { viewModel.requestEditPlan(plan) },
                                onRemoveRecipe = { recipeId ->
                                    viewModel.requestRemoveRecipeFromMenuPlan(
                                        plan.id,
                                        recipeId
                                    )
                                },
                                onEditRecipe = { recipe ->
                                    viewModel.openRecipeViewer(
                                        recipe,
                                        plan.id
                                    )
                                },
                                groceryListName = uiState.targetGroceryListName,
                                canAddToGroceryList = uiState.canAddIngredientsToGroceryList,
                                onAddIngredientToGroceryList = viewModel::addMergedIngredientToGroceryList,
                            )
                        }
                    }

                    if (uiState.searchQuery.isNotBlank()) {
                        if (uiState.filteredRecipes.isEmpty()) {
                            item(key = "empty_search") {
                                EmptyMenuPlansState(
                                    isFiltered = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            itemsIndexed(
                                uiState.filteredRecipes,
                                key = { _, it -> "recipe_${it.id}" }) { index, recipe ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    SearchRecipeRow(
                                        recipe = recipe,
                                        onClick = { viewModel.openRecipeViewer(recipe) },
                                        onDelete = { viewModel.requestDeleteRecipe(recipe) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AddFab(
            onAddPlan = viewModel::showAddPlanDialog,
            onAddRecipe = viewModel::showAddRecipeDialog,
            onImportRecipe = viewModel::showImportRecipeDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
        )
    }

    if (uiState.showAddPlanDialog) {
        AddMenuPlanDialog(
            name = uiState.newPlanName,
            onNameChange = viewModel::updateNewPlanName,
            onConfirm = viewModel::addMenuPlan,
            onDismiss = viewModel::dismissAddPlanDialog
        )
    }

    if (uiState.showAddRecipeDialog) {
        AddRecipeDialog(
            name = uiState.newRecipeName,
            description = uiState.newRecipeDescription,
            onNameChange = viewModel::updateNewRecipeName,
            onDescriptionChange = viewModel::updateNewRecipeDescription,
            onConfirm = viewModel::addRecipe,
            onDismiss = viewModel::dismissAddRecipeDialog
        )
    }

    if (uiState.showImportRecipeDialog) {
        ImportRecipeDialog(
            name = uiState.importRecipeName,
            ingredients = uiState.importRecipeIngredients,
            instructions = uiState.importRecipeInstructions,
            onNameChange = viewModel::updateImportRecipeName,
            onIngredientsChange = viewModel::updateImportRecipeIngredients,
            onInstructionsChange = viewModel::updateImportRecipeInstructions,
            onConfirm = viewModel::importRecipe,
            onDismiss = viewModel::dismissImportRecipeDialog,
        )
    }

    uiState.pendingDeletePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeletePlanDialog,
            title = { Text("Slet madplan") },
            text = { Text("\"${plan.name}\" slettes. Dette kan ikke fortrydes.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeletePlan,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Slet") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeletePlanDialog) { Text("Annuller") }
            }
        )
    }

    if (uiState.editingPlan != null) {
        EditMenuPlanDialog(
            name = uiState.editPlanName,
            description = uiState.editPlanDescription,
            servings = uiState.editPlanServings,
            recipes = uiState.editPlanRecipes,
            onNameChange = viewModel::updateEditPlanName,
            onDescriptionChange = viewModel::updateEditPlanDescription,
            onServingsChange = viewModel::updateEditPlanServings,
            onReorderRecipes = viewModel::reorderEditPlanRecipes,
            onSave = viewModel::saveEditPlan,
            onDelete = { viewModel.requestDeletePlan(uiState.editingPlan!!) },
            onDismiss = viewModel::dismissEditPlanDialog,
        )
    }

    uiState.pendingDeleteRecipe?.let { recipe ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteRecipeDialog,
            title = { Text("Slet opskrift") },
            text = { Text("\"${recipe.name}\" slettes og fjernes fra alle madplaner. Dette kan ikke fortrydes.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteRecipe,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Slet") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteRecipeDialog) { Text("Annuller") }
            }
        )
    }

    uiState.pendingRemoveRecipeFromPlan?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoveRecipeFromMenuPlanDialog,
            title = { Text("Vil du fjerne opskriften?") },
            text = {
                Text(
                    "\"${pending.recipeName}\" fjernes fra \"${pending.planName}\". Opskriften slettes ikke."
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRemoveRecipeFromMenuPlan) { Text("Fjern") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRemoveRecipeFromMenuPlanDialog) {
                    Text("Annuller")
                }
            }
        )
    }
}

// ── Recipe editor (full-screen) ────────────────────────────────────────────────

@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeEditorScreen(
    recipe: Recipe,
    onRecipeChange: (Recipe) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onAddIngredientSection: () -> Unit,
    onRemoveIngredientSection: (Int) -> Unit,
    onUpdateIngredientSectionTitle: (Int, String) -> Unit,
    onAddIngredient: (Int) -> Unit,
    onRemoveIngredient: (Int, Int) -> Unit,
    onUpdateIngredient: (Int, Int, Ingredient) -> Unit,
    onReorderIngredients: (List<IngredientSection>) -> Unit,
    onAddInstructionSection: () -> Unit,
    onRemoveInstructionSection: (Int) -> Unit,
    onUpdateInstructionSectionTitle: (Int, String) -> Unit,
    onAddInstructionStep: (Int) -> Unit,
    onRemoveInstructionStep: (Int, Int) -> Unit,
    onUpdateInstructionStep: (Int, Int, String) -> Unit,
    onReorderInstructions: (List<InstructionSection>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rediger opskrift") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tilbage")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text("Gem", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = recipe.name.ifBlank { " " },
                onValueChange = { onRecipeChange(recipe.copy(name = it.trimStart())) },
                label = { Text("Navn") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = recipe.description.ifBlank { " " },
                onValueChange = { onRecipeChange(recipe.copy(description = it.trimStart())) },
                label = { Text("Beskrivelse") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("Bedømmelse")
            RatingBar(
                rating = recipe.rating,
                onRatingChange = { onRecipeChange(recipe.copy(rating = it)) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = if (recipe.servings > 0) recipe.servings.toString() else " ",
                    onValueChange = {
                        onRecipeChange(
                            recipe.copy(
                                servings = it.trim().toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Antal personer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = recipe.courseType.ifBlank { " " },
                    onValueChange = { onRecipeChange(recipe.copy(courseType = it.trimStart())) },
                    label = { Text("Kategori") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = if (recipe.prepTimeMinutes > 0) recipe.prepTimeMinutes.toString() else " ",
                    onValueChange = {
                        onRecipeChange(
                            recipe.copy(
                                prepTimeMinutes = it.trim().toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Tilberedningstid (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (recipe.totalTimeMinutes > 0) recipe.totalTimeMinutes.toString() else " ",
                    onValueChange = {
                        onRecipeChange(
                            recipe.copy(
                                totalTimeMinutes = it.trim().toIntOrNull() ?: 0
                            )
                        )
                    },
                    label = { Text("Samlet tid (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = recipe.durability.ifBlank { " " },
                onValueChange = { onRecipeChange(recipe.copy(durability = it.trimStart())) },
                label = { Text("Holdbarhed") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = recipe.nutritionFacts.ifBlank { " " },
                onValueChange = { onRecipeChange(recipe.copy(nutritionFacts = it.trimStart())) },
                label = { Text("Næringsfakta") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // ── Ingredients ──
            SectionLabel("Ingredienser")
            DraggableIngredientSections(
                sections = recipe.ingredientSections,
                showSectionTitles = recipe.ingredientSections.size > 1,
                onSectionsReordered = onReorderIngredients,
                onUpdateSectionTitle = onUpdateIngredientSectionTitle,
                onRemoveSection = onRemoveIngredientSection,
                onAddIngredient = onAddIngredient,
                onRemoveIngredient = onRemoveIngredient,
                onUpdateIngredient = onUpdateIngredient,
            )
            TextButton(onClick = onAddIngredientSection) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (recipe.ingredientSections.isEmpty()) "Tilføj ingredienser"
                    else "Tilføj sektion"
                )
            }

            HorizontalDivider()

            // ── Instructions ──
            SectionLabel("Fremgangsmåde")
            DraggableInstructionSections(
                sections = recipe.instructionSections,
                showSectionTitles = recipe.instructionSections.size > 1,
                onSectionsReordered = onReorderInstructions,
                onUpdateSectionTitle = onUpdateInstructionSectionTitle,
                onRemoveSection = onRemoveInstructionSection,
                onAddStep = onAddInstructionStep,
                onRemoveStep = onRemoveInstructionStep,
                onUpdateStep = onUpdateInstructionStep,
            )
            TextButton(onClick = onAddInstructionSection) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (recipe.instructionSections.isEmpty()) "Tilføj fremgangsmåde"
                    else "Tilføj sektion"
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = recipe.tips.ifBlank { " " },
                onValueChange = { onRecipeChange(recipe.copy(tips = it.trimStart())) },
                label = { Text("Tips") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Recipe detail (read-only) ──────────────────────────────────────────────────

@Suppress("FunctionNaming")
@Composable
private fun RecipeStepTimerInline(
    timer: RecipeStepTimerEntry,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = RecipeStepTimer.formatClock(timer.remainingSeconds),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (timer.remainingSeconds < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            if (timer.remainingSeconds <= 0) {
                IconButton(
                    onClick = { RecipeStepTimer.clear(timer.id) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (timer.isRunning) RecipeStepTimer.pause(timer.id)
                        else RecipeStepTimer.resume(timer.id)
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (timer.isRunning) "Pause" else "Fortsæt",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { RecipeStepTimer.clear(timer.id) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fjern timer",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailScreen(
    recipe: Recipe,
    completedSteps: List<CompletedStep> = emptyList(),
    showStepProgress: Boolean = false,
    onToggleStep: (sectionIndex: Int, stepIndex: Int) -> Unit = { _, _ -> },
    planServings: Int = 0,
    showAddToPlan: Boolean = false,
    menuPlans: List<MenuPlan> = emptyList(),
    onAddToPlan: (planId: String) -> Unit = {},
    onCreatePlanAndAdd: (name: String) -> Unit = {},
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showPlanPicker by remember { mutableStateOf(false) }
    val allTimers by RecipeStepTimer.timers.collectAsStateWithLifecycle()

    if (showPlanPicker) {
        AddToPlanDialog(
            menuPlans = menuPlans,
            onSelect = { planId ->
                onAddToPlan(planId)
                showPlanPicker = false
            },
            onCreateNew = { name ->
                onCreatePlanAndAdd(name)
                showPlanPicker = false
            },
            onDismiss = { showPlanPicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tilbage")
                    }
                },
                actions = {
                    if (showAddToPlan) {
                        IconButton(onClick = { showPlanPicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Tilføj til madplan")
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Rediger")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
                if (recipe.description.isNotBlank()) {
                    LinkedText(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (recipe.rating > 0) {
                    Row {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (star <= recipe.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val scaleFactor = if (planServings > 0 && recipe.servings > 0)
                    planServings.toDouble() / recipe.servings else 1.0
                val effectiveServings = if (planServings > 0) planServings else recipe.servings

                val metaItems = buildList {
                    if (recipe.courseType.isNotBlank()) add("Kategori" to recipe.courseType)
                    if (effectiveServings > 0) add("Antal personer" to effectiveServings.toString())
                    if (recipe.prepTimeMinutes > 0) add("Tilberedningstid" to "${recipe.prepTimeMinutes} min")
                    if (recipe.totalTimeMinutes > 0) add("Samlet tid" to "${recipe.totalTimeMinutes} min")
                    if (recipe.durability.isNotBlank()) add("Holdbarhed" to recipe.durability)
                }
                if (metaItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.4f
                            )
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            metaItems.forEach { (label, value) ->
                                Row {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(130.dp)
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (recipe.nutritionFacts.isNotBlank()) {
                    SectionLabel("Næringsfakta")
                    Text(
                        text = recipe.nutritionFacts,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (recipe.ingredientSections.any { it.ingredients.isNotEmpty() }) {
                    SectionLabel("Ingredienser")
                    Spacer(modifier = Modifier.height(4.dp))
                    recipe.ingredientSections.forEach { section ->
                        if (section.ingredients.isEmpty()) return@forEach
                        if (section.title.isNotBlank() && recipe.ingredientSections.size > 1) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        section.ingredients.forEach { ingredient ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                val scaledQty = scaleQuantity(ingredient.quantity, scaleFactor)
                                val qty = listOf(scaledQty, ingredient.unit)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                if (qty.isNotBlank()) {
                                    Text(
                                        text = qty,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                }
                                Text(
                                    text = ingredient.name.capitalizeIngredientFirstLetter(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (recipe.instructionSections.any { it.steps.isNotEmpty() }) {
                    SectionLabel("Fremgangsmåde")
                    Spacer(modifier = Modifier.height(4.dp))
                    recipe.instructionSections.forEachIndexed { sIdx, section ->
                        if (section.steps.isEmpty()) return@forEachIndexed
                        if (section.title.isNotBlank() && recipe.instructionSections.size > 1) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        section.steps.forEachIndexed { idx, step ->
                            val isDone = showStepProgress &&
                                    completedSteps.any { it.sectionIndex == sIdx && it.stepIndex == idx }
                            val stepTimer = allTimers.find {
                                it.recipeId == recipe.id &&
                                    it.sectionIndex == sIdx &&
                                    it.stepIndex == idx
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .let { m ->
                                            if (showStepProgress) m.clickable {
                                                onToggleStep(
                                                    sIdx,
                                                    idx
                                                )
                                            }
                                            else m
                                        }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    val circleColor = if (isDone) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .then(
                                                if (isDone) Modifier.background(
                                                    circleColor,
                                                    CircleShape
                                                )
                                                else Modifier.border(1.5.dp, circleColor, CircleShape)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Udført",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        } else {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = step,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium.let { s ->
                                            if (isDone) s.copy(textDecoration = TextDecoration.LineThrough)
                                            else s
                                        },
                                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                val stepMinutes = remember(step) { parseInstructionMinutes(step) }
                                if (stepMinutes != null && stepTimer == null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 36.dp, top = 4.dp, bottom = 2.dp)
                                            .clickable {
                                                RecipeStepTimer.start(
                                                    stepMinutes * 60,
                                                    step,
                                                    recipe.id,
                                                    recipe.name,
                                                    sIdx,
                                                    idx,
                                                )
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Start timer",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                if (stepTimer != null) {
                                    RecipeStepTimerInline(
                                        timer = stepTimer,
                                        modifier = Modifier
                                            .padding(start = 36.dp, top = 2.dp, bottom = 4.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (recipe.tips.isNotBlank()) {
                    SectionLabel("Tips")
                    LinkedText(
                        text = recipe.tips,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

private val urlPattern = Regex(
    """https?://[^\s<>\"')\]]+""",
    RegexOption.IGNORE_CASE,
)

@Suppress("FunctionNaming")
@Composable
private fun LinkedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            var cursor = 0
            for (match in urlPattern.findAll(text)) {
                if (match.range.first > cursor) {
                    append(text.substring(cursor, match.range.first))
                }
                val url = match.value
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    )
                ) {
                    append(url)
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
    Text(
        text = annotated,
        style = style,
        color = color,
        modifier = modifier,
    )
}

@Suppress("FunctionNaming")
@Composable
private fun RatingBar(rating: Int, onRatingChange: (Int) -> Unit) {
    Row {
        (1..5).forEach { star ->
            IconButton(
                onClick = { onRatingChange(if (rating == star) 0 else star) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "$star stjerner",
                    tint = if (star <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private sealed interface FlatIngredientItem {
    val id: Int

    data class SectionHeader(override val id: Int, val sIdx: Int) : FlatIngredientItem
    data class Entry(
        override val id: Int,
        val ingredient: Ingredient,
    ) : FlatIngredientItem

    data class AddButton(override val id: Int, val sIdx: Int) : FlatIngredientItem
}

private fun buildFlatIngredientList(
    sections: List<IngredientSection>,
    showSectionTitles: Boolean,
): List<FlatIngredientItem> = buildList {
    var nextId = 0
    sections.forEachIndexed { s, sec ->
        if (showSectionTitles) add(FlatIngredientItem.SectionHeader(nextId++, s))
        sec.ingredients.forEach { add(FlatIngredientItem.Entry(nextId++, it)) }
        add(FlatIngredientItem.AddButton(nextId++, s))
    }
}

private fun rebuildSections(
    flatList: List<FlatIngredientItem>,
    originalSections: List<IngredientSection>,
): List<IngredientSection> {
    var currentSection = 0
    val map = mutableMapOf<Int, MutableList<Ingredient>>()
    originalSections.indices.forEach { map[it] = mutableListOf() }
    for (item in flatList) {
        when (item) {
            is FlatIngredientItem.SectionHeader -> currentSection = item.sIdx
            is FlatIngredientItem.Entry -> map.getOrPut(currentSection) { mutableListOf() }
                .add(item.ingredient)

            is FlatIngredientItem.AddButton -> {}
        }
    }
    return originalSections.mapIndexed { i, sec -> sec.copy(ingredients = map[i] ?: emptyList()) }
}

@Suppress("FunctionNaming", "LongParameterList", "LongMethod")
@Composable
private fun DraggableIngredientSections(
    sections: List<IngredientSection>,
    showSectionTitles: Boolean,
    onSectionsReordered: (List<IngredientSection>) -> Unit,
    onUpdateSectionTitle: (Int, String) -> Unit,
    onRemoveSection: (Int) -> Unit,
    onAddIngredient: (Int) -> Unit,
    onRemoveIngredient: (Int, Int) -> Unit,
    onUpdateIngredient: (Int, Int, Ingredient) -> Unit,
) {
    val externalFlat = remember(sections, showSectionTitles) {
        buildFlatIngredientList(sections, showSectionTitles)
    }

    var localFlat by remember { mutableStateOf(externalFlat) }
    var isDragging by remember { mutableStateOf(false) }
    val activeFlat = if (isDragging) localFlat else externalFlat

    val currentSections by rememberUpdatedState(sections)
    val currentOnReorder by rememberUpdatedState(onSectionsReordered)
    val currentExternalFlat by rememberUpdatedState(externalFlat)

    var dragIdx by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemY = remember { mutableMapOf<Int, Float>() }
    val itemH = remember { mutableMapOf<Int, Int>() }

    Column {
        activeFlat.forEachIndexed { idx, item ->
            key(item.id) {
                when (item) {
                    is FlatIngredientItem.SectionHeader -> {
                        if (item.sIdx > 0) Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { c ->
                                    itemY[idx] = c.positionInParent().y
                                    itemH[idx] = c.size.height
                                }
                        ) {
                            OutlinedTextField(
                                value = sections[item.sIdx].title.ifBlank { " " },
                                onValueChange = { onUpdateSectionTitle(item.sIdx, it.trimStart()) },
                                label = { Text("Sektionsnavn") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveSection(item.sIdx) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Fjern sektion",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    is FlatIngredientItem.Entry -> {
                        val beingDragged = dragIdx == idx
                        val sectionAndIndex = remember(activeFlat) {
                            var s = 0
                            var iInSection = 0
                            for (j in 0 until idx) {
                                when (activeFlat[j]) {
                                    is FlatIngredientItem.SectionHeader -> {
                                        s =
                                            (activeFlat[j] as FlatIngredientItem.SectionHeader).sIdx; iInSection =
                                            0
                                    }

                                    is FlatIngredientItem.Entry -> iInSection++
                                    is FlatIngredientItem.AddButton -> {}
                                }
                            }
                            s to iInSection
                        }
                        var hadIngredientNameFocus by remember(
                            sectionAndIndex.first,
                            sectionAndIndex.second
                        ) {
                            mutableStateOf(false)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { c ->
                                    itemY[idx] = c.positionInParent().y
                                    itemH[idx] = c.size.height
                                }
                                .zIndex(if (beingDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (beingDragged) dragOffsetY else 0f
                                    if (beingDragged) {
                                        shadowElevation = 8f
                                    }
                                }
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Flyt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                localFlat = currentExternalFlat
                                                dragIdx = idx
                                                isDragging = true
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, offset ->
                                                change.consume()
                                                dragOffsetY += offset.y

                                                val di = dragIdx
                                                    ?: return@detectDragGesturesAfterLongPress
                                                val myCenter = (itemY[di] ?: 0f) + (itemH[di]
                                                    ?: 0) / 2f + dragOffsetY

                                                if (dragOffsetY > 0f) {
                                                    val nextIdx = di + 1
                                                    if (nextIdx in localFlat.indices) {
                                                        val nextCenter =
                                                            (itemY[nextIdx] ?: 0f) + (itemH[nextIdx]
                                                                ?: 0) / 2f
                                                        if (myCenter > nextCenter) {
                                                            val items = localFlat.toMutableList()
                                                            val entry = items.removeAt(di)
                                                            items.add(nextIdx, entry)
                                                            localFlat = items
                                                            dragOffsetY -= (itemY[nextIdx]
                                                                ?: 0f) - (itemY[di] ?: 0f)
                                                            val tmpY = itemY[di];
                                                            val tmpH = itemH[di]
                                                            itemY[di] =
                                                                itemY[nextIdx] ?: 0f; itemH[di] =
                                                                itemH[nextIdx] ?: 0
                                                            itemY[nextIdx] =
                                                                tmpY ?: 0f; itemH[nextIdx] =
                                                                tmpH ?: 0
                                                            dragIdx = nextIdx
                                                        }
                                                    }
                                                }

                                                if (dragOffsetY < 0f) {
                                                    val prevIdx = di - 1
                                                    if (prevIdx in localFlat.indices) {
                                                        val prevCenter =
                                                            (itemY[prevIdx] ?: 0f) + (itemH[prevIdx]
                                                                ?: 0) / 2f
                                                        if (myCenter < prevCenter) {
                                                            val items = localFlat.toMutableList()
                                                            val entry = items.removeAt(di)
                                                            items.add(prevIdx, entry)
                                                            localFlat = items
                                                            dragOffsetY += (itemY[di]
                                                                ?: 0f) - (itemY[prevIdx] ?: 0f)
                                                            val tmpY = itemY[di];
                                                            val tmpH = itemH[di]
                                                            itemY[di] =
                                                                itemY[prevIdx] ?: 0f; itemH[di] =
                                                                itemH[prevIdx] ?: 0
                                                            itemY[prevIdx] =
                                                                tmpY ?: 0f; itemH[prevIdx] =
                                                                tmpH ?: 0
                                                            dragIdx = prevIdx
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                currentOnReorder(
                                                    rebuildSections(
                                                        localFlat,
                                                        currentSections
                                                    )
                                                )
                                                dragIdx = null
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                localFlat = externalFlat
                                                dragIdx = null
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                        )
                                    }
                            )
                            OutlinedTextField(
                                value = item.ingredient.quantity.ifBlank { " " },
                                onValueChange = {
                                    onUpdateIngredient(
                                        sectionAndIndex.first, sectionAndIndex.second,
                                        item.ingredient.copy(quantity = it.trimStart())
                                    )
                                },
                                label = { Text("Mgd") },
                                singleLine = true,
                                modifier = Modifier.weight(0.2f)
                            )
                            OutlinedTextField(
                                value = item.ingredient.unit.ifBlank { " " },
                                onValueChange = {
                                    onUpdateIngredient(
                                        sectionAndIndex.first, sectionAndIndex.second,
                                        item.ingredient.copy(unit = it.trimStart())
                                    )
                                },
                                label = { Text("Enh") },
                                singleLine = true,
                                modifier = Modifier.weight(0.2f)
                            )
                            OutlinedTextField(
                                value = item.ingredient.name.ifBlank { " " },
                                onValueChange = {
                                    onUpdateIngredient(
                                        sectionAndIndex.first, sectionAndIndex.second,
                                        item.ingredient.copy(name = it.trimStart())
                                    )
                                },
                                label = { Text("Ingrediens") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None
                                ),
                                modifier = Modifier
                                    .weight(0.6f)
                                    .onFocusChanged { focusState ->
                                        if (hadIngredientNameFocus && !focusState.isFocused) {
                                            val trimmed = item.ingredient.name.trim()
                                            val normalized =
                                                trimmed.capitalizeIngredientFirstLetter()
                                            if (normalized != trimmed) {
                                                onUpdateIngredient(
                                                    sectionAndIndex.first,
                                                    sectionAndIndex.second,
                                                    item.ingredient.copy(name = normalized)
                                                )
                                            }
                                        }
                                        hadIngredientNameFocus = focusState.isFocused
                                    }
                            )
                            IconButton(
                                onClick = {
                                    onRemoveIngredient(
                                        sectionAndIndex.first,
                                        sectionAndIndex.second
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, "Fjern", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    is FlatIngredientItem.AddButton -> {
                        TextButton(
                            onClick = { onAddIngredient(item.sIdx) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.onGloballyPositioned { c ->
                                itemY[idx] = c.positionInParent().y
                                itemH[idx] = c.size.height
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tilføj ingrediens", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private sealed interface FlatInstructionItem {
    val id: Int

    data class SectionHeader(override val id: Int, val sIdx: Int) : FlatInstructionItem
    data class StepEntry(override val id: Int, val step: String) : FlatInstructionItem
    data class AddButton(override val id: Int, val sIdx: Int) : FlatInstructionItem
}

private fun buildFlatInstructionList(
    sections: List<InstructionSection>,
    showSectionTitles: Boolean,
): List<FlatInstructionItem> = buildList {
    var nextId = 0
    sections.forEachIndexed { s, sec ->
        if (showSectionTitles) add(FlatInstructionItem.SectionHeader(nextId++, s))
        sec.steps.forEach { add(FlatInstructionItem.StepEntry(nextId++, it)) }
        add(FlatInstructionItem.AddButton(nextId++, s))
    }
}

private fun rebuildInstructionSections(
    flatList: List<FlatInstructionItem>,
    originalSections: List<InstructionSection>,
): List<InstructionSection> {
    var currentSection = 0
    val map = mutableMapOf<Int, MutableList<String>>()
    originalSections.indices.forEach { map[it] = mutableListOf() }
    for (item in flatList) {
        when (item) {
            is FlatInstructionItem.SectionHeader -> currentSection = item.sIdx
            is FlatInstructionItem.StepEntry -> map.getOrPut(currentSection) { mutableListOf() }
                .add(item.step)

            is FlatInstructionItem.AddButton -> {}
        }
    }
    return originalSections.mapIndexed { i, sec -> sec.copy(steps = map[i] ?: emptyList()) }
}

@Suppress("FunctionNaming", "LongParameterList", "LongMethod")
@Composable
private fun DraggableInstructionSections(
    sections: List<InstructionSection>,
    showSectionTitles: Boolean,
    onSectionsReordered: (List<InstructionSection>) -> Unit,
    onUpdateSectionTitle: (Int, String) -> Unit,
    onRemoveSection: (Int) -> Unit,
    onAddStep: (Int) -> Unit,
    onRemoveStep: (Int, Int) -> Unit,
    onUpdateStep: (Int, Int, String) -> Unit,
) {
    val externalFlat = remember(sections, showSectionTitles) {
        buildFlatInstructionList(sections, showSectionTitles)
    }

    var localFlat by remember { mutableStateOf(externalFlat) }
    var isDragging by remember { mutableStateOf(false) }
    val activeFlat = if (isDragging) localFlat else externalFlat

    val currentSections by rememberUpdatedState(sections)
    val currentOnReorder by rememberUpdatedState(onSectionsReordered)
    val currentExternalFlat by rememberUpdatedState(externalFlat)

    var dragIdx by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemY = remember { mutableMapOf<Int, Float>() }
    val itemH = remember { mutableMapOf<Int, Int>() }

    Column {
        activeFlat.forEachIndexed { idx, item ->
            key(item.id) {
                when (item) {
                    is FlatInstructionItem.SectionHeader -> {
                        if (item.sIdx > 0) Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { c ->
                                    itemY[idx] = c.positionInParent().y
                                    itemH[idx] = c.size.height
                                }
                        ) {
                            OutlinedTextField(
                                value = sections[item.sIdx].title.ifBlank { " " },
                                onValueChange = { onUpdateSectionTitle(item.sIdx, it.trimStart()) },
                                label = { Text("Sektionsnavn") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveSection(item.sIdx) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Fjern sektion",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    is FlatInstructionItem.StepEntry -> {
                        val beingDragged = dragIdx == idx
                        val sectionAndIndex = remember(activeFlat) {
                            var s = 0
                            var iInSection = 0
                            for (j in 0 until idx) {
                                when (activeFlat[j]) {
                                    is FlatInstructionItem.SectionHeader -> {
                                        s =
                                            (activeFlat[j] as FlatInstructionItem.SectionHeader).sIdx; iInSection =
                                            0
                                    }

                                    is FlatInstructionItem.StepEntry -> iInSection++
                                    is FlatInstructionItem.AddButton -> {}
                                }
                            }
                            s to iInSection
                        }
                        val stepNumber = remember(activeFlat) {
                            var count = 0
                            for (j in 0 until idx) {
                                if (activeFlat[j] is FlatInstructionItem.StepEntry) count++
                            }
                            count + 1
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { c ->
                                    itemY[idx] = c.positionInParent().y
                                    itemH[idx] = c.size.height
                                }
                                .zIndex(if (beingDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (beingDragged) dragOffsetY else 0f
                                    if (beingDragged) shadowElevation = 8f
                                }
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Flyt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(24.dp)
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                localFlat = currentExternalFlat
                                                dragIdx = idx
                                                isDragging = true
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, offset ->
                                                change.consume()
                                                dragOffsetY += offset.y

                                                val di = dragIdx
                                                    ?: return@detectDragGesturesAfterLongPress
                                                val myCenter = (itemY[di] ?: 0f) + (itemH[di]
                                                    ?: 0) / 2f + dragOffsetY

                                                if (dragOffsetY > 0f) {
                                                    val nextIdx = di + 1
                                                    if (nextIdx in localFlat.indices) {
                                                        val nextCenter =
                                                            (itemY[nextIdx] ?: 0f) + (itemH[nextIdx]
                                                                ?: 0) / 2f
                                                        if (myCenter > nextCenter) {
                                                            val items = localFlat.toMutableList()
                                                            val entry = items.removeAt(di)
                                                            items.add(nextIdx, entry)
                                                            localFlat = items
                                                            dragOffsetY -= (itemY[nextIdx]
                                                                ?: 0f) - (itemY[di] ?: 0f)
                                                            val tmpY = itemY[di];
                                                            val tmpH = itemH[di]
                                                            itemY[di] =
                                                                itemY[nextIdx] ?: 0f; itemH[di] =
                                                                itemH[nextIdx] ?: 0
                                                            itemY[nextIdx] =
                                                                tmpY ?: 0f; itemH[nextIdx] =
                                                                tmpH ?: 0
                                                            dragIdx = nextIdx
                                                        }
                                                    }
                                                }

                                                if (dragOffsetY < 0f) {
                                                    val prevIdx = di - 1
                                                    if (prevIdx in localFlat.indices) {
                                                        val prevCenter =
                                                            (itemY[prevIdx] ?: 0f) + (itemH[prevIdx]
                                                                ?: 0) / 2f
                                                        if (myCenter < prevCenter) {
                                                            val items = localFlat.toMutableList()
                                                            val entry = items.removeAt(di)
                                                            items.add(prevIdx, entry)
                                                            localFlat = items
                                                            dragOffsetY += (itemY[di]
                                                                ?: 0f) - (itemY[prevIdx] ?: 0f)
                                                            val tmpY = itemY[di];
                                                            val tmpH = itemH[di]
                                                            itemY[di] =
                                                                itemY[prevIdx] ?: 0f; itemH[di] =
                                                                itemH[prevIdx] ?: 0
                                                            itemY[prevIdx] =
                                                                tmpY ?: 0f; itemH[prevIdx] =
                                                                tmpH ?: 0
                                                            dragIdx = prevIdx
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                currentOnReorder(
                                                    rebuildInstructionSections(
                                                        localFlat,
                                                        currentSections
                                                    )
                                                )
                                                dragIdx = null
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                dragIdx = null
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                        )
                                    }
                            )
                            Text(
                                text = "$stepNumber.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            OutlinedTextField(
                                value = item.step.ifBlank { " " },
                                onValueChange = {
                                    onUpdateStep(
                                        sectionAndIndex.first,
                                        sectionAndIndex.second,
                                        it.trimStart()
                                    )
                                },
                                label = { Text("Trin $stepNumber") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    onRemoveStep(
                                        sectionAndIndex.first,
                                        sectionAndIndex.second
                                    )
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(top = 12.dp)
                            ) {
                                Icon(Icons.Default.Close, "Fjern", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    is FlatInstructionItem.AddButton -> {
                        TextButton(
                            onClick = { onAddStep(item.sIdx) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.onGloballyPositioned { c ->
                                itemY[idx] = c.positionInParent().y
                                itemH[idx] = c.size.height
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tilføj trin", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Servings picker ────────────────────────────────────────────────────────────

@Suppress("FunctionNaming")
@Composable
private fun ServingsRow(
    servings: Int,
    onServingsChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Text(
            text = "Antal personer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { if (servings > 1) onServingsChange(servings - 1) },
            enabled = servings > 1,
            modifier = Modifier.size(32.dp),
        ) {
            Text(
                "−",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = if (servings > 0) servings.toString() else "–",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(
            onClick = { onServingsChange(servings + 1) },
            modifier = Modifier.size(32.dp),
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Menu plan shopping list dialog ─────────────────────────────────────────────

private fun mergedIngredientRowKey(line: MergedIngredientRow): String =
    listOf(line.name, line.quantity, line.unit).joinToString("\u0000")

@Suppress("FunctionNaming")
@Composable
private fun MenuPlanIngredientsDialog(
    lines: List<MergedIngredientRow>,
    groceryListName: String?,
    canAddToGroceryList: Boolean,
    onAddIngredientToGroceryList: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var addedLineKeys by remember { mutableStateOf(emptySet<String>()) }

    val titleText = if (groceryListName != null) {
        "Tilføj til $groceryListName"
    } else {
        "Tilføj til indkøbsliste"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column {
                if (groceryListName == null) {
                    Text(
                        text = "Ingen indkøbsliste — opret og åbn en liste først",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (lines.isEmpty()) {
                    Text(
                        text = "Ingen ingredienser i denne madplan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        lines.forEach { line ->
                            val rowKey = mergedIngredientRowKey(line)
                            val isAdded = rowKey in addedLineKeys
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .alpha(if (isAdded) 0.6f else 1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val qty = listOf(line.quantity, line.unit)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" ")
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (qty.isNotBlank()) {
                                        Text(
                                            text = qty,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(
                                                min = 56.dp,
                                                max = 65.dp,
                                            ),
                                        )
                                    }
                                    Text(
                                        text = line.name,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Box(
                                    modifier = Modifier.width(60.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    if (isAdded) {
                                        Text(
                                            text = "Tilføjet",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                onAddIngredientToGroceryList(
                                                    line.name,
                                                    line.quantity,
                                                    line.unit,
                                                )
                                                addedLineKeys = addedLineKeys + rowKey
                                            },
                                            enabled = canAddToGroceryList && line.name.isNotBlank(),
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Tilføj ${line.name} til indkøbsliste",
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Luk") }
        },
    )
}

// ── Menu plan card ─────────────────────────────────────────────────────────────

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun MenuPlanCard(
    plan: MenuPlan,
    recipes: List<Recipe>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditPlan: () -> Unit,
    onRemoveRecipe: (String) -> Unit,
    onEditRecipe: (Recipe) -> Unit,
    groceryListName: String?,
    canAddToGroceryList: Boolean,
    onAddIngredientToGroceryList: (String, String, String) -> Unit,
) {
    val mergedIngredientLines = remember(plan.id, plan.servings, plan.recipeIds, recipes) {
        buildMergedMenuPlanIngredients(plan, recipes)
    }
    var showIngredientsDialog by remember { mutableStateOf(false) }

    if (showIngredientsDialog) {
        MenuPlanIngredientsDialog(
            lines = mergedIngredientLines,
            groceryListName = groceryListName,
            canAddToGroceryList = canAddToGroceryList,
            onAddIngredientToGroceryList = onAddIngredientToGroceryList,
            onDismiss = { showIngredientsDialog = false },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                val infoParts = mutableListOf<String>()
                infoParts += when (recipes.size) {
                    0 -> "Ingen opskrifter"
                    1 -> "1 opskrift"
                    else -> "${recipes.size} opskrifter"
                }
                if (plan.servings > 0) infoParts += "${plan.servings} pers."
                Text(
                    text = infoParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { showIngredientsDialog = true },
                enabled = mergedIngredientLines.isNotEmpty(),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Indkøbsliste for ${plan.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (mergedIngredientLines.isNotEmpty()) 1f else 0.38f
                    ),
                    modifier = Modifier.size(22.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Skjul" else "Vis opskrifter",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            IconButton(onClick = onEditPlan, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rediger ${plan.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 16.dp, bottom = 12.dp)) {
                if (plan.description.isNotBlank()) {
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                if (recipes.isEmpty()) {
                    Text(
                        text = "Ingen opskrifter tilknyttet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    recipes.forEachIndexed { index, recipe ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        val totalSteps = recipe.instructionSections.sumOf { it.steps.size }
                        val doneSteps = plan.recipeProgress[recipe.id]?.size ?: 0
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                            tonalElevation = 0.dp,
                        ) {
                            RecipeRow(
                                recipe = recipe,
                                completedSteps = doneSteps,
                                totalSteps = totalSteps,
                                onEdit = { onEditRecipe(recipe) },
                                onRemove = { onRemoveRecipe(recipe.id) },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

            }
        }

    }
}

@Suppress("FunctionNaming")
@Composable
private fun RecipeTimersOnListItem(
    timers: List<RecipeStepTimerEntry>,
    modifier: Modifier = Modifier,
) {
    if (timers.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        timers.forEach { t ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val initialMin = (t.initialTotalSeconds / 60).coerceAtLeast(1)
                    val leftClock = RecipeStepTimer.formatClock(t.remainingSeconds)
                    Text(
                        text = "Timer $initialMin min. Left: $leftClock min.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = if (t.remainingSeconds < 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                    if (t.remainingSeconds <= 0) {
                        IconButton(
                            onClick = { RecipeStepTimer.clear(t.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (t.isRunning) RecipeStepTimer.pause(t.id)
                                else RecipeStepTimer.resume(t.id)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (t.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (t.isRunning) "Pause" else "Fortsæt",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = { RecipeStepTimer.clear(t.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fjern timer",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun RecipeRow(
    recipe: Recipe,
    completedSteps: Int = 0,
    totalSteps: Int = 0,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allTimers by RecipeStepTimer.timers.collectAsStateWithLifecycle()
    val recipeTimers = allTimers
        .filter { it.recipeId == recipe.id }
        .sortedWith(compareBy({ it.sectionIndex }, { it.stepIndex }))
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (totalSteps > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isAllDone = completedSteps >= totalSteps
                        Text(
                            text = "$completedSteps/$totalSteps",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAllDone) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isAllDone) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
                if (totalSteps > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { completedSteps.toFloat() / totalSteps },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                val subtitle = buildRecipeSubtitle(recipe, showServings = false)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fjern ${recipe.name} fra plan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (recipeTimers.isNotEmpty()) {
            RecipeTimersOnListItem(
                timers = recipeTimers,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private val fractionMap = mapOf(
    '½' to 0.5, '⅓' to 1.0 / 3, '¼' to 0.25,
    '⅔' to 2.0 / 3, '¾' to 0.75,
)

private fun parseQuantity(raw: String): Double? {
    val s = raw.trim()
    if (s.isEmpty()) return null
    fractionMap[s.singleOrNull()]?.let { return it }
    if ('/' in s) {
        val parts = s.split('/')
        val num = parts[0].trim().toDoubleOrNull() ?: return null
        val den = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
        if (den == 0.0) return null
        return num / den
    }
    return s.replace(',', '.').toDoubleOrNull()
}

private fun formatQuantity(value: Double): String {
    if (value == value.toLong().toDouble() && value < 10_000) return value.toLong().toString()
    val rounded = (value * 100).toLong() / 100.0
    val s = rounded.toBigDecimal().stripTrailingZeros().toPlainString()
    return s.replace('.', ',')
}

private fun scaleQuantity(raw: String, factor: Double): String {
    if (factor == 1.0) return raw
    val parsed = parseQuantity(raw) ?: return raw
    return formatQuantity(parsed * factor)
}

private data class MergedIngredientRow(
    val quantity: String,
    val unit: String,
    val name: String,
)

private fun normalizeIngredientMergeKey(name: String, unit: String): String {
    val n = name.trim().lowercase()
    val u = unit.trim().lowercase()
    return "$n|$u"
}

private fun buildMergedMenuPlanIngredients(
    plan: MenuPlan,
    recipes: List<Recipe>
): List<MergedIngredientRow> {
    val scaleFactorFor: (Recipe) -> Double = { recipe ->
        if (plan.servings > 0 && recipe.servings > 0) plan.servings.toDouble() / recipe.servings
        else 1.0
    }
    val scaled = buildList {
        for (recipe in recipes) {
            val factor = scaleFactorFor(recipe)
            for (section in recipe.ingredientSections) {
                for (ing in section.ingredients) {
                    if (ing.name.isBlank() && ing.quantity.isBlank() && ing.unit.isBlank()) continue
                    val scaledQty = scaleQuantity(ing.quantity, factor)
                    add(
                        Triple(
                            ing.name.trim(),
                            ing.unit.trim(),
                            scaledQty.trim(),
                        )
                    )
                }
            }
        }
    }
    return scaled
        .groupBy { normalizeIngredientMergeKey(it.first, it.second) }
        .map { (_, group) ->
            val displayName = group.firstOrNull { it.first.isNotBlank() }?.first.orEmpty()
                .capitalizeIngredientFirstLetter()
            val unit = group.first().second
            val qtyStrings = group.map { it.third }
            val mergedQty = mergeQuantityStrings(qtyStrings)
            MergedIngredientRow(mergedQty, unit, displayName)
        }
        .sortedBy { it.name.lowercase() }
}

private fun mergeQuantityStrings(quantities: List<String>): String {
    val nonBlank = quantities.filter { it.isNotBlank() }
    if (nonBlank.isEmpty()) return ""
    val allParse = nonBlank.all { parseQuantity(it) != null }
    return if (allParse) {
        val sum = nonBlank.sumOf { parseQuantity(it)!! }
        formatQuantity(sum)
    } else {
        nonBlank.distinct().joinToString(" + ")
    }
}

private fun buildRecipeSubtitle(recipe: Recipe, showServings: Boolean = true): String {
    val parts = mutableListOf<String>()
    if (recipe.courseType.isNotBlank()) parts += recipe.courseType
    if (recipe.totalTimeMinutes > 0) parts += "${recipe.totalTimeMinutes} min"
    if (showServings && recipe.servings > 0) parts += "${recipe.servings} pers."
    if (recipe.rating > 0) parts += "★".repeat(recipe.rating)
    return parts.joinToString(" · ")
}

@Suppress("FunctionNaming")
@Composable
private fun SearchRecipeRow(
    recipe: Recipe,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val allTimers by RecipeStepTimer.timers.collectAsStateWithLifecycle()
    val recipeTimers = allTimers
        .filter { it.recipeId == recipe.id }
        .sortedWith(compareBy({ it.sectionIndex }, { it.stepIndex }))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val subtitle = buildRecipeSubtitle(recipe)
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Slet ${recipe.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (recipeTimers.isNotEmpty()) {
                RecipeTimersOnListItem(
                    timers = recipeTimers,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AddFab(
    onAddPlan: () -> Unit,
    onAddRecipe: () -> Unit,
    onImportRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tilføj")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Ny madplan") },
                onClick = { expanded = false; onAddPlan() }
            )
            DropdownMenuItem(
                text = { Text("Ny opskrift") },
                onClick = { expanded = false; onAddRecipe() }
            )
            DropdownMenuItem(
                text = { Text("Importér opskrift") },
                onClick = { expanded = false; onImportRecipe() }
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun EmptyMenuPlansState(
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isFiltered) "Ingen opskrifter matcher søgningen" else "Ingen madplaner endnu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFiltered) "Prøv en anden søgning" else "Tryk + for at oprette din første madplan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun EditMenuPlanDialog(
    name: String,
    description: String,
    servings: Int,
    recipes: List<Recipe>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onServingsChange: (Int) -> Unit,
    onReorderRecipes: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rediger madplan") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Navn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Noter") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                ServingsRow(
                    servings = servings,
                    onServingsChange = onServingsChange,
                )
                if (recipes.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Opskrifter",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ReorderableRecipeList(
                        recipes = recipes,
                        onReorder = onReorderRecipes,
                    )
                }
                HorizontalDivider()
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Slet madplan")
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = name.isNotBlank()) { Text("Gem") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun ReorderableRecipeList(
    recipes: List<Recipe>,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 48.dp.toPx() }

    Column(modifier = modifier) {
        recipes.forEachIndexed { index, recipe ->
            val isDragging = index == draggingIndex
            val draggedTo = draggingIndex?.let { from ->
                (from + (draggingOffsetY / itemHeightPx).roundToInt()).coerceIn(
                    0,
                    recipes.lastIndex
                )
            }
            val shiftY = when {
                draggingIndex == null || isDragging -> 0f
                draggingIndex!! < index && draggedTo != null && index <= draggedTo -> -itemHeightPx
                draggingIndex!! > index && draggedTo != null && index >= draggedTo -> itemHeightPx
                else -> 0f
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) draggingOffsetY else shiftY
                        shadowElevation = if (isDragging) 8f else 0f
                    }
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
                        else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 4.dp),
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Flyt",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingIndex = index; draggingOffsetY = 0f },
                                onDrag = { _, delta -> draggingOffsetY += delta.y },
                                onDragEnd = {
                                    val from =
                                        draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    val to = (from + (draggingOffsetY / itemHeightPx).roundToInt())
                                        .coerceIn(0, recipes.lastIndex)
                                    if (from != to) onReorder(from, to)
                                    draggingIndex = null; draggingOffsetY = 0f
                                },
                                onDragCancel = { draggingIndex = null; draggingOffsetY = 0f },
                            )
                        },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun AddToPlanDialog(
    menuPlans: List<MenuPlan>,
    onSelect: (planId: String) -> Unit,
    onCreateNew: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creatingNew by remember { mutableStateOf(false) }
    var newPlanName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(creatingNew) {
        if (creatingNew) focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tilføj til madplan") },
        text = {
            Column {
                menuPlans.forEach { plan ->
                    TextButton(
                        onClick = { onSelect(plan.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = plan.name,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (menuPlans.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                if (creatingNew) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newPlanName,
                            onValueChange = { newPlanName = it },
                            label = { Text("Navn på ny madplan") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                        )
                        IconButton(
                            onClick = { onCreateNew(newPlanName) },
                            enabled = newPlanName.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Opret")
                        }
                    }
                } else {
                    TextButton(
                        onClick = { creatingNew = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ny madplan", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun AddMenuPlanDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny madplan") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Navn") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Opret") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun AddRecipeDialog(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny opskrift") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Navn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Beskrivelse (valgfri)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Opret") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun ImportRecipeDialog(
    name: String,
    ingredients: String,
    instructions: String,
    onNameChange: (String) -> Unit,
    onIngredientsChange: (String) -> Unit,
    onInstructionsChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importér opskrift") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Navn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = onIngredientsChange,
                    label = { Text("Ingredienser") },
                    placeholder = { Text("Én ingrediens per linje\nf.eks. 2 dl mel") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = onInstructionsChange,
                    label = { Text("Fremgangsmåde") },
                    placeholder = { Text("Hver sætning bliver et trin") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Importér") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuller") }
        }
    )
}
