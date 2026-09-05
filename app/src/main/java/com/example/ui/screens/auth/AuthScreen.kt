package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.ui.components.Animated3DBackground
import com.example.ui.components.HumanVerificationCaptchaDialog
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    SIGNUP
}

/**
 * Unified Modern Auth Screen (Login & SignUp)
 * 1. 3D Animated Background continuing uninterrupted across screens
 * 2. Floating Transparent Header (PX Badge, Title, Subtitle with NO container)
 * 3. Single Unified Glassmorphism Card (All inputs, captcha trigger, terms, and primary button inside ONE card)
 * 4. Micro-interactions: Focused border glow, scale animations, tactile haptics, sweeping gradient button shimmer
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    initialMode: AuthMode = AuthMode.LOGIN,
    onLoginSuccess: (UserProfile) -> Unit,
    onSignUpSuccess: () -> Unit,
    onNavigateToOther: (() -> Unit)? = null,
    primaryColor: Color = Color(0xFF059669)
) {
    var authMode by rememberSaveable { mutableStateOf(initialMode) }
    var showCaptchaDialog by rememberSaveable { mutableStateOf(false) }
    var isCaptchaVerified by rememberSaveable { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Login Form States from ViewModel
    val loginEmail by authViewModel.loginEmail.collectAsState()
    val loginPassword by authViewModel.loginPassword.collectAsState()
    val isLoginLoading by authViewModel.isLoginLoading.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()

    // SignUp Form States from ViewModel
    val signUpEmail by authViewModel.signUpEmail.collectAsState()
    val signUpPassword by authViewModel.signUpPassword.collectAsState()
    val confirmPassword by authViewModel.confirmPassword.collectAsState()
    val isSignUpLoading by authViewModel.isSignUpLoading.collectAsState()
    val isTermsAccepted by authViewModel.isTermsAccepted.collectAsState()
    val signUpError by authViewModel.signUpError.collectAsState()
    val signUpSuccess by authViewModel.signUpSuccess.collectAsState()

    // Password visibility toggles
    var isLoginPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isSignUpPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.loginError.value = null
        authViewModel.signUpError.value = null
        authViewModel.loginSuccess.value = false
        authViewModel.signUpSuccess.value = false
        authViewModel.generateLoginCaptcha()
        authViewModel.generateSignUpCaptcha()
    }

    LaunchedEffect(signUpSuccess) {
        if (signUpSuccess) {
            onSignUpSuccess()
            authViewModel.signUpSuccess.value = false
        }
    }

    // 4-Step Interactive Captcha Modal
    HumanVerificationCaptchaDialog(
        visible = showCaptchaDialog,
        onDismiss = { showCaptchaDialog = false },
        onVerificationSuccess = {
            isCaptchaVerified = true
            showCaptchaDialog = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (authMode == AuthMode.LOGIN) {
                authViewModel.markLoginCaptchaVerified()
            } else {
                authViewModel.markSignUpCaptchaVerified()
            }
        },
        primaryColor = primaryColor
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Continuous 3D Animated Background
        Animated3DBackground(
            modifier = Modifier.fillMaxSize(),
            accentColor = primaryColor
        )

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 2. FLOATING TRANSPARENT HEADER (App Logo, Title, Subtitle directly on background)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.35f),
                                Color(0xFF0F172A).copy(alpha = 0.6f)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(primaryColor, primaryColor.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PX",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF8FAFC),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Title & Subtitle based on active mode
            Text(
                text = if (authMode == AuthMode.LOGIN) "Welcome Back" else "Create Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF8FAFC),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (authMode == AuthMode.LOGIN)
                    "Sign in to access your account"
                else
                    "Create an account to get started",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. SINGLE UNIFIED GLASSMORPHISM CARD CONTAINER
            // Groups ALL inputs, security verification trigger, terms checkbox, and primary action button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF475569).copy(alpha = 0.5f),
                                Color(0xFF1E293B).copy(alpha = 0.3f),
                                primaryColor.copy(alpha = 0.35f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .shadow(24.dp, RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.82f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Switcher Pill: [ Sign In ]   [ Create Account ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color(0xFF0B1120).copy(alpha = 0.7f))
                            .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(23.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sign In Tab
                        val isLogin = authMode == AuthMode.LOGIN
                        val loginBg by animateColorAsState(
                            targetValue = if (isLogin) primaryColor else Color.Transparent,
                            animationSpec = tween(250),
                            label = "loginTabBg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(19.dp))
                                .background(loginBg)
                                .clickable {
                                    if (authMode != AuthMode.LOGIN) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        authMode = AuthMode.LOGIN
                                        authViewModel.resetAuthState()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                fontSize = 13.sp,
                                fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLogin) Color.White else Color(0xFF94A3B8)
                            )
                        }

                        // Sign Up Tab
                        val isSignUp = authMode == AuthMode.SIGNUP
                        val signUpBg by animateColorAsState(
                            targetValue = if (isSignUp) primaryColor else Color.Transparent,
                            animationSpec = tween(250),
                            label = "signUpTabBg"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(19.dp))
                                .background(signUpBg)
                                .clickable {
                                    if (authMode != AuthMode.SIGNUP) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        authMode = AuthMode.SIGNUP
                                        authViewModel.resetAuthState()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                fontSize = 13.sp,
                                fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSignUp) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Notification Pill
                    val activeError = if (authMode == AuthMode.LOGIN) loginError else signUpError
                    AnimatedVisibility(
                        visible = !activeError.isNullOrBlank(),
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Form Content Transition with AnimatedContent
                    AnimatedContent(
                        targetState = authMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "AuthFormTransition"
                    ) { mode ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (mode == AuthMode.LOGIN) {
                                // LOGIN FORM INPUTS
                                PolishedInputField(
                                    value = loginEmail,
                                    onValueChange = { authViewModel.loginEmail.value = it },
                                    label = "Email Address",
                                    placeholder = "name@domain.com",
                                    leadingIcon = Icons.Default.Email,
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                    primaryColor = primaryColor,
                                    testTag = "login_email_input"
                                )

                                PolishedInputField(
                                    value = loginPassword,
                                    onValueChange = { authViewModel.loginPassword.value = it },
                                    label = "Password",
                                    placeholder = "••••••••",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isLoginPasswordVisible,
                                    onTogglePasswordVisibility = { isLoginPasswordVisible = !isLoginPasswordVisible },
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                    onDone = { focusManager.clearFocus() },
                                    primaryColor = primaryColor,
                                    testTag = "login_password_input"
                                )
                            } else {
                                // SIGNUP FORM INPUTS
                                PolishedInputField(
                                    value = signUpEmail,
                                    onValueChange = { authViewModel.signUpEmail.value = it },
                                    label = "Email Address",
                                    placeholder = "name@domain.com",
                                    leadingIcon = Icons.Default.Email,
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                    primaryColor = primaryColor,
                                    testTag = "signup_email_input"
                                )

                                PolishedInputField(
                                    value = signUpPassword,
                                    onValueChange = { authViewModel.signUpPassword.value = it },
                                    label = "Create Password",
                                    placeholder = "Min 6 characters",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isSignUpPasswordVisible,
                                    onTogglePasswordVisibility = { isSignUpPasswordVisible = !isSignUpPasswordVisible },
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next,
                                    primaryColor = primaryColor,
                                    testTag = "signup_password_input"
                                )

                                PolishedInputField(
                                    value = confirmPassword,
                                    onValueChange = { authViewModel.confirmPassword.value = it },
                                    label = "Confirm Password",
                                    placeholder = "Re-enter password",
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isConfirmPasswordVisible,
                                    onTogglePasswordVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                    onDone = { focusManager.clearFocus() },
                                    primaryColor = primaryColor,
                                    testTag = "signup_confirm_password_input"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECURITY VERIFICATION TRIGGER (Within the same unified card)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCaptchaVerified) primaryColor.copy(alpha = 0.14f)
                                else Color(0xFF0B1120).copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (isCaptchaVerified) primaryColor.copy(alpha = 0.6f)
                                else Color(0xFF334155),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCaptchaDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("captcha_trigger_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCaptchaVerified) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isCaptchaVerified) primaryColor else Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isCaptchaVerified) "Verification Complete" else "Human Verification",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF1F5F9)
                                )
                                Text(
                                    text = if (isCaptchaVerified) "Verification completed successfully" else "Complete captcha to continue",
                                    fontSize = 11.sp,
                                    color = if (isCaptchaVerified) primaryColor else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCaptchaVerified) primaryColor.copy(alpha = 0.2f)
                                    else Color(0xFF1E293B)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isCaptchaVerified) "Verified" else "Verify",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCaptchaVerified) primaryColor else Color(0xFF38BDF8)
                            )
                        }
                    }

                    // TERMS CHECKBOX (For SignUp mode)
                    if (authMode == AuthMode.SIGNUP) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    authViewModel.isTermsAccepted.value = !isTermsAccepted
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isTermsAccepted,
                                onCheckedChange = {
                                    authViewModel.isTermsAccepted.value = it
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    uncheckedColor = Color(0xFF64748B),
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "I agree to Plenxo Terms of Service & Privacy Policy",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // PRIMARY ACTION BUTTON (With scale micro-interaction, haptics, and sweeping gradient shimmer)
                    val isLoading = if (authMode == AuthMode.LOGIN) isLoginLoading else isSignUpLoading
                    val isActionEnabled = !isLoading

                    ShimmerActionButton(
                        text = if (authMode == AuthMode.LOGIN) "Sign In" else "Sign Up",
                        isLoading = isLoading,
                        enabled = isActionEnabled,
                        primaryColor = primaryColor,
                        testTag = if (authMode == AuthMode.LOGIN) "login_button" else "signup_button",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            focusManager.clearFocus()

                            if (authMode == AuthMode.LOGIN) {
                                if (!isCaptchaVerified) {
                                    showCaptchaDialog = true
                                } else {
                                    authViewModel.performLogin(onSuccess = onLoginSuccess)
                                }
                            } else {
                                if (!isCaptchaVerified) {
                                    showCaptchaDialog = true
                                } else {
                                    authViewModel.performSignUp()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Don't have an account? " else "Already have an account? ",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Sign Up" else "Sign In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
                            authViewModel.resetAuthState()
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                        .testTag("toggle_auth_mode_button")
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Custom Polished Input Field with focused border glow and floating label
 */
@Composable
private fun PolishedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    primaryColor: Color,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> primaryColor
            value.isNotEmpty() -> Color(0xFF475569)
            else -> Color(0xFF334155)
        },
        label = "fieldBorderColor"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF64748B), fontSize = 13.sp) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .testTag(testTag),
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) primaryColor else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() }
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color(0xFFF1F5F9),
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            focusedContainerColor = Color(0xFF0B1120).copy(alpha = 0.7f),
            unfocusedContainerColor = Color(0xFF0B1120).copy(alpha = 0.55f),
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = Color(0xFF94A3B8)
        )
    )
}

/**
 * Modern Action Button with click scale micro-interaction and sweeping gradient shimmer
 */
@Composable
private fun ShimmerActionButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    primaryColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmerTransition")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -350f,
        targetValue = 650f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            primaryColor,
            primaryColor.copy(alpha = 0.85f),
            Color(0xFF34D399),
            primaryColor.copy(alpha = 0.85f),
            primaryColor
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 240f, 120f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) shimmerBrush else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B))))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading
            ) { onClick() }
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(14.dp))
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
        }
    }
}
