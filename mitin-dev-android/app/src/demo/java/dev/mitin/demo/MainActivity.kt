package dev.mitin.demo

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { MitinTheme { MitinApp() } }
    }
}

@Composable fun MitinApp(vm: DemoViewModel = viewModel()) {
    val data by vm.repository.state.collectAsStateWithLifecycle()
    var roleName by rememberSaveable { mutableStateOf(DemoRole.CLIENT.name) }
    val role = DemoRole.valueOf(roleName)
    var stack by rememberSaveable { mutableStateOf(listOf("c-welcome")) }
    val screen = stack.last()
    var projectHistory by rememberSaveable { mutableStateOf(listOf(1)) }
    var leadHistory by rememberSaveable { mutableStateOf(listOf(1045)) }
    var selectedLeadId by rememberSaveable { mutableIntStateOf(1045) }
    var selectedProjectId by rememberSaveable { mutableIntStateOf(1) }
    var selectedStage by rememberSaveable { mutableIntStateOf(2) }
    var stageChoice by rememberSaveable { mutableStateOf(StageStatus.IN_PROGRESS.name) }
    var type by rememberSaveable { mutableStateOf("Сайт") }
    var task by rememberSaveable { mutableStateOf(Brief().task) }
    var features by rememberSaveable { mutableStateOf(Brief().features) }
    var budgetName by rememberSaveable { mutableStateOf(BudgetRange.FROM_20_40.name) }
    var deadline by rememberSaveable { mutableStateOf(Brief().deadline) }
    var integrations by rememberSaveable { mutableStateOf(Brief().integrations) }
    var comments by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var leadFilter by rememberSaveable { mutableStateOf("Все") }
    var showReset by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val project = data.projects.find { it.id == selectedProjectId } ?: data.projects.first()
    val lead = data.leads.find { it.id == selectedLeadId } ?: data.leads.last()
    val projectLead = data.leads.single { it.id == project.leadId }
    val brief = Brief(type, task, features, BudgetRange.valueOf(budgetName), deadline, integrations, comments)
    fun go(route: String) {
        keyboard?.hide()
        if (screen != route) {
            stack = stack + route
            projectHistory = projectHistory + selectedProjectId
            leadHistory = leadHistory + selectedLeadId
        }
    }
    fun root(route: String) {
        keyboard?.hide(); stack = listOf(route)
        projectHistory = listOf(selectedProjectId); leadHistory = listOf(selectedLeadId)
    }
    fun back() {
        keyboard?.hide()
        if (stack.size > 1) {
            stack = stack.dropLast(1)
            projectHistory = projectHistory.dropLast(1); leadHistory = leadHistory.dropLast(1)
            selectedProjectId = projectHistory.last(); selectedLeadId = leadHistory.last()
        }
    }
    fun stages() { selectedStage = project.currentStage; stageChoice = project.stages[selectedStage].name; go("o-stages") }
    BackHandler(enabled = stack.size > 1) { back() }
    val onboarding = screen in setOf("demo", "c-welcome", "c-login", "c-type", "c-brief", "c-brief-details", "c-review", "c-sent")
    val title = when (screen) {
        "c-brief" -> "MITIN DEV AI"
        "c-brief-details" -> "Уточним детали"
        "c-review" -> "Проверка заявки"
        "c-project", "o-project" -> "Проект"
        "c-stage", "o-stages" -> "Этапы проекта"
        "o-leads", "o-lead" -> "Заявки"
        "c-demo", "c-demo-site" -> "Демо проекта"
        else -> "MITIN DEV"
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (stack.size > 1) IconButton(onClick = { back() }, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад")
                    } else Spacer(Modifier.width(12.dp))
                    BrandEmblem(32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { go("demo") }, modifier = Modifier.testTag("demo-menu")) { Text("ДЕМО") }
                }
            }
        },
        bottomBar = {
            if (!onboarding) {
                val entries = if (role == DemoRole.CLIENT) listOf(
                    Triple("Главная", "c-home", Icons.Outlined.Home), Triple("Проект", "c-project", Icons.Outlined.WorkOutline),
                    Triple("Сообщения", "c-chat", Icons.AutoMirrored.Outlined.Chat), Triple("Профиль", "c-profile", Icons.Outlined.PersonOutline)
                ) else listOf(
                    Triple("Главная", "o-home", Icons.Outlined.Dashboard), Triple("Заявки", "o-leads", Icons.Outlined.Inbox),
                    Triple("Проекты", "o-projects", Icons.Outlined.WorkOutline), Triple("Ещё", "o-more", Icons.Outlined.MoreHoriz)
                )
                NavigationBar(modifier = Modifier.border(androidx.compose.foundation.BorderStroke(0.5.dp, NeonEdge))) {
                    entries.forEach { (label, route, icon) ->
                        NavigationBarItem(
                            selected = screen == route,
                            onClick = { root(route) },
                            icon = { Icon(icon, null) }, label = { Text(label) },
                            modifier = Modifier.testTag("nav-$route").heightIn(min = if (LocalDensity.current.fontScale > 1.3f) 110.dp else 80.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(BrandBackground).padding(padding), contentAlignment = Alignment.TopCenter) {
            key(screen) {
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(20.dp).testTag("screen-$screen"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (screen) {
                        "demo" -> {
                            Eyebrow("Демонстрационная сборка")
                            Heading("Один проект.\nДве стороны.")
                            InfoCard("Только на этом устройстве", "Обе роли видят общие вымышленные данные. Авторизации и соединения с сервером нет.", true)
                            PrimaryAction("Я клиент", "choose-client") { roleName = DemoRole.CLIENT.name; root("c-welcome") }
                            SecondaryAction("Я владелец", "choose-owner") { roleName = DemoRole.OWNER.name; root("o-home") }
                            Note("Не вводите реальные персональные данные. Демо сохраняется между запусками; сброс доступен в профиле и разделе «Ещё».")
                        }
                        "c-welcome" -> {
                            BrandHero("СОЗДАЁМ DIGITAL-ПРОДУКТЫ")
                            Text("Сайты, боты и digital-системы для бизнеса.", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(12.dp))
                            InfoCard("Ваш проект — рядом", "Заявка, этапы и решения в одном месте. Здесь можно пройти весь путь на демоданных.", true)
                            PrimaryAction("Создать проект", "create-request") { go("c-type") }
                            SecondaryAction("Войти · демо", "demo-login") { go("c-login") }
                            Note("Офлайн-демо · без аккаунта и отправки данных")
                        }
                        "c-login" -> {
                            Heading("Посмотреть кабинет")
                            InfoCard("Демо-вход", "Будет открыт вымышленный профиль Алексея Смирнова. Пароль и настоящий вход в этой сборке отсутствуют.")
                            PrimaryAction("Открыть кабинет · демо", "open-home") { root("c-home") }
                        }
                        "c-type" -> {
                            Eyebrow("Новый проект · шаг 1 из 4")
                            Heading("Что хотите\nсоздать?")
                            projectTypes.forEachIndexed { index, value ->
                                val icon = listOf(Icons.Outlined.Language, Icons.Outlined.SmartToy, Icons.Outlined.Dashboard,
                                    Icons.Outlined.Bolt, Icons.Outlined.AutoAwesome, Icons.Outlined.Lightbulb)[index]
                                ActionRow(value, "Короткий бриф по вашей идее", icon, "type-$index") { type = value; go("c-brief") }
                            }
                        }
                        "c-brief" -> {
                            Eyebrow("Новый проект · шаг 2 из 4")
                            Heading("Начнём с идеи")
                            InfoCard("MITIN DEV AI · демонстрация", "Расскажите своими словами, что хотите создать и какую задачу бизнеса это должно решить.", true)
                            Note("Сценарный бриф: вопросы подготовлены заранее, AI не подключён. В поле — вымышленный пример, его можно изменить.")
                            Input("Ваша задача", task, "task", 2000) { task = it }
                            InfoCard("Подсказка", "Для кого проект? Что сейчас неудобно? Какой результат вы хотите получить?")
                            PrimaryAction("Продолжить бриф", "brief-next", task.isNotBlank()) { go("c-brief-details") }
                        }
                        "c-brief-details" -> {
                            Eyebrow("Новый проект · шаг 3 из 4")
                            InfoCard("MITIN DEV AI · демонстрация", "Что должно работать в первой версии? Какой бюджет и срок вы рассматриваете?", true)
                            Input("Основные функции", features, "features", 2000) { features = it }
                            Picker("Бюджет", BudgetRange.valueOf(budgetName).label, BudgetRange.entries.map { it.label }, "budget") {
                                budgetName = BudgetRange.entries[it].name
                            }
                            Note("Диапазон бюджета — ориентир, не согласованная стоимость.")
                            Input("Желаемый срок", deadline, "deadline", 200) { deadline = it }
                            Input("Интеграции", integrations, "integrations", 1000) { integrations = it }
                            Input("Комментарии", comments, "comments", 2000) { comments = it }
                            PrimaryAction("Проверить заявку", "review", features.isNotBlank()) { go("c-review") }
                        }
                        "c-review" -> {
                            Eyebrow("Новый проект · шаг 4 из 4")
                            Heading("Всё верно?")
                            BriefDetails(brief)
                            Note("Сохранение создаст только локальную демозаявку. Ничего не отправляется MITIN DEV.")
                            PrimaryAction("Сохранить демозаявку", "submit", !vm.busy) {
                                vm.act { selectedLeadId = it.submit(brief); root("c-sent") }
                            }
                            SecondaryAction("Изменить", "edit-brief") { go("c-brief") }
                        }
                        "c-sent" -> {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Heading("Заявка №${lead.id}\nсохранена")
                            InfoCard("Локально · без отправки", "Владелец уже увидит её в списке заявок этой демосборки. Реальный проект и договор не созданы.", true)
                            Note("Для знакомства с кабинетом ниже доступен подготовленный пример согласованного проекта. Он не является результатом вашей новой заявки.")
                            PrimaryAction("Главная клиента · демо", "sent-home") { selectedProjectId = 1; root("c-home") }
                            SecondaryAction("Посмотреть как владелец", "sent-owner") { roleName = DemoRole.OWNER.name; root("o-leads") }
                        }
                        "c-home" -> {
                            Eyebrow("Добрый день")
                            Heading("Алексей")
                            data.leads.lastOrNull { it.status == LeadStatus.NEW }?.let {
                                InfoCard("Заявка №${it.id} · новая", "Сохранена локально. Создание проекта доступно в роли владельца.")
                            }
                            Eyebrow("Ваши демопроекты")
                            data.projects.reversed().forEach { p ->
                                ActionRow(p.name, "${stageNames[p.currentStage]} · ${p.stages[p.currentStage].label}", Icons.Outlined.WorkOutline, "client-project-${p.id}") {
                                    selectedProjectId = p.id; go("c-project")
                                }
                            }
                            ActionRow("Следующее действие", if (project.demoAvailable) "Согласовать мобильную версию" else "Согласовать ТЗ и стоимость", Icons.Outlined.TaskAlt, "next-action") {
                                go(if (project.demoAvailable) "c-demo" else "c-project")
                            }
                            ActionRow("Материалы", "Что нужно для проекта", Icons.Outlined.FolderOpen, "materials") { go("c-materials") }
                            ActionRow("Поддержка", "Поможем с вашим сайтом", Icons.Outlined.SupportAgent, "support") { go("c-support") }
                        }
                        "c-project", "o-project" -> {
                            Heading(project.name)
                            Note("${projectLead.brief.type} · вымышленный проект · заявка №${project.leadId}")
                            InfoCard(if (project.agreedPrice == null) "Стоимость не согласована" else "${money(project.agreedPrice)} · разработка",
                                if (project.paid == null) "Оплата отсутствует. Бюджет заявки не является ценой." else "Оплачено ${money(project.paid)} · остаток ${money(project.agreedPrice!! - project.paid)}", true)
                            Detail("Бюджет клиента", projectLead.brief.budget.label)
                            ActionRow("Текущий этап", "${stageNames[project.currentStage]} · ${project.stages[project.currentStage].label}", Icons.Outlined.Code, "current-stage") {
                                if (role == DemoRole.OWNER) stages() else go("c-stage")
                            }
                            StageSummary(project)
                            if (role == DemoRole.OWNER) {
                                PrimaryAction("Изменить этап", "edit-stages") { stages() }
                                InfoCard("Что согласовано", if (project.agreedPrice == null) "Пока ничего. Необходимо обсудить ТЗ, стоимость и срок." else "Главная, услуги, каталог и форма заявки. Мобильная версия входит в стоимость.")
                                Detail("Срок", project.agreedDeadline ?: "Не согласован")
                                Detail("Следующее действие", if (project.demoAvailable) "Получить фотографии работ" else "Согласовать условия")
                                ActionRow("Решение по демо", project.demoDecision, Icons.Outlined.FactCheck, "owner-demo") { go("o-demo-review") }
                            } else {
                                PrimaryAction("Подробнее об этапе", "stage-detail") { go("c-stage") }
                                SecondaryAction("Посмотреть демо", "view-demo") { go("c-demo") }
                                SecondaryAction("Стоимость и расходы", "cost") { go("c-cost") }
                            }
                        }
                        "c-stage" -> {
                            Heading(stageNames[project.currentStage])
                            InfoCard("Статус этапа", project.stages[project.currentStage].label, true)
                            if (project.id == 1 && project.currentStage == 2) {
                                Detail("Что сделано · пример", "Главная, каталог, мобильная версия")
                                Detail("Что сейчас делаем · пример", "Форму заявки")
                                Detail("Что ждём от клиента · пример", "Фотографии работ")
                            } else Detail("Следующее действие", "Обсудить результат этого этапа. Подробные задачи в первой сборке не редактируются.")
                            PrimaryAction("Открыть демо", "stage-demo") { go("c-demo") }
                        }
                        "c-demo" -> {
                            Heading(if (project.demoAvailable) "Демо готово" else "Демо ещё нет")
                            InfoCard(project.name, if (project.demoAvailable) "Локальный макет мобильной версии. Можно оставить демонстрационное решение." else "У нового проекта нет готовой версии. Посмотрите подготовленный пример автосервиса.", true)
                            if (project.demoAvailable) {
                                PrimaryAction("Открыть демо ↗", "open-demo") { go("c-demo-site") }
                                Detail("Ваше решение", project.demoDecision)
                                PrimaryAction("Одобрить · локально", "approve-demo", !vm.busy) {
                                    vm.act { it.decideDemo(role, project.id, true); go("c-demo-result") }
                                }
                                SecondaryAction("Есть правки", "demo-feedback") { go("c-feedback") }
                            } else SecondaryAction("Открыть подготовленный пример", "sample-demo") { selectedProjectId = 1; go("c-demo-site") }
                            Note("Без открытия браузера и внешних ссылок. Согласование сохраняется только в демоданных.")
                        }
                        "c-demo-site" -> {
                            Eyebrow("Локальный макет · вымышленный автосервис")
                            Heading("Ваш автомобиль.\nВ надёжных руках.")
                            InfoCard("АВТО / СЕРВИС", "Диагностика, обслуживание и ремонт с понятными этапами работы.", true)
                            listOf("Диагностика", "Техническое обслуживание", "Ремонт").forEach { InfoCard(it, "Пример раздела будущего сайта") }
                            Note("Это нативный макет демо внутри приложения, не работающий сайт. Запись на ремонт не выполняется.")
                            PrimaryAction("Вернуться к согласованию", "demo-return") { back() }
                        }
                        "c-feedback" -> {
                            Heading("Что поправить?")
                            Input("Комментарий к демо", feedback, "feedback", 2000) { feedback = it }
                            PrimaryAction("Сохранить правки локально", "save-feedback", feedback.isNotBlank() && !vm.busy) {
                                vm.act { it.decideDemo(role, project.id, false, feedback); go("c-demo-result") }
                            }
                        }
                        "c-demo-result", "o-demo-review" -> {
                            Heading("Решение по демо")
                            InfoCard(project.demoDecision, project.demoFeedback.ifBlank { "Без комментария" }, true)
                            Note("Общее локальное состояние двух ролей. Реальное согласование и уведомление отсутствуют.")
                        }
                        "c-cost" -> {
                            Heading("Стоимость")
                            Detail("Разработка", project.agreedPrice?.let { money(it) } ?: "Не согласована")
                            Detail("Оплачено", project.paid?.let { money(it) } ?: "Нет оплаты")
                            Detail("Остаток", if (project.agreedPrice != null && project.paid != null) money(project.agreedPrice - project.paid) else "Не определён")
                            HorizontalDivider()
                            Detail("Сторонние расходы", if (project.id == 1) "Домен — 199 ₽/год · пример" else "Не определены")
                            Detail("Ежемесячно", if (project.id == 1) "Хостинг — 500 ₽/мес. · пример" else "Не определено")
                            Detail("Платные API", if (project.id == 1) "Нет" else "Не определено")
                            Note("Демонстрационные суммы. Платежи в приложении недоступны.")
                        }
                        "o-home" -> {
                            Eyebrow("Рабочий день · демоданные")
                            Heading("Всё под\nконтролем")
                            InfoCard("${data.leads.count { it.status == LeadStatus.NEW }} · новые заявки", "${data.projects.size} демопроекта в общем хранилище", true)
                            PrimaryAction("Открыть заявки", "owner-leads") { go("o-leads") }
                            Eyebrow("Последняя заявка")
                            val latest = data.leads.last()
                            ActionRow("Алексей Смирнов · №${latest.id}", "${latest.brief.type} · ${latest.brief.budget.label}", Icons.Outlined.PersonOutline, "latest-lead") {
                                selectedLeadId = latest.id; go("o-lead")
                            }
                            ActionRow("Проекты", "Этапы и договорённости", Icons.Outlined.WorkOutline, "owner-projects") { go("o-projects") }
                            Note("Показатели рассчитаны из локальных демоданных, это не CRM-аналитика.")
                        }
                        "o-leads" -> {
                            Heading("Заявки")
                            Picker("Фильтр", leadFilter, listOf("Все", "Новая", "В работе", "Ожидание", "Закрыта", "Проект создан"), "lead-filter") {
                                leadFilter = listOf("Все", "Новая", "В работе", "Ожидание", "Закрыта", "Проект создан")[it]
                            }
                            val visible = data.leads.reversed().filter { leadFilter == "Все" || it.status.label == leadFilter }
                            if (visible.isEmpty()) InfoCard("Заявок пока нет", "Выберите другой фильтр или создайте локальную заявку в роли клиента.")
                            visible.forEach { l ->
                                ActionRow("Алексей Смирнов · №${l.id}", "${l.brief.type} · ${l.brief.budget.label}\n${l.status.label} · демоприложение", Icons.Outlined.Inbox, "lead-${l.id}") {
                                    selectedLeadId = l.id; go("o-lead")
                                }
                            }
                        }
                        "o-lead", "o-brief-full" -> {
                            Eyebrow("Локальная заявка №${lead.id}")
                            Heading("Алексей Смирнов")
                            Note("Вымышленный клиент · реальный контакт не собирается")
                            BriefDetails(lead.brief)
                            Detail("Статус", lead.status.label)
                            val existing = data.projects.find { it.leadId == lead.id }
                            PrimaryAction(if (existing == null) "Создать демопроект" else "Открыть демопроект", "convert-lead", !vm.busy) {
                                vm.act { selectedProjectId = it.createProject(role, lead.id); go("o-project") }
                            }
                            if (existing == null) SecondaryAction("Изменить статус", "lead-status") { go("o-lead-status") }
                            Note("Цена и срок нового проекта останутся несогласованными.")
                        }
                        "o-lead-status" -> {
                            Heading("Статус заявки")
                            LeadStatus.entries.filter { it != LeadStatus.CONVERTED }.forEach { value ->
                                PrimaryAction(value.label, "lead-status-${value.name}", !vm.busy) {
                                    vm.act { it.changeLeadStatus(role, lead.id, value); back() }
                                }
                            }
                        }
                        "o-projects" -> {
                            Heading("Проекты")
                            data.projects.reversed().forEach { p ->
                                ActionRow(p.name, "Алексей Смирнов\n${stageNames[p.currentStage]} · ${p.stages[p.currentStage].label}", Icons.Outlined.WorkOutline, "owner-project-${p.id}") {
                                    selectedProjectId = p.id; go("o-project")
                                }
                            }
                        }
                        "o-stages" -> {
                            Heading("Этапы проекта")
                            Note(project.name)
                            Picker("Текущий этап", stageNames[selectedStage], stageNames, "stage-picker") {
                                selectedStage = it; stageChoice = project.stages[it].name
                            }
                            Picker("Статус этапа", StageStatus.valueOf(stageChoice).label, StageStatus.entries.map { it.label }, "status-picker") {
                                stageChoice = StageStatus.entries[it].name
                            }
                            PrimaryAction("Сохранить этап локально", "save-stage", !vm.busy) {
                                vm.act { it.changeStage(role, project.id, selectedStage, StageStatus.valueOf(stageChoice)); back() }
                            }
                            StageSummary(project)
                            Note("Сохранение сразу видно клиенту. Изменение статуса не создаёт вымышленную стоимость или готовое демо.")
                        }
                        "c-chat", "o-chat" -> {
                            Heading("Сообщения")
                            Note("${project.name} · локальный проектный чат. Ничего не отправляется собеседнику или в MAX/VK.")
                            project.messages.forEach { msg ->
                                InfoCard(if (msg.role == DemoRole.CLIENT) "Алексей · демо" else "MITIN DEV · демо", msg.text, msg.role == role)
                            }
                            Input("Сообщение", message, "message", 1000) { message = it }
                            PrimaryAction("Добавить локально", "save-message", message.isNotBlank() && !vm.busy) {
                                vm.act { it.addMessage(role, project.id, message); message = ""; keyboard?.hide() }
                            }
                        }
                        "c-materials", "o-materials" -> {
                            Heading("Файлы проекта")
                            if (project.id == 1) {
                                Detail("Логотип", "✓ · пример")
                                Detail("Фотографии", "12 файлов · только пример списка")
                                Detail("Тексты", "✓ · пример")
                            }
                            InfoCard("Загрузка ещё не реализована", "Файлы не выбираются и не загружаются. Реальных материалов в сборке нет.")
                        }
                        "c-support" -> {
                            Heading("Поддержка")
                            InfoCard("Следующая версия", "Создание обращений и сопровождение пока не подключены. Сообщение о проблеме не отправляется.")
                            SecondaryAction("Новый проект · демо", "support-new") { go("c-type") }
                        }
                        "c-profile", "o-more" -> {
                            Heading(if (role == DemoRole.CLIENT) "Профиль" else "Ещё")
                            InfoCard(if (role == DemoRole.CLIENT) "Алексей Смирнов" else "Владелец MITIN DEV", "Вымышленная роль без входа. Версия ${BuildConfig.VERSION_NAME}", true)
                            if (role == DemoRole.OWNER) ActionRow("Сообщения проекта", "Локальный диалог", Icons.AutoMirrored.Outlined.Chat, "owner-chat") { go("o-chat") }
                            SecondaryAction("Выбрать демороль", "profile-demo") { go("demo") }
                            SecondaryAction("Данные и доступ", "privacy") { go("c-privacy") }
                            SecondaryAction("Сбросить демоданные", "reset") { showReset = true }
                        }
                        "c-privacy" -> {
                            Heading("Данные и доступ")
                            InfoCard("Полностью офлайн", "Нет регистрации, паролей, токенов, аналитики и сетевого доступа. Вводите только вымышленные данные.")
                            Detail("Хранение", "Демоданные находятся в закрытом хранилище приложения. Резервное копирование отключено. Сбросьте демо или очистите данные приложения, чтобы удалить ввод.")
                            Detail("Дальше", "До настоящего входа понадобятся отдельная интеграция с MITIN DEV API и проверка прав на backend. Выбор роли здесь не является механизмом авторизации.")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    if (showReset) AlertDialog(onDismissRequest = { showReset = false }, title = { Text("Сбросить демо?") },
        text = { Text("Локальные заявки, проекты, решения и сообщения будут заменены исходным вымышленным примером.") },
        confirmButton = { TextButton(onClick = {
            vm.act {
                it.reset()
                selectedProjectId = 1; selectedLeadId = 1045
                type = "Сайт"; task = Brief().task; features = Brief().features
                budgetName = BudgetRange.FROM_20_40.name; deadline = Brief().deadline
                integrations = Brief().integrations; comments = ""; feedback = ""; message = ""
                leadFilter = "Все"; showReset = false; root("c-welcome"); roleName = DemoRole.CLIENT.name
            }
        }) { Text("Сбросить") } }, dismissButton = { TextButton(onClick = { showReset = false }) { Text("Отмена") } })
    vm.error?.let { error -> AlertDialog(onDismissRequest = { vm.dismissError() }, title = { Text("Локальное хранилище") },
        text = { Text(error) }, confirmButton = { TextButton(onClick = { vm.dismissError() }) { Text("Понятно") } }) }
}

@Composable private fun Input(label: String, value: String, tag: String, limit: Int, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= limit) change(it) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        maxLines = 6, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        supportingText = { Text("${value.length} / $limit") })
}

@Composable private fun Picker(label: String, selected: String, choices: List<String>, tag: String, choose: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), contentPadding = PaddingValues(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(selected, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Outlined.ExpandMore, null)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            choices.forEachIndexed { index, choice ->
                TextButton(onClick = { choose(index); open = false }, modifier = Modifier.fillMaxWidth().testTag("$tag-option-$index")) {
                    Text(choice, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } }, confirmButton = { TextButton(onClick = { open = false }) { Text("Отмена") } })
}

@Composable private fun BriefDetails(brief: Brief) {
    Detail("Тип проекта", brief.type)
    Detail("Задача", brief.task)
    Detail("Основные функции", brief.features)
    Detail("Бюджет · не цена договора", brief.budget.label, "brief-budget")
    Detail("Желаемые сроки", brief.deadline)
    Detail("Интеграции", brief.integrations)
    Detail("Комментарии", brief.comments)
}

@Composable private fun StageSummary(project: DemoProject) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        project.stages.forEachIndexed { index, status ->
            Row(Modifier.fillMaxWidth().testTag("stage-summary-$index"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(status.symbol, color = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stageNames[index], style = MaterialTheme.typography.titleSmall)
                    Text(status.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index == project.currentStage) Text("Сейчас", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun money(value: Int): String = "%,d ₽".format(java.util.Locale.forLanguageTag("ru-RU"), value)
