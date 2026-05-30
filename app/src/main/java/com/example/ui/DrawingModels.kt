package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import org.json.JSONArray
import org.json.JSONObject

enum class ToolType { PEN, HIGHLIGHTER, ERASER }

data class DrawingStroke(
    val points: List<Offset>,
    val color: Long,
    val strokeWidth: Float,
    val toolType: ToolType
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
        return path
    }
}

object DrawingSerializer {
    fun serialize(strokes: List<DrawingStroke>): String {
        val array = JSONArray()
        for (stroke in strokes) {
            val obj = JSONObject()
            obj.put("c", stroke.color)
            obj.put("w", stroke.strokeWidth.toDouble())
            obj.put("t", stroke.toolType.name)
            val pointsArray = JSONArray()
            for (p in stroke.points) {
                val pObj = JSONObject()
                pObj.put("x", p.x.toDouble())
                pObj.put("y", p.y.toDouble())
                pointsArray.put(pObj)
            }
            obj.put("p", pointsArray)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserialize(json: String): List<DrawingStroke> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<DrawingStroke>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val color = obj.getLong("c")
                val strokeWidth = obj.getDouble("w").toFloat()
                val toolType = ToolType.valueOf(obj.getString("t"))
                val pointsArray = obj.getJSONArray("p")
                val points = mutableListOf<Offset>()
                for (j in 0 until pointsArray.length()) {
                    val pObj = pointsArray.getJSONObject(j)
                    points.add(Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                }
                list.add(DrawingStroke(points, color, strokeWidth, toolType))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
