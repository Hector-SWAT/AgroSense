import SwiftUI
import SwiftData

@main
struct AgroSenseApp: App {
    let container: ModelContainer = {
        do { return try ModelContainer(for: Client.self, Plot.self, Product.self, ApplicationRecord.self) }
        catch { fatalError("No se pudo abrir la base de datos: \(error)") }
    }()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .tint(.agGreen)
                .onAppear { Seeder.seedIfNeeded(container.mainContext) }
        }
        .modelContainer(container)
    }
}

/// Navegación entre pestañas compartida (para saltar desde Inicio al asistente, etc.).
@Observable final class Router {
    var tab = 0
    var wizardClient: Int64? = nil
    var wizardPlot: Int64? = nil
    var wizardToken = UUID()   // cambia para reiniciar el asistente

    func newApplication(client: Int64? = nil, plot: Int64? = nil) {
        wizardClient = client; wizardPlot = plot; wizardToken = UUID(); tab = 2
    }
}

struct ContentView: View {
    @State private var router = Router()
    var body: some View {
        TabView(selection: $router.tab) {
            NavigationStack { HomeView() }.tabItem { Label("Inicio", systemImage: "house.fill") }.tag(0)
            NavigationStack { ClientsView() }.tabItem { Label("Clientes", systemImage: "person.2.fill") }.tag(1)
            NavigationStack { WizardView(startClient: router.wizardClient, startPlot: router.wizardPlot).id(router.wizardToken) }
                .tabItem { Label("Aplicar", systemImage: "plus.circle.fill") }.tag(2)
            NavigationStack { ProductsView() }.tabItem { Label("Productos", systemImage: "shippingbox.fill") }.tag(3)
            NavigationStack { HistoryView() }.tabItem { Label("Historial", systemImage: "clock.fill") }.tag(4)
        }
        .environment(router)
    }
}
