package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveScreen
import com.example.ui.PresentationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(viewModel: PresentationViewModel) {
    val templates = listOf(
        LargeTemplate("Blank Presentation", "Perfect for starting fresh with empty canvas layout", Color(0xFF64748B), "#FFFFFF"),
        LargeTemplate("Startup Pitch Deck", "Elegant dark theme with navy/slate backgrounds & orange-gold accents", Color(0xFF0F172A), "#0F172A"),
        LargeTemplate("Minimal Corporate", "Clean professional style with navy blue headers & corporate table", Color(0xFF1E3A8A), "#F8FAFC"),
        LargeTemplate("Creative Portfolio", "Charming pink-rose editorial theme with picture canvas", Color(0xFFDB2777), "#FDF2F8"),
        LargeTemplate("Science Lesson", "Mint green educational theme featuring solar structure diagrams", Color(0xFF047857), "#ECFDF5")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Presentation") },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.activeScreen.value = ActiveScreen.Home },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.createPresentationFromTemplate("Blank Presentation") },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "Blank")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Blank PPT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.requestImportJson?.invoke() },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD24726))
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = "Open file")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Text(
                text = "Choose a Professional Template",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Start
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(templates) { template ->
                    Card(
                        onClick = { viewModel.createPresentationFromTemplate(template.title) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Mini canvas preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(template.previewBgColor))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Slideshow,
                                    contentDescription = null,
                                    tint = if (template.previewBgColor.uppercase() == "#FFFFFF" || template.previewBgColor.uppercase() == "#F8FAFC" || template.previewBgColor.uppercase() == "#ECFDF5") Color.Gray else Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = template.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LargeTemplate(
    val title: String,
    val description: String,
    val accentColor: Color,
    val previewBgColor: String
)
