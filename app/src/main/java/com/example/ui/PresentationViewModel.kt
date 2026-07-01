package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ActiveScreen {
    object Home : ActiveScreen()
    object TemplateSelection : ActiveScreen()
    object Editor : ActiveScreen()
    data class Slideshow(val startIndex: Int) : ActiveScreen()
    object AboutDeveloper : ActiveScreen()
}

enum class EditorTab {
    HOME, INSERT, SHAPES, TRANSITIONS, ANIMATIONS, COMMENTS, FILE
}

class PresentationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PresentationRepository
    val allPresentations: StateFlow<List<Presentation>>

    // Screen State
    val activeScreen = MutableStateFlow<ActiveScreen>(ActiveScreen.Home)

    // Current Presentation being edited
    val currentPresentation = MutableStateFlow<Presentation?>(null)
    val currentSlideIndex = MutableStateFlow(0)
    val selectedElementId = MutableStateFlow<String?>(null)

    // Editor UI state
    val activeTab = MutableStateFlow(EditorTab.HOME)
    val isAutoSaveEnabled = MutableStateFlow(true)
    val syncStatus = MutableStateFlow("Synced") // Synced, Syncing, Offline

    // Collaborative comment box input
    val commentAuthorInput = MutableStateFlow("Presenter")
    val commentTextInput = MutableStateFlow("")

    // Export & Import callbacks (delegated to MainActivity)
    var requestExportJson: ((filename: String, json: String) -> Unit)? = null
    var requestImportJson: (() -> Unit)? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PresentationRepository(database.presentationDao())
        allPresentations = repository.allPresentations
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed some initial presentations if the database is empty
        viewModelScope.launch {
            allPresentations.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultPresentations()
                }
            }
        }
    }

    // CREATE PRESENTATION FROM TEMPLATE
    fun createPresentationFromTemplate(templateName: String) {
        val p = when (templateName) {
            "Blank Presentation" -> createBlankPresentation()
            "Startup Pitch Deck" -> createStartupPitchDeck()
            "Minimal Corporate" -> createMinimalCorporate()
            "Creative Portfolio" -> createCreativePortfolio()
            "Science Lesson" -> createScienceLesson()
            else -> createBlankPresentation()
        }
        viewModelScope.launch {
            repository.savePresentation(p)
            currentPresentation.value = p
            currentSlideIndex.value = 0
            selectedElementId.value = null
            activeScreen.value = ActiveScreen.Editor
        }
    }

    // OPEN PRESENTATION FOR EDITING
    fun editPresentation(presentation: Presentation) {
        currentPresentation.value = presentation
        currentSlideIndex.value = 0
        selectedElementId.value = null
        activeScreen.value = ActiveScreen.Editor
    }

    // BACK TO HOME
    fun navigateToHome() {
        saveCurrentPresentation()
        activeScreen.value = ActiveScreen.Home
    }

    // AUTO-SAVE LOGIC
    private fun saveCurrentPresentation() {
        val p = currentPresentation.value ?: return
        viewModelScope.launch {
            val updated = p.copy(lastModified = System.currentTimeMillis())
            repository.savePresentation(updated)
            syncStatus.value = "Synced"
        }
    }

    private fun triggerAutoSave() {
        if (isAutoSaveEnabled.value) {
            syncStatus.value = "Syncing..."
            saveCurrentPresentation()
        }
    }

    // DELETE PRESENTATION
    fun deletePresentation(id: String) {
        viewModelScope.launch {
            repository.deletePresentationById(id)
            if (currentPresentation.value?.id == id) {
                currentPresentation.value = null
                activeScreen.value = ActiveScreen.Home
            }
        }
    }

    // EXPORT FILE TO LOCAL STORAGE
    fun exportCurrentPresentation() {
        val p = currentPresentation.value ?: return
        val filename = "${p.title.replace(" ", "_").lowercase()}_presento.json"
        val slidesJson = JsonConverter.serializeSlides(p.slides)
        
        // Wrap everything in a main json structure
        val root = org.json.JSONObject()
        root.put("id", p.id)
        root.put("title", p.title)
        root.put("slides", org.json.JSONArray(slidesJson))
        root.put("comments", org.json.JSONArray(JsonConverter.serializeComments(p.comments)))
        root.put("lastModified", p.lastModified)
        root.put("isCloudSynced", p.isCloudSynced)

        requestExportJson?.invoke(filename, root.toString(2))
    }

    // IMPORT FILE FROM LOCAL STORAGE
    fun importPresentationFromJson(jsonStr: String) {
        try {
            val root = org.json.JSONObject(jsonStr)
            val id = root.optString("id", UUID.randomUUID().toString())
            val title = root.optString("title", "Imported Presentation")
            val slidesArray = root.optJSONArray("slides")?.toString() ?: "[]"
            val commentsArray = root.optJSONArray("comments")?.toString() ?: "[]"
            
            val p = Presentation(
                id = id,
                title = title,
                slides = JsonConverter.deserializeSlides(slidesArray),
                comments = JsonConverter.deserializeComments(commentsArray),
                lastModified = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.savePresentation(p)
                editPresentation(p)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // RENAME PRESENTATION
    fun renamePresentation(newTitle: String) {
        val p = currentPresentation.value ?: return
        currentPresentation.value = p.copy(title = newTitle)
        triggerAutoSave()
    }

    // SLIDE NAVIGATION & ACTIONS
    fun addSlide() {
        val p = currentPresentation.value ?: return
        val newSlide = Slide(id = "slide_${UUID.randomUUID()}", background = "#FFFFFF")
        val newList = p.slides.toMutableList()
        newList.add(newSlide)
        currentPresentation.value = p.copy(slides = newList)
        currentSlideIndex.value = newList.size - 1
        selectedElementId.value = null
        triggerAutoSave()
    }

    fun duplicateSlide() {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val sourceSlide = p.slides[idx]
            val duplicatedSlide = sourceSlide.copy(id = "slide_${UUID.randomUUID()}")
            val newList = p.slides.toMutableList()
            newList.add(idx + 1, duplicatedSlide)
            currentPresentation.value = p.copy(slides = newList)
            currentSlideIndex.value = idx + 1
            selectedElementId.value = null
            triggerAutoSave()
        }
    }

    fun deleteSlide() {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (p.slides.size > 1 && idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            newList.removeAt(idx)
            currentPresentation.value = p.copy(slides = newList)
            currentSlideIndex.value = (idx - 1).coerceAtLeast(0)
            selectedElementId.value = null
            triggerAutoSave()
        }
    }

    fun moveSlideLeft() {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx > 0 && idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList.removeAt(idx)
            newList.add(idx - 1, slide)
            currentPresentation.value = p.copy(slides = newList)
            currentSlideIndex.value = idx - 1
            triggerAutoSave()
        }
    }

    fun moveSlideRight() {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx < p.slides.size - 1 && idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList.removeAt(idx)
            newList.add(idx + 1, slide)
            currentPresentation.value = p.copy(slides = newList)
            currentSlideIndex.value = idx + 1
            triggerAutoSave()
        }
    }

    fun setSlideBackground(hexColor: String) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            newList[idx] = newList[idx].copy(background = hexColor)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    fun setSlideTransition(transition: SlideTransition) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            newList[idx] = newList[idx].copy(transition = transition)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    // ELEMENT ACTIONS (ADD)
    fun addTextBox() {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val elemId = "textbox_${UUID.randomUUID()}"
            val newElem = SlideElement.TextBox(
                id = elemId,
                x = 15f, y = 30f, width = 70f, height = 15f,
                text = "Double Tap to Edit Text"
            )
            val newList = p.slides.toMutableList()
            val currentSlide = newList[idx]
            newList[idx] = currentSlide.copy(elements = currentSlide.elements + newElem)
            currentPresentation.value = p.copy(slides = newList)
            selectedElementId.value = elemId
            triggerAutoSave()
        }
    }

    fun addShape(shapeType: ShapeType) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val elemId = "shape_${UUID.randomUUID()}"
            val newElem = SlideElement.Shape(
                id = elemId,
                x = 35f, y = 35f, width = 30f, height = 20f,
                shapeType = shapeType
            )
            val newList = p.slides.toMutableList()
            val currentSlide = newList[idx]
            newList[idx] = currentSlide.copy(elements = currentSlide.elements + newElem)
            currentPresentation.value = p.copy(slides = newList)
            selectedElementId.value = elemId
            triggerAutoSave()
        }
    }

    fun addPicture(uri: String) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val elemId = "picture_${UUID.randomUUID()}"
            val newElem = SlideElement.Picture(
                id = elemId,
                x = 25f, y = 20f, width = 50f, height = 45f,
                uri = uri,
                caption = "Imported Image"
            )
            val newList = p.slides.toMutableList()
            val currentSlide = newList[idx]
            newList[idx] = currentSlide.copy(elements = currentSlide.elements + newElem)
            currentPresentation.value = p.copy(slides = newList)
            selectedElementId.value = elemId
            triggerAutoSave()
        }
    }

    fun addTable() {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val elemId = "table_${UUID.randomUUID()}"
            val newElem = SlideElement.TableElement(
                id = elemId,
                x = 15f, y = 25f, width = 70f, height = 40f,
                rows = 3, cols = 3,
                data = listOf(
                    listOf("Product", "Q1", "Q2"),
                    listOf("Presento", "12k", "24k"),
                    listOf("Template", "8k", "15k")
                )
            )
            val newList = p.slides.toMutableList()
            val currentSlide = newList[idx]
            newList[idx] = currentSlide.copy(elements = currentSlide.elements + newElem)
            currentPresentation.value = p.copy(slides = newList)
            selectedElementId.value = elemId
            triggerAutoSave()
        }
    }

    fun addChart(chartType: ChartType) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val elemId = "chart_${UUID.randomUUID()}"
            val newElem = SlideElement.ChartElement(
                id = elemId,
                x = 15f, y = 20f, width = 70f, height = 50f,
                chartType = chartType,
                title = "Sales Projections",
                labels = listOf("USA", "EU", "Asia", "Africa"),
                values = listOf(75f, 45f, 90f, 30f)
            )
            val newList = p.slides.toMutableList()
            val currentSlide = newList[idx]
            newList[idx] = currentSlide.copy(elements = currentSlide.elements + newElem)
            currentPresentation.value = p.copy(slides = newList)
            selectedElementId.value = elemId
            triggerAutoSave()
        }
    }

    fun addVideo(videoUrl: String) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val elemId = "video_${UUID.randomUUID()}"
            val newElem = SlideElement.VideoElement(
                id = elemId,
                x = 20f, y = 25f, width = 60f, height = 40f,
                videoUrl = videoUrl
            )
            val newList = p.slides.toMutableList()
            val currentSlide = newList[idx]
            newList[idx] = currentSlide.copy(elements = currentSlide.elements + newElem)
            currentPresentation.value = p.copy(slides = newList)
            selectedElementId.value = elemId
            triggerAutoSave()
        }
    }

    // ELEMENT POSITION & SIZING & ANIMATION MODIFICATIONS
    fun updateElementBounds(elemId: String, x: Float, y: Float, w: Float, h: Float) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updatedElems = slide.elements.map {
                if (it.id == elemId) it.copyWithNewBounds(x, y, w, h) else it
            }
            newList[idx] = slide.copy(elements = updatedElems)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    fun deleteElement(elemId: String) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            newList[idx] = slide.copy(elements = slide.elements.filterNot { it.id == elemId })
            currentPresentation.value = p.copy(slides = newList)
            if (selectedElementId.value == elemId) {
                selectedElementId.value = null
            }
            triggerAutoSave()
        }
    }

    fun updateElementAnimation(elemId: String, animation: ElementAnimation) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updatedElems = slide.elements.map {
                if (it.id == elemId) it.copyWithNewAnimation(animation) else it
            }
            newList[idx] = slide.copy(elements = updatedElems)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    // FORMATTING UPDATES ON SPECIFIC ELEMENT TYPES
    fun updateTextBoxFormatting(
        elemId: String,
        text: String? = null,
        fontSize: Int? = null,
        color: String? = null,
        isBold: Boolean? = null,
        isItalic: Boolean? = null,
        isUnderline: Boolean? = null,
        alignment: String? = null,
        fontFamily: String? = null
    ) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updated = slide.elements.map {
                if (it.id == elemId && it is SlideElement.TextBox) {
                    it.copy(
                        text = text ?: it.text,
                        fontSize = fontSize ?: it.fontSize,
                        color = color ?: it.color,
                        isBold = isBold ?: it.isBold,
                        isItalic = isItalic ?: it.isItalic,
                        isUnderline = isUnderline ?: it.isUnderline,
                        alignment = alignment ?: it.alignment,
                        fontFamily = fontFamily ?: it.fontFamily
                    )
                } else it
            }
            newList[idx] = slide.copy(elements = updated)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    fun updateShapeFormatting(
        elemId: String,
        shapeType: ShapeType? = null,
        fillColor: String? = null,
        strokeColor: String? = null,
        strokeWidth: Float? = null,
        text: String? = null
    ) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updated = slide.elements.map {
                if (it.id == elemId && it is SlideElement.Shape) {
                    it.copy(
                        shapeType = shapeType ?: it.shapeType,
                        fillColor = fillColor ?: it.fillColor,
                        strokeColor = strokeColor ?: it.strokeColor,
                        strokeWidth = strokeWidth ?: it.strokeWidth,
                        text = text ?: it.text
                    )
                } else it
            }
            newList[idx] = slide.copy(elements = updated)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    fun updateTableFormatting(
        elemId: String,
        rows: Int? = null,
        cols: Int? = null,
        data: List<List<String>>? = null
    ) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updated = slide.elements.map {
                if (it.id == elemId && it is SlideElement.TableElement) {
                    it.copy(
                        rows = rows ?: it.rows,
                        cols = cols ?: it.cols,
                        data = data ?: it.data
                    )
                } else it
            }
            newList[idx] = slide.copy(elements = updated)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    fun updateChartFormatting(
        elemId: String,
        chartType: ChartType? = null,
        title: String? = null,
        labels: List<String>? = null,
        values: List<Float>? = null
    ) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updated = slide.elements.map {
                if (it.id == elemId && it is SlideElement.ChartElement) {
                    it.copy(
                        chartType = chartType ?: it.chartType,
                        title = title ?: it.title,
                        labels = labels ?: it.labels,
                        values = values ?: it.values
                    )
                } else it
            }
            newList[idx] = slide.copy(elements = updated)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    fun updateVideoUrl(elemId: String, url: String) {
        val p = currentPresentation.value ?: return
        val idx = currentSlideIndex.value
        if (idx in p.slides.indices) {
            val newList = p.slides.toMutableList()
            val slide = newList[idx]
            val updated = slide.elements.map {
                if (it.id == elemId && it is SlideElement.VideoElement) {
                    it.copy(videoUrl = url)
                } else it
            }
            newList[idx] = slide.copy(elements = updated)
            currentPresentation.value = p.copy(slides = newList)
            triggerAutoSave()
        }
    }

    // COMMENT FUNCTIONALITIES (Collaboration Tab)
    fun addCommentOnSlide(text: String, author: String) {
        val p = currentPresentation.value ?: return
        if (text.isEmpty() || author.isEmpty()) return
        val cIdx = currentSlideIndex.value
        if (cIdx in p.slides.indices) {
            val slideId = p.slides[cIdx].id
            val newComment = Comment(
                id = "comment_${UUID.randomUUID()}",
                slideId = slideId,
                author = author,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            currentPresentation.value = p.copy(comments = p.comments + newComment)
            commentTextInput.value = "" // clear input
            triggerAutoSave()
        }
    }

    fun deleteComment(commentId: String) {
        val p = currentPresentation.value ?: return
        currentPresentation.value = p.copy(comments = p.comments.filterNot { it.id == commentId })
        triggerAutoSave()
    }

    // PRIVATE FACTORY FUNCTIONS TO SEED AND BUILD TEMPLATES
    private fun createBlankPresentation(): Presentation {
        val slide1 = Slide(
            id = "slide_${UUID.randomUUID()}",
            background = "#FFFFFF",
            elements = listOf(
                SlideElement.TextBox(
                    id = "title_${UUID.randomUUID()}",
                    x = 10f, y = 30f, width = 80f, height = 20f,
                    text = "Double Tap to Add Title",
                    fontSize = 32,
                    isBold = true,
                    alignment = "CENTER"
                ),
                SlideElement.TextBox(
                    id = "sub_${UUID.randomUUID()}",
                    x = 15f, y = 52f, width = 70f, height = 15f,
                    text = "Double Tap to Add Subtitle",
                    fontSize = 18,
                    alignment = "CENTER",
                    color = "#666666"
                )
            )
        )
        return Presentation(
            id = UUID.randomUUID().toString(),
            title = "Presentation 1",
            slides = listOf(slide1),
            lastModified = System.currentTimeMillis()
        )
    }

    private fun createStartupPitchDeck(): Presentation {
        val id = UUID.randomUUID().toString()
        val s1 = Slide(
            id = "slide_1",
            background = "#0F172A", // Dark Slate Blue
            elements = listOf(
                SlideElement.TextBox(
                    id = "s1_title", x = 10f, y = 32f, width = 80f, height = 22f,
                    text = "PRESENTO MOBILE SYSTEM",
                    fontSize = 34, isBold = true, color = "#F59E0B", alignment = "CENTER",
                    fontFamily = "SERIF"
                ),
                SlideElement.TextBox(
                    id = "s1_sub", x = 15f, y = 55f, width = 70f, height = 12f,
                    text = "High Fidelity Presentation Engine for Android",
                    fontSize = 16, color = "#94A3B8", alignment = "CENTER"
                ),
                SlideElement.Shape(
                    id = "s1_accent", x = 45f, y = 20f, width = 10f, height = 8f,
                    shapeType = ShapeType.HEXAGON, fillColor = "#F59E0B", strokeColor = "#D97706"
                )
            )
        )
        val s2 = Slide(
            id = "slide_2",
            background = "#1E293B",
            elements = listOf(
                SlideElement.TextBox(
                    id = "s2_title", x = 8f, y = 10f, width = 84f, height = 10f,
                    text = "Our Core Objectives",
                    fontSize = 26, isBold = true, color = "#F8FAFC", alignment = "LEFT"
                ),
                SlideElement.Shape(
                    id = "s2_circle", x = 12f, y = 30f, width = 18f, height = 18f,
                    shapeType = ShapeType.CIRCLE, fillColor = "#3B82F6", strokeColor = "#1D4ED8",
                    text = "1"
                ),
                SlideElement.TextBox(
                    id = "s2_text1", x = 34f, y = 32f, width = 56f, height = 16f,
                    text = "Ensure seamless offline-first experience.",
                    fontSize = 16, color = "#CBD5E1"
                ),
                SlideElement.Shape(
                    id = "s2_square", x = 12f, y = 55f, width = 18f, height = 18f,
                    shapeType = ShapeType.SQUARE, fillColor = "#10B981", strokeColor = "#047857",
                    text = "2"
                ),
                SlideElement.TextBox(
                    id = "s2_text2", x = 34f, y = 57f, width = 56f, height = 16f,
                    text = "Empower local file editing & save integration.",
                    fontSize = 16, color = "#CBD5E1"
                )
            )
        )
        val s3 = Slide(
            id = "slide_3",
            background = "#0F172A",
            elements = listOf(
                SlideElement.TextBox(
                    id = "s3_title", x = 8f, y = 10f, width = 84f, height = 10f,
                    text = "Global Market Traction",
                    fontSize = 24, isBold = true, color = "#FFFFFF", alignment = "CENTER"
                ),
                SlideElement.ChartElement(
                    id = "s3_chart", x = 10f, y = 25f, width = 80f, height = 65f,
                    chartType = ChartType.BAR, title = "Quarterly Signups (Millions)",
                    labels = listOf("USA", "GER", "IND", "BRZ"),
                    values = listOf(65f, 40f, 85f, 50f)
                )
            )
        )
        return Presentation(
            id = id,
            title = "Startup Pitch Deck",
            slides = listOf(s1, s2, s3),
            lastModified = System.currentTimeMillis()
        )
    }

    private fun createMinimalCorporate(): Presentation {
        val id = UUID.randomUUID().toString()
        val s1 = Slide(
            id = "slide_1",
            background = "#F8FAFC", // Ultra Light Gray
            elements = listOf(
                SlideElement.TextBox(
                    id = "s1_corp", x = 10f, y = 25f, width = 80f, height = 8f,
                    text = "QUARTERLY STATISTICAL REPORT",
                    fontSize = 14, isBold = true, color = "#1E3A8A", alignment = "LEFT"
                ),
                SlideElement.TextBox(
                    id = "s1_title", x = 10f, y = 35f, width = 80f, height = 25f,
                    text = "Revenue & Growth Metrics",
                    fontSize = 36, isBold = true, color = "#0F172A", alignment = "LEFT"
                ),
                SlideElement.Shape(
                    id = "s1_line", x = 10f, y = 62f, width = 40f, height = 1f,
                    shapeType = ShapeType.LINE, fillColor = "#1E3A8A", strokeColor = "#1E3A8A", strokeWidth = 3f
                ),
                SlideElement.TextBox(
                    id = "s1_sub", x = 10f, y = 67f, width = 60f, height = 10f,
                    text = "Compiled by Presento Analytics • FY26",
                    fontSize = 14, color = "#475569"
                )
            )
        )
        val s2 = Slide(
            id = "slide_2",
            background = "#FFFFFF",
            elements = listOf(
                SlideElement.TextBox(
                    id = "s2_title", x = 8f, y = 8f, width = 84f, height = 10f,
                    text = "Sales Performance Ledger",
                    fontSize = 24, isBold = true, color = "#0F172A", alignment = "LEFT"
                ),
                SlideElement.TableElement(
                    id = "s2_table", x = 8f, y = 22f, width = 84f, height = 65f,
                    rows = 4, cols = 3,
                    data = listOf(
                        listOf("Region", "Direct Sales", "Partner Sales"),
                        listOf("North America", "$24.5M", "$18.2M"),
                        listOf("Europe Union", "$19.8M", "$14.5M"),
                        listOf("Asia Pacific", "$32.4M", "$22.8M")
                    )
                )
            )
        )
        return Presentation(
            id = id,
            title = "Corporate Growth Review",
            slides = listOf(s1, s2),
            lastModified = System.currentTimeMillis()
        )
    }

    private fun createCreativePortfolio(): Presentation {
        val id = UUID.randomUUID().toString()
        val s1 = Slide(
            id = "slide_1",
            background = "#FDF2F8", // Soft Rose Pink
            elements = listOf(
                SlideElement.TextBox(
                    id = "s1_title", x = 10f, y = 30f, width = 80f, height = 24f,
                    text = "CREATIVE CANVAS",
                    fontSize = 38, isBold = true, color = "#DB2777", alignment = "CENTER",
                    fontFamily = "CURSIVE"
                ),
                SlideElement.TextBox(
                    id = "s1_sub", x = 15f, y = 56f, width = 70f, height = 12f,
                    text = "Visual ideas curated, crafted and delivered",
                    fontSize = 16, color = "#BE185D", alignment = "CENTER"
                ),
                SlideElement.Shape(
                    id = "s1_cloud", x = 43f, y = 12f, width = 14f, height = 12f,
                    shapeType = ShapeType.CLOUD, fillColor = "#FCE7F3", strokeColor = "#DB2777"
                )
            )
        )
        val s2 = Slide(
            id = "slide_2",
            background = "#FFF1F2",
            elements = listOf(
                SlideElement.TextBox(
                    id = "s2_title", x = 8f, y = 10f, width = 84f, height = 10f,
                    text = "Aesthetic Showcase",
                    fontSize = 26, isBold = true, color = "#9F1239", alignment = "LEFT"
                ),
                SlideElement.Picture(
                    id = "s2_pic", x = 20f, y = 24f, width = 60f, height = 55f,
                    uri = "android.resource://com.example/drawable/ic_launcher_foreground",
                    caption = "Featured Design Portfolio Artwork"
                )
            )
        )
        return Presentation(
            id = id,
            title = "Aesthetic Portfolio",
            slides = listOf(s1, s2),
            lastModified = System.currentTimeMillis()
        )
    }

    private fun createScienceLesson(): Presentation {
        val id = UUID.randomUUID().toString()
        val s1 = Slide(
            id = "slide_1",
            background = "#ECFDF5", // Light Mint Green
            elements = listOf(
                SlideElement.TextBox(
                    id = "s1_title", x = 10f, y = 28f, width = 80f, height = 22f,
                    text = "Astrophysics for Kids",
                    fontSize = 32, isBold = true, color = "#047857", alignment = "CENTER"
                ),
                SlideElement.TextBox(
                    id = "s1_sub", x = 15f, y = 52f, width = 70f, height = 12f,
                    text = "A journey through stellar mechanics and stars",
                    fontSize = 16, color = "#065F46", alignment = "CENTER"
                ),
                SlideElement.Shape(
                    id = "s1_star", x = 46f, y = 12f, width = 8f, height = 8f,
                    shapeType = ShapeType.STAR, fillColor = "#34D399", strokeColor = "#047857"
                )
            )
        )
        val s2 = Slide(
            id = "slide_2",
            background = "#ECFDF5",
            elements = listOf(
                SlideElement.TextBox(
                    id = "s2_title", x = 8f, y = 10f, width = 84f, height = 10f,
                    text = "Structure of a Solar Body",
                    fontSize = 24, isBold = true, color = "#065F46", alignment = "LEFT"
                ),
                SlideElement.Shape(
                    id = "s2_sun", x = 15f, y = 35f, width = 22f, height = 22f,
                    shapeType = ShapeType.CIRCLE, fillColor = "#F59E0B", strokeColor = "#B45309"
                ),
                SlideElement.Shape(
                    id = "s2_arrow", x = 42f, y = 43f, width = 12f, height = 6f,
                    shapeType = ShapeType.ARROW_RIGHT, fillColor = "#10B981", strokeColor = "#047857"
                ),
                SlideElement.Shape(
                    id = "s2_core", x = 58f, y = 35f, width = 26f, height = 22f,
                    shapeType = ShapeType.HEXAGON, fillColor = "#EF4444", strokeColor = "#B91C1C",
                    text = "Fusion Core"
                )
            )
        )
        return Presentation(
            id = id,
            title = "Solar Astrophysics",
            slides = listOf(s1, s2),
            lastModified = System.currentTimeMillis()
        )
    }

    private suspend fun seedDefaultPresentations() {
        // Automatically save initial templates to Room so they show up under Recents/This Device right away!
        repository.savePresentation(createStartupPitchDeck().copy(title = "Welcome to Presento"))
        repository.savePresentation(createMinimalCorporate())
    }
}
