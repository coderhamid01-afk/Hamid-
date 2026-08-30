package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit,
    primaryColor: Color = Color(0xFF07C160)
) {
    val otpInput by authViewModel.otpInput.collectAsState()
    val secondsRemaining by authViewModel.secondsRemaining.collectAsState()
    val isTimerRunning by authViewModel.isTimerRunning.collectAsState()
    val otpError by authViewModel.otpError.collectAsState()
    val isVerifying by authViewModel.isVerifyingOtp.collectAsState()
    val otpSuccess by authViewModel.otpSuccess.collectAsState()

    LaunchedEffect(otpSuccess) {
        if (otpSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Email", fontWeight = FontWeight.Bold) },
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
            // Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(primaryColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Email,
                    contentDescription = "Email Icon",
                    tint = primaryColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Enter Verification Code",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "We have sent an 8-digit secure OTP to your registered email address. Please check your inbox to retrieve your verification code.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Verification Error Card
            otpError?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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

            // OTP Input
            OutlinedTextField(
                value = otpInput,
                onValueChange = { newValue ->
                    if (newValue.length <= 8 && newValue.all { char -> char.isDigit() }) {
                        authViewModel.otpInput.value = newValue
                    }
                },
                label = { Text("8-Digit Verification Code") },
                placeholder = { Text("00000000") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor
                )
            )

            // Countdown Timer Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTimerRunning) {
                    Text(
                        text = "Resend code in $secondsRemaining seconds",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Row(
                        modifier = Modifier.clickable { authViewModel.resendOtp() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Resend Code",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Resend Code",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("resend_button")
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = { authViewModel.verifyOtp() },
                enabled = !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_button"),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
