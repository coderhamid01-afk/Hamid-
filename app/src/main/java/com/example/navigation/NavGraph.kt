package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.PlenxoAppContent
import com.example.ui.theme.PlenxoTheme
import com.example.util.PermissionManager
import com.example.viewmodel.PlenxoViewModel

@Composable
fun PlenxoNavGraph(
    viewModel: PlenxoViewModel,
    permissionManager: PermissionManager,
    modifier: Modifier = Modifier
) {
    PlenxoAppContent(
        viewModel = viewModel,
        permissionManager = permissionManager
    )
}

@Composable
fun PlenxoApp(
    viewModel: PlenxoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val permissionManager = remember(context) {
        if (activity != null) PermissionManager(activity) else null
    }
    val themeMode by viewModel.appThemeMode.collectAsState()

    PlenxoTheme(themeMode = themeMode) {
        if (permissionManager != null) {
            PlenxoNavGraph(
                viewModel = viewModel,
                permissionManager = permissionManager,
                modifier = modifier
            )
        }
    }
}

