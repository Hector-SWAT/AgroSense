# AgroSense — App de cálculo de plaguicidas (Android + iOS)

App nativa para ingenieros agrónomos: calcula dosis, agua, tanques y tiempo de aplicación para 51 cultivos (tus 50 + chícharo, que viene en el Excel) y 15 tipos de plaguicida, lleva el control de **clientes → parcelas → aplicaciones** y guarda todo en la memoria interna del teléfono (funciona sin internet).

```
AgroSense/
├── android/          Proyecto Android Studio (Kotlin + Jetpack Compose + Room)
├── ios/              Proyecto Xcode (SwiftUI + SwiftData, iOS 17+)
├── datos/            Datos_Completos.xlsx (catálogo original)
└── gen_catalog.py    Regenera Catalog.kt y Catalog.swift desde el Excel
```

## Qué hace

| Función | Detalle |
|---|---|
| Asistente de 5 pasos | Cliente → Cultivo → Plaga y producto → Área/equipo/dosis → Resultado. Cada paso trae un recuadro de ayuda para usarla sin manual. |
| Cálculo | Producto total, agua total, tanques llenos + último tanque parcial, producto por tanque, concentración por litro, tiempo (aplicación + recargas) y costo. |
| Unidades de dosis | L/ha, mL/ha, kg/ha, g/ha, mL/100 L, g/100 L, mL/L, g/L, % v/v y % p/v (1 % = 10 mL o g por litro de caldo). |
| Por cultivo | Volumen de agua orientativo (L/ha) y equipo sugerido para cada uno de los 51 cultivos. |
| Equipos | Mochila manual, mochila motorizada, motobomba, tractor con barra, turboatomizador y dron (tanque, gasto y recarga editables). |
| Seguridad | Equipo de protección según tipo, formulación y banda toxicológica (lista para marcar), horario y clima recomendados, pasos previos y cuidados posteriores. |
| Alertas | Dosis fuera del rango de etiqueta, máximo de aplicaciones por ciclo, intervalo mínimo entre aplicaciones, 3.ª aplicación seguida del mismo ingrediente activo (resistencia), categorías de uso restringido. |
| Fechas | Reentrada (horas) y cosecha permitida (intervalo de seguridad) por aplicación; avisos activos en Inicio. |
| Clientes | Clientes con parcelas, ciclo de cultivo (siembra → cosecha), línea de tiempo de aplicaciones, costo acumulado e ingredientes activos usados. |
| Catálogo | 49 usos de etiqueta importados de `Datos_Completos.xlsx` + productos que agregue el ingeniero. |
| Respaldo | JSON (mismo formato en Android e iOS), exportación CSV para Excel y compartir reporte por WhatsApp/correo. |
| Diseño | Botones con degradado RGB y borde arcoíris animado; ícono AgroSense con sombrero vaquero. |

## Android (Android Studio)

1. Abre Android Studio → **Open** → carpeta `android/`.
2. Deja que sincronice Gradle (descarga AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1).
3. Ejecuta en un emulador o teléfono (Android 8.0 / API 26 o superior).
4. APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
5. Pruebas del motor de cálculo: `./gradlew test` (archivo `app/src/test/.../CalculatorTest.kt`).

Si Android Studio propone actualizar AGP o dependencias, puedes aceptar.

## iOS (Xcode, requiere Mac)

1. Abre `ios/AgroSense.xcodeproj` en **Xcode 16 o superior**.
2. En *Signing & Capabilities* elige tu Team (cuenta de Apple).
3. Ejecuta en simulador o iPhone con iOS 17+.

Alternativa si el proyecto no abre: `brew install xcodegen && cd ios && xcodegen` (usa `project.yml`).

## Actualizar el catálogo

Edita `datos/Datos_Completos.xlsx` (mismas columnas) y ejecuta:

```bash
python3 gen_catalog.py datos/Datos_Completos.xlsx \
  android/app/src/main/java/com/swat/agrosense/data/Catalog.kt \
  ios/AgroSense/Catalog.swift
```

En la app, **Productos → Restaurar catálogo** agrega los usos nuevos sin borrar los del usuario.

## Importante

- Las dosis del catálogo vienen del Excel. Los volúmenes de agua por cultivo, gastos de equipo y tiempos son **valores orientativos editables**; usa siempre los de tu calibración.
- Intervalo de seguridad, reentrada, máximo de aplicaciones y banda toxicológica no vienen en el Excel: captúralos en cada producto desde su etiqueta para activar fechas y alertas.
- La app no sustituye la etiqueta ni el registro vigente (COFEPRIS/SENASICA).
