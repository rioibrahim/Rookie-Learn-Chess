package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.ChessMove
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRookieColors

@Composable
fun FeedbackBanner(
  message: String,
  isError: Boolean = false,
  isHint: Boolean = false,
  modifier: Modifier = Modifier,
) {
  if (message.isBlank()) return
  val rookieColors = LocalRookieColors.current
  val accentColor = when {
    isError -> rookieColors.dangerAccent
    isHint -> rookieColors.hintBannerBorder
    else -> rookieColors.successAccent
  }
  val bgColor = accentColor.copy(alpha = 0.13f)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .drawBehind {
        drawRect(
          color = accentColor,
          topLeft = Offset.Zero,
          size = Size(4.dp.toPx(), size.height),
        )
      }
      .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
fun RookieCard(
  modifier: Modifier = Modifier,
  title: String? = null,
  content: @Composable () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (!title.isNullOrBlank()) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      content()
    }
  }
}

@Composable
fun ExplanationCard(
  explanation: String,
  modifier: Modifier = Modifier,
) {
  if (explanation.isBlank()) return
  RookieCard(title = "What just happened?", modifier = modifier) {
    Text(
      text = explanation,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
fun MoveHistoryCard(
  moves: List<ChessMove>,
  modifier: Modifier = Modifier,
) {
  if (moves.isEmpty()) return
  val formatted = buildString {
    moves.forEachIndexed { idx, move ->
      if (idx % 2 == 0) {
        if (idx > 0) append("  ")
        append("${idx / 2 + 1}. ")
      } else {
        append(' ')
      }
      append(move.san)
    }
  }
  RookieCard(title = "Moves", modifier = modifier) {
    Text(
      text = formatted,
      style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
