package dev.mitin.internal

import android.content.Context

internal class LocalAuthEntryHistory(context: Context) : AuthEntryHistory {
    private val preferences = context.getSharedPreferences("auth_entry_ux", Context.MODE_PRIVATE)
    override val returning: Boolean get() = preferences.getBoolean("returning_account", false)
    override fun rememberAccount() {
        // Only this UX boolean belongs in preferences. Credentials stay in KeystoreRefreshStore.
        preferences.edit().putBoolean("returning_account", true).apply()
    }
}
