package com.swat.agrosense.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cliente (productor) que el ingeniero atiende. */
@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Parcela / lote de un cliente, con su ciclo de cultivo. */
@Entity(
    tableName = "plots",
    foreignKeys = [ForeignKey(entity = Client::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clientId")]
)
data class Plot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val name: String,
    val crop: String,
    val variety: String = "",
    val areaHa: Double,
    val cycleStart: Long? = null,   // fecha de siembra/trasplante (ms)
    val cycleEnd: Long? = null,     // cosecha estimada (ms)
    val notes: String = ""
)

/** Uso autorizado de un producto según su etiqueta (producto + cultivo + plaga + dosis). */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: String = "",
    val category: String,            // clave de Catalog.categories
    val crop: String = "",           // cultivo autorizado ("" = cualquiera según etiqueta)
    val target: String = "",         // plaga / maleza / enfermedad
    val scientific: String = "",
    val doseMin: Double,
    val doseMax: Double,
    val unit: String,                // una de Catalog.units
    val method: String = "",         // forma de aplicación / notas de dosis
    val formulation: String = "",
    val registration: String = "",
    val toxBand: String = "",        // Rojo / Amarillo / Azul / Verde
    val phiDays: Int? = null,        // intervalo de seguridad (días a cosecha)
    val reiHours: Double? = null,    // reentrada (horas)
    val maxApps: Int? = null,        // aplicaciones máximas por ciclo
    val intervalDays: Int? = null,   // días mínimos entre aplicaciones
    val price: Double? = null,       // precio por priceUnit (MXN)
    val priceUnit: String = "L",
    val source: String = "Usuario"   // "Catálogo" o "Usuario"
)

/** Registro de una aplicación realizada. Guarda una copia de los datos clave para trazabilidad. */
@Entity(tableName = "applications", indices = [Index("clientId"), Index("plotId")])
data class ApplicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long? = null,
    val plotId: Long? = null,
    val clientName: String = "",
    val plotName: String = "",
    val crop: String,
    val category: String,
    val productId: Long? = null,
    val productName: String,
    val active: String = "",
    val target: String = "",
    val date: Long,
    val areaHa: Double,
    val dose: Double,
    val doseUnit: String,
    val productTotal: Double,        // en unidad base (mL o g)
    val productBaseUnit: String,     // "mL" o "g"
    val waterTotalL: Double?,
    val waterLHa: Double?,
    val tanks: Int?,
    val tankL: Double?,
    val equipment: String,
    val durationMin: Double?,
    val cost: Double?,
    val operator: String = "",
    val weather: String = "",
    val ppe: String = "",            // EPP confirmado, separado por "|"
    val phiDays: Int? = null,
    val reiHours: Double? = null,
    val harvestAllowedAt: Long? = null,
    val reentryAt: Long? = null,
    val notes: String = ""
)
