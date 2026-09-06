package dev.mitin.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NeonViolet = Color(0xFFB59BFF)
val NeonBlue = Color(0xFF5EDCFF)
val NeonEdge = Brush.linearGradient(listOf(NeonViolet.copy(alpha = .6f), NeonBlue.copy(alpha = .15f), NeonViolet.copy(alpha = .25f)))
val BrandBackground = Brush.verticalGradient(listOf(Color(0xFF111424), Color(0xFF090C13), Color(0xFF0C111B)))

@Composable fun BrandEmblem(size: Dp = 56.dp) {
    Box(Modifier.size(size).background(Color(0xFF121929), RoundedCornerShape(size / 3))
        .border(1.dp, NeonEdge, RoundedCornerShape(size / 3)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(size / 5)) {
            val path = Path().apply {
                moveTo(0f, this@Canvas.size.height * .85f)
                lineTo(0f, this@Canvas.size.height * .15f)
                lineTo(this@Canvas.size.width * .5f, this@Canvas.size.height * .65f)
                lineTo(this@Canvas.size.width, this@Canvas.size.height * .15f)
                lineTo(this@Canvas.size.width, this@Canvas.size.height * .85f)
            }
            drawPath(path, NeonBlue.copy(alpha = .07f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, NeonViolet.copy(alpha = .17f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Brush.linearGradient(listOf(NeonViolet, NeonBlue)), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
@Composable fun BrandHero(subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BrandEmblem(88.dp)
        Text("MITIN DEV", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text(subtitle, style = MaterialTheme.typography.labelLarge, color = NeonBlue)
        Box(Modifier.width(64.dp).height(2.dp).background(NeonEdge))
    }
}
@Composable fun BrandLoading() {
    Column(Modifier.fillMaxSize().background(BrandBackground).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        BrandHero("ТЕСТОВЫЙ КОНТУР")
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
        Spacer(Modifier.height(16.dp))
        Note("Проверяем сессию на сервере…")
    }
}
