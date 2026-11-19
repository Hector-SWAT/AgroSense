package com.swat.agrosense.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swat.agrosense.AppViewModel
import com.swat.agrosense.data.Catalog
import com.swat.agrosense.data.Product
import com.swat.agrosense.domain.Calculator
import com.swat.agrosense.ui.components.Dropdown
import com.swat.agrosense.ui.components.HelpBox
import com.swat.agrosense.ui.components.NumberField
import com.swat.agrosense.ui.components.RgbButton
import com.swat.agrosense.ui.components.TextFieldX
import com.swat.agrosense.ui.theme.Danger
import com.swat.agrosense.ui.theme.Muted
import com.swat.agrosense.ui.theme.Rgb

@Composable
fun ProductsScreen(vm: AppViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🧴 Productos", style = MaterialTheme.typography.headlineSmall)
        Text("Usos autorizados según etiqueta: producto + cultivo + plaga + dosis.", color = Muted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RgbButton("Agregar", Modifier.weight(1f), Rgb.Orange, icon = Icons.Filled.Add) {
                editing = Product(name = "", category = cat.ifBlank { "insecticida" }, doseMin = 0.0, doseMax = 0.0, unit = "L/ha")
            }
            RgbButton("Restaurar catálogo", Modifier.weight(1f), Rgb.Teal, emoji = "♻️") { vm.restoreCatalog() }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(q, { q = it }, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Producto, ingrediente, cultivo o plaga") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(cat.isBlank(), { cat = "" }, label = { Text("Todos") })
            Catalog.categories.forEach { c -> FilterChip(cat == c.key, { cat = c.key }, label = { Text("${c.emoji} ${c.name}") }) }
        }
        val list = products.filter {
            (cat.isBlank() || it.category == cat) &&
                (q.isBlank() || listOf(it.name, it.active, it.crop, it.target).joinToString(" ").contains(q, true))
        }
        if (list.isEmpty()) HelpBox("No hay productos con ese filtro. Toca \"Agregar\" y captura los datos exactos de la etiqueta.")
        LazyColumn {
            items(list, key = { it.id }) { p ->
                val c = Catalog.category(p.category)
                ChoiceRow(
                    title = "${c.emoji} ${p.name}",
                    subtitle = "${p.active.ifBlank { "—" }} · ${p.crop.ifBlank { "cualquier cultivo" }.substringBefore(" (")} · ${p.target.ifBlank { c.name }}" +
                        (if (p.phiDays != null) " · IS ${p.phiDays} d" else "") + (if (p.source == "Catálogo") " · catálogo" else ""),
                    trailing = "${Calculator.fmt(p.doseMin, 3)}${if (p.doseMax > p.doseMin) "–" + Calculator.fmt(p.doseMax, 3) else ""}\n${p.unit}",
                    selected = false
                ) { editing = p }
            }
        }
    }
    editing?.let { p ->
        ProductDialog(p, onDismiss = { editing = null }, onDelete = if (p.id != 0L) ({ vm.deleteProduct(p); editing = null }) else null) {
            vm.saveProduct(it); editing = null
        }
    }
}

@Composable
fun ProductDialog(initial: Product, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (Product) -> Unit) {
    fun d(v: Double?) = v?.takeIf { it > 0 }?.let { Calculator.fmt(it, 4).replace(",", "") } ?: ""
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var active by remember { mutableStateOf(initial.active) }
    var formulation by remember { mutableStateOf(initial.formulation) }
    var registration by remember { mutableStateOf(initial.registration) }
    var crop by remember { mutableStateOf(initial.crop) }
    var target by remember { mutableStateOf(initial.target) }
    var doseMin by remember { mutableStateOf(d(initial.doseMin)) }
    var doseMax by remember { mutableStateOf(d(initial.doseMax)) }
    var unit by remember { mutableStateOf(initial.unit) }
    var method by remember { mutableStateOf(initial.method) }
    var tox by remember { mutableStateOf(initial.toxBand) }
    var phi by remember { mutableStateOf(initial.phiDays?.toString() ?: "") }
    var rei by remember { mutableStateOf(initial.reiHours?.let { Calculator.fmt(it) } ?: "") }
    var maxApps by remember { mutableStateOf(initial.maxApps?.toString() ?: "") }
    var interval by remember { mutableStateOf(initial.intervalDays?.toString() ?: "") }
    var price by remember { mutableStateOf(d(initial.price)) }
    var priceUnit by remember { mutableStateOf(initial.priceUnit) }
    var confirmDel by remember { mutableStateOf(false) }

    val lo = doseMin.toDoubleOrNull() ?: 0.0
    val hi = doseMax.toDoubleOrNull()?.takeIf { it > 0 } ?: lo
    val valid = name.isNotBlank() && lo > 0 && hi >= lo

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nuevo producto" else "Editar producto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelpBox("Copia los datos tal como aparecen en la etiqueta autorizada.")
                TextFieldX("Nombre comercial *", name, { name = it })
                Dropdown("Tipo *", Catalog.categories.map { it.key }, category, { category = it }, display = { k -> Catalog.category(k).let { "${it.emoji} ${it.name}" } })
                TextFieldX("Ingrediente activo y concentración", active, { active = it })
                TextFieldX("Formulación (EC, SC, WG, WP, SL…)", formulation, { formulation = it })
                TextFieldX("Número de registro", registration, { registration = it })
                Dropdown("Cultivo autorizado", listOf("") + Catalog.crops.map { it.name }, crop, { crop = it }, display = { if (it.isBlank()) "Cualquiera (según etiqueta)" else it })
                TextFieldX("Plaga / maleza / enfermedad", target, { target = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Dosis mínima *", doseMin, { doseMin = it }, Modifier.weight(1f))
                    NumberField("Dosis máxima", doseMax, { doseMax = it }, Modifier.weight(1f))
                }
                Dropdown("Unidad de dosis *", Catalog.units, unit, { unit = it })
                TextFieldX("Forma de aplicación / notas de dosis", method, { method = it })
                Dropdown("Banda de color de la etiqueta", listOf("", "Rojo", "Amarillo", "Azul", "Verde"), tox, { tox = it }, display = { if (it.isBlank()) "Sin dato" else it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Int. seguridad", phi, { phi = it.filter(Char::isDigit) }, Modifier.weight(1f), suffix = "días")
                    NumberField("Reentrada", rei, { rei = it }, Modifier.weight(1f), suffix = "h")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Máx. aplicaciones", maxApps, { maxApps = it.filter(Char::isDigit) }, Modifier.weight(1f), suffix = "/ciclo")
                    NumberField("Intervalo mín.", interval, { interval = it.filter(Char::isDigit) }, Modifier.weight(1f), suffix = "días")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Precio", price, { price = it }, Modifier.weight(1f), suffix = "MXN")
                    Dropdown("por", listOf("L", "mL", "kg", "g"), priceUnit, { priceUnit = it }, Modifier.weight(1f))
                }
                if (onDelete != null) TextButton(onClick = { confirmDel = true }) { Text("🗑️ Eliminar producto", color = Danger, fontWeight = FontWeight.Bold) }
                if (!valid) Text("Completa nombre y dosis (la máxima no puede ser menor que la mínima).", color = Danger, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(initial.copy(
                    name = name.trim(), category = category, active = active.trim(), formulation = formulation.trim(), registration = registration.trim(),
                    crop = crop, target = target.trim(), doseMin = lo, doseMax = hi, unit = unit, method = method.trim(), toxBand = tox,
                    phiDays = phi.toIntOrNull(), reiHours = rei.toDoubleOrNull(), maxApps = maxApps.toIntOrNull(), intervalDays = interval.toIntOrNull(),
                    price = price.toDoubleOrNull(), priceUnit = priceUnit
                ))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
    if (confirmDel) AlertDialog(
        onDismissRequest = { confirmDel = false },
        title = { Text("¿Eliminar producto?") },
        text = { Text("Las aplicaciones ya registradas conservan sus datos.") },
        confirmButton = { TextButton(onClick = { confirmDel = false; onDelete?.invoke() }) { Text("Eliminar", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmDel = false }) { Text("Cancelar") } }
    )
}
