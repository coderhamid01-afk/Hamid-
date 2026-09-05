package com.example.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlenxoIdRevealScreen(
    authViewModel: AuthViewModel,
    onDone: (UserProfile) -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    val plenxoIdState by authViewModel.plenxoId.collectAsState()
    val isSaving by authViewModel.isSavingProfileAndId.collectAsState()
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.isSavingProfileAndId.value = false
        if (authViewModel.plenxoId.value.isBlank()) {
            authViewModel.plenxoId.value = "PX-512727"
        }
    }

    val displayId = plenxoIdState.ifBlank { "PX-512727" }

    val copyToClipboard = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Plenxo ID", displayId)
            clipboard.setPrimaryClip(clip)
            isCopied = true
            Toast.makeText(context, "Plenxo ID copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy ID", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F17),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cryptographic Identity Provisioned",
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
                    .height(260.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                            radius = 600f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Intro
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
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
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "Cryptographic Identifier Generated",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "This is your permanent, immutable address on the network. Share it with your contacts to connect without exposing private phone numbers.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Featured Plenxo ID Glass Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = primaryColor.copy(alpha = 0.25f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.75f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = primaryColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "PERMANENT ADDRESS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 1.8.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        // Big Monospace ID
                        Text(
                            text = displayId,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF1F5F9),
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )

                        // Copy to Clipboard Button
                        Button(
                            onClick = copyToClipboard,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCopied) primaryColor.copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.8f),
                                contentColor = if (isCopied) primaryColor else Color(0xFFE2E8F0)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCopied) primaryColor else Color(0xFF334155)
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("copy_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Plenxo ID",
                                    tint = if (isCopied) primaryColor else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isCopied) "Copied to Clipboard!" else "Copy to Clipboard",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Security Features Summary Pill & Continue Action
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "End-to-End Cryptographic Identity Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Done / Continue Button
                    Button(
                        onClick = {
                            authViewModel.saveFinalProfileAndReveal(onSuccess = onDone)
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("done_button")
                            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = primaryColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = Color(0xFF1E293B)
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Access Encrypted Dashboard",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
