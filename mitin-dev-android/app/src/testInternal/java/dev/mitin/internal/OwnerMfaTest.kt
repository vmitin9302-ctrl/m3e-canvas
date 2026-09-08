package dev.mitin.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerMfaTest {
    private class Fixture(scope: CoroutineScope) {
        val store = SessionManagerTest.Store()
        var clock = 0L
        var fail: Exception? = null
        var verifyCalls = 0
        var gate: CompletableDeferred<Unit>? = null
        val api = object : AuthApi by SessionManagerTest.Api() {
            override suspend fun login(email: String, password: Secret, epoch: Long): TokenPair =
                throw MfaRequired(Secret("c".repeat(43)), 180)
            override suspend fun verifyMfa(challenge: Secret, code: Secret, epoch: Long): TokenPair {
                verifyCalls++
                withContext(NonCancellable) { gate?.await() }
                fail?.let { throw it }
                return TokenPair(Secret("a".repeat(200)), Secret("mdr1_" + "r".repeat(43)), 600)
            }
            override suspend fun me(access: Secret, epoch: Long) = Me("owner", null, "Owner", "owner")
        }
        val manager = SessionManager(api, store, scope) { clock }
        suspend fun challenge() {
            assertTrue(runCatching { manager.login("owner", Secret("synthetic-password")) }.exceptionOrNull() is MfaRequired)
            assertTrue(manager.state.value.mfaRequired)
            assertNull(manager.state.value.profile)
            assertNull(store.record)
        }
    }
    @Test fun passwordDoesNotPersistTokensAndCodeCompletesOwnerLogin() = runTest {
        val f = Fixture(backgroundScope); f.challenge()
        assertEquals("owner", f.manager.verifyMfa(Secret("123456")).role)
        assertFalse(f.manager.state.value.mfaRequired)
        assertEquals(1, f.store.saves)
    }
    @Test fun wrongCodeAllowsExplicitRetryWithoutRepeatingPassword() = runTest {
        val f = Fixture(backgroundScope); f.challenge(); f.fail = AuthFailure(401)
        assertTrue(runCatching { f.manager.verifyMfa(Secret("123456")) }.isFailure)
        assertTrue(f.manager.state.value.mfaRequired); assertNull(f.store.record)
        f.fail = null; f.manager.verifyMfa(Secret("654321"))
        assertEquals(2, f.verifyCalls)
    }
    @Test fun ambiguousNetworkFailureRequiresFreshPasswordAndDoesNotReplay() = runTest {
        val f = Fixture(backgroundScope); f.challenge(); f.fail = AuthFailure()
        assertTrue(runCatching { f.manager.verifyMfa(Secret("123456")) }.isFailure)
        assertFalse(f.manager.state.value.mfaRequired)
        assertTrue(runCatching { f.manager.verifyMfa(Secret("123456")) }.exceptionOrNull() is SignedOut)
        assertEquals(1, f.verifyCalls); assertNull(f.store.record)
    }
    @Test fun lateVerificationCannotRestoreSessionAfterBack() = runTest {
        val f = Fixture(backgroundScope); f.challenge(); f.gate = CompletableDeferred()
        val request = async { runCatching { f.manager.verifyMfa(Secret("123456")) } }
        runCurrent(); f.manager.cancelMfa(); f.gate!!.complete(Unit)
        assertTrue(request.await().exceptionOrNull() is Superseded)
        assertNull(f.manager.state.value.profile); assertNull(f.store.record)
    }
    @Test fun expiredChallengeNeverSendsCode() = runTest {
        val f = Fixture(backgroundScope); f.challenge(); f.clock = 180_000
        assertTrue(runCatching { f.manager.verifyMfa(Secret("123456")) }.exceptionOrNull() is SignedOut)
        assertEquals(0, f.verifyCalls)
    }
    @Test fun newManagerCannotRestorePendingChallenge() = runTest {
        val f = Fixture(backgroundScope); f.challenge()
        val restarted = SessionManager(f.api, f.store, backgroundScope)
        restarted.restore()
        assertFalse(restarted.state.value.mfaRequired); assertNull(restarted.state.value.profile)
    }
    @Test fun simultaneousSubmissionsSendOneRequest() = runTest {
        val f = Fixture(backgroundScope); f.challenge(); f.gate = CompletableDeferred()
        val first = async { f.manager.verifyMfa(Secret("123456")) }; runCurrent()
        assertTrue(runCatching { f.manager.verifyMfa(Secret("123456")) }.exceptionOrNull() is Superseded)
        f.gate!!.complete(Unit); first.await(); assertEquals(1, f.verifyCalls)
    }
}
