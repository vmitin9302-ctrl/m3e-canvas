package dev.mitin.internal

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.mitin.demo.*
import kotlinx.coroutines.*

@Composable fun PortfolioScreen(vm: PortfolioViewModel, discuss: () -> Unit) {
    DisposableEffect(vm) { vm.enter(); onDispose { vm.leave() } }
    BackHandler(vm.selectedSlug != null) { vm.back() }
    Heading("Готовые проекты")
    Note("Реальные проекты MITIN DEV")
    if (vm.selectedSlug == null) {
        SecondaryAction("Обновить", "portfolio-refresh") { vm.refresh() }
        when (val state = vm.list) {
            PortfolioState.Loading -> PortfolioLoading()
            PortfolioState.Empty -> InfoCard("Пока нет проектов", "Новые кейсы появятся здесь после публикации.", tag = "portfolio-empty")
            is PortfolioState.Error -> PortfolioError(state.message) { vm.refresh() }
            is PortfolioState.Success -> state.value.forEach { item ->
                Card(Modifier.fillMaxWidth().testTag("portfolio-${item.slug}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PortfolioCover(item.coverImageUrl, item.title)
                        Text(item.title, style = MaterialTheme.typography.titleLarge)
                        Text(item.shortDescription)
                        item.category?.let { Note(it) }
                        if (item.tags.isNotEmpty()) Note(item.tags.joinToString(" · "))
                        PrimaryAction("Подробнее", "portfolio-open-${item.slug}") { vm.open(item.slug) }
                    }
                }
            }
        }
    } else {
        SecondaryAction("Назад к проектам", "portfolio-back") { vm.back() }
        when (val state = vm.detail) {
            PortfolioState.Loading -> PortfolioLoading()
            PortfolioState.Empty -> Unit
            is PortfolioState.Error -> PortfolioError(state.message) { vm.selectedSlug?.let(vm::open) }
            is PortfolioState.Success -> PortfolioDetail(state.value, discuss)
        }
    }
}

@Composable private fun PortfolioLoading() {
    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("portfolio-loading"))
    Note("Загружаем проекты…")
}
@Composable private fun PortfolioError(message: String, retry: () -> Unit) {
    InfoCard("Каталог недоступен", message, tag = "portfolio-error")
    PrimaryAction("Повторить", "portfolio-retry", action = retry)
}
@Composable private fun PortfolioDetail(item: PortfolioItem, discuss: () -> Unit) {
    val context = LocalContext.current
    var linkError by remember(item.slug) { mutableStateOf(false) }
    PortfolioCover(item.coverImageUrl, item.title)
    Text(item.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("portfolio-detail-title"))
    Text(item.description, modifier = Modifier.testTag("portfolio-detail-description"))
    item.category?.let { Note(it) }
    if (item.tags.isNotEmpty()) { Heading("Технологии и направления"); Note(item.tags.joinToString(" · ")) }
    safePortfolioUrl(item.projectUrl)?.let { url ->
        PrimaryAction("Открыть сайт", "portfolio-site") {
            linkError = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) }.isFailure
        }
    }
    if (linkError) Note("Не удалось открыть браузер на устройстве.")
    SecondaryAction("Обсудить похожий проект", "portfolio-discuss", action = discuss)
}

/** Composition-owned job: cancelled when the card leaves the screen. No Activity retained. */
@Composable fun PortfolioCover(url: String?, title: String) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(url) { mutableStateOf(url != null) }
    LaunchedEffect(url) {
        loading = url != null
        try {
            if (url != null && BuildConfig.API_BASE_URL.isNotBlank()) {
                val data = HttpPortfolioRepository(BuildConfig.API_BASE_URL).image(url)
                bitmap = withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                    if (bounds.outWidth !in 1..8192 || bounds.outHeight !in 1..8192) throw PortfolioFailure()
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        while (bounds.outWidth / inSampleSize > 1440 || bounds.outHeight / inSampleSize > 1440) inSampleSize *= 2
                    }
                    BitmapFactory.decodeByteArray(data, 0, data.size, options)?.asImageBitmap() ?: throw PortfolioFailure()
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { bitmap = null }
        finally { loading = false }
    }
    val loaded = bitmap
    if (loaded != null) Image(loaded, "Обложка: $title", Modifier.fillMaxWidth().height(200.dp).testTag("portfolio-image"), contentScale = ContentScale.Crop)
    else Surface(Modifier.fillMaxWidth().heightIn(min = 100.dp).testTag("portfolio-image-placeholder")) {
        Text(if (loading) "Загружаем изображение…" else "Изображение недоступно", Modifier.padding(20.dp))
    }
}
