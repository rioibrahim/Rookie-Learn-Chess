package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
fun LearnPanel(
  state: RookieUiState,
  onSelectLesson: (Int) -> Unit,
  onNextStep: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val lesson = RookieCurriculum.lessons[state.lessonIndex]

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    RookieCard(title = "${lesson.title} lesson — step ${state.stepIndex + 1}/${lesson.steps.size}") {
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
      Button(
        onClick = onNextStep,
        enabled = state.stepSolved,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("next_step_button"),
      ) {
        Text("Next step")
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    }

    ExplanationCard(explanation = state.explanation)

    RookieCard(title = "Lessons") {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RookieCurriculum.lessons.forEachIndexed { idx, item ->
          val completed = item.steps.indices.all { sIdx ->
            RookieCurriculum.lessonStepKey(idx, sIdx) in state.progress.completedStepKeys
          }
          val isSelected = idx == state.lessonIndex
          Surface(
            onClick = { onSelectLesson(idx) },
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("lesson_item_${item.title.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${item.steps.size} ${if (item.steps.size == 1) "step" else "steps"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (completed) "Completed" else "Not completed",
                tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
      Text(
        text = "Openings, endgames, tactics & strategy lessons are not built yet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
