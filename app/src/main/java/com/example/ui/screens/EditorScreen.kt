package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.ActiveScreen
import com.example.ui.EditorTab
import com.example.ui.PresentationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: PresentationViewModel) {
    val context = LocalContext.current
    val presentation by viewModel.currentPresentation.collectAsState()
    val slideIndex by viewModel.currentSlideIndex.collectAsState()
    val selectedElemId by viewModel.selectedElementId.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isAutoSave by viewModel.isAutoSaveEnabled.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val slide = presentation?.slides?.getOrNull(slideIndex)

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    // Dialogs for editing elements
    var editingTextElement by remember { mutableStateOf<SlideElement.TextBox?>(null) }
    var editingShapeElement by remember { mutableStateOf<SlideElement.Shape?>(null) }
    var editingTableElement by remember { mutableStateOf<SlideElement.TableElement?>(null) }
    var editingChartElement by remember { mutableStateOf<SlideElement.ChartElement?>(null) }
    var editingVideoElement by remember { mutableStateOf<SlideElement.VideoElement?>(null) }

    if (presentation == null || slide == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFD24726))
        }
        return
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Presentation") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    placeholder = { Text("Presentation Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD24726))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotEmpty()) {
                            viewModel.renamePresentation(renameInput)
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Modal dialog trigger for TextBox Content
    editingTextElement?.let { textElem ->
        var textContentInput by remember { mutableStateOf(textElem.text) }
        AlertDialog(
            onDismissRequest = { editingTextElement = null },
            title = { Text("Edit TextBox Content") },
            text = {
                OutlinedTextField(
                    value = textContentInput,
                    onValueChange = { textContentInput = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD24726)),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTextBoxFormatting(textElem.id, text = textContentInput)
                        editingTextElement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTextElement = null }) { Text("Cancel") }
            }
        )
    }

    // Modal dialog trigger for Shape Content
    editingShapeElement?.let { shapeElem ->
        var labelInput by remember { mutableStateOf(shapeElem.text) }
        var fillColorInput by remember { mutableStateOf(shapeElem.fillColor) }
        AlertDialog(
            onDismissRequest = { editingShapeElement = null },
            title = { Text("Edit Shape Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Shape Label:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD24726))
                    )
                    Text("Fill Color (Hex):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fillColorInput,
                        onValueChange = { fillColorInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD24726))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateShapeFormatting(shapeElem.id, text = labelInput, fillColor = fillColorInput)
                        editingShapeElement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingShapeElement = null }) { Text("Cancel") }
            }
        )
    }

    // Modal dialog trigger for Table Content
    editingTableElement?.let { tableElem ->
        var tableDataState = remember { mutableStateListOf<MutableList<String>>().apply {
            addAll(tableElem.data.map { it.toMutableList() })
        }}
        AlertDialog(
            onDismissRequest = { editingTableElement = null },
            title = { Text("Edit Table Cells") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0 until tableElem.rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (c in 0 until tableElem.cols) {
                                OutlinedTextField(
                                    value = tableDataState.getOrNull(r)?.getOrNull(c) ?: "",
                                    onValueChange = { newVal: String ->
                                        tableDataState[r][c] = newVal
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTableFormatting(tableElem.id, data = tableDataState.toList())
                        editingTableElement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                ) {
                    Text("Save Table")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTableElement = null }) { Text("Cancel") }
            }
        )
    }

    // Modal dialog trigger for Chart Content
    editingChartElement?.let { chartElem ->
        var chartTitleInput by remember { mutableStateOf(chartElem.title) }
        var labelsInput by remember { mutableStateOf(chartElem.labels.joinToString(",")) }
        var valuesInput by remember { mutableStateOf(chartElem.values.joinToString(",")) }
        AlertDialog(
            onDismissRequest = { editingChartElement = null },
            title = { Text("Edit Chart Variables") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chartTitleInput,
                        onValueChange = { chartTitleInput = it },
                        label = { Text("Chart Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = labelsInput,
                        onValueChange = { labelsInput = it },
                        label = { Text("Labels (Comma separated)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = valuesInput,
                        onValueChange = { valuesInput = it },
                        label = { Text("Values (Comma separated numbers)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lbls = labelsInput.split(",").map { it.trim() }
                        val vls = valuesInput.split(",").mapNotNull { it.trim().toFloatOrNull() }
                        viewModel.updateChartFormatting(chartElem.id, title = chartTitleInput, labels = lbls, values = vls)
                        editingChartElement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingChartElement = null }) { Text("Cancel") }
            }
        )
    }

    // Modal dialog trigger for Video Content
    editingVideoElement?.let { videoElem ->
        var urlInput by remember { mutableStateOf(videoElem.videoUrl) }
        AlertDialog(
            onDismissRequest = { editingVideoElement = null },
            title = { Text("Edit Video URL") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Enter Video Link (YouTube or MP4)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD24726))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateVideoUrl(videoElem.id, urlInput)
                        editingVideoElement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726))
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVideoElement = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            renameInput = presentation?.title ?: ""
                            showRenameDialog = true
                        }
                    ) {
                        Text(
                            text = presentation?.title ?: "Presentation",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename PPT",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFD24726)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit Presentation Editor")
                    }
                },
                actions = {
                    // Sync Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (syncStatus == "Synced") Icons.Default.CloudQueue else Icons.Default.Sync,
                            contentDescription = "Sync Indicator",
                            tint = if (syncStatus == "Synced") Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = syncStatus,
                            fontSize = 11.sp,
                            color = if (syncStatus == "Synced") Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }

                    // Present / Slideshow (▶️)
                    IconButton(
                        onClick = { viewModel.activeScreen.value = ActiveScreen.Slideshow(slideIndex) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Present slideshow full screen",
                            tint = Color(0xFFD24726)
                        )
                    }

                    // Duplicate Slide Shortcut
                    IconButton(
                        onClick = { viewModel.duplicateSlide() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CopyAll,
                            contentDescription = "Duplicate current slide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                .background(Color(0xFFF1F5F9)) // subtle cool gray workspace background
        ) {
            // (A) Slide Area (Middle Canvas with percentage coordinates)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Aspect Ratio Border (16:9 standard slide ratio!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.777f) // 16:9
                        .shadow(6.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(slide.background))
                            } catch (e: Exception) {
                                Color.White
                            }
                        )
                        .pointerInput(Unit) {
                            // Tap outside to deselect
                            detectDragGestures { _, _ -> }
                        }
                        .clickable { viewModel.selectedElementId.value = null }
                ) {
                    // Draw elements relative to container dimensions!
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val containerW = maxWidth.value
                        val containerH = maxHeight.value

                        slide.elements.forEach { elem ->
                            val isSelected = elem.id == selectedElemId

                            // Compute local pixel placement
                            val pxX = (elem.x / 100f) * containerW
                            val pxY = (elem.y / 100f) * containerH
                            val pxW = (elem.width / 100f) * containerW
                            val pxH = (elem.height / 100f) * containerH

                            Box(
                                modifier = Modifier
                                    .offset(x = pxX.dp, y = pxY.dp)
                                    .size(width = pxW.dp, height = pxH.dp)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, Color(0xFF2563EB), RoundedCornerShape(4.dp))
                                        } else Modifier
                                    )
                                    .pointerInput(elem.id) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            // Compute change relative to percentage container bounds
                                            val pctDX = (dragAmount.x / (containerW * density)) * 100f
                                            val pctDY = (dragAmount.y / (containerH * density)) * 100f

                                            val newX = (elem.x + pctDX).coerceIn(0f, 100f - elem.width)
                                            val newY = (elem.y + pctDY).coerceIn(0f, 100f - elem.height)

                                            viewModel.updateElementBounds(elem.id, newX, newY, elem.width, elem.height)
                                        }
                                    }
                                    .clickable {
                                        viewModel.selectedElementId.value = elem.id
                                    }
                            ) {
                                // Render specific element
                                when (elem) {
                                    is SlideElement.TextBox -> {
                                        TextBoxRenderer(
                                            elem = elem,
                                            onDoubleTap = { editingTextElement = elem }
                                        )
                                    }
                                    is SlideElement.Shape -> {
                                        ShapeRenderer(
                                            elem = elem,
                                            onDoubleTap = { editingShapeElement = elem }
                                        )
                                    }
                                    is SlideElement.Picture -> {
                                        PictureRenderer(elem = elem)
                                    }
                                    is SlideElement.TableElement -> {
                                        TableRenderer(
                                            elem = elem,
                                            onDoubleTap = { editingTableElement = elem }
                                        )
                                    }
                                    is SlideElement.ChartElement -> {
                                        ChartRenderer(
                                            elem = elem,
                                            onDoubleTap = { editingChartElement = elem }
                                        )
                                    }
                                    is SlideElement.VideoElement -> {
                                        VideoRenderer(
                                            elem = elem,
                                            onDoubleTap = { editingVideoElement = elem }
                                        )
                                    }
                                }

                                // Delete Button shortcut overlay when selected
                                if (isSelected) {
                                    IconButton(
                                        onClick = { viewModel.deleteElement(elem.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-10).dp)
                                            .size(24.dp)
                                            .background(Color.Red, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete element",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Thumbnail Carousel Slide strip
            SlideThumbnailStrip(viewModel)

            // Dynamic Tabbed Property Editing Sheets
            BottomToolbarSheet(viewModel)
        }
    }
}

// RENDERERS FOR INDIVIDUAL ELEMENTS
@Composable
fun TextBoxRenderer(elem: SlideElement.TextBox, onDoubleTap: () -> Unit) {
    val align = when (elem.alignment) {
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        else -> TextAlign.Left
    }
    val fontStyle = if (elem.isItalic) FontStyle.Italic else FontStyle.Normal
    val fontWeight = if (elem.isBold) FontWeight.Bold else FontWeight.Normal
    val fontFamily = when (elem.fontFamily) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDoubleTap() }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = elem.text,
            fontSize = elem.fontSize.sp,
            color = try { Color(android.graphics.Color.parseColor(elem.color)) } catch (e: Exception) { Color.Black },
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ShapeRenderer(elem: SlideElement.Shape, onDoubleTap: () -> Unit) {
    val strokeColor = try { Color(android.graphics.Color.parseColor(elem.strokeColor)) } catch(e:Exception) { Color.DarkGray }
    val fillColor = try { Color(android.graphics.Color.parseColor(elem.fillColor)) } catch(e:Exception) { Color.LightGray }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDoubleTap() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (elem.shapeType) {
                ShapeType.CIRCLE -> {
                    drawCircle(color = fillColor)
                    drawCircle(color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.SQUARE -> {
                    drawRect(color = fillColor)
                    drawRect(color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.TRIANGLE -> {
                    val path = Path().apply {
                        moveTo(w / 2f, 0f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.ARROW_RIGHT -> {
                    val path = Path().apply {
                        moveTo(0f, h * 0.3f)
                        lineTo(w * 0.6f, h * 0.3f)
                        lineTo(w * 0.6f, 0f)
                        lineTo(w, h / 2f)
                        lineTo(w * 0.6f, h)
                        lineTo(w * 0.6f, h * 0.7f)
                        lineTo(0f, h * 0.7f)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.ARROW_LEFT -> {
                    val path = Path().apply {
                        moveTo(w, h * 0.3f)
                        lineTo(w * 0.4f, h * 0.3f)
                        lineTo(w * 0.4f, 0f)
                        lineTo(0f, h / 2f)
                        lineTo(w * 0.4f, h)
                        lineTo(w * 0.4f, h * 0.7f)
                        lineTo(w, h * 0.7f)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.ARROW_UP -> {
                    val path = Path().apply {
                        moveTo(w * 0.3f, h)
                        lineTo(w * 0.3f, h * 0.4f)
                        lineTo(0f, h * 0.4f)
                        lineTo(w / 2f, 0f)
                        lineTo(w, h * 0.4f)
                        lineTo(w * 0.7f, h * 0.4f)
                        lineTo(w * 0.7f, h)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.ARROW_DOWN -> {
                    val path = Path().apply {
                        moveTo(w * 0.3f, 0f)
                        lineTo(w * 0.3f, h * 0.6f)
                        lineTo(0f, h * 0.6f)
                        lineTo(w / 2f, h)
                        lineTo(w, h * 0.6f)
                        lineTo(w * 0.7f, h * 0.6f)
                        lineTo(w * 0.7f, 0f)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.LINE -> {
                    drawLine(color = strokeColor, start = Offset(0f, h/2f), end = Offset(w, h/2f), strokeWidth = elem.strokeWidth.dp.toPx() * 2)
                }
                ShapeType.HEXAGON -> {
                    val path = Path().apply {
                        moveTo(w * 0.25f, 0f)
                        lineTo(w * 0.75f, 0f)
                        lineTo(w, h / 2f)
                        lineTo(w * 0.75f, h)
                        lineTo(w * 0.25f, h)
                        lineTo(0f, h / 2f)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.STAR -> {
                    val path = Path().apply {
                        moveTo(w / 2f, 0f)
                        lineTo(w * 0.65f, h * 0.35f)
                        lineTo(w, h * 0.35f)
                        lineTo(w * 0.72f, h * 0.6f)
                        lineTo(w * 0.85f, h)
                        lineTo(w / 2f, h * 0.78f)
                        lineTo(w * 0.15f, h)
                        lineTo(w * 0.28f, h * 0.6f)
                        lineTo(0f, h * 0.35f)
                        lineTo(w * 0.35f, h * 0.35f)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
                ShapeType.CLOUD -> {
                    val path = Path().apply {
                        moveTo(w * 0.3f, h * 0.8f)
                        cubicTo(w * 0.1f, h * 0.8f, w * 0.05f, h * 0.5f, w * 0.2f, h * 0.4f)
                        cubicTo(w * 0.15f, h * 0.15f, w * 0.45f, h * 0.1f, w * 0.55f, h * 0.25f)
                        cubicTo(w * 0.7f, h * 0.1f, w * 0.9f, h * 0.3f, w * 0.85f, h * 0.5f)
                        cubicTo(w * 0.98f, h * 0.55f, w * 0.95f, h * 0.8f, w * 0.75f, h * 0.8f)
                        close()
                    }
                    drawPath(path = path, color = fillColor)
                    drawPath(path = path, color = strokeColor, style = Stroke(elem.strokeWidth.dp.toPx()))
                }
            }
        }

        if (elem.text.isNotEmpty()) {
            Text(
                text = elem.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
fun PictureRenderer(elem: SlideElement.Picture) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
    ) {
        AsyncImage(
            model = elem.uri,
            contentDescription = elem.caption,
            modifier = Modifier.fillMaxSize()
        )
        if (elem.caption.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Text(
                    text = elem.caption,
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TableRenderer(elem: SlideElement.TableElement, onDoubleTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.LightGray)
            .background(Color.White)
            .clickable { onDoubleTap() }
    ) {
        for (r in 0 until elem.rows) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                for (c in 0 until elem.cols) {
                    val cellText = elem.data.getOrNull(r)?.getOrNull(c) ?: ""
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, Color.Gray)
                            .background(if (r == 0) Color(0xFFD24726).copy(alpha = 0.1f) else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cellText,
                            fontSize = 10.sp,
                            fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartRenderer(elem: SlideElement.ChartElement, onDoubleTap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDoubleTap() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = elem.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (elem.values.isEmpty()) {
                    Text("No chart data", fontSize = 10.sp)
                } else {
                    val maxValue = elem.values.maxOrNull() ?: 100f
                    val scaleFactor = if (maxValue > 0) 1f / maxValue else 1f

                    when (elem.chartType) {
                        ChartType.BAR -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                elem.values.forEachIndexed { idx, valItem ->
                                    val heightPercent = (valItem * scaleFactor) * 0.8f
                                    val lbl = elem.labels.getOrNull(idx) ?: ""

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(valItem.toString(), fontSize = 8.sp, color = Color.Gray)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.6f)
                                                .fillMaxHeight(heightPercent)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFFD24726), Color(0xFFF27D26))
                                                    )
                                                )
                                        )
                                        Text(lbl, fontSize = 7.sp, maxLines = 1, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                        ChartType.LINE -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val spacing = w / (elem.values.size - 1).coerceAtLeast(1)

                                val points = elem.values.mapIndexed { i, v ->
                                    val xPoint = i * spacing
                                    val yPoint = h - ((v * scaleFactor) * h * 0.7f)
                                    Offset(xPoint, yPoint)
                                }

                                for (i in 0 until points.size - 1) {
                                    drawLine(
                                        color = Color(0xFFD24726),
                                        start = points[i],
                                        end = points[i + 1],
                                        strokeWidth = 3f
                                    )
                                }
                                points.forEach { pt ->
                                    drawCircle(color = Color(0xFFD24726), radius = 5f, center = pt)
                                }
                            }
                        }
                        ChartType.PIE -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val total = elem.values.sum()
                                if (total > 0f) {
                                    var currentAngle = 0f
                                    val colors = listOf(Color(0xFFD24726), Color(0xFFF27D26), Color(0xFF3B82F6), Color(0xFF10B981))

                                    elem.values.forEachIndexed { i, v ->
                                        val sweep = (v / total) * 360f
                                        val color = colors[i % colors.size]
                                        drawArc(
                                            color = color,
                                            startAngle = currentAngle,
                                            sweepAngle = sweep,
                                            useCenter = true
                                        )
                                        currentAngle += sweep
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoRenderer(elem: SlideElement.VideoElement, onDoubleTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black)
            .clickable { onDoubleTap() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Video placeholder icon",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = if (elem.videoUrl.isEmpty()) "Double Tap to Insert Video Link" else "Video Added",
                color = Color.LightGray,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// THUMBNAIL BOTTOM CAROUSEL
@Composable
fun SlideThumbnailStrip(viewModel: PresentationViewModel) {
    val presentation by viewModel.currentPresentation.collectAsState()
    val activeIndex by viewModel.currentSlideIndex.collectAsState()

    val slides = presentation?.slides ?: emptyList()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail strip label & Quick Add
        Column(
            modifier = Modifier.padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { viewModel.addSlide() },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFD24726), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Slide", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text("Add", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        // Horizontal Slide Cards scrollway
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(slides) { idx, item ->
                val isActive = idx == activeIndex
                val borderCol = if (isActive) Color(0xFFD24726) else MaterialTheme.colorScheme.outlineVariant

                Card(
                    onClick = {
                        viewModel.currentSlideIndex.value = idx
                        viewModel.selectedElementId.value = null
                    },
                    modifier = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .border(if (isActive) 2.dp else 1.dp, borderCol, RoundedCornerShape(6.dp)),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(item.background))
                                } catch (e: Exception) {
                                    Color.White
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Slideshow,
                                contentDescription = null,
                                tint = if (item.background.uppercase() == "#FFFFFF" || item.background.uppercase() == "#F8FAFC") Color.DarkGray else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Slide ${idx + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.background.uppercase() == "#FFFFFF" || item.background.uppercase() == "#F8FAFC") Color.DarkGray else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Arrangement Controls
        Row(
            modifier = Modifier.padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { viewModel.moveSlideLeft() },
                enabled = activeIndex > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Rearrange Left", modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = { viewModel.moveSlideRight() },
                enabled = activeIndex < slides.size - 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Rearrange Right", modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = { viewModel.deleteSlide() },
                enabled = slides.size > 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete slide", tint = Color.Red, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// BOTTOM EDITING SHEETS (HOME, INSERT, SHAPES, TRANSITIONS, ANIMATIONS, COMMENTS, FILE)
@Composable
fun BottomToolbarSheet(viewModel: PresentationViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val presentation by viewModel.currentPresentation.collectAsState()
    val slideIndex by viewModel.currentSlideIndex.collectAsState()
    val selectedElemId by viewModel.selectedElementId.collectAsState()

    val slide = presentation?.slides?.getOrNull(slideIndex)
    val selectedElement = slide?.elements?.find { it.id == selectedElemId }

    val tabs = listOf(
        TabInfo(EditorTab.HOME, "Home", Icons.Default.FormatAlignLeft),
        TabInfo(EditorTab.INSERT, "Insert", Icons.Default.AddBox),
        TabInfo(EditorTab.SHAPES, "Shapes", Icons.Default.Category),
        TabInfo(EditorTab.TRANSITIONS, "Transitions", Icons.Default.Transform),
        TabInfo(EditorTab.ANIMATIONS, "Animations", Icons.Default.Animation),
        TabInfo(EditorTab.COMMENTS, "Review", Icons.Default.Comment),
        TabInfo(EditorTab.FILE, "File", Icons.Default.Save)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tab Headers Row
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.type == activeTab },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFFD24726),
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                val activeIdx = tabs.indexOfFirst { it.type == activeTab }
                if (activeIdx >= 0) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeIdx]),
                        color = Color(0xFFD24726)
                    )
                }
            }
        ) {
            tabs.forEach { t ->
                Tab(
                    selected = activeTab == t.type,
                    onClick = { viewModel.activeTab.value = t.type },
                    text = { Text(t.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(t.icon, contentDescription = t.title, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // Active Tab Settings Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(12.dp)
        ) {
            when (activeTab) {
                EditorTab.HOME -> {
                    // TEXT FORMATTING AND PRECISION CONTROLS
                    if (selectedElement == null) {
                        // Slide Background picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Slide Canvas Properties", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Select Slide Background Color:", fontSize = 11.sp)
                            val backgrounds = listOf(
                                "#FFFFFF" to "White",
                                "#F1F5F9" to "Sleek Gray",
                                "#0F172A" to "Slate Dark",
                                "#ECFDF5" to "Mint Science",
                                "#FDF2F8" to "Soft Rose",
                                "#FFF1F2" to "Lush Editorial"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                backgrounds.forEach { bg ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(bg.first)))
                                            .border(1.dp, Color.LightGray, CircleShape)
                                            .clickable { viewModel.setSlideBackground(bg.first) }
                                    )
                                }
                            }
                        }
                    } else if (selectedElement is SlideElement.TextBox) {
                        // Formatting tools for TextBox
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Format Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Bold Toggle
                                    IconButton(
                                        onClick = { viewModel.updateTextBoxFormatting(selectedElement.id, isBold = !selectedElement.isBold) },
                                        modifier = Modifier.size(36.dp).background(if (selectedElement.isBold) Color(0xFFD24726).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                                    ) { Text("B", fontWeight = FontWeight.Bold, color = if (selectedElement.isBold) Color(0xFFD24726) else Color.DarkGray) }

                                    // Italic Toggle
                                    IconButton(
                                        onClick = { viewModel.updateTextBoxFormatting(selectedElement.id, isItalic = !selectedElement.isItalic) },
                                        modifier = Modifier.size(36.dp).background(if (selectedElement.isItalic) Color(0xFFD24726).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                                    ) { Text("I", fontStyle = FontStyle.Italic, color = if (selectedElement.isItalic) Color(0xFFD24726) else Color.DarkGray) }

                                    // Alignments
                                    IconButton(
                                        onClick = { viewModel.updateTextBoxFormatting(selectedElement.id, alignment = "LEFT") },
                                        modifier = Modifier.size(36.dp)
                                    ) { Icon(Icons.Default.FormatAlignLeft, contentDescription = "Left", modifier = Modifier.size(16.dp)) }

                                    IconButton(
                                        onClick = { viewModel.updateTextBoxFormatting(selectedElement.id, alignment = "CENTER") },
                                        modifier = Modifier.size(36.dp)
                                    ) { Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", modifier = Modifier.size(16.dp)) }

                                    IconButton(
                                        onClick = { viewModel.updateTextBoxFormatting(selectedElement.id, alignment = "RIGHT") },
                                        modifier = Modifier.size(36.dp)
                                    ) { Icon(Icons.Default.FormatAlignRight, contentDescription = "Right", modifier = Modifier.size(16.dp)) }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Size: ${selectedElement.fontSize}", fontSize = 11.sp)
                                    Slider(
                                        value = selectedElement.fontSize.toFloat(),
                                        onValueChange = { viewModel.updateTextBoxFormatting(selectedElement.id, fontSize = it.toInt()) },
                                        valueRange = 10f..64f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(activeTrackColor = Color(0xFFD24726), thumbColor = Color(0xFFD24726))
                                    )
                                }
                            }

                            // Precise Layout Sliders
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Precise Alignment Tools", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                PreciseBoundsSliders(selectedElement, viewModel)
                            }
                        }
                    } else {
                        // Other non-textbox shape alignments
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Selected Element Position Controls", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            PreciseBoundsSliders(selectedElement, viewModel)
                        }
                    }
                }

                EditorTab.INSERT -> {
                    // INSERT CONTENT TOOLS
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add to Slide Container", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.addTextBox() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = "Text Box", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Text", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.addTable() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.GridOn, contentDescription = "Table", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Table", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.addPicture("android.resource://com.example/drawable/ic_launcher_foreground") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Picture", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Image", fontSize = 11.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.addChart(ChartType.BAR) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = "Chart", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chart", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.addVideo("") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = "Video", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Video", fontSize = 11.sp)
                            }
                        }
                    }
                }

                EditorTab.SHAPES -> {
                    // INSERT SHAPES
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Insert Diagram & Flowchart Shapes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val shapes = listOf(
                            ShapeType.CIRCLE to "Circle",
                            ShapeType.SQUARE to "Square",
                            ShapeType.TRIANGLE to "Triangle",
                            ShapeType.ARROW_RIGHT to "Arrow R",
                            ShapeType.ARROW_LEFT to "Arrow L",
                            ShapeType.LINE to "Divider Line",
                            ShapeType.STAR to "Star Rating",
                            ShapeType.CLOUD to "Cloud"
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(shapes) { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { viewModel.addShape(item.first) }
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .width(60.dp)
                                ) {
                                    Icon(
                                        imageVector = when(item.first) {
                                            ShapeType.CIRCLE -> Icons.Default.Circle
                                            ShapeType.SQUARE -> Icons.Default.Square
                                            ShapeType.TRIANGLE -> Icons.Default.ChangeHistory
                                            ShapeType.ARROW_RIGHT -> Icons.Default.ArrowForward
                                            ShapeType.LINE -> Icons.Default.HorizontalRule
                                            ShapeType.STAR -> Icons.Default.Star
                                            else -> Icons.Default.Cloud
                                        },
                                        contentDescription = item.second,
                                        tint = Color(0xFFD24726),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(item.second, fontSize = 8.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                EditorTab.TRANSITIONS -> {
                    // SLIDE TRANSITIONS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Slide Entry Animations", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Current Slide Transition: ${slide?.transition?.name ?: "None"}", fontSize = 11.sp)
                        val transitionsList = listOf(
                            SlideTransition.NONE to "None",
                            SlideTransition.FADE to "Fade",
                            SlideTransition.SLIDE to "Slide-In",
                            SlideTransition.ZOOM to "Zoom-In",
                            SlideTransition.WIPE to "Wipe",
                            SlideTransition.PUSH to "Push"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            transitionsList.forEach { tr ->
                                val active = slide?.transition == tr.first
                                Button(
                                    onClick = { viewModel.setSlideTransition(tr.first) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (active) Color(0xFFD24726) else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text(tr.second, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                EditorTab.ANIMATIONS -> {
                    // ELEMENT ENTRANCE ANIMATIONS
                    if (selectedElement == null) {
                        Text("Select an element on slide to customize entry animation effects", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Element Entrance Animation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Selected: ${selectedElement.id.split("_").firstOrNull() ?: ""} Animation: ${selectedElement.animation.name}", fontSize = 11.sp)
                            val animList = listOf(
                                ElementAnimation.NONE to "None",
                                ElementAnimation.FADE_IN to "Fade In",
                                ElementAnimation.ZOOM_IN to "Zoom In",
                                ElementAnimation.SLIDE_IN to "Slide In",
                                ElementAnimation.BOUNCE to "Bounce",
                                ElementAnimation.SPIN to "Spin Effect"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                animList.forEach { an ->
                                    val active = selectedElement.animation == an.first
                                    Button(
                                        onClick = { viewModel.updateElementAnimation(selectedElement.id, an.first) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (active) Color(0xFFD24726) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text(an.second, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                EditorTab.COMMENTS -> {
                    // SLIDE LEVEL DISCUSSIONS / SPEAKERS NOTES
                    val currentSlideComments = presentation?.comments?.filter { it.slideId == slide?.id } ?: emptyList()
                    val commentText by viewModel.commentTextInput.collectAsState()
                    val commentAuthor by viewModel.commentAuthorInput.collectAsState()

                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Slide Comments & Collaboration", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedTextField(
                                value = commentAuthor,
                                onValueChange = { viewModel.commentAuthorInput.value = it },
                                placeholder = { Text("Your Name") },
                                label = { Text("Author", fontSize = 8.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { viewModel.commentTextInput.value = it },
                                placeholder = { Text("Write comment...") },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.addCommentOnSlide(commentText, commentAuthor) }) {
                                        Icon(Icons.Default.Send, contentDescription = "Add comment", tint = Color(0xFFD24726), modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }

                        // Comments List View
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (currentSlideComments.isEmpty()) {
                                Text("No comments yet. Co-authors can write comments here.", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            } else {
                                currentSlideComments.forEach { comment ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFD24726))
                                                Text(comment.text, fontSize = 9.sp, color = Color.DarkGray)
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteComment(comment.id) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "delete", tint = Color.Red, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                EditorTab.FILE -> {
                    // FILE INTEGRATION (EXPORT, LOCAL SAVE, AUTO-SAVE TOGGLE)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Local Presentation Management", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    viewModel.exportCurrentPresentation()
                                    Toast.makeText(context, "Presentation saved successfully to local storage!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD24726)),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = "Export file")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save As JSON File", fontSize = 12.sp)
                            }

                            val isAutoSaveVal by viewModel.isAutoSaveEnabled.collectAsState()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Auto Save:", fontSize = 11.sp)
                                Switch(
                                    checked = isAutoSaveVal,
                                    onCheckedChange = { viewModel.isAutoSaveEnabled.value = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD24726), checkedTrackColor = Color(0xFFD24726).copy(alpha = 0.4f))
                                )
                            }
                        }

                        // Print & Share
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Exporting to PDF file...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export to PDF", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "Connecting cloud system...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Collaboration Link", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreciseBoundsSliders(selectedElement: SlideElement, viewModel: PresentationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // X slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("X:", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
            Slider(
                value = selectedElement.x,
                onValueChange = { viewModel.updateElementBounds(selectedElement.id, it, selectedElement.y, selectedElement.width, selectedElement.height) },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f).height(16.dp),
                colors = SliderDefaults.colors(activeTrackColor = Color(0xFFD24726), thumbColor = Color(0xFFD24726))
            )
            Text("${selectedElement.x.toInt()}%", fontSize = 9.sp, modifier = Modifier.width(28.dp))
        }

        // Y slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Y:", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
            Slider(
                value = selectedElement.y,
                onValueChange = { viewModel.updateElementBounds(selectedElement.id, selectedElement.x, it, selectedElement.width, selectedElement.height) },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f).height(16.dp),
                colors = SliderDefaults.colors(activeTrackColor = Color(0xFFD24726), thumbColor = Color(0xFFD24726))
            )
            Text("${selectedElement.y.toInt()}%", fontSize = 9.sp, modifier = Modifier.width(28.dp))
        }

        // Width slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("W:", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
            Slider(
                value = selectedElement.width,
                onValueChange = { viewModel.updateElementBounds(selectedElement.id, selectedElement.x, selectedElement.y, it, selectedElement.height) },
                valueRange = 5f..100f,
                modifier = Modifier.weight(1f).height(16.dp),
                colors = SliderDefaults.colors(activeTrackColor = Color(0xFFD24726), thumbColor = Color(0xFFD24726))
            )
            Text("${selectedElement.width.toInt()}%", fontSize = 9.sp, modifier = Modifier.width(28.dp))
        }

        // Height slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H:", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
            Slider(
                value = selectedElement.height,
                onValueChange = { viewModel.updateElementBounds(selectedElement.id, selectedElement.x, selectedElement.y, selectedElement.width, it) },
                valueRange = 5f..100f,
                modifier = Modifier.weight(1f).height(16.dp),
                colors = SliderDefaults.colors(activeTrackColor = Color(0xFFD24726), thumbColor = Color(0xFFD24726))
            )
            Text("${selectedElement.height.toInt()}%", fontSize = 9.sp, modifier = Modifier.width(28.dp))
        }
    }
}

data class TabInfo(
    val type: EditorTab,
    val title: String,
    val icon: ImageVector
)
