package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    onSave: (color: Long?, iconName: String?, viewMode: String?, compactPreviews: Boolean) -> Unit
) {
    var selectedColor by remember { mutableStateOf(folder.color) }
    var selectedIcon by remember { mutableStateOf(folder.iconName) }
    var selectedViewMode by remember { mutableStateOf(folder.defaultViewModeString) }
    var showCompactPreviews by remember { mutableStateOf(folder.showCompactPreviews) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Folder Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 32.dp).heightIn(max = 200.dp)
            ) {
                items(predefinedColors) { colorLong ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colorLong?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedColor = colorLong }
                            .border(
                                width = if (selectedColor == colorLong) 3.dp else 0.dp,
                                color = if (selectedColor == colorLong) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorLong == null) {
                            Icon(Icons.Default.Clear, contentDescription = "Default color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (selectedColor == colorLong) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Text("Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 32.dp).heightIn(max = 200.dp)
            ) {
                items(FolderIcons.icons.keys.toList()) { iconName ->
                    val isSelected = selectedIcon == iconName || (selectedIcon == null && iconName == "folder")
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedIcon = if (iconName == "folder") null else iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            FolderIcons.getIcon(iconName),
                            contentDescription = iconName,
                            tint = if (isSelected) (selectedColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(selectedColor, selectedIcon, selectedViewMode, showCompactPreviews)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
