package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.Presentation
import com.example.ui.ActiveScreen
import com.example.ui.PresentationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PresentationViewModel) {
    val presentations by viewModel.allPresentations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Recent, 1: This Device, 2: OneDrive

    val filteredPresentations = presentations.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFD24726), Color(0xFFF27D26))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Presento",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.requestImportJson?.invoke() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Import local presentation file",
                            tint = Color(0xFFD24726)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.createPresentationFromTemplate("Blank Presentation") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New blank presentation",
                            tint = Color(0xFFD24726)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.activeScreen.value = ActiveScreen.AboutDeveloper },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Developer & Company",
                            tint = Color(0xFFD24726)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.activeScreen.value = ActiveScreen.TemplateSelection },
                containerColor = Color(0xFFD24726),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new presentation from templates",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search presentations...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD24726),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Templates quick horizontal bar
            Text(
                text = "Start a New Presentation",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
            )

            val templates = listOf(
                TemplateItem("Blank Presentation", "Blank Slide", Color(0xFF64748B), Icons.Default.CropFree),
                TemplateItem("Startup Pitch Deck", "Dark Slate", Color(0xFF0F172A), Icons.Default.RocketLaunch),
                TemplateItem("Minimal Corporate", "Clean Slate", Color(0xFF1E3A8A), Icons.Default.Business),
                TemplateItem("Creative Portfolio", "Soft Rose", Color(0xFFDB2777), Icons.Default.Palette),
                TemplateItem("Science Lesson", "Mint Green", Color(0xFF047857), Icons.Default.School)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(115.dp)
            ) {
                items(templates) { template ->
                    Card(
                        onClick = { viewModel.createPresentationFromTemplate(template.name) },
                        modifier = Modifier
                            .width(135.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(template.accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = template.icon,
                                    contentDescription = template.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = template.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = template.sub,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs (Recent, This Device, OneDrive Cloud)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFFD24726),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Color(0xFFD24726)
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Outlined.History, contentDescription = "Recents") },
                    text = { Text("Recent", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Outlined.Computer, contentDescription = "Local Files") },
                    text = { Text("This Device", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Outlined.CloudQueue, contentDescription = "OneDrive") },
                    text = { Text("OneDrive", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Presentation list based on selected Tab
            val visiblePresentations = when (activeTab) {
                0 -> filteredPresentations // Recents
                1 -> filteredPresentations // Local Files
                2 -> filteredPresentations.map { it.copy(isCloudSynced = true) } // OneDrive (Virtual Cloud files!)
                else -> filteredPresentations
            }

            if (visiblePresentations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (activeTab) {
                                0 -> Icons.Default.HourglassEmpty
                                1 -> Icons.Default.FolderOpen
                                else -> Icons.Default.CloudOff
                            },
                            contentDescription = "Empty State",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = when (activeTab) {
                                0 -> "No recently opened files"
                                1 -> "No presentation files on this device"
                                else -> "Connect your OneDrive account to sync presentations"
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.createPresentationFromTemplate("Blank Presentation") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                        ) {
                            Text("Create New Slide")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visiblePresentations, key = { it.id }) { item ->
                        PresentationCard(
                            presentation = item,
                            onEdit = { viewModel.editPresentation(item) },
                            onDelete = { viewModel.deletePresentation(item.id) },
                            isCloudMode = activeTab == 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresentationCard(
    presentation: Presentation,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isCloudMode: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(presentation.lastModified))
    val firstSlideBg = presentation.slides.firstOrNull()?.background ?: "#FFFFFF"

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Presentation") },
            text = { Text("Are you sure you want to permanently delete '${presentation.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini Presentation slide icon color
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(firstSlideBg))
                        } catch (e: Exception) {
                            Color(0xFF64748B)
                        }
                    )
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                // Render a mini preview play indicator
                Icon(
                    imageVector = Icons.Default.Slideshow,
                    contentDescription = null,
                    tint = if (firstSlideBg.uppercase() == "#FFFFFF" || firstSlideBg.uppercase() == "#F8FAFC" || firstSlideBg.uppercase() == "#ECFDF5") Color.DarkGray else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = presentation.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCloudMode || presentation.isCloudSynced) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced to OneDrive",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${presentation.slides.size} slides • $formattedDate",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete presentation file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

data class TemplateItem(
    val name: String,
    val sub: String,
    val accentColor: Color,
    val icon: ImageVector
)

