package dev.mitin.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    class Store : RefreshStore {
        var record: StoredSession? = null
        var saves = 0; var failSave = false; var failRead = false
        override suspend fun read(): StoredSession? { if (failRead) throw AuthFailure(); return record }
        override suspend fun save(refresh: Secret) { if (failSave) throw AuthFailure(); saves++; record = StoredSession(refresh) }
        override suspend fun beginExchange() { record = StoredSession(null, true) }
        override suspend fun clear() { record = null }
    }
    class Api : AuthApi {
        var rotations = 0; var logins = 0; var meCalls = 0; var logoutCalls = 0
        var failRefresh = false; var failLogout = false
        var gate: CompletableDeferred<Unit>? = null
        var started = CompletableDeferred<Unit>()
        var late = false
        var meHandler: (suspend (Secret) -> Me)? = null
        var account = "A"
        fun pair() = TokenPair(Secret(account.repeat(200)), Secret("mdr1_" + ('a' + rotations).toString().repeat(43)), 10)
        override suspend fun login(email: String, password: Secret, epoch: Long): TokenPair { account = email; logins++; return pair() }
        override suspend fun refresh(token: Secret, epoch: Long): TokenPair {
            rotations++; started.complete(Unit)
            if (late) withContext(NonCancellable) { gate?.await() } else gate?.await()
            if (failRefresh) throw AuthFailure()
            return pair()
        }
        override suspend fun me(access: Secret, epoch: Long): Me { meCalls++; return meHandler?.invoke(access) ?: Me(account, account, account, "client") }
        override suspend fun sessions(access: Secret, epoch: Long, offset: Int) = SessionPage(emptyList(), null)
        override suspend fun revoke(access: Secret, epoch: Long, id: String) { }
        override suspend fun logout(access: Secret, epoch: Long, all: Boolean) { logoutCalls++; if (failLogout) throw AuthFailure() }
        override fun cancel(epoch: Long) { }
    }
    private class Fixture(scope: CoroutineScope) {
        val api = Api(); val store = Store(); var clock = 0L
        val manager = SessionManager(api, store, scope) { clock }
        suspend fun login() = manager.login("A", Secret("synthetic-password-only"))
    }
    @Test fun loginObtainsRealProfileAndOnlyRefreshIsStored() = runTest {
        val f=Fixture(backgroundScope); f.login()
        assertEquals("A", f.manager.state.value.profile?.displayName)
        assertEquals(1, f.api.logins); assertEquals(1,f.store.saves)
        assertFalse(f.store.record.toString().contains("synthetic-password-only"))
    }
    @Test fun eightExpiredRequestsShareOneFlight() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.clock=11_000
        f.api.gate=CompletableDeferred()
        val jobs=List(8){async { f.manager.loadMe() }}
        runCurrent(); assertEquals(1,f.api.rotations); assertTrue(f.store.record!!.exchanging)
        f.api.gate!!.complete(Unit); jobs.awaitAll()
        assertEquals(1,f.api.rotations); assertFalse(f.store.record!!.exchanging)
    }
    @Test fun cancellationOfOneWaiterDoesNotCancelRefresh() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.clock=11_000; f.api.gate=CompletableDeferred()
        val first=async { f.manager.loadMe() }; val second=async { f.manager.loadMe() }; runCurrent()
        first.cancel(); runCurrent(); assertEquals(1,f.api.rotations)
        f.api.gate!!.complete(Unit); assertEquals("A",second.await().displayName)
    }
    @Test fun responseLossClearsStateAndNeverRetriesOldRefresh() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.clock=11_000; f.api.failRefresh=true
        assertTrue(runCatching { f.manager.loadMe() }.isFailure)
        assertNull(f.store.record); assertNull(f.manager.state.value.profile)
        f.manager.restore(); assertEquals(1,f.api.rotations)
    }
    @Test fun restartWithExchangeMarkerRequiresLoginWithoutNetwork() = runTest {
        val f=Fixture(backgroundScope); f.store.record=StoredSession(null,true)
        f.manager.restore(); assertNull(f.store.record); assertEquals(0,f.api.rotations)
        assertFalse(f.manager.state.value.restoring); assertNull(f.manager.state.value.profile)
    }
    @Test fun restartRestoresThroughServerAndRotates() = runTest {
        val f=Fixture(backgroundScope); f.store.record=StoredSession(Secret("mdr1_"+"a".repeat(43)))
        f.manager.restore(); assertEquals(1,f.api.rotations); assertEquals(1,f.api.meCalls)
        assertEquals("A", f.manager.state.value.profile?.displayName)
    }
    @Test fun saveFailureCannotPublishNewPairOrKeepProfile() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.clock=11_000; f.store.failSave=true
        assertTrue(runCatching { f.manager.loadMe() }.isFailure)
        assertNull(f.store.record); assertNull(f.manager.state.value.profile)
    }
    @Test fun storageFailureDuringRestoreFailsClosed() = runTest {
        val f=Fixture(backgroundScope); f.store.failRead=true; f.manager.restore()
        assertFalse(f.manager.state.value.restoring); assertNull(f.manager.state.value.profile)
    }
    @Test fun getRetriesOnceThenClosesOn401() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.api.meHandler={throw AuthFailure(401)}
        assertTrue(runCatching { f.manager.loadMe() }.isFailure)
        assertEquals(1,f.api.rotations); assertEquals(3,f.api.meCalls)
        assertNull(f.manager.state.value.profile)
    }
    @Test fun lateRefreshAfterLogoutCannotResurrectProfile() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.clock=11_000
        f.api.gate=CompletableDeferred(); f.api.late=true
        val wait=async { runCatching { f.manager.loadMe() } }; runCurrent()
        f.manager.logout(); f.api.gate!!.complete(Unit); runCurrent(); wait.await()
        assertNull(f.manager.state.value.profile); assertNull(f.store.record)
    }
    @Test fun old401MayNotRetryAsNewAccount() = runTest {
        val f=Fixture(backgroundScope); f.login()
        val gate=CompletableDeferred<Unit>()
        f.api.meHandler={ access -> if(access.value.startsWith("A")){gate.await();throw AuthFailure(401)} else Me("B","B","B","client") }
        val old=async { runCatching { f.manager.loadMe() } }; runCurrent()
        f.manager.login("B",Secret("synthetic-password-only")); gate.complete(Unit); old.await()
        assertEquals("B",f.manager.state.value.profile?.displayName); assertEquals(0,f.api.rotations)
    }
    @Test fun offlineLogoutIsLocalOnly() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.api.failLogout=true
        assertFalse(f.manager.logout().serverRevoked); assertNull(f.store.record)
        assertTrue(f.manager.state.value.message!!.contains("не подтверждён"))
    }
    @Test fun logoutAllConfirmedOnlyAfterServerSuccess() = runTest {
        val f=Fixture(backgroundScope); f.login(); assertTrue(f.manager.logout(true).serverRevoked)
        f.login(); f.api.failLogout=true; assertFalse(f.manager.logout(true).serverRevoked)
        assertNull(f.manager.state.value.profile)
    }
    @Test fun rateLimitAndUnavailableDoNotLoop() = runTest {
        for(status in listOf(403,404,429,503)) {
            val f=Fixture(backgroundScope); f.login(); f.api.meHandler={throw AuthFailure(status)}
            assertTrue(runCatching { f.manager.loadMe() }.isFailure)
            assertEquals(0,f.api.rotations); assertEquals(2,f.api.meCalls)
        }
    }
    @Test fun failedExchangeDoesNotCancelIndependentNextLogin() = runTest {
        val f=Fixture(backgroundScope); f.login(); f.clock=11_000; f.api.failRefresh=true
        assertTrue(runCatching { f.manager.loadMe() }.isFailure)
        assertNull(f.store.record); assertNull(f.manager.state.value.profile)
        f.api.failRefresh=false
        f.manager.login("B", Secret("synthetic-password-only"))
        assertEquals("B", f.manager.state.value.profile?.displayName)
        assertEquals(2, f.api.logins)
    }
    @Test fun secretDiagnosticsAreRedacted() {
        val value="synthetic-marker-never-log"
        assertFalse(Secret(value).toString().contains(value))
        assertFalse(TokenPair(Secret(value),Secret(value),10).toString().contains(value))
    }
}
