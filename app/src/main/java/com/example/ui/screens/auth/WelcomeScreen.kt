package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    Scaffold(
        containerColor = Color(0xFF0B0F17)
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
            // Subtle ambient aura at top (subdued, elegant, no neon)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 600f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Intro Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    // Security Brand Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Plenxo Security",
                            tint = primaryColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = primaryColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "SECURE PROTOCOL V2",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Welcome to Plenxo",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "A modern standard for cryptographically fortified messaging. Let's configure your permanent identity.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Central Modern Glassmorphism Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp)
                ) {
                    ModernFeatureCard(
                        icon = Icons.Default.Lock,
                        title = "Zero-Knowledge Encryption",
                        description = "Direct peer security keeps all chats, voice notes, and media mathematically sealed from third parties.",
                        accentColor = primaryColor
                    )

                    ModernFeatureCard(
                        icon = Icons.Default.Fingerprint,
                        title = "Immutable Plenxo ID",
                        description = "Your unique PX-XXXXXX identity is permanently linked to your private cryptographic key.",
                        accentColor = primaryColor
                    )

                    ModernFeatureCard(
                        icon = Icons.Default.Speed,
                        title = "Instant Local Cache Engine",
                        description = "Optimized SQLite cache delivers zero-lag offline navigation and immediate message loading.",
                        accentColor = primaryColor
                    )
                }

                // Bottom Action
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("welcome_next_button")
                            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = primaryColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Get Started",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ModernFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFF1F5F9)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
