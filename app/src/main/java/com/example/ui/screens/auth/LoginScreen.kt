package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (UserProfile) -> Unit,
    primaryColor: Color = Color(0xFF07C160)
) {
    val email by authViewModel.loginEmail.collectAsState()
    val password by authViewModel.loginPassword.collectAsState()
    val captchaInput by authViewModel.loginCaptchaInput.collectAsState()
    val captchaDisplay by authViewModel.loginCaptchaDisplay.collectAsState()
    val isTermsAccepted by authViewModel.isLoginTermsAccepted.collectAsState()
    
    val isLoading by authViewModel.isLoginLoading.collectAsState()
    val error by authViewModel.loginError.collectAsState()
    val success by authViewModel.loginSuccess.collectAsState()

    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.loginError.value = null
        authViewModel.loginSuccess.value = false
        authViewModel.generateLoginCaptcha()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome Back", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Logo Accent
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PX",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
            }

            Text(
                text = "Sign In to Plenxo Pro",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enter your credentials below to sync with your private secure vault.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Error display
            error?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Input Fields
            OutlinedTextField(
                value = email,
                onValueChange = { authViewModel.loginEmail.value = it },
                label = { Text("Email Address") },
                placeholder = { Text("user@gmail.com") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { authViewModel.loginPassword.value = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                trailingIcon = {
                    val iconText = if (isPasswordVisible) "Hide" else "Show"
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(iconText, color = primaryColor)
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor
                )
            )

            // Captcha Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Security Verification Captcha",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = captchaDisplay,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = primaryColor
                        )
                    }
                    IconButton(onClick = { authViewModel.generateLoginCaptcha() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Captcha", tint = primaryColor)
                    }
                }

                OutlinedTextField(
                    value = captchaInput,
                    onValueChange = { authViewModel.loginCaptchaInput.value = it },
                    label = { Text("Enter Verification Answer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("captcha_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor
                    )
                )
            }

            // Terms acceptance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { authViewModel.isLoginTermsAccepted.value = !isTermsAccepted }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isTermsAccepted,
                    onCheckedChange = { authViewModel.isLoginTermsAccepted.value = it },
                    modifier = Modifier.testTag("terms_checkbox"),
                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I accept the Terms & Services of Plenxo Pro",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Login button
            Button(
                onClick = { authViewModel.performLogin(onLoginSuccess) },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Navigate to Sign Up
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "New to Plenxo? ", fontSize = 14.sp)
                Text(
                    text = "Sign Up Now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier
                        .clickable { onNavigateToSignUp() }
                        .testTag("signup_link")
                )
            }
        }
    }
}
