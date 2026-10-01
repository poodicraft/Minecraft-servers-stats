package io.github.poodicraft.serverscope.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.poodicraft.serverscope.data.model.Edition
import io.github.poodicraft.serverscope.ui.home.HomeScreen
import io.github.poodicraft.serverscope.ui.result.ResultScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** [address] is a normalized [io.github.poodicraft.serverscope.data.ServerAddress.query]. */
@Serializable
data class ServerRoute(val address: String, val edition: String)

@Composable
fun ServerScopeNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = { slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(320)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(280)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenServer = { address, edition ->
                    navController.navigate(ServerRoute(address, edition.name)) { launchSingleTop = true }
                },
            )
        }
        composable<ServerRoute> { entry ->
            val route = entry.toRoute<ServerRoute>()
            ResultScreen(
                address = route.address,
                edition = Edition.fromName(route.edition),
                onBack = { navController.navigateUp() },
            )
        }
    }
}
