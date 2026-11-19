package com.swat.agrosense.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.swat.agrosense.AppViewModel
import com.swat.agrosense.data.ApplicationRecord
import com.swat.agrosense.data.Catalog
import com.swat.agrosense.domain.Calculator
import com.swat.agrosense.domain.Safety
import com.swat.agrosense.ui.components.HelpBox
import com.swat.agrosense.ui.components.RgbButton
import com.swat.agrosense.ui.components.SectionCard
import com.swat.agrosense.ui.components.StatCard
import com.swat.agrosense.ui.components.WarnBox
import com.swat.agrosense.ui.theme.Danger
import com.swat.agrosense.ui.theme.Muted
import com.swat.agrosense.ui.theme.Rgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(vm: AppViewModel, nav: NavHostController) {
    val apps by vm.applications.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var from by remember { mutableStateOf<Long?>(null) }
    var to by remember { mutableStateOf<Long?>(null) }
    val list = apps.filter { a ->
        (q.isBlank() || listOf(a.clientName, a.plotName, a.crop, a.productName, a.target, a.operator).joinToString(" ").contains(q, true)) &&
            (cat.isBlank() || a.category == cat) && (from == null || a.date >= from!! - 43_200_000L) && (to == null || a.date <= to!! + 43_200_000L)
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("📋 Historial", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(q, { q = it }, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Cliente, parcela, cultivo, producto…") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(12.dp))
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Desde", from, { from = it }, Modifier.weight(1f))
            DateField("Hasta", to, { to = it }, Modifier.weight(1f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(cat.isBlank(), { cat = "" }, label = { Text("Todos") })
            Catalog.categories.filter { c -> apps.any { it.category == c.key } }.forEach { c -> FilterChip(cat == c.key, { cat = c.key }, label = { Text("${c.emoji} ${c.name}") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Registros", list.size.toString(), "🧾", Modifier.weight(1f))
            StatCard("Hectáreas", Calculator.fmt(list.sumOf { it.areaHa }), "🌾", Modifier.weight(1f))
            StatCard("Costo", money(list.sumOf { it.cost ?: 0.0 }), "💲", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        if (list.isEmpty()) HelpBox("No hay aplicaciones con estos filtros.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { a ->
                val c = Catalog.category(a.category)
                ChoiceRow(
                    title = "${c.emoji} ${a.productName}",
                    subtitle = "${dateFmt.format(a.date)} · ${a.clientName.ifBlank { "Sin cliente" }}${if (a.plotName.isNotBlank()) " / " + a.plotName else ""} · ${a.crop.substringBefore(" (")}",
                    trailing = "${Calculator.fmt(a.areaHa)} ha\n${Calculator.fmtQty(a.productTotal, a.productBaseUnit)}",
                    selected = false
                ) { nav.navigate("app/${a.id}") }
            }
        }
    }
}

fun reportText(a: ApplicationRecord): String = buildString {
    appendLine("🌱 AgroSense · Registro de aplicación")
    appendLine("Fecha: ${dateTimeFmt.format(a.date)}")
    appendLine("Cliente: ${a.clientName.ifBlank { "—" }} · Parcela: ${a.plotName.ifBlank { "—" }}")
    appendLine("Cultivo: ${a.crop} · Objetivo: ${a.target.ifBlank { Catalog.category(a.category).name }}")
    appendLine("Producto: ${a.productName} (${a.active})")
    appendLine("Dosis: ${Calculator.fmt(a.dose, 3)} ${a.doseUnit} · Superficie: ${Calculator.fmt(a.areaHa, 4)} ha")
    appendLine("Producto total: ${Calculator.fmtQty(a.productTotal, a.productBaseUnit)} · Agua: ${a.waterTotalL?.let { Calculator.fmt(it, 1) } ?: "—"} L · Tanques: ${a.tanks ?: "—"}")
    appendLine("Equipo: ${a.equipment} · Tiempo: ${a.durationMin?.let { Calculator.fmtMinutes(it) } ?: "—"} · Costo: ${money(a.cost)}")
    a.reentryAt?.let { appendLine("Reentrada: ${dateTimeFmt.format(it)}") }
    a.harvestAllowedAt?.let { appendLine("Cosecha permitida desde: ${dateFmt.format(it)}") }
    if (a.operator.isNotBlank()) appendLine("Aplicador: ${a.operator}")
    if (a.weather.isNotBlank()) appendLine("Clima: ${a.weather}")
    if (a.ppe.isNotBlank()) appendLine("EPP: ${a.ppe.replace("|", ", ")}")
    if (a.notes.isNotBlank()) appendLine("Notas: ${a.notes}")
}

@Composable
fun ApplicationDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val apps by vm.applications.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val a = apps.firstOrNull { it.id == id } ?: run { Text("Registro no encontrado", Modifier.padding(16.dp)); return }
    val c = Catalog.category(a.category)
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Regresar") }
            Column(Modifier.weight(1f)) {
                Text("${c.emoji} ${a.productName}", style = MaterialTheme.typography.titleLarge)
                Text(dateTimeFmt.format(a.date), color = Muted)
            }
            IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Danger) }
        }
        a.reentryAt?.takeIf { it > now }?.let { WarnBox("🚷 No entrar a la parcela hasta ${dateTimeFmt.format(it)}") }
        a.harvestAllowedAt?.takeIf { it > now }?.let { WarnBox("🧺 No cosechar antes del ${dateFmt.format(it)}") }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Producto", Calculator.fmtQty(a.productTotal, a.productBaseUnit), "🧪", Modifier.weight(1f))
            StatCard("Agua", "${a.waterTotalL?.let { Calculator.fmt(it, 1) } ?: "—"} L", "💧", Modifier.weight(1f))
        }
        SectionCard("Detalle", "📄") { Text(reportText(a), fontSize = 14.sp) }
        SectionCard("Cuidados posteriores", "🧼") { Safety.afterApplying(a.category).forEach { Text("• $it", fontSize = 14.sp) } }
        RgbButton("Compartir reporte (WhatsApp, correo…)", Modifier.fillMaxWidth(), Rgb.Blue, icon = Icons.Filled.Share) {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, reportText(a)), "Compartir"))
        }
        if (a.clientId != null) RgbButton("Ver cliente", Modifier.fillMaxWidth(), Rgb.Teal, emoji = "👨‍🌾") { nav.navigate("client/${a.clientId}") }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("¿Eliminar registro?") },
        text = { Text("Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = { vm.deleteApplication(a); confirm = false; nav.popBackStack() }) { Text("Eliminar", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } }
    )
}

@Composable
fun BackupScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_LONG).show()

    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) } }
            }.onSuccess { toast("Respaldo guardado ✅") }.onFailure { toast("No se pudo guardar: ${it.message}") }
        }
    }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(vm.exportCsv().toByteArray()) } } }
                .onSuccess { toast("CSV exportado ✅ (ábrelo en Excel)") }.onFailure { toast("No se pudo exportar: ${it.message}") }
        }
    }
    val openJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("⚙️ Respaldo y exportación", style = MaterialTheme.typography.headlineSmall)
        HelpBox("Todos tus datos se guardan en la memoria interna del teléfono, aunque no tengas internet. Haz un respaldo seguido para no perderlos si cambias de equipo.")
        RgbButton("Guardar respaldo (JSON)", Modifier.fillMaxWidth(), Rgb.Green, emoji = "💾") { saveJson.launch("AgroSense_respaldo.json") }
        RgbButton("Exportar historial a Excel (CSV)", Modifier.fillMaxWidth(), Rgb.Blue, emoji = "📊") { saveCsv.launch("AgroSense_historial.csv") }
        RgbButton("Restaurar respaldo", Modifier.fillMaxWidth(), Rgb.Red, emoji = "📥") { openJson.launch(arrayOf("application/json", "*/*")) }
        Text("El respaldo de AgroSense también es compatible con la versión de iPhone.", color = Muted, fontSize = 13.sp)
        Text("AgroSense 1.0 · Hecho para ingenieros agrónomos", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("¿Restaurar respaldo?") },
            text = { Text("Se reemplazarán TODOS los clientes, parcelas, productos y aplicaciones actuales por los del archivo.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        runCatching {
                            val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
                            vm.importJson(text)
                        }.onSuccess { toast("Respaldo restaurado ✅") }.onFailure { toast("Archivo no válido: ${it.message}") }
                    }
                }) { Text("Restaurar", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancelar") } }
        )
    }
}
