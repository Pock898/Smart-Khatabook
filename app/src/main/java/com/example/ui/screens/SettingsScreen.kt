package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.FirebaseAuthHelper
import com.example.data.local.BusinessEntity
import com.example.ui.components.BusinessProfileDialog
import com.example.ui.localization.AppStrings
import com.example.ui.pdf.ReportExporter
import com.example.ui.theme.CreditRed
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.PaymentGreen

@Composable
fun SettingsScreen(
    activeBusiness: BusinessEntity?,
    allBusinesses: List<BusinessEntity>,
    loggedInPhone: String,
    currentLang: String,
    currentCurrency: String,
    isPinEnabled: Boolean,
    currentTheme: String,
    upiId: String = "",
    recycleBinCount: Int = 0,
    strings: AppStrings.Dictionary,
    onSelectBusiness: (Long) -> Unit,
    onUpdateBusiness: (BusinessEntity) -> Unit,
    onCreateBusiness: (name: String, owner: String, phone: String, address: String, gst: String) -> Unit,
    onDeleteBusiness: (BusinessEntity) -> Unit = {},
    onSetLanguage: (String) -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetPin: (pin: String, enabled: Boolean, securityAnswer: String) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onRestoreBackup: (String, (Boolean) -> Unit) -> Unit,
    onLoadDemoData: () -> Unit,
    onOpenCashbook: () -> Unit = {},
    onOpenBills: () -> Unit = {},
    onOpenItems: () -> Unit = {},
    onOpenStaff: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenDashboard: () -> Unit = {},
    onOpenPaymentSettings: () -> Unit = {},
    onOpenRecycleBin: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditBusinessDialog by remember { mutableStateOf(false) }
    var showNewBusinessDialog by remember { mutableStateOf(false) }
    var showDeleteBusinessConfirm by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDemoDataConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(true) }

    val profileFieldsCount = remember(activeBusiness, upiId) {
        var count = 0
        if (!activeBusiness?.name.isNullOrBlank() && activeBusiness?.name != "My Business") count++
        if (!activeBusiness?.ownerName.isNullOrBlank()) count++
        if (!activeBusiness?.phone.isNullOrBlank()) count++
        if (!activeBusiness?.address.isNullOrBlank()) count++
        if (upiId.isNotBlank()) count++
        count.coerceIn(1, 5)
    }
    val profileStrength = profileFieldsCount / 5f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Business Profile + Profile Strength Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KhatabookBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = KhatabookBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showEditBusinessDialog = true }
                            ) {
                                Text(
                                    text = activeBusiness?.name ?: strings.appName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Edit business",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Profile Strength: ${(profileStrength * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { profileStrength },
                                color = if (profileStrength >= 0.8f) PaymentGreen else KhatabookBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        IconButton(
                            onClick = { showEditBusinessDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Edit profile")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Businesses (${allBusinesses.size})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showNewBusinessDialog = true }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addNewBusiness, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (allBusinesses.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allBusinesses, key = { it.id }) { bus ->
                                FilterChip(
                                    selected = bus.id == activeBusiness?.id,
                                    onClick = { onSelectBusiness(bus.id) },
                                    label = { Text(bus.name, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // UPI Bank Collection Banner (PROCEED -> Payment Settings)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = KhatabookBlue.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = KhatabookBlue,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (upiId.isBlank()) "Collect payments via UPI" else "UPI Active: $upiId",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (upiId.isBlank()) "Add your UPI ID to share with customers" else "Customers can pay straight to your UPI",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onOpenPaymentSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = KhatabookBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(if (upiId.isBlank()) "PROCEED" else "EDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dedicated Quick App Lock Card (matching reference screenshot)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = KhatabookBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.appLockLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isPinEnabled,
                        onCheckedChange = { checked ->
                            if (checked) showPinDialog = true else onSetPin("", false, "")
                        }
                    )
                }
            }
        }

        // 3-Column Feature Grid (Every tile opens a real implemented destination)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HubGridTile(
                            icon = Icons.Default.MenuBook,
                            label = strings.cashbook,
                            tint = KhatabookBlue,
                            onClick = onOpenCashbook,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_tile_cashbook")
                        )
                        HubGridTile(
                            icon = Icons.Default.ReceiptLong,
                            label = strings.bills,
                            tint = Color(0xFFEA580C),
                            onClick = onOpenBills,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_tile_bills")
                        )
                        HubGridTile(
                            icon = Icons.Default.Inventory2,
                            label = strings.items,
                            tint = Color(0xFF0F766E),
                            onClick = onOpenItems,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_tile_items")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HubGridTile(
                            icon = Icons.Default.Badge,
                            label = strings.staff,
                            tint = Color(0xFF7C3AED),
                            onClick = onOpenStaff,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_tile_staff")
                        )
                        HubGridTile(
                            icon = Icons.Default.Payment,
                            label = strings.collection,
                            tint = Color(0xFFD97706),
                            onClick = onOpenCollection,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_tile_collection")
                        )
                        HubGridTile(
                            icon = Icons.Default.Assessment,
                            label = strings.reports,
                            tint = PaymentGreen,
                            onClick = onOpenReports,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_tile_reports")
                        )
                    }
                }
            }
        }

        // Expandable Settings Section matching screenshots
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsExpanded = !settingsExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = KhatabookBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings.settings,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = KhatabookBlue,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (settingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = settingsExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            SettingsRowItem(
                                icon = Icons.Default.Payment,
                                title = strings.paymentSettings,
                                subtitle = if (upiId.isBlank()) "Configure UPI ID & default payment mode" else "UPI: $upiId",
                                onClick = onOpenPaymentSettings,
                                testTag = "settings_payment_row"
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SettingsRowItem(
                                icon = Icons.Default.RestoreFromTrash,
                                title = if (recycleBinCount > 0) "${strings.recycleBin} ($recycleBinCount)" else strings.recycleBin,
                                subtitle = "Restore deleted parties, transactions & cashbook entries",
                                onClick = onOpenRecycleBin,
                                testTag = "settings_recycle_bin_row"
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Inline App Lock row with Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = KhatabookBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(strings.appLockLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = if (isPinEnabled) "4-digit PIN lock is ACTIVE" else "Protect app access with PIN",
                                        fontSize = 12.sp,
                                        color = if (isPinEnabled) PaymentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPinEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) showPinDialog = true else onSetPin("", false, "")
                                    },
                                    modifier = Modifier.testTag("pin_lock_switch")
                                )
                            }
                            if (isPinEnabled) {
                                TextButton(onClick = { showPinDialog = true }) {
                                    Text(strings.changePin)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SettingsRowItem(
                                icon = Icons.Default.Language,
                                title = strings.language,
                                subtitle = if (currentLang == "hi") "हिंदी (Hindi)" else "English",
                                onClick = { showLanguageDialog = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SettingsRowItem(
                                icon = Icons.Default.MonetizationOn,
                                title = strings.currency,
                                subtitle = "$currentCurrency  •  ${when (currentTheme) {
                                    "LIGHT" -> "Light"
                                    "DARK" -> "Dark"
                                    else -> "System"
                                }}",
                                onClick = { showCurrencyDialog = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SettingsRowItem(
                                icon = Icons.Default.DarkMode,
                                title = strings.theme,
                                subtitle = when (currentTheme) {
                                    "LIGHT" -> "Light Mode"
                                    "DARK" -> "Dark Mode"
                                    else -> "System Default"
                                },
                                onClick = { showThemeDialog = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SettingsRowItem(
                                icon = Icons.Default.CloudDone,
                                title = strings.backupInfo,
                                subtitle = "Export JSON backup, restore data, or load sample shop",
                                onClick = { showBackupDialog = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SettingsRowItem(
                                icon = Icons.Default.DeleteForever,
                                title = strings.deleteKhata,
                                subtitle = "Delete active khata (${activeBusiness?.name ?: "My Business"})",
                                iconTint = CreditRed,
                                onClick = { showDeleteBusinessConfirm = true }
                            )
                        }
                    }
                }
            }
        }

        // Help & Support + About Us
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = strings.helpAndSupport,
                        subtitle = "FAQs, Cashbook tips & offline backup guide",
                        onClick = { showHelpDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = strings.aboutUs,
                        subtitle = "Version 1.0.0 • Smart Khatabook Ledger",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }

        // Invite Friends Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ReportExporter.safeShareText(
                                context = context,
                                chooserTitle = "Invite Friends",
                                subject = "Smart Khatabook - Digital Ledger",
                                text = "Manage customer credit, suppliers, cashbook, staff attendance, and PDF statements easily with Smart Khatabook!"
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = KhatabookBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = KhatabookBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Invite your friends to use Khatabook",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.inviteFriends,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KhatabookBlue
                        )
                    }
                }
            }
        }

        // Account & Phone Session (Logout)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = KhatabookBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.loggedInAs, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = FirebaseAuthHelper.currentVerifiedLabel()
                                ?: loggedInPhone.ifBlank { activeBusiness?.phone ?: "Offline Ledger" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { showLogoutConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CreditRed),
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = strings.logout,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.logout, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEditBusinessDialog && activeBusiness != null) {
        BusinessProfileDialog(
            business = activeBusiness,
            onDismiss = { showEditBusinessDialog = false },
            onSave = { name, owner, phone, address, gst ->
                onUpdateBusiness(
                    activeBusiness.copy(
                        name = name,
                        ownerName = owner,
                        phone = phone,
                        address = address,
                        gstNumber = gst
                    )
                )
                showEditBusinessDialog = false
            }
        )
    }

    if (showNewBusinessDialog) {
        BusinessProfileDialog(
            business = null,
            isNew = true,
            onDismiss = { showNewBusinessDialog = false },
            onSave = { name, owner, phone, address, gst ->
                onCreateBusiness(name, owner, phone, address, gst)
                showNewBusinessDialog = false
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(strings.backupAndRestore, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsRowItem(
                        icon = Icons.Default.Upload,
                        title = strings.createBackup,
                        subtitle = "Export JSON backup file to Drive, Files, or WhatsApp",
                        onClick = {
                            showBackupDialog = false
                            onExportBackup { json ->
                                ReportExporter.safeShareText(
                                    context = context,
                                    chooserTitle = "Share/Save Khatabook Backup",
                                    subject = "Smart Khatabook Backup (${activeBusiness?.name ?: "Store"})",
                                    text = json
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                    SettingsRowItem(
                        icon = Icons.Default.Download,
                        title = strings.restoreBackup,
                        subtitle = "Restore customers, suppliers, cashbook & items from JSON",
                        onClick = {
                            showBackupDialog = false
                            showRestoreDialog = true
                        }
                    )
                    HorizontalDivider()
                    SettingsRowItem(
                        icon = Icons.Default.Storage,
                        title = strings.loadDemoData,
                        subtitle = "Populate sample customers, udhar, payments & expenses",
                        onClick = {
                            showBackupDialog = false
                            showDemoDataConfirm = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDeleteBusinessConfirm && activeBusiness != null) {
        AlertDialog(
            onDismissRequest = { showDeleteBusinessConfirm = false },
            title = { Text("${strings.deleteKhata}?", fontWeight = FontWeight.Bold, color = CreditRed) },
            text = {
                Text("This will permanently delete \"${activeBusiness.name}\" along with its parties, cashbook, expenses, and items.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteBusinessConfirm = false
                        onDeleteBusiness(activeBusiness)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBusinessConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(strings.helpAndSupport, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Parties (Customers & Suppliers): Track money to get and pay separately, with WhatsApp/SMS reminders and PDF statements.", fontSize = 13.sp)
                    Text("• Cashbook: Record daily Cash and Online IN/OUT using the built-in calculator keypad.", fontSize = 13.sp)
                    Text("• Recycle Bin: Restore deleted parties, transactions, or cashbook entries anytime.", fontSize = 13.sp)
                    Text("• Backup: Use Backup Information to export/restore a complete JSON snapshot.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("OK") }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select App Language", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetLanguage("en")
                                showLanguageDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = currentLang == "en", onClick = {
                            onSetLanguage("en")
                            showLanguageDialog = false
                        })
                        Text("English", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetLanguage("hi")
                                showLanguageDialog = false
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = currentLang == "hi", onClick = {
                            onSetLanguage("hi")
                            showLanguageDialog = false
                        })
                        Text("हिंदी (Hindi)", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency Symbol", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("₹" to "Indian Rupee (₹)", "$" to "US Dollar ($)", "€" to "Euro (€)", "£" to "British Pound (£)").forEach { (symbol, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetCurrency(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = currentCurrency == symbol, onClick = {
                                onSetCurrency(symbol)
                                showCurrencyDialog = false
                            })
                            Text(name, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select App Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("SYSTEM" to "System Default", "LIGHT" to "Light Mode", "DARK" to "Dark Mode").forEach { (mode, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetTheme(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = currentTheme == mode, onClick = {
                                onSetTheme(mode)
                                showThemeDialog = false
                            })
                            Text(name, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var securityAnswerInput by remember {
            mutableStateOf(activeBusiness?.name?.ifBlank { "My Business" } ?: "My Business")
        }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                pinInput = input
                                pinError = null
                            }
                        },
                        label = { Text("Enter 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_pin_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                confirmPinInput = input
                                pinError = null
                            }
                        },
                        label = { Text("Confirm 4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_pin_confirm_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = securityAnswerInput,
                        onValueChange = {
                            securityAnswerInput = it
                            pinError = null
                        },
                        label = { Text("Recovery Answer (Shop Name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = CreditRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            pinInput.length != 4 || !pinInput.all { it.isDigit() } -> {
                                pinError = "PIN must be exactly 4 numeric digits"
                            }
                            pinInput != confirmPinInput -> {
                                pinError = "PINs do not match"
                            }
                            securityAnswerInput.isBlank() -> {
                                pinError = "Recovery answer is required for Forgot PIN"
                            }
                            else -> {
                                onSetPin(pinInput, true, securityAnswerInput.trim())
                                showPinDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestoreDialog) {
        var restoreText by remember { mutableStateOf("") }
        var restoreResultMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Backup (JSON Content)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste your exported JSON backup text below:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = {
                            restoreText = it
                            restoreResultMsg = null
                        },
                        placeholder = { Text("{\"version\": 1, ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    if (restoreResultMsg != null) {
                        Text(
                            text = restoreResultMsg!!,
                            color = CreditRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreText.isBlank()) {
                            restoreResultMsg = "Please paste backup JSON first"
                            return@Button
                        }
                        onRestoreBackup(restoreText) { success ->
                            if (success) {
                                showRestoreDialog = false
                            } else {
                                restoreResultMsg = "Invalid JSON backup format"
                            }
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDemoDataConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoDataConfirm = false },
            title = { Text("Load Sample Shop Demo Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will add 4 realistic sample customers with credit/payment transactions and sample daily expenses so you can immediately test all charts, reports, and ledgers.") },
            confirmButton = {
                Button(
                    onClick = {
                        onLoadDemoData()
                        showDemoDataConfirm = false
                    }
                ) {
                    Text("Load Sample Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoDataConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Smart Khatabook", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Smart Khatabook is a digital credit account ledger book for shopkeepers and small businesses to record customer & supplier ledgers, daily cashbook, items, staff attendance, and expenses.", fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• 100% Offline persistent Room Database\n• Cashbook with built-in Calculator Keypad\n• Customers & Suppliers ledgers\n• Dual-language: English & Hindi\n• WhatsApp payment reminders & PDF statements", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(strings.logout, fontWeight = FontWeight.Bold, color = CreditRed) },
            text = { Text(strings.logoutConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditRed),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text(strings.logout)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun HubGridTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = KhatabookBlue,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 8.dp)
        .let { if (testTag != null) it.testTag(testTag) else it }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}
