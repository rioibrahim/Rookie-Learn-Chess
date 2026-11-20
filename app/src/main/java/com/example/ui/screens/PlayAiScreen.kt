package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.chess.AI_LEVELS
import com.example.chess.PieceColor
import com.example.ui.RookieUiState
import com.example.ui.components.ExplanationCard
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.MoveHistoryCard
import com.example.ui.components.RookieCard
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayAiPanel(
  state: RookieUiState,
  onSelectLevel: (Int) -> Unit,
  onNewGame: (PieceColor?) -> Unit,
  onResign: () -> Unit,
  onCopiedFeedback: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboard = LocalClipboardManager.current

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RookieCard(title = "Play vs AI") {
      if (state.gameOverText.isNotEmpty()) {
        FeedbackBanner(message = state.gameOverText)
      } else {
        val statusText = buildString {
          if (state.isAiThinking) {
            append("Thinking…")
          } else if (state.turn == state.playerColor) {
            append("Your move (${state.playerColor.displayName()})")
          } else {
            append("Opponent's turn")
          }
          if (state.inCheckSquare != null) append(" · CHECK")
        }
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodyMedium,
          color = if (state.inCheckSquare != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
      }

      if (state.isAiThinking && state.aiTelemetryText.isNotBlank()) {
        Text(
          text = state.aiTelemetryText,
          style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (state.hintLevel > 0 && state.hintMessage.isNotBlank()) {
        FeedbackBanner(message = state.hintMessage, isHint = true)
      }

      Text(
        text = "Difficulty",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        AI_LEVELS.forEachIndexed { idx, level ->
          val selected = idx == state.aiLevelIndex
          FilterChip(
            selected = selected,
            onClick = { onSelectLevel(idx) },
            label = { Text(level.name) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.testTag("ai_level_${level.name.lowercase()}"),
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = { onNewGame(PieceColor.WHITE) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("new_white_button"),
        ) {
          Text("New as White")
        }
        OutlinedButton(
          onClick = { onNewGame(PieceColor.BLACK) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("new_black_button"),
        ) {
          Text("as Black")
        }
        OutlinedButton(
          onClick = { onNewGame(null) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.heightIn(min = 48.dp).testTag("new_random_button"),
        ) {
          Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Random")
        }
        OutlinedButton(
          onClick = onResign,
          enabled = state.gameOverText.isEmpty() && state.moveHistory.isNotEmpty(),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.heightIn(min = 48.dp).testTag("resign_button"),
        ) {
          Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Resign")
        }
      }

      Text(
        text = "Engine: Built-in Alpha-Beta search (${AI_LEVELS[state.aiLevelIndex].name}, depth ${AI_LEVELS[state.aiLevelIndex].depth}).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    ExplanationCard(explanation = state.explanation)
    MoveHistoryCard(moves = state.moveHistory)

    RookieCard(title = "Export") {
      ExportRow(
        label = "FEN",
        value = state.fen,
        onCopy = {
          clipboard.setText(AnnotatedString(state.fen))
          onCopiedFeedback("FEN copied")
        },
        testTag = "copy_fen_button",
      )
      ExportRow(
        label = "PGN",
        value = state.pgn.ifBlank { "—" },
        onCopy = if (state.pgn.isNotBlank()) {
          {
            clipboard.setText(AnnotatedString(state.pgn))
            onCopiedFeedback("PGN copied")
          }
        } else null,
        testTag = "copy_pgn_button",
      )
    }
  }
}

@Composable
internal fun ExportRow(
  label: String,
  value: String,
  onCopy: (() -> Unit)?,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    if (onCopy != null) {
      IconButton(
        onClick = onCopy,
        modifier = Modifier.testTag(testTag),
      ) {
        Icon(
          imageVector = Icons.Rounded.ContentCopy,
          contentDescription = "Copy $label",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
