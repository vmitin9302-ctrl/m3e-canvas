package dev.mitin.internal

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthEntryHistoryTest {
    private class History : AuthEntryHistory {
        override var returning = false
        override fun rememberAccount() { returning = true }
    }
    private class Storage : RefreshStore {
        var stored: StoredSession? = null
        override suspend fun read() = stored
        override suspend fun save(refresh: Secret) { stored = StoredSession(refresh) }
        override suspend fun beginExchange() { stored = StoredSession(null, true) }
        override suspend fun clear() { stored = null }
    }
    @Test fun cleanInstallAndClearDataUseRegistration() = runBlocking {
        val history = History()
        val store = RememberingRefreshStore(Storage(), history)
        assertNull(store.read())
        assertEquals("register", initialAuthMode(history.returning, true))
        assertEquals("login", initialAuthMode(history.returning, false))
        store.clear()
        assertFalse(history.returning)
    }
    @Test fun logoutClearsCredentialsButRetainsLoginEntry() = runBlocking {
        val history = History(); val secure = Storage()
        val store = RememberingRefreshStore(secure, history)
        store.save(Secret("synthetic-refresh"))
        assertNotNull(secure.stored)
        store.clear()
        assertNull(secure.stored)
        assertEquals("login", initialAuthMode(history.returning, true))
        // A new session manager reads no credentials; UX history does not create them.
        assertNull(RememberingRefreshStore(secure, history).read())
    }
    @Test fun upgradeAndInterruptedExchangeRememberExistingAccount() = runBlocking {
        for(saved in listOf(StoredSession(Secret("synthetic")), StoredSession(null,true))) {
            val secure = Storage().apply { stored = saved }; val history = History()
            val store = RememberingRefreshStore(secure, history)
            assertSame(saved,store.read())
            store.clear()
            assertEquals("login",initialAuthMode(history.returning,true))
        }
    }
}
