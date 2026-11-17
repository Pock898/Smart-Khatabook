package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.model.DashboardSummary
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCustomer
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

sealed interface TransactionUiState {
    data object Loading : TransactionUiState
    data class Success(
        val transactions: List<TransactionWithCustomer>,
        val summary: DashboardSummary
    ) : TransactionUiState
    data class Error(val message: String) : TransactionUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    application: Application,
    private val repository: KhatabookRepository,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = KhatabookRepository(AppDatabase.getDatabase(application)),
        prefs = PreferencesManager.getInstance(application)
    )

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val allTransactions: StateFlow<List<TransactionWithCustomer>> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.getAllTransactionsWithCustomer(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerTransactions: StateFlow<List<TransactionEntity>> =
        _selectedCustomerId.flatMapLatest { customerId ->
            if (customerId == null) flowOf(emptyList()) else repository.getCustomerTransactions(customerId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id <= 0) flowOf(DashboardSummary()) else repository.getDashboardSummary(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val uiState: StateFlow<TransactionUiState> = combine(
        allTransactions,
        dashboardSummary
    ) { transactions, summary ->
        TransactionUiState.Success(
            transactions = transactions,
            summary = summary
        ) as TransactionUiState
    }
        .catch { emit(TransactionUiState.Error(it.localizedMessage ?: "Failed to load transactions")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionUiState.Loading)

    private suspend fun resolveActiveBusinessId(): Long {
        val current = prefs.activeBusinessId.value
        if (current > 0) return current
        val busId = repository.ensureDefaultBusiness()
        prefs.setActiveBusinessId(busId)
        return busId
    }

    fun selectCustomer(customerId: Long?) {
        _selectedCustomerId.value = customerId
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addTransaction(
        customerId: Long,
        amount: Double,
        type: TransactionType,
        paymentMethod: String = "CASH",
        description: String = "",
        billNumber: String = "",
        transactionDate: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        if (customerId <= 0L) {
            _userMessage.value = "Please select a customer"
            return
        }
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
            _userMessage.value = "Amount must be greater than zero"
            return
        }
        viewModelScope.launch {
            try {
                val businessId = resolveActiveBusinessId()
                val id = repository.addTransaction(
                    businessId = businessId,
                    customerId = customerId,
                    amount = amount,
                    type = type,
                    paymentMethod = paymentMethod,
                    description = description,
                    billNumber = billNumber,
                    transactionDate = transactionDate,
                    dueDate = dueDate
                )
                _userMessage.value = if (type == TransactionType.CREDIT) {
                    "Credit (Gave) entry added"
                } else {
                    "Payment received added"
                }
                onSuccess(id)
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to add transaction"
            }
        }
    }

    fun updateTransaction(
        transaction: TransactionEntity,
        onSuccess: () -> Unit = {}
    ) {
        if (transaction.amount <= 0.0 || transaction.amount.isNaN() || transaction.amount.isInfinite()) {
            _userMessage.value = "Amount must be greater than zero"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                _userMessage.value = "Transaction updated"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to update transaction"
            }
        }
    }

    fun deleteTransaction(
        transactionId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transactionId)
                _userMessage.value = "Transaction deleted"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to delete transaction"
            }
        }
    }
}
