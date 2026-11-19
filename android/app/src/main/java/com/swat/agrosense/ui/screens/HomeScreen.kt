package com.swat.agrosense.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.swat.agrosense.AppViewModel
import com.swat.agrosense.R
import com.swat.agrosense.data.Catalog
import com.swat.agrosense.domain.Calculator
import com.swat.agrosense.goTab
import com.swat.agrosense.ui.components.BigTile
import com.swat.agrosense.ui.components.HelpBox
import com.swat.agrosense.ui.components.SectionCard
import com.swat.agrosense.ui.components.StatCard
import com.swat.agrosense.ui.theme.GreenDark
import com.swat.agrosense.ui.theme.Muted
import com.swat.agrosense.ui.theme.Rgb
import java.text.SimpleDateFormat
import java.util.Locale

val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
fun money(v: Double?) = if (v == null) "—" else "$" + Calculator.fmt(v, 2)

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val clients by vm.clients.collectAsStateWithLifecycle()
    val apps by vm.applications.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Encabezado
        Row(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(GreenDark, Color(0xFF218354), Color(0xFF2979FF))))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.logo_agrosense), "AgroSense", Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("AgroSense", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Cálculo y control de aplicaciones de plaguicidas", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HelpBox("¿Por dónde empiezo? Toca \"Nueva aplicación\" y la app te guiará paso a paso: cliente → cultivo → plaga y producto → área → resultado.")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigTile("🧮", "Nueva aplicación", "Calcula dosis, agua y tiempo", Rgb.Green, Modifier.weight(1f)) { nav.goTab("wizard") }
                BigTile("👨‍🌾", "Clientes", "${clients.size} registrados", Rgb.Blue, Modifier.weight(1f)) { nav.goTab("clients") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigTile("🧴", "Productos", "${products.size} usos de etiqueta", Rgb.Orange, Modifier.weight(1f)) { nav.goTab("products") }
                BigTile("📋", "Historial", "${apps.size} aplicaciones", Rgb.Purple, Modifier.weight(1f)) { nav.goTab("history") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Superficie", Calculator.fmt(apps.sumOf { it.areaHa }) + " ha", "🌾", Modifier.weight(1f))
                StatCard("Costo", money(apps.sumOf { it.cost ?: 0.0 }), "💲", Modifier.weight(1f))
            }

            // Parcelas en periodo de reentrada o de seguridad
            val active = apps.filter { (it.reentryAt ?: 0) > now || (it.harvestAllowedAt ?: 0) > now }.take(6)
            SectionCard("Avisos activos", "⏳") {
                if (active.isEmpty()) Text("No hay parcelas en periodo de reentrada ni de seguridad.", color = Muted)
                active.forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Column(Modifier.fillMaxWidth().clickable { nav.navigate("app/${a.id}") }) {
                        Text("${Catalog.crop(a.crop)?.emoji ?: "🌱"} ${a.plotName.ifBlank { a.crop }} · ${a.clientName.ifBlank { "Sin cliente" }}", fontWeight = FontWeight.SemiBold)
                        a.reentryAt?.takeIf { it > now }?.let { Text("🚷 No entrar hasta: ${dateTimeFmt.format(it)}", color = Color(0xFFA83434), fontSize = 13.sp) }
                        a.harvestAllowedAt?.takeIf { it > now }?.let { Text("🧺 Cosechar a partir de: ${dateFmt.format(it)}", color = Color(0xFF8A5B08), fontSize = 13.sp) }
                    }
                }
            }

            SectionCard("Últimas aplicaciones", "🕘") {
                if (apps.isEmpty()) Text("Todavía no hay aplicaciones. ¡Registra la primera!", color = Muted)
                apps.take(5).forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate("app/${a.id}") }, verticalAlignment = Alignment.CenterVertically) {
                        Text(Catalog.category(a.category).emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.productName, fontWeight = FontWeight.SemiBold)
                            Text("${dateFmt.format(a.date)} · ${a.crop} · ${Calculator.fmt(a.areaHa)} ha", fontSize = 13.sp, color = Muted)
                        }
                    }
                }
            }

            Text(
                "AgroSense es una herramienta de cálculo y registro. No sustituye la etiqueta ni la recomendación de un profesional. " +
                    "Verifica siempre el registro vigente (COFEPRIS/SENASICA), la dosis, el intervalo de seguridad y la reentrada.",
                fontSize = 11.sp, color = Muted
            )
            Text("⚙️ Respaldo y exportación", color = GreenDark, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { nav.navigate("backup") }.padding(vertical = 6.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}
