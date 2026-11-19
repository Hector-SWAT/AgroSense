package com.swat.agrosense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.swat.agrosense.data.AppDatabase
import com.swat.agrosense.data.ApplicationRecord
import com.swat.agrosense.data.Client
import com.swat.agrosense.data.Plot
import com.swat.agrosense.data.Product
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val dao = db.dao()

    val clients: StateFlow<List<Client>> = dao.clients().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val plots: StateFlow<List<Plot>> = dao.plots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val products: StateFlow<List<Product>> = dao.products().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val applications: StateFlow<List<ApplicationRecord>> = dao.applications().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { if (dao.productCount() == 0) dao.insertProducts(AppDatabase.seedProducts()) }
    }

    fun saveClient(c: Client, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        if (c.id == 0L) onSaved(dao.insertClient(c)) else { dao.updateClient(c); onSaved(c.id) }
    }
    fun deleteClient(c: Client) = viewModelScope.launch { dao.deleteClient(c) }

    fun savePlot(p: Plot, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        if (p.id == 0L) onSaved(dao.insertPlot(p)) else { dao.updatePlot(p); onSaved(p.id) }
    }
    fun deletePlot(p: Plot) = viewModelScope.launch { dao.deletePlot(p) }

    fun saveProduct(p: Product, onSaved: (Long) -> Unit = {}) = viewModelScope.launch { onSaved(dao.upsertProduct(p)) }
    fun deleteProduct(p: Product) = viewModelScope.launch { dao.deleteProduct(p) }
    fun restoreCatalog() = viewModelScope.launch {
        val existing = dao.productsOnce().filter { it.source == "Catálogo" }.map { it.name + it.crop + it.target }.toSet()
        dao.insertProducts(AppDatabase.seedProducts().filter { (it.name + it.crop + it.target) !in existing })
    }

    fun saveApplication(a: ApplicationRecord, onSaved: () -> Unit) = viewModelScope.launch { dao.insertApplication(a); onSaved() }
    fun deleteApplication(a: ApplicationRecord) = viewModelScope.launch { dao.deleteApplication(a) }

    // ---------- Respaldo JSON ----------
    suspend fun exportJson(): String {
        val o = JSONObject()
        o.put("app", "AgroSense"); o.put("version", 1); o.put("exportedAt", System.currentTimeMillis())
        o.put("clients", JSONArray(dao.clientsOnce().map { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("phone", c.phone).put("location", c.location).put("notes", c.notes).put("createdAt", c.createdAt)
        }))
        o.put("plots", JSONArray(dao.plotsOnce().map { p ->
            JSONObject().put("id", p.id).put("clientId", p.clientId).put("name", p.name).put("crop", p.crop).put("variety", p.variety)
                .put("areaHa", p.areaHa).put("cycleStart", p.cycleStart ?: JSONObject.NULL).put("cycleEnd", p.cycleEnd ?: JSONObject.NULL).put("notes", p.notes)
        }))
        o.put("products", JSONArray(dao.productsOnce().map { p ->
            JSONObject().put("id", p.id).put("name", p.name).put("active", p.active).put("category", p.category).put("crop", p.crop)
                .put("target", p.target).put("scientific", p.scientific).put("doseMin", p.doseMin).put("doseMax", p.doseMax).put("unit", p.unit)
                .put("method", p.method).put("formulation", p.formulation).put("registration", p.registration).put("toxBand", p.toxBand)
                .put("phiDays", p.phiDays ?: JSONObject.NULL).put("reiHours", p.reiHours ?: JSONObject.NULL).put("maxApps", p.maxApps ?: JSONObject.NULL)
                .put("intervalDays", p.intervalDays ?: JSONObject.NULL).put("price", p.price ?: JSONObject.NULL).put("priceUnit", p.priceUnit).put("source", p.source)
        }))
        o.put("applications", JSONArray(dao.applicationsOnce().map { a ->
            JSONObject().put("id", a.id).put("clientId", a.clientId ?: JSONObject.NULL).put("plotId", a.plotId ?: JSONObject.NULL)
                .put("clientName", a.clientName).put("plotName", a.plotName).put("crop", a.crop).put("category", a.category)
                .put("productId", a.productId ?: JSONObject.NULL).put("productName", a.productName).put("active", a.active).put("target", a.target)
                .put("date", a.date).put("areaHa", a.areaHa).put("dose", a.dose).put("doseUnit", a.doseUnit).put("productTotal", a.productTotal)
                .put("productBaseUnit", a.productBaseUnit).put("waterTotalL", a.waterTotalL ?: JSONObject.NULL).put("waterLHa", a.waterLHa ?: JSONObject.NULL)
                .put("tanks", a.tanks ?: JSONObject.NULL).put("tankL", a.tankL ?: JSONObject.NULL).put("equipment", a.equipment)
                .put("durationMin", a.durationMin ?: JSONObject.NULL).put("cost", a.cost ?: JSONObject.NULL).put("operator", a.operator)
                .put("weather", a.weather).put("ppe", a.ppe).put("phiDays", a.phiDays ?: JSONObject.NULL).put("reiHours", a.reiHours ?: JSONObject.NULL)
                .put("harvestAllowedAt", a.harvestAllowedAt ?: JSONObject.NULL).put("reentryAt", a.reentryAt ?: JSONObject.NULL).put("notes", a.notes)
        }))
        return o.toString(2)
    }

    /** Reemplaza todos los datos locales por los del respaldo. Lanza excepción si el archivo no es válido. */
    suspend fun importJson(text: String) {
        val o = JSONObject(text)
        require(o.optString("app") == "AgroSense") { "El archivo no es un respaldo de AgroSense." }
        fun JSONObject.lN(k: String) = if (isNull(k) || !has(k)) null else getLong(k)
        fun JSONObject.dN(k: String) = if (isNull(k) || !has(k)) null else getDouble(k)
        fun JSONObject.iN(k: String) = if (isNull(k) || !has(k)) null else getInt(k)
        fun JSONArray.objs() = (0 until length()).map { getJSONObject(it) }
        val c = o.getJSONArray("clients").objs().map { Client(it.getLong("id"), it.getString("name"), it.optString("phone"), it.optString("location"), it.optString("notes"), it.optLong("createdAt")) }
        val p = o.getJSONArray("plots").objs().map { Plot(it.getLong("id"), it.getLong("clientId"), it.getString("name"), it.getString("crop"), it.optString("variety"), it.getDouble("areaHa"), it.lN("cycleStart"), it.lN("cycleEnd"), it.optString("notes")) }
        val pr = o.getJSONArray("products").objs().map {
            Product(it.getLong("id"), it.getString("name"), it.optString("active"), it.getString("category"), it.optString("crop"), it.optString("target"),
                it.optString("scientific"), it.getDouble("doseMin"), it.getDouble("doseMax"), it.getString("unit"), it.optString("method"), it.optString("formulation"),
                it.optString("registration"), it.optString("toxBand"), it.iN("phiDays"), it.dN("reiHours"), it.iN("maxApps"), it.iN("intervalDays"),
                it.dN("price"), it.optString("priceUnit", "L"), it.optString("source", "Usuario"))
        }
        val a = o.getJSONArray("applications").objs().map {
            ApplicationRecord(it.getLong("id"), it.lN("clientId"), it.lN("plotId"), it.optString("clientName"), it.optString("plotName"), it.getString("crop"),
                it.getString("category"), it.lN("productId"), it.getString("productName"), it.optString("active"), it.optString("target"), it.getLong("date"),
                it.getDouble("areaHa"), it.getDouble("dose"), it.getString("doseUnit"), it.getDouble("productTotal"), it.getString("productBaseUnit"),
                it.dN("waterTotalL"), it.dN("waterLHa"), it.iN("tanks"), it.dN("tankL"), it.optString("equipment"), it.dN("durationMin"), it.dN("cost"),
                it.optString("operator"), it.optString("weather"), it.optString("ppe"), it.iN("phiDays"), it.dN("reiHours"), it.lN("harvestAllowedAt"),
                it.lN("reentryAt"), it.optString("notes"))
        }
        db.withTransaction {
            dao.clearApplications(); dao.clearPlots(); dao.clearClients(); dao.clearProducts()
            dao.insertClients(c); dao.insertPlots(p); dao.insertProducts(pr); dao.insertApplications(a)
        }
    }

    fun exportCsv(): String {
        val df = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val header = listOf("Fecha", "Cliente", "Parcela", "Cultivo", "Categoría", "Plaga/objetivo", "Producto", "Ingrediente activo", "Superficie_ha",
            "Dosis", "Unidad_dosis", "Producto_total", "Unidad", "Agua_L", "Tanques", "Equipo", "Duración_min", "Costo_MXN", "Responsable",
            "Clima", "EPP", "Cosecha_permitida", "Observaciones")
        val rows = applications.value.map { a ->
            listOf(df.format(a.date), a.clientName, a.plotName, a.crop, a.category, a.target, a.productName, a.active, a.areaHa, a.dose, a.doseUnit,
                a.productTotal, a.productBaseUnit, a.waterTotalL, a.tanks, a.equipment, a.durationMin, a.cost, a.operator, a.weather,
                a.ppe.replace("|", "; "), a.harvestAllowedAt?.let { df.format(it) }, a.notes)
        }
        return "﻿" + (listOf(header) + rows).joinToString("\r\n") { r -> r.joinToString(",") { "\"" + (it?.toString() ?: "").replace("\"", "\"\"") + "\"" } }
    }
}
