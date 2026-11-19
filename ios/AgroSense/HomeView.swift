import SwiftUI
import SwiftData

struct HomeView: View {
    @Environment(Router.self) private var router
    @Query(sort: \Client.name) private var clients: [Client]
    @Query(sort: \ApplicationRecord.date, order: .reverse) private var apps: [ApplicationRecord]
    @Query private var products: [Product]

    private var totalArea: Double { apps.reduce(0.0) { $0 + $1.areaHa } }
    private var totalCost: Double { apps.reduce(0.0) { $0 + ($1.cost ?? 0) } }
    private var activeAlerts: [ApplicationRecord] {
        let now = Date()
        return Array(apps.filter { ($0.reentryAt ?? .distantPast) > now || ($0.harvestAllowedAt ?? .distantPast) > now }.prefix(6))
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                HStack(spacing: 14) {
                    Image("Logo").resizable().scaledToFit().frame(width: 64, height: 64).clipShape(RoundedRectangle(cornerRadius: 16))
                    VStack(alignment: .leading) {
                        Text("AgroSense").font(.title.weight(.heavy)).foregroundStyle(.white)
                        Text("Cálculo y control de aplicaciones de plaguicidas").font(.caption).foregroundStyle(.white.opacity(0.9))
                    }
                    Spacer()
                }
                .padding(18)
                .background(LinearGradient(colors: [.agGreenDark, Color(hex: 0x218354), Color(hex: 0x2979FF)], startPoint: .leading, endPoint: .trailing))
                .clipShape(RoundedRectangle(cornerRadius: 22))

                HelpBox(text: "¿Por dónde empiezo? Toca \"Nueva aplicación\" y la app te guiará paso a paso: cliente → cultivo → plaga y producto → área → resultado.")

                HStack(spacing: 12) {
                    Button { router.newApplication() } label: { BigTile(emoji: "🧮", title: "Nueva aplicación", subtitle: "Dosis, agua y tiempo", colors: Rgb.green) }
                    Button { router.tab = 1 } label: { BigTile(emoji: "👨‍🌾", title: "Clientes", subtitle: "\(clients.count) registrados", colors: Rgb.blue) }
                }.buttonStyle(.plain)
                HStack(spacing: 12) {
                    Button { router.tab = 3 } label: { BigTile(emoji: "🧴", title: "Productos", subtitle: "\(products.count) usos de etiqueta", colors: Rgb.orange) }
                    Button { router.tab = 4 } label: { BigTile(emoji: "📋", title: "Historial", subtitle: "\(apps.count) aplicaciones", colors: Rgb.purple) }
                }.buttonStyle(.plain)

                HStack(spacing: 10) {
                    StatCard(label: "Superficie", value: Calculator.fmt(totalArea) + " ha", emoji: "🌾")
                    StatCard(label: "Costo", value: money(totalCost), emoji: "💲")
                }

                let now = Date()
                let active = activeAlerts
                SectionCard(title: "Avisos activos", emoji: "⏳") {
                    if active.isEmpty { Text("No hay parcelas en periodo de reentrada ni de seguridad.").foregroundStyle(Color.agMuted) }
                    ForEach(active) { a in
                        NavigationLink { ApplicationDetailView(app: a) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("\(Catalog.crop(a.crop)?.emoji ?? "🌱") \(a.plotName.isEmpty ? a.crop.short : a.plotName) · \(a.clientName.isEmpty ? "Sin cliente" : a.clientName)").fontWeight(.semibold)
                                if let r = a.reentryAt, r > now { Text("🚷 No entrar hasta: \(dateTimeFmt.string(from: r))").font(.footnote).foregroundStyle(Color.agDanger) }
                                if let h = a.harvestAllowedAt, h > now { Text("🧺 Cosechar a partir de: \(dateFmt.string(from: h))").font(.footnote).foregroundStyle(Color.agWarn) }
                            }
                        }.buttonStyle(.plain)
                        Divider()
                    }
                }

                SectionCard(title: "Últimas aplicaciones", emoji: "🕘") {
                    if apps.isEmpty { Text("Todavía no hay aplicaciones. ¡Registra la primera!").foregroundStyle(Color.agMuted) }
                    ForEach(apps.prefix(5)) { a in
                        NavigationLink { ApplicationDetailView(app: a) } label: {
                            HStack {
                                Text(Catalog.category(a.category).emoji).font(.title2)
                                VStack(alignment: .leading) {
                                    Text(a.productName).fontWeight(.semibold)
                                    Text("\(dateFmt.string(from: a.date)) · \(a.crop.short) · \(Calculator.fmt(a.areaHa)) ha").font(.caption).foregroundStyle(Color.agMuted)
                                }
                                Spacer()
                            }
                        }.buttonStyle(.plain)
                    }
                }

                NavigationLink { BackupView() } label: { Label("Respaldo y exportación", systemImage: "gearshape.fill").fontWeight(.bold) }
                Text("AgroSense es una herramienta de cálculo y registro. No sustituye la etiqueta ni la recomendación de un profesional. Verifica siempre el registro vigente (COFEPRIS/SENASICA), la dosis, el intervalo de seguridad y la reentrada.")
                    .font(.caption2).foregroundStyle(Color.agMuted)
            }
            .padding(16)
        }
        .background(Color.agBg)
        .navigationBarHidden(true)
    }
}
