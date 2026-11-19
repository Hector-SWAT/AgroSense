package com.swat.agrosense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swat.agrosense.ui.screens.ApplicationDetailScreen
import com.swat.agrosense.ui.screens.BackupScreen
import com.swat.agrosense.ui.screens.ClientDetailScreen
import com.swat.agrosense.ui.screens.ClientsScreen
import com.swat.agrosense.ui.screens.HistoryScreen
import com.swat.agrosense.ui.screens.HomeScreen
import com.swat.agrosense.ui.screens.ProductsScreen
import com.swat.agrosense.ui.screens.WizardScreen
import com.swat.agrosense.ui.theme.AgroSenseTheme
import com.swat.agrosense.ui.theme.Green
import com.swat.agrosense.ui.theme.GreenSoft

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AgroSenseTheme { AgroSenseApp(vm) } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Inicio", Icons.Filled.Home),
    Tab("clients", "Clientes", Icons.Filled.People),
    Tab("wizard", "Aplicar", Icons.Filled.AddCircle),
    Tab("products", "Productos", Icons.Filled.Inventory2),
    Tab("history", "Historial", Icons.Filled.History)
)

fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo("home") { saveState = false }
    launchSingleTop = true
}

@androidx.compose.runtime.Composable
fun AgroSenseApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route?.substringBefore("?")?.substringBefore("/")
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = current == t.route,
                        onClick = { nav.goTab(t.route) },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = GreenSoft)
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, nav) }
            composable("clients") { ClientsScreen(vm, nav) }
            composable("client/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                ClientDetailScreen(vm, nav, it.arguments!!.getLong("id"))
            }
            composable(
                "wizard?clientId={clientId}&plotId={plotId}",
                arguments = listOf(
                    navArgument("clientId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("plotId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                WizardScreen(vm, nav, it.arguments!!.getLong("clientId"), it.arguments!!.getLong("plotId"))
            }
            composable("products") { ProductsScreen(vm) }
            composable("history") { HistoryScreen(vm, nav) }
            composable("app/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                ApplicationDetailScreen(vm, nav, it.arguments!!.getLong("id"))
            }
            composable("backup") { BackupScreen(vm) }
        }
    }
}
