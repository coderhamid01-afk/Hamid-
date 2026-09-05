package com.example.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.viewmodel.AuthViewModel

/**
 * SignUp Screen wrapper delegating to the unified modern AuthScreen.
 */
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onSuccess: () -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    AuthScreen(
        authViewModel = authViewModel,
        initialMode = AuthMode.SIGNUP,
        onLoginSuccess = { /* Handled in AuthScreen */ },
        onSignUpSuccess = onSuccess,
        onNavigateToOther = onNavigateToLogin,
        primaryColor = primaryColor
    )
}
