package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Strict state machine for Captcha transitions.
 */
enum class CaptchaStep {
    MATH_QUIZ,
    PUZZLE_PIECES,
    SHAPE_SEQUENCE,
    SLIDER_TERMS
}

/**
 * Modern 4-Step Interactive Captcha Modal Dialog ("Verify You Are Human")
 * Features randomized puzzle pieces, tactile haptics, smooth micro-interactions,
 * and high-contrast glassmorphism visual styling.
 */
@Composable
fun HumanVerificationCaptchaDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            HumanVerificationCaptchaContent(
                onDismiss = onDismiss,
                onVerificationSuccess = onVerificationSuccess,
                primaryColor = primaryColor
            )
        }
    }
}

@Composable
fun HumanVerificationCaptchaContent(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit,
    primaryColor: Color = Color(0xFF059669)
) {
    var currentStep by rememberSaveable { mutableStateOf(CaptchaStep.MATH_QUIZ) }
    var isOverallVerified by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val currentStepNumber = when (currentStep) {
        CaptchaStep.MATH_QUIZ -> 1
        CaptchaStep.PUZZLE_PIECES -> 2
        CaptchaStep.SHAPE_SEQUENCE -> 3
        CaptchaStep.SLIDER_TERMS -> 4
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .widthIn(max = 440.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFF475569).copy(alpha = 0.6f),
                        Color(0xFF1E293B).copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.3f)
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .shadow(24.dp, RoundedCornerShape(26.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A).copy(alpha = 0.94f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Security Badge, Title, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.08f))
                                )
                            )
                            .border(1.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Human Verification",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "Step $currentStepNumber of 4 • Quick security check",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step Progress Indicator Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (step in 1..4) {
                    val isDone = step < currentStepNumber || isOverallVerified
                    val isActive = step == currentStepNumber && !isOverallVerified
                    val barColor by animateColorAsState(
                        targetValue = when {
                            isDone -> primaryColor
                            isActive -> primaryColor.copy(alpha = 0.9f)
                            else -> Color(0xFF334155)
                        },
                        label = "barColor_$step"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Strict State Machine Transitions with AnimatedContent
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "CaptchaStepTransition"
            ) { step ->
                when (step) {
                    CaptchaStep.MATH_QUIZ -> {
                        CaptchaStep1MathQuiz(
                            primaryColor = primaryColor,
                            onStepComplete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    delay(300)
                                    currentStep = CaptchaStep.PUZZLE_PIECES
                                }
                            }
                        )
                    }
                    CaptchaStep.PUZZLE_PIECES -> {
                        CaptchaStep2Puzzle(
                            primaryColor = primaryColor,
                            onStepComplete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    delay(350)
                                    currentStep = CaptchaStep.SHAPE_SEQUENCE
                                }
                            }
                        )
                    }
                    CaptchaStep.SHAPE_SEQUENCE -> {
                        CaptchaStep3Sequence(
                            primaryColor = primaryColor,
                            onStepComplete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    delay(300)
                                    currentStep = CaptchaStep.SLIDER_TERMS
                                }
                            }
                        )
                    }
                    CaptchaStep.SLIDER_TERMS -> {
                        CaptchaStep4SliderTerms(
                            primaryColor = primaryColor,
                            onStepComplete = {
                                isOverallVerified = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    delay(380)
                                    onVerificationSuccess()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: DYNAMIC MATH QUIZ (WITH SHAKE & MICRO-CHECK ANIMATION)
// -------------------------------------------------------------
@Composable
private fun CaptchaStep1MathQuiz(
    primaryColor: Color,
    onStepComplete: () -> Unit
) {
    val num1 = rememberSaveable { Random.nextInt(12, 48) }
    val num2 = rememberSaveable { Random.nextInt(3, 17) }
    val isAddition = rememberSaveable { Random.nextBoolean() }
    val correctAnswer = if (isAddition) (num1 + num2) else (num1 - num2)

    var answerInput by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isSuccess by rememberSaveable { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun verifyAnswer() {
        val trimmed = answerInput.trim()
        if (trimmed == correctAnswer.toString()) {
            isError = false
            isSuccess = true
            onStepComplete()
        } else {
            isError = true
            answerInput = ""
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            coroutineScope.launch {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 350
                        0f at 0
                        -18f at 50
                        18f at 100
                        -12f at 150
                        12f at 200
                        -6f at 250
                        6f at 300
                        0f at 350
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Solve Math Challenge",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )

        // Math Equation Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color(0xFF475569).copy(alpha = 0.5f), Color(0xFF334155).copy(alpha = 0.3f))
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(vertical = 18.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$num1",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = if (isAddition) " + " else " − ",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = "$num2",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = " = ?",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Shaking Input Field
        OutlinedTextField(
            value = answerInput,
            onValueChange = {
                if (it.length <= 4 && (it.isEmpty() || it.all { ch -> ch.isDigit() || ch == '-' })) {
                    answerInput = it
                    isError = false
                }
            },
            placeholder = { Text("Enter answer", color = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .testTag("captcha_math_input"),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { verifyAnswer() }),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else primaryColor,
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color(0xFF334155),
                focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
            )
        )

        if (isError) {
            Text(
                text = "Incorrect answer, please try again",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }

        // Action Button or Success Check Animation
        if (isSuccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Solved! Next step...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        } else {
            Button(
                onClick = { verifyAnswer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Verify Answer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: 3-PIECE PUZZLE (EXPLICIT SHUFFLE & DRAG-SNAP HAPTICS)
// -------------------------------------------------------------
private data class PuzzlePieceModel(
    val id: Int,       // 0 = Left, 1 = Center, 2 = Right
    val label: String,
    val title: String
)

@Composable
private fun CaptchaStep2Puzzle(
    primaryColor: Color,
    onStepComplete: () -> Unit
) {
    // Explicitly SHUFFLE puzzle pieces so they are never presented in default order
    val shuffledPieces = remember {
        listOf(
            PuzzlePieceModel(0, "Slot 1", "Left Crest"),
            PuzzlePieceModel(1, "Slot 2", "Core Shield"),
            PuzzlePieceModel(2, "Slot 3", "Right Crest")
        ).shuffled()
    }

    // State for placement of pieces 0, 1, 2
    var isPlaced0 by rememberSaveable { mutableStateOf(false) }
    var isPlaced1 by rememberSaveable { mutableStateOf(false) }
    var isPlaced2 by rememberSaveable { mutableStateOf(false) }

    // Drag offsets for the 3 slots in dock
    var dragOffset0 by remember { mutableStateOf(Offset.Zero) }
    var dragOffset1 by remember { mutableStateOf(Offset.Zero) }
    var dragOffset2 by remember { mutableStateOf(Offset.Zero) }

    // Hover state over target slots for visual aura
    var isHovering0 by remember { mutableStateOf(false) }
    var isHovering1 by remember { mutableStateOf(false) }
    var isHovering2 by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val snapThresholdPx = with(density) { 20.dp.toPx() }
    val haptic = LocalHapticFeedback.current

    // Coordinates in root
    var slotPos0 by remember { mutableStateOf(Offset.Zero) }
    var slotPos1 by remember { mutableStateOf(Offset.Zero) }
    var slotPos2 by remember { mutableStateOf(Offset.Zero) }

    var dockPos0 by remember { mutableStateOf(Offset.Zero) }
    var dockPos1 by remember { mutableStateOf(Offset.Zero) }
    var dockPos2 by remember { mutableStateOf(Offset.Zero) }

    fun isPiecePlaced(id: Int): Boolean = when (id) {
        0 -> isPlaced0
        1 -> isPlaced1
        2 -> isPlaced2
        else -> false
    }

    fun markPiecePlaced(id: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (id) {
            0 -> isPlaced0 = true
            1 -> isPlaced1 = true
            2 -> isPlaced2 = true
        }
    }

    val isAllPlaced = isPlaced0 && isPlaced1 && isPlaced2

    LaunchedEffect(isAllPlaced) {
        if (isAllPlaced) {
            delay(300)
            onStepComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Complete the Puzzle",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )
        Text(
            text = "Drag or tap each piece into its matching slot",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        // Target Board (Contains 3 slots: Left, Center, Right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0B1120))
                .border(
                    width = 1.5.dp,
                    color = if (isAllPlaced) primaryColor else Color(0xFF334155),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Target Slot 0 (Left slice)
                val targetBorder0 by animateColorAsState(
                    targetValue = when {
                        isPlaced0 -> primaryColor
                        isHovering0 -> Color(0xFF38BDF8)
                        else -> Color(0xFF475569).copy(alpha = 0.5f)
                    },
                    label = "border0"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPlaced0) primaryColor.copy(alpha = 0.18f)
                            else if (isHovering0) Color(0xFF38BDF8).copy(alpha = 0.12f)
                            else Color(0xFF1E293B).copy(alpha = 0.5f)
                        )
                        .border(1.dp, targetBorder0, RoundedCornerShape(12.dp))
                        .onGloballyPositioned { slotPos0 = it.positionInRoot() }
                        .clickable { if (!isPlaced0) markPiecePlaced(0) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaced0) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawEmblemSlice(slice = 0, primaryColor = primaryColor)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isHovering0) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Slot 1", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Target Slot 1 (Center slice)
                val targetBorder1 by animateColorAsState(
                    targetValue = when {
                        isPlaced1 -> primaryColor
                        isHovering1 -> Color(0xFF38BDF8)
                        else -> Color(0xFF475569).copy(alpha = 0.5f)
                    },
                    label = "border1"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPlaced1) primaryColor.copy(alpha = 0.18f)
                            else if (isHovering1) Color(0xFF38BDF8).copy(alpha = 0.12f)
                            else Color(0xFF1E293B).copy(alpha = 0.5f)
                        )
                        .border(1.dp, targetBorder1, RoundedCornerShape(12.dp))
                        .onGloballyPositioned { slotPos1 = it.positionInRoot() }
                        .clickable { if (!isPlaced1) markPiecePlaced(1) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaced1) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawEmblemSlice(slice = 1, primaryColor = primaryColor)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isHovering1) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Slot 2", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Target Slot 2 (Right slice)
                val targetBorder2 by animateColorAsState(
                    targetValue = when {
                        isPlaced2 -> primaryColor
                        isHovering2 -> Color(0xFF38BDF8)
                        else -> Color(0xFF475569).copy(alpha = 0.5f)
                    },
                    label = "border2"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPlaced2) primaryColor.copy(alpha = 0.18f)
                            else if (isHovering2) Color(0xFF38BDF8).copy(alpha = 0.12f)
                            else Color(0xFF1E293B).copy(alpha = 0.5f)
                        )
                        .border(1.dp, targetBorder2, RoundedCornerShape(12.dp))
                        .onGloballyPositioned { slotPos2 = it.positionInRoot() }
                        .clickable { if (!isPlaced2) markPiecePlaced(2) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaced2) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawEmblemSlice(slice = 2, primaryColor = primaryColor)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isHovering2) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Slot 3", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Draggable Pieces Dock (Showing explicitly shuffled pieces)
        Text(
            text = "Puzzle Pieces",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF94A3B8)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            shuffledPieces.forEachIndexed { index, piece ->
                val pieceId = piece.id
                val isPlaced = isPiecePlaced(pieceId)

                var currentDragOffset by remember {
                    when (index) {
                        0 -> mutableStateOf(dragOffset0)
                        1 -> mutableStateOf(dragOffset1)
                        else -> mutableStateOf(dragOffset2)
                    }
                }

                val targetSlotPos = when (pieceId) {
                    0 -> slotPos0
                    1 -> slotPos1
                    else -> slotPos2
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned {
                            when (index) {
                                0 -> dockPos0 = it.positionInRoot()
                                1 -> dockPos1 = it.positionInRoot()
                                2 -> dockPos2 = it.positionInRoot()
                            }
                        }
                ) {
                    if (!isPlaced) {
                        val isDragging = currentDragOffset != Offset.Zero
                        val pieceScale by animateFloatAsState(
                            targetValue = if (isDragging) 1.08f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.7f),
                            label = "pieceScale_$pieceId"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pieceScale
                                    scaleY = pieceScale
                                    translationX = currentDragOffset.x
                                    translationY = currentDragOffset.y
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { markPiecePlaced(pieceId) }
                                .pointerInput(pieceId) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val dockPos = when (index) {
                                                0 -> dockPos0
                                                1 -> dockPos1
                                                else -> dockPos2
                                            }
                                            val currentPos = dockPos + currentDragOffset
                                            val distance = hypot(currentPos.x - targetSlotPos.x, currentPos.y - targetSlotPos.y)

                                            // Snap to target if dropped within 20dp threshold (or generous touch margin)
                                            if (distance <= snapThresholdPx * 2.8f || (targetSlotPos.y - currentPos.y) > snapThresholdPx) {
                                                markPiecePlaced(pieceId)
                                            }
                                            currentDragOffset = Offset.Zero
                                            when (pieceId) {
                                                0 -> isHovering0 = false
                                                1 -> isHovering1 = false
                                                2 -> isHovering2 = false
                                            }
                                        },
                                        onDragCancel = {
                                            currentDragOffset = Offset.Zero
                                            when (pieceId) {
                                                0 -> isHovering0 = false
                                                1 -> isHovering1 = false
                                                2 -> isHovering2 = false
                                            }
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        currentDragOffset += dragAmount

                                        // Update hover aura state
                                        val dockPos = when (index) {
                                            0 -> dockPos0
                                            1 -> dockPos1
                                            else -> dockPos2
                                        }
                                        val currentPos = dockPos + currentDragOffset
                                        val distance = hypot(currentPos.x - targetSlotPos.x, currentPos.y - targetSlotPos.y)
                                        val hovering = distance <= snapThresholdPx * 3.5f

                                        when (pieceId) {
                                            0 -> isHovering0 = hovering
                                            1 -> isHovering1 = hovering
                                            2 -> isHovering2 = hovering
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawEmblemSlice(slice = pieceId, primaryColor = Color(0xFF38BDF8))
                            }
                        }
                    } else {
                        // Placed lock indicator
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.25f))
                                .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Status feedback
        if (isAllPlaced) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Puzzle Completed! Next step...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
    }
}

/**
 * Custom canvas painter for the 3 horizontal/vertical slices of the security shield.
 */
private fun DrawScope.drawEmblemSlice(
    slice: Int,
    primaryColor: Color
) {
    val w = size.width
    val h = size.height

    when (slice) {
        0 -> {
            val path = Path().apply {
                moveTo(w * 0.8f, h * 0.15f)
                lineTo(w * 0.2f, h * 0.25f)
                lineTo(w * 0.2f, h * 0.75f)
                lineTo(w * 0.8f, h * 0.85f)
            }
            drawPath(path = path, color = primaryColor, style = Stroke(width = 3.5f))
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(w * 0.4f, h * 0.45f),
                end = Offset(w * 0.8f, h * 0.45f),
                strokeWidth = 2f
            )
        }
        1 -> {
            val path = Path().apply {
                moveTo(w * 0.1f, h * 0.15f)
                lineTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.9f, h * 0.15f)
                moveTo(w * 0.1f, h * 0.85f)
                lineTo(w * 0.5f, h * 0.95f)
                lineTo(w * 0.9f, h * 0.85f)
            }
            drawPath(path = path, color = primaryColor, style = Stroke(width = 3.5f))
            drawCircle(color = primaryColor, radius = 12f, center = Offset(w * 0.5f, h * 0.5f))
            drawCircle(color = Color(0xFF0F172A), radius = 6f, center = Offset(w * 0.5f, h * 0.5f))
        }
        2 -> {
            val path = Path().apply {
                moveTo(w * 0.2f, h * 0.15f)
                lineTo(w * 0.8f, h * 0.25f)
                lineTo(w * 0.8f, h * 0.75f)
                lineTo(w * 0.2f, h * 0.85f)
            }
            drawPath(path = path, color = primaryColor, style = Stroke(width = 3.5f))
            drawLine(
                color = primaryColor.copy(alpha = 0.6f),
                start = Offset(w * 0.2f, h * 0.45f),
                end = Offset(w * 0.6f, h * 0.45f),
                strokeWidth = 2f
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 3: SHAPE SEQUENCE MATCH (AUTO-ADVANCE ON EXACT ORDER)
// -------------------------------------------------------------
private data class ShapeItem(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun CaptchaStep3Sequence(
    primaryColor: Color,
    onStepComplete: () -> Unit
) {
    val targetSequence = remember {
        listOf(
            ShapeItem(1, "Star", Icons.Default.Star, Color(0xFFF59E0B)),
            ShapeItem(2, "Circle", Icons.Default.Circle, Color(0xFF06B6D4)),
            ShapeItem(3, "Triangle", Icons.Default.ChangeHistory, Color(0xFFF43F5E)),
            ShapeItem(4, "Square", Icons.Default.CropSquare, Color(0xFF818CF8))
        )
    }

    val scrambledShapes = remember { targetSequence.shuffled(Random(42)) }
    val selectedShapesList = remember { mutableStateListOf<Int>() }

    var isWrongSequence by rememberSaveable { mutableStateOf(false) }
    var isSuccess by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun handleShapeTap(shape: ShapeItem) {
        if (isSuccess) return

        val currentIndex = selectedShapesList.size
        if (currentIndex < targetSequence.size) {
            val expectedShape = targetSequence[currentIndex]
            if (shape.id == expectedShape.id) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isWrongSequence = false
                selectedShapesList.add(shape.id)

                if (selectedShapesList.size == targetSequence.size) {
                    isSuccess = true
                    coroutineScope.launch {
                        delay(300)
                        onStepComplete()
                    }
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isWrongSequence = true
                selectedShapesList.clear()
                coroutineScope.launch {
                    delay(1200)
                    isWrongSequence = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Match the Sequence",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )
        Text(
            text = "Tap the shapes below in the order shown above",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        // Target Sequence Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B))
                .border(
                    1.dp,
                    if (isWrongSequence) MaterialTheme.colorScheme.error else Color(0xFF334155),
                    RoundedCornerShape(16.dp)
                )
                .padding(vertical = 12.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                targetSequence.forEachIndexed { index, shape ->
                    val isMatched = index < selectedShapesList.size
                    val isCurrent = index == selectedShapesList.size && !isSuccess

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isMatched -> primaryColor.copy(alpha = 0.25f)
                                    isCurrent -> shape.color.copy(alpha = 0.2f)
                                    else -> Color(0xFF0F172A)
                                }
                            )
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = when {
                                    isMatched -> primaryColor
                                    isCurrent -> shape.color
                                    else -> Color(0xFF334155)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMatched) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Matched",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = shape.icon,
                                contentDescription = shape.name,
                                tint = shape.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (index < targetSequence.lastIndex) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(14.dp)
                        )
                    }
                }
            }
        }

        if (isWrongSequence) {
            Text(
                text = "Incorrect order. Please try again",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }

        // Scrambled Selectable Shapes Below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            scrambledShapes.forEach { shape ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(enabled = !isSuccess) { handleShapeTap(shape) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = shape.icon,
                            contentDescription = shape.name,
                            tint = shape.color,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shape.name,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isSuccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Matched! Final step...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: SLIDER BAR & TERMS CHECKBOX
// -------------------------------------------------------------
@Composable
private fun CaptchaStep4SliderTerms(
    primaryColor: Color,
    onStepComplete: () -> Unit
) {
    var sliderProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var isSliderLocked by rememberSaveable { mutableStateOf(false) }
    var isTermsChecked by rememberSaveable { mutableStateOf(false) }
    var isDone by rememberSaveable { mutableStateOf(false) }

    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val thumbSizeDp = 48.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val isButtonEnabled = (sliderProgress >= 0.95f || isSliderLocked) && isTermsChecked && !isDone

    fun completeVerification() {
        if (isButtonEnabled) {
            isDone = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            coroutineScope.launch {
                delay(300)
                onStepComplete()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Final Confirmation",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFCBD5E1)
        )
        Text(
            text = "Slide to confirm and accept the Terms of Service",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        // Custom Horizontal Slide Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(27.dp))
                .pointerInput(isSliderLocked) {
                    if (isSliderLocked) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!isSliderLocked) {
                                if (sliderProgress >= 0.95f) {
                                    sliderProgress = 1f
                                    isSliderLocked = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    sliderProgress = 0f
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        if (!isSliderLocked) {
                            val maxDragPx = (trackWidthPx - thumbSizePx).coerceAtLeast(1f)
                            val newProgress = (sliderProgress + (dragAmount / maxDragPx)).coerceIn(0f, 1f)
                            sliderProgress = newProgress
                            if (newProgress >= 0.95f) {
                                sliderProgress = 1f
                                isSliderLocked = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                trackWidthPx = size.width
            }

            // Fill Progress Gradient
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(sliderProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.65f))
                        )
                    )
            )

            // Centered Hint Text
            if (sliderProgress < 0.6f) {
                Text(
                    text = "Slide to verify  ➔",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Slider Thumb
            val maxOffsetDp = with(density) { (trackWidthPx - thumbSizePx).toDp() }
            val currentOffsetDp = maxOffsetDp * sliderProgress

            Box(
                modifier = Modifier
                    .offset { IntOffset(with(density) { currentOffsetDp.toPx() }.roundToInt(), 0) }
                    .size(54.dp)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .shadow(6.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sliderProgress >= 0.95f || isSliderLocked) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Slide Thumb",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Custom Terms & Services Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    isTermsChecked = !isTermsChecked
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isTermsChecked,
                onCheckedChange = {
                    isTermsChecked = it
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryColor,
                    uncheckedColor = Color(0xFF64748B),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "I agree to Terms of Service",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )
        }

        // Complete Verification Action Button
        if (isDone) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Verified",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verified successfully! Continuing...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        } else {
            Button(
                onClick = { completeVerification() },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFF1E293B)
                )
            ) {
                Text("Confirm & Continue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
