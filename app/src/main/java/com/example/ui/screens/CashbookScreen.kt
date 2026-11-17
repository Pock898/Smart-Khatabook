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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BusinessEntity
import com.example.data.local.CashbookEntity
import com.example.data.model.CashbookDayGroup
import com.example.data.model.CashbookEntryUi
import com.example.data.model.CashbookPaymentMode
import com.example.data.model.CashbookSummary
import com.example.data.model.CashbookType
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.CreditRed
import com.example.ui.theme.CreditRedContainer
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.PaymentGreen
import com.example.ui.theme.PaymentGreenContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CashbookScreen(
    business: BusinessEntity?,
    summary: CashbookSummary,
    dayGroups: List<CashbookDayGroup>,
    rawEntries: List<CashbookEntity>,
    currency: String,
    strings: AppStrings.Dictionary,
    hasSeenGuide: Boolean,
    onDismissGuide: () -> Unit,
    onBackClick: () -> Unit,
    onAddOutClick: () -> Unit,
    onAddInClick: () -> Unit,
    onEditEntry: (CashbookEntryUi) -> Unit,
    onDeleteEntry: (CashbookEntryUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showGuideDialog by remember(hasSeenGuide) { mutableStateOf(!hasSeenGuide) }
    var showReportDialog by remember { mutableStateOf(false) }
    val todayHeaderLabel = remember {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = KhatabookBlue,
                contentColor = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cashbook_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.cashbook,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cashbook_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Cashbook Guide"
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAddOutClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CreditRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cashbook_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.cashOut,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = onAddInClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaymentGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cashbook_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.cashIn,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Balance Summary Header Card (side-by-side Total & Today Balance)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KhatabookBlue)
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BalanceColumnCard(
                                    title = strings.totalBalance,
                                    total = summary.totalBalance,
                                    cashInHand = summary.totalCashInHand,
                                    online = summary.totalOnline,
                                    currency = currency,
                                    strings = strings,
                                    modifier = Modifier.weight(1f)
                                )
                                BalanceColumnCard(
                                    title = strings.todaysBalance,
                                    total = summary.todayBalance,
                                    cashInHand = summary.todayCashInHand,
                                    online = summary.todayOnline,
                                    currency = currency,
                                    strings = strings,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showReportDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("view_cashbook_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = KhatabookBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.viewCashbookReport,
                                    fontWeight = FontWeight.Bold,
                                    color = KhatabookBlue,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            if (dayGroups.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = todayHeaderLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "0 Entry",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(strings.cashOut, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency 0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CreditRed)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(strings.cashIn, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency 0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaymentGreen)
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.empty_cashbook_illustration),
                            contentDescription = strings.cashbookEmptyMsg,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.cashbookEmptyMsg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.addFirstEntry,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = KhatabookBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                items(dayGroups, key = { it.dateKey }) { dayGroup ->
                    CashbookDaySection(
                        dayGroup = dayGroup,
                        currency = currency,
                        strings = strings,
                        onEditEntry = onEditEntry,
                        onDeleteEntry = onDeleteEntry
                    )
                }
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = {
                showGuideDialog = false
                onDismissGuide()
            },
            title = {
                Text(
                    text = "Manage your daily cash transactions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GuideFeatureRow(
                        icon = Icons.Default.Today,
                        title = strings.todaysBalance,
                        description = "Track daily money IN and OUT to verify your closing cash counter every evening."
                    )
                    GuideFeatureRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "${strings.cashInHand} & ${strings.online}",
                        description = "Separate physical drawer cash from UPI / bank transfers automatically."
                    )
                    GuideFeatureRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "PDF & CSV Reports",
                        description = "Generate shareable Cashbook statements anytime in one tap."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuideDialog = false
                        onDismissGuide()
                    },
                    modifier = Modifier.testTag("cashbook_guide_okay_button")
                ) {
                    Text("OKAY", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = strings.viewCashbookReport,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose how you want to export or share your Cashbook ledger.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showReportDialog = false
                            ReportExporter.generateAndShareCashbookPdf(
                                context = context,
                                business = business,
                                summary = summary,
                                entries = rawEntries,
                                currency = currency
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportPdf, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showReportDialog = false
                            ReportExporter.shareCashbookCsv(
                                context = context,
                                business = business,
                                summary = summary,
                                entries = rawEntries,
                                currency = currency
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportCsv, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun BalanceColumnCard(
    title: String,
    total: Double,
    cashInHand: Double,
    online: Double,
    currency: String,
    strings: AppStrings.Dictionary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatSignedCurrency(total, currency),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (total < -0.009) CreditRed else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.cashInHand,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatSignedCurrency(cashInHand, currency),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.online,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatSignedCurrency(online, currency),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CashbookDaySection(
    dayGroup: CashbookDayGroup,
    currency: String,
    strings: AppStrings.Dictionary,
    onEditEntry: (CashbookEntryUi) -> Unit,
    onDeleteEntry: (CashbookEntryUi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        // Day summary bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = dayGroup.dateLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${dayGroup.entryCount} ${if (dayGroup.entryCount == 1) "Entry" else "Entries"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = strings.cashOut,
                        fontSize = 10.sp,
                        color = CreditRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currency%.2f".format(dayGroup.totalOut),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreditRed
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = strings.cashIn,
                        fontSize = 10.sp,
                        color = PaymentGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currency%.2f".format(dayGroup.totalIn),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaymentGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        dayGroup.entries.forEach { entry ->
            CashbookEntryRow(
                entry = entry,
                currency = currency,
                strings = strings,
                onEdit = { onEditEntry(entry) },
                onDelete = { onDeleteEntry(entry) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CashbookEntryRow(
    entry: CashbookEntryUi,
    currency: String,
    strings: AppStrings.Dictionary,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val isOut = entry.type == CashbookType.OUT
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (entry.paymentMode == CashbookPaymentMode.ONLINE) {
                            KhatabookBlue.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (entry.paymentMode == CashbookPaymentMode.ONLINE) strings.online else strings.cashMode,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.paymentMode == CashbookPaymentMode.ONLINE) KhatabookBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = timeFormatter.format(Date(entry.entryDate)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.note,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOut) CreditRedContainer else PaymentGreenContainer
            ) {
                Text(
                    text = "${if (isOut) "-" else "+"} $currency%.2f".format(entry.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOut) CreditRed else PaymentGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Entry Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = CreditRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CreditRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = KhatabookBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = KhatabookBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSignedCurrency(value: Double, currency: String): String {
    return if (value < -0.009) {
        "-$currency%.2f".format(-value)
    } else {
        "$currency%.2f".format(value)
    }
}
