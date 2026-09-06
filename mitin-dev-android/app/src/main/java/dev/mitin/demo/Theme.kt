package dev.mitin.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFD2BCFC), onPrimary = Color(0xFF32226F),
    primaryContainer = Color(0xFF4C3889), onPrimaryContainer = Color(0xFFE9DDFF),
    secondaryContainer = Color(0xFF4B425D), onSecondaryContainer = Color(0xFFE9DDFD),
    tertiaryContainer = Color(0xFF6C3644), onTertiaryContainer = Color(0xFFFDDAE1),
    surface = Color(0xFF141317), background = Color(0xFF141317),
    surfaceContainerLow = Color(0xFF1C1B1F), surfaceContainer = Color(0xFF201F23),
    surfaceContainerHigh = Color(0xFF2B292D), surfaceContainerHighest = Color(0xFF363438),
    onSurface = Color(0xFFE4E1E7), onBackground = Color(0xFFE4E1E7),
    onSurfaceVariant = Color(0xFFC9C4D1), outline = Color(0xFF938F9B), outlineVariant = Color(0xFF494550),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable fun MitinTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = PurpleDark, content = content)
}

@Composable fun Eyebrow(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
}

@Composable fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
}

@Composable fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable fun InfoCard(title: String, body: String, accent: Boolean = false, tag: String = "") {
    Card(Modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (accent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium,
                color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun ActionRow(title: String, detail: String, icon: ImageVector, tag: String, click: () -> Unit) {
    Card(onClick = click, modifier = Modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(10.dp).size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}

@Composable fun PrimaryAction(label: String, tag: String, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable fun SecondaryAction(label: String, tag: String, action: () -> Unit) {
    FilledTonalButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) { Text(label) }
}

@Composable fun Detail(label: String, value: String, tag: String = "") {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag(tag), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Eyebrow(label)
        Text(value.ifBlank { "Не указано" }, style = MaterialTheme.typography.bodyLarge)
    }
}
