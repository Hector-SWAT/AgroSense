package com.swat.agrosense.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.swat.agrosense.AppViewModel
import com.swat.agrosense.data.ApplicationRecord
import com.swat.agrosense.data.Catalog
import com.swat.agrosense.data.Client
import com.swat.agrosense.data.Plot
import com.swat.agrosense.data.Product
import com.swat.agrosense.domain.Calculator
import com.swat.agrosense.domain.Safety
import com.swat.agrosense.ui.components.ChoiceCard
import com.swat.agrosense.ui.components.Dropdown
import com.swat.agrosense.ui.components.HelpBox
import com.swat.agrosense.ui.components.NumberField
import com.swat.agrosense.ui.components.RgbButton
import com.swat.agrosense.ui.components.SectionCard
import com.swat.agrosense.ui.components.StatCard
import com.swat.agrosense.ui.components.StepIndicator
import com.swat.agrosense.ui.components.TextFieldX
import com.swat.agrosense.ui.components.WarnBox
import com.swat.agrosense.ui.theme.Danger
import com.swat.agrosense.ui.theme.Green
import com.swat.agrosense.ui.theme.Muted
import com.swat.agrosense.ui.theme.Rgb

private val STEPS = listOf("Cliente", "Cultivo", "Producto", "Área", "Resultado")
private const val NONE = -1L

@Composable
fun WizardScreen(vm: AppViewModel, nav: NavHostController, startClient: Long, startPlot: Long) {
    val clients by vm.clients.collectAsStateWithLifecycle()
    val plots by vm.plots.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val history by vm.applications.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    var step by remember { mutableStateOf(0) }
    var clientId by remember { mutableStateOf(startClient) }
    var plotId by remember { mutableStateOf(startPlot) }
    var crop by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf(NONE) }
    var target by remember { mutableStateOf("") }
    var areaText by remember { mutableStateOf("") }
    var areaUnit by remember { mutableStateOf("m²") }
    var equipKey by remember { mutableStateOf("") }
    var waterText by remember { mutableStateOf("") }
    var tankText by remember { mutableStateOf("") }
    var flowText by remember { mutableStateOf("") }
    var refillText by remember { mutableStateOf("") }
    var doseText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var weather by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var appDate by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    val ppeChecked = remember { mutableStateListOf<String>() }
    var newClient by remember { mutableStateOf(false) }
    var newPlot by remember { mutableStateOf(false) }
    var newProduct by remember { mutableStateOf<Product?>(null) }

    val client = clients.firstOrNull { it.id == clientId }
    val plot = plots.firstOrNull { it.id == plotId }
    val product = products.firstOrNull { it.id == productId }
    val cropInfo = Catalog.crop(crop)
    val equip = if (equipKey.isBlank()) null else Catalog.equipment(equipKey)

    // Al elegir parcela: precargar cultivo y superficie
    LaunchedEffect(plotId, plots.size) {
        plot?.let { crop = it.crop; areaText = Calculator.fmt(it.areaHa, 4).replace(",", ""); areaUnit = "ha" }
    }
    // Al elegir producto: precargar dosis, objetivo y precio
    LaunchedEffect(productId) {
        product?.let {
            doseText = Calculator.fmt(it.doseMin, 4).replace(",", "")
            if (target.isBlank()) target = it.target
            priceText = it.price?.let { p -> Calculator.fmt(p, 2).replace(",", "") } ?: ""
        }
    }
    LaunchedEffect(step) { scroll.animateScrollTo(0) }

    fun selectEquipment(key: String) {
        val e = Catalog.equipment(key)
        equipKey = key
        tankText = Calculator.fmt(e.tankL, 1).replace(",", "")
        flowText = Calculator.fmt(e.flowLMin, 2)
        refillText = Calculator.fmt(e.refillMin, 1)
        waterText = when {
            e.fixedWaterLHa > 0 -> Calculator.fmt(e.fixedWaterLHa, 0)
            cropInfo != null -> ((cropInfo.waterMin + cropInfo.waterMax) / 2).toString()
            else -> "300"
        }
    }

    val areaHa = (areaText.toDoubleOrNull() ?: 0.0).let { if (areaUnit == "m²") it / 10_000.0 else it }
    val dose = doseText.toDoubleOrNull() ?: 0.0
    val outcome = if (product != null) Calculator.calculate(
        Calculator.Input(
            dose = dose, unit = product.unit, areaHa = areaHa, waterLHa = waterText.toDoubleOrNull() ?: 0.0,
            tankL = tankText.toDoubleOrNull() ?: 0.0, flowLMin = flowText.toDoubleOrNull() ?: 0.0,
            refillMin = refillText.toDoubleOrNull() ?: 0.0, price = priceText.toDoubleOrNull(), priceUnit = product.priceUnit
        )
    ) else null

    val canNext = when (step) {
        0 -> true
        1 -> crop.isNotBlank()
        2 -> product != null
        3 -> outcome is Calculator.Outcome.Ok && equip != null
        else -> false
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("🧮 Nueva aplicación", style = MaterialTheme.typography.headlineSmall)
            StepIndicator(step, STEPS)
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                // ---------------- PASO 1: CLIENTE ----------------
                0 -> {
                    HelpBox("Paso 1 de 5 · Elige para quién es la aplicación y en qué parcela. Si solo quieres calcular, deja \"Sin cliente\" y toca Siguiente.")
                    SectionCard("Cliente", "👨‍🌾") {
                        Dropdown("Cliente", listOf(NONE.toString()) + clients.map { it.id.toString() }, clientId.toString(),
                            { clientId = it.toLong(); plotId = NONE },
                            display = { s -> if (s == NONE.toString()) "Sin cliente (solo calcular)" else clients.firstOrNull { it.id.toString() == s }?.name ?: "" })
                        Spacer(Modifier.height(8.dp))
                        RgbButton("Cliente nuevo", Modifier.fillMaxWidth(), Rgb.Blue, icon = Icons.Filled.Add, height = 48.dp) { newClient = true }
                    }
                    if (client != null) SectionCard("Parcela", "🗺️") {
                        val cp = plots.filter { it.clientId == client.id }
                        Dropdown("Parcela / lote", listOf(NONE.toString()) + cp.map { it.id.toString() }, plotId.toString(), { plotId = it.toLong() },
                            display = { s -> if (s == NONE.toString()) "Sin parcela" else cp.firstOrNull { it.id.toString() == s }?.let { "${Catalog.crop(it.crop)?.emoji ?: ""} ${it.name} · ${Calculator.fmt(it.areaHa)} ha" } ?: "" })
                        Spacer(Modifier.height(8.dp))
                        RgbButton("Parcela nueva", Modifier.fillMaxWidth(), Rgb.Teal, icon = Icons.Filled.Add, height = 48.dp) { newPlot = true }
                    }
                }
                // ---------------- PASO 2: CULTIVO ----------------
                1 -> {
                    HelpBox("Paso 2 de 5 · Toca el cultivo. El cultivo define el volumen de agua recomendado y los productos autorizados.")
                    var q by remember { mutableStateOf("") }
                    OutlinedTextField(q, { q = it }, leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Search, null) },
                        placeholder = { Text("Buscar cultivo…") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Catalog.crops.filter { q.isBlank() || it.name.contains(q, true) }.groupBy { it.group }.forEach { (group, list) ->
                        Text(group, fontWeight = FontWeight.Bold, color = Green)
                        list.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { c ->
                                    ChoiceCard(c.emoji, c.name.substringBefore(" ("), "${c.waterMin}–${c.waterMax} L/ha", crop == c.name, modifier = Modifier.weight(1f)) {
                                        crop = c.name; productId = NONE; equipKey = ""
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                // ---------------- PASO 3: PLAGA Y PRODUCTO ----------------
                2 -> {
                    HelpBox("Paso 3 de 5 · ¿Qué quieres controlar? Elige el tipo y luego el producto. Primero aparecen los autorizados para ${crop.substringBefore(" (")}.")
                    Catalog.categories.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { c ->
                                ChoiceCard(c.emoji, c.name, c.controls, category == c.key, Color(c.color), Modifier.weight(1f)) { category = c.key; productId = NONE; target = "" }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (category.isNotBlank()) {
                        val cat = Catalog.category(category)
                        if (cat.restricted) WarnBox("${cat.name}: uso restringido. Solo con autorización vigente y conforme a la etiqueta.")
                        val inCat = products.filter { it.category == category }
                        val forCrop = inCat.filter { it.crop == crop }
                        val others = inCat.filter { it.crop != crop }
                        SectionCard("Productos para ${crop.substringBefore(" (")}", cat.emoji) {
                            if (forCrop.isEmpty()) Text("No hay productos de ${cat.name.lowercase()} registrados para este cultivo. Agrega uno con los datos de su etiqueta.", color = Muted, fontSize = 14.sp)
                            forCrop.forEach { p -> ProductRow(p, productId == p.id) { productId = p.id; target = p.target } }
                        }
                        if (others.isNotEmpty()) SectionCard("Otros ${cat.name.lowercase()} del catálogo", "📦") {
                            Text("Verifica que la etiqueta autorice su uso en tu cultivo antes de elegirlos.", color = Danger, fontSize = 13.sp)
                            others.forEach { p -> ProductRow(p, productId == p.id) { productId = p.id; target = p.target } }
                        }
                        RgbButton("Agregar producto de etiqueta", Modifier.fillMaxWidth(), Rgb.Orange, icon = Icons.Filled.Add, height = 48.dp) {
                            newProduct = Product(name = "", category = category, crop = crop, doseMin = 0.0, doseMax = 0.0, unit = "L/ha")
                        }
                        if (product != null) TextFieldX("Plaga / maleza / enfermedad a controlar", target, { target = it })
                    }
                }
                // ---------------- PASO 4: ÁREA, EQUIPO Y DOSIS ----------------
                3 -> {
                    val p = product!!
                    HelpBox("Paso 4 de 5 · Escribe la superficie, elige tu equipo y ajusta la dosis dentro del rango de la etiqueta.")
                    SectionCard("Superficie a tratar", "📐") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberField("Superficie", areaText, { areaText = it }, Modifier.weight(1f))
                            listOf("m²", "ha").forEach { u -> FilterChip(areaUnit == u, { areaUnit = u }, label = { Text(u) }) }
                        }
                        if (areaHa > 0) Text("= ${Calculator.fmt(areaHa, 4)} ha  ·  ${Calculator.fmt(areaHa * 10000, 0)} m²", color = Muted, fontSize = 13.sp)
                    }
                    SectionCard("Equipo de aplicación", "🧰") {
                        Catalog.equipment.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { e ->
                                    ChoiceCard(e.emoji, e.name, if (cropInfo?.equipment == e.key) "⭐ Sugerido" else "${Calculator.fmt(e.tankL, 0)} L", equipKey == e.key, Color(0xFF2979FF), Modifier.weight(1f)) { selectEquipment(e.key) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (equip != null) SectionCard("Agua y tanque", "💧") {
                        NumberField("Volumen de agua por hectárea", waterText, { waterText = it }, suffix = "L/ha",
                            help = if (equip.fixedWaterLHa > 0) "Dron: usa el volumen que indique la etiqueta para aplicación aérea."
                            else "Rango orientativo para ${crop.substringBefore(" (")}: ${cropInfo?.waterMin}–${cropInfo?.waterMax} L/ha. Usa el de tu calibración.")
                        Spacer(Modifier.height(6.dp))
                        NumberField("Capacidad del tanque", tankText, { tankText = it }, suffix = "L")
                        var adv by remember { mutableStateOf(false) }
                        TextButton(onClick = { adv = !adv }) { Text(if (adv) "▲ Ocultar ajustes del equipo" else "▼ Ajustes del equipo (gasto y recarga)") }
                        AnimatedVisibility(adv) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                NumberField("Gasto del equipo", flowText, { flowText = it }, suffix = "L/min", help = "Mide cuántos litros descarga en 1 minuto.")
                                NumberField("Tiempo de recarga por tanque", refillText, { refillText = it }, suffix = "min")
                            }
                        }
                    }
                    SectionCard("Dosis de etiqueta", "🧪") {
                        Text("${p.name} · ${p.active}", fontWeight = FontWeight.SemiBold)
                        Text("Rango de etiqueta: ${Calculator.fmt(p.doseMin, 3)}${if (p.doseMax > p.doseMin) "–" + Calculator.fmt(p.doseMax, 3) else ""} ${p.unit}" +
                            if (p.method.isNotBlank()) " · ${p.method}" else "", color = Muted, fontSize = 13.sp)
                        if (p.doseMax > p.doseMin) {
                            Slider(
                                value = dose.coerceIn(p.doseMin, p.doseMax).toFloat(),
                                onValueChange = { doseText = Calculator.fmt(it.toDouble(), 3).replace(",", "") },
                                valueRange = p.doseMin.toFloat()..p.doseMax.toFloat()
                            )
                            Row { Text("Mínima", fontSize = 12.sp, color = Muted, modifier = Modifier.weight(1f)); Text("Máxima", fontSize = 12.sp, color = Muted) }
                            Text("Usa la dosis baja con poca presión de plaga y la alta con mucha presión.", fontSize = 12.sp, color = Muted)
                        }
                        NumberField("Dosis a usar", doseText, { doseText = it }, suffix = p.unit)
                        if (p.unit.startsWith("%")) Text("El porcentaje se calcula sobre el caldo: 1 % = 10 ${if (p.unit == "% v/v") "mL" else "g"} por litro de agua.", fontSize = 12.sp, color = Muted)
                        NumberField("Precio del producto (opcional)", priceText, { priceText = it }, suffix = "MXN/${p.priceUnit}")
                    }
                    if (outcome is Calculator.Outcome.Error && equip != null) WarnBox(outcome.message)
                }
                // ---------------- PASO 5: RESULTADO ----------------
                4 -> {
                    val p = product!!
                    val r = (outcome as Calculator.Outcome.Ok).r
                    val e = equip!!
                    val date = appDate ?: System.currentTimeMillis()
                    val warnings = Safety.warnings(p, plot?.id, history, date, dose)
                    val ppe = Safety.ppe(p.category, p.formulation, p.toxBand, e.key)

                    HelpBox("Paso 5 de 5 · Revisa el resultado, marca el equipo de protección y guarda la aplicación.")
                    warnings.forEach { WarnBox(it) }
                    SectionCard("Lo que necesitas", "✅") {
                        Text(p.name, color = Muted)
                        Text(Calculator.fmtQty(r.productBase, r.baseUnit), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Green)
                        Text("de producto para ${Calculator.fmt(areaHa, 4)} ha de ${crop.substringBefore(" (")}", color = Muted)
                        if (p.method.isNotBlank()) Text("Nota de etiqueta: ${p.method}", fontSize = 13.sp, color = Color(0xFF8A5B08))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Agua total", "${Calculator.fmt(r.waterTotalL, 1)} L", "💧", Modifier.weight(1f), Color(0xFF2979FF))
                        StatCard("Tanques", r.tanksTotal.toString(), e.emoji, Modifier.weight(1f), Color(0xFF7C4DFF))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Tiempo estimado", Calculator.fmtMinutes(r.totalMin), "⏱️", Modifier.weight(1f), Color(0xFFFF6D00))
                        StatCard("Costo", money(r.cost), "💲", Modifier.weight(1f))
                    }
                    SectionCard("Preparación por tanque", "🪣") {
                        if (r.fullTanks > 0) Text("• ${r.fullTanks} tanque(s) llenos de ${Calculator.fmt(tankText.toDoubleOrNull() ?: e.tankL, 1)} L → ${Calculator.fmtQty(r.productPerFullTank, r.baseUnit)} de producto cada uno", fontSize = 15.sp)
                        if (r.lastTankL > 0) Text("• Último tanque con ${Calculator.fmt(r.lastTankL, 1)} L de agua → ${Calculator.fmtQty(r.productLastTank, r.baseUnit)} de producto", fontSize = 15.sp)
                        Text("Concentración: ${Calculator.fmt(r.productPerLiter, 3)} ${r.baseUnit} por litro de agua.", fontSize = 13.sp, color = Muted)
                        Text("Tiempo: ${Calculator.fmtMinutes(r.sprayMin)} aplicando + ${Calculator.fmtMinutes(r.refillTotalMin)} de recargas (estimado con el gasto del equipo).", fontSize = 13.sp, color = Muted)
                    }
                    SectionCard("Cuándo aplicar", "🌤️") { Safety.bestTime.forEach { Text(it, fontSize = 14.sp) } }
                    SectionCard("Antes de aplicar", "📋") { Safety.beforeApplying(e.key).forEach { Text("• $it", fontSize = 14.sp) } }
                    SectionCard("Equipo de protección (márcalo)", "🦺") {
                        ppe.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(item in ppeChecked, { checked -> if (checked) ppeChecked.add(item) else ppeChecked.remove(item) })
                                Text(item, fontSize = 14.sp)
                            }
                        }
                        if (ppeChecked.size < ppe.size) Text("Faltan ${ppe.size - ppeChecked.size} elementos por confirmar.", color = Danger, fontSize = 12.sp)
                    }
                    SectionCard("Después de aplicar", "🧼") {
                        p.reiHours?.let { Text("🚷 Reentrada: ${Calculator.fmt(it)} h → ${dateTimeFmt.format(date + (it * 3_600_000).toLong())}", fontWeight = FontWeight.SemiBold) }
                        p.phiDays?.let { Text("🧺 Cosechar a partir de: ${dateFmt.format(date + it * 86_400_000L)} ($it días)", fontWeight = FontWeight.SemiBold) }
                        if (p.reiHours == null || p.phiDays == null) Text("Captura la reentrada y el intervalo de seguridad en el producto para calcular las fechas.", color = Muted, fontSize = 12.sp)
                        Safety.afterApplying(p.category).forEach { Text("• $it", fontSize = 14.sp) }
                    }
                    SectionCard("Datos del registro", "📝") {
                        Text("Cliente: ${client?.name ?: "Sin cliente"} · Parcela: ${plot?.name ?: "—"}", fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        DateField("Fecha de aplicación", appDate, { appDate = it })
                        Spacer(Modifier.height(6.dp))
                        TextFieldX("Responsable / aplicador", operator, { operator = it })
                        Spacer(Modifier.height(6.dp))
                        TextFieldX("Clima (viento, temperatura, humedad)", weather, { weather = it })
                        Spacer(Modifier.height(6.dp))
                        TextFieldX("Observaciones", notes, { notes = it }, singleLine = false)
                    }
                    RgbButton("Guardar aplicación", Modifier.fillMaxWidth(), Rgb.Green, icon = Icons.Filled.Save, height = 60.dp) {
                        val rec = ApplicationRecord(
                            clientId = client?.id, plotId = plot?.id, clientName = client?.name ?: "", plotName = plot?.name ?: "",
                            crop = crop, category = p.category, productId = p.id, productName = p.name, active = p.active, target = target,
                            date = date, areaHa = areaHa, dose = dose, doseUnit = p.unit, productTotal = r.productBase, productBaseUnit = r.baseUnit,
                            waterTotalL = r.waterTotalL, waterLHa = waterText.toDoubleOrNull(), tanks = r.tanksTotal, tankL = tankText.toDoubleOrNull(),
                            equipment = e.name, durationMin = r.totalMin, cost = r.cost, operator = operator.trim(), weather = weather.trim(),
                            ppe = ppeChecked.joinToString("|"), phiDays = p.phiDays, reiHours = p.reiHours,
                            harvestAllowedAt = p.phiDays?.let { date + it * 86_400_000L }, reentryAt = p.reiHours?.let { date + (it * 3_600_000).toLong() },
                            notes = notes.trim()
                        )
                        // Guarda el precio capturado en el producto para la próxima vez
                        priceText.toDoubleOrNull()?.let { pr -> if (pr != p.price) vm.saveProduct(p.copy(price = pr)) }
                        vm.saveApplication(rec) { nav.navigate("history") { popUpTo("home") } }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        // Barra inferior Atrás / Siguiente
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) RgbButton("Atrás", Modifier.weight(1f), Rgb.Purple, icon = Icons.Filled.ArrowBack) { step-- }
            if (step < 4) RgbButton(if (step == 3) "Ver resultado" else "Siguiente", Modifier.weight(1f), if (canNext) Rgb.Green else (Color.Gray to Color.LightGray),
                icon = Icons.Filled.ArrowForward, enabled = canNext) {
                if (step == 2 && equipKey.isBlank()) cropInfo?.let { selectEquipment(it.equipment) }
                step++
            }
        }
    }

    if (newClient) ClientDialog(Client(name = ""), { newClient = false }) { c -> vm.saveClient(c) { clientId = it; plotId = NONE }; newClient = false }
    if (newPlot && client != null) PlotDialog(Plot(clientId = client.id, name = "", crop = crop, areaHa = 0.0), { newPlot = false }) { p -> vm.savePlot(p) { plotId = it }; newPlot = false }
    newProduct?.let { np -> ProductDialog(np, { newProduct = null }) { p -> vm.saveProduct(p) { productId = it }; newProduct = null } }
}

@Composable
private fun ProductRow(p: Product, selected: Boolean, onClick: () -> Unit) {
    ChoiceRow(
        title = p.name,
        subtitle = "${p.active.ifBlank { "—" }} · ${p.target.ifBlank { "uso general" }}${if (p.crop.isNotBlank()) " · ${p.crop.substringBefore(" (")}" else ""}",
        trailing = "${Calculator.fmt(p.doseMin, 3)}${if (p.doseMax > p.doseMin) "–" + Calculator.fmt(p.doseMax, 3) else ""} ${p.unit}",
        selected = selected, onClick = onClick
    )
}

@Composable
fun ChoiceRow(title: String, subtitle: String, trailing: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Green else Color(0xFFDCE6DF), shape)
            .background(if (selected) Green.copy(alpha = 0.08f) else Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (selected) "🔘" else "⚪", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = Muted)
        }
        Text(trailing, fontWeight = FontWeight.SemiBold, color = Green, fontSize = 13.sp)
    }
}
