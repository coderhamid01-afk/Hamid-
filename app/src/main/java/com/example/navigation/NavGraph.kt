package com.example.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.PlenxoAppContent
import com.example.util.PermissionManager
import com.example.viewmodel.PlenxoScreen
import com.example.viewmodel.PlenxoViewModel

@Composable
fun PlenxoNavGraph(
    viewModel: PlenxoViewModel,
    permissionManager: PermissionManager,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 300),
        modifier = modifier,
        label = "nav_graph_transition"
    ) { screen ->
        when (screen) {
            PlenxoScreen.PERMISSION_GATEWAY -> {
                viewModel.navigateToScreen(PlenxoScreen.HOME, addToHistory = false, clearHistory = true)
            }
            else -> {
                PlenxoAppContent(
                    viewModel = viewModel,
                    permissionManager = permissionManager
                )
            }
        }
    }
}
