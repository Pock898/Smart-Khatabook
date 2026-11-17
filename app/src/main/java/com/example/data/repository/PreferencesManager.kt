package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("khatabook_prefs", Context.MODE_PRIVATE)

    private val _activeBusinessId = MutableStateFlow(prefs.getLong(KEY_ACTIVE_BUSINESS_ID, -1L))
    val activeBusinessId: StateFlow<Long> = _activeBusinessId.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, "₹") ?: "₹")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInPhone = MutableStateFlow(prefs.getString(KEY_LOGGED_IN_PHONE, "") ?: "")
    val loggedInPhone: StateFlow<String> = _loggedInPhone.asStateFlow()

    private val _upiId = MutableStateFlow(prefs.getString(KEY_UPI_ID, "") ?: "")
    val upiId: StateFlow<String> = _upiId.asStateFlow()

    private val _defaultCashbookMode = MutableStateFlow(prefs.getString(KEY_DEFAULT_CASHBOOK_MODE, "CASH") ?: "CASH")
    val defaultCashbookMode: StateFlow<String> = _defaultCashbookMode.asStateFlow()

    private val _hasSeenCashbookGuide = MutableStateFlow(prefs.getBoolean(KEY_CASHBOOK_GUIDE_SEEN, false))
    val hasSeenCashbookGuide: StateFlow<Boolean> = _hasSeenCashbookGuide.asStateFlow()

    fun loginWithPhone(fullPhone: String) {
        val cleaned = fullPhone.trim()
        prefs.edit()
            .putString(KEY_LOGGED_IN_PHONE, cleaned)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        _loggedInPhone.value = cleaned
        _isLoggedIn.value = true
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
        _isLoggedIn.value = false
    }

    fun setActiveBusinessId(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE_BUSINESS_ID, id).apply()
        _activeBusinessId.value = id
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply()
        _currency.value = currency
    }

    fun setPin(pin: String, enabled: Boolean) {
        prefs.edit()
            .putString(KEY_PIN_CODE, pin)
            .putBoolean(KEY_PIN_ENABLED, enabled)
            .apply()
        _isPinEnabled.value = enabled
    }

    fun verifyPin(enteredPin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_CODE, "") ?: ""
        return stored.isNotEmpty() && stored == enteredPin
    }

    fun setSecurityQuestion(question: String, answer: String) {
        prefs.edit()
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER, answer.trim().lowercase())
            .apply()
    }

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SECURITY_QUESTION, "What is your shop or business name?") ?: "What is your shop or business name?"
    }

    fun getSecurityAnswer(): String {
        return prefs.getString(KEY_SECURITY_ANSWER, "") ?: ""
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val stored = prefs.getString(KEY_SECURITY_ANSWER, "") ?: ""
        return stored.isNotBlank() && stored.trim().lowercase() == answer.trim().lowercase()
    }

    fun setBusinessIdForAccount(accountLabel: String, businessId: Long) {
        val key = normalizeAccountKey(accountLabel)
        if (key.isBlank() || businessId <= 0L) return
        prefs.edit()
            .putLong("$KEY_ACCOUNT_BIZ_PREFIX$key", businessId)
            .putString("$KEY_BIZ_OWNER_PREFIX$businessId", key)
            .apply()
    }

    fun getBusinessIdForAccount(accountLabel: String): Long {
        val key = normalizeAccountKey(accountLabel)
        if (key.isBlank()) return -1L
        return prefs.getLong("$KEY_ACCOUNT_BIZ_PREFIX$key", -1L)
    }

    fun getAccountOwnerForBusiness(businessId: Long): String {
        if (businessId <= 0L) return ""
        return prefs.getString("$KEY_BIZ_OWNER_PREFIX$businessId", "") ?: ""
    }

    fun clearBusinessOwnerMapping(businessId: Long) {
        if (businessId <= 0L) return
        val ownerKey = getAccountOwnerForBusiness(businessId)
        prefs.edit().apply {
            remove("$KEY_BIZ_OWNER_PREFIX$businessId")
            if (ownerKey.isNotBlank()) {
                remove("$KEY_ACCOUNT_BIZ_PREFIX$ownerKey")
            }
        }.apply()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setPaymentSettings(upiId: String, defaultMode: String) {
        val cleanUpi = upiId.trim()
        val cleanMode = if (defaultMode == "ONLINE") "ONLINE" else "CASH"
        prefs.edit()
            .putString(KEY_UPI_ID, cleanUpi)
            .putString(KEY_DEFAULT_CASHBOOK_MODE, cleanMode)
            .apply()
        _upiId.value = cleanUpi
        _defaultCashbookMode.value = cleanMode
    }

    fun markCashbookGuideSeen() {
        prefs.edit().putBoolean(KEY_CASHBOOK_GUIDE_SEEN, true).apply()
        _hasSeenCashbookGuide.value = true
    }

    companion object {
        private const val KEY_ACTIVE_BUSINESS_ID = "active_business_id"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_CURRENCY = "currency_symbol"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGGED_IN_PHONE = "logged_in_phone"
        private const val KEY_ACCOUNT_BIZ_PREFIX = "account_biz_"
        private const val KEY_BIZ_OWNER_PREFIX = "biz_owner_"
        private const val KEY_UPI_ID = "upi_id"
        private const val KEY_DEFAULT_CASHBOOK_MODE = "default_cashbook_mode"
        private const val KEY_CASHBOOK_GUIDE_SEEN = "cashbook_guide_seen"

        @Volatile
        private var instance: PreferencesManager? = null

        @Volatile
        private var boundAppContext: Context? = null

        fun getInstance(context: Context): PreferencesManager {
            val appContext = context.applicationContext ?: context
            val existing = instance
            if (existing != null && boundAppContext === appContext) {
                return existing
            }
            return synchronized(this) {
                val rechecked = instance
                if (rechecked != null && boundAppContext === appContext) {
                    rechecked
                } else {
                    PreferencesManager(appContext).also {
                        instance = it
                        boundAppContext = appContext
                    }
                }
            }
        }

        fun normalizeAccountKey(accountLabel: String): String {
            return accountLabel.trim().lowercase().replace(Regex("\\s+"), " ")
        }
    }
}
