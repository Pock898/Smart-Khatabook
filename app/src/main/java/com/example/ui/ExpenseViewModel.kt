package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ExpenseEntity
import com.example.data.model.CategoryExpenseSummary
import com.example.data.repository.KhatabookRepository
import com.example.data.repository.PreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExpenseUiState {
    data object Loading : ExpenseUiState
    data class Success(
        val expenses: List<ExpenseEntity>,
        val categoryBreakdown: List<CategoryExpenseSummary>,
        val totalExpenses: Double
    ) : ExpenseUiState
    data class Error(val message: String) : ExpenseUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(
    application: Application,
    private val repository: KhatabookRepository,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = KhatabookRepository(AppDatabase.getDatabase(application)),
        prefs = PreferencesManager.getInstance(application)
    )

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val allExpenses: StateFlow<List<ExpenseEntity>> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.getExpenses(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryExpenses: StateFlow<List<CategoryExpenseSummary>> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.getCategoryExpenseSummary(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        _selectedCategory
    ) { expenses, category ->
        if (category.isNullOrBlank()) expenses else expenses.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ExpenseUiState> = combine(
        filteredExpenses,
        categoryExpenses
    ) { expenses, breakdown ->
        ExpenseUiState.Success(
            expenses = expenses,
            categoryBreakdown = breakdown,
            totalExpenses = expenses.sumOf { it.amount }
        ) as ExpenseUiState
    }
        .catch { emit(ExpenseUiState.Error(it.localizedMessage ?: "Failed to load expenses")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseUiState.Loading)

    private suspend fun resolveActiveBusinessId(): Long {
        val current = prefs.activeBusinessId.value
        if (current > 0) return current
        val busId = repository.ensureDefaultBusiness()
        prefs.setActiveBusinessId(busId)
        return busId
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addExpense(
        amount: Double,
        category: String,
        paymentMethod: String = "CASH",
        description: String = "",
        expenseDate: Long = System.currentTimeMillis(),
        onSuccess: (Long) -> Unit = {}
    ) {
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
            _userMessage.value = "Expense amount must be greater than zero"
            return
        }
        val cleanCategory = category.trim().ifBlank { "OTHER" }
        viewModelScope.launch {
            try {
                val businessId = resolveActiveBusinessId()
                val id = repository.addExpense(
                    businessId = businessId,
                    amount = amount,
                    category = cleanCategory,
                    paymentMethod = paymentMethod,
                    description = description,
                    expenseDate = expenseDate
                )
                _userMessage.value = "Expense recorded"
                onSuccess(id)
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to add expense"
            }
        }
    }

    fun updateExpense(
        expense: ExpenseEntity,
        onSuccess: () -> Unit = {}
    ) {
        if (expense.amount <= 0.0 || expense.amount.isNaN() || expense.amount.isInfinite()) {
            _userMessage.value = "Expense amount must be greater than zero"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateExpense(expense)
                _userMessage.value = "Expense updated"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to update expense"
            }
        }
    }

    fun deleteExpense(
        expenseId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                _userMessage.value = "Expense deleted"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to delete expense"
            }
        }
    }
}
