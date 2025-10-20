package com.lingobot.studyassistant

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject

@Composable
fun StudyScreen(
    viewModel: BakingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var subject by remember { mutableStateOf("") }
    var conversationParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPartIndex by remember { mutableStateOf(0) }

    // Parsing da resposta feito apenas uma vez quando uiState for Success
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val output = (uiState as UiState.Success).outputText
            try {
                val cleanOutput = output.replace("```json", "").replace("```", "").trim()
                val jsonObject = JSONObject(cleanOutput)

                val parts = mutableListOf<String>()
                var i = 1
                while (jsonObject.has("response$i")) {
                    parts.add(jsonObject.getString("response$i"))
                    i++
                }
                conversationParts = parts
                currentPartIndex = 0
            } catch (e: Exception) {
                conversationParts = listOf("Erro ao processar a resposta da IA.")
                currentPartIndex = 0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Assistente de Estudos - Explicações Naturais",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Digite um assunto (ex: Revolução Francesa)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                val prompt = """
                    Explique o seguinte assunto de forma natural, como se fosse uma conversa informal entre amigos.
                    Use linguagem simples e interessante. Divida a explicação em partes, como se fossem mensagens em um chat.
                    Retorne a resposta no seguinte formato JSON:
                    
                    {
                      "response1": "Parte 1 da explicação...",
                      "response2": "Parte 2 da explicação...",
                      ...
                    }

                    Assunto: $subject
                """.trimIndent()

                viewModel.sendPrompt(prompt)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Explicar assunto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is UiState.Error -> {
                Text(
                    text = "Erro: ${(uiState as UiState.Error).errorMessage}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }

        if (conversationParts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Parte ${currentPartIndex + 1}/${conversationParts.size}:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = conversationParts[currentPartIndex],
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Justify,
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
    }
}




@Preview(showSystemUi = true)
@Composable
fun StudyScreenPreview() {
    StudyScreen()
}
