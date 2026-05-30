package com.example.whispry.presentation.settings.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.whispry.service.TrainedModelMatcher
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.util.liquid.components.LiquidButton
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTrainingBottomSheet(
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit,
    trainedModelMatcher: TrainedModelMatcher,
    backdrop: Backdrop
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var selectedPhrase by remember { mutableStateOf("Hey Whispry") }
    val samples = remember { mutableStateListOf<FloatArray>() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.9f)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ChoosePhraseScreen(
                        selectedPhrase = selectedPhrase,
                        onPhraseSelected = { selectedPhrase = it },
                        onNext = { scope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    1 -> RecordSamplesScreen(
                        phrase = selectedPhrase,
                        onSamplesComplete = { recordedSamples ->
                            samples.clear()
                            samples.addAll(recordedSamples)
                            trainedModelMatcher.train(recordedSamples)
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )
                    2 -> TrainingCompleteScreen(
                        onDone = {
                            val fp = trainedModelMatcher.getFingerprintString()
                            if (fp != null) onComplete(fp)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChoosePhraseScreen(
    selectedPhrase: String,
    onPhraseSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Choose your wake phrase", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Pick a unique phrase you'll say to start recording.", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val phrases = listOf("Hey Whispry", "Ok computer", "Start recording")
        phrases.forEach { phrase ->
            PhraseChip(
                phrase = phrase,
                isSelected = selectedPhrase == phrase,
                onClick = { onPhraseSelected(phrase) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WhispryTheme.colors.accent)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PhraseChip(phrase: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) WhispryTheme.colors.accent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (isSelected) WhispryTheme.colors.accent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(phrase, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RecordSamplesScreen(
    phrase: String,
    onSamplesComplete: (List<FloatArray>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentSampleIdx by remember { mutableIntStateOf(0) }
    val recordedSamples = remember { mutableStateListOf<FloatArray>() }
    var isRecording by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) { /* Handle error */ }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sample ${currentSampleIdx + 1} of 3", style = MaterialTheme.typography.labelLarge, color = WhispryTheme.colors.accent)
        Spacer(modifier = Modifier.height(16.dp))
        Text(phrase, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            // Visualizer would go here
            if (isRecording) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(160.dp), color = WhispryTheme.colors.accent, strokeWidth = 4.dp)
            }
            
            IconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        isRecording = true
                        scope.launch {
                            val sample = recordSample()
                            recordedSamples.add(sample)
                            isRecording = false
                            if (currentSampleIdx < 2) {
                                currentSampleIdx++
                            } else {
                                onSamplesComplete(recordedSamples)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(WhispryTheme.colors.accent, WhispryTheme.colors.accent.copy(alpha = 0.7f)))),
                enabled = !isRecording
            ) {
                Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            if (isRecording) "Say it clearly..." else "Tap the mic and say the phrase",
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private suspend fun recordSample(): FloatArray = withContext(Dispatchers.IO) {
    val sampleRate = 16000
    val minBufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
    val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, minBufSize)
    
    val maxSamples = sampleRate * 3
    val buffer = FloatArray(maxSamples)
    var samplesRead = 0
    var silentFrames = 0
    
    audioRecord.startRecording()
    
    while (samplesRead < maxSamples) {
        val chunk = FloatArray(512)
        val read = audioRecord.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
        if (read > 0) {
            chunk.copyInto(buffer, samplesRead, 0, read)
            samplesRead += read
            
            val rms = sqrt(chunk.sumOf { (it * it).toDouble() } / read).toFloat()
            if (rms < 0.01f) silentFrames++ else silentFrames = 0
            if (silentFrames > 12 && samplesRead > 8000) break
        }
    }
    
    audioRecord.stop()
    audioRecord.release()
    buffer.copyOf(samplesRead)
}

@Composable
fun TrainingCompleteScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF00E676)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(60.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Your voice is registered", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Whispry will now wake up when it hears you.", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WhispryTheme.colors.accent)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}
