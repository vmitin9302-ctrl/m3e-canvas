package dev.mitin.internal

/** UX history only. Never grants access or replaces server session validation. */
interface AuthEntryHistory {
    val returning: Boolean
    fun rememberAccount()
}

class RememberingRefreshStore(
    private val secureStore: RefreshStore,
    private val history: AuthEntryHistory,
) : RefreshStore {
    override suspend fun read(): StoredSession? = try {
        secureStore.read().also {
            // Includes interrupted exchanges: an existing installation returns to login.
            if (it != null) history.rememberAccount()
        }
    } catch(failure: Exception) {
        // An unreadable old encrypted store is not evidence of a clean install.
        history.rememberAccount()
        throw failure
    }
    override suspend fun save(refresh: Secret) {
        secureStore.save(refresh)
        history.rememberAccount()
    }
    override suspend fun beginExchange() = secureStore.beginExchange()
    override suspend fun clear() = secureStore.clear()
}

internal fun initialAuthMode(returning: Boolean, registration: Boolean) =
    if (returning || !registration) "login" else "register"
