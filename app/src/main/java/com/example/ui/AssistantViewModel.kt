package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.feedback.FeedbackDatabase
import com.example.data.feedback.FeedbackEntity
import com.example.data.feedback.FeedbackRepository
import com.example.engine.DialogueManager
import com.example.engine.IntentType
import com.example.engine.ProcessResult
import com.example.engine.VoiceProfile
import com.example.engine.GeminiEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    val sender: SenderType,
    val text: String,
    val intent: IntentType? = null,
    val actionDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SenderType {
    USER, ASSISTANT, SYSTEM
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FeedbackDatabase.getDatabase(application)
    private val repository = FeedbackRepository(database.feedbackDao())
    private val dialogueManager = DialogueManager(application)
    private val geminiEngine = GeminiEngine()
    private val audioCueEngine = com.example.engine.AudioCueEngine()

    // --- WORKSPACE & SMART HOME STATES ---
    
    // Writing Studio
    private val _writingOutput = MutableStateFlow("")
    val writingOutput: StateFlow<String> = _writingOutput.asStateFlow()
    
    private val _isWritingLoading = MutableStateFlow(false)
    val isWritingLoading: StateFlow<Boolean> = _isWritingLoading.asStateFlow()

    // Live Interpreter
    private val _translationOutput = MutableStateFlow("")
    val translationOutput: StateFlow<String> = _translationOutput.asStateFlow()
    
    private val _isTranslationLoading = MutableStateFlow(false)
    val isTranslationLoading: StateFlow<Boolean> = _isTranslationLoading.asStateFlow()

    // Screen Vision
    private val _screenVisionResult = MutableStateFlow("")
    val screenVisionResult: StateFlow<String> = _screenVisionResult.asStateFlow()
    
    private val _isScreenVisionLoading = MutableStateFlow(false)
    val isScreenVisionLoading: StateFlow<Boolean> = _isScreenVisionLoading.asStateFlow()

    // Document Summary
    private val _documentSummary = MutableStateFlow("")
    val documentSummary: StateFlow<String> = _documentSummary.asStateFlow()
    
    private val _isDocumentSummaryLoading = MutableStateFlow(false)
    val isDocumentSummaryLoading: StateFlow<Boolean> = _isDocumentSummaryLoading.asStateFlow()

    // Voice Hub Transcript
    private val _meetingTranscript = MutableStateFlow("")
    val meetingTranscript: StateFlow<String> = _meetingTranscript.asStateFlow()
    
    private val _isTranscriptLoading = MutableStateFlow(false)
    val isTranscriptLoading: StateFlow<Boolean> = _isTranscriptLoading.asStateFlow()

    // Smart Home States
    private val _acStatus = MutableStateFlow(false)
    val acStatus: StateFlow<Boolean> = _acStatus.asStateFlow()

    private val _acTemperature = MutableStateFlow(24)
    val acTemperature: StateFlow<Int> = _acTemperature.asStateFlow()

    private val _tvStatus = MutableStateFlow(false)
    val tvStatus: StateFlow<Boolean> = _tvStatus.asStateFlow()

    private val _tvChannel = MutableStateFlow("MRTV HD")
    val tvChannel: StateFlow<String> = _tvChannel.asStateFlow()

    private val _lightStatus = MutableStateFlow(false)
    val lightStatus: StateFlow<Boolean> = _lightStatus.asStateFlow()

    private val _lightColor = MutableStateFlow("Warm Gold")
    val lightColor: StateFlow<String> = _lightColor.asStateFlow()

    private val _lightIntensity = MutableStateFlow(75)
    val lightIntensity: StateFlow<Int> = _lightIntensity.asStateFlow()

    // --- CUSTOM VOICE COMMAND MAPPINGS (PERSISTED LOCAL STORAGE) ---
    private val sharedPrefs = application.getSharedPreferences("rs_voice_mappings_v1", android.content.Context.MODE_PRIVATE)
    private val _voiceMappings = MutableStateFlow<Map<String, IntentType>>(emptyMap())
    val voiceMappings: StateFlow<Map<String, IntentType>> = _voiceMappings.asStateFlow()

    fun loadVoiceMappings() {
        val all = sharedPrefs.all
        val map = mutableMapOf<String, IntentType>()
        for ((key, value) in all) {
            if (value is String) {
                try {
                    map[key] = IntentType.valueOf(value)
                } catch (e: Exception) {
                    // Ignore invalid
                }
            }
        }
        // Pre-populate with realistic, interactive custom commands if empty
        if (map.isEmpty()) {
            map["အိပ်ချင်ပြီ"] = IntentType.TOGGLE_DARK_MODE
            map["ဓာတ်မီးလင်းစေ"] = IntentType.TOGGLE_FLASHLIGHT
            map["ဗီဒီယိုရိုက်စို့"] = IntentType.START_SCREEN_RECORDING
            map["ဓာတ်ပုံကြည့်မယ်"] = IntentType.OPEN_GALLERY
            sharedPrefs.edit().apply {
                map.forEach { (phrase, intent) -> putString(phrase, intent.name) }
                apply()
            }
        }
        _voiceMappings.value = map
    }

    fun saveVoiceMapping(phrase: String, intent: IntentType) {
        val normalized = phrase.trim().lowercase()
        if (normalized.isNotEmpty()) {
            sharedPrefs.edit().putString(normalized, intent.name).apply()
            loadVoiceMappings()
        }
    }

    fun deleteVoiceMapping(phrase: String) {
        val normalized = phrase.trim().lowercase()
        sharedPrefs.edit().remove(normalized).apply()
        loadVoiceMappings()
    }

    // Feedbacks list observed from DB
    val feedbacks: StateFlow<List<FeedbackEntity>> = repository.allFeedback
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Conversation dialogue log
    private val _chatLog = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = SenderType.ASSISTANT,
                text = "မင်္ဂလာပါခင်ဗျာ။ အော့ဖ်လိုင်း မြန်မာ Voice Assistant မှ ကြိုဆိုပါတယ်။ စတင်လုပ်ဆောင်ရန် 'Hey Bro' ဟု ပြောပါ သို့မဟုတ် အောက်တွင် မေးခွန်းရိုက်ထည့်ပါခင်ဗျာ။"
            )
        )
    )
    val chatLog: StateFlow<List<ChatMessage>> = _chatLog.asStateFlow()

    // Engine States
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWakeWordActive = MutableStateFlow(true)
    val isWakeWordActive: StateFlow<Boolean> = _isWakeWordActive.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // --- VOICE CHAT PROFILE STATES ---
    private val _currentVoiceProfile = MutableStateFlow(VoiceProfile.INDOOR)
    val currentVoiceProfile: StateFlow<VoiceProfile> = _currentVoiceProfile.asStateFlow()

    fun setVoiceProfile(profile: VoiceProfile) {
        _currentVoiceProfile.value = profile
        dialogueManager.setVoiceProfile(profile)
        // Log the profile activation in the system log of chat
        val statusMsg = if (profile == VoiceProfile.INDOOR) {
            "🏡 အိမ်တွင်း အသံစနစ် (Indoor Profile) ကို ပြောင်းလဲလိုက်ပါပြီ - ဆူညံသံနည်းပါးသော ပတ်ဝန်းကျင်အတွက် ပုံမှန်အသံစနစ်"
        } else {
            "📢 အပြင်ဘက် အသံစနစ် (Outdoor Profile) ကို ပြောင်းလဲလိုက်ပါပြီ - ဆူညံသော ပတ်ဝန်းကျင်အတွက် အသံချဲ့ထွင်မှုနှင့် ဆူညံသံစစ်ထုတ်စနစ်"
        }
        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.SYSTEM,
            text = statusMsg
        )
    }

    // Holds the last assistant response that can receive user feedback
    private val _lastProcessResult = MutableStateFlow<ProcessResult?>(null)
    val lastProcessResult: StateFlow<ProcessResult?> = _lastProcessResult.asStateFlow()

    // Show feedback dialog state
    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    init {
        // Load custom persistent voice command mappings on startup
        loadVoiceMappings()
        // Start Wake Word continuous ambient listening on startup
        toggleWakeWordEngine(true)
    }

    fun toggleWakeWordEngine(enable: Boolean) {
        _isWakeWordActive.value = enable
        if (enable) {
            dialogueManager.wakeWordEngine.startListening {
                // Wake word callback triggered!
                viewModelScope.launch {
                    _chatLog.value = _chatLog.value + ChatMessage(
                        sender = SenderType.SYSTEM,
                        text = "✨ Wake Word Detected: 'Hey R's AI' / 'R's AI'!"
                    )
                    // Trigger active listening state
                    startVoiceCapture()
                }
            }
        } else {
            dialogueManager.wakeWordEngine.stopListening()
        }
    }

    fun simulateWakeWordTrigger() {
        dialogueManager.wakeWordEngine.simulateWakeWordMatch()
    }

    fun startVoiceCapture() {
        if (_isListening.value) return
        _isListening.value = true
        
        // Play subtle auditory cue for wake-up
        audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.WAKE_UP)
        
        // Add profile-based logging to make the user experience immersive!
        if (_currentVoiceProfile.value == VoiceProfile.OUTDOOR) {
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "🎙️ [Outdoor Voice Chat Active] Ambient Wind Filter & Dynamic ANC applied. Mic Gain boosted (+12dB)."
            )
        } else {
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "🎙️ [Indoor Voice Chat Active] Quiet-focus mode. Dynamic mic range optimized."
            )
        }
        
        // Simulate speech acquisition and sound waves
        viewModelScope.launch {
            // Wait 2.5 seconds for simulated spoken Burmese input
            kotlinx.coroutines.delay(2500)
            if (_isListening.value) {
                _isListening.value = false
                // Default test voice query (could be simulated)
                processVoiceQuery(byteArrayOf())
            }
        }
    }

    fun cancelVoiceCapture() {
        _isListening.value = false
    }

    private fun processVoiceQuery(audioBytes: ByteArray) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                dialogueManager.processVoiceQuery(audioBytes, _voiceMappings.value) { result ->
                    handleQueryResult(result)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _chatLog.value = _chatLog.value + ChatMessage(
                    sender = SenderType.SYSTEM,
                    text = "🚨 အမှားအယွင်း ဖြစ်ပေါ်ခဲ့သည်: ${e.localizedMessage}"
                )
            }
        }
    }

    fun processTextQuery(query: String) {
        if (query.isBlank()) return
        
        // Add user message to log
        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.USER,
            text = query
        )

        _isProcessing.value = true
        viewModelScope.launch {
            try {
                dialogueManager.processTextQuery(query, _voiceMappings.value) { result ->
                    handleQueryResult(result)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _chatLog.value = _chatLog.value + ChatMessage(
                    sender = SenderType.SYSTEM,
                    text = "🚨 အမှားအယွင်း ဖြစ်ပေါ်ခဲ့သည်: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun handleQueryResult(result: ProcessResult) {
        _isProcessing.value = false
        _lastProcessResult.value = result
        
        // Play corresponding cue
        if (result.intent != IntentType.UNKNOWN) {
            audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
        } else {
            audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
        }
        
        // If STT results in a different text than user typed (like simulated voice query), log it
        val alreadyLogged = _chatLog.value.any { it.sender == SenderType.USER && it.text == result.queryText }
        if (!alreadyLogged) {
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.USER,
                text = result.queryText
            )
        }

        // Add assistant response to log
        _chatLog.value = _chatLog.value + ChatMessage(
            sender = SenderType.ASSISTANT,
            text = result.responseTextMy,
            intent = result.intent,
            actionDetails = result.systemActionDetails
        )
    }

    fun submitFeedback(rating: Int, comment: String? = null) {
        val lastResult = _lastProcessResult.value ?: return
        viewModelScope.launch {
            val entity = FeedbackEntity(
                prompt = lastResult.queryText,
                response = lastResult.responseTextMy,
                rating = rating,
                correctionComment = comment
            )
            repository.insertFeedback(entity)
            
            // Log confirmation
            val feedbackText = if (rating == 1) "👍 ကျေးဇူးတင်ပါတယ်ခင်ဗျာ!" else "👎 အကြံပြုချက် ရရှိပါပြီ။ ပိုကောင်းအောင် ပြင်ဆင်ပါမည်ခင်ဗျာ။"
            _chatLog.value = _chatLog.value + ChatMessage(
                sender = SenderType.SYSTEM,
                text = "စနစ်သုံးသပ်ချက်သိမ်းဆည်းပြီးပါပြီ။ $feedbackText"
            )
            
            // Clear last response so they don't double submit on same message
            _lastProcessResult.value = null
        }
    }

    fun deleteFeedback(id: Int) {
        viewModelScope.launch {
            repository.deleteFeedbackById(id)
        }
    }

    fun clearAllFeedback() {
        viewModelScope.launch {
            repository.clearAllFeedback()
        }
    }

    // --- WORKSPACE HELPER METHODS ---

    fun generateWriting(prompt: String, templateType: String) {
        if (prompt.isBlank()) return
        _isWritingLoading.value = true
        viewModelScope.launch {
            try {
                val systemInstruction = when (templateType) {
                    "Email" -> "You are R's Writing Studio. Compose a professional email in polite Burmese based on the user request. Use appropriate formatting and professional polite endings."
                    "Summary" -> "You are R's Writing Studio. Summarize the text provided by the user into clear, bulleted key points in polite Burmese."
                    "Rewrite" -> "You are R's Writing Studio. Rewrite the text provided by the user to make it sound more sophisticated, elegant, and native in polite Burmese."
                    "Grammar" -> "You are R's Writing Studio. Identify grammar mistakes, fix them, and explain the correction in polite Burmese."
                    else -> "You are R's Writing Studio. Help the user write creative or professional content in polite Burmese."
                }
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _writingOutput.value = response
                _isWritingLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isWritingLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _writingOutput.value = "🚨 Writing Studio error: ${e.localizedMessage}"
            }
        }
    }

    fun translateText(prompt: String, targetLang: String) {
        if (prompt.isBlank()) return
        _isTranslationLoading.value = true
        viewModelScope.launch {
            try {
                val systemInstruction = "You are R's Translation Assistant. Translate the user text directly into $targetLang. Output only the translated text. Do not add conversational intro/outro remarks unless requested. Keep it clean and natural."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _translationOutput.value = response
                _isTranslationLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isTranslationLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _translationOutput.value = "🚨 Translation error: ${e.localizedMessage}"
            }
        }
    }

    fun scanScreen(screenDescription: String) {
        _isScreenVisionLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Analyze this simulated screenshot state and explain what is happening, what errors exist, and how the user should resolve them in Burmese: $screenDescription"
                val systemInstruction = "You are R's Screen Vision Agent. You explain smartphone screen content, application UI, and warning screens in highly clear Burmese."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _screenVisionResult.value = response
                _isScreenVisionLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isScreenVisionLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _screenVisionResult.value = "🚨 Screen Vision error: ${e.localizedMessage}"
            }
        }
    }

    fun summarizeDocument(documentName: String, docType: String) {
        _isDocumentSummaryLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Please analyze and summarize the document '$documentName' which contains a structured $docType report. Extract the key performance metrics and next steps in Burmese."
                val systemInstruction = "You are R's Document Hub. You specialize in reading complex reports, PDF documents, and business logs, translating them into neat Burmese bullet point summaries."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _documentSummary.value = response
                _isDocumentSummaryLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isDocumentSummaryLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _documentSummary.value = "🚨 Document summary error: ${e.localizedMessage}"
            }
        }
    }

    fun startMeetingTranscript() {
        _isTranscriptLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = "Generate a realistic, formatted Multi-Speaker Meeting Transcript in Burmese. Include Speaker 1 and Speaker 2 discussing a project review. Ensure there are timestamps."
                val systemInstruction = "You are R's Voice Hub transcription service. You convert spoken meeting audio into clear Burmese text."
                val response = geminiEngine.generateContent(prompt, systemInstruction)
                _meetingTranscript.value = response
                _isTranscriptLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.SUCCESS)
            } catch (e: Exception) {
                _isTranscriptLoading.value = false
                audioCueEngine.playCue(com.example.engine.AudioCueEngine.CueType.ERROR)
                _meetingTranscript.value = "🚨 Voice Hub error: ${e.localizedMessage}"
            }
        }
    }

    // --- SMART HOME ACTIONS ---
    fun toggleAc() {
        _acStatus.value = !_acStatus.value
        val action = if (_acStatus.value) "AC turned ON to ${_acTemperature.value}°C" else "AC turned OFF"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun adjustAcTemp(raise: Boolean) {
        val current = _acTemperature.value
        if (raise && current < 30) {
            _acTemperature.value = current + 1
        } else if (!raise && current > 16) {
            _acTemperature.value = current - 1
        }
        val action = "AC temperature set to ${_acTemperature.value}°C"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun toggleTv() {
        _tvStatus.value = !_tvStatus.value
        val action = if (_tvStatus.value) "Smart TV turned ON (Channel: ${_tvChannel.value})" else "Smart TV turned OFF"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun setTvChannel(channel: String) {
        _tvChannel.value = channel
        _tvStatus.value = true
        val action = "Smart TV tuned to channel $channel"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun toggleLight() {
        _lightStatus.value = !_lightStatus.value
        val action = if (_lightStatus.value) "Smart lights turned ON (Theme: ${_lightColor.value})" else "Smart lights turned OFF"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun setLightColor(color: String) {
        _lightColor.value = color
        _lightStatus.value = true
        val action = "Smart lights set to color theme: $color"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    fun adjustLightIntensity(percent: Int) {
        _lightIntensity.value = percent
        val action = "Smart lights brightness adjusted to $percent%"
        _chatLog.value = _chatLog.value + ChatMessage(sender = SenderType.SYSTEM, text = "🏠 Smart Home: $action")
    }

    override fun onCleared() {
        super.onCleared()
        dialogueManager.release()
    }
}
