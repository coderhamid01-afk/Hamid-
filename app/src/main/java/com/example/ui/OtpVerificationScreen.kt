package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
    primaryColor: Color = Color(0xFF059669)
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
        containerColor = Color(0xFF0B0F17),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Email Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFFF1F5F9)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0B0F17),
                            Color(0xFF080C14)
                        )
                    )
                )
        ) {
            // Subtle ambient aura
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent),
                            radius = 550f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Icon Badge
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Email Verified",
                        tint = primaryColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Title & Instructions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Enter Security Code",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "We have sent an 8-digit secure code to your registered email address. Please enter it below to confirm your account.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Error Card
                AnimatedVisibility(visible = otpError != null) {
                    otpError?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Modern OTP Input Glass Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "8-DIGIT OTP VERIFICATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF94A3B8)
                        )

                        // 8 Segment Boxes Visualizer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 8) {
                                val char = otpInput.getOrNull(i)
                                val isCurrent = i == otpInput.length
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                                        .border(
                                            width = if (isCurrent) 2.dp else 1.dp,
                                            color = when {
                                                char != null -> primaryColor
                                                isCurrent -> primaryColor.copy(alpha = 0.7f)
                                                else -> Color(0xFF334155)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char?.toString() ?: "",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (char != null) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        // Text Field backing the entry (testTag: otp_input)
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { newValue ->
                                if (newValue.length <= 8 && newValue.all { ch -> ch.isDigit() }) {
                                    authViewModel.otpInput.value = newValue
                                }
                            },
                            placeholder = { Text("Paste or type 8-digit OTP", color = Color(0xFF64748B), fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input"),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (otpInput.length == 8) {
                                        authViewModel.verifyOtp()
                                    }
                                }
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                textAlign = TextAlign.Center,
                                color = primaryColor,
                                fontSize = 18.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // Resend Timer Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTimerRunning) {
                        Text(
                            text = "Resend code in $secondsRemaining seconds",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { authViewModel.resendOtp() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Resend Code",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Resend Code",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("resend_button")
                            )
                        }
                    }
                }

                // Verify Button
                Button(
                    onClick = { authViewModel.verifyOtp() },
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("verify_button")
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = primaryColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = Color(0xFF1E293B)
                    )
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Verify & Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
