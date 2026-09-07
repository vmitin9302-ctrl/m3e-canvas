package dev.mitin.internal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import dev.mitin.demo.BuildConfig
import kotlinx.coroutines.*

sealed interface PortfolioState<out T> {
    data object Loading : PortfolioState<Nothing>
    data class Success<T>(val value: T) : PortfolioState<T>
    data object Empty : PortfolioState<Nothing>
    data class Error(val message: String) : PortfolioState<Nothing>
}

class PortfolioViewModel(
    private val repository: PortfolioRepository? = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() }?.let { HttpPortfolioRepository(it) }
) : ViewModel() {
    var list by mutableStateOf<PortfolioState<List<PortfolioItem>>>(PortfolioState.Loading); private set
    var detail by mutableStateOf<PortfolioState<PortfolioItem>>(PortfolioState.Loading); private set
    var selectedSlug by mutableStateOf<String?>(null); private set
    private var listJob: Job? = null
    private var detailJob: Job? = null
    private var loadedAt = 0L
    fun enter() {
        if (list is PortfolioState.Loading || System.nanoTime() - loadedAt > 60_000_000_000L) refresh()
        selectedSlug?.let { if (detail is PortfolioState.Loading) open(it) }
    }
    fun refresh() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            list = PortfolioState.Loading
            if (repository == null) { list = PortfolioState.Error("Тестовый сервер каталога не настроен."); return@launch }
            try {
                val items = repository.list()
                list = if (items.isEmpty()) PortfolioState.Empty else PortfolioState.Success(items)
                loadedAt = System.nanoTime()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { list = PortfolioState.Error("Не удалось загрузить проекты. Проверьте подключение и повторите.") }
        }
    }
    fun open(slug: String) {
        detailJob?.cancel(); selectedSlug = slug; detail = PortfolioState.Loading
        detailJob = viewModelScope.launch {
            try { detail = PortfolioState.Success(repository?.detail(slug) ?: throw PortfolioFailure()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { detail = PortfolioState.Error(if (e is PortfolioFailure && e.status == 404) "Проект больше недоступен." else "Не удалось загрузить проект. Попробуйте ещё раз.") }
        }
    }
    fun back() { detailJob?.cancel(); selectedSlug = null }
    fun leave() { listJob?.cancel(); detailJob?.cancel() }
}
