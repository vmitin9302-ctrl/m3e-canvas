package dev.mitin.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mitin.demo.BuildConfig
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

const val TEST_PASSWORD = "  synthetic mobile test password  " // instrumentation APK ONLY
const val CLIENT_A = "client-a@example.com"
const val CLIENT_B = "client-b@example.com"

@RunWith(AndroidJUnit4::class)
class NetworkE2ETest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun api(port: Int = 8443) = HttpAuthApi("https://localhost:$port/")
    private class CountingApi(val remote: AuthApi) : AuthApi by remote {
        private val count=java.util.concurrent.atomic.AtomicInteger()
        val refreshes get()=count.get()
        var callsStarted=CompletableDeferred<Unit>()
        override suspend fun refresh(token: Secret, epoch: Long): TokenPair { count.incrementAndGet(); callsStarted.complete(Unit); return remote.refresh(token,epoch) }
    }
    private suspend fun scenario(port: Int = 8443, action: suspend (SessionManager, CountingApi, KeystoreRefreshStore) -> Unit) {
        assertTrue(BuildConfig.API_BASE_URL == "https://localhost:8443/")
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
        val store=KeystoreRefreshStore(context,"test_"+UUID.randomUUID().toString().replace("-",""))
        val remote=CountingApi(api(port));val manager=SessionManager(remote,store,scope)
        try { action(manager,remote,store) } finally { manager.logout();store.clear();scope.cancel() }
    }
    @Test fun clientIdentitySessionOwnershipAndRevocation() = runBlocking {
        scenario { manager,remote,_ ->
            assertEquals("Тестовый клиент А",manager.login(CLIENT_A,Secret(TEST_PASSWORD)).displayName)
            val aId=manager.state.value.profile!!.userId
            val otherDevice=remote.remote.login(CLIENT_A,Secret(TEST_PASSWORD),100)
            val currentOther=remote.remote.sessions(otherDevice.access,100,0).items.single { it.current }
            val b=remote.remote.login(CLIENT_B,Secret(TEST_PASSWORD),200)
            val bMe=remote.remote.me(b.access,200);assertNotEquals(aId,bMe.userId)
            val bId=remote.remote.sessions(b.access,200,0).items.single { it.current }.id
            assertTrue(manager.sessions().items.none { it.id == bId })
            val denial=runCatching { manager.revoke(bId,false) }.exceptionOrNull()
            assertTrue(denial is AuthFailure && denial.status==404)
            manager.revoke(currentOther.id,false)
            val rejected=runCatching { remote.remote.refresh(otherDevice.refresh,100) }.exceptionOrNull()
            assertTrue(rejected is AuthFailure && rejected.status==401)
            assertTrue(manager.logout(true).serverRevoked)
            val freshB=remote.remote.refresh(b.refresh,200)
            assertEquals("Тестовый клиент Б",remote.remote.me(freshB.access,200).displayName)
            remote.remote.logout(freshB.access,200,false)
        }
    }
    @Test fun blockedPendingOwnerAndBadCredentialsAreRejected() = runBlocking {
        val remote=api()
        for(email in listOf("pending@example.com","blocked@example.com","owner@example.com","unverified@example.com","no-profile@example.com","absent@example.com")) {
            val failure=runCatching { remote.login(email,Secret(TEST_PASSWORD),1) }.exceptionOrNull()
            assertTrue(failure is AuthFailure && failure.status==401)
        }
        val wrong=runCatching { remote.login(CLIENT_A,Secret("wrong synthetic password"),1) }.exceptionOrNull()
        assertTrue(wrong is AuthFailure && wrong.status==401)
    }
    @Test fun expiredAccessUsesOneRealRotationForSixRequests() = runBlocking {
        scenario { manager,remote,_ ->
            manager.login(CLIENT_A,Secret(TEST_PASSWORD))
            delay(4500) // Actual backend TTL is 4 seconds in this isolated E2E.
            coroutineScope { List(6){async { manager.loadMe() }}.awaitAll() }
            assertEquals(1,remote.refreshes)
            assertEquals("Тестовый клиент А",manager.state.value.profile?.displayName)
        }
    }
    @Test fun lostCommittedRefreshResponseRequiresLoginAndNoReplay() = runBlocking {
        scenario(8444) { manager,remote,store ->
            manager.login(CLIENT_A,Secret(TEST_PASSWORD));delay(4500)
            assertTrue(runCatching { manager.loadMe() }.isFailure)
            assertNull(manager.state.value.profile);assertNull(store.read())
            manager.restore();assertEquals(1,remote.refreshes)
        }
    }
    @Test fun logoutWhileCommittedResponseIsDelayedCannotRestoreAccess() = runBlocking {
        scenario(8445) { manager,remote,store ->
            manager.login(CLIENT_A,Secret(TEST_PASSWORD));delay(4500)
            val refresh=async { runCatching { manager.loadMe() } }
            remote.callsStarted.await();delay(500)
            manager.logout();refresh.await();delay(3200)
            assertNull(manager.state.value.profile);assertNull(store.read())
        }
    }
    @Test fun badCertificateAndWrongHostnameCannotSendCredentials() = runBlocking {
        val wrongHost=runCatching { HttpAuthApi("https://127.0.0.1:8443/").login(CLIENT_A,Secret(TEST_PASSWORD),1) }.exceptionOrNull()
        assertTrue(wrongHost is AuthFailure)
        val wrongCert=runCatching { api(8446).login(CLIENT_A,Secret(TEST_PASSWORD),1) }.exceptionOrNull()
        assertTrue(wrongCert is AuthFailure)
    }
    @Test fun encryptedStoreUsesNewIvAndSurvivesTamperWithoutPlaintextFallback() = runBlocking {
        val ns="test_"+UUID.randomUUID().toString().replace("-","")
        val store=KeystoreRefreshStore(context,ns);val file=File(context.noBackupFilesDir,"$ns-session-v1.bin")
        val secret=Secret("mdr1_"+"Z".repeat(43))
        try {
            store.save(secret);val first=file.readText()
            assertFalse(first.contains(secret.value));assertFalse(first.contains(TEST_PASSWORD))
            store.save(secret);assertNotEquals(first,file.readText())
            assertTrue(store.read()?.refresh?.value == secret.value)
            file.writeText("unreadable ciphertext")
            assertTrue(store.read()?.exchanging == true);assertNull(store.read())
        } finally {store.clear()}
    }
    @Test fun accountBReplacesAllProfileStateFromA() = runBlocking {
        scenario { manager,_,_ ->
            val first=manager.login(CLIENT_A,Secret(TEST_PASSWORD));manager.sessions()
            val second=manager.login(CLIENT_B,Secret(TEST_PASSWORD))
            assertNotEquals(first.userId,second.userId)
            assertEquals("Тестовый клиент Б",manager.state.value.profile?.displayName)
        }
    }
}

/** CI runs each method in a NEW OS process with am force-stop between phases. */
@RunWith(AndroidJUnit4::class)
class ProcessLifecycleTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager get() = (context.applicationContext as InternalApplication).manager!!
    private suspend fun ready() = withTimeout(30_000) { while(manager.state.value.restoring) delay(50) }
    @Test fun prepareRestart() = runBlocking {
        ready();manager.logout();manager.login(CLIENT_A,Secret(TEST_PASSWORD))
        assertEquals("Тестовый клиент А",manager.state.value.profile?.displayName)
    }
    @Test fun verifyRestart() = runBlocking {
        ready();assertEquals("Тестовый клиент А",manager.state.value.profile?.displayName)
        assertEquals("Тестовый клиент А",manager.loadMe().displayName)
    }
    @Test fun prepareInterruptedExchange() = runBlocking {
        ready();manager.logout();manager.login(CLIENT_A,Secret(TEST_PASSWORD))
        val store=KeystoreRefreshStore(context);val previous=store.read()!!.refresh!!
        store.beginExchange()
        // Actual server commit; response deliberately never saved. CI kills this process next.
        HttpAuthApi(BuildConfig.API_BASE_URL).refresh(previous,900)
        assertTrue(store.read()?.exchanging == true)
        // read() discards an interrupted record; write marker again for the new process.
        store.beginExchange()
    }
    @Test fun verifyInterruptedExchange() = runBlocking {
        ready();assertNull(manager.state.value.profile);assertNull(KeystoreRefreshStore(context).read())
        assertTrue(manager.state.value.message?.contains("не завершён") == true)
    }
}
