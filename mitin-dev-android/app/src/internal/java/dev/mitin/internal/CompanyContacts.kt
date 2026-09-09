package dev.mitin.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.mitin.demo.SecondaryAction

@Composable internal fun CompanyContacts() {
    val uri = LocalUriHandler.current
    var unavailable by remember { mutableStateOf(false) }
    Column(Modifier.testTag("company-contacts"), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Связаться с Владимиром", style=MaterialTheme.typography.titleLarge)
        Text("Владимир Митин · MITIN DEV")
        for((id,label,url) in listOf(
            Triple("phone", "+7 (950) 350-11-90", "tel:+79503501190"),
            Triple("email", "inbox@24promtbot.ru", "mailto:inbox@24promtbot.ru"),
            Triple("max", "Написать в MAX", "https://max.ru/u/f9LHodD0cOIq_cVn9ayrUFYyokrrK-V9mkxyH4OPCSKbb_GuJzOQh4-fUQQ"),
            Triple("vk", "Написать в VK", "https://vk.ru/id824748307"),
        )) SecondaryAction(label,"company-contact-$id") { unavailable=runCatching { uri.openUri(url) }.isFailure }
        if(unavailable) Text("Не найдено приложение для открытия ссылки. Контакты указаны выше.",color=MaterialTheme.colorScheme.error)
    }
}
