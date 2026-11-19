package com.swat.agrosense.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.swat.agrosense.AppViewModel
import com.swat.agrosense.data.ApplicationRecord
import com.swat.agrosense.data.Catalog
import com.swat.agrosense.data.Client
import com.swat.agrosense.data.Plot
import com.swat.agrosense.domain.Calculator
import com.swat.agrosense.ui.components.Dropdown
import com.swat.agrosense.ui.components.HelpBox
import com.swat.agrosense.ui.components.NumberField
import com.swat.agrosense.ui.components.RgbButton
import com.swat.agrosense.ui.components.SectionCard
import com.swat.agrosense.ui.components.StatCard
import com.swat.agrosense.ui.components.TextFieldX
import com.swat.agrosense.ui.theme.Danger
import com.swat.agrosense.ui.theme.Green
import com.swat.agrosense.ui.theme.Muted
import com.swat.agrosense.ui.theme.Rgb
import java.util.TimeZone

@Composable
fun ClientsScreen(vm: AppViewModel, nav: NavHostController) {
    val clients by vm.clients.collectAsStateWithLifecycle()
    val plots by vm.plots.collectAsStateWithLifecycle()
    val apps by vm.applications.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Client?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("👨‍🌾 Clientes", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Text("Cada cliente tiene sus parcelas y el historial de lo aplicado.", color = Muted)
        Spacer(Modifier.height(10.dp))
        RgbButton("Agregar cliente", Modifier.fillMaxWidth(), Rgb.Blue, icon = Icons.Filled.Add) { editing = Client(name = "") }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(q, { q = it }, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Buscar por nombre o comunidad") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(10.dp))
        val list = clients.filter { q.isBlank() || (it.name + " " + it.location).contains(q, true) }
        if (list.isEmpty()) HelpBox(if (clients.isEmpty()) "Aún no tienes clientes. Toca \"Agregar cliente\" para registrar al primero." else "Sin resultados.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { c ->
                val cp = plots.filter { it.clientId == c.id }
                val ca = apps.filter { it.clientId == c.id }
                SectionCard {
                    Row(Modifier.fillMaxWidth().clickable { nav.navigate("client/${c.id}") }, verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(Rgb.Teal.toList())), contentAlignment = Alignment.Center) {
                            Text(c.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(listOf(c.location, c.phone).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin datos de contacto" }, color = Muted, fontSize = 13.sp)
                            Text("${cp.size} parcelas · ${ca.size} aplicaciones · ${Calculator.fmt(cp.sumOf { it.areaHa })} ha", fontSize = 13.sp, color = Green)
                        }
                        Text("›", fontSize = 28.sp, color = Muted)
                    }
                }
            }
        }
    }
    editing?.let { c -> ClientDialog(c, onDismiss = { editing = null }) { vm.saveClient(it); editing = null } }
}

@Composable
fun ClientDialog(initial: Client, onDismiss: () -> Unit, onSave: (Client) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var phone by remember { mutableStateOf(initial.phone) }
    var loc by remember { mutableStateOf(initial.location) }
    var notes by remember { mutableStateOf(initial.notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nuevo cliente" else "Editar cliente") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextFieldX("Nombre del productor *", name, { name = it })
                TextFieldX("Teléfono", phone, { phone = it }, keyboard = KeyboardType.Phone)
                TextFieldX("Comunidad / municipio", loc, { loc = it })
                TextFieldX("Notas", notes, { notes = it }, singleLine = false)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(initial.copy(name = name.trim(), phone = phone.trim(), location = loc.trim(), notes = notes.trim())) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: Long?, onChange: (Long?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value?.let { dateFmt.format(it) } ?: "", onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.CalendarMonth, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
    }
    if (expanded) {
        // DatePicker trabaja en UTC: ajustamos para que la fecha local no se recorra un día.
        val tz = TimeZone.getDefault()
        val st = rememberDatePickerState(initialSelectedDateMillis = (value ?: System.currentTimeMillis()).let { it + tz.getOffset(it) })
        DatePickerDialog(
            onDismissRequest = { expanded = false },
            confirmButton = { TextButton(onClick = { onChange(st.selectedDateMillis?.let { it - tz.getOffset(it) + 12 * 3_600_000L }); expanded = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { onChange(null); expanded = false }) { Text("Quitar") } }
        ) { DatePicker(st) }
    }
}

@Composable
fun PlotDialog(initial: Plot, onDismiss: () -> Unit, onSave: (Plot) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var crop by remember { mutableStateOf(initial.crop.ifBlank { Catalog.crops.first().name }) }
    var variety by remember { mutableStateOf(initial.variety) }
    var area by remember { mutableStateOf(if (initial.areaHa > 0) Calculator.fmt(initial.areaHa, 4).replace(",", "") else "") }
    var start by remember { mutableStateOf(initial.cycleStart) }
    var end by remember { mutableStateOf(initial.cycleEnd) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nueva parcela" else "Editar parcela") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextFieldX("Nombre de la parcela / lote *", name, { name = it })
                Dropdown("Cultivo *", Catalog.crops.map { it.name }, crop, { crop = it }, display = { n -> (Catalog.crop(n)?.emoji ?: "") + " " + n })
                TextFieldX("Variedad", variety, { variety = it })
                NumberField("Superficie *", area, { area = it }, suffix = "ha", help = "1 ha = 10,000 m²")
                DateField("Inicio del ciclo (siembra)", start, { start = it })
                DateField("Cosecha estimada", end, { end = it })
            }
        },
        confirmButton = {
            val a = area.toDoubleOrNull() ?: 0.0
            TextButton(enabled = name.isNotBlank() && a > 0, onClick = { onSave(initial.copy(name = name.trim(), crop = crop, variety = variety.trim(), areaHa = a, cycleStart = start, cycleEnd = end)) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ClientDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val clients by vm.clients.collectAsStateWithLifecycle()
    val plots by vm.plots.collectAsStateWithLifecycle()
    val apps by vm.applications.collectAsStateWithLifecycle()
    val client = clients.firstOrNull { it.id == id } ?: run { Text("Cliente no encontrado", Modifier.padding(16.dp)); return }
    val cPlots = plots.filter { it.clientId == id }
    val cApps = apps.filter { it.clientId == id }
    var editClient by remember { mutableStateOf(false) }
    var editPlot by remember { mutableStateOf<Plot?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Regresar") }
            Column(Modifier.weight(1f)) {
                Text(client.name, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                Text(listOf(client.location, client.phone).filter { it.isNotBlank() }.joinToString(" · "), color = Muted)
            }
            IconButton(onClick = { editClient = true }) { Icon(Icons.Filled.Edit, "Editar") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Eliminar", tint = Danger) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Aplicaciones", cApps.size.toString(), "🧪", Modifier.weight(1f))
            StatCard("Costo total", money(cApps.sumOf { it.cost ?: 0.0 }), "💲", Modifier.weight(1f))
        }
        RgbButton("Agregar parcela", Modifier.fillMaxWidth(), Rgb.Teal, icon = Icons.Filled.Add) { editPlot = Plot(clientId = id, name = "", crop = "", areaHa = 0.0) }
        if (cPlots.isEmpty()) HelpBox("Agrega las parcelas del cliente (nombre, cultivo y superficie). Así cada aplicación queda ligada a su ciclo de cultivo.")

        cPlots.forEach { p -> PlotCard(p, cApps.filter { it.plotId == p.id }, onEdit = { editPlot = p }, onApply = { nav.navigate("wizard?clientId=$id&plotId=${p.id}") }, onOpen = { nav.navigate("app/$it") }) }

        val loose = cApps.filter { a -> cPlots.none { it.id == a.plotId } }
        if (loose.isNotEmpty()) SectionCard("Aplicaciones sin parcela", "📄") { Timeline(loose) { nav.navigate("app/$it") } }
        RgbButton("Nueva aplicación para ${client.name}", Modifier.fillMaxWidth(), Rgb.Green, emoji = "🧮") { nav.navigate("wizard?clientId=$id&plotId=-1") }
        Spacer(Modifier.height(20.dp))
    }

    if (editClient) ClientDialog(client, { editClient = false }) { vm.saveClient(it); editClient = false }
    editPlot?.let { p -> PlotDialog(p, { editPlot = null }) { vm.savePlot(it); editPlot = null } }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("¿Eliminar cliente?") },
        text = { Text("Se borrarán el cliente y sus parcelas. Las aplicaciones registradas se conservan en el historial.") },
        confirmButton = { TextButton(onClick = { vm.deleteClient(client); confirmDelete = false; nav.popBackStack() }) { Text("Eliminar", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun PlotCard(p: Plot, apps: List<ApplicationRecord>, onEdit: () -> Unit, onApply: () -> Unit, onOpen: (Long) -> Unit) {
    val crop = Catalog.crop(p.crop)
    val now = System.currentTimeMillis()
    val inCycle = apps.filter { a -> (p.cycleStart == null || a.date >= p.cycleStart) && (p.cycleEnd == null || a.date <= p.cycleEnd + 86_400_000L) }
    val harvest = apps.mapNotNull { it.harvestAllowedAt }.maxOrNull()
    SectionCard("${p.name} · ${Calculator.fmt(p.areaHa)} ha", crop?.emoji ?: "🌱") {
        Text("${p.crop}${if (p.variety.isNotBlank()) " · ${p.variety}" else ""}", color = Muted)
        if (p.cycleStart != null || p.cycleEnd != null)
            Text("Ciclo: ${p.cycleStart?.let { dateFmt.format(it) } ?: "?"} → ${p.cycleEnd?.let { dateFmt.format(it) } ?: "?"}", fontSize = 13.sp)
        Text("Aplicaciones en el ciclo: ${inCycle.size} · Costo: ${money(inCycle.sumOf { it.cost ?: 0.0 })}", fontSize = 13.sp, color = Green)
        if (harvest != null) {
            val ok = harvest <= now
            Text(if (ok) "🧺 Cosecha permitida (intervalo de seguridad cumplido)" else "⏳ Cosechar a partir de ${dateFmt.format(harvest)}",
                color = if (ok) Green else Color(0xFF8A5B08), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        // Resumen por ingrediente activo (para rotación)
        val byActive = inCycle.groupBy { it.active.ifBlank { it.productName } }
        if (byActive.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Ingredientes activos usados:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            byActive.forEach { (k, v) -> Text("• $k: ${v.size} vez/veces", fontSize = 13.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Timeline(apps, onOpen)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RgbButton("Aplicar aquí", Modifier.weight(1f), Rgb.Green, emoji = "💧", height = 48.dp, onClick = onApply)
            RgbButton("Editar", Modifier.weight(1f), Rgb.Purple, icon = Icons.Filled.Edit, height = 48.dp, onClick = onEdit)
        }
    }
}

/** Línea de tiempo vertical de aplicaciones. */
@Composable
fun Timeline(apps: List<ApplicationRecord>, onOpen: (Long) -> Unit) {
    if (apps.isEmpty()) { Text("Sin aplicaciones registradas.", color = Muted, fontSize = 13.sp); return }
    apps.sortedBy { it.date }.forEachIndexed { i, a ->
        val cat = Catalog.category(a.category)
        Row(Modifier.fillMaxWidth().clickable { onOpen(a.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color(cat.color)), contentAlignment = Alignment.Center) { Text(cat.emoji, fontSize = 15.sp) }
                if (i < apps.size - 1) Box(Modifier.width(2.dp).height(26.dp).background(Color(0xFFDCE6DF)))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${dateFmt.format(a.date)} · ${a.productName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${a.target.ifBlank { cat.name }} · ${Calculator.fmt(a.dose)} ${a.doseUnit} · ${Calculator.fmtQty(a.productTotal, a.productBaseUnit)}", fontSize = 12.sp, color = Muted)
            }
        }
    }
    HorizontalDivider(color = Color.Transparent)
}
