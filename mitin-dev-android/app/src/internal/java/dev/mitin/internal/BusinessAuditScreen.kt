package dev.mitin.internal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mitin.demo.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume

class BusinessAuditViewModel(private val saved: SavedStateHandle) : ViewModel() {
    var step by mutableIntStateOf(saved["audit-step"] ?: 0); private set
    var business by mutableStateOf(saved.get<String>("audit-business") ?: ""); private set
    var website by mutableStateOf(saved.get<String>("audit-website") ?: ""); private set
    var answers by mutableStateOf(saved.get<ArrayList<Int>>("audit-answers")?.toList() ?: List(7) { -1 }); private set
    var commentary by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(false); private set
    private var aiJob: Job? = null
    private var aiVersion=0L
    val result get() = calculateBusinessAudit(answers)
    private fun invalidateCommentary() { aiVersion++;aiJob?.cancel();loading=false;commentary=null }
    fun business(value:String) { invalidateCommentary();business=value.take(120); saved["audit-business"]=business }
    fun website(value:String) { invalidateCommentary();website=value.take(500); saved["audit-website"]=website }
    fun select(value:Int) {
        answers=answers.toMutableList().also { it[step]=value }; saved["audit-answers"]=ArrayList(answers)
        invalidateCommentary()
    }
    fun move(value:Int) { if(value<7)invalidateCommentary();step=value.coerceIn(0,7); saved["audit-step"]=step }
    fun restart() {
        invalidateCommentary()
        answers=List(7){-1}; saved["audit-answers"]=ArrayList(answers); business("");website("");move(0)
    }
    fun requestCommentary() {
        if(loading || step!=7) return
        val version=++aiVersion
        aiJob=viewModelScope.launch {
            loading=true
            try {
                val text=fetchAuditCommentary(business,website,answers,result.score)
                if(version==aiVersion)commentary=text ?: "AI-комментарий сейчас недоступен. Расчёт и рекомендации выше доступны без сети. Проверьте подключение и повторите запрос."
            } finally { if(version==aiVersion)loading=false }
        }
    }
}

internal suspend fun fetchAuditCommentary(business:String,website:String,answers:List<Int>,score:Int):String? {
    val answerText=auditDimensions.mapIndexed { i,d -> "${d.title}: ${answers[i]}/100 — ${d.answers.first { it.first==answers[i] }.second}" }.joinToString("\n")
    val prompt="""РЕЖИМ ЭКСПРЕСС-АУДИТА БИЗНЕСА MITIN DEV.
Это НЕ бриф проекта. Не задавай вопросов, не формируй техническое задание, не проси контакт и не используй маркер готовности ТЗ.
Дай практический комментарий на русском до 1200 символов: краткая оценка, 3 приоритетных действия и вывод. Не придумывай факты.
Сфера: $business. Digital Score: $score/100.
$answerText
Указанная ссылка: $website. Ссылка не открывалась и не сканировалась; не утверждай, что видел сайт.""".take(3900)
    val body=buildJsonObject { put("session_id",UUID.randomUUID().toString());put("message",prompt);put("conversation",JsonArray(emptyList()));put("start",false) }
    val origin=HttpAuthApi.checkedOrigin(BuildConfig.API_BASE_URL)
    val request=Request.Builder().url(origin.newBuilder().encodedPath("/api/public/website-chat").build())
        .header("Cache-Control","no-store").post(body.toString().toRequestBody("application/json".toMediaType())).build()
    val call=HttpAuthApi.secureClient().newCall(request)
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException) { if(continuation.isActive)continuation.resume(null) }
            override fun onResponse(call:Call,response:Response) {
                val result=response.use { r -> runCatching {
                    if(!r.isSuccessful || r.headers("Set-Cookie").isNotEmpty() || r.body.source().request(16385)) return@runCatching null
                    val data=Json.parseToJsonElement(r.body.string()).jsonObject
                    val message=data.text("message").trim()
                    if(data["is_final"]?.jsonPrimitive?.booleanOrNull==true || data["needs_contact"]?.jsonPrimitive?.booleanOrNull==true ||
                        message.length !in 120..2400 || message.contains("техническое задание",true) || message.contains("готовность тз",true)) null else message
                }.getOrNull() }
                if(continuation.isActive)continuation.resume(result)
            }
        })
    }
}

@Composable internal fun BusinessAuditScreen(close:()->Unit, vm:BusinessAuditViewModel=viewModel()) {
    val keyboard=LocalSoftwareKeyboardController.current
    val ime=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val scroll=rememberScrollState()
    fun back() { if(vm.step>0)vm.move(vm.step-1) else close() }
    BackHandler(!ime) { back() }
    LaunchedEffect(vm.step) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp).testTag("business-audit"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SecondaryAction("Назад","audit-back") { back() }
        Heading("Аудит бизнеса")
        if(vm.step<7) {
            Text("${vm.step+1} из 7 · ${auditDimensions[vm.step].title}")
            LinearProgressIndicator(progress={vm.step/7f},modifier=Modifier.fillMaxWidth())
            if(vm.step==0) {
                Text("Оценим сайт, заявки, CRM, скорость ответа, аналитику, автоматизацию и AI. Расчёт работает без сети.")
                Input("Сфера бизнеса",vm.business,"audit-business",maxLines=1,changed=vm::business)
                Input("Ссылка на сайт (необязательно)",vm.website,"audit-website",maxLines=1,changed=vm::website)
                Text("Сайт по ссылке не сканируется.",style=MaterialTheme.typography.bodySmall)
            }
            for((value,label) in auditDimensions[vm.step].answers) Row(Modifier.fillMaxWidth().selectable(vm.answers[vm.step]==value,role=Role.RadioButton,onClick={vm.select(value)}).padding(vertical=8.dp).testTag("audit-option-$value")) {
                RadioButton(selected=vm.answers[vm.step]==value,onClick=null)
                Text(label,Modifier.weight(1f).padding(start=8.dp))
            }
            PrimaryAction(if(vm.step==6)"Показать результат" else "Далее","audit-next",vm.answers[vm.step]>=0 && vm.business.isNotBlank()) { keyboard?.hide();vm.move(vm.step+1) }
        } else {
            Text("Digital Score: ${vm.result.score}/100",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.testTag("audit-score"))
            Text(auditVerdict(vm.result.score))
            Text("Три приоритета",style=MaterialTheme.typography.titleLarge)
            vm.result.priorities.forEachIndexed { i,d -> InfoCard("${i+1}. ${d.recommendation}",d.explanation) }
            Text("AI-комментарий",style=MaterialTheme.typography.titleLarge)
            Text("По нажатию ответы и сфера бизнеса передаются сервису MITIN DEV для подготовки комментария. Не указывайте секреты и данные клиентов.",style=MaterialTheme.typography.bodySmall)
            vm.commentary?.let { Text(it,Modifier.testTag("audit-commentary")) }
            PrimaryAction(if(vm.loading)"Готовим комментарий…" else if(vm.commentary==null)"Получить AI-комментарий" else "Повторить AI-комментарий","audit-ai",!vm.loading) { vm.requestCommentary() }
            CompanyContacts()
            SecondaryAction("Пройти заново","audit-restart") { vm.restart() }
        }
        Spacer(Modifier.height(24.dp))
    }
}
