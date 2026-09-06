package dev.mitin.demo

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DemoViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("mitin_offline_demo_v1", Context.MODE_PRIVATE)
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    private val initial = runCatching {
        preferences.getString("snapshot", null)?.let { Json.decodeFromString<DemoSnapshot>(it) }
            ?: DemoSnapshot.initial()
    }.getOrElse {
        error = "Не удалось прочитать локальное демо. Показан исходный пример; данные можно сбросить в профиле."
        DemoSnapshot.initial()
    }
    val repository = DemoRepository(initial) { snapshot ->
        withContext(Dispatchers.IO) {
            check(preferences.edit().putString("snapshot", Json.encodeToString(snapshot)).commit())
        }
    }

    fun act(action: suspend (DemoRepository) -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try { action(repository) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Изменение не сохранено. Проверьте поля и свободное место на устройстве." }
            finally { busy = false }
        }
    }
    fun dismissError() { error = null }
}
