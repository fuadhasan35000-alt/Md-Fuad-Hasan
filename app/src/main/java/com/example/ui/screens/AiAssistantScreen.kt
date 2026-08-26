package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AmarDokanViewModel
import com.example.ui.theme.*
import com.example.util.AiAdvisorService

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val time: Long = System.currentTimeMillis()
)

@Composable
fun AiAssistantScreen(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit
) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "AI",
                text = "নমস্কার/সালাম! আমি আপনার দোকান **${currentBusiness?.name ?: "আমার দোকান"}**-এর এআই বিজনেস অ্যাডভাইজর।\n\nআপনি ব্যবসা বৃদ্ধি, বকেয়া দ্রুত আদায়, স্টক নিয়ন্ত্রণ বা লাভ বাড়ানোর কৌশল সম্পর্কে যেকোনো প্রশ্ন করতে পারেন।"
            )
        )
    }

    val quickQuestions = listOf(
        "কীভাবে বিক্রি বাড়াবো?",
        "বকেয়া আদায়ের কৌশল",
        "স্মার্ট স্টক প্ল্যানিং",
        "দোকানের লাভ বাড়ানোর উপায়"
    )

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে")
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "স্মার্ট এআই বিজনেস অ্যাসিস্ট্যান্ট",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Quick questions chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        items(quickQuestions) { q ->
                            SuggestionChip(
                                onClick = {
                                    messages.add(ChatMessage(sender = "USER", text = q))
                                    val answer = AiAdvisorService.answerBusinessQuery(q, currentBusiness?.name ?: "আমার দোকান")
                                    messages.add(ChatMessage(sender = "AI", text = answer))
                                },
                                label = { Text(q, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            placeholder = { Text("ব্যবসায়িক পরামর্শ জিজ্ঞাসা করুন...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_prompt_input"),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (inputPrompt.isNotBlank()) {
                                    val query = inputPrompt.trim()
                                    messages.add(ChatMessage(sender = "USER", text = query))
                                    inputPrompt = ""
                                    val reply = AiAdvisorService.answerBusinessQuery(query, currentBusiness?.name ?: "আমার দোকান")
                                    messages.add(ChatMessage(sender = "AI", text = reply))
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(24.dp))
                                .testTag("ai_send_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "পাঠান", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Live Insights Box
            if (aiInsights.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💡 আজকের লাইভ ইনসাইটস:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            aiInsights.forEach { insight ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(insight.titleBn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(insight.descriptionBn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chat Messages
            items(messages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
