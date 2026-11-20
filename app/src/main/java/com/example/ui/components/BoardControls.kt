package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoardControlsRow(
  showGameControls: Boolean,
  showRestart: Boolean,
  canUndo: Boolean,
  canRedo: Boolean,
  hintCount: Int,
  maxHints: Int,
  drawMode: Boolean,
  selectedDrawColor: DrawColor,
  hasDrawings: Boolean,
  onFlip: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onHint: () -> Unit,
  onRestart: () -> Unit,
  onToggleDrawMode: () -> Unit,
  onSelectDrawColor: (DrawColor) -> Unit,
  onClearDrawings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ControlChipButton(
        icon = Icons.Rounded.SwapVert,
        label = "Flip",
        onClick = onFlip,
        testTag = "flip_button",
      )
      if (showGameControls) {
        ControlChipButton(
          icon = Icons.AutoMirrored.Rounded.Undo,
          label = "Undo",
          enabled = canUndo,
          onClick = onUndo,
          testTag = "undo_button",
        )
        ControlChipButton(
          icon = Icons.AutoMirrored.Rounded.Redo,
          label = "Redo",
          enabled = canRedo,
          onClick = onRedo,
          testTag = "redo_button",
        )
        val hintLabel = if (hintCount > 0 && maxHints > 0) "Hint ($hintCount/$maxHints)" else "Hint"
        ControlChipButton(
          icon = Icons.Rounded.Lightbulb,
          label = hintLabel,
          onClick = onHint,
          testTag = "hint_button",
        )
      }
      if (showRestart) {
        ControlChipButton(
          icon = Icons.Rounded.Refresh,
          label = "Restart",
          onClick = onRestart,
          testTag = "restart_button",
        )
      }
      ControlChipButton(
        icon = Icons.Rounded.Edit,
        label = if (drawMode) "Draw ON" else "Draw",
        selected = drawMode,
        onClick = onToggleDrawMode,
        testTag = "draw_mode_button",
      )
      if (hasDrawings) {
        ControlChipButton(
          icon = Icons.Rounded.ClearAll,
          label = "Clear",
          onClick = onClearDrawings,
          testTag = "clear_drawings_button",
        )
      }
    }

    AnimatedVisibility(visible = drawMode) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.Center,
      ) {
        DrawColor.entries.forEach { dc ->
          val isSelected = dc == selectedDrawColor
          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(34.dp)
              .clip(CircleShape)
              .background(dc.color)
              .then(
                if (isSelected) {
                  Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                  Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
              )
              .clickable { onSelectDrawColor(dc) }
              .semantics { contentDescription = "Draw color ${dc.label}" }
              .testTag("draw_color_${dc.name.lowercase()}"),
          )
        }
      }
    }
  }
}

@Composable
private fun ControlChipButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  testTag: String,
  enabled: Boolean = true,
  selected: Boolean = false,
) {
  if (selected) {
    FilledTonalButton(
      onClick = onClick,
      enabled = enabled,
      shape = RoundedCornerShape(10.dp),
      colors = ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag(testTag),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(5.dp))
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
  } else {
    OutlinedButton(
      onClick = onClick,
      enabled = enabled,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag(testTag),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(5.dp))
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
  }
}
