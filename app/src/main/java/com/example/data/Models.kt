package com.example.data

import org.json.JSONArray
import org.json.JSONObject

// Presentation Core Data Class
data class Presentation(
    val id: String,
    val title: String,
    val slides: List<Slide>,
    val lastModified: Long,
    val isCloudSynced: Boolean = false,
    val comments: List<Comment> = emptyList()
)

// Slide Model
data class Slide(
    val id: String,
    val background: String = "#FFFFFF", // Hex color
    val transition: SlideTransition = SlideTransition.NONE,
    val elements: List<SlideElement> = emptyList()
)

// Transition Effects
enum class SlideTransition {
    NONE, FADE, SLIDE, ZOOM, WIPE, SPLIT, PUSH, REVEAL
}

// Element Animation Effects
enum class ElementAnimation {
    NONE, FADE_IN, ZOOM_IN, SLIDE_IN, BOUNCE, SPIN
}

// Supported Shapes for diagrams & flowcharts
enum class ShapeType {
    CIRCLE, SQUARE, ARROW_RIGHT, ARROW_LEFT, ARROW_UP, ARROW_DOWN, LINE, TRIANGLE, STAR, HEXAGON, CLOUD
}

// Supported Charts for data visualization
enum class ChartType {
    BAR, LINE, PIE
}

// Comments on slides/elements for collaboration
data class Comment(
    val id: String,
    val slideId: String,
    val author: String,
    val text: String,
    val timestamp: Long
)

// Slide elements
sealed class SlideElement {
    abstract val id: String
    abstract val x: Float // Percentage 0..100
    abstract val y: Float // Percentage 0..100
    abstract val width: Float // Percentage 0..100
    abstract val height: Float // Percentage 0..100
    abstract val animation: ElementAnimation

    abstract fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float): SlideElement
    abstract fun copyWithNewAnimation(animation: ElementAnimation): SlideElement

    data class TextBox(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val animation: ElementAnimation = ElementAnimation.NONE,
        val text: String = "Double tap to edit",
        val fontSize: Int = 18,
        val color: String = "#000000",
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val alignment: String = "LEFT", // LEFT, CENTER, RIGHT
        val fontFamily: String = "SANS_SERIF"
    ) : SlideElement() {
        override fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float) =
            this.copy(x = x, y = y, width = width, height = height)
        override fun copyWithNewAnimation(animation: ElementAnimation) =
            this.copy(animation = animation)
    }

    data class Shape(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val animation: ElementAnimation = ElementAnimation.NONE,
        val shapeType: ShapeType = ShapeType.SQUARE,
        val fillColor: String = "#FF9800",
        val strokeColor: String = "#E65100",
        val strokeWidth: Float = 2f,
        val text: String = "" // Inner text
    ) : SlideElement() {
        override fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float) =
            this.copy(x = x, y = y, width = width, height = height)
        override fun copyWithNewAnimation(animation: ElementAnimation) =
            this.copy(animation = animation)
    }

    data class Picture(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val animation: ElementAnimation = ElementAnimation.NONE,
        val uri: String, // local path, gallery URI or dynamic asset
        val caption: String = ""
    ) : SlideElement() {
        override fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float) =
            this.copy(x = x, y = y, width = width, height = height)
        override fun copyWithNewAnimation(animation: ElementAnimation) =
            this.copy(animation = animation)
    }

    data class TableElement(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val animation: ElementAnimation = ElementAnimation.NONE,
        val rows: Int = 3,
        val cols: Int = 3,
        val data: List<List<String>> = List(3) { List(3) { "" } }
    ) : SlideElement() {
        override fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float) =
            this.copy(x = x, y = y, width = width, height = height)
        override fun copyWithNewAnimation(animation: ElementAnimation) =
            this.copy(animation = animation)
    }

    data class ChartElement(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val animation: ElementAnimation = ElementAnimation.NONE,
        val chartType: ChartType = ChartType.BAR,
        val title: String = "Performance",
        val labels: List<String> = listOf("Jan", "Feb", "Mar", "Apr"),
        val values: List<Float> = listOf(40f, 65f, 50f, 85f)
    ) : SlideElement() {
        override fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float) =
            this.copy(x = x, y = y, width = width, height = height)
        override fun copyWithNewAnimation(animation: ElementAnimation) =
            this.copy(animation = animation)
    }

    data class VideoElement(
        override val id: String,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        override val animation: ElementAnimation = ElementAnimation.NONE,
        val videoUrl: String,
        val isPlaying: Boolean = false
    ) : SlideElement() {
        override fun copyWithNewBounds(x: Float, y: Float, width: Float, height: Float) =
            this.copy(x = x, y = y, width = width, height = height)
        override fun copyWithNewAnimation(animation: ElementAnimation) =
            this.copy(animation = animation)
    }
}

// Built-in JSON Serialization / Deserialization helpers using org.json
object JsonConverter {

    fun serializeSlides(slides: List<Slide>): String {
        val array = JSONArray()
        for (slide in slides) {
            val sObj = JSONObject()
            sObj.put("id", slide.id)
            sObj.put("background", slide.background)
            sObj.put("transition", slide.transition.name)

            val elemArray = JSONArray()
            for (elem in slide.elements) {
                val eObj = JSONObject()
                eObj.put("id", elem.id)
                eObj.put("x", elem.x)
                eObj.put("y", elem.y)
                eObj.put("width", elem.width)
                eObj.put("height", elem.height)
                eObj.put("animation", elem.animation.name)

                when (elem) {
                    is SlideElement.TextBox -> {
                        eObj.put("type", "TEXT")
                        eObj.put("text", elem.text)
                        eObj.put("fontSize", elem.fontSize)
                        eObj.put("color", elem.color)
                        eObj.put("isBold", elem.isBold)
                        eObj.put("isItalic", elem.isItalic)
                        eObj.put("isUnderline", elem.isUnderline)
                        eObj.put("alignment", elem.alignment)
                        eObj.put("fontFamily", elem.fontFamily)
                    }
                    is SlideElement.Shape -> {
                        eObj.put("type", "SHAPE")
                        eObj.put("shapeType", elem.shapeType.name)
                        eObj.put("fillColor", elem.fillColor)
                        eObj.put("strokeColor", elem.strokeColor)
                        eObj.put("strokeWidth", elem.strokeWidth.toDouble())
                        eObj.put("text", elem.text)
                    }
                    is SlideElement.Picture -> {
                        eObj.put("type", "PICTURE")
                        eObj.put("uri", elem.uri)
                        eObj.put("caption", elem.caption)
                    }
                    is SlideElement.TableElement -> {
                        eObj.put("type", "TABLE")
                        eObj.put("rows", elem.rows)
                        eObj.put("cols", elem.cols)
                        val dataArray = JSONArray()
                        for (row in elem.data) {
                            val rArray = JSONArray()
                            for (cell in row) {
                                rArray.put(cell)
                            }
                            dataArray.put(rArray)
                        }
                        eObj.put("data", dataArray)
                    }
                    is SlideElement.ChartElement -> {
                        eObj.put("type", "CHART")
                        eObj.put("chartType", elem.chartType.name)
                        eObj.put("title", elem.title)
                        
                        val lArray = JSONArray()
                        elem.labels.forEach { lArray.put(it) }
                        eObj.put("labels", lArray)

                        val vArray = JSONArray()
                        elem.values.forEach { vArray.put(it.toDouble()) }
                        eObj.put("values", vArray)
                    }
                    is SlideElement.VideoElement -> {
                        eObj.put("type", "VIDEO")
                        eObj.put("videoUrl", elem.videoUrl)
                    }
                }
                elemArray.put(eObj)
            }
            sObj.put("elements", elemArray)
            array.put(sObj)
        }
        return array.toString()
    }

    fun deserializeSlides(jsonStr: String): List<Slide> {
        if (jsonStr.isEmpty()) return emptyList()
        val list = mutableListOf<Slide>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val sObj = array.getJSONObject(i)
                val id = sObj.optString("id", "slide_$i")
                val background = sObj.optString("background", "#FFFFFF")
                val transitionStr = sObj.optString("transition", "NONE")
                val transition = try { SlideTransition.valueOf(transitionStr) } catch(e: Exception) { SlideTransition.NONE }

                val elemArray = sObj.optJSONArray("elements") ?: JSONArray()
                val elements = mutableListOf<SlideElement>()

                for (j in 0 until elemArray.length()) {
                    val eObj = elemArray.getJSONObject(j)
                    val eId = eObj.optString("id", "elem_$j")
                    val x = eObj.optDouble("x", 10.0).toFloat()
                    val y = eObj.optDouble("y", 10.0).toFloat()
                    val w = eObj.optDouble("width", 80.0).toFloat()
                    val h = eObj.optDouble("height", 20.0).toFloat()
                    val animStr = eObj.optString("animation", "NONE")
                    val animation = try { ElementAnimation.valueOf(animStr) } catch(e: Exception) { ElementAnimation.NONE }

                    val type = eObj.optString("type", "TEXT")
                    when (type) {
                        "TEXT" -> {
                            elements.add(
                                SlideElement.TextBox(
                                    id = eId, x = x, y = y, width = w, height = h, animation = animation,
                                    text = eObj.optString("text", ""),
                                    fontSize = eObj.optInt("fontSize", 18),
                                    color = eObj.optString("color", "#000000"),
                                    isBold = eObj.optBoolean("isBold", false),
                                    isItalic = eObj.optBoolean("isItalic", false),
                                    isUnderline = eObj.optBoolean("isUnderline", false),
                                    alignment = eObj.optString("alignment", "LEFT"),
                                    fontFamily = eObj.optString("fontFamily", "SANS_SERIF")
                                )
                            )
                        }
                        "SHAPE" -> {
                            val stStr = eObj.optString("shapeType", "SQUARE")
                            val shapeType = try { ShapeType.valueOf(stStr) } catch(e: Exception) { ShapeType.SQUARE }
                            elements.add(
                                SlideElement.Shape(
                                    id = eId, x = x, y = y, width = w, height = h, animation = animation,
                                    shapeType = shapeType,
                                    fillColor = eObj.optString("fillColor", "#FF9800"),
                                    strokeColor = eObj.optString("strokeColor", "#E65100"),
                                    strokeWidth = eObj.optDouble("strokeWidth", 2.0).toFloat(),
                                    text = eObj.optString("text", "")
                                )
                            )
                        }
                        "PICTURE" -> {
                            elements.add(
                                SlideElement.Picture(
                                    id = eId, x = x, y = y, width = w, height = h, animation = animation,
                                    uri = eObj.optString("uri", ""),
                                    caption = eObj.optString("caption", "")
                                )
                            )
                        }
                        "TABLE" -> {
                            val rows = eObj.optInt("rows", 3)
                            val cols = eObj.optInt("cols", 3)
                            val dataArray = eObj.optJSONArray("data") ?: JSONArray()
                            val data = MutableList(rows) { MutableList(cols) { "" } }
                            for (r in 0 until dataArray.length()) {
                                if (r >= rows) break
                                val rowArr = dataArray.getJSONArray(r)
                                for (c in 0 until rowArr.length()) {
                                    if (c >= cols) break
                                    data[r][c] = rowArr.optString(c, "")
                                }
                            }
                            elements.add(
                                SlideElement.TableElement(
                                    id = eId, x = x, y = y, width = w, height = h, animation = animation,
                                    rows = rows, cols = cols, data = data
                                )
                            )
                        }
                        "CHART" -> {
                            val ctStr = eObj.optString("chartType", "BAR")
                            val chartType = try { ChartType.valueOf(ctStr) } catch(e: Exception) { ChartType.BAR }
                            val title = eObj.optString("title", "Performance")

                            val lArr = eObj.optJSONArray("labels") ?: JSONArray()
                            val labels = mutableListOf<String>()
                            for (l in 0 until lArr.length()) {
                                labels.add(lArr.optString(l))
                            }

                            val vArr = eObj.optJSONArray("values") ?: JSONArray()
                            val values = mutableListOf<Float>()
                            for (v in 0 until vArr.length()) {
                                values.add(vArr.optDouble(v).toFloat())
                            }

                            elements.add(
                                SlideElement.ChartElement(
                                    id = eId, x = x, y = y, width = w, height = h, animation = animation,
                                    chartType = chartType, title = title, labels = labels, values = values
                                )
                            )
                        }
                        "VIDEO" -> {
                            elements.add(
                                SlideElement.VideoElement(
                                    id = eId, x = x, y = y, width = w, height = h, animation = animation,
                                    videoUrl = eObj.optString("videoUrl", "")
                                )
                            )
                        }
                    }
                }
                list.add(Slide(id = id, background = background, transition = transition, elements = elements))
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeComments(comments: List<Comment>): String {
        val array = JSONArray()
        for (c in comments) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("slideId", c.slideId)
            obj.put("author", c.author)
            obj.put("text", c.text)
            obj.put("timestamp", c.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeComments(jsonStr: String): List<Comment> {
        if (jsonStr.isEmpty()) return emptyList()
        val list = mutableListOf<Comment>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Comment(
                        id = obj.optString("id"),
                        slideId = obj.optString("slideId"),
                        author = obj.optString("author"),
                        text = obj.optString("text"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
