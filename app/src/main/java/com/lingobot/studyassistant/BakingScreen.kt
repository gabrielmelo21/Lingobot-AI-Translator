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
import kotlinx.coroutines.delay

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
    var showAiResponse by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${R.raw.main_video_v3}")))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.button_sound) }
    val mediaPlayerArrow = remember { MediaPlayer.create(context, R.raw.button_sound2) }

    // Gerenciar transições de vídeo de forma mais eficiente
    LaunchedEffect(isRequestSent) {
        if (isRequestSent) {
            exoPlayer.pause()
            exoPlayer.seekTo(19800)
            // Aguarda o seek completar antes de tocar
            kotlinx.coroutines.delay(100)
            exoPlayer.play()
        } else {
            exoPlayer.pause()
            showAiResponse = false
            kotlinx.coroutines.delay(100)
            exoPlayer.play()
        }
    }

    // Usar listener do player ao invés de polling
    DisposableEffect(exoPlayer, isRequestSent) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    currentPosition = exoPlayer.currentPosition
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = exoPlayer.currentPosition
            }
        }

        exoPlayer.addListener(listener)

        // Corrotina para controlar pausas em pontos específicos
        val job = scope.launch {
            while (true) {
                currentPosition = exoPlayer.currentPosition

                if (isRequestSent) {
                    // Mostrar resposta AI aos 27 segundos
                    if (currentPosition >= 27000 && !showAiResponse) {
                        showAiResponse = true
                    }
                    // Pausar aos 31 segundos
                    if (currentPosition >= 31000 && exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    }
                } else {
                    // Pausar aos 19.8 segundos
                    if (currentPosition >= 19800 && exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    }
                }

                kotlinx.coroutines.delay(50)
            }
        }

        onDispose {
            job.cancel()
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            mediaPlayer.release()
            mediaPlayerArrow.release()
        }
    }

    // Parsing da resposta
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val output = (uiState as UiState.Success).outputText
            try {
                val cleanOutput = output.replace("```json", "").replace("```", "").trim()
                val jsonObject = JSONObject(cleanOutput)

                val parts = mutableListOf<String>()
                parts.add(jsonObject.getString("meaning"))
                parts.add(jsonObject.getString("usage"))
                parts.add(jsonObject.getString("notes"))
                parts.add(jsonObject.getString("example_1"))
                parts.add(jsonObject.getString("example_2"))
                conversationParts = parts
                currentPartIndex = 0
            } catch (e: Exception) {
                conversationParts = listOf("Erro ao processar a resposta da IA.")
                currentPartIndex = 0
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (conversationParts.isNotEmpty() && showAiResponse) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-80).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (currentPartIndex) {
                            0 -> "Meaning"
                            1 -> "Usage"
                            2 -> "Notes"
                            3 -> "Example 1"
                            else -> "Example 2"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = Color(0xFFFFFFFF),
                    )

                    Text(
                        text = conversationParts[currentPartIndex],
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Left Arrow
                Image(
                    painter = painterResource(id = R.drawable.left_arrow),
                    contentDescription = "Previous",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable(enabled = currentPartIndex > 0) {
                            mediaPlayerArrow.start()
                            currentPartIndex--
                        }
                        .alpha(if (currentPartIndex > 0) 1f else 0.5f)
                )

                // Right Arrow
                Image(
                    painter = painterResource(id = R.drawable.right_arrow),
                    contentDescription = "Next",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable(enabled = currentPartIndex < conversationParts.lastIndex) {
                            mediaPlayerArrow.start()
                            currentPartIndex++
                        }
                        .alpha(if (currentPartIndex < conversationParts.lastIndex) 1f else 0.5f)
                )

                // New Chat Button
                if (currentPartIndex == conversationParts.lastIndex) {
                    Image(
                        painter = painterResource(id = R.drawable.new_chat),
                        contentDescription = "New Chat",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .size(64.dp)
                            .clickable {
                                mediaPlayer.start()
                                // Resetar estados diretamente
                                showAiResponse = false
                                currentPartIndex = 0
                                conversationParts = emptyList()
                                subject = ""
                                isRequestSent = false
                                viewModel.sendPrompt("")
                                // Manter isRequestSent = true para manter na posição 19.8s
                                scope.launch {
                                    exoPlayer.pause()
                                    exoPlayer.seekTo(19800)
                                }
                            }
                    )
                }
            }

            // Skip Button
            if (!isRequestSent && currentPosition < 19000) {
                Image(
                    painter = painterResource(id = R.drawable.skip_button),
                    contentDescription = "Skip Intro",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(84.dp)
                        .clickable {
                            mediaPlayerArrow.start()
                            scope.launch {
                                exoPlayer.pause()
                                exoPlayer.seekTo(19800)
                                kotlinx.coroutines.delay(100)
                                exoPlayer.play()
                            }
                        }
                )
            }

            // Input area
            if (!isRequestSent) {
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
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black,
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
                            .alpha(if (isSendButtonEnabled) 1f else 0.5f)
                            .clickable(enabled = isSendButtonEnabled) {
                                mediaPlayer.start()
                                val prompt = """
Você é um assistente de idiomas que traduz e explica expressões em inglês de forma clara e curta.
O usuário pode pedir tradução, significado ou uso de uma palavra ou expressão.
Responda sempre em português.

Cada campo deve ter no máximo 200 caracteres.

Retorne exatamente neste formato JSON:
{
  "meaning": "Tradução direta ou explicação curta do termo",
  "usage": "Explique o uso, contexto ou sentido figurado",
  "notes": "Observações gramaticais, curiosidades ou diferenças culturais",
  "example_1": "Frase em inglês. (Tradução em português.)",
  "example_2": "Outra frase em inglês. (Tradução em português.)"
}

Pergunta: $subject
""".trimIndent()

                                viewModel.sendPrompt(prompt)
                                isRequestSent = true
                                subject = ""
                            }
                    )
                }
            }
        }
    }
}

