package dev.mitin.internal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.mitin.demo.*
import kotlinx.serialization.json.*

private data class Field(val key:String,val label:String,val required:Boolean=false,val money:Boolean=false)
@Composable internal fun CabinetForm(kind:String, vm:CabinetViewModel, close:()->Unit) {
    if(kind == "sessions") { SecondaryAction("Закрыть сессии","close-sessions",close);return }
    val fields = when(kind) {
        "profile"->listOf(Field("name","Имя",true),Field("phone","Телефон"),Field("telegram","Telegram"),Field("city","Город"),Field("company_name","Компания"))
        "lead"->listOf(Field("title","Название задачи",true),Field("service","Услуга",true),Field("description","Описание задачи"))
        "owner-lead"->listOf(Field("note","Внутренняя заметка владельца"))
        "project"->listOf(Field("title","Название проекта",true),Field("description","Описание"),Field("next_action","Следующее действие"),Field("agreed_price_amount","Согласованная цена, ₽",money=true),Field("planned_start_at","Начало: ГГГГ-ММ-ДД"),Field("planned_finish_at","Завершение: ГГГГ-ММ-ДД"),Field("support_until","Поддержка до: ГГГГ-ММ-ДД"))
        "stages"->listOf(Field("title","Название этапа",true),Field("description","Описание"),Field("client_action_text","Что требуется от клиента"),Field("due_at","Срок: ГГГГ-ММ-ДД"))
        "demos"->listOf(Field("title","Название версии",true),Field("description","Что изменилось"),Field("demo_url","Ссылка на демо (HTTPS)"))
        "materials"->listOf(Field("title","Что нужно подготовить",true),Field("description","Требования к материалу"),Field("due_at","Срок: ГГГГ-ММ-ДД"))
        "payments"->listOf(Field("amount_minor","Сумма, ₽",true,true),Field("note","Комментарий"))
        "expenses"->listOf(Field("title","Название расхода",true),Field("amount_minor","Сумма, ₽",money=true),Field("provider","Поставщик"))
        "support"->listOf(Field("subject","Тема обращения",true),Field("description","Что произошло или что нужно изменить",true))
        else->emptyList()
    }
    val values = remember(kind,vm.route) { mutableStateMapOf<String,String>().also { values -> for(f in fields) {
        val raw=vm.editRecord?.text(f.key) ?: if(kind in setOf("profile","project")) vm.document.text(f.key) else ""
        values[f.key]=if(f.money && raw.isNotBlank()) raw.toLongOrNull()?.let{java.math.BigDecimal.valueOf(it,2).toPlainString()} ?: "" else if(f.key.endsWith("_at") || f.key == "support_until") raw.take(10) else raw
    } } }
    val choices=when(kind) {
        "profile"->mapOf("legal_status" to listOf("unknown" to "Не указан","individual" to "Физическое лицо","self_employed" to "Самозанятый","individual_entrepreneur" to "ИП","individual_entrepreneur_npd" to "ИП на НПД","llc" to "ООО","other" to "Другое"))
        "lead"->mapOf("budget_range" to listOf("unknown" to "Бюджет не определён","under_10k" to "До 10 000 ₽","10_20k" to "10–20 тыс. ₽","20_40k" to "20–40 тыс. ₽","40_70k" to "40–70 тыс. ₽","70k_plus" to "От 70 тыс. ₽"))
        "owner-lead"->mapOf("status" to listOf("new" to "Получена","reviewing" to "Рассматриваем","need_info" to "Нужна информация","qualified" to "Квалифицирована","accepted" to "Принята","rejected" to "Не реализуем","archived" to "Архив"))
        "project"->mapOf("status" to listOf("draft" to "Подготовка","active" to "В работе","waiting_client" to "Ожидаем клиента","paused" to "Пауза","completed" to "Завершён","cancelled" to "Отменён","support" to "Поддержка"))
        "stages"->mapOf("status" to listOf("not_started" to "Не начат","in_progress" to "В работе","waiting_client" to "Ожидаем клиента","completed" to "Завершён"))
        "payments"->mapOf("type" to listOf("prepayment" to "Предоплата","payment" to "Оплата","final_payment" to "Финальный платёж","refund" to "Возврат","correction" to "Дополнительное поступление"),"status" to listOf("planned" to "Запланирован","confirmed" to "Подтверждён","cancelled" to "Отменён"))
        "expenses"->mapOf("category" to listOf("one_time_external" to "Разовый сторонний расход","monthly" to "Ежемесячный расход","infrastructure" to "Инфраструктура","api_service" to "API и сервисы"),"recurrence" to listOf("once" to "Один раз","monthly" to "Ежемесячно","yearly" to "Ежегодно"),"status" to listOf("planned" to "Запланирован","confirmed" to "Подтверждён","cancelled" to "Отменён"))
        "support"->mapOf("type" to listOf("question" to "Вопрос","bug" to "Ошибка","change" to "Изменение","feature" to "Новая функция"))
        else->emptyMap()
    }
    val selected=remember(kind,vm.route) { mutableStateMapOf<String,String>().also { for((key,options) in choices) it[key]=(vm.editRecord ?: vm.document).text(key).takeIf { value->options.any{p->p.first==value} } ?: options.first().first } }
    var submitted by remember {mutableStateOf(false)}
    var observedBusy by remember {mutableStateOf(false)}
    LaunchedEffect(vm.busy,vm.status,submitted) {
        if(submitted && vm.busy) observedBusy=true
        if(submitted && observedBusy && !vm.busy) {
            if(vm.status in setOf(CabinetStatus.Success,CabinetStatus.Empty)) close()
            submitted=false;observedBusy=false
        }
    }
    var error by remember {mutableStateOf<String?>(null)}
    var clientAction by remember {mutableStateOf(false)}
    var archived by remember {mutableStateOf(vm.document["archived_at"] is JsonPrimitive)}
    for(f in fields) Input(f.label+(if(f.required) " *" else ""),values[f.key] ?: "","field-${f.key}"){values[f.key]=it}
    for((key,options) in choices) {
        Text(when(key){"status"->"Статус";"type"->"Тип";"legal_status"->"Правовой статус — выберите самостоятельно";"category"->"Категория";"recurrence"->"Периодичность";else->"Бюджет"})
        for((value,label) in options) Row(Modifier.fillMaxWidth()) {RadioButton(selected[key]==value,{selected[key]=value});Text(label)}
    }
    if(kind=="stages") Row {Checkbox(clientAction,{clientAction=it});Text("Нужно действие клиента")}
    if(kind=="project") Row {Checkbox(archived,{archived=it});Text("Перенести в архив")}
    error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    PrimaryAction("Сохранить","cabinet-save",!vm.busy) {
        try {
            val data=buildJsonObject {
                for(f in fields) {
                    val value=values[f.key].orEmpty().trim()
                    require(!f.required || value.isNotBlank())
                    when {
                        f.money -> put(f.key,if(value.isBlank()) JsonNull else JsonPrimitive(value.replace(',','.').toBigDecimal().movePointRight(2).longValueExact().also{require(it>=0)}))
                        f.key.endsWith("_at") || f.key=="support_until" -> put(f.key,if(value.isBlank()) JsonNull else JsonPrimitive(java.time.LocalDate.parse(value).toString()+"T00:00:00Z"))
                        f.key=="demo_url" -> put(f.key,if(value.isBlank()) JsonNull else JsonPrimitive(value))
                        else->put(f.key,value)
                    }
                }
                for((key,value) in selected) put(key,value)
                if(kind=="stages") {put("position",vm.items.size);put("client_action_required",clientAction)}
                if(kind=="project")put("archived",archived)
            }
            submitted=true
            when(kind){"profile"->vm.saveProfile(data);"lead"->vm.createLead(data);"owner-lead"->vm.patchLead(data);"project"->vm.saveProject(data);else->vm.editRecord?.let{vm.changeChild(kind,it.text("id"),data)} ?: vm.createChild(kind,data)}
        } catch(_:Exception) {error="Заполните обязательные поля. Сумму укажите в рублях, дату — ГГГГ-ММ-ДД."}
    }
    SecondaryAction("Отмена","cabinet-form-cancel",close)
}
