package dev.mitin.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StoredSession(val refresh: Secret?, val exchanging: Boolean = false)
interface RefreshStore {
    suspend fun read(): StoredSession?
    suspend fun save(refresh: Secret)
    /** Durable marker removes the old refresh before ANY exchange request. */
    suspend fun beginExchange()
    suspend fun clear()
}
data class AuthState(val restoring: Boolean = true, val profile: Me? = null, val message: String? = null)
class AccessLease(val epoch: Long, val generation: Long, val access: Secret)
data class LogoutResult(val serverRevoked: Boolean)

/** One per Application; async exchange is owned by this scope, never by a screen. */
class SessionManager(
    private val api: AuthApi,
    private val store: RefreshStore,
    scope: CoroutineScope,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    // Independent account operations cannot cancel the application's parent job.
    // Awaiters still receive every failure; parent cancellation still cancels us.
    private val operations = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val mutex = Mutex()
    private var epoch = 0L
    private var generation = 0L
    private class Credentials(val access: Secret?, val refresh: Secret, val expiresAt: Long)
    private var credentials: Credentials? = null
    private var exchange: Deferred<AccessLease>? = null
    private val mutable = MutableStateFlow(AuthState())
    val state = mutable.asStateFlow()

    private suspend fun clearLocked(message: String? = null) {
        val previous = epoch++
        credentials = null; generation++
        api.cancel(previous)
        exchange?.cancel(); exchange = null
        // State changes immediately; a late response may never restore a private screen.
        mutable.value = AuthState(restoring = false, message = message)
        store.clear()
    }
    private suspend fun persistPairLocked(pair: TokenPair): AccessLease {
        store.save(pair.refresh) // atomic encrypted save BEFORE publication/use
        generation++
        credentials = Credentials(pair.access, pair.refresh, now() + pair.expiresIn * 1000L)
        return AccessLease(epoch, generation, pair.access)
    }
    suspend fun restore() {
        var start = -1L
        try {
        start = mutex.withLock {
            val saved = store.read()
            if (saved == null || saved.exchanging || saved.refresh == null) {
                clearLocked(if (saved?.exchanging == true) "Предыдущий обмен не завершён. Войдите заново." else null)
                return
            }
            credentials = Credentials(null, saved.refresh, 0)
            epoch
        }
        loadMe()
        } catch (_: Exception) {
            mutex.withLock {
                if (start == -1L || epoch == start) {
                    try { clearLocked("Не удалось восстановить доступ. Войдите заново.") }
                    catch (_: Exception) { mutable.value = AuthState(restoring = false, message = "Защищённое хранилище недоступно. Вход необходим повторно.") }
                }
            }
        }
    }
    suspend fun login(email: String, password: Secret): Me {
        val start = mutex.withLock { clearLocked(); epoch }
        // Screen cancellation does not retry login. The single operation continues
        // in application scope, and an account change invalidates its epoch.
        return operations.async {
            try {
                val pair = api.login(email, password, start)
                mutex.withLock {
                    if (epoch != start) throw Superseded()
                    persistPairLocked(pair)
                }
                loadMe()
            } catch (failure: Exception) {
                mutex.withLock { if (epoch == start) clearLocked() }
                throw failure
            }
        }.await()
    }
    private suspend fun rotate(start: Long, raw: Secret): AccessLease {
        try {
            mutex.withLock {
                if (epoch != start) throw Superseded()
                store.beginExchange()
            }
            val pair = api.refresh(raw, start) // EXACTLY ONE attempt; ambiguous outcome requires login.
            return mutex.withLock {
                if (epoch != start) throw Superseded()
                persistPairLocked(pair)
            }
        } catch (failure: Exception) {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (epoch == start) {
                        // Do not cancel the currently executing deferred from itself.
                        exchange = null
                        clearLocked("Обновление доступа не завершено. Войдите заново.")
                    }
                }
            }
            throw failure
        } finally {
            withContext(NonCancellable) { mutex.withLock { if (epoch == start) exchange = null } }
        }
    }
    private suspend fun access(forceGeneration: Long? = null, expectedEpoch: Long? = null): AccessLease {
        val pending = mutex.withLock {
            if (expectedEpoch != null && expectedEpoch != epoch) throw Superseded()
            val current = credentials ?: throw SignedOut()
            if (current.access != null && now() < current.expiresAt &&
                (forceGeneration == null || generation != forceGeneration)) {
                return AccessLease(epoch, generation, current.access)
            }
            exchange ?: run {
                val start = epoch
                operations.async(start = CoroutineStart.LAZY) { rotate(start, current.refresh) }.also {
                    exchange = it; it.start()
                }
            }
        }
        return pending.await()
    }
    /** Only a GET may be retried, once, after a shared rotation. */
    private suspend fun <T> get(operation: suspend (AccessLease) -> T): Pair<AccessLease, T> {
        var lease = access()
        val result = try { operation(lease) } catch (failure: AuthFailure) {
            if (failure.status != 401) throw failure
            lease = access(lease.generation, lease.epoch)
            try { operation(lease) } catch (again: AuthFailure) {
                if (again.status == 401) mutex.withLock { if (epoch == lease.epoch) clearLocked("Сессия завершена. Войдите заново.") }
                throw again
            }
        }
        mutex.withLock { if (epoch != lease.epoch) throw Superseded() }
        return lease to result
    }
    suspend fun loadMe(): Me {
        val (lease, profile) = get { api.me(it.access, it.epoch) }
        mutex.withLock {
            if (epoch != lease.epoch) throw Superseded()
            mutable.value = AuthState(restoring = false, profile = profile)
        }
        return profile
    }
    suspend fun sessions(offset: Int = 0): SessionPage = get { api.sessions(it.access, it.epoch, offset) }.second
    suspend fun revoke(id: String, current: Boolean) {
        val lease = access()
        api.revoke(lease.access, lease.epoch, id) // mutation has no automatic replay
        mutex.withLock {
            if (epoch != lease.epoch) throw Superseded()
            if (current) clearLocked("Текущая сессия отозвана на сервере.")
        }
    }
    suspend fun logout(all: Boolean = false): LogoutResult {
        if (all) {
            // Obtain a usable token first; no blind retry of logout-all after a network error.
            val lease = access()
            return try {
                api.logout(lease.access, lease.epoch, true)
                mutex.withLock { if (epoch == lease.epoch) clearLocked("Все сессии отозваны на сервере.") }
                LogoutResult(true)
            } catch (_: Exception) {
                mutex.withLock { if (epoch == lease.epoch) clearLocked("Данные на телефоне очищены. Отзыв всех сессий на сервере не подтверждён.") }
                LogoutResult(false)
            }
        }
        val previous = mutex.withLock {
            val captured = credentials?.access
            clearLocked("Данные на телефоне очищены. Проверяем отзыв на сервере…")
            epoch to captured
        }
        val revoked = if (previous.second == null) false else try {
            api.logout(previous.second!!, previous.first, false); true
        } catch (_: Exception) { false }
        mutex.withLock {
            if (epoch == previous.first) mutable.value = AuthState(restoring = false, message =
                if (revoked) "Сессия отозвана на сервере. Данные на телефоне очищены."
                else "Данные на телефоне очищены. Отзыв сессии на сервере не подтверждён.")
        }
        return LogoutResult(revoked)
    }
}

class AuthRepository(private val sessions: SessionManager) {
    val state get() = sessions.state
    suspend fun login(email: String, password: Secret) = sessions.login(email, password)
    suspend fun profile() = sessions.loadMe()
    suspend fun page(offset: Int = 0) = sessions.sessions(offset)
    suspend fun revoke(item: RemoteSession) = sessions.revoke(item.id, item.current)
    suspend fun logout(all: Boolean) = sessions.logout(all)
}
