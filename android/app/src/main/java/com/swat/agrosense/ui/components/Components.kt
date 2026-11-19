package com.swat.agrosense.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swat.agrosense.ui.theme.Green
import com.swat.agrosense.ui.theme.GreenSoft
import com.swat.agrosense.ui.theme.Line
import com.swat.agrosense.ui.theme.Muted
import com.swat.agrosense.ui.theme.Rgb
import com.swat.agrosense.ui.theme.Warn

/** Botón principal con relleno degradado RGB y un borde arcoíris animado. */
@Composable
fun RgbButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: Pair<Color, Color> = Rgb.Green,
    icon: ImageVector? = null,
    emoji: String? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    onClick: () -> Unit
) {
    val t = rememberInfiniteTransition(label = "rgb")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "angle")
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .heightIn(min = height)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .drawBehind {
                // borde arcoíris girando
                rotate(angle) {
                    drawCircle(Brush.sweepGradient(Rgb.spectrum), radius = size.maxDimension, center = center)
                }
            }
            .padding(3.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(colors.first, colors.second), start = Offset.Zero, end = Offset.Infinite))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (emoji != null) { Text(emoji, fontSize = 22.sp); Spacer(Modifier.width(8.dp)) }
            if (icon != null) { Icon(icon, null, tint = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Mosaico grande de la pantalla de inicio. */
@Composable
fun BigTile(emoji: String, title: String, subtitle: String, colors: Pair<Color, Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "tile")
    val glow by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "glow")
    Box(
        modifier
            .heightIn(min = 128.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(colors.first, colors.second)))
            .border(2.dp, Brush.sweepGradient(Rgb.spectrum.map { it.copy(alpha = glow) }), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        }
    }
}

@Composable
fun SectionCard(title: String? = null, emoji: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (emoji != null) { Text(emoji, fontSize = 20.sp); Spacer(Modifier.width(8.dp)) }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

/** Caja de ayuda: explica qué hacer en cada paso, para usar la app sin manual. */
@Composable
fun HelpBox(text: String, color: Color = Green, background: Color = GreenSoft) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(background).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = color)
    }
}

@Composable
fun WarnBox(text: String) = HelpBox(text, Warn, Color(0xFFFFF8E8))

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, suffix: String? = null, help: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.replace(',', '.').filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        supportingText = help?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun TextFieldX(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine,
        minLines = if (singleLine) 1 else 3, keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, display: (String) -> String = { it }) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = display(selected), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(display(o)) }, onClick = { onSelect(o); expanded = false }) }
        }
    }
}

/** Opción seleccionable grande (cultivo, categoría, equipo). */
@Composable
fun ChoiceCard(emoji: String, title: String, subtitle: String? = null, selected: Boolean, accent: Color = Green, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.White)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) accent else Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(emoji, fontSize = 30.sp)
            Text(title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier, color: Color = Green) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(12.dp)
    ) {
        Text("$emoji  $label", fontSize = 12.sp, color = Muted)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

/** Indicador de pasos del asistente: 1 ● 2 ● 3 ... */
@Composable
fun StepIndicator(current: Int, labels: List<String>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { i, l ->
            val done = i < current; val now = i == current
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape)
                        .background(if (now) Brush.linearGradient(Rgb.Blue.toList()) else if (done) Brush.linearGradient(Rgb.Green.toList()) else Brush.linearGradient(listOf(Line, Line))),
                    contentAlignment = Alignment.Center
                ) { Text(if (done) "✓" else "${i + 1}", color = if (done || now) Color.White else Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Text(l, fontSize = 10.sp, color = if (now) Green else Muted, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}
