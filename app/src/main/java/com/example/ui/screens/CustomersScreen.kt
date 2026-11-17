package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BalanceType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.PartyType
import com.example.ui.components.CustomerCard
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer

enum class CustomerFilter {
    ALL,
    YOU_WILL_GET,
    YOU_WILL_GIVE,
    SETTLED
}

enum class CustomerSort {
    RECENT,
    NAME,
    BALANCE_HIGH
}

private val CustomerFabBerry = Color(0xFF9D174D)
private val SupplierFabTeal = Color(0xFF0F766E)

@Composable
fun CustomersScreen(
    customers: List<CustomerWithBalance>,
    suppliers: List<CustomerWithBalance> = emptyList(),
    currency: String,
    strings: AppStrings.Dictionary,
    onCustomerClick: (CustomerWithBalance) -> Unit,
    onAddCustomerClick: () -> Unit,
    onAddSupplierClick: () -> Unit = onAddCustomerClick,
    onOpenCashbookClick: () -> Unit = {},
    onViewReportsClick: () -> Unit = {},
    onReminderClick: (CustomerWithBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activePartyTab by remember { mutableStateOf(PartyType.CUSTOMER) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CustomerFilter.ALL) }
    var selectedSort by remember { mutableStateOf(CustomerSort.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }

    val activeParties = if (activePartyTab == PartyType.SUPPLIER) suppliers else customers

    val filteredParties = remember(activeParties, searchQuery, selectedFilter, selectedSort) {
        activeParties.filter { cust ->
            val matchesSearch = cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                CustomerFilter.ALL -> true
                CustomerFilter.YOU_WILL_GET -> cust.balanceType == BalanceType.YOU_WILL_GET
                CustomerFilter.YOU_WILL_GIVE -> cust.balanceType == BalanceType.YOU_WILL_GIVE
                CustomerFilter.SETTLED -> cust.balanceType == BalanceType.SETTLED
            }
            matchesSearch && matchesFilter
        }.let { list ->
            when (selectedSort) {
                CustomerSort.RECENT -> list.sortedByDescending { it.lastTransactionDate ?: 0L }
                CustomerSort.NAME -> list.sortedBy { it.name.lowercase() }
                CustomerSort.BALANCE_HIGH -> list.sortedByDescending { kotlin.math.abs(it.netBalance) }
            }
        }
    }

    val totalGet = activeParties.filter { it.netBalance > 0.009 }.sumOf { it.netBalance }
    val totalGive = activeParties.filter { it.netBalance < -0.009 }.sumOf { -it.netBalance }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // CUSTOMERS | SUPPLIERS tab bar matching Khatabook Parties view
            TabRow(
                selectedTabIndex = activePartyTab.ordinal,
                containerColor = KhatabookBlue,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activePartyTab.ordinal]),
                        height = 3.dp,
                        color = Color.White
                    )
                }
            ) {
                Tab(
                    selected = activePartyTab == PartyType.CUSTOMER,
                    onClick = {
                        activePartyTab = PartyType.CUSTOMER
                        selectedFilter = CustomerFilter.ALL
                    },
                    text = {
                        Text(
                            text = strings.customers.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (activePartyTab == PartyType.CUSTOMER) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.testTag("parties_tab_customers")
                )
                Tab(
                    selected = activePartyTab == PartyType.SUPPLIER,
                    onClick = {
                        activePartyTab = PartyType.SUPPLIER
                        selectedFilter = CustomerFilter.ALL
                    },
                    text = {
                        Text(
                            text = strings.suppliers.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (activePartyTab == PartyType.SUPPLIER) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.testTag("parties_tab_suppliers")
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary Banner (You will give | You will get | View Reports)
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = strings.totalPayable,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$currency%.0f".format(totalGive),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PaymentGreen
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(34.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = strings.totalReceivable,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$currency%.0f".format(totalGet),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CreditRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KhatabookBlue.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onViewReportsClick() }
                                    .testTag("parties_view_reports_button")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = KhatabookBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.viewReports,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KhatabookBlue
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = KhatabookBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search, Filters & Cashbook Quick Action Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = if (activePartyTab == PartyType.SUPPLIER) strings.searchSupplier else strings.searchCustomer,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_customer_input")
                        )

                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = strings.filter)
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Recent First") },
                                    onClick = {
                                        selectedSort = CustomerSort.RECENT
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)") },
                                    onClick = {
                                        selectedSort = CustomerSort.NAME
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Highest Balance") },
                                    onClick = {
                                        selectedSort = CustomerSort.BALANCE_HIGH
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KhatabookBlue.copy(alpha = 0.12f),
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenCashbookClick() }
                                .testTag("parties_cashbook_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = strings.cashbook,
                                    tint = KhatabookBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.cashbook,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KhatabookBlue
                                )
                            }
                        }
                    }
                }

                // Filter Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == CustomerFilter.ALL,
                                onClick = { selectedFilter = CustomerFilter.ALL },
                                label = { Text("${strings.allCustomers} (${activeParties.size})") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == CustomerFilter.YOU_WILL_GET,
                                onClick = { selectedFilter = CustomerFilter.YOU_WILL_GET },
                                label = { Text("${strings.filterYouWillGet} (${activeParties.count { it.balanceType == BalanceType.YOU_WILL_GET }})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CreditRedContainer,
                                    selectedLabelColor = CreditRed
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == CustomerFilter.YOU_WILL_GIVE,
                                onClick = { selectedFilter = CustomerFilter.YOU_WILL_GIVE },
                                label = { Text("${strings.filterYouWillGive} (${activeParties.count { it.balanceType == BalanceType.YOU_WILL_GIVE }})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PaymentGreenContainer,
                                    selectedLabelColor = PaymentGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == CustomerFilter.SETTLED,
                                onClick = { selectedFilter = CustomerFilter.SETTLED },
                                label = { Text("${strings.filterSettled} (${activeParties.count { it.balanceType == BalanceType.SETTLED }})") }
                            )
                        }
                    }
                }

                // List or Empty State Illustration
                if (filteredParties.isEmpty()) {
                    item {
                        val isSupplier = activePartyTab == PartyType.SUPPLIER
                        val hasSearchOrFilter = searchQuery.isNotEmpty() || selectedFilter != CustomerFilter.ALL
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (isSupplier) R.drawable.empty_supplier_illustration else R.drawable.empty_customer_illustration
                                ),
                                contentDescription = if (isSupplier) strings.noSuppliersYet else strings.noCustomersYet,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(190.dp)
                                    .clip(RoundedCornerShape(22.dp))
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = when {
                                    hasSearchOrFilter && isSupplier -> "No matching suppliers"
                                    hasSearchOrFilter -> "No matching customers"
                                    isSupplier -> strings.addSuppliersPrompt
                                    else -> strings.addCustomersPrompt
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    hasSearchOrFilter -> "Try clearing your search or filter selection"
                                    isSupplier -> strings.addFirstSupplier
                                    else -> strings.addFirstCustomer
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (!hasSearchOrFilter) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = if (isSupplier) onAddSupplierClick else onAddCustomerClick,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSupplier) strings.addSupplier else strings.addCustomer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredParties, key = { it.id }) { party ->
                        CustomerCard(
                            customer = party,
                            currency = currency,
                            strings = strings,
                            onClick = { onCustomerClick(party) },
                            onCallClick = { ReportExporter.safeDialPhone(context, party.phone) },
                            onReminderClick = { onReminderClick(party) }
                        )
                    }
                }
            }
        }

        // Distinct Add Customer (berry) / Add Supplier (teal) FAB pill
        val isSupplierTab = activePartyTab == PartyType.SUPPLIER
        FloatingActionButton(
            onClick = if (isSupplierTab) onAddSupplierClick else onAddCustomerClick,
            containerColor = if (isSupplierTab) SupplierFabTeal else CustomerFabBerry,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_customer_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = if (isSupplierTab) strings.addSupplier else strings.addCustomer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = (if (isSupplierTab) strings.addSupplier else strings.addCustomer).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
