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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONObject

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

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/${R.raw.main_video}")))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(currentPartIndex) {
        when (currentPartIndex) {
            0 -> exoPlayer.seekTo(0)
            1 -> exoPlayer.seekTo(6000)
            2 -> exoPlayer.seekTo(11000)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            when (currentPartIndex) {
                0 -> if (exoPlayer.currentPosition >= 5000) exoPlayer.pause()
                1 -> if (exoPlayer.currentPosition >= 10000) exoPlayer.pause()
                2 -> if (exoPlayer.currentPosition >= 15000) exoPlayer.pause()
            }
            kotlinx.coroutines.delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
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
            if (conversationParts.isNotEmpty()) {
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
                Image(
                    painter = painterResource(id = R.drawable.send),
                    contentDescription = "Send",
                    modifier = Modifier
                        .height(56.dp)
                        .clickable {
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
                        }
                )
            }
        }
    }
    }
}




@Preview(showSystemUi = true)
@Composable
fun StudyScreenPreview() {
    StudyScreen()
}
