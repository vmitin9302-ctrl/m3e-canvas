package dev.mitin.internal

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    var form by remember(auth.profile?.userId, vm.route) { mutableStateOf<String?>(null) }
    var material by remember(auth.profile?.userId, vm.route) { mutableStateOf<String?>(null) }
    var downloadId by remember(auth.profile?.userId, vm.route) { mutableStateOf<String?>(null) }
    val upload = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri != null && auth.profile != null) vm.uploadUri(uri,material) }
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if(uri != null && auth.profile != null) downloadId?.let{vm.saveFile(it,uri)};downloadId=null }
    val ime = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = !ime && (form != null || vm.route != CabinetRoute())) { if (form != null) form = null else vm.back() }
    Column(Modifier.fillMaxSize().testTag("cabinet")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Heading(if(auth.profile == null) "Личный кабинет" else if(vm.owner) "Кабинет владельца" else "Ваши проекты")
            vm.notice?.let { InfoCard("Уведомление", it) }
            authVm.error?.let { InfoCard("Вход", it) }
            if (auth.profile == null) {
                CabinetAuth(authVm, vm)
            } else {
                Text(sections[vm.route.section] ?: "Кабинет", style = MaterialTheme.typography.titleLarge)
                if (vm.route != CabinetRoute()) SecondaryAction("Назад", "cabinet-back") { vm.back() }
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (form != null) CabinetForm(form!!, vm) { form = null }
                else {
                    if (vm.route.section == "dashboard") {
                        for (section in if(vm.owner) listOf("projects","leads","clients","inbox","all-payments","all-support","analytics") else listOf("projects","leads","profile"))
                            SecondaryAction(sections.getValue(section), "cabinet-$section") { vm.open(CabinetRoute(section)) }
                        SecondaryAction("Активные сессии", "cabinet-sessions") { form = "sessions"; authVm.sessions() }
                        SecondaryAction("Выйти", "cabinet-logout") { authVm.logout() }
                    }
                    CabinetDocument(vm.document)
                    if(vm.route.section == "project") {
                        for(section in listOf("stages","demos","messages","materials","files","financial","payments","expenses","support","events"))
                            SecondaryAction(sections.getValue(section), "project-$section") { vm.open(CabinetRoute(section, project=vm.route.id)) }
                    }
                    for ((index,item) in vm.items.withIndex()) key(item.text("id")) {
                        Card(Modifier.fillMaxWidth().testTag("cabinet-item-$index")) { Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            CabinetDocument(item)
                            when(vm.route.section) {
                                "projects", "leads", "clients" -> SecondaryAction("Открыть", "open-$index") { vm.open(CabinetRoute(when(vm.route.section){"projects"->"project";"clients"->"client";else->"lead"},item.text("id"))) }
                                "inbox", "all-payments", "all-support" -> SecondaryAction("К проекту", "open-$index") { vm.open(CabinetRoute("project", item.text("project_id"))) }
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
                                    SecondaryAction("Загрузить материал","upload-material-$index") {material=item.text("id");upload.launch(arrayOf("text/plain","application/pdf","image/png","image/jpeg"))}
                                    if(vm.owner) for(status in listOf("accepted","needs_replacement","cancelled")) SecondaryAction(states.getValue(status),"material-$status-$index") { vm.changeChild("materials",item.text("id"),buildJsonObject {put("status",status)}) }
                                }
                                "files" -> if(item.text("status")=="available") SecondaryAction("Сохранить файл","download-$index") { downloadId=item.text("id");download.launch(item.text("original_name")) }
                                "support" -> if(vm.owner) for(status in listOf("in_progress","waiting_client","resolved","closed")) SecondaryAction(states.getValue(status),"support-$status-$index") { vm.changeChild("support",item.text("id"),buildJsonObject {put("status",status)}) }
                            }
                        } }
                    }
                    if(vm.status == CabinetStatus.Empty) InfoCard("Здесь пока пусто", "Новые данные появятся после первого действия.")
                    if(vm.route.section=="files") SecondaryAction("Загрузить файл","upload-file") {material=null;upload.launch(arrayOf("text/plain","application/pdf","image/png","image/jpeg"))}
                    val action = when(vm.route.section) {"profile"->"profile";"leads"->if(!vm.owner) "lead" else null;"project"->if(vm.owner) "project" else null;"stages","demos","materials","payments","expenses"->if(vm.owner) vm.route.section else null;"support"->"support";else->null}
                    if(action != null) PrimaryAction(if(action in setOf("profile","project")) "Редактировать" else "Добавить", "cabinet-add",!vm.busy) { form=action }
                    if(vm.route.section == "lead" && vm.owner) PrimaryAction("Создать проект из заявки", "convert-lead",!vm.busy) { vm.convert() }
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
            var body by remember(auth.profile.userId, vm.route.project) { mutableStateOf("") }
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Input("Сообщение по проекту",body,"project-message"){body=it}
                PrimaryAction("Отправить","send-project-message",!vm.busy && body.isNotBlank()){vm.sendMessage(body)}
            }
        }
    }
}

@Composable private fun CabinetDocument(value: JsonObject) {
    for((key,label) in labels) if(value.containsKey(key)) {
        val raw=value.text(key)
        val money=key.endsWith("amount") || key.endsWith("_minor") || key in setOf("confirmed_payments","outstanding_balance")
        val display=if(value[key] is JsonNull) "Не согласовано" else if(money) value.number(key)?.let { java.math.BigDecimal.valueOf(it,2).toPlainString()+" ₽" } ?: raw else states[raw] ?: raw
        if(display.isNotBlank()) Text("$label: $display", style=MaterialTheme.typography.bodyLarge)
    }
    (value["decision"] as? JsonObject)?.let { Text("Решение: ${states[it.text("decision")] ?: it.text("decision")}");Text(it.text("comment")) }
}

@Composable internal fun Input(label:String, value:String, tag:String, secret:Boolean=false, changed:(String)->Unit) {
    OutlinedTextField(value, changed, label={Text(label)}, modifier=Modifier.fillMaxWidth().testTag(tag), visualTransformation=if(secret) PasswordVisualTransformation() else VisualTransformation.None, singleLine=secret, maxLines=if(secret) 1 else 5)
}

@Composable private fun CabinetAuth(auth:InternalViewModel, vm:CabinetViewModel) {
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }; var password by remember {mutableStateOf("")}; var name by remember {mutableStateOf("")};var phone by remember {mutableStateOf("")};var token by remember {mutableStateOf("")};var consent by remember {mutableStateOf(false)}
    Text(when(mode){"register"->"Регистрация клиента";"verify-email"->"Подтверждение email";"password-reset/request"->"Восстановление пароля";"password-reset/confirm"->"Новый пароль";else->"Вход"})
    if(mode in setOf("login","register","password-reset/request","resend-verification")) Input("Email",email,"cabinet-email"){email=it}
    if(mode == "register") {Input("Имя",name,"cabinet-name"){name=it};Input("Телефон (необязательно)",phone,"cabinet-phone"){phone=it}}
    if(mode in setOf("login","register","password-reset/confirm")) Input("Пароль",password,"cabinet-password",true){password=it}
    if(mode in setOf("verify-email","password-reset/confirm")) Input("Код из письма",token,"cabinet-token",true){token=it}
    if(mode == "register") {Row {Checkbox(consent,{consent=it});Text("Согласен на обработку данных для регистрации и работы над моими проектами")}}
    PrimaryAction("Продолжить","cabinet-auth-submit",!vm.busy && !auth.busy) {
        if(mode == "login") { auth.login(email,Secret(password));password="" }
        else vm.authAction(mode,buildJsonObject {
            if(mode in setOf("register","password-reset/request","resend-verification")) put("email",email)
            if(mode == "register") {put("name",name);put("phone",phone);put("consent",consent)}
            if(mode in setOf("register","password-reset/confirm"))put("password",password)
            if(mode in setOf("verify-email","password-reset/confirm"))put("token",token)
        }) { password="";token="";mode=if(mode=="register") "verify-email" else "login" }
    }
    for((action,label) in listOf("login" to "Уже есть аккаунт", "register" to "Создать аккаунт", "verify-email" to "Подтвердить email", "resend-verification" to "Отправить письмо ещё раз", "password-reset/request" to "Забыли пароль?", "password-reset/confirm" to "Есть код восстановления")) if(action != mode) SecondaryAction(label,"auth-$action"){mode=action;password="";token=""}
}
