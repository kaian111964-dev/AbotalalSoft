package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AbuTalalSoftApp()
                }
            }
        }
    }
}

@Composable
fun AbuTalalSoftApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.SPLASH) {
        viewModel.navigateBack()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (currentScreen) {
            AppScreen.SPLASH -> SplashScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.SETUP -> ShopSetupScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.HOME -> HomeScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.PURCHASES -> PurchasesScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.SALES -> SalesScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.CUSTOMERS -> CustomersScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.CUSTOMER_DETAIL -> CustomerDetailScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.EXPENSES -> ExpensesScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.CASHBOX -> CashboxScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.PRICING_PURCHASE -> PricingSettingsScreen(priceType = "PURCHASE", viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.PRICING_SALE -> PricingSettingsScreen(priceType = "SALE", viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.REPORTS -> ReportsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            AppScreen.DEVELOPER_INFO -> DeveloperInfoScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
