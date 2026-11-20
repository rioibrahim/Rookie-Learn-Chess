package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AnimationSpeed
import com.example.data.AppThemeMode
import com.example.data.UserSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
  settings: UserSettings,
  onDismiss: () -> Unit,
  onToggleAnimations: () -> Unit,
  onSelectSpeed: (AnimationSpeed) -> Unit,
  onToggleCaptureEffects: () -> Unit,
  onToggleHighlights: () -> Unit,
  onToggleArrows: () -> Unit,
  onToggleClearOnMove: () -> Unit,
  onToggleReducedMotion: () -> Unit,
  onToggleMuted: () -> Unit,
  onVolumeChange: (Float) -> Unit,
  onToggleBeginnerMode: () -> Unit,
  onSelectThemeMode: (AppThemeMode) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Board & App Settings",
        style = MaterialTheme.typography.headlineSmall,
      )

      Text(
        text = "Coaching Mode",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      SettingSwitchRow(
        title = if (settings.beginnerMode) "Beginner mode" else "Advanced mode",
        subtitle = "Explain algebraic notation symbols after every move",
        checked = settings.beginnerMode,
        onCheckedChange = { onToggleBeginnerMode() },
        testTag = "setting_beginner_mode",
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Text(
        text = "Theme",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AppThemeMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = settings.themeMode == mode,
            onClick = { onSelectThemeMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppThemeMode.entries.size),
            modifier = Modifier.testTag("theme_${mode.name.lowercase()}"),
          ) {
            Text(mode.label)
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Text(
        text = "Sound",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      SettingSwitchRow(
        title = "Sound effects",
        subtitle = "Move, capture, check, castle, and lesson cues",
        checked = !settings.muted,
        onCheckedChange = { onToggleMuted() },
        testTag = "setting_sound",
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Volume",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
          value = settings.volume,
          onValueChange = onVolumeChange,
          valueRange = 0f..1f,
          steps = 9,
          enabled = !settings.muted,
          modifier = Modifier.weight(1f).testTag("volume_slider"),
        )
        Text(
          text = "${(settings.volume * 100).roundToInt()}%",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Text(
        text = "Board & Motion",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FilterChip(
          selected = settings.animationsEnabled,
          onClick = onToggleAnimations,
          label = { Text("Animations") },
        )
        FilterChip(
          selected = settings.captureEffects,
          onClick = onToggleCaptureEffects,
          label = { Text("Capture effects") },
        )
        FilterChip(
          selected = settings.highlightsEnabled,
          onClick = onToggleHighlights,
          label = { Text("Highlights") },
        )
        FilterChip(
          selected = settings.arrowsEnabled,
          onClick = onToggleArrows,
          label = { Text("Arrows") },
        )
        FilterChip(
          selected = settings.clearDrawingsOnMove,
          onClick = onToggleClearOnMove,
          label = { Text("Clear drawings on move") },
        )
        FilterChip(
          selected = settings.reducedMotion,
          onClick = onToggleReducedMotion,
          label = { Text("Reduced motion") },
        )
      }

      Text(
        text = "Animation speed",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AnimationSpeed.entries.forEachIndexed { index, speed ->
          SegmentedButton(
            selected = settings.animationSpeed == speed,
            onClick = { onSelectSpeed(speed) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = AnimationSpeed.entries.size),
          ) {
            Text(speed.label)
          }
        }
      }

      Text(
        text = "Tip: Turn on Draw mode below the board, then tap any square to highlight or drag between squares to draw arrows.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(4.dp))
    }
  }
}

@Composable
private fun SettingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
