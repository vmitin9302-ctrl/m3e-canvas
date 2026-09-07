package dev.mitin.internal

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mitin.demo.*

@Composable fun LaunchScreen(failed: Boolean = false, retry: () -> Unit = {}) {
    val transition = rememberInfiniteTransition(label = "launch")
    val glow by transition.animateFloat(.35f, .7f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "glow")
    Box(Modifier.fillMaxSize().background(Color(0xFF090C13)).testTag("launch-screen")) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF7561DF).copy(alpha = glow * .25f), Color.Transparent),
                center = Offset(size.width * .5f, size.height * .4f), radius = size.width * .8f), radius = size.width * .8f,
                center = Offset(size.width * .5f, size.height * .4f))
            val step = 36.dp.toPx()
            for (x in 0..(size.width / step).toInt()) for (y in 0..(size.height / step).toInt())
                drawCircle(Color.White.copy(alpha = .055f), 1.dp.toPx(), Offset(x * step, y * step))
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            BrandEmblem(100.dp)
            Spacer(Modifier.height(28.dp))
            Text("MITIN DEV", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            Text("От идеи — к работающему продукту", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            if (failed) {
                Text("Не удалось подключиться", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Проверьте интернет и попробуйте ещё раз.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                PrimaryAction("Повторить", "startup-retry", action = retry)
            } else {
                LinearProgressIndicator(Modifier.width(120.dp).height(3.dp), color = NeonBlue,
                    trackColor = Color.White.copy(alpha = .07f))
                Spacer(Modifier.height(16.dp))
                Text("Подключаем возможности", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("САЙТЫ  /  AI  /  DIGITAL", Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp),
            style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = NeonBlue.copy(alpha = .7f))
    }
}

@Composable fun SectionIntro(kicker: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(6.dp).background(NeonBlue, RoundedCornerShape(3.dp)))
            Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = NeonBlue, letterSpacing = 2.sp)
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun PublicCabinet(onBrief: () -> Unit) {
    SectionIntro("Личное пространство", "Ваш следующий проект", "Начните с идеи. Вместе превратим её в понятный план разработки.")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, NeonEdge)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(32.dp), tint = NeonBlue)
            Text("Сначала — хороший бриф", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("AI поможет выделить главное: задачу, функции, бюджет и первый этап.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryAction("Обсудить идею", "cabinet-brief", action = onBrief)
        }
    }
    InfoCard("Кабинет ещё не открыт", "Вход, рабочие проекты и сообщения появятся позже. Публичные кейсы и AI-бриф уже доступны без аккаунта.", tag = "production-auth-unavailable")
}

@Composable fun ChatBubble(message: BriefMessage) {
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if(user) Arrangement.End else Arrangement.Start) {
        Surface(Modifier.fillMaxWidth(if(user) .94f else 1f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = if(user) 22.dp else 5.dp, bottomEnd = if(user) 5.dp else 22.dp),
            color = if(user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, if(user) NeonViolet.copy(alpha = .25f) else Color.White.copy(alpha = .06f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if(user) "ВЫ" else "MITIN AI", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp,
                    color = if(user) NeonViolet else NeonBlue)
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
