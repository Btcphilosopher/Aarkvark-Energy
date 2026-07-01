package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.UtilityViewModel
import com.example.ui.UtilityViewModelFactory
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: UtilityViewModel by viewModels {
    UtilityViewModelFactory((application as AardvarkApplication).repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var currentTab by remember { mutableStateOf("Dashboard") }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            NavigationBar(
              modifier = Modifier.testTag("main_bottom_nav"),
              windowInsets = WindowInsets.navigationBars
            ) {
              NavigationBarItem(
                selected = currentTab == "Dashboard",
                onClick = { currentTab = "Dashboard" },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard") },
                modifier = Modifier.testTag("nav_dashboard")
              )
              NavigationBarItem(
                selected = currentTab == "Meters",
                onClick = { currentTab = "Meters" },
                icon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = "Meters") },
                label = { Text("Meters") },
                modifier = Modifier.testTag("nav_meters")
              )
              NavigationBarItem(
                selected = currentTab == "Customers",
                onClick = { currentTab = "Customers" },
                icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                label = { Text("Customers") },
                modifier = Modifier.testTag("nav_customers")
              )
              NavigationBarItem(
                selected = currentTab == "Billing",
                onClick = { currentTab = "Billing" },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Billing") },
                label = { Text("Billing") },
                modifier = Modifier.testTag("nav_billing")
              )
              NavigationBarItem(
                selected = currentTab == "Admin",
                onClick = { currentTab = "Admin" },
                icon = { Icon(Icons.Default.Build, contentDescription = "Admin") },
                label = { Text("Admin") },
                modifier = Modifier.testTag("nav_admin")
              )
            }
          },
          contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
              "Dashboard" -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToMeters = { currentTab = "Meters" },
                onNavigateToAlerts = { currentTab = "Admin" }
              )
              "Meters" -> MetersScreen(viewModel = viewModel)
              "Customers" -> CustomersScreen(viewModel = viewModel)
              "Billing" -> BillingScreen(viewModel = viewModel)
              "Admin" -> ManagementScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}

