package dk.joachim.shopping.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.joachim.shopping.data.CatalogItem
import dk.joachim.shopping.data.CompletedStep
import dk.joachim.shopping.data.Ingredient
import dk.joachim.shopping.data.IngredientSection
import dk.joachim.shopping.data.InstructionSection
import dk.joachim.shopping.data.MenuPlan
import dk.joachim.shopping.data.Recipe
import dk.joachim.shopping.data.ShoppingRepository
import dk.joachim.shopping.data.UserCategory
import dk.joachim.shopping.data.capitalizeIngredientFirstLetter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingRemoveRecipeFromPlan(
    val planId: String,
    val planName: String,
    val recipeId: String,
    val recipeName: String,
)

data class CookingUiState(
    val menuPlans: List<MenuPlan> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showAddPlanDialog: Boolean = false,
    val newPlanName: String = "",
    val newPlanDescription: String = "",
    val pendingDeletePlan: MenuPlan? = null,
    val editingPlan: MenuPlan? = null,
    val editPlanName: String = "",
    val editPlanDescription: String = "",
    val editPlanServings: Int = 0,
    val editPlanRecipeIds: List<String> = emptyList(),
    val showAddRecipeDialog: Boolean = false,
    val newRecipeName: String = "",
    val newRecipeDescription: String = "",
    val pendingDeleteRecipe: Recipe? = null,
    val pendingRemoveRecipeFromPlan: PendingRemoveRecipeFromPlan? = null,
    val expandedPlanId: String? = null,
    val viewingRecipe: Recipe? = null,
    val viewingMenuPlanId: String? = null,
    val completedSteps: List<CompletedStep> = emptyList(),
    val editingRecipe: Recipe? = null,
    val showImportRecipeDialog: Boolean = false,
    val importRecipeName: String = "",
    val importRecipeIngredients: String = "",
    val importRecipeInstructions: String = "",
    /** Resolved target for “add to grocery list” from last-used id, or first list. */
    val targetGroceryListName: String? = null,
    val canAddIngredientsToGroceryList: Boolean = false,
) {
    val filteredRecipes: List<Recipe>
        get() = if (searchQuery.isBlank()) recipes
        else recipes.filter { it.name.contains(searchQuery, ignoreCase = true) }

    fun recipesForPlan(plan: MenuPlan): List<Recipe> =
        plan.recipeIds.mapNotNull { id -> recipes.find { it.id == id } }

    val editPlanRecipes: List<Recipe>
        get() = editPlanRecipeIds.mapNotNull { id -> recipes.find { it.id == id } }

    fun recipesNotInPlan(plan: MenuPlan): List<Recipe> =
        recipes.filter { it.id !in plan.recipeIds }
}

@Suppress("TooManyFunctions")
class CookingViewModel : ViewModel() {

    private val repository = ShoppingRepository

    private data class ExtraState(
        val isLoading: Boolean = true,
        val searchQuery: String = "",
        val showAddPlanDialog: Boolean = false,
        val newPlanName: String = "",
        val newPlanDescription: String = "",
        val pendingDeletePlan: MenuPlan? = null,
        val editingPlan: MenuPlan? = null,
        val editPlanName: String = "",
        val editPlanDescription: String = "",
        val editPlanServings: Int = 0,
        val editPlanRecipeIds: List<String> = emptyList(),
        val showAddRecipeDialog: Boolean = false,
        val newRecipeName: String = "",
        val newRecipeDescription: String = "",
        val pendingDeleteRecipe: Recipe? = null,
        val pendingRemoveRecipeFromPlan: PendingRemoveRecipeFromPlan? = null,
        val expandedPlanId: String? = null,
        val viewingRecipe: Recipe? = null,
        val viewingMenuPlanId: String? = null,
        val editingRecipe: Recipe? = null,
        val showImportRecipeDialog: Boolean = false,
        val importRecipeName: String = "",
        val importRecipeIngredients: String = "",
        val importRecipeInstructions: String = "",
    )

    private val _extra = MutableStateFlow(ExtraState())
    private val _userCategories = MutableStateFlow<List<UserCategory>>(emptyList())

    // Nested combine(3)+combine(3)+lastListId: last list id must be a flow so UI updates when the user opens another list (prefs alone does not emit).
    val uiState = combine(
        combine(
            repository.menuPlans,
            repository.recipes,
            repository.lists,
        ) { plans, recipes, lists ->
            Triple(plans, recipes, lists)
        },
        combine(
            repository.catalogItems,
            _userCategories,
            _extra,
        ) { catalogItems, userCategories, extra ->
            Triple(catalogItems, userCategories, extra)
        },
        repository.lastListId,
    ) { core, meta, lastListId ->
        val plans = core.first
        val recipes = core.second
        val lists = core.third
        val extra = meta.third
        val completed = extra.viewingMenuPlanId?.let { planId ->
            val recipeId = extra.viewingRecipe?.id
            if (recipeId != null) plans.firstOrNull { it.id == planId }?.recipeProgress?.get(recipeId)
            else null
        }.orEmpty()
        val targetList = lists.firstOrNull { it.id == lastListId } ?: lists.firstOrNull()
        CookingUiState(
            menuPlans = plans,
            recipes = recipes,
            isLoading = extra.isLoading,
            searchQuery = extra.searchQuery,
            showAddPlanDialog = extra.showAddPlanDialog,
            newPlanName = extra.newPlanName,
            newPlanDescription = extra.newPlanDescription,
            pendingDeletePlan = extra.pendingDeletePlan,
            editingPlan = extra.editingPlan,
            editPlanName = extra.editPlanName,
            editPlanDescription = extra.editPlanDescription,
            editPlanServings = extra.editPlanServings,
            editPlanRecipeIds = extra.editPlanRecipeIds,
            showAddRecipeDialog = extra.showAddRecipeDialog,
            newRecipeName = extra.newRecipeName,
            newRecipeDescription = extra.newRecipeDescription,
            pendingDeleteRecipe = extra.pendingDeleteRecipe,
            pendingRemoveRecipeFromPlan = extra.pendingRemoveRecipeFromPlan,
            expandedPlanId = extra.expandedPlanId,
            viewingRecipe = extra.viewingRecipe,
            viewingMenuPlanId = extra.viewingMenuPlanId,
            completedSteps = completed,
            editingRecipe = extra.editingRecipe,
            showImportRecipeDialog = extra.showImportRecipeDialog,
            importRecipeName = extra.importRecipeName,
            importRecipeIngredients = extra.importRecipeIngredients,
            importRecipeInstructions = extra.importRecipeInstructions,
            targetGroceryListName = targetList?.name,
            canAddIngredientsToGroceryList = targetList != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CookingUiState())

    init {
        viewModelScope.launch {
            repository.syncRecipes()
            repository.syncMenuPlans()
            val savedExpandedId = repository.getLastExpandedMenuPlanId()
            if (savedExpandedId != null && repository.menuPlans.value.any { it.id == savedExpandedId }) {
                _extra.update { it.copy(expandedPlanId = savedExpandedId) }
            }
            _extra.update { it.copy(isLoading = false) }
        }
        viewModelScope.launch {
            _userCategories.value = repository.loadCategories()
        }
        viewModelScope.launch {
            repository.loadCatalogItems()
        }
    }

    /** Fetches latest menu plans (including recipe progress) from the server — call when opening the Cooking tab. */
    fun syncMenuPlansFromServer() {
        viewModelScope.launch { repository.syncMenuPlans() }
    }

    /**
     * Adds one ingredient line to the last-used grocery list (or first list if the id is stale).
     * Category comes from the catalog when the name matches; otherwise the “Andet” category.
     */
    fun addMergedIngredientToGroceryList(ingredientName: String, quantity: String, unit: String) {
        val lists = repository.lists.value
        val lastId = repository.getLastListId()
        val targetList = lists.firstOrNull { it.id == lastId } ?: lists.firstOrNull() ?: return
        val qty =
            listOf(quantity, unit).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "1" }
        val trimmedName = ingredientName.trim()
        if (trimmedName.isBlank()) return
        val categoryId = categoryIdForGroceryAdd(
            trimmedName,
            repository.catalogItems.value,
            _userCategories.value
        )
        repository.addOrMergeItem(targetList.id, trimmedName, qty, categoryId)
    }

    private fun categoryIdForGroceryAdd(
        name: String,
        catalogItems: List<CatalogItem>,
        userCategories: List<UserCategory>,
    ): String {
        val fromCatalog = catalogItems.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (fromCatalog != null && fromCatalog.category.isNotBlank()) return fromCatalog.category
        val andet = userCategories.firstOrNull { it.name.contains("Andet", ignoreCase = true) }
        return andet?.id ?: ""
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun updateSearchQuery(query: String) = _extra.update { it.copy(searchQuery = query) }

    // ── Menu plan dialogs ─────────────────────────────────────────────────────

    fun showAddPlanDialog() = _extra.update { it.copy(showAddPlanDialog = true) }
    fun dismissAddPlanDialog() = _extra.update {
        it.copy(
            showAddPlanDialog = false,
            newPlanName = "",
            newPlanDescription = ""
        )
    }

    fun updateNewPlanName(name: String) = _extra.update { it.copy(newPlanName = name) }
    fun updateNewPlanDescription(desc: String) =
        _extra.update { it.copy(newPlanDescription = desc) }

    fun addMenuPlan() {
        val name = _extra.value.newPlanName.trim()
        if (name.isBlank()) return
        repository.addMenuPlan(name, _extra.value.newPlanDescription.trim())
        _extra.update {
            it.copy(
                showAddPlanDialog = false,
                newPlanName = "",
                newPlanDescription = ""
            )
        }
    }

    fun requestDeletePlan(plan: MenuPlan) =
        _extra.update { it.copy(pendingDeletePlan = plan, editingPlan = null) }

    fun dismissDeletePlanDialog() = _extra.update { it.copy(pendingDeletePlan = null) }
    fun confirmDeletePlan() {
        val plan = _extra.value.pendingDeletePlan ?: return
        _extra.update {
            it.copy(
                pendingDeletePlan = null,
                expandedPlanId = if (it.expandedPlanId == plan.id) null else it.expandedPlanId,
            )
        }
        if (repository.getLastExpandedMenuPlanId() == plan.id) {
            repository.saveLastExpandedMenuPlanId(null)
        }
        repository.deleteMenuPlan(plan.id)
    }

    fun requestEditPlan(plan: MenuPlan) = _extra.update {
        it.copy(
            editingPlan = plan,
            editPlanName = plan.name,
            editPlanDescription = plan.description,
            editPlanServings = plan.servings,
            editPlanRecipeIds = plan.recipeIds,
        )
    }

    fun dismissEditPlanDialog() = _extra.update {
        it.copy(
            editingPlan = null,
            editPlanName = "",
            editPlanDescription = "",
            editPlanServings = 0,
            editPlanRecipeIds = emptyList()
        )
    }

    fun updateEditPlanName(name: String) = _extra.update { it.copy(editPlanName = name) }
    fun updateEditPlanDescription(desc: String) =
        _extra.update { it.copy(editPlanDescription = desc) }

    fun updateEditPlanServings(servings: Int) =
        _extra.update { it.copy(editPlanServings = servings) }

    fun saveEditPlan() {
        val plan = _extra.value.editingPlan ?: return
        val name = _extra.value.editPlanName.trim()
        if (name.isBlank()) return
        repository.updateMenuPlan(
            plan.id, name, _extra.value.editPlanDescription.trim(),
            _extra.value.editPlanServings, _extra.value.editPlanRecipeIds,
        )
        _extra.update {
            it.copy(
                editingPlan = null,
                editPlanName = "",
                editPlanDescription = "",
                editPlanServings = 0,
                editPlanRecipeIds = emptyList()
            )
        }
    }

    fun reorderEditPlanRecipes(from: Int, to: Int) {
        _extra.update { state ->
            val ids = state.editPlanRecipeIds.toMutableList()
            val item = ids.removeAt(from)
            ids.add(to, item)
            state.copy(editPlanRecipeIds = ids)
        }
    }

    // ── Recipe dialogs ────────────────────────────────────────────────────────

    fun showAddRecipeDialog() = _extra.update { it.copy(showAddRecipeDialog = true) }
    fun dismissAddRecipeDialog() = _extra.update {
        it.copy(
            showAddRecipeDialog = false,
            newRecipeName = "",
            newRecipeDescription = ""
        )
    }

    fun updateNewRecipeName(name: String) = _extra.update { it.copy(newRecipeName = name) }
    fun updateNewRecipeDescription(desc: String) =
        _extra.update { it.copy(newRecipeDescription = desc) }

    fun addRecipe() {
        val name = _extra.value.newRecipeName.trim()
        if (name.isBlank()) return
        repository.addRecipe(name, _extra.value.newRecipeDescription.trim())
        _extra.update {
            it.copy(
                showAddRecipeDialog = false,
                newRecipeName = "",
                newRecipeDescription = ""
            )
        }
    }

    fun requestDeleteRecipe(recipe: Recipe) =
        _extra.update { it.copy(pendingDeleteRecipe = recipe) }

    fun dismissDeleteRecipeDialog() = _extra.update { it.copy(pendingDeleteRecipe = null) }
    fun confirmDeleteRecipe() {
        val recipe = _extra.value.pendingDeleteRecipe ?: return
        _extra.update { it.copy(pendingDeleteRecipe = null) }
        repository.deleteRecipe(recipe.id)
    }

    // ── Import recipe dialog ──────────────────────────────────────────────────

    fun showImportRecipeDialog() = _extra.update { it.copy(showImportRecipeDialog = true) }

    fun dismissImportRecipeDialog() = _extra.update {
        it.copy(
            showImportRecipeDialog = false,
            importRecipeName = "",
            importRecipeIngredients = "",
            importRecipeInstructions = "",
        )
    }

    fun updateImportRecipeName(name: String) = _extra.update { it.copy(importRecipeName = name) }
    fun updateImportRecipeIngredients(text: String) =
        _extra.update { it.copy(importRecipeIngredients = text) }

    fun updateImportRecipeInstructions(text: String) =
        _extra.update { it.copy(importRecipeInstructions = text) }

    fun importRecipe() {
        val extra = _extra.value
        val name = extra.importRecipeName.trim()
        if (name.isBlank()) return

        val ingredients = parseIngredients(extra.importRecipeIngredients)
        val steps = parseInstructions(extra.importRecipeInstructions)

        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            profileId = repository.getOrCreateProfileId(),
            name = name,
            ingredientSections = if (ingredients.isNotEmpty())
                listOf(IngredientSection(ingredients = ingredients)) else emptyList(),
            instructionSections = if (steps.isNotEmpty())
                listOf(InstructionSection(steps = steps)) else emptyList(),
        )
        repository.addRecipe(recipe)
        _extra.update {
            it.copy(
                showImportRecipeDialog = false,
                importRecipeName = "",
                importRecipeIngredients = "",
                importRecipeInstructions = "",
                editingRecipe = recipe,
            )
        }
    }

    // ── Recipe viewing ─────────────────────────────────────────────────────────

    fun openRecipeViewer(recipe: Recipe, menuPlanId: String? = null) {
        _extra.update { it.copy(viewingRecipe = recipe, viewingMenuPlanId = menuPlanId) }
        if (menuPlanId != null) {
            viewModelScope.launch {
                repository.syncRecipes()
                repository.syncMenuPlans()
                repository.pruneInvalidRecipeProgress(menuPlanId, recipe.id)
                if (_extra.value.viewingRecipe?.id == recipe.id &&
                    _extra.value.viewingMenuPlanId == menuPlanId
                ) {
                    repository.recipes.value
                        .firstOrNull { it.id == recipe.id }
                        ?.let { latest -> _extra.update { it.copy(viewingRecipe = latest) } }
                }
            }
        }
    }

    fun dismissRecipeViewer() {
        val fromMenuPlan = _extra.value.viewingMenuPlanId != null
        _extra.update { it.copy(viewingRecipe = null, viewingMenuPlanId = null) }
        if (fromMenuPlan) {
            viewModelScope.launch { repository.syncMenuPlans() }
        }
    }

    fun toggleStepCompletion(sectionIndex: Int, stepIndex: Int) {
        val planId = _extra.value.viewingMenuPlanId ?: return
        val recipeId = _extra.value.viewingRecipe?.id ?: return
        repository.toggleStepCompletion(planId, recipeId, sectionIndex, stepIndex)
    }

    fun startEditingFromViewer() {
        val recipe = _extra.value.viewingRecipe ?: return
        _extra.update { it.copy(viewingRecipe = null, editingRecipe = recipe) }
    }

    // ── Recipe editing ────────────────────────────────────────────────────────

    fun openRecipeEditor(recipe: Recipe) = _extra.update { it.copy(editingRecipe = recipe) }

    fun dismissRecipeEditor() {
        val extra = _extra.value
        _extra.update { it.copy(editingRecipe = null, viewingRecipe = extra.editingRecipe) }
    }

    fun updateEditingRecipe(recipe: Recipe) = _extra.update { it.copy(editingRecipe = recipe) }

    fun saveEditingRecipe() {
        val recipe = _extra.value.editingRecipe ?: return
        val trimmed = recipe.copy(
            name = recipe.name.trim(),
            description = recipe.description.trim(),
            courseType = recipe.courseType.trim(),
            durability = recipe.durability.trim(),
            nutritionFacts = recipe.nutritionFacts.trim(),
            tips = recipe.tips,
            ingredientSections = recipe.ingredientSections.map { s ->
                s.copy(
                    title = s.title.trim(),
                    ingredients = s.ingredients.map { i ->
                        i.copy(
                            name = i.name.trim().capitalizeIngredientFirstLetter(),
                            quantity = i.quantity.trim(),
                            unit = i.unit.trim()
                        )
                    }
                )
            },
            instructionSections = recipe.instructionSections.map { s ->
                s.copy(title = s.title.trim(), steps = s.steps.map { it.trim() })
            },
        )
        repository.updateRecipe(trimmed)
        _extra.update { it.copy(editingRecipe = null, viewingRecipe = trimmed) }
    }

    fun addIngredientSection() {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                    ingredientSections = recipe.ingredientSections + IngredientSection()
                )
            )
        }
    }

    fun removeIngredientSection(index: Int) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                ingredientSections = recipe.ingredientSections.filterIndexed { i, _ -> i != index }
            ))
        }
    }

    fun updateIngredientSectionTitle(sectionIndex: Int, title: String) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                ingredientSections = recipe.ingredientSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(title = title) else s
                }
            ))
        }
    }

    fun addIngredient(sectionIndex: Int) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                ingredientSections = recipe.ingredientSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(ingredients = s.ingredients + Ingredient()) else s
                }
            ))
        }
    }

    fun removeIngredient(sectionIndex: Int, ingredientIndex: Int) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                ingredientSections = recipe.ingredientSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(
                        ingredients = s.ingredients.filterIndexed { j, _ -> j != ingredientIndex }
                    ) else s
                }
            ))
        }
    }

    fun updateIngredient(sectionIndex: Int, ingredientIndex: Int, ingredient: Ingredient) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                ingredientSections = recipe.ingredientSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(
                        ingredients = s.ingredients.mapIndexed { j, ing ->
                            if (j == ingredientIndex) ingredient else ing
                        }
                    ) else s
                }
            ))
        }
    }

    fun reorderIngredients(newSections: List<dk.joachim.shopping.data.IngredientSection>) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(editingRecipe = recipe.copy(ingredientSections = newSections))
        }
    }

    fun reorderInstructions(newSections: List<dk.joachim.shopping.data.InstructionSection>) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(editingRecipe = recipe.copy(instructionSections = newSections))
        }
    }

    fun addInstructionSection() {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                    instructionSections = recipe.instructionSections + InstructionSection()
                )
            )
        }
    }

    fun removeInstructionSection(index: Int) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                instructionSections = recipe.instructionSections.filterIndexed { i, _ -> i != index }
            ))
        }
    }

    fun updateInstructionSectionTitle(sectionIndex: Int, title: String) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                instructionSections = recipe.instructionSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(title = title) else s
                }
            ))
        }
    }

    fun addInstructionStep(sectionIndex: Int) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                instructionSections = recipe.instructionSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(steps = s.steps + "") else s
                }
            ))
        }
    }

    fun removeInstructionStep(sectionIndex: Int, stepIndex: Int) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                instructionSections = recipe.instructionSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(
                        steps = s.steps.filterIndexed { j, _ -> j != stepIndex }
                    ) else s
                }
            ))
        }
    }

    fun updateInstructionStep(sectionIndex: Int, stepIndex: Int, text: String) {
        val recipe = _extra.value.editingRecipe ?: return
        _extra.update {
            it.copy(
                editingRecipe = recipe.copy(
                instructionSections = recipe.instructionSections.mapIndexed { i, s ->
                    if (i == sectionIndex) s.copy(
                        steps = s.steps.mapIndexed { j, step -> if (j == stepIndex) text else step }
                    ) else s
                }
            ))
        }
    }

    // ── Plan ↔ Recipe linking ─────────────────────────────────────────────────

    fun addRecipeToMenuPlan(planId: String, recipeId: String) =
        repository.addRecipeToMenuPlan(planId, recipeId)

    fun createMenuPlanAndAddRecipe(planName: String, recipeId: String) {
        val planId = repository.addMenuPlan(planName.trim())
        repository.addRecipeToMenuPlan(planId, recipeId)
        _extra.update {
            it.copy(
                viewingRecipe = null,
                viewingMenuPlanId = null,
                expandedPlanId = planId,
                searchQuery = "",
            )
        }
        repository.saveLastExpandedMenuPlanId(planId)
    }

    fun requestRemoveRecipeFromMenuPlan(planId: String, recipeId: String) {
        val plan = repository.menuPlans.value.firstOrNull { it.id == planId } ?: return
        val recipe = repository.recipes.value.firstOrNull { it.id == recipeId } ?: return
        _extra.update {
            it.copy(
                pendingRemoveRecipeFromPlan = PendingRemoveRecipeFromPlan(
                    planId = planId,
                    planName = plan.name,
                    recipeId = recipeId,
                    recipeName = recipe.name,
                ),
            )
        }
    }

    fun dismissRemoveRecipeFromMenuPlanDialog() =
        _extra.update { it.copy(pendingRemoveRecipeFromPlan = null) }

    fun confirmRemoveRecipeFromMenuPlan() {
        val pending = _extra.value.pendingRemoveRecipeFromPlan ?: return
        _extra.update { it.copy(pendingRemoveRecipeFromPlan = null) }
        repository.removeRecipeFromMenuPlan(pending.planId, pending.recipeId)
    }

    fun updateMenuPlan(planId: String, name: String, servings: Int) =
        repository.updateMenuPlan(planId, name, servings = servings)

    fun togglePlanExpanded(planId: String) {
        _extra.update {
            val next = if (it.expandedPlanId == planId) null else planId
            repository.saveLastExpandedMenuPlanId(next)
            it.copy(expandedPlanId = next)
        }
    }
}

private val knownUnits = listOf(
    "dl", "ml", "cl", "l", "g", "kg",
    "tsk", "spsk", "stk",
    "fed", "bundt", "dåse", "dåser",
    "pk", "pakke", "pakker", "pose", "poser",
    "glas", "skive", "skiver", "knivspids", "nip",
    "tb", "tbsp", "tsp", "cup", "cups", "oz", "lb", "lbs",
)

private val quantityRegex = Regex("""^([\d.,/]+\s*[½⅓¼⅔¾]?|[½⅓¼⅔¾])""")
private val unitRegex = Regex(
    "^(${knownUnits.joinToString("|")})\\.?\\b\\s*",
    RegexOption.IGNORE_CASE,
)

private fun parseIngredientLine(line: String): Ingredient? {
    val trimmed = line.trim()
    if (trimmed.isBlank()) return null

    val qtyMatch = quantityRegex.find(trimmed)
    val quantity = qtyMatch?.value?.trim().orEmpty()
    val afterQty =
        if (qtyMatch != null) trimmed.substring(qtyMatch.range.last + 1).trim() else trimmed

    val unitMatch = unitRegex.find(afterQty)
    val unit = unitMatch?.groupValues?.get(1)?.trim().orEmpty()
    val name =
        if (unitMatch != null) afterQty.substring(unitMatch.range.last + 1).trim() else afterQty

    if (name.isBlank() && quantity.isBlank()) return null
    return Ingredient(name = name.capitalizeIngredientFirstLetter(), quantity = quantity, unit = unit)
}

private fun parseIngredients(text: String): List<Ingredient> =
    text.lines().mapNotNull { parseIngredientLine(it) }

private fun parseInstructions(text: String): List<String> =
    text.split(Regex("(?<=[.!?])\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
