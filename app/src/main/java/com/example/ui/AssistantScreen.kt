package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.feedback.FeedbackEntity
import com.example.engine.IntentType
import kotlinx.coroutines.launch

enum class WorkspaceType(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ASSISTANT("R's AI", Icons.Default.Hearing),
    WRITING("Writing Suite", Icons.Default.Create),
    IMAGE("Image Studio", Icons.Default.Palette),
    DOCUMENTS("Doc & Voice", Icons.Default.Description),
    SMART_HOME("Smart Home", Icons.Default.Home),
    TRANSLATOR("Vision & Trans", Icons.Default.Translate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, modifier: Modifier = Modifier) {
    val chatLog by viewModel.chatLog.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isWakeWordActive by viewModel.isWakeWordActive.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastProcessResult.collectAsStateWithLifecycle()
    val feedbacks by viewModel.feedbacks.collectAsStateWithLifecycle()

    // Workspace and Tab management
    var currentTab by remember { mutableStateOf(0) } // 0 = Assistant, 1 = Feedback Log, 2 = Guide
    var activeWorkspace by remember { mutableStateOf(WorkspaceType.ASSISTANT) }

    // Screen input states
    var textInput by remember { mutableStateOf("") }
    var writingInput by remember { mutableStateOf("") }
    var translateInput by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("English") }
    var imagePromptInput by remember { mutableStateOf("") }

    // Selected items for simulations
    var selectedDocument by remember { mutableStateOf("business_report.pdf") }
    var selectedScreenState by remember { mutableStateOf("System Error Dialog on Main Home launcher") }



    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Auto-scroll chat log on update
    LaunchedEffect(chatLog.size) {
        if (chatLog.isNotEmpty()) {
            listState.animateScrollToItem(chatLog.size - 1)
        }
    }

    // Modern High-Contrast Palette: Deep space with ultraviolet and cyan accents
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF1E1B4B))
    )
    val neonPurple = Color(0xFF8B5CF6)
    val neonCyan = Color(0xFF06B6D4)
    val neonPink = Color(0xFFEC4899)
    val glassBg = Color(0x1F334155)
    val glassBorder = Color(0x3394A3B8)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_rs_ai_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, neonCyan, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "R's AI",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Myanmar HyperAI OS Assistant",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isWakeWordActive) "Hey Bro: On" else "Hey Bro: Off",
                            color = if (isWakeWordActive) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = isWakeWordActive,
                            onCheckedChange = { viewModel.toggleWakeWordEngine(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF064E3B),
                                uncheckedThumbColor = Color(0xFFEF4444),
                                uncheckedTrackColor = Color(0xFF7F1D1D)
                            ),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF030712),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF030712),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Intelligence Workspace") },
                    label = { Text("အလုပ်ခွင်") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonCyan,
                        selectedTextColor = neonCyan,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Local Feedback database") },
                    label = { Text("မှတ်တမ်း") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonPurple,
                        selectedTextColor = neonPurple,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Guide and Technical Help") },
                    label = { Text("လမ်းညွှန်") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Custom Voice Commands Settings") },
                    label = { Text("ဆက်တင်") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = neonCyan,
                        selectedTextColor = neonCyan,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1E293B)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Horizontal Sub-workspace selector
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF030712))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(WorkspaceType.values()) { workspace ->
                                val isSelected = activeWorkspace == workspace
                                val chipBg = if (isSelected) neonCyan.copy(alpha = 0.2f) else glassBg
                                val chipBorder = if (isSelected) neonCyan else glassBorder
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                        .clickable { activeWorkspace = workspace }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            workspace.icon,
                                            contentDescription = workspace.displayName,
                                            tint = if (isSelected) neonCyan else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            workspace.displayName,
                                            color = if (isSelected) neonCyan else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Render Workspace Content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            when (activeWorkspace) {
                                WorkspaceType.ASSISTANT -> {
                                    // 1. VOICE ASSISTANT WITH GLOWING ORB
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0x33334155)),
                                            border = BorderStroke(1.dp, glassBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            "Burmese AI Engine Status",
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            "Hybrid Local NLU + Cloud Gemini 3.5",
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { viewModel.simulateWakeWordTrigger() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.testTag("wake_word_simulate_button")
                                                    ) {
                                                        Icon(Icons.Default.ElectricBolt, "Trigger Orb", modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Hey Bro!", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Spacer(Modifier.height(12.dp))
                                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                                Spacer(Modifier.height(12.dp))
                                                
                                                val activeProfile by viewModel.currentVoiceProfile.collectAsStateWithLifecycle()
                                                
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        "Live Voice Chat Profile (အသံပတ်ဝန်းကျင် ရွေးချယ်မှု)",
                                                        fontWeight = FontWeight.Bold,
                                                        color = neonCyan,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                     ) {
                                                         // Indoor Option Button
                                                         Button(
                                                             onClick = { viewModel.setVoiceProfile(com.example.engine.VoiceProfile.INDOOR) },
                                                             colors = ButtonDefaults.buttonColors(
                                                                 containerColor = if (activeProfile == com.example.engine.VoiceProfile.INDOOR) neonCyan else Color(0xFF1E293B)
                                                             ),
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(36.dp)
                                                                 .testTag("profile_indoor_btn"),
                                                             contentPadding = PaddingValues(0.dp)
                                                         ) {
                                                             Icon(
                                                                 Icons.Default.Home,
                                                                 contentDescription = "Indoor Mode",
                                                                 tint = if (activeProfile == com.example.engine.VoiceProfile.INDOOR) Color.Black else Color.White,
                                                                 modifier = Modifier.size(14.dp)
                                                             )
                                                             Spacer(Modifier.width(4.dp))
                                                             Text(
                                                                 "အိမ်တွင်း (Indoor)",
                                                                 fontSize = 11.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 color = if (activeProfile == com.example.engine.VoiceProfile.INDOOR) Color.Black else Color.White
                                                             )
                                                         }
                                                         
                                                         // Outdoor Option Button
                                                         Button(
                                                             onClick = { viewModel.setVoiceProfile(com.example.engine.VoiceProfile.OUTDOOR) },
                                                             colors = ButtonDefaults.buttonColors(
                                                                 containerColor = if (activeProfile == com.example.engine.VoiceProfile.OUTDOOR) neonPurple else Color(0xFF1E293B)
                                                             ),
                                                             modifier = Modifier
                                                                 .weight(1f)
                                                                 .height(36.dp)
                                                                 .testTag("profile_outdoor_btn"),
                                                             contentPadding = PaddingValues(0.dp)
                                                         ) {
                                                             Icon(
                                                                 Icons.Default.VolumeUp,
                                                                 contentDescription = "Outdoor Mode",
                                                                 tint = if (activeProfile == com.example.engine.VoiceProfile.OUTDOOR) Color.Black else Color.White,
                                                                 modifier = Modifier.size(14.dp)
                                                             )
                                                             Spacer(Modifier.width(4.dp))
                                                             Text(
                                                                 "အပြင်ဘက် (Outdoor)",
                                                                 fontSize = 11.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 color = if (activeProfile == com.example.engine.VoiceProfile.OUTDOOR) Color.Black else Color.White
                                                             )
                                                         }
                                                     }
                                                     
                                                     // Helper text
                                                     Text(
                                                         text = if (activeProfile == com.example.engine.VoiceProfile.INDOOR) {
                                                             "🏡 Quiet-focus mode. Standard speakers. Clean mic sensitivity."
                                                         } else {
                                                             "📢 High ambient noise cancellation. Max audio boost (+12dB). Clear speech rate."
                                                         },
                                                         color = Color.LightGray,
                                                         fontSize = 10.sp,
                                                         modifier = Modifier.padding(top = 6.dp)
                                                     )
                                                 }

                                                 Spacer(Modifier.height(16.dp))

                                                // Gorgeous Interactive Orb Animation Frame
                                                Box(
                                                    modifier = Modifier
                                                        .size(100.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.radialGradient(
                                                                colors = listOf(
                                                                    if (isListening) Color.Red else if (isProcessing) neonPurple else neonCyan,
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            BorderStroke(
                                                                2.dp,
                                                                if (isListening) Color.Red else if (isProcessing) neonPurple else neonCyan
                                                            ),
                                                            CircleShape
                                                        )
                                                        .clickable {
                                                            if (isListening) viewModel.cancelVoiceCapture() else viewModel.startVoiceCapture()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    // Rotating / pulsing core
                                                    val pulseScale by rememberInfiniteTransition(label = "").animateFloat(
                                                        initialValue = 0.8f,
                                                        targetValue = 1.2f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(1000, easing = EaseInOutSine),
                                                            repeatMode = RepeatMode.Reverse
                                                        ),
                                                        label = ""
                                                    )
                                                    Image(
                                                        painter = painterResource(id = R.drawable.img_rs_ai_logo),
                                                        contentDescription = "Core Logo",
                                                        modifier = Modifier
                                                            .size(70.dp * pulseScale)
                                                            .clip(CircleShape)
                                                    )
                                                }

                                                Spacer(Modifier.height(12.dp))

                                                // Dynamic Waveform or Status Box
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color(0xFF030712))
                                                        .padding(horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isListening) {
                                                        VoiceWaveform(color = Color.Red)
                                                        Text("အသံဖမ်းယူနေပါသည်... စကားပြောပါ", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    } else if (isProcessing) {
                                                        VoiceWaveform(color = neonPurple)
                                                        Text("R's AI စဉ်းစားနေပါသည်...", color = neonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Text(
                                                            "အသံဖြင့်ခေါ်ဆိုရန် အလယ်မှ Orb ကို နှိပ်ပါ",
                                                            color = Color.Gray,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Chat messages
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(chatLog) { msg ->
                                                ChatBubble(message = msg)
                                            }
                                        }

                                        // Suggestions Shortcuts Row
                                        HorizontalShortcutRow(
                                            onSelect = { query -> viewModel.processTextQuery(query) }
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        // Text Input and Microphone Button Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .navigationBarsPadding(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = textInput,
                                                onValueChange = { textInput = it },
                                                placeholder = { Text("အမိန့်ပေးချက်များ သို့မဟုတ် မေးခွန်းများ...", color = Color.Gray, fontSize = 13.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("chat_text_input")
                                                    .clip(RoundedCornerShape(24.dp)),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color(0xFF1E293B),
                                                    unfocusedContainerColor = Color(0xFF1E293B),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                                keyboardActions = KeyboardActions(
                                                    onSend = {
                                                        if (textInput.isNotBlank()) {
                                                            viewModel.processTextQuery(textInput)
                                                            textInput = ""
                                                            focusManager.clearFocus()
                                                            keyboardController?.hide()
                                                        }
                                                    }
                                                )
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            FloatingActionButton(
                                                onClick = {
                                                    if (isListening) viewModel.cancelVoiceCapture() else viewModel.startVoiceCapture()
                                                },
                                                containerColor = if (isListening) Color.Red else neonCyan,
                                                contentColor = Color.White,
                                                shape = CircleShape,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .testTag("voice_record_fab")
                                            ) {
                                                Icon(
                                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Outlined.Mic,
                                                    contentDescription = "Voice Assistant Capture Button"
                                                )
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.WRITING -> {
                                    // 2. AI WRITING WORKSPACE
                                    val writingOutput by viewModel.writingOutput.collectAsStateWithLifecycle()
                                    val isWritingLoading by viewModel.isWritingLoading.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_home_banner),
                                                    contentDescription = "Creative Banner",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.5f))
                                                        .padding(12.dp),
                                                    contentAlignment = Alignment.BottomStart
                                                ) {
                                                    Column {
                                                        Text("R's Writing Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                                        Text("AI စနစ်ဖြင့် စာတို/အီးမေးလ်များ ဖန်တီးတည်ဆောက်ပေးခြင်း", color = Color.LightGray, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = writingInput,
                                                onValueChange = { writingInput = it },
                                                label = { Text("စာရေးသားရန် အကြောင်းအရာ ရေးသားပါ", color = Color.LightGray) },
                                                placeholder = { Text("ဥပမာ- 'ခွင့်တိုင်စာတစ်စောင် ရေးသားပေးပါ'", color = Color.DarkGray) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = neonPurple,
                                                    unfocusedBorderColor = glassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val templates = listOf(
                                                    "Email" to "📧 Email",
                                                    "Summary" to "📝 Summary",
                                                    "Rewrite" to "✍️ Rewrite",
                                                    "Grammar" to "🔍 Grammar"
                                                )
                                                templates.forEach { (type, label) ->
                                                    Button(
                                                        onClick = {
                                                            viewModel.generateWriting(writingInput, type)
                                                            focusManager.clearFocus()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple.copy(alpha = 0.8f)),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isWritingLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        if (writingOutput.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("AI Generated Content", fontWeight = FontWeight.Bold, color = neonPurple, fontSize = 13.sp)
                                                            IconButton(
                                                                onClick = {
                                                                    clipboardManager.setText(AnnotatedString(writingOutput))
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.ContentCopy, "Copy", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(
                                                            text = writingOutput,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            lineHeight = 20.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.IMAGE -> {
                                    // 3. AI IMAGE STUDIO (WALLPAPER & PHOTO EDITOR)
                                    var generatedImageResult by remember { mutableStateOf<Int?>(null) }
                                    var isImageLoading by remember { mutableStateOf(false) }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text(
                                                "R's Image Studio",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                "Wallpaper Generator & AI Photo Expanded Filters",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = imagePromptInput,
                                                onValueChange = { imagePromptInput = it },
                                                label = { Text("ဖန်တီးချင်သော ဓာတ်ပုံ ဖော်ပြချက် ရေးသားပါ", color = Color.LightGray) },
                                                placeholder = { Text("ဥပမာ- 'လှပသော မြန်မာ့ရွှေတိဂုံစေတီတော် ရှုခင်း Wallpaper'", color = Color.DarkGray) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = neonPink,
                                                    unfocusedBorderColor = glassBorder,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White
                                                )
                                            )
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val imageTools = listOf(
                                                    "Wallpaper" to "🎨 Wallpaper",
                                                    "Expand" to "📐 Expand",
                                                    "RemoveObj" to "🧹 Remove Object",
                                                    "SkyReplace" to "🌌 Sky Change"
                                                )
                                                imageTools.forEach { (tool, label) ->
                                                    Button(
                                                        onClick = {
                                                            isImageLoading = true
                                                            generatedImageResult = null
                                                            scope.launch {
                                                                kotlinx.coroutines.delay(2000)
                                                                isImageLoading = false
                                                                // Point to generated banner or logo as realistic simulated outcomes
                                                                generatedImageResult = if (tool == "Wallpaper") R.drawable.img_home_banner else R.drawable.img_rs_ai_logo
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                                                        modifier = Modifier.weight(1.5f),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isImageLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        CircularProgressIndicator(color = neonPink)
                                                        Spacer(Modifier.height(4.dp))
                                                        Text("AI စနစ်မှ ပုံဆွဲနေပါသည်...", color = Color.LightGray, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }

                                        generatedImageResult?.let { imgRes ->
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPink.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("AI Processed Image", fontWeight = FontWeight.Bold, color = neonPink, fontSize = 13.sp)
                                                            Button(
                                                                onClick = { /* Simulated action */ },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("Save Wallpaper", fontSize = 10.sp)
                                                            }
                                                        }
                                                        Spacer(Modifier.height(10.dp))
                                                        Image(
                                                            painter = painterResource(id = imgRes),
                                                            contentDescription = "Simulated Image Result",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.DOCUMENTS -> {
                                    // 4. DOCUMENT SUMMARIZER & VOICE TRANSCRIPTS
                                    val documentSummary by viewModel.documentSummary.collectAsStateWithLifecycle()
                                    val isDocLoading by viewModel.isDocumentSummaryLoading.collectAsStateWithLifecycle()
                                    val meetingTranscript by viewModel.meetingTranscript.collectAsStateWithLifecycle()
                                    val isTranscriptLoading by viewModel.isTranscriptLoading.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text("R's Document & Voice Hub", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text("PDF Summarizers & Multi-speaker Meeting transcripts", color = Color.LightGray, fontSize = 11.sp)
                                        }

                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3394A3B8)),
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("Select PDF Document to Analyze", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Spacer(Modifier.height(8.dp))
                                                    val docs = listOf("business_report.pdf", "study_guide.pdf", "trip_itinerary.pdf")
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        docs.forEach { doc ->
                                                            val isDocSelected = selectedDocument == doc
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isDocSelected) neonCyan.copy(alpha = 0.3f) else Color.DarkGray)
                                                                    .clickable { selectedDocument = doc }
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(doc, color = if (isDocSelected) neonCyan else Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.height(10.dp))
                                                    Button(
                                                        onClick = {
                                                            viewModel.summarizeDocument(selectedDocument, "PDF report")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Summarize Document (အနှစ်ချုပ် ထုတ်ပါ)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isDocLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonCyan)
                                                }
                                            }
                                        }

                                        if (documentSummary.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Document Insights Summary", fontWeight = FontWeight.Bold, color = neonCyan, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(documentSummary, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3394A3B8)),
                                                border = BorderStroke(1.dp, glassBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("AI Meeting Transcript", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Text("အသံဖမ်းယူ၍ စာသားဖြင့် မှတ်တမ်းအပြည့်အစုံထုတ်ခြင်း", color = Color.LightGray, fontSize = 11.sp)
                                                    Spacer(Modifier.height(10.dp))
                                                    Button(
                                                        onClick = { viewModel.startMeetingTranscript() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Transcribe Live Meeting (စတင် မှတ်တမ်းတင်ရန်)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isTranscriptLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        if (meetingTranscript.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Generated Meeting Transcript", fontWeight = FontWeight.Bold, color = neonPurple, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(meetingTranscript, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.SMART_HOME -> {
                                    // 5. SMART HOME CONTROLLER
                                    val acStatus by viewModel.acStatus.collectAsStateWithLifecycle()
                                    val acTemperature by viewModel.acTemperature.collectAsStateWithLifecycle()
                                    val tvStatus by viewModel.tvStatus.collectAsStateWithLifecycle()
                                    val tvChannel by viewModel.tvChannel.collectAsStateWithLifecycle()
                                    val lightStatus by viewModel.lightStatus.collectAsStateWithLifecycle()
                                    val lightColor by viewModel.lightColor.collectAsStateWithLifecycle()
                                    val lightIntensity by viewModel.lightIntensity.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text("R's Smart Home Control Center", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text("အိမ်သုံးပစ္စည်းများကို AI ဖြင့် အော့ဖ်လိုင်း အဝေးမှ ထိန်းချုပ်ရန်", color = Color.LightGray, fontSize = 11.sp)
                                        }

                                        // Air Conditioner Card
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3306B6D4)),
                                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("❄️ Air Conditioner (လေအေးပေးစက်)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                            Text(if (acStatus) "Active - $acTemperature°C" else "OFF", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                        Switch(
                                                            checked = acStatus,
                                                            onCheckedChange = { viewModel.toggleAc() },
                                                            colors = SwitchDefaults.colors(checkedThumbColor = neonCyan)
                                                        )
                                                    }
                                                    if (acStatus) {
                                                        Spacer(Modifier.height(10.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Button(
                                                                onClick = { viewModel.adjustAcTemp(false) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                                            ) {
                                                                Text("- Temp")
                                                            }
                                                            Text("$acTemperature°C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                            Button(
                                                                onClick = { viewModel.adjustAcTemp(true) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                                            ) {
                                                                Text("+ Temp")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Smart TV Card
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x338B5CF6)),
                                                border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("📺 Smart TV (တီဗွီ)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                            Text(if (tvStatus) "ON - Channel: $tvChannel" else "OFF", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                        Switch(
                                                            checked = tvStatus,
                                                            onCheckedChange = { viewModel.toggleTv() },
                                                            colors = SwitchDefaults.colors(checkedThumbColor = neonPurple)
                                                        )
                                                    }
                                                    if (tvStatus) {
                                                        Spacer(Modifier.height(10.dp))
                                                        Text("Select Channel", color = Color.LightGray, fontSize = 11.sp)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val channels = listOf("MRTV HD", "CHANNEL 7", "MESSENGER", "FORTUNE")
                                                            channels.forEach { ch ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (tvChannel == ch) neonPurple.copy(alpha = 0.3f) else Color.DarkGray)
                                                                        .clickable { viewModel.setTvChannel(ch) }
                                                                        .padding(8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(ch, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Smart Lights Card
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x33EC4899)),
                                                border = BorderStroke(1.dp, neonPink.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("💡 Smart Light Color themes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                            Text(if (lightStatus) "ON - $lightColor ($lightIntensity%)" else "OFF", color = Color.LightGray, fontSize = 11.sp)
                                                        }
                                                        Switch(
                                                            checked = lightStatus,
                                                            onCheckedChange = { viewModel.toggleLight() },
                                                            colors = SwitchDefaults.colors(checkedThumbColor = neonPink)
                                                        )
                                                    }
                                                    if (lightStatus) {
                                                        Spacer(Modifier.height(10.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            val lightColors = listOf("Warm Gold", "Ocean Blue", "Electric Purple", "Sunset Red")
                                                            lightColors.forEach { color ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (lightColor == color) neonPink.copy(alpha = 0.3f) else Color.DarkGray)
                                                                        .clickable { viewModel.setLightColor(color) }
                                                                        .padding(8.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(color, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                        Spacer(Modifier.height(10.dp))
                                                        Text("Brightness Intensity", color = Color.LightGray, fontSize = 11.sp)
                                                        Slider(
                                                            value = lightIntensity.toFloat(),
                                                            onValueChange = { viewModel.adjustLightIntensity(it.toInt()) },
                                                            valueRange = 10f..100f,
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = neonPink,
                                                                activeTrackColor = neonPink
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkspaceType.TRANSLATOR -> {
                                    // 6. SCREEN VISION & TRANSLATION INTERPRETER
                                    val translationOutput by viewModel.translationOutput.collectAsStateWithLifecycle()
                                    val isTransLoading by viewModel.isTranslationLoading.collectAsStateWithLifecycle()
                                    val screenVisionResult by viewModel.screenVisionResult.collectAsStateWithLifecycle()
                                    val isVisionLoading by viewModel.isScreenVisionLoading.collectAsStateWithLifecycle()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Text("R's Screen Vision & Interpreter", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                            Text("Real-time Gemini Translation & Screen error analysis", color = Color.LightGray, fontSize = 11.sp)
                                        }

                                        // Translation Panel
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x338B5CF6)),
                                                border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("AI Translator & Interpreter", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Spacer(Modifier.height(8.dp))
                                                    OutlinedTextField(
                                                        value = translateInput,
                                                        onValueChange = { translateInput = it },
                                                        label = { Text("စာသား သို့မဟုတ် စကားစု ရေးသားပါ", color = Color.LightGray) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = neonPurple,
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White
                                                        )
                                                    )
                                                    Spacer(Modifier.height(10.dp))
                                                    Text("Select Target Language", color = Color.LightGray, fontSize = 11.sp)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        val langs = listOf("English", "Chinese", "Japanese", "Korean", "Thai")
                                                        langs.forEach { lang ->
                                                            val isLangSelected = targetLanguage == lang
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(if (isLangSelected) neonPurple.copy(alpha = 0.3f) else Color.DarkGray)
                                                                    .clickable { targetLanguage = lang }
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(lang, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.height(12.dp))
                                                    Button(
                                                        onClick = { viewModel.translateText(translateInput, targetLanguage) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Translate (ဘာသာပြန်ဆိုမည်)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isTransLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonPurple)
                                                }
                                            }
                                        }

                                        if (translationOutput.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonPurple.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Translation Result", fontWeight = FontWeight.Bold, color = neonPurple, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(translationOutput, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                                    }
                                                }
                                            }
                                        }

                                        // Screen Vision Panel
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0x3306B6D4)),
                                                border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text("AI Screen Understanding (Vision)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Text("ဖုန်းမျက်နှာပြင်တွင် ဖြစ်ပျက်နေသည်များကို ဆန်းစစ်သုံးသပ်ပေးခြင်း", color = Color.LightGray, fontSize = 11.sp)
                                                    Spacer(Modifier.height(10.dp))
                                                    val screenStates = listOf(
                                                        "System Error Dialog on Main Home launcher",
                                                        "Play Store download stalled on Telegram App",
                                                        "Myanmar fonts configuration page mismatch error"
                                                    )
                                                    screenStates.forEach { state ->
                                                        val isStateSelected = selectedScreenState == state
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isStateSelected) neonCyan.copy(alpha = 0.2f) else Color.DarkGray)
                                                                .clickable { selectedScreenState = state }
                                                                .padding(10.dp)
                                                        ) {
                                                            Text(state, color = if (isStateSelected) neonCyan else Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    Spacer(Modifier.height(12.dp))
                                                    Button(
                                                        onClick = { viewModel.scanScreen(selectedScreenState) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Analyze Screen content (စကရင်ကို ခွဲခြမ်းစိတ်ဖြာပါ)", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (isVisionLoading) {
                                            item {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = neonCyan)
                                                }
                                            }
                                        }

                                        if (screenVisionResult.isNotBlank()) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                    border = BorderStroke(1.dp, neonCyan.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text("Screen Intelligence Output", fontWeight = FontWeight.Bold, color = neonCyan, fontSize = 13.sp)
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(screenVisionResult, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Local Offline Feedback Database Log Viewer
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Local Feedback Logs",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "ဒေတာဘေ့စ်ရှိ သုံးသပ်ချက်နှင့် ပြင်ဆင်ချက်များ",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                            if (feedbacks.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearAllFeedback() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                    modifier = Modifier.testTag("clear_feedback_button")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, "Clear database")
                                    Spacer(Modifier.width(4.dp))
                                    Text("အားလုံးဖျက်ပါ")
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (feedbacks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Storage,
                                        contentDescription = "Empty Database",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "အော့ဖ်လိုင်း သုံးသပ်ချက်များ မရှိသေးပါ",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "အကူအညီပေးသူ၏ အမေးအဖြေများကို 👍 သို့မဟုတ် 👎 ပေးပြီး စမ်းသပ်သိမ်းဆည်းနိုင်ပါသည်ခင်ဗျာ။",
                                        color = Color.DarkGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                             ) {
                                items(feedbacks) { item ->
                                    FeedbackCard(item = item, onDelete = { viewModel.deleteFeedback(item.id) })
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: Offline Technical Architecture & Build Guide
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "R's AI (Myanmar HyperAI)",
                                fontWeight = FontWeight.Bold,
                                color = neonCyan,
                                fontSize = 18.sp
                            )
                            Text(
                                "အော့ဖ်လိုင်းနှင့် အွန်လိုင်းစနစ်များ၏ နည်းပညာ တည်ဆောက်ပုံ အနှစ်ချုပ်",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        }
                        item {
                            GuideCard(
                                title = "STT (Speech-to-Text)",
                                description = "အသံလှိုင်းများကို စာသားအဖြစ် ပြောင်းလဲခြင်း",
                                details = "Model: Whisper-Tiny (Fine-tuned on Burmese)\nFormat: TensorFlow Lite (FP16/INT8 Quantized)\nProcessing: Offline inference via TFLite runtime C++ dynamic engine wrapper."
                            )
                        }
                        item {
                            GuideCard(
                                title = "NLU (Natural Language Understanding)",
                                description = "စာသား၏ ရည်ရွယ်ချက်နှင့် စကားလုံးများ ခွဲထုတ်ခြင်း",
                                details = "Model: mBART or XLM-RoBERTa (Burmese Instruction dataset)\nFormat: ONNX Runtime (Mobile Optimized)\nCapabilities: Wifi, Bluetooth, Volume, Flashlight, Brightness, DND, Calling, Notes, Knowledge QA, Reminders."
                            )
                        }
                        item {
                            GuideCard(
                                title = "TTS (Text-to-Speech)",
                                description = "စာသားမှ သဘာဝကျသော မြန်မာအသံ ထွက်ပေါ်ခြင်း",
                                details = "Model: Tacotron2 or VITS (Myanmar Male/Female voice audio dataset)\nFormat: ONNX Model / native engine bridge\nSpeed: Real-time RTF < 0.6 on average Android SoC hardware acceleration."
                            )
                        }
                        item {
                            GuideCard(
                                title = "Continuous Wake Word (KWS)",
                                description = "အမြဲတမ်း နားထောင်ပြီး အမိန့်ပေးရန် အဆင်သင့်ဖြစ်စေခြင်း",
                                details = "Model: MicroSpeech CNN keyword spotter (110 KB)\nFormat: TensorFlow Lite Micro (Continuous audio circular buffer pooling)\nKeyword: \"Hey Bro\" (Hello Assistant)."
                            )
                        }
                    }
                }
                3 -> {
                    // TAB 3: Custom Voice Command Mappings Configuration Settings
                    val voiceMappings by viewModel.voiceMappings.collectAsStateWithLifecycle()
                    var newPhrase by remember { mutableStateOf("") }
                    var selectedIntent by remember { mutableStateOf(IntentType.TOGGLE_DARK_MODE) }
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "Custom Voice Commands (စိတ်ကြိုက် အမိန့်ပေးစနစ်များ)",
                                fontWeight = FontWeight.Bold,
                                color = neonCyan,
                                fontSize = 18.sp
                            )
                            Text(
                                "ကိုယ်ပိုင် Burmese voice commands များကို သတ်မှတ်ပြီး ဖုန်းလုပ်ဆောင်ချက်များနှင့် ချိတ်ဆက်သိမ်းဆည်းပါ (Settings survive across sessions via SharedPreferences)",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        }

                        // Form card to add new mappings
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x33334155)),
                                border = BorderStroke(1.dp, glassBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "အမိန့်အသစ် ထည့်သွင်းရန် (Add Custom Command Mapping)",
                                        fontWeight = FontWeight.Bold,
                                        color = neonPurple,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    // Input for trigger phrase
                                    OutlinedTextField(
                                        value = newPhrase,
                                        onValueChange = { newPhrase = it },
                                        label = { Text("ပြောမည့် စကားစု (e.g. အိပ်ချင်ပြီ)", color = Color.Gray) },
                                        placeholder = { Text("အမိန့်ပေး စကားလုံး ရေးပါ...", color = Color.DarkGray) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("custom_phrase_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = neonCyan,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    // Dropdown for target IntentType
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { dropdownExpanded = true },
                                            modifier = Modifier.fillMaxWidth().testTag("intent_dropdown_trigger"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("ခေါ်ယူမည့် လုပ်ဆောင်ချက်: ${selectedIntent.name}")
                                                Icon(Icons.Default.ArrowDropDown, "Expand dropdown")
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1E293B)).width(300.dp)
                                        ) {
                                            val validIntents = listOf(
                                                IntentType.TOGGLE_DARK_MODE,
                                                IntentType.TOGGLE_FLASHLIGHT,
                                                IntentType.TOGGLE_WIFI,
                                                IntentType.TOGGLE_BLUETOOTH,
                                                IntentType.VOLUME_UP,
                                                IntentType.VOLUME_DOWN,
                                                IntentType.MUTE_VOLUME,
                                                IntentType.TAKE_SCREENSHOT,
                                                IntentType.START_SCREEN_RECORDING,
                                                IntentType.OPEN_GALLERY,
                                                IntentType.CHECK_BATTERY,
                                                IntentType.CHECK_DIAGNOSTICS,
                                                IntentType.TOGGLE_POWER_SAVING,
                                                IntentType.TOGGLE_DND
                                            )
                                            validIntents.forEach { intent ->
                                                DropdownMenuItem(
                                                    text = { Text(intent.name, color = Color.White, fontSize = 13.sp) },
                                                    onClick = {
                                                        selectedIntent = intent
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (newPhrase.isNotBlank()) {
                                                viewModel.saveVoiceMapping(newPhrase, selectedIntent)
                                                newPhrase = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("save_mapping_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
                                    ) {
                                        Icon(Icons.Default.Add, "Add")
                                        Spacer(Modifier.width(6.dp))
                                        Text("အမိန့်အဖြစ် သိမ်းဆည်းမည် (Save Command)", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }

                        // Saved mappings list header
                        item {
                            Text(
                                "လက်ရှိ သိမ်းဆည်းထားသော အမိန့်များ (Saved voice mappings)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        if (voiceMappings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("စိတ်ကြိုက် သတ်မှတ်ထားသော အမိန့်များ မရှိသေးပါခင်ဗျာ။", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(voiceMappings.toList()) { (phrase, intent) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("mapping_item_${phrase}"),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val icon = when (intent) {
                                                IntentType.TOGGLE_WIFI -> Icons.Default.Settings
                                                IntentType.TOGGLE_BLUETOOTH -> Icons.Default.Settings
                                                IntentType.VOLUME_UP -> Icons.Default.Notifications
                                                IntentType.VOLUME_DOWN -> Icons.Default.Notifications
                                                IntentType.MUTE_VOLUME -> Icons.Default.Notifications
                                                IntentType.TOGGLE_FLASHLIGHT -> Icons.Default.Build
                                                IntentType.TOGGLE_DARK_MODE -> Icons.Default.Settings
                                                IntentType.TAKE_SCREENSHOT -> Icons.Default.Create
                                                IntentType.START_SCREEN_RECORDING -> Icons.Default.Create
                                                IntentType.OPEN_GALLERY -> Icons.Default.Palette
                                                IntentType.CHECK_BATTERY -> Icons.Default.Info
                                                IntentType.CHECK_DIAGNOSTICS -> Icons.Default.Info
                                                IntentType.TOGGLE_POWER_SAVING -> Icons.Default.Info
                                                IntentType.TOGGLE_DND -> Icons.Default.Settings
                                                else -> Icons.Default.Settings
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(neonCyan.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(icon, contentDescription = intent.name, tint = neonCyan, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "\"$phrase\"",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "လုပ်ဆောင်ချက်: ${intent.name}",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteVoiceMapping(phrase) },
                                            modifier = Modifier.testTag("delete_mapping_${phrase}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete mapping", tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == SenderType.USER
    val isSystem = message.sender == SenderType.SYSTEM

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = when {
            isSystem -> Alignment.Center
            isUser -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (isSystem) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF334155))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 16.dp
                            )
                        )
                        .background(
                            if (isUser) Color(0xFF8B5CF6) else Color(0xFF1E293B)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        // Show classification badge for assistant messages
                        message.intent?.let { int ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Intent: ${int.name}",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Show physical action execution note
                message.actionDetails?.let { act ->
                    Text(
                        text = act,
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackCard(item: FeedbackEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, if (item.rating == 1) Color(0xFF059669) else Color(0xFFDC2626))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.rating == 1) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                        contentDescription = "Rating status",
                        tint = if (item.rating == 1) Color(0xFF34D399) else Color(0xFFF87171),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (item.rating == 1) "ကျေနပ်မှု ရှိသည်" else "ပြန်လည်ပြင်ဆင်ရန်",
                        color = if (item.rating == 1) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, "Delete item", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("User Speech (Prompt):", color = Color.Gray, fontSize = 10.sp)
            Text(item.prompt, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(4.dp))
            Text("Assistant Answer (Response):", color = Color.Gray, fontSize = 10.sp)
            Text(item.response, color = Color.LightGray, fontSize = 13.sp)

            item.correctionComment?.let { comm ->
                if (comm.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("ညွှန်ကြားပြင်ဆင်ချက် (Correction Comments):", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(comm, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideCard(title: String, description: String, details: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 14.sp)
            Text(description, color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .padding(8.dp)
            ) {
                Text(
                    text = details,
                    color = Color(0xFFA5B4FC),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun HorizontalShortcutRow(onSelect: (String) -> Unit) {
    val shortcuts = listOf(
        "ဝိုင်ဖိုင် ဖွင့်ပါ" to "🌐 Wifi ဖွင့်",
        "ဘလူးတုသ် ဖွင့်ပါ" to "📡 Bluetooth",
        "ဓာတ်မီး ဖွင့်ပေးပါ" to "🔦 Flashlight ဖွင့်",
        "ဓာတ်မီး ပိတ်ပါ" to "🔦 Flashlight ပိတ်",
        "ဘက်ထရီ စစ်ပေးပါ" to "🔋 Battery စစ်",
        "အမှောင်မုဒ် ပြောင်းပါ" to "🌒 Dark Mode",
        "စကရင်ရှော့ ရိုက်ပါ" to "📸 Screenshot",
        "စကရင်ဗီဒီယို ရိုက်ပါ" to "🎥 Record Screen",
        "ပုံများ ဖွင့်ပါ" to "🖼️ Gallery",
        "မက်ဆေ့ခ်ျ ပို့ပါ" to "💬 SMS စာပို့",
        "09123456789 ကို ဖုန်းခေါ်ပေးပါ" to "📞 Call",
        "တည်နေရာ ပြပါဦး" to "🗺️ Maps",
        "ချိန်းဆိုမှု ပြက္ခဒိန်ထဲ ထည့်ပါ" to "🗓️ Calendar",
        "မှတ်စု ရေးမှတ်ပါ" to "📝 Note",
        "ပုဂံအကြောင်း ပြောပြပါ" to "🏛️ Pagan QA",
        "ကဗျာတစ်ပုဒ် ရေးပြပါ" to "✍️ Poem"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(shortcuts) { (query, label) ->
            SuggestionChip(
                onClick = { onSelect(query) },
                label = { Text(label, color = Color.White) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

@Composable
fun VoiceWaveform(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2
        val path = Path()

        path.moveTo(0f, midY)
        for (x in 0..width.toInt() step 5) {
            val relativeX = x / width
            val sine = Math.sin((relativeX * 3 * Math.PI) + waveOffset).toFloat()
            val dampening = (1f - Math.abs(relativeX - 0.5f) * 2).coerceIn(0f, 1f)
            val y = midY + sine * 15f * dampening
            path.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = path,
            color = color.copy(alpha = 0.5f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
