package dev.mitin.internal

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mitin.demo.BuildConfig
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.util.UUID

enum class CabinetStatus { Loading, Success, Empty, Error, Unauthorized, Forbidden, Offline }
data class CabinetRoute(val section: String = "dashboard", val id: String? = null, val project: String? = null)
fun JsonObject.text(key: String): String = (get(key) as? JsonPrimitive)?.contentOrNull ?: ""
fun JsonObject.number(key: String): Long? = (get(key) as? JsonPrimitive)?.longOrNull

class CabinetViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = (app as InternalApplication).manager!!
    private val repository = CabinetRepository(manager, CabinetApi(BuildConfig.API_BASE_URL))
    var route by mutableStateOf(CabinetRoute()); private set
    var document by mutableStateOf(JsonObject(emptyMap())); private set
    var items by mutableStateOf<List<JsonObject>>(emptyList()); private set
    var status by mutableStateOf(CabinetStatus.Empty); private set
    var notice by mutableStateOf<String?>(null); private set
    var authMode by mutableStateOf("login")
    var nextOffset by mutableStateOf<Int?>(null); private set
    var busy by mutableStateOf(false); private set
    var owner by mutableStateOf(false); private set
    private var userId: String? = null
    private var job: Job? = null
    private var workVersion=0L
    private val history = mutableListOf<CabinetRoute>()
    private var pendingMessage: Pair<String,String>? = null
    var editRecord by mutableStateOf<JsonObject?>(null); private set
    fun edit(value:JsonObject?) { editRecord=value }
    private val formState=mutableStateOf<String?>(null)
    val formValues=mutableStateMapOf<String,String>()
    val formChoices=mutableStateMapOf<String,String>()
    var formClientAction by mutableStateOf(false)
    var formArchived by mutableStateOf(false)
    var messageDraft by mutableStateOf("")
    var form:String?
        get()=formState.value
        set(value) {
            if(value!=formState.value) {
                formValues.clear();formChoices.clear();formClientAction=false
                formArchived=document["archived_at"] is JsonPrimitive
            }
            formState.value=value
        }

    init {
        viewModelScope.launch {
            manager.state.collect { auth ->
                if (userId != auth.profile?.userId) {
                    workVersion++;job?.cancel(); userId = auth.profile?.userId; owner = auth.profile?.role == "owner"
                    document = JsonObject(emptyMap()); items = emptyList(); history.clear(); route = CabinetRoute()
                    nextOffset = null; notice = null; pendingMessage = null; editRecord=null;form=null;messageDraft=""; busy = false
                    if (auth.profile != null) refresh() else status = CabinetStatus.Unauthorized
                }
            }
        }
    }
    private fun prefix() = if (owner) "owner/" else ""
    private fun path(target: CabinetRoute): String = when (target.section) {
        "profile" -> "profile"
        "lead" -> prefix()+"leads/${target.id}"
        "project" -> prefix()+"projects/${target.id}"
        "client" -> "owner/clients/${target.id}"
        "assign-lead", "assign-project" -> "owner/clients"
        "stages", "demos", "messages", "materials", "files", "payments", "expenses", "support", "events" -> prefix()+"projects/${target.project}/${target.section}"
        "financial" -> prefix()+"projects/${target.project}/financial-summary"
        "inbox" -> prefix()+"messages"
        "all-payments" -> prefix()+"payments"
        "all-support" -> prefix()+"support"
        "all-materials" -> "materials"
        else -> prefix()+target.section
    }
    private fun run(action: suspend () -> Unit) {
        if (busy) return
        val version=++workVersion
        job = viewModelScope.launch {
            busy = true; notice = null; status = CabinetStatus.Loading
            try { action(); if(version==workVersion) status = if (items.isEmpty() && document.isEmpty()) CabinetStatus.Empty else CabinetStatus.Success }
            catch (e: CancellationException) { throw e }
            catch (_: Superseded) { }
            catch (_: SignedOut) { if(version==workVersion) {status = CabinetStatus.Unauthorized; notice = "Войдите заново."} }
            catch (e: AuthFailure) {
                if(version==workVersion) {
                status = when(e.status) {401->CabinetStatus.Unauthorized;403->CabinetStatus.Forbidden;503->CabinetStatus.Offline;else->CabinetStatus.Error}
                notice = when(e.status) {401->"Сессия завершена. Войдите заново.";403->"Действие недоступно.";404->"Данные не найдены.";409->"Данные изменились или действие уже выполнено. Обновите экран.";413->"Файл должен быть непустым и не больше 10 МБ.";415->"Поддерживаются TXT, PDF, PNG и JPEG. Проверьте формат файла.";422->"Проверьте заполненные поля.";429->"Слишком много запросов. Попробуйте позже.";503->"Нет связи с сервисом. Проверьте подключение.";else->"Сервис не смог завершить действие. Попробуйте позже."}
                }
            }
            catch (_: Exception) { if(version==workVersion) {status = CabinetStatus.Error; notice = "Не удалось завершить действие."} }
            finally { if(version==workVersion) busy = false }
        }
    }
    private suspend fun load(more: Boolean = false) {
        val list = route.section in setOf("leads","projects","clients","assign-lead","assign-project","stages","demos","messages","materials","files","payments","expenses","support","events","inbox","all-payments","all-support","all-materials")
        val data = repository.read(path(route), if(list) (if(more) nextOffset ?: return else 0) else null)
        if (list) {
            val fetched = data["items"]!!.jsonArray.map { it.jsonObject }
            items = if(more) (items+fetched).distinctBy { it.text("id") } else fetched
            document = JsonObject(emptyMap()); nextOffset = data.number("next_offset")?.toInt()
            if (route.section == "messages" && items.isNotEmpty()) {
                repository.mutate(prefix()+"projects/${route.project}/read-state", "PUT", buildJsonObject { put("last_read_message_id", items.last().text("id")) })
            }
        } else { document = data; items = emptyList(); nextOffset = null }
    }
    fun open(target: CabinetRoute) {
        if (busy) return
        form=null
        if(route.project!=target.project)messageDraft=""
        history.add(route); route = target; items = emptyList(); document = JsonObject(emptyMap()); nextOffset = null
        refresh()
    }
    fun back(): Boolean {
        if (history.isEmpty()) return false
        form=null
        workVersion++;job?.cancel(); busy = false; route = history.removeAt(history.lastIndex); items = emptyList(); document = JsonObject(emptyMap()); refresh(); return true
    }
    fun refresh(more: Boolean = false) = run { load(more) }
    fun authAction(action: String, data: JsonObject, done: () -> Unit) = run {
        try { repository.publicAction("auth/$action", data) }
        catch (failure: AuthFailure) {
            if (failure.status != 401) throw failure
            notice = "Ссылка недействительна, уже использована или срок её действия истёк. Запросите новое письмо."
            return@run
        }
        notice = when(action) {
            "verify-email" -> "Email подтверждён. Можно войти."
            "password-reset/confirm" -> "Пароль изменён. Войдите с новым паролем."
            else -> "Проверьте почту. Если адрес подходит для этого действия, вы получите письмо."
        }
        done()
    }
    fun saveProfile(data: JsonObject) = run { repository.mutate("profile", "PATCH", data); load();form=null }
    fun createLead(data: JsonObject) = run {
        val result = repository.mutate("leads", "POST", data)
        form=null;history.add(route); route = CabinetRoute("lead", result.text("id")); load()
    }
    fun patchLead(data: JsonObject) = run { repository.mutate("owner/leads/${route.id}", "PATCH", data); load();form=null }
    fun assign(profileId:String) = run {
        val target=history.last()
        val data=buildJsonObject {
            put("client_profile_id",profileId)
            if(route.section=="assign-project") put("version",repository.read("owner/projects/${target.id}")["version"]!!)
        }
        repository.mutate("owner/"+(if(route.section=="assign-project") "projects/" else "leads/")+target.id,"PATCH",data)
        route=history.removeAt(history.lastIndex);load()
    }
    fun convert() = run {
        val result = repository.mutate("owner/leads/${route.id}/project", "POST", buildJsonObject { put("title", document.text("business")) })
        history.add(route); route = CabinetRoute("project", result.text("id")); load()
    }
    fun saveProject(data: JsonObject) = run {
        repository.mutate("owner/projects/${route.id}", "PATCH", JsonObject(data + ("version" to (document["version"] ?: JsonPrimitive(1)))))
        load();form=null
    }
    fun createChild(section: String, data: JsonObject) = run {
        repository.mutate(prefix()+"projects/${route.project}/$section", "POST", data); load();form=null
    }
    fun changeChild(section: String, id: String, data: JsonObject) = run {
        repository.mutate("owner/projects/${route.project}/$section/$id", "PATCH", data); load();form=null
    }
    fun decide(id: String, decision: String, comment: String) = run {
        repository.mutate("projects/${route.project}/demos/$id/decision", "POST", buildJsonObject { put("decision", decision); put("comment", comment) }); load()
    }
    fun sendMessage(body: String) = run {
        val request = pendingMessage?.takeIf { it.first == body } ?: (body to UUID.randomUUID().toString()).also { pendingMessage = it }
        repository.mutate(prefix()+"projects/${route.project}/messages", "POST", buildJsonObject { put("body", body); put("client_request_id", request.second) })
        pendingMessage = null;if(messageDraft==body)messageDraft=""; load()
    }
    fun reorderStages(ids: List<String>) = run {
        repository.mutate("owner/projects/${route.project}/stage-order", "PUT", buildJsonObject { put("ids", JsonArray(ids.map(::JsonPrimitive))) }); load()
    }
    fun upload(name: String, type: String, bytes: ByteArray, material: String?) = run {
        repository.upload(route.project!!, name, type, bytes, material); load()
    }
    fun download(id: String, save: suspend (ByteArray) -> Unit) = run {
        val data = repository.download(route.project!!, id); save(data); notice = "Файл сохранён в выбранное место."; load()
    }
    fun uploadUri(uri: android.net.Uri, material: String?) = run {
        val resolver = getApplication<Application>().contentResolver
        val file = withContext(Dispatchers.IO) {
            val name = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if(cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "material.txt"
            val type = resolver.getType(uri) ?: "application/octet-stream"
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val buffer=java.io.ByteArrayOutputStream(); val chunk=ByteArray(8192)
                while(true) { val count=input.read(chunk); if(count<0) break; if(buffer.size()+count>10*1024*1024)throw AuthFailure(413); buffer.write(chunk,0,count) }
                buffer.toByteArray()
            } ?: error("unavailable")
            Triple(name,type,bytes)
        }
        repository.upload(route.project!!,file.first,file.second,file.third,material);load()
    }
    fun saveFile(id:String, uri:android.net.Uri) = download(id) { bytes ->
        withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("unavailable") }
    }
}
