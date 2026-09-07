package dev.mitin.internal

import android.app.Application
import android.util.Base64
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mitin.demo.BuildConfig
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.security.SecureRandom
import java.util.UUID

class BriefViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() }?.let { HttpBriefRepository(it) }
    private val store = BriefStore(app)
    private var record: BriefRecord? = null
    var state by mutableStateOf<BriefState?>(null); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var portfolio by mutableStateOf<String?>(null); private set
    var portfolioTitle by mutableStateOf<String?>(null); private set
    var contactStep by mutableStateOf(false); private set
    var canRetry by mutableStateOf(false); private set
    init { run { record = store.read(); record?.let { recover(it) } } }
    private fun run(action: suspend () -> Unit) {
        if(busy) return
        busy = true; error = null; canRetry = false
        viewModelScope.launch {
            try { action() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                error = when ((e as? BriefFailure)?.status) {
                    0 -> "Нет соединения. Восстановите сеть и нажмите «Повторить»."
                    404 -> "AI-бриф пока недоступен в этом окружении."
                    410 -> "Срок сессии или подтверждения истёк. Обновите данные или начните новый бриф."
                    409 -> "Состояние изменилось или AI ещё отвечает. Обновите сессию."
                    422 -> when((e as? BriefFailure)?.code) {
                        "history_limit" -> "Достигнут лимит обсуждения. Начните новый бриф."
                        "invalid_contact" -> "Проверьте контактные данные."
                        "more_context_required" -> "Нужно больше деталей. Продолжите обсуждение."
                        else -> "Проверьте данные и согласие на обработку данных."
                    }
                    429 -> "Слишком много запросов. Подождите перед повтором."
                    504 -> "AI не успел ответить. Можно безопасно повторить запрос."
                    else -> "Не удалось подтвердить результат. Обновите сессию перед повторной отправкой."
                }
                canRetry = record != null
            } finally { busy = false }
        }
    }
    fun fromPortfolio(slug: String?, title: String?) {
        // Never silently discard an existing discussion or ambiguous submission.
        if(record == null) { portfolio = slug; portfolioTitle = title }
        else if(slug != state?.portfolio) error = "У вас уже есть бриф. Завершите его или начните новый, затем выберите кейс."
    }
    fun start(service: String, budget: String) = run {
        if(repository == null) throw BriefFailure(404)
        val id = UUID.randomUUID().toString()
        val secret = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val body = buildJsonObject {
            put("session_id", id); put("session_secret", secret); put("service", service); put("budget_range", budget)
            portfolio?.let { put("source_portfolio_slug", it) }
        }
        val next = BriefRecord(id, secret, body, "sessions", body)
        store.save(next); record = next
        accept(repository.execute(next))
    }
    private suspend fun accept(value: BriefState) {
        val updated = record!!.copy(pendingPath = null, pendingBody = null)
        store.save(updated); record = updated; state = value
        portfolio = value.portfolio
        contactStep = value.prepared != null && !value.submitted
    }
    private suspend fun recover(current: BriefRecord) {
        val api = repository ?: throw BriefFailure(404)
        val value = try { api.get(current) } catch(e: BriefFailure) {
            if(e.status == 404 && current.pendingPath == "sessions") { accept(api.execute(current)); return } else throw e
        }
        state = value
        contactStep = value.prepared != null && !value.submitted
        if(value.submitted || (current.pendingBody?.get("revision")?.jsonPrimitive?.intOrNull?.let { value.revision > it } == true) || current.pendingPath == "sessions") accept(value)
        else if(current.pendingPath != null) {
            if(value.pending) throw BriefFailure(409, "ai_pending")
            accept(api.execute(current))
        }
    }
    private fun command(path: String, extra: JsonObject = JsonObject(emptyMap())) = run {
        val existing = record ?: return@run
        if(existing.pendingPath != null) { recover(existing); return@run }
        val value = state ?: return@run
        val body = buildJsonObject {
            put("request_id", UUID.randomUUID().toString()); put("revision", value.revision)
            extra.forEach { (k,v) -> put(k,v) }
        }
        val next = existing.copy(pendingPath = "sessions/${existing.id}/$path", pendingBody = body)
        store.save(next); record = next
        try { accept(repository!!.execute(next)) }
        catch(e: BriefFailure) {
            if(e.status in setOf(410, 422, 429)) {
                // These responses explicitly reject the operation before a commit.
                val cleared = next.copy(pendingPath=null, pendingBody=null); store.save(cleared); record=cleared
            }
            throw e
        }
    }
    fun message(text: String) = command("messages", buildJsonObject { put("message", text) })
    fun finalBrief() = command("final")
    fun prepare(name: String, type: String, contact: String) = command("prepare", buildJsonObject {
        put("name",name); put("contact_type",type); put("contact",contact)
    })
    fun confirm(consent: Boolean) { val value=state ?: return; command("confirm", buildJsonObject {
        put("proof", value.proof ?: ""); put("consent",consent); put("legal_digest",value.legalDigest)
    }) }
    fun showContact() { contactStep=true }
    fun discuss() { contactStep=false }
    fun retry() = run { record?.let { recover(it) } }
    fun reset() = run {
        if(record?.pendingPath != null) { record?.let { recover(it) }; return@run }
        store.clear(); record=null; state=null; portfolio=null; portfolioTitle=null; contactStep=false
    }
}
