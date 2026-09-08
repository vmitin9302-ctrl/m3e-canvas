package dev.mitin.internal

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mitin.demo.*
import kotlinx.serialization.json.*

private val sections = linkedMapOf("dashboard" to "Обзор", "projects" to "Проекты", "leads" to "Заявки", "profile" to "Мой профиль", "clients" to "Клиенты", "analytics" to "Аналитика", "inbox" to "Все сообщения", "all-payments" to "Все платежи", "all-support" to "Все обращения", "project" to "Проект", "lead" to "Заявка", "client" to "Клиент", "stages" to "Этапы", "demos" to "Демо", "messages" to "Сообщения", "materials" to "Материалы", "files" to "Файлы", "financial" to "Стоимость и оплата", "payments" to "Платежи", "expenses" to "Сторонние расходы", "support" to "Поддержка", "events" to "История проекта")
private val labels = mapOf("title" to "Название", "description" to "Описание", "name" to "Имя", "email" to "Email", "phone" to "Телефон", "telegram" to "Telegram", "city" to "Город", "company_name" to "Компания", "status" to "Статус", "next_action" to "Следующий шаг", "agreed_price_amount" to "Стоимость разработки", "paid_amount" to "Оплачено", "balance_amount" to "Остаток", "amount_minor" to "Сумма", "body" to "Сообщение", "subject" to "Тема", "original_name" to "Файл", "active_projects" to "Активные проекты", "completed_projects" to "Завершённые проекты", "leads" to "Заявки", "new_messages" to "Новые сообщения", "waiting_client" to "Ожидают клиента", "client_actions" to "Требуют вашего участия", "overdue_stages" to "Просроченные этапы", "open_support" to "Обращения поддержки", "confirmed_payments" to "Подтверждённые оплаты", "outstanding_balance" to "Остаток к оплате", "service" to "Услуга", "business" to "Название заявки", "task" to "Задача", "comment" to "Комментарий", "client_action_text" to "Что нужно от клиента", "demo_url" to "Ссылка на демо", "version_number" to "Версия", "lead_count" to "Всего заявок", "project_count" to "Всего проектов", "average_confirmed_project_value_minor" to "Средняя стоимость проекта")
private val states = mapOf("draft" to "Подготовка", "active" to "В работе", "new" to "Новая", "not_started" to "Не начат", "in_progress" to "В работе", "waiting_client" to "Ожидаем клиента", "completed" to "Завершён", "cancelled" to "Отменён", "paused" to "Приостановлен", "support" to "Поддержка", "confirmed" to "Подтверждён", "planned" to "Запланирован", "available" to "Доступен", "quarantined" to "Проверяется", "rejected" to "Отклонён", "approved" to "Согласовано", "changes_requested" to "Нужны изменения", "requested" to "Запрошен", "received" to "Получен", "accepted" to "Принят", "needs_replacement" to "Нужна замена", "resolved" to "Решено", "closed" to "Закрыто")

@Composable fun CabinetScreen(authVm: InternalViewModel, vm: CabinetViewModel = viewModel()) {
    val auth = authVm.manager!!.state.collectAsStateWithLifecycle().value
    val uriHandler=LocalUriHandler.current
    var form by vm::form
    var material by remember(auth.profile?.userId, vm.route) { mutableStateOf<String?>(null) }
    var downloadId by remember(auth.profile?.userId, vm.route) { mutableStateOf<String?>(null) }
    var uploadContext by remember(auth.profile?.userId,vm.route) {mutableStateOf<Pair<String,String>?>(null)}
    var selectedClient by remember(auth.profile?.userId, vm.route) {mutableStateOf<JsonObject?>(null)}
    val upload = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val captured=uploadContext
        if(uri != null && captured != null && captured.first==auth.profile?.userId && captured.second==vm.route.project) vm.uploadUri(uri,material)
        uploadContext=null
    }
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if(uri != null && auth.profile != null) downloadId?.let{vm.saveFile(it,uri)};downloadId=null }
    val ime = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val scroll = key(auth.profile?.userId, vm.route, form) { rememberScrollState() }
    selectedClient?.let { client -> AlertDialog(onDismissRequest={selectedClient=null},title={Text("Назначить клиента?")},text={Text(if(vm.route.section=="assign-project") "${client.text("name")} получит доступ к проекту и его истории. Предыдущий клиент потеряет доступ." else "Заявка будет связана с клиентом ${client.text("name")}. Он получит доступ к ней.")},confirmButton={TextButton(onClick={selectedClient=null;vm.assign(client.text("id"))}){Text("Назначить")}},dismissButton={TextButton(onClick={selectedClient=null}){Text("Отмена")}}) }
    BackHandler(enabled = !ime && (form != null || vm.route != CabinetRoute())) { if (form != null) form = null else vm.back() }
    Column(Modifier.fillMaxSize().testTag("cabinet")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Heading(if(auth.profile == null) "Личный кабинет" else if(vm.route.section=="dashboard") {if(vm.owner) "Кабинет владельца" else "Ваши проекты"} else sections[vm.route.section] ?: "Выберите клиента")
            vm.notice?.let { InfoCard("Уведомление", it,tag="cabinet-notice") }
            auth.message?.let {InfoCard("Вход",it)}
            authVm.error?.let { InfoCard("Вход", it) }
            if (auth.profile == null) {
                CabinetAuth(authVm, vm)
            } else {
                if (vm.route != CabinetRoute()) SecondaryAction("Назад", "cabinet-back") { vm.back() }
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("cabinet-loading"))
                if (form != null) CabinetForm(form!!, vm) { form = null }
                else {
                    if (vm.route.section == "dashboard") {
                        if(vm.document.isNotEmpty()) Column(Modifier.testTag("cabinet-summary"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            for(key in listOf("active_projects","new_messages","client_actions")) if(vm.document.containsKey(key))
                                Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
                                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                        Text(labels.getValue(key),Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                                        Text(vm.document.text(key),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
                                    }
                                }
                        }
                        val menu=if(vm.owner) listOf("projects","leads","clients","inbox","all-payments","all-support","analytics") else listOf("projects","leads","profile","inbox","all-payments","all-materials","all-support")
                        val columns=if(LocalDensity.current.fontScale>=1.4f)1 else 2
                        for(group in menu.chunked(columns)) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            for(section in group) OutlinedCard(onClick={vm.open(CabinetRoute(section))},enabled=!vm.busy,modifier=Modifier.weight(1f).testTag("cabinet-$section"),shape=RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Icon(when(section){"projects"->Icons.Outlined.Folder;"leads"->Icons.Outlined.Description;"profile"->Icons.Outlined.Person;"clients"->Icons.Outlined.People;"inbox"->Icons.Outlined.ChatBubbleOutline;"all-payments"->Icons.Outlined.Payments;"all-materials"->Icons.Outlined.AttachFile;"analytics"->Icons.Outlined.QueryStats;else->Icons.Outlined.SupportAgent},contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                                    Text(sections[section] ?: "Материалы",style=MaterialTheme.typography.titleMedium)
                                }
                            }
                            if(columns==2 && group.size==1) Spacer(Modifier.weight(1f))
                        }
                        SecondaryAction("Активные сессии", "cabinet-sessions") { form = "sessions"; authVm.sessions() }
                        SecondaryAction("Выйти", "cabinet-logout") { authVm.logout() }
                    }
                    CabinetDocument(if(vm.route.section=="dashboard") JsonObject(vm.document.filterKeys{it !in setOf("active_projects","new_messages","client_actions")}) else vm.document)
                    if(vm.route.section == "dashboard") for((key,title) in listOf("expected_actions" to "Ожидаемые действия","latest_updates" to "Последние обновления")) {
                        Text(title,style=MaterialTheme.typography.titleMedium)
                        (vm.document[key] as? JsonArray)?.forEach { element -> val event=element.jsonObject
                            CabinetDocument(event)
                            SecondaryAction("К проекту","dashboard-${event.text("id")}") {vm.open(CabinetRoute("project",event.text("project_id")))}
                        }
                    }
                    if(vm.route.section == "client") for(section in listOf("projects","leads")) {
                        Text(sections.getValue(section))
                        (vm.document[section] as? JsonArray)?.forEach { element -> val item=element.jsonObject
                            CabinetDocument(item)
                            SecondaryAction("Открыть", "client-$section-${item.text("id")}") {vm.open(CabinetRoute(if(section=="projects") "project" else "lead",item.text("id")))}
                        }
                    }
                    if(vm.route.section == "client") (vm.document["support_requests"] as? JsonArray)?.let { requests ->
                        Text("Последние обращения поддержки: ${requests.size}")
                        for(element in requests) {val request=element.jsonObject;CabinetDocument(request)
                            SecondaryAction("К проекту","client-support-${request.text("id")}"){vm.open(CabinetRoute("project",request.text("project_id")))}
                        }
                    }
                    if(vm.route.section == "project") {
                        for(section in listOf("stages","demos","messages","materials","files","financial","payments","expenses","support","events"))
                            SecondaryAction(sections.getValue(section), "project-$section") { vm.open(CabinetRoute(section, project=vm.route.id)) }
                    }
                    for ((index,item) in vm.items.withIndex()) key(item.text("id")) {
                        Card(Modifier.fillMaxWidth().testTag("cabinet-item-$index")) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            CabinetDocument(item)
                            if(vm.route.section=="demos") safePortfolioUrl(item.text("demo_url"))?.let { url ->
                                SecondaryAction("Открыть демо","open-demo-$index") {uriHandler.openUri(url)}
                            }
                            when(vm.route.section) {
                                "assign-lead", "assign-project" -> SecondaryAction("Выбрать клиента","assign-$index") {selectedClient=item}
                                "projects", "leads", "clients" -> SecondaryAction("Открыть", "open-$index") { vm.open(CabinetRoute(when(vm.route.section){"projects"->"project";"clients"->"client";else->"lead"},item.text("id"))) }
                                "inbox", "all-payments", "all-support", "all-materials" -> SecondaryAction("К проекту", "open-$index") { vm.open(CabinetRoute("project", item.text("project_id"))) }
                                "demos" -> if(!vm.owner && item["decision"] !is JsonObject) {
                                    var comment by remember { mutableStateOf("") }
                                    Input("Комментарий", comment, "demo-comment-$index") { comment=it }
                                    PrimaryAction("Согласовать эту версию", "approve-$index", !vm.busy) { vm.decide(item.text("id"),"approved",comment) }
                                    SecondaryAction("Запросить изменения", "changes-$index") { vm.decide(item.text("id"),"changes_requested",comment) }
                                }
                                "stages" -> if(vm.owner) {
                                    for (status in listOf("in_progress","waiting_client","completed","cancelled")) SecondaryAction(states.getValue(status), "stage-$status-$index") {
                                        vm.changeChild("stages",item.text("id"), buildJsonObject { put("title",item.text("title")); put("description",item.text("description")); put("position",item.number("position") ?: 0);put("status",status) })
                                    }
                                    if(index > 0) SecondaryAction("Переместить выше", "stage-up-$index") { val ids=vm.items.map{it.text("id")}.toMutableList(); val id=ids.removeAt(index); ids.add(index-1,id);vm.reorderStages(ids) }
                                }
                                "materials" -> {
                                    SecondaryAction("Загрузить материал","upload-material-$index") {material=item.text("id");uploadContext=auth.profile.userId to vm.route.project!!;upload.launch(arrayOf("text/plain","application/pdf","image/png","image/jpeg"))}
                                    if(vm.owner) for(status in listOf("accepted","needs_replacement","cancelled")) SecondaryAction(states.getValue(status),"material-$status-$index") { vm.changeChild("materials",item.text("id"),buildJsonObject {put("status",status)}) }
                                }
                                "files" -> if(item.text("status")=="available") SecondaryAction("Сохранить файл","download-$index") { downloadId=item.text("id");download.launch(item.text("original_name")) }
                                "payments", "expenses" -> if(vm.owner) SecondaryAction("Изменить запись","edit-$index") {vm.edit(item);form=vm.route.section}
                                "support" -> if(vm.owner) for(status in listOf("in_progress","waiting_client","resolved","closed")) SecondaryAction(states.getValue(status),"support-$status-$index") { vm.changeChild("support",item.text("id"),buildJsonObject {put("status",status)}) }
                            }
                        } }
                    }
                    if(vm.status == CabinetStatus.Empty) InfoCard("Здесь пока пусто", "Новые данные появятся после первого действия.")
                    if(vm.route.section=="files") SecondaryAction("Загрузить файл","upload-file") {material=null;uploadContext=auth.profile.userId to vm.route.project!!;upload.launch(arrayOf("text/plain","application/pdf","image/png","image/jpeg"))}
                    val action = when(vm.route.section) {"profile"->"profile";"leads"->if(!vm.owner) "lead" else null;"project"->if(vm.owner) "project" else null;"stages","demos","materials","payments","expenses"->if(vm.owner) vm.route.section else null;"support"->"support";else->null}
                    if(action != null) PrimaryAction(if(action in setOf("profile","project")) "Редактировать" else "Добавить", "cabinet-add",!vm.busy) { vm.edit(null);form=action }
                    if(vm.route.section == "lead" && vm.owner) PrimaryAction("Создать проект из заявки", "convert-lead",!vm.busy) { vm.convert() }
                    if(vm.route.section == "lead" && vm.owner) SecondaryAction("Изменить статус заявки","edit-lead") {form="owner-lead"}
                    if(vm.owner && vm.route.section in setOf("lead","project")) SecondaryAction("Назначить клиента","assign-client") {vm.open(CabinetRoute("assign-${vm.route.section}",vm.route.id))}
                    vm.nextOffset?.let { SecondaryAction("Показать ещё", "cabinet-more") {vm.refresh(true)} }
                    SecondaryAction("Обновить", "cabinet-refresh") { vm.refresh() }
                }
                if(form == "sessions") {
                    for(session in authVm.page.items) { Text(session.id); SecondaryAction("Завершить сессию", "revoke-${session.id}") {authVm.revoke(session)} }
                    authVm.page.nextOffset?.let{SecondaryAction("Ещё сессии","sessions-more"){authVm.sessions(true)}}
                    SecondaryAction("Выйти на всех устройствах", "cabinet-logout-all") {authVm.logout(true)}
                }
            }
        }
        if(auth.profile != null && vm.route.section == "messages") {
            var body by vm::messageDraft
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(body,{if(it.length<=10000)body=it},label={Text("Сообщение")},maxLines=if(ime)2 else 3,modifier=Modifier.weight(1f).testTag("project-message"))
                FilledIconButton(onClick={vm.sendMessage(body)},enabled=!vm.busy && body.isNotBlank(),modifier=Modifier.size(56.dp).testTag("send-project-message")) {
                    Icon(Icons.AutoMirrored.Outlined.Send,contentDescription="Отправить сообщение")
                }
            }
        }
    }
}

@Composable private fun CabinetDocument(value: JsonObject) {
    val extra=mapOf("due_at" to "Срок","planned_start_at" to "Плановое начало","planned_finish_at" to "Плановое завершение","actual_finish_at" to "Завершён","support_until" to "Поддержка до","last_activity_at" to "Последняя активность","legal_status" to "Правовой статус","registration_status" to "Регистрация","confirmed_project_value" to "Стоимость согласованных проектов","recurrence" to "Периодичность","category" to "Категория","provider" to "Поставщик","note" to "Примечание","type" to "Тип")
    val names=mapOf("reviewing" to "Рассматриваем","need_info" to "Нужна информация","qualified" to "Квалифицирована","converted" to "Проект создан","archived" to "Архив","unknown" to "Не указан","individual" to "Физическое лицо","self_employed" to "Самозанятый","individual_entrepreneur" to "ИП","individual_entrepreneur_npd" to "ИП на НПД","llc" to "ООО","other" to "Другое","pending" to "Ожидает подтверждения","unregistered" to "Без аккаунта","once" to "Разово","monthly" to "Ежемесячно","yearly" to "Ежегодно","one_time_external" to "Разовый сторонний расход","infrastructure" to "Инфраструктура","api_service" to "API и сервисы","prepayment" to "Предоплата","payment" to "Оплата","final_payment" to "Финальный платёж","refund" to "Возврат","correction" to "Дополнительное поступление","bug" to "Ошибка","change" to "Изменение","feature" to "Новая функция","question" to "Вопрос")
    for((key,label) in labels+extra) if(value.containsKey(key)) {
        val raw=value.text(key)
        val money=key.endsWith("amount") || key.endsWith("_minor") || key in setOf("confirmed_payments","outstanding_balance","confirmed_project_value")
        val display=when {
            value[key] is JsonNull -> if(money) "Не согласовано" else "Не указано"
            money -> value.number(key)?.let { java.math.BigDecimal.valueOf(it,2).toPlainString()+" ₽" } ?: raw
            key.endsWith("_at") || key=="support_until" -> runCatching{java.time.Instant.parse(raw).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))}.getOrDefault(raw)
            key=="registration_status" && raw=="active" -> "Подтверждена"
            else -> names[raw] ?: states[raw] ?: raw
        }
        if(display.isNotBlank()) Text("$label: $display", style=MaterialTheme.typography.bodyLarge)
    }
    (value["decision"] as? JsonObject)?.let { Text("Решение: ${states[it.text("decision")] ?: it.text("decision")}");Text(it.text("comment")) }
    (value["lead_to_project_conversion"] as? JsonObject)?.let { Text("Заявок стали проектами: ${it.text("numerator")} из ${it.text("denominator")}") }
    (value["project_sources"] as? JsonArray)?.forEach { element -> val source=element.jsonObject;Text("${mapOf("app" to "Приложение","site" to "Сайт","telegram" to "Telegram","max" to "MAX","vk" to "VK")[source.text("source")] ?: "Другой источник"}: ${source.text("count")}") }
}

@Composable internal fun Input(label:String, value:String, tag:String, secret:Boolean=false, maxLines:Int=5, changed:(String)->Unit) {
    OutlinedTextField(value, changed, label={Text(label)}, modifier=Modifier.fillMaxWidth().testTag(tag), visualTransformation=if(secret) PasswordVisualTransformation() else VisualTransformation.None, singleLine=secret || maxLines==1, maxLines=if(secret) 1 else maxLines)
}

@Composable private fun CabinetAuth(auth:InternalViewModel, vm:CabinetViewModel) {
    val state = auth.manager!!.state.collectAsStateWithLifecycle().value
    if (state.mfaRequired) {
        OwnerMfaScreen(auth.busy, auth.error, auth::verifyMfa, auth::cancelMfa)
        return
    }
    val registration = auth.getApplication<InternalApplication>().capabilities?.registration == true
    var mode by vm::authMode
    val uri = LocalUriHandler.current
    LaunchedEffect(registration) { if (!registration) mode = "login" }
    var email by remember { mutableStateOf("") }; var password by remember {mutableStateOf("")}; var name by remember {mutableStateOf("")};var phone by remember {mutableStateOf("")};var token by remember {mutableStateOf("")};var consent by remember {mutableStateOf(false)}
    var confirmation by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    val ime = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = mode != "login" && !ime && !vm.busy) { mode="login";password="";confirmation="";token="" }
    Text(when(mode){"register"->"Регистрация клиента";"verify-email"->"Подтверждение email";"password-reset/request"->"Восстановление пароля";"password-reset/confirm"->"Новый пароль";else->"Вход"})
    if(mode in setOf("login","register","password-reset/request","resend-verification")) Input("Email",email,"cabinet-email"){email=it}
    if(mode == "register") {Input("Имя",name,"cabinet-name"){name=it};Input("Телефон (необязательно)",phone,"cabinet-phone"){phone=it}}
    if(mode in setOf("login","register","password-reset/confirm")) Input("Пароль",password,"cabinet-password",true){password=it}
    if(mode in setOf("register","password-reset/confirm")) Text("Пароль — от 15 до 128 символов.",style=MaterialTheme.typography.bodySmall)
    if(mode in setOf("register","password-reset/confirm")) {
        Input("Повтор пароля",confirmation,"cabinet-password-repeat",true){confirmation=it}
        if(confirmation.isNotEmpty() && password != confirmation) Text("Пароли не совпадают.")
    }
    if(mode in setOf("verify-email","password-reset/confirm")) Input("Код из письма",token,"cabinet-token",true){token=it}
    if(mode == "register") {Row {Checkbox(consent,{consent=it},Modifier.testTag("cabinet-consent"));Text("Согласен на обработку данных для регистрации и работы над моими проектами")}}
    if(mode == "register") {
        TextButton(onClick={uri.openUri("https://24promtbot.ru/privacy.html")}) {Text("Политика конфиденциальности")}
        TextButton(onClick={uri.openUri("https://24promtbot.ru/consent.html")}) {Text("Согласие на обработку ПД")}
        Row {Checkbox(terms,{terms=it},Modifier.testTag("cabinet-terms"));Text("Принимаю пользовательское соглашение")}
        TextButton(onClick={uri.openUri("https://24promtbot.ru/terms.html")}) {Text("Пользовательское соглашение")}
    }
    val valid = validAuthForm(mode,email,name,password,confirmation,token,consent,terms)
    PrimaryAction(if(vm.busy) "Выполняем…" else if(mode=="register") "Создать аккаунт" else "Продолжить","cabinet-auth-submit",!vm.busy && !auth.busy && valid) {
        if(mode == "login") { auth.login(email,Secret(password));password="" }
        else vm.authAction(mode,buildJsonObject {
            if(mode in setOf("register","password-reset/request","resend-verification")) put("email",email)
            if(mode == "register") {put("name",name);put("phone",phone);put("consent",consent);put("terms_consent",terms)}
            if(mode in setOf("register","password-reset/confirm"))put("password",password)
            if(mode in setOf("verify-email","password-reset/confirm"))put("token",token)
        }) { password="";confirmation="";token="";mode=if(mode=="register") "verify-email" else "login" }
    }
    if (!registration) Text("Регистрация временно недоступна", Modifier.testTag("registration-unavailable"))
    for((action,label) in listOf("login" to "Уже есть аккаунт", "register" to "Создать аккаунт", "verify-email" to "Подтвердить email", "resend-verification" to "Отправить письмо ещё раз", "password-reset/request" to "Забыли пароль?", "password-reset/confirm" to "Есть код восстановления")) if(!vm.busy && action != mode && (registration || action == "login")) SecondaryAction(label,"auth-$action"){mode=action;password="";confirmation="";token=""}
}

@Composable internal fun OwnerMfaScreen(busy: Boolean, error: String?, verify: (Secret) -> Unit, cancel: () -> Unit) {
    // Codes never enter SavedState. The application retains only the short-lived
    // challenge across Activity recreation; process death requires a fresh login.
    var code by remember { mutableStateOf("") }
    val ime = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = !ime) { code = ""; cancel() }
    Text("Подтверждение входа", style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("owner-mfa"))
    Text("Введите шестизначный код из приложения-аутентификатора.")
    OutlinedTextField(value = code, onValueChange = { value -> code = value.filter { it in '0'..'9' }.take(6) },
        label = { Text("Код подтверждения") }, singleLine = true, enabled = !busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("owner-mfa-code"))
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    PrimaryAction(if (busy) "Проверяем…" else "Подтвердить вход", "owner-mfa-submit", !busy && code.length == 6) {
        val submitted = Secret(code); code = ""; verify(submitted)
    }
    SecondaryAction("Вернуться ко входу", "owner-mfa-cancel") { code = ""; cancel() }
}
