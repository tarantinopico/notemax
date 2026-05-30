package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entities.FolderEntity

val predefinedColors = listOf(
    null, // Default
    0xFFE53935, // Red
    0xFFD81B60, // Pink
    0xFF8E24AA, // Purple
    0xFF5E35B1, // Deep Purple
    0xFF3949AB, // Indigo
    0xFF1E88E5, // Blue
    0xFF039BE5, // Light Blue
    0xFF00ACC1, // Cyan
    0xFF00897B, // Teal
    0xFF43A047, // Green
    0xFF7CB342, // Light Green
    0xFFFDD835, // Yellow
    0xFFFFB300, // Amber
    0xFFFB8C00, // Orange
    0xFFF4511E, // Deep Orange
    0xFF6D4C41, // Brown
    0xFF757575, // Grey
    0xFF546E7A  // Blue Grey
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSettingsSheet(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onSave: (color: Long?, iconName: String?, viewMode: String?, compactPreviews: Boolean, onSuccess: () -> Unit) -> Unit
) {
    var selectedColor by remember { mutableStateOf(folder.color) }
    var selectedIcon by remember { mutableStateOf(folder.iconName) }
    var selectedViewMode by remember { mutableStateOf(folder.defaultViewModeString) }
    var showCompactPreviews by remember { mutableStateOf(folder.showCompactPreviews) }
    var isSaving by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Folder Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                
                // Colors Grid
                val colorColumns = 6
                val colorRows = (predefinedColors.size + colorColumns - 1) / colorColumns
                Column(modifier = Modifier.padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (row in 0 until colorRows) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (col in 0 until colorColumns) {
                                val idx = row * colorColumns + col
                                if (idx < predefinedColors.size) {
                                    val colorLong = predefinedColors[idx]
                                    val isSelected = selectedColor == colorLong
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(colorLong?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedColor = colorLong }
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (colorLong == null) {
                                            Icon(Icons.Default.Clear, contentDescription = "Default color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(44.dp))
                                }
                            }
                        }
                    }
                }

                Text("Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                
                // Icons Grid
                val icons = FolderIcons.icons.keys.toList()
                val iconColumns = 5
                val iconRows = (icons.size + iconColumns - 1) / iconColumns
                Column(modifier = Modifier.padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (row in 0 until iconRows) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            for (col in 0 until iconColumns) {
                                val idx = row * iconColumns + col
                                if (idx < icons.size) {
                                    val iconName = icons[idx]
                                    val isSelected = selectedIcon == iconName || (selectedIcon == null && iconName == "folder")
                                    val iconBg = if (isSelected) (selectedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer) else MaterialTheme.colorScheme.surfaceVariant
                                    val iconTint = if (isSelected) (selectedColor?.let { Color.White } ?: MaterialTheme.colorScheme.onPrimaryContainer) else MaterialTheme.colorScheme.onSurfaceVariant
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(iconBg)
                                            .clickable { selectedIcon = if (iconName == "folder") null else iconName }
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            FolderIcons.getIcon(iconName),
                                            contentDescription = iconName,
                                            tint = iconTint
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(52.dp))
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compact Previews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Show snippet of notes in folder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = showCompactPreviews, onCheckedChange = { showCompactPreviews = it })
                }

                Text("Default View Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                SegmentedViewModePicker(selectedViewMode = selectedViewMode) { mode ->
                    selectedViewMode = mode
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Bottom Save Button
            Button(
                onClick = {
                    isSaving = true
                    onSave(selectedColor, selectedIcon, selectedViewMode, showCompactPreviews) {
                        isSaving = false
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Apply Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SegmentedViewModePicker(selectedViewMode: String?, onSelected: (String?) -> Unit) {
    val options = listOf(null to "Inherit", "LIST" to "List", "GRID" to "Grid", "TABLE" to "Table")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, label) ->
            val isSelected = selectedViewMode == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onSelected(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
