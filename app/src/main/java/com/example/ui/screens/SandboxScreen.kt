package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ui.RookieUiState
import com.example.ui.components.ExplanationCard
import com.example.ui.components.MoveHistoryCard
import com.example.ui.components.RookieCard

@Composable
fun SandboxPanel(
  state: RookieUiState,
  onResetBoard: () -> Unit,
  onCopiedFeedback: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboard = LocalClipboardManager.current

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RookieCard(title = "Sandbox") {
      Text(
        text = "Move any side freely (legal moves only), undo/redo, draw arrows, and copy the FEN.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onResetBoard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("sandbox_reset_button"),
      ) {
        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Reset board")
      }
    }

    ExplanationCard(explanation = state.explanation)
    MoveHistoryCard(moves = state.moveHistory)

    RookieCard(title = "Position") {
      ExportRow(
        label = "FEN",
        value = state.fen,
        onCopy = {
          clipboard.setText(AnnotatedString(state.fen))
          onCopiedFeedback("FEN copied")
        },
        testTag = "sandbox_copy_fen_button",
      )
    }
  }
}
