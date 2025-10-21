package com.lingobot.studyassistant

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.MediaPlayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONObject
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
fun VideoPlayer(exoPlayer: ExoPlayer) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun StudyScreen(
    viewModel: BakingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var subject by remember { mutableStateOf("") }
    var conversationParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPartIndex by remember { mutableStateOf(0) }
    var isRequestSent by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var isPlayingResponseVideo by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${R.raw.main_video}")))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.button_sound) }



    LaunchedEffect(exoPlayer, isRequestSent, isPlayingResponseVideo) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            if (isPlayingResponseVideo) { // If playing response video segment
                if (exoPlayer.currentPosition >= 31000) {
                    exoPlayer.pause()
                    isPlayingResponseVideo = false // Reset after pausing
                }
            } else if (!isRequestSent) { // If no request is sent (intro segment)
                if (exoPlayer.currentPosition >= 19800) {
                    exoPlayer.pause()
                }
            }
            // If isRequestSent is true, and not playing response video, then video should be paused at 19.8s.
            kotlinx.coroutines.delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            mediaPlayer.release() // Release the media player
        }
    }

    // Parsing da resposta feito apenas uma vez quando uiState for Success
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val output = (uiState as UiState.Success).outputText
            try {
                val cleanOutput = output.replace("```json", "").replace("```", "").trim()
                val jsonObject = JSONObject(cleanOutput)

                val parts = mutableListOf<String>()
                parts.add(jsonObject.getString("direct_answer"))
                parts.add(jsonObject.getString("example_1"))
                parts.add(jsonObject.getString("example_2"))
                conversationParts = parts
                currentPartIndex = 0
                isRequestSent = false // Reset isRequestSent
                isPlayingResponseVideo = true // Start playing response video segment
                exoPlayer.seekTo(19800) // Seek to 19.8s
                exoPlayer.play() // Start playing
            } catch (e: Exception) {
                conversationParts = listOf("Erro ao processar a resposta da IA.")
                currentPartIndex = 0
                isRequestSent = false // Reset isRequestSent even on error
                isPlayingResponseVideo = true // Start playing response video segment
                exoPlayer.seekTo(19800) // Seek to 19.8s
                exoPlayer.play() // Start playing
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.poster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        VideoPlayer(exoPlayer = exoPlayer)

        Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.poster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        VideoPlayer(exoPlayer = exoPlayer)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (conversationParts.isNotEmpty() && currentPosition >= 31000) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (currentPartIndex) {
                            0 -> "Your Answer"
                            1 -> "Example 1"
                            else -> "Example 2"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = conversationParts[currentPartIndex],
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = { currentPartIndex++ },
                        enabled = currentPartIndex < conversationParts.lastIndex
                    ) {
                        Text(if (currentPartIndex < conversationParts.lastIndex) "Próximo" else "Fim")
                    }
                }
            }

            // This is the input area
            if (!isRequestSent) { // Show input field if no request sent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Digite sua dúvida de inglês") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .padding(end = 8.dp)
                    )
                    val isSendButtonEnabled = subject.isNotBlank()
                    Image(
                        painter = painterResource(id = R.drawable.send),
                        contentDescription = "Send",
                        modifier = Modifier
                            .height(56.dp)
                            .alpha(if (isSendButtonEnabled) 1f else 0.5f) // Change alpha for visual feedback
                            .clickable(enabled = isSendButtonEnabled) { // Control clickability
                                mediaPlayer.start() // Play the sound
                                val prompt = """
                                                        You are an AI language learning assistant. Your goal is to explain things in English in a clear and concise way.
                                                        The user will ask a question, and you should provide a direct answer and two examples of use.
                                                        Return the response in the following JSON format:
                            
                                                        {
                                                          \"direct_answer\": \"The direct answer to the user's question.\",
                                                          \"example_1\": \"An example of how to use the answer in a sentence.\",
                                                          \"example_2\": \"Another example of how to use the answer in a sentence.\"
                                                        }
                            
                                                        Question: $subject
                                                    """.trimIndent()
                                viewModel.sendPrompt(prompt)
                                isRequestSent = true // Set to true after sending request
                                subject = "" // Clear the input field
                            }
                        )
                    }
                }
            }
        }
    }
}




