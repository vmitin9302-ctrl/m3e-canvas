package dev.mitin.internal

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mitin.demo.*
import kotlinx.coroutines.*

class InternalApplication : Application() {
    var capabilities by mutableStateOf<Capabilities?>(if (BuildConfig.FLAVOR == "production") null else Capabilities(true, true, true))
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val manager: SessionManager? by lazy {
        if (BuildConfig.API_BASE_URL.isBlank() || BuildConfig.FLAVOR == "production") null else SessionManager(
            HttpAuthApi(BuildConfig.API_BASE_URL), KeystoreRefreshStore(this), authScope
        )
    }
    override fun onCreate() {
        super.onCreate()
        // Does not connect when the build has no explicit test configuration.
        manager?.let { authScope.launch { it.restore() } }
    }
}
class InternalViewModel(app: Application) : AndroidViewModel(app) {
    val manager = (app as InternalApplication).manager
    private val repository = manager?.let { AuthRepository(it) }
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var page by mutableStateOf(SessionPage(emptyList(), null)); private set
    private var profileId: String? = null
    init {
        manager?.let { auth -> viewModelScope.launch {
            auth.state.collect { state ->
                if (profileId != state.profile?.userId) {
                    profileId = state.profile?.userId; page = SessionPage(emptyList(), null); error = null
                }
            }
        } }
    }
    private fun run(action: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true; error = null
            try { action() }
            catch (_: CancellationException) { throw CancellationException() }
            catch (_: Superseded) { }
            catch (_: SignedOut) { error = "Войдите снова, чтобы продолжить." }
            catch (failure: AuthFailure) { error = when (failure.status) {
                401 -> "Вход не выполнен. Проверьте данные или войдите заново."
                403 -> "Это действие недоступно."
                404 -> "Сессия недоступна."
                429 -> "Слишком много запросов. Подождите перед следующей попыткой."
                422 -> "Проверьте формат введённых данных."
                else -> "Тестовый сервис временно недоступен. Проверьте подключение."
            } }
            catch (_: Exception) { error = "Не удалось безопасно завершить операцию. Войдите заново." }
            finally { busy = false }
        }
    }
    fun login(email: String, password: Secret) = run { repository?.login(email, password) }
    fun profile() = run { repository?.profile() }
    fun sessions(more: Boolean = false) {
        val owner = profileId
        run {
            val result = repository?.page(if (more) page.nextOffset ?: return@run else 0) ?: return@run
            if (owner == profileId && owner != null) page = if (more) result.copy(items = (page.items + result.items).distinctBy { it.id }) else result
        }
    }
    fun revoke(item: RemoteSession) = run { repository?.revoke(item); if (!item.current) sessions() }
    fun logout(all: Boolean = false) = run { repository?.logout(all) }
}
class InternalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen(); super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider -> provider.view.animate().alpha(0f).setDuration(220).withEndAction { provider.remove() }.start() }
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { MitinTheme { InternalApp() } }
    }
}
@Composable fun InternalApp(vm: InternalViewModel = viewModel()) {
    val meta: MetaViewModel = viewModel()
    if (meta.loading) { LaunchScreen(); return }
    if (meta.failed) { LaunchScreen(failed = true, retry = meta::reload); return }
    val portfolio: PortfolioViewModel = viewModel()
    val brief: BriefViewModel = viewModel()
    val configured = vm.manager != null
    val auth = if (configured) vm.manager!!.state.collectAsStateWithLifecycle().value else AuthState(restoring = false)
    var tab by rememberSaveable { mutableIntStateOf(if(BuildConfig.FLAVOR == "production") 0 else 2) }
    var showSessions by remember(auth.profile?.userId) { mutableStateOf(false) }
    var confirmAll by remember(auth.profile?.userId) { mutableStateOf(false) }
    var selected by remember(auth.profile?.userId) { mutableStateOf<RemoteSession?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = !imeVisible && (showSessions || tab != 2)) { showSessions = false; tab = 2; keyboard?.hide() }
    if (auth.restoring) { LaunchScreen(); return }
    Scaffold(Modifier.fillMaxSize().imePadding(), topBar = {
        if (!imeVisible) Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandEmblem(36.dp)
                Column(Modifier.weight(1f)) { Text("MITIN DEV", style = MaterialTheme.typography.titleMedium); Text(if(BuildConfig.FLAVOR == "production") "ПРОЕКТЫ И РАЗРАБОТКА" else "ТЕСТОВОЕ ОКРУЖЕНИЕ", style = MaterialTheme.typography.labelSmall, color = NeonBlue) }
                if (showSessions) IconButton(onClick = { showSessions = false }, modifier = Modifier.testTag("sessions-back")) { Icon(Icons.Outlined.Close, "Закрыть сессии") }
            }
        }
    }, bottomBar = {
        if (!imeVisible) InternalNavigationBar(tab) { tab = it; showSessions = false; keyboard?.hide() }
    }) { padding ->
        Box(Modifier.fillMaxSize().background(BrandBackground).padding(padding), contentAlignment = Alignment.TopCenter) {
            key(auth.profile?.userId, tab, showSessions) {
                if (tab == 1) BriefScreen(brief)
                else Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when {
                        tab == 0 -> PortfolioScreen(portfolio) { brief.fromPortfolio(portfolio.selectedSlug, (portfolio.detail as? PortfolioState.Success<PortfolioItem>)?.value?.title); tab = 1 }
                        tab == 1 -> BriefScreen(brief)
                        BuildConfig.FLAVOR == "production" -> PublicCabinet { tab = 1 }
                        !configured -> { BrandHero("МОЙ КАБИНЕТ"); InfoCard("Тестовый сервер не настроен", "Для этой сборки не задан тестовый API. Подключение не выполняется.", true) }
                        auth.profile == null -> {
                            BrandHero("МОЙ КАБИНЕТ")
                            Heading("Добро пожаловать")
                            Note("Вход только для вымышленных тестовых клиентов. Профиль и доступ проверяет сервер.")
                            auth.message?.let { InfoCard("Состояние доступа", it) }
                            var email by remember { mutableStateOf("") }
                            // Deliberately NOT rememberSaveable or SavedStateHandle. Cleared at submit/disposal.
                            var password by remember { mutableStateOf("") }
                            DisposableEffect(Unit) { onDispose { password = ""; email = "" } }
                            OutlinedTextField(value = email, onValueChange = { if (it.length <= 254) email = it }, label = { Text("Email") },
                                shape = RoundedCornerShape(20.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().testTag("login-email"))
                            OutlinedTextField(value = password, onValueChange = { if (it.codePointCount(0, it.length) <= 128) password = it }, label = { Text("Пароль") },
                                shape = RoundedCornerShape(20.dp), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("login-password"))
                            PrimaryAction(if (vm.busy) "Проверяем…" else "Войти", "network-login", !vm.busy && email.isNotBlank() && password.codePointCount(0, password.length) in 15..128) {
                                val transient = Secret(password); password = ""; keyboard?.hide(); vm.login(email, transient)
                            }
                            Note("Роль владельца пока недоступна: вход с MFA ещё не подключён.")
                        }
                        showSessions -> {
                            Eyebrow("Только ваши устройства")
                            Heading("Мои сессии")
                            Note("Список получен с тестового сервера. Отзыв закрывает доступ выбранной сессии.")
                            vm.page.items.forEachIndexed { index, session ->
                                InfoCard(if (session.current) "Это устройство" else "Другая сессия", "Создана: ${session.createdAt}\nДействует до: ${session.idleExpiresAt}" +
                                    if (session.revokedAt != null) "\nОтозвана: ${session.revokedAt}" else "", session.current, "session-$index")
                                if (session.revokedAt == null) SecondaryAction("Отозвать сессию", "revoke-$index") { selected = session }
                            }
                            if (vm.page.items.isEmpty() && !vm.busy) Note("Список пока не получен. Нажмите «Обновить».")
                            SecondaryAction("Обновить", "sessions-reload") { vm.sessions() }
                            if (vm.page.nextOffset != null) SecondaryAction("Ещё сессии", "sessions-more") { vm.sessions(true) }
                            SecondaryAction("Выйти со всех устройств", "logout-all") { confirmAll = true }
                        }
                        else -> {
                            BrandHero("МОЙ КАБИНЕТ")
                            val profile = auth.profile!!
                            InfoCard(profile.displayName, "Клиент · профиль подтверждён ответом тестового сервера", true, "server-profile")
                            Detail("ID профиля", profile.clientProfileId)
                            ActionRow("Мои сессии", "Устройства и управление доступом", Icons.Outlined.Devices, "open-sessions") { showSessions = true; vm.sessions() }
                            SecondaryAction("Обновить профиль", "profile-reload") { vm.profile() }
                            SecondaryAction("Выйти", "network-logout") { vm.logout() }
                            InfoCard("Мои проекты", "Проекты появятся здесь после подключения кабинета")
                        }
                    }
                    if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("network-error")) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    if (confirmAll) AlertDialog(onDismissRequest = { confirmAll = false }, title = { Text("Выйти на всех устройствах?") }, text = { Text("Сервер закроет все ваши сессии. При ошибке сети отзыв нельзя будет подтвердить.") },
        confirmButton = { TextButton(onClick = { confirmAll = false; vm.logout(true) }, modifier = Modifier.testTag("confirm-logout-all")) { Text("Выйти везде") } }, dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Отмена") } })
    selected?.let { item -> AlertDialog(onDismissRequest = { selected = null }, title = { Text("Отозвать сессию?") }, text = { Text(if (item.current) "Это устройство потеряет доступ." else "Выбранное устройство потеряет доступ.") },
        confirmButton = { TextButton(onClick = { selected = null; vm.revoke(item) }, modifier = Modifier.testTag("confirm-revoke")) { Text("Отозвать") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text("Отмена") } }) }
}

@Composable private fun InternalNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    val destinations = listOf("Готовые проекты" to Icons.Outlined.WorkOutline,
        "Обсудить с AI" to Icons.Outlined.AutoAwesome, "Мой кабинет" to Icons.Outlined.PersonOutline)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center)
    // Consume horizontal safe insets before measuring; NavigationBar retains its
    // bottom system inset, outside the items' full-size touch targets.
    BoxWithConstraints(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
        // Material NavigationBar has horizontal content padding. Reserve that
        // space plus breathing room inside each of the three equal-width items.
        val labelWidth = (maxWidth / destinations.size - 12.dp).coerceAtLeast(1.dp)
        val availablePixels = with(density) { labelWidth.roundToPx() }
        val useShortLabels = destinations.any { (label, _) ->
            label.split(' ').any { word ->
                measurer.measure(AnnotatedString(word), style = labelStyle, softWrap = false).size.width > availablePixels
            }
        }
        val labels = listOf("Кейсы", "AI-бриф", "Кабинет")
        val textHeight = with(density) {
            labels.maxOf { label ->
                measurer.measure(AnnotatedString(label), style = labelStyle,
                    constraints = Constraints(maxWidth = labelWidth.roundToPx())).size.height
            }.toDp()
        }
        val itemHeight = maxOf(80.dp, textHeight + 56.dp)
        NavigationBar(modifier = Modifier.testTag("internal-navigation-bar")
            .border(androidx.compose.foundation.BorderStroke(0.5.dp, NeonEdge)),
            windowInsets = NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)) {
            destinations.forEachIndexed { index, (label, icon) ->
                NavigationBarItem(selected = selected == index, onClick = { onSelect(index) },
                    icon = { Icon(icon, null) },
                    label = { Text(labels[index], style = labelStyle,
                        modifier = Modifier.width(labelWidth).height(textHeight).testTag("internal-nav-label-$index")) },
                    modifier = Modifier.testTag("internal-nav-$index").heightIn(min = itemHeight)
                        .semantics { contentDescription = label })
            }
        }
    }
}
