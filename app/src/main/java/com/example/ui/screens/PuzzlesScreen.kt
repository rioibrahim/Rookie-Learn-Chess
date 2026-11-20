package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.content.RookieCurriculum
import com.example.ui.RookieUiState
import com.example.ui.components.ExplanationCard
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.RookieCard

@Composable
fun PuzzlesPanel(
  state: RookieUiState,
  onPreviousPuzzle: () -> Unit,
  onNextPuzzle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val puzzles = RookieCurriculum.puzzles
  val puzzle = puzzles[state.puzzleIndex]
  val solvedBefore = state.puzzleIndex in state.progress.solvedPuzzleIndices

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RookieCard {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Puzzle ${state.puzzleIndex + 1}/${puzzles.size} · ${puzzle.category}",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1f),
        )
        if (solvedBefore) {
          Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Puzzle solved",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Text(
        text = "${state.turn.displayName()} to move",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
      )
      FeedbackBanner(
        message = state.message,
        isError = state.isErrorMessage,
      )
      if (state.hintLevel > 0 && state.hintMessage.isNotBlank()) {
        FeedbackBanner(
          message = state.hintMessage,
          isHint = true,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onPreviousPuzzle,
          enabled = state.puzzleIndex > 0,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("prev_puzzle_button"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Previous")
        }
        Button(
          onClick = onNextPuzzle,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("next_puzzle_button"),
        ) {
          Text("Next puzzle")
          Spacer(Modifier.width(4.dp))
          Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
      }
    }

    ExplanationCard(explanation = state.explanation)

    RookieCard {
      Text(
        text = "Rating ${state.progress.puzzleRating} · solved ${state.progress.puzzlesSolved}/${state.progress.puzzlesAttempted} attempted · streak ${state.progress.puzzleStreak}. ${puzzles.size} verified puzzles (mate-in-1 and a fork).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
