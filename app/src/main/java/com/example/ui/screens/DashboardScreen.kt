package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.content.RookieCurriculum
import com.example.ui.RookieTab
import com.example.ui.RookieUiState
import com.example.ui.components.RookieCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardPanel(
  state: RookieUiState,
  onNavigateTab: (RookieTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val totalSteps = RookieCurriculum.totalLessonSteps
  val completedSteps = state.progress.completedStepKeys.size
  val fraction = if (totalSteps > 0) (completedSteps.toFloat() / totalSteps).coerceIn(0f, 1f) else 0f

  val nextIncompleteIdx = RookieCurriculum.lessons.indexOfFirst { lesson ->
    val lIdx = RookieCurriculum.lessons.indexOf(lesson)
    lesson.steps.indices.any { sIdx ->
      RookieCurriculum.lessonStepKey(lIdx, sIdx) !in state.progress.completedStepKeys
    }
  }

  val weakestCategory = state.progress.categoryStats.values
    .filter { it.attempted > 0 && it.solved < it.attempted }
    .minByOrNull { it.solved.toFloat() / it.attempted }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RookieCard(title = "Your progress") {
      LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )
      Text(
        text = "$completedSteps of $totalSteps lesson steps completed",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StatTile(
          value = "${state.progress.puzzleRating}",
          label = "Puzzle rating",
          modifier = Modifier.weight(1f),
        )
        StatTile(
          value = "${state.progress.accuracyPercent}%",
          label = "Accuracy",
          modifier = Modifier.weight(1f),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StatTile(
          value = "${state.progress.puzzleStreak}",
          label = "Streak",
          modifier = Modifier.weight(1f),
        )
        StatTile(
          value = "${state.progress.wins}/${state.progress.draws}/${state.progress.losses}",
          label = "W/D/L vs AI",
          modifier = Modifier.weight(1f),
        )
      }
    }

    RookieCard(title = "Recommended next") {
      val recText = if (nextIncompleteIdx < 0) {
        "All piece lessons done! Try puzzles or a game against the AI."
      } else {
        "Continue with the ${RookieCurriculum.lessons[nextIncompleteIdx].title} lesson."
      }
      Text(
        text = recText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (weakestCategory != null) {
        Text(
          text = "Weakest puzzle category: ${weakestCategory.category}.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = { onNavigateTab(RookieTab.LEARN) },
          modifier = Modifier.heightIn(min = 48.dp).testTag("dash_learn_button"),
        ) {
          Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Start learning")
        }
        FilledTonalButton(
          onClick = { onNavigateTab(RookieTab.PUZZLES) },
          modifier = Modifier.heightIn(min = 48.dp).testTag("dash_puzzles_button"),
        ) {
          Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Puzzles")
        }
        FilledTonalButton(
          onClick = { onNavigateTab(RookieTab.PLAY) },
          modifier = Modifier.heightIn(min = 48.dp).testTag("dash_play_button"),
        ) {
          Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Play the AI")
        }
      }
    }

    RookieCard {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = "Quick start",
          style = MaterialTheme.typography.titleSmall,
        )
      }
      Text(
        text = "New to chess? Start with Learn → Pawn. Tap or drag a piece to see its legal moves (dots), make a move, and use Hint up to 4 times for progressively clearer help. All progress is saved offline on this device.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun StatTile(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
