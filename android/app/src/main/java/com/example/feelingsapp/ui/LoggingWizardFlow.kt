package com.example.feelingsapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feelingsapp.data.FeelingsData
import com.example.feelingsapp.data.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggingWizardFlow(
    onAddLog: (LogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(0) }
    var selectedCore by remember { mutableStateOf("") }
    var selectedSub1 by remember { mutableStateOf("") }
    var selectedSub2 by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf(5f) }
    var note by remember { mutableStateOf("") }

    val coreColor = if (selectedCore.isNotEmpty()) {
        Color(android.graphics.Color.parseColor(FeelingsData.emotions[selectedCore]?.color ?: "#5f27cd"))
    } else {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            if (step in 1..4) {
                TopAppBar(
                    title = {
                        // Steps Progress Dots
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(end = 48.dp)
                        ) {
                            (1..4).forEach { s ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (s == step) coreColor
                                            else if (s < step) coreColor.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { step -= 1 }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (step) {
                0 -> StartScreen(onStart = {
                    selectedCore = ""
                    selectedSub1 = ""
                    selectedSub2 = ""
                    intensity = 5f
                    note = ""
                    step = 1
                })
                1 -> CoreEmotionSelectScreen(onSelect = {
                    selectedCore = it
                    step = 2
                })
                2 -> Sub1SelectScreen(
                    core = selectedCore,
                    onSelect = {
                        selectedSub1 = it
                        step = 3
                    }
                )
                3 -> Sub2SelectScreen(
                    core = selectedCore,
                    sub1 = selectedSub1,
                    onSelect = {
                        selectedSub2 = it
                        step = 4
                    }
                )
                4 -> IntensitySelectScreen(
                    coreColor = coreColor,
                    selectedCore = selectedCore,
                    selectedSub1 = selectedSub1,
                    selectedSub2 = selectedSub2,
                    intensity = intensity,
                    note = note,
                    onIntensityChange = { intensity = it },
                    onNoteChange = { note = it },
                    onSubmit = {
                        val newLog = LogEntry(
                            id = UUID.randomUUID().toString(),
                            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                            core = selectedCore,
                            sub1 = selectedSub1,
                            sub2 = selectedSub2,
                            intensity = intensity.toInt(),
                            note = note,
                            color = FeelingsData.emotions[selectedCore]?.color ?: "#5f27cd"
                        )
                        onAddLog(newLog)
                        step = 5
                    }
                )
                5 -> SuccessScreen(
                    coreColor = coreColor,
                    selectedSub1 = selectedSub1,
                    selectedSub2 = selectedSub2,
                    intensity = intensity.toInt(),
                    onFinish = { step = 0 }
                )
            }
        }
    }
}

@Composable
fun StartScreen(onStart: () -> Unit) {
    // Calming breathing animation simulation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val breathingText = if (scale > 1.0f) "Inhale" else "Exhale"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.padding(bottom = 64.dp)
        ) {
            Text(
                text = "How are you feeling today?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        // Breathing circle button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onStart)
        ) {
            // Pulse Ring
            Box(
                modifier = Modifier
                    .size((180 * scale).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            )
            
            // Core circle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = breathingText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to log",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CoreEmotionSelectScreen(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Select your main feeling", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Choose the core emotion that matches your current state.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FeelingsData.emotions.keys.toList()) { core ->
                val colorHex = FeelingsData.emotions[core]?.color ?: "#5f27cd"
                val composeColor = Color(android.graphics.Color.parseColor(colorHex))
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(core) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(core, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Sub1SelectScreen(core: String, onSelect: (String) -> Unit) {
    val coreEmotion = FeelingsData.emotions[core] ?: return
    val composeColor = Color(android.graphics.Color.parseColor(coreEmotion.color))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Let's get more specific", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)) {
            Text("Under ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(core, fontSize = 14.sp, color = composeColor, fontWeight = FontWeight.Bold)
            Text(", which path fits best?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(coreEmotion.sub1.keys.toList()) { sub1 ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(sub1) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sub1, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("→", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun Sub2SelectScreen(core: String, sub1: String, onSelect: (String) -> Unit) {
    val coreEmotion = FeelingsData.emotions[core] ?: return
    val options = coreEmotion.sub1[sub1] ?: return
    val composeColor = Color(android.graphics.Color.parseColor(coreEmotion.color))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Do any of these options describe how you are feeling?", fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
        Text(
            "Refining $core → $sub1",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { sub2 ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(sub2) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sub2, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.Check, contentDescription = "Select", tint = composeColor)
                    }
                }
            }
        }
    }
}

@Composable
fun IntensitySelectScreen(
    coreColor: Color,
    selectedCore: String,
    selectedSub1: String,
    selectedSub2: String,
    intensity: Float,
    note: String,
    onIntensityChange: (Float) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("How intense is this emotion?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Rate the strength of your feeling, and add optional notes.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = intensity.toInt().toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = coreColor
                )
                
                val label = when (intensity.toInt()) {
                    in 1..3 -> "Mild"
                    in 4..6 -> "Moderate"
                    in 7..8 -> "Intense"
                    else -> "Overwhelming"
                }
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        activeTrackColor = coreColor,
                        thumbColor = coreColor
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 (Subtle)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("10 (Peak)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Journal reflection (Optional)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("What is causing this feeling? Writing it down can bring clarity...") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = coreColor,
                cursorColor = coreColor
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = coreColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Log Reflection", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun SuccessScreen(
    coreColor: Color,
    selectedSub1: String,
    selectedSub2: String,
    intensity: Int,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(coreColor.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.Check, contentDescription = "Success", tint = coreColor, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Reflection Logged", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your feelings have been recorded. Acknowledge this moment and take a deep breath.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "LOGGED EMOTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(selectedSub2, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("($selectedSub1)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Intensity: $intensity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onFinish,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            modifier = Modifier.width(180.dp)
        ) {
            Text("Back to Home", fontWeight = FontWeight.Medium)
        }
    }
}
