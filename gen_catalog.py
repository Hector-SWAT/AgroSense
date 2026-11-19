"""Genera el catálogo compartido de AgroSense (Kotlin + Swift) a partir de Datos_Completos.xlsx.
Uso: python3 -I gen_catalog.py <xlsx> <salida_kotlin> <salida_swift>
"""
import re, sys, json
import openpyxl

XLSX, OUT_KT, OUT_SW = sys.argv[1:4]

# ---------------- Cultivos ----------------
# (nombre, grupo, emoji, agua_min L/ha, agua_max L/ha, equipo sugerido)
G, I, H, F, B = "Granos básicos", "Industriales y oleaginosas", "Hortalizas", "Frutales", "Berries y vid"
CROPS = [
    ("Maíz (blanco y amarillo)", G, "🌽", 150, 300, "tractor"),
    ("Frijol", G, "🫘", 150, 300, "mochila_motor"),
    ("Sorgo", G, "🌾", 150, 300, "tractor"),
    ("Trigo", G, "🌾", 150, 300, "tractor"),
    ("Arroz", G, "🌾", 150, 300, "dron"),
    ("Cebada", G, "🌾", 150, 300, "tractor"),
    ("Avena", G, "🌾", 150, 300, "tractor"),
    ("Centeno", G, "🌾", 150, 300, "tractor"),
    ("Caña de azúcar", I, "🎋", 200, 400, "tractor"),
    ("Agave (azul y otros)", I, "🌵", 200, 400, "mochila_motor"),
    ("Café", I, "☕", 400, 800, "mochila_motor"),
    ("Cacao", I, "🍫", 400, 800, "mochila_motor"),
    ("Tabaco", I, "🍂", 200, 400, "tractor"),
    ("Algodón", I, "☁️", 150, 300, "tractor"),
    ("Soya", I, "🫛", 150, 300, "tractor"),
    ("Cártamo", I, "🌼", 150, 300, "tractor"),
    ("Ajonjolí", I, "🌱", 150, 300, "tractor"),
    ("Girasol", I, "🌻", 150, 300, "tractor"),
    ("Canola", I, "🌼", 150, 300, "tractor"),
    ("Chile (jalapeño, serrano, poblano, etc.)", H, "🌶️", 300, 600, "mochila_manual"),
    ("Tomate (jitomate)", H, "🍅", 300, 600, "mochila_manual"),
    ("Papa", H, "🥔", 300, 500, "tractor"),
    ("Cebolla", H, "🧅", 300, 500, "mochila_manual"),
    ("Ajo", H, "🧄", 300, 500, "mochila_manual"),
    ("Zanahoria", H, "🥕", 300, 500, "mochila_manual"),
    ("Lechuga", H, "🥬", 300, 500, "mochila_manual"),
    ("Espinaca", H, "🥬", 300, 500, "mochila_manual"),
    ("Acelga", H, "🥬", 300, 500, "mochila_manual"),
    ("Brócoli", H, "🥦", 300, 600, "mochila_manual"),
    ("Coliflor", H, "🥦", 300, 600, "mochila_manual"),
    ("Repollo", H, "🥬", 300, 600, "mochila_manual"),
    ("Chícharo", H, "🫛", 300, 500, "mochila_manual"),
    ("Pepino", H, "🥒", 300, 600, "mochila_manual"),
    ("Calabacita", H, "🥒", 300, 600, "mochila_manual"),
    ("Calabaza", H, "🎃", 300, 600, "mochila_manual"),
    ("Melón", H, "🍈", 300, 600, "mochila_motor"),
    ("Sandía", H, "🍉", 300, 600, "mochila_motor"),
    ("Fresa", B, "🍓", 400, 800, "mochila_manual"),
    ("Frambuesa", B, "🫐", 500, 1000, "mochila_motor"),
    ("Zarzamora", B, "🫐", 500, 1000, "mochila_motor"),
    ("Arándano", B, "🫐", 500, 1000, "mochila_motor"),
    ("Uva", B, "🍇", 400, 1000, "turbo"),
    ("Naranja", F, "🍊", 1000, 2500, "turbo"),
    ("Limón", F, "🍋", 1000, 2500, "turbo"),
    ("Toronja", F, "🍊", 1000, 2500, "turbo"),
    ("Mandarina", F, "🍊", 1000, 2500, "turbo"),
    ("Mango", F, "🥭", 1000, 2500, "turbo"),
    ("Aguacate", F, "🥑", 1000, 3000, "turbo"),
    ("Plátano", F, "🍌", 300, 600, "motobomba"),
    ("Piña", F, "🍍", 400, 1000, "motobomba"),
    ("Papaya", F, "🍈", 500, 1000, "motobomba"),
]
CROP_ALIAS = {"Jitomate": "Tomate (jitomate)", "Chile serrano": "Chile (jalapeño, serrano, poblano, etc.)"}

# ---------------- Categorías de plaguicida ----------------
BASE_CARE = []
CATS = [
    ("insecticida", "Insecticidas", "Controlan insectos", "🐛", "#E53935", False,
     ["Protege a las abejas: no apliques en floración abierta ni con abejas activas.", "Revisa el envés de las hojas 3 a 5 días después para evaluar el control."]),
    ("herbicida", "Herbicidas", "Controlan malezas / plantas indeseadas", "🌿", "#43A047", False,
     ["Evita la deriva hacia cultivos vecinos sensibles; no apliques con viento.", "Lava a fondo el equipo: los residuos de herbicida dañan otros cultivos."]),
    ("fungicida", "Fungicidas", "Controlan hongos", "🍄", "#8E24AA", False,
     ["Rota grupos de modo de acción (FRAC) para evitar resistencia.", "Funcionan mejor de forma preventiva; vigila humedad y rocío."]),
    ("bactericida", "Bactericidas", "Controlan bacterias", "🦠", "#00897B", False,
     ["Los productos cúpricos pueden quemar hojas con calor: aplica en horas frescas.", "Desinfecta herramientas de poda para no dispersar la bacteria."]),
    ("nematicida", "Nematicidas", "Controlan nematodos", "🪱", "#6D4C41", False,
     ["Aplica al suelo húmedo y sigue la etiqueta sobre riego posterior.", "Evita el contacto con el suelo tratado sin botas y guantes."]),
    ("acaricida", "Acaricidas", "Controlan ácaros", "🕷️", "#D81B60", False,
     ["Asegura buena cobertura del envés de la hoja.", "Alterna ingredientes activos: los ácaros generan resistencia rápido."]),
    ("molusquicida", "Molusquicidas", "Controlan caracoles y babosas", "🐌", "#7CB342", False,
     ["Coloca cebos lejos de mascotas, aves y niños.", "Recoge los cebos sobrantes y los organismos muertos."]),
    ("rodenticida", "Rodenticidas", "Controlan roedores", "🐀", "#5D4037", False,
     ["Usa cebaderas cerradas y rotuladas; nunca cebo suelto.", "Recoge roedores muertos con guantes y entiérralos o deséchalos según la etiqueta.", "Mantén a niños, mascotas y ganado lejos de las cebaderas."]),
    ("avicida", "Avicidas", "Controlan aves", "🐦", "#1E88E5", True,
     ["Uso restringido: aplica solo con autorización vigente y conforme a la etiqueta.", "Prefiere métodos no químicos (mallas, ahuyentadores) cuando sea posible."]),
    ("piscicida", "Piscicidas", "Controlan peces", "🐟", "#039BE5", True,
     ["Uso restringido en cuerpos de agua: requiere autorización.", "No permitas consumo de agua ni pesca en la zona hasta que la etiqueta lo indique."]),
    ("larvicida", "Larvicidas", "Controlan larvas", "🐜", "#FB8C00", False,
     ["Aplica cuando las larvas son pequeñas: el control es mayor.", "Cuida fuentes de agua cercanas si el producto es para larvas acuáticas."]),
    ("ovicida", "Ovicidas", "Destruyen huevos de insectos/ácaros", "🥚", "#FDD835", False,
     ["Aplica al inicio de la ovipostura; no controlan adultos.", "Combina con monitoreo para repetir solo si la etiqueta lo permite."]),
    ("adulticida", "Adulticidas", "Controlan insectos adultos", "🦟", "#F4511E", False,
     ["Aplica en las horas de mayor actividad del insecto, respetando a polinizadores.", "Combina con trampas para medir la población antes y después."]),
    ("alguicida", "Alguicidas", "Controlan algas", "💧", "#00ACC1", False,
     ["Dosifica sobre el volumen real de agua (canales, estanques, sistemas de riego).", "Evita aplicar con peces o fauna acuática salvo que la etiqueta lo permita."]),
    ("virucida", "Virucidas", "Controlan virus", "🧬", "#3949AB", False,
     ["En campo no hay cura para plantas ya infectadas: el manejo es preventivo.", "Controla insectos vectores y elimina plantas enfermas."]),
]
TYPE_TO_CAT = {"Herbicida": "herbicida", "Insecticida": "insecticida", "Acaricida": "acaricida",
               "Fungicida": "fungicida", "Bactericida": "bactericida", "Nematicida": "nematicida"}

# ---------------- Equipos ----------------
# key, nombre, emoji, tanque L, gasto L/min, recarga min, agua fija L/ha (0 = usar la del cultivo)
EQUIP = [
    ("mochila_manual", "Mochila manual", "🎒", 20, 0.8, 6, 0),
    ("mochila_motor", "Mochila motorizada", "⚙️", 25, 2.5, 6, 0),
    ("motobomba", "Motobomba con manguera", "🚿", 200, 10.0, 15, 0),
    ("tractor", "Tractor con barra", "🚜", 400, 25.0, 20, 0),
    ("turbo", "Turboatomizador", "🌀", 1500, 40.0, 25, 0),
    ("dron", "Dron agrícola", "🛸", 20, 5.0, 4, 20),
]

UNITS = ["L/ha", "mL/ha", "kg/ha", "g/ha", "mL/100L", "g/100L", "mL/L", "g/L", "% v/v", "% p/v"]

# ---------------- Catálogo de etiqueta desde Excel ----------------
def parse_dose(s):
    s0 = s.strip()
    m = re.match(r"^\s*([\d.,]+)\s*(?:[–-]\s*([\d.,]+))?\s*(L|mL|kg|g)\s*/\s*(ha|L de agua|L)\b(.*)$", s0)
    if not m:
        raise ValueError("Dosis no reconocida: " + s0)
    a, b, u, base, rest = m.groups()
    num = lambda x: float(x.replace(",", ""))
    lo = num(a); hi = num(b) if b else lo
    unit = f"{u}/ha" if base == "ha" else f"{u}/L"
    note = rest.strip(" ,.;")
    return lo, hi, unit, note

wb = openpyxl.load_workbook(XLSX, data_only=True)
ws = wb.active
rows, header = [], None
for r in ws.iter_rows(values_only=True):
    vals = list(r)
    if header is None:
        if "ID" in vals:
            start = vals.index("ID"); header = vals[start:start + 9]
        continue
    v = vals[start:start + 9]
    if v[0] is None:
        continue
    _id, crop, org, common, sci, prod, active, typ, dose = v
    lo, hi, unit, note = parse_dose(str(dose))
    crop = CROP_ALIAS.get(crop.strip(), crop.strip())
    assert crop in [c[0] for c in CROPS], crop
    rows.append(dict(name=str(prod).split(";")[0].strip().rstrip("."), active=str(active).strip(),
                     cat=TYPE_TO_CAT[typ.strip()], crop=crop, organism=org.strip(), target=common.strip(),
                     sci=str(sci).strip().rstrip("."), lo=lo, hi=hi, unit=unit, note=note, original=str(dose).strip()))
print(f"{len(rows)} usos de etiqueta importados", file=sys.stderr)

def kstr(s): return json.dumps(s, ensure_ascii=False)
def fnum(x): return repr(float(x))

# ---------------- Kotlin ----------------
kt = ["// ARCHIVO GENERADO por gen_catalog.py a partir de Datos_Completos.xlsx. No editar a mano.",
      "package com.swat.agrosense.data", "",
      "data class CropInfo(val name: String, val group: String, val emoji: String, val waterMin: Int, val waterMax: Int, val equipment: String)",
      "data class CategoryInfo(val key: String, val name: String, val controls: String, val emoji: String, val color: Long, val restricted: Boolean, val care: List<String>)",
      "data class EquipmentInfo(val key: String, val name: String, val emoji: String, val tankL: Double, val flowLMin: Double, val refillMin: Double, val fixedWaterLHa: Double)",
      "data class LabelSeed(val name: String, val active: String, val category: String, val crop: String, val organism: String, val target: String, val scientific: String, val doseMin: Double, val doseMax: Double, val unit: String, val note: String, val original: String)",
      "", "object Catalog {",
      "    val units = listOf(" + ", ".join(kstr(u) for u in UNITS) + ")", "",
      "    val crops = listOf("]
kt += [f"        CropInfo({kstr(n)}, {kstr(g)}, {kstr(e)}, {a}, {b}, {kstr(q)})," for n, g, e, a, b, q in CROPS]
kt += ["    )", "", "    val categories = listOf("]
for k, n, c, e, col, r, care in CATS:
    kt.append(f"        CategoryInfo({kstr(k)}, {kstr(n)}, {kstr(c)}, {kstr(e)}, 0xFF{col[1:]}, {str(r).lower()}, listOf({', '.join(kstr(x) for x in care)})),")
kt += ["    )", "", "    val equipment = listOf("]
kt += [f"        EquipmentInfo({kstr(k)}, {kstr(n)}, {kstr(e)}, {fnum(t)}, {fnum(fl)}, {fnum(rf)}, {fnum(w)})," for k, n, e, t, fl, rf, w in EQUIP]
kt += ["    )", "", "    val labelSeeds = listOf("]
for x in rows:
    kt.append("        LabelSeed(" + ", ".join([kstr(x['name']), kstr(x['active']), kstr(x['cat']), kstr(x['crop']), kstr(x['organism']),
              kstr(x['target']), kstr(x['sci']), fnum(x['lo']), fnum(x['hi']), kstr(x['unit']), kstr(x['note']), kstr(x['original'])]) + "),")
kt += ["    )", "",
       "    fun crop(name: String) = crops.firstOrNull { it.name == name }",
       "    fun category(key: String) = categories.firstOrNull { it.key == key } ?: categories.first()",
       "    fun equipment(key: String) = equipment.firstOrNull { it.key == key } ?: equipment.first()",
       "}", ""]
open(OUT_KT, "w", encoding="utf-8").write("\n".join(kt))

# ---------------- Swift ----------------
sw = ["// ARCHIVO GENERADO por gen_catalog.py a partir de Datos_Completos.xlsx. No editar a mano.",
      "import Foundation", "",
      "struct CropInfo: Hashable { let name: String; let group: String; let emoji: String; let waterMin: Int; let waterMax: Int; let equipment: String }",
      "struct CategoryInfo: Hashable { let key: String; let name: String; let controls: String; let emoji: String; let color: UInt32; let restricted: Bool; let care: [String] }",
      "struct EquipmentInfo: Hashable { let key: String; let name: String; let emoji: String; let tankL: Double; let flowLMin: Double; let refillMin: Double; let fixedWaterLHa: Double }",
      "struct LabelSeed { let name: String; let active: String; let category: String; let crop: String; let organism: String; let target: String; let scientific: String; let doseMin: Double; let doseMax: Double; let unit: String; let note: String; let original: String }",
      "", "enum Catalog {",
      "    static let units: [String] = [" + ", ".join(kstr(u) for u in UNITS) + "]", "",
      "    static let crops: [CropInfo] = ["]
sw += [f"        CropInfo(name: {kstr(n)}, group: {kstr(g)}, emoji: {kstr(e)}, waterMin: {a}, waterMax: {b}, equipment: {kstr(q)})," for n, g, e, a, b, q in CROPS]
sw += ["    ]", "", "    static let categories: [CategoryInfo] = ["]
for k, n, c, e, col, r, care in CATS:
    sw.append(f"        CategoryInfo(key: {kstr(k)}, name: {kstr(n)}, controls: {kstr(c)}, emoji: {kstr(e)}, color: 0x{col[1:]}, restricted: {str(r).lower()}, care: [{', '.join(kstr(x) for x in care)}]),")
sw += ["    ]", "", "    static let equipment: [EquipmentInfo] = ["]
sw += [f"        EquipmentInfo(key: {kstr(k)}, name: {kstr(n)}, emoji: {kstr(e)}, tankL: {fnum(t)}, flowLMin: {fnum(fl)}, refillMin: {fnum(rf)}, fixedWaterLHa: {fnum(w)})," for k, n, e, t, fl, rf, w in EQUIP]
sw += ["    ]", "", "    static let labelSeeds: [LabelSeed] = ["]
for x in rows:
    sw.append(f"        LabelSeed(name: {kstr(x['name'])}, active: {kstr(x['active'])}, category: {kstr(x['cat'])}, crop: {kstr(x['crop'])}, organism: {kstr(x['organism'])}, target: {kstr(x['target'])}, scientific: {kstr(x['sci'])}, doseMin: {fnum(x['lo'])}, doseMax: {fnum(x['hi'])}, unit: {kstr(x['unit'])}, note: {kstr(x['note'])}, original: {kstr(x['original'])}),")
sw += ["    ]", "",
       "    static func crop(_ name: String) -> CropInfo? { crops.first { $0.name == name } }",
       "    static func category(_ key: String) -> CategoryInfo { categories.first { $0.key == key } ?? categories[0] }",
       "    static func equipment(_ key: String) -> EquipmentInfo { equipment.first { $0.key == key } ?? equipment[0] }",
       "}", ""]
open(OUT_SW, "w", encoding="utf-8").write("\n".join(sw))
