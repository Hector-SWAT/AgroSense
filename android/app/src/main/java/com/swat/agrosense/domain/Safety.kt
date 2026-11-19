package com.swat.agrosense.domain

import com.swat.agrosense.data.ApplicationRecord
import com.swat.agrosense.data.Catalog
import com.swat.agrosense.data.Product
import java.util.concurrent.TimeUnit

/** Sugerencias de seguridad y buenas prácticas. Son generales: la etiqueta del producto siempre manda. */
object Safety {

    fun ppe(category: String, formulation: String, toxBand: String, equipment: String): List<String> {
        val list = mutableListOf(
            "Overol de manga larga o traje desechable",
            "Guantes de nitrilo (nunca de tela o piel)",
            "Botas de hule con el pantalón por fuera",
            "Lentes de seguridad o goggles",
            "Gorra o capucha impermeable"
        )
        val f = formulation.uppercase()
        val powder = listOf("WP", "WG", "DP", "SP", "GR", "PH").any { f.contains(it) }
        list += if (powder) "Respirador con filtro para partículas (N95/P100) al pesar el polvo"
        else "Careta facial y mandil impermeable al preparar la mezcla"
        if (toxBand == "Rojo" || toxBand == "Amarillo" || equipment == "turbo" || equipment == "motobomba")
            list += "Respirador de media cara con cartuchos para vapores orgánicos + prefiltro"
        if (toxBand == "Rojo") list += "Traje impermeable completo; nunca aplicar solo, ten a alguien cerca"
        when (category) {
            "rodenticida", "molusquicida" -> list += "Guantes al colocar cebos y cebaderas rotuladas"
            "nematicida" -> list += "Evitar contacto con suelo tratado: botas y guantes hasta terminar"
            "piscicida", "alguicida" -> list += "Botas o vadeadores impermeables si entras al agua"
        }
        if (equipment == "dron") list += "Operador a distancia segura y área despejada de personas"
        return list
    }

    fun beforeApplying(equipment: String): List<String> = listOf(
        "Lee la etiqueta completa y confirma que el cultivo y la plaga estén autorizados.",
        "Calibra el equipo con agua limpia y revisa fugas en boquillas y mangueras.",
        "Prepara la mezcla al aire libre, lejos de pozos, ríos y casas.",
        "Llena el tanque a la mitad con agua, agrega el producto y completa con agua mientras agitas.",
        if (equipment == "dron") "Verifica permisos y que la etiqueta autorice aplicación aérea o con dron."
        else "Ten a la mano agua limpia y jabón para lavarte en caso de contacto."
    )

    val bestTime = listOf(
        "🕕 Mejor horario: temprano (6–10 h) o al atardecer (17–19 h).",
        "🌬️ Viento ideal entre 3 y 10 km/h. No apliques con más de 15 km/h.",
        "🌡️ Temperatura menor a 30 °C y humedad relativa mayor a 50 %.",
        "🌧️ Sin lluvia pronosticada en las próximas 4–6 horas (o lo que indique la etiqueta).",
        "🐝 Evita horas con abejas activas y la floración abierta."
    )

    fun afterApplying(category: String): List<String> =
        Catalog.category(category).care + listOf(
            "Haz el triple lavado del envase, perfóralo y entrégalo a un centro de acopio.",
            "Lava el equipo lejos de fuentes de agua y vierte el enjuague en la parcela tratada.",
            "Báñate con agua y jabón; lava la ropa de trabajo aparte de la ropa de la familia.",
            "Señaliza la parcela y respeta el tiempo de reentrada y el intervalo de seguridad.",
            "No comas, bebas ni fumes hasta lavarte las manos y la cara.",
            "Si alguien presenta mareo, náusea o irritación, retíralo del área y llama al número de emergencia de la etiqueta (en México, SINTOX)."
        )

    /** Alertas a partir del historial de la parcela. */
    fun warnings(product: Product?, plotId: Long?, history: List<ApplicationRecord>, date: Long, dose: Double): List<String> {
        val w = mutableListOf<String>()
        if (product == null) return w
        val cat = Catalog.category(product.category)
        if (cat.restricted) w += "⛔ ${cat.name}: uso restringido. Requiere autorización vigente y seguir estrictamente la etiqueta."
        if (dose > product.doseMax + 1e-9) w += "⛔ La dosis supera el máximo de etiqueta (${Calculator.fmt(product.doseMax)} ${product.unit})."
        if (dose < product.doseMin - 1e-9) w += "⚠️ La dosis está por debajo del mínimo de etiqueta: puede no controlar y generar resistencia."
        if (plotId == null) return w
        val onPlot = history.filter { it.plotId == plotId }
        val same = onPlot.filter { it.productName.equals(product.name, true) }
        product.maxApps?.let { max -> if (same.size >= max) w += "⛔ Ya se registraron ${same.size} aplicaciones de ${product.name} en esta parcela (máximo de etiqueta: $max)." }
        product.intervalDays?.let { iv ->
            same.maxByOrNull { it.date }?.let { last ->
                val days = TimeUnit.MILLISECONDS.toDays(date - last.date)
                if (days < iv) w += "⚠️ Han pasado $days días desde la última aplicación de este producto; la etiqueta pide al menos $iv."
            }
        }
        val lastTwo = onPlot.sortedByDescending { it.date }.take(2)
        if (product.active.isNotBlank() && lastTwo.size == 2 && lastTwo.all { it.active.equals(product.active, true) })
            w += "🔁 Sería la 3.ª aplicación seguida con ${product.active}. Rota el modo de acción para evitar resistencia."
        return w
    }
}
