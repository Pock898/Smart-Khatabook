package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CustomerUiState {
    data object Loading : CustomerUiState
    data class Success(
        val customers: List<CustomerWithBalance>,
        val totalReceivable: Double,
        val totalPayable: Double
    ) : CustomerUiState
    data class Error(val message: String) : CustomerUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModel(
    application: Application,
    private val repository: KhatabookRepository,
    private val prefs: PreferencesManager
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = KhatabookRepository(AppDatabase.getDatabase(application)),
        prefs = PreferencesManager.getInstance(application)
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<BalanceType?>(null)
    val filterType: StateFlow<BalanceType?> = _filterType.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.getCustomersWithBalance(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliersWithBalance: StateFlow<List<CustomerWithBalance>> =
        prefs.activeBusinessId.flatMapLatest { id ->
            if (id <= 0) flowOf(emptyList()) else repository.getSuppliersWithBalance(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(
        customersWithBalance,
        _searchQuery,
        _filterType
    ) { customers, query, filter ->
        customers.filter { customer ->
            val matchesQuery = query.isBlank() ||
                customer.name.contains(query, ignoreCase = true) ||
                customer.phone.contains(query, ignoreCase = true)
            val matchesFilter = filter == null || customer.balanceType == filter
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<CustomerUiState> = filteredCustomers
        .map<List<CustomerWithBalance>, CustomerUiState> { list ->
            val receivable = list.filter { it.netBalance > 0.009 }.sumOf { it.netBalance }
            val payable = list.filter { it.netBalance < -0.009 }.sumOf { -it.netBalance }
            CustomerUiState.Success(
                customers = list,
                totalReceivable = receivable,
                totalPayable = payable
            )
        }
        .catch { emit(CustomerUiState.Error(it.localizedMessage ?: "Failed to load customers")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerUiState.Loading)

    val selectedCustomer: StateFlow<CustomerWithBalance?> = _selectedCustomerId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getCustomerWithBalance(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private suspend fun resolveActiveBusinessId(): Long {
        val current = prefs.activeBusinessId.value
        if (current > 0) return current
        val busId = repository.ensureDefaultBusiness()
        prefs.setActiveBusinessId(busId)
        return busId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: BalanceType?) {
        _filterType.value = type
    }

    fun selectCustomer(customerId: Long?) {
        _selectedCustomerId.value = customerId
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addCustomer(
        name: String,
        phone: String = "",
        address: String = "",
        openingBalance: Double = 0.0,
        openingBalanceType: String = "GET",
        partyType: String = "CUSTOMER",
        onSuccess: (Long) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        val label = if (partyType.equals("SUPPLIER", ignoreCase = true)) "Supplier" else "Customer"
        if (trimmedName.isBlank()) {
            _userMessage.value = "$label name is required"
            return
        }
        val safeOpeningBalance = openingBalance.coerceAtLeast(0.0)
        viewModelScope.launch {
            try {
                val businessId = resolveActiveBusinessId()
                val id = repository.addCustomer(
                    businessId = businessId,
                    name = trimmedName,
                    phone = phone.trim(),
                    address = address.trim(),
                    openingBalance = safeOpeningBalance,
                    openingBalanceType = openingBalanceType,
                    partyType = partyType
                )
                _userMessage.value = "$label added: $trimmedName"
                onSuccess(id)
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to add ${label.lowercase()}"
            }
        }
    }

    fun updateCustomer(
        customer: CustomerEntity,
        onSuccess: () -> Unit = {}
    ) {
        if (customer.name.isBlank()) {
            _userMessage.value = "Customer name is required"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateCustomer(customer.copy(name = customer.name.trim()))
                _userMessage.value = "Customer updated"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to update customer"
            }
        }
    }

    fun deleteCustomer(
        customerId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customerId)
                if (_selectedCustomerId.value == customerId) {
                    _selectedCustomerId.value = null
                }
                _userMessage.value = "Customer deleted"
                onSuccess()
            } catch (e: Exception) {
                _userMessage.value = e.localizedMessage ?: "Unable to delete customer"
            }
        }
    }
}
