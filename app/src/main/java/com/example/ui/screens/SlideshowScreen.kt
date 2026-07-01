package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.ActiveScreen
import com.example.ui.PresentationViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SlideshowScreen(viewModel: PresentationViewModel, startIndex: Int) {
    val presentation by viewModel.currentPresentation.collectAsState()
    var currentIdx by remember { mutableStateOf(startIndex) }

    val slides = presentation?.slides ?: emptyList()
    val slide = slides.getOrNull(currentIdx)

    // Interactive presentation utilities
    var isLaserPointerEnabled by remember { mutableStateOf(false) }
    var isPenEnabled by remember { mutableStateOf(false) }
    var isSpeakerNotesVisible by remember { mutableStateOf(false) }

    // Laser pointer coordinate
    var laserCoordinate by remember { mutableStateOf<Offset?>(null) }

    // Pen drawing coordinates
    var currentDrawColor by remember { mutableStateOf(Color.Red) }
    var penPaths = remember { mutableStateListOf<DrawingPath>() }
    var activePath by remember { mutableStateOf<Path?>(null) }

    if (presentation == null || slide == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No slides to present.")
        }
        return
    }

    Scaffold(
        containerColor = Color.Black // Immersive pitch black screen
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MAIN PRESENTATION SLIDE BOX with animated entry transition!
            AnimatedContent(
                targetState = currentIdx,
                transitionSpec = {
                    val slideTrans = slides.getOrNull(targetState)?.transition ?: SlideTransition.NONE
                    when (slideTrans) {
                        SlideTransition.FADE -> {
                            fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500))
                        }
                        SlideTransition.SLIDE -> {
                            slideInHorizontally(animationSpec = tween(450)) { width -> width } with slideOutHorizontally(animationSpec = tween(450)) { width -> -width }
                        }
                        SlideTransition.ZOOM -> {
                            scaleIn(initialScale = 0.8f, animationSpec = tween(400)) + fadeIn() with scaleOut(targetScale = 1.1f, animationSpec = tween(400)) + fadeOut()
                        }
                        else -> {
                            fadeIn(animationSpec = tween(100)) with fadeOut(animationSpec = tween(100))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { targetIndex ->
                val targetSlide = slides[targetIndex]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(targetSlide.background))
                            } catch (e: Exception) {
                                Color.White
                            }
                        )
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val containerW = maxWidth.value
                        val containerH = maxHeight.value

                        targetSlide.elements.forEach { elem ->
                            // Element Entrance Animations
                            val animState = remember(targetIndex) { Animatable(0f) }
                            LaunchedEffect(targetIndex) {
                                when (elem.animation) {
                                    ElementAnimation.NONE -> animState.snapTo(1f)
                                    else -> animState.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 600,
                                            easing = LinearOutSlowInEasing
                                        )
                                    )
                                }
                            }

                            // Scaling animations
                            val animatedModifier = when (elem.animation) {
                                ElementAnimation.FADE_IN -> Modifier.background(Color.Transparent).size(1.dp) // placeholder logic, done via graphicsLayer
                                else -> Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = ((elem.x / 100f) * containerW).dp,
                                        y = ((elem.y / 100f) * containerH).dp
                                    )
                                    .size(
                                        width = ((elem.width / 100f) * containerW).dp,
                                        height = ((elem.height / 100f) * containerH).dp
                                    )
                                    .pointerInput(Unit) {
                                        // Prevents element taps interfering with slide touches unless desired
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .animateGraphicsLayer(elem.animation, animState.value)
                                ) {
                                    when (elem) {
                                        is SlideElement.TextBox -> TextBoxRenderer(elem = elem, onDoubleTap = {})
                                        is SlideElement.Shape -> ShapeRenderer(elem = elem, onDoubleTap = {})
                                        is SlideElement.Picture -> PictureRenderer(elem = elem)
                                        is SlideElement.TableElement -> TableRenderer(elem = elem, onDoubleTap = {})
                                        is SlideElement.ChartElement -> ChartRenderer(elem = elem, onDoubleTap = {})
                                        is SlideElement.VideoElement -> VideoRenderer(elem = elem, onDoubleTap = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // GESTURE CANVAS FOR DRAWING ANNOTATIONS AND LASER POINTER
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLaserPointerEnabled, isPenEnabled) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (isLaserPointerEnabled) {
                                    laserCoordinate = offset
                                } else if (isPenEnabled) {
                                    activePath = Path().apply { moveTo(offset.x, offset.y) }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentOffset = change.position
                                if (isLaserPointerEnabled) {
                                    laserCoordinate = currentOffset
                                } else if (isPenEnabled) {
                                    activePath?.lineTo(currentOffset.x, currentOffset.y)
                                    // Trigger canvas redraw
                                    val temp = activePath
                                    activePath = null
                                    activePath = temp
                                }
                            },
                            onDragEnd = {
                                if (isLaserPointerEnabled) {
                                    laserCoordinate = null
                                } else if (isPenEnabled) {
                                    activePath?.let {
                                        penPaths.add(DrawingPath(it, currentDrawColor))
                                    }
                                    activePath = null
                                }
                            }
                        )
                    }
            ) {
                // Render custom drawing pen paths
                penPaths.forEach { drawPath ->
                    drawPath(
                        path = drawPath.path,
                        color = drawPath.color,
                        style = Stroke(width = 6f)
                    )
                }

                // Render current active path
                activePath?.let { path ->
                    drawPath(
                        path = path,
                        color = currentDrawColor,
                        style = Stroke(width = 6f)
                    )
                }

                // Render glowing laser pointer trail
                laserCoordinate?.let { coord ->
                    drawCircle(
                        color = Color.Red,
                        radius = 12f,
                        center = coord
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = 4f,
                        center = coord
                    )
                }
            }

            // TAP AREAS FOR PREVIOUS / NEXT SLIDE NAVIGATION
            Row(modifier = Modifier.fillMaxSize()) {
                // Previous Slide trigger (Left 15%)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.15f)
                        .clickable {
                            if (currentIdx > 0) {
                                currentIdx--
                                laserCoordinate = null
                                // Clear annotations of previous slide unless persistent is wanted.
                                // In PowerPoint, we clear per slide transition.
                                penPaths.clear()
                            }
                        }
                )

                // Mid area (No tap actions to allow laser pointer drag)
                Box(modifier = Modifier.fillMaxHeight().weight(0.7f))

                // Next Slide trigger (Right 15%)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.15f)
                        .clickable {
                            if (currentIdx < slides.size - 1) {
                                currentIdx++
                                laserCoordinate = null
                                penPaths.clear()
                            }
                        }
                )
            }

            // FLOATING PRESENTATION CONTROL PANEL (Immersive Top Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit Presentation
                IconButton(
                    onClick = { viewModel.activeScreen.value = ActiveScreen.Editor },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Presentation", tint = Color.White)
                }

                // Slide Index Info
                Text(
                    text = "${currentIdx + 1} / ${slides.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Laser, Pen, Notes triggers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Laser Toggle
                    IconButton(
                        onClick = {
                            isLaserPointerEnabled = !isLaserPointerEnabled
                            if (isLaserPointerEnabled) isPenEnabled = false
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isLaserPointerEnabled) Color.Red else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Laser Pointer",
                            tint = if (isLaserPointerEnabled) Color.White else Color.LightGray
                        )
                    }

                    // Pen Toggle
                    IconButton(
                        onClick = {
                            isPenEnabled = !isPenEnabled
                            if (isPenEnabled) isLaserPointerEnabled = false
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isPenEnabled) Color(0xFFD24726) else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Drawing Pen Annotation",
                            tint = if (isPenEnabled) Color.White else Color.LightGray
                        )
                    }

                    // Speaker notes toggle
                    IconButton(
                        onClick = { isSpeakerNotesVisible = !isSpeakerNotesVisible },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = "Toggle Speaker Notes",
                            tint = if (isSpeakerNotesVisible) Color(0xFFD24726) else Color.LightGray
                        )
                    }

                    // Clear drawings shortcut
                    if (isPenEnabled) {
                        IconButton(
                            onClick = { penPaths.clear() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.LayersClear, contentDescription = "Clear ink", tint = Color.LightGray)
                        }
                    }
                }
            }

            // PEN BRUSH COLOR SELECTOR (Shows when pen is active)
            if (isPenEnabled) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-70).dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val brushColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.White)
                    brushColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (currentDrawColor == color) 2.dp else 0.dp,
                                    Color(0xFFD24726),
                                    CircleShape
                                )
                                .clickable { currentDrawColor = color }
                        )
                    }
                }
            }

            // EXPANDABLE SPEAKER NOTES / COLLABORATOR CHAT SHEET
            if (isSpeakerNotesVisible) {
                val comments = presentation?.comments?.filter { it.slideId == slide.id } ?: emptyList()
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Presenter Notes & Comments", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(
                                onClick = { isSpeakerNotesVisible = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Notes")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Speaker comments list scrollway
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "💡 Tip: Swipe left or right near the edges of the slide to navigate back/forth in full screen.",
                                fontSize = 11.sp,
                                color = Color(0xFFD24726)
                            )
                            if (comments.isEmpty()) {
                                Text("No speaker notes or discussions saved on this slide. Write some comments in the Editor's Review tab to view here.", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                comments.forEach { comment ->
                                    Text(
                                        text = "• [${comment.author}]: ${comment.text}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom animated modifier utility representing actual presentation elements entrance animation!
fun Modifier.animateGraphicsLayer(
    animationType: ElementAnimation,
    progress: Float
): Modifier = this.then(
    Modifier.graphicsLayer {
        when (animationType) {
            ElementAnimation.FADE_IN -> {
                alpha = progress
            }
            ElementAnimation.ZOOM_IN -> {
                alpha = progress
                scaleX = 0.5f + (progress * 0.5f)
                scaleY = 0.5f + (progress * 0.5f)
            }
            ElementAnimation.SLIDE_IN -> {
                alpha = progress
                translationY = 100f * (1f - progress)
            }
            ElementAnimation.BOUNCE -> {
                alpha = progress
                // custom bouncy overshoot calculation
                val overshoot = progress * 1.2f
                scaleX = if (overshoot > 1.0f) 1.0f else overshoot
                scaleY = if (overshoot > 1.0f) 1.0f else overshoot
            }
            ElementAnimation.SPIN -> {
                alpha = progress
                rotationZ = 360f * progress
            }
            ElementAnimation.NONE -> {
                alpha = 1f
            }
        }
    }
)

data class DrawingPath(
    val path: Path,
    val color: Color
)
