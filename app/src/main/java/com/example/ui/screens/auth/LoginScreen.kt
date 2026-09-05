package com.example.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.UserProfile
import com.example.viewmodel.AuthViewModel

/**
 * Login Screen wrapper delegating to the unified modern AuthScreen.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (UserProfile) -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    AuthScreen(
        authViewModel = authViewModel,
        initialMode = AuthMode.LOGIN,
        onLoginSuccess = onLoginSuccess,
        onSignUpSuccess = onNavigateToSignUp,
        onNavigateToOther = onNavigateToSignUp,
        primaryColor = primaryColor
    )
}
