package dev.mitin.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CabinetSessionTest {
    @Test fun unauthorizedMutationIsNeverReplayedAndClearsPrivateSession() = runTest {
        val api=SessionManagerTest.Api();val store=SessionManagerTest.Store();val manager=SessionManager(api,store,backgroundScope){0}
        manager.login("A",Secret("synthetic"));var calls=0
        val error=runCatching{manager.authorizedMutation {calls++;throw AuthFailure(401)}}.exceptionOrNull()
        assertTrue(error is AuthFailure);assertEquals(1,calls);assertEquals(0,api.rotations)
        assertNull(manager.state.value.profile);assertNull(store.record)
    }
    @Test fun lateReadAfterLogoutCannotPublishPrivateData() = runTest {
        val manager=SessionManager(SessionManagerTest.Api(),SessionManagerTest.Store(),backgroundScope){0}
        manager.login("A",Secret("synthetic"));val gate=CompletableDeferred<Unit>()
        val result=async{runCatching{manager.authorizedRead{gate.await();"private A"}}};runCurrent()
        manager.logout();gate.complete(Unit)
        assertTrue(result.await().exceptionOrNull() is Superseded);assertNull(manager.state.value.profile)
    }
    @Test fun lateMutationCannotOverwriteTheNextAccount() = runTest {
        val manager=SessionManager(SessionManagerTest.Api(),SessionManagerTest.Store(),backgroundScope){0}
        manager.login("A",Secret("synthetic"));val gate=CompletableDeferred<Unit>()
        val result=async{runCatching{manager.authorizedMutation{gate.await();"private A"}}};runCurrent()
        manager.logout();manager.login("B",Secret("synthetic"));gate.complete(Unit)
        assertTrue(result.await().exceptionOrNull() is Superseded);assertEquals("B",manager.state.value.profile?.userId)
    }
    @Test fun unauthorizedReadRotatesOnlyOnce() = runTest {
        val api=SessionManagerTest.Api();val manager=SessionManager(api,SessionManagerTest.Store(),backgroundScope){0}
        manager.login("A",Secret("synthetic"));var calls=0
        val value=manager.authorizedRead {calls++;if(calls==1)throw AuthFailure(401);"authorized"}
        assertEquals("authorized",value);assertEquals(2,calls);assertEquals(1,api.rotations)
    }
}
