package com.example.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    authViewModel: AuthViewModel,
    onNext: () -> Unit,
    primaryColor: Color = Color(0xFF07C160)
) {
    val profilePicUrl by authViewModel.profilePicUrl.collectAsState()
    val name by authViewModel.name.collectAsState()
    val bio by authViewModel.bio.collectAsState()
    val dob by authViewModel.dob.collectAsState()
    val calculatedAge by authViewModel.calculatedAge.collectAsState()
    val gender by authViewModel.gender.collectAsState()

    val isLoading by authViewModel.isProfileSetupLoading.collectAsState()
    val error by authViewModel.profileSetupError.collectAsState()
    val success by authViewModel.profileSetupSuccess.collectAsState()

    val context = LocalContext.current

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            authViewModel.profilePicUrl.value = uri.toString()
        }
    }

    // Reset setup states when screen is first displayed
    LaunchedEffect(Unit) {
        authViewModel.isProfileSetupLoading.value = false
        authViewModel.profileSetupSuccess.value = false
    }

    // Trigger navigation to Plenxo ID Reveal Screen upon successful save
    LaunchedEffect(success) {
        if (success) {
            onNext()
            authViewModel.profileSetupSuccess.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Personalization", fontWeight = FontWeight.Bold) },
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
            // Screen Header Title
            Text(
                text = "Setup Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Profile Picture Circle Selector
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { galleryLauncher.launch("image/*") }
                    .border(3.dp, primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profilePicUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profilePicUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = "Upload Photo",
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Upload Photo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            "(Optional)",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error display
            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Full Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { authViewModel.name.value = it },
                label = { Text("Display/Full Name") },
                placeholder = { Text("John Doe") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor
                )
            )

            // Bio Input
            OutlinedTextField(
                value = bio,
                onValueChange = { authViewModel.bio.value = it },
                label = { Text("Personal Bio") },
                placeholder = { Text("Hey there! Let's chat securely.") },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bio_input"),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor
                )
            )

            // Date of Birth & Auto Age Calculation Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = { authViewModel.updateDob(it) },
                    label = { Text("DoB (DD-MM-YYYY)") },
                    placeholder = { Text("15-08-1995") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("dob_input"),
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

                // Age Readout Box next to DoB (AUTO CALCULATION READOUT)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Calculated Age",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = calculatedAge.ifEmpty { "--" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (calculatedAge.isNotEmpty()) primaryColor else Color.Gray,
                        modifier = Modifier.testTag("age_display")
                    )
                }
            }

            // Gender Segment Selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Select Gender",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Male", "Female", "Other").forEach { choice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { authViewModel.gender.value = choice }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (gender == choice),
                                onClick = { authViewModel.gender.value = choice },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = choice, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done Button
            Button(
                onClick = { authViewModel.completeProfileSetup() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("setup_profile_done_button"),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
