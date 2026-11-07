package com.example.urduai

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.util.Locale

// ========== COLORS ==========
private val GreenPrimary = Color(0xFF1B5E20)
private val LightBackground = Color(0xFFF5F5F5)
private val LightSurface = Color.White
private val LightBubbleAI = Color(0xFFE8F5E9)
private val TextDark = Color(0xFF212121)
private val TextGrey = Color(0xFF616161)

// ========== FONT ==========
private val UrduFont = FontFamily(
    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal)
)

// ========== MAIN ACTIVITY ==========
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = GreenPrimary,
                    onPrimary = Color.White,
                    background = LightBackground,
                    onBackground = TextDark,
                    surface = LightSurface,
                    onSurface = TextDark,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// ========== DATA CLASS ==========
data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// ========== CHAT SCREEN ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController) {
    val context = LocalContext.current
    val session = remember { UserSession(context) }
    val db = remember { DatabaseHelper(context) }

    val userEmail = session.getLoggedInEmail() ?: "guest"
    val isGuest = (userEmail == "guest")

    val messages = remember { mutableStateListOf<Message>() }
    val inputText = remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isTyping by remember { mutableStateOf(false) }

    // GUEST LIMIT LOGIC
    val sharedPref = context.getSharedPreferences("GuestPrefs", Context.MODE_PRIVATE)
    var showLoginDialog by remember { mutableStateOf(false) }

    // ----- Guest limit dialog (English) -----
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            containerColor = LightSurface,
            title = {
                Text(
                    text = "Limit Reached!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "You can ask only 3 questions in guest mode. Please Login or Sign Up to continue using UrduAI.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = TextGrey,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLoginDialog = false
                    navController.navigate("login") { popUpTo("chat") { inclusive = true } }
                }) {
                    Text("Login / Sign Up", fontSize = 16.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Cancel", fontSize = 16.sp, color = TextGrey)
                }
            }
        )
    }

    // ========== MIC / SPEECH ==========
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(context, "آواز سمجھ نہیں آئی، دوبارہ کوشش کریں۔", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrEmpty()) inputText.value = spoken
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
    }

    fun launchSpeech() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Speech recognition is not available on this phone.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeech()
        } else {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
        }
    }


    LaunchedEffect(userEmail) {
        messages.clear()
        val oldChats = db.getChatHistory(userEmail)
        if (oldChats.isEmpty()) {
            messages.add(Message(text = "السلام علیکم! میں اردو اے آئی ہوں۔ آپ مجھ سے اردو میں سوال پوچھ سکتے ہیں۔", isUser = false))
        } else {
            messages.addAll(oldChats)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun sendMessage(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return

        // English text par toast
        if (q.contains(Regex("[a-zA-Z]"))) {
            Toast.makeText(context, "I am Urdu AI. Please ask only in Urdu.", Toast.LENGTH_LONG).show()
            return
        }

        if (isGuest) {
            val currentCount = sharedPref.getInt("guest_question_count", 0)
            if (currentCount >= 3) {
                showLoginDialog = true
                return
            } else {
                sharedPref.edit().putInt("guest_question_count", currentCount + 1).apply()
            }
        }

        messages.add(Message(text = q, isUser = true))
        db.saveMessage(userEmail, q, true)
        inputText.value = ""
        isTyping = true


        scope.launch {
            try{
                val historyList= messages.takeLast(4).map {
                    HistoryMessage(
                        role=if(it.isUser)"user" else "assistant",
                        content=it.text
                    )
                }

                val result= RetrofitClient.getApi(context).chat(
                    ChatRequest(message = q, history = historyList)
                )

                isTyping=false
                messages.add(Message(text=result.response, isUser = false))
                db.saveMessage(userEmail,result.response,false)
            } catch (e: Exception){
                isTyping=false
                messages.add(Message(text="معذرت، جواب نہیں آ سکا۔",isUser = false))
                Toast.makeText(context,"Error:8B Model disconnected",Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "UrduAI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.List, contentDescription = "History", tint = Color.White)
                    }
                },
                actions = {
                    // New Chat
                    IconButton(onClick = {
                        messages.clear()
                        messages.add(Message(text = "السلام علیکم! میں اردو اے آئی ہوں۔ نیا سوال پوچھیں۔", isUser = false))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
                    }

                    // 🚀 Profile/Settings Icon (Right side)
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Person, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
        containerColor = LightBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LightBackground)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { msg -> MessageBubble(msg) }
                if (isTyping) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "لکھ رہا ہے...",
                                color = Color.Gray,
                                fontFamily = UrduFont,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            if (messages.size <= 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { sendMessage("ایک مضمون لکھیں") },
                        label = { Text("مضمون لکھیں", fontFamily = UrduFont, fontSize = 14.sp, color = GreenPrimary) }
                    )
                    SuggestionChip(
                        onClick = { sendMessage("ایک کہانی سنائیں") },
                        label = { Text("کہانی سنائیں", fontFamily = UrduFont, fontSize = 14.sp, color = GreenPrimary) }
                    )
                    SuggestionChip(
                        onClick = { sendMessage("سلام") },
                        label = { Text("سلام", fontFamily = UrduFont, fontSize = 14.sp, color = GreenPrimary) }
                    )
                }
            }

            // ========== INPUT BAR ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface)
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, GreenPrimary, RoundedCornerShape(24.dp))
                        .background(LightSurface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = inputText.value,
                        onValueChange = { inputText.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = UrduFont,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            color = TextDark,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Right
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                if (inputText.value.isEmpty()) {
                                    Text(
                                        text = "اپنا سوال لکھیں...",
                                        fontFamily = UrduFont,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                            } else {
                                launchSpeech()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = if (isListening) Color.Red else GreenPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(GreenPrimary, CircleShape)
                        .clickable { sendMessage(inputText.value) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ========== MESSAGE BUBBLE ==========
@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) GreenPrimary else LightBubbleAI
    val textColor = if (isUser) Color.White else TextDark
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            // 🚀 Yahan wrapContentWidth lagaya hai taaki box text ke hisaab se chota/bada ho
            modifier = Modifier.wrapContentWidth().widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 16.sp,
                    fontFamily = UrduFont,
                    textAlign = TextAlign.Right,
                    style = TextStyle(textDirection = TextDirection.Rtl),
                    // 🚀 Yahan se fillMaxWidth() HATA dein taaki box full width na le
                    modifier = Modifier.wrapContentWidth()
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionButtonsRow(aiMessageText = message.text)
                }
            }
        }
    }
}

// ========== ACTION BUTTONS ==========
@Composable
fun ActionButtonsRow(aiMessageText: String) {
    val context = LocalContext.current
    var isThumbsUp by rememberSaveable { mutableStateOf(false) }
    var isThumbsDown by rememberSaveable { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val urduResult = tts?.setLanguage(Locale.forLanguageTag("ur-PK"))
                if (urduResult == TextToSpeech.LANG_MISSING_DATA || urduResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { isSpeaking = true }
                    override fun onDone(utteranceId: String?) { isSpeaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { isSpeaking = false }
                })
                isTtsReady = true
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (isTtsReady && tts != null) {
                if (isSpeaking) {
                    tts?.stop()
                    isSpeaking = false
                } else {
                    tts?.speak(aiMessageText, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
                }
            } else {
                Toast.makeText(context, "Awaaz tayar ho rahi hai...", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                contentDescription = "Speak",
                tint = if (isSpeaking) GreenPrimary else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", aiMessageText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Text Copied!", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, aiMessageText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = {
            isThumbsUp = !isThumbsUp
            if (isThumbsUp) {
                isThumbsDown = false
                Toast.makeText(context, "Thanks for feedback!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ThumbUp, contentDescription = "Thumbs Up", tint = if (isThumbsUp) GreenPrimary else Color.Gray, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = {
            isThumbsDown = !isThumbsDown
            if (isThumbsDown) {
                isThumbsUp = false
                Toast.makeText(context, "Feedback noted!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ThumbDown, contentDescription = "Thumbs Down", tint = if (isThumbsDown) Color.Red else Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

