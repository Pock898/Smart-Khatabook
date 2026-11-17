package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.auth.FirebaseAuthHelper
import com.example.ui.localization.AppStrings
import com.example.ui.theme.CreditRed
import com.example.ui.theme.KhatabookBlue
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PaymentGreen

@Composable
fun PhoneLoginScreen(
    initialPhone: String,
    initialShopName: String,
    isPinEnabled: Boolean,
    securityQuestion: String,
    currentLang: String,
    strings: AppStrings.Dictionary,
    onToggleLang: () -> Unit,
    onLoginSubmit: (countryCode: String, phoneDigits: String, shopName: String, pin: String, startFresh: Boolean) -> Boolean,
    onGoogleLoginSuccess: (accountLabel: String, shopName: String, startFresh: Boolean) -> Unit,
    onResetPinViaSecurity: (answer: String, newPin: String) -> Boolean,
    onSkipOffline: (shopName: String, startFresh: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val countryOptions = remember {
        listOf(
            "+91" to "India (+91)",
            "+1" to "USA (+1)",
            "+44" to "UK (+44)",
            "+971" to "UAE (+971)",
            "+977" to "Nepal (+977)",
            "+880" to "Bangladesh (+880)"
        )
    }

    val supportedCodes = remember(countryOptions) { countryOptions.map { it.first } }
    val initialParts = remember(initialPhone) {
        FirebaseAuthHelper.extractPhoneParts(initialPhone, supportedCodes)
    }

    var selectedCountryCode by remember { mutableStateOf(initialParts.first) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var phoneDigits by remember { mutableStateOf(initialParts.second) }
    var shopName by remember { mutableStateOf(if (initialShopName == "My Business") "" else initialShopName) }
    var phoneEditedByUser by remember { mutableStateOf(false) }
    var shopEditedByUser by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var googleErrorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var isNewAccountMode by remember { mutableStateOf(false) }

    LaunchedEffect(initialPhone) {
        if (!phoneEditedByUser && !isNewAccountMode) {
            val parts = FirebaseAuthHelper.extractPhoneParts(initialPhone, supportedCodes)
            if (parts.second.isNotBlank()) {
                selectedCountryCode = parts.first
                phoneDigits = parts.second
            }
        }
    }

    LaunchedEffect(initialShopName) {
        if (!shopEditedByUser && !isNewAccountMode && initialShopName.isNotBlank() && initialShopName != "My Business") {
            shopName = initialShopName
        }
    }

    fun attemptLogin() {
        googleErrorMessage = null
        val cleaned = FirebaseAuthHelper.normalizePhoneDigits(selectedCountryCode, phoneDigits)
        phoneDigits = cleaned
        if (!FirebaseAuthHelper.isValidPhoneDigits(cleaned)) {
            phoneError = true
            return
        }
        val success = onLoginSubmit(selectedCountryCode, cleaned, shopName, pinInput, isNewAccountMode)
        if (!success) {
            pinError = true
        }
    }

    fun startGoogleSignIn() {
        googleErrorMessage = null
        isGoogleSigningIn = true
        FirebaseAuthHelper.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = { label ->
                isGoogleSigningIn = false
                onGoogleLoginSuccess(label, shopName, isNewAccountMode)
            },
            onAuthError = { errorMsg ->
                isGoogleSigningIn = false
                googleErrorMessage = errorMsg
            },
            onAuthCancelled = {
                isGoogleSigningIn = false
                googleErrorMessage = strings.googleSignInCancelledHint
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NavyPrimary, KhatabookBlue)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Language Switcher Pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onToggleLang() }
                        .testTag("login_language_toggle")
                ) {
                    Text(
                        text = if (currentLang == "hi") "हिंदी / EN" else "EN / हिंदी",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = strings.appName,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.appName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = strings.loginSubtitle,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.widthIn(max = 360.dp)
                    )
                }
            }

            // Overlapping Login Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 18.dp)
                    .offset(y = (-24).dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Login vs New Account Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (!isNewAccountMode) KhatabookBlue else Color.Transparent)
                                .clickable(enabled = !isGoogleSigningIn) {
                                    isNewAccountMode = false
                                    googleErrorMessage = null
                                    phoneError = false
                                    pinError = false
                                    if (phoneDigits.isBlank()) {
                                        phoneEditedByUser = false
                                        val parts = FirebaseAuthHelper.extractPhoneParts(initialPhone, supportedCodes)
                                        selectedCountryCode = parts.first
                                        phoneDigits = parts.second
                                    }
                                    if (shopName.isBlank()) {
                                        shopEditedByUser = false
                                        if (initialShopName.isNotBlank() && initialShopName != "My Business") {
                                            shopName = initialShopName
                                        }
                                    }
                                }
                                .testTag("login_mode_existing"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.existingLoginTab,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!isNewAccountMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isNewAccountMode) KhatabookBlue else Color.Transparent)
                                .clickable(enabled = !isGoogleSigningIn) {
                                    isNewAccountMode = true
                                    phoneEditedByUser = true
                                    shopEditedByUser = true
                                    phoneDigits = ""
                                    shopName = ""
                                    phoneError = false
                                    pinError = false
                                    googleErrorMessage = null
                                }
                                .testTag("login_mode_new_account"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.newAccountTab,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isNewAccountMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isNewAccountMode) strings.newAccountTitle else strings.loginTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isNewAccountMode) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.newAccountZeroHint,
                            fontSize = 12.sp,
                            color = PaymentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Country Code + Phone Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isGoogleSigningIn) { showCountryMenu = true }
                                    .testTag("login_country_code_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = selectedCountryCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select country code",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCountryMenu,
                                onDismissRequest = { showCountryMenu = false }
                            ) {
                                countryOptions.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedCountryCode = code
                                            showCountryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = phoneDigits,
                            onValueChange = { input ->
                                phoneEditedByUser = true
                                phoneDigits = FirebaseAuthHelper.normalizePhoneDigits(selectedCountryCode, input)
                                phoneError = false
                                googleErrorMessage = null
                            },
                            label = { Text(strings.enterMobileNumber) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            enabled = !isGoogleSigningIn,
                            isError = phoneError,
                            supportingText = {
                                if (phoneError) {
                                    Text(strings.invalidPhone, color = CreditRed)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("login_phone_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = {
                            shopEditedByUser = true
                            shopName = it
                        },
                        label = { Text(strings.shopNameOptional) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        enabled = !isGoogleSigningIn,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (isPinEnabled) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (!isPinEnabled && !isGoogleSigningIn) attemptLogin() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_shop_name_input")
                    )

                    if (isPinEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { input ->
                                if (input.length <= 4 && input.all { it.isDigit() }) {
                                    pinInput = input
                                    pinError = false
                                    googleErrorMessage = null
                                }
                            },
                            label = { Text(strings.enterPin) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            enabled = !isGoogleSigningIn,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = pinError,
                            supportingText = {
                                if (pinError) {
                                    Text(strings.pinIncorrect, color = CreditRed)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (!isGoogleSigningIn) attemptLogin() }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_pin_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showForgotPinDialog = true },
                                enabled = !isGoogleSigningIn,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("login_forgot_pin_button")
                            ) {
                                Text(
                                    text = strings.forgotPin,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KhatabookBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { attemptLogin() },
                        enabled = !isGoogleSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KhatabookBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text(
                            text = strings.continueLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    if (!googleErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CreditRed.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = CreditRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = googleErrorMessage!!,
                                    color = CreditRed,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = " ${strings.orDivider} ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { startGoogleSignIn() },
                        enabled = !isGoogleSigningIn,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isGoogleSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = KhatabookBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.signInWithGoogle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!isPinEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                googleErrorMessage = null
                                onSkipOffline(shopName, isNewAccountMode)
                            },
                            enabled = !isGoogleSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("skip_login_offline_button")
                        ) {
                            Text(
                                text = strings.continueOffline,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = KhatabookBlue
                            )
                        }
                    }
                }
            }

            // Trust / Offline badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = PaymentGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.safeOfflineBadge,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showForgotPinDialog) {
        var answerInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var resetError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = { Text(strings.resetPinTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = securityQuestion,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = {
                            answerInput = it
                            resetError = null
                        },
                        label = { Text(strings.securityAnswer) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                newPinInput = input
                                resetError = null
                            }
                        },
                        label = { Text(strings.enterNewPin) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetError != null) {
                        Text(
                            text = resetError!!,
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
                        if (newPinInput.length != 4) {
                            resetError = strings.invalidNewPinError
                        } else if (onResetPinViaSecurity(answerInput, newPinInput)) {
                            pinInput = newPinInput
                            pinError = false
                            showForgotPinDialog = false
                        } else {
                            resetError = strings.invalidSecurityAnswerError
                        }
                    }
                ) {
                    Text(strings.resetPinButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
