package com.example.data

import com.example.content.RookieCurriculum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.max

data class ProgressSnapshot(
  val completedStepKeys: Set<String> = emptySet(),
  val puzzleRating: Int = 800,
  val puzzlesSolved: Int = 0,
  val puzzlesAttempted: Int = 0,
  val puzzleStreak: Int = 0,
  val solvedPuzzleIndices: Set<Int> = emptySet(),
  val categoryStats: Map<String, PuzzleCategoryEntity> = emptyMap(),
  val wins: Int = 0,
  val draws: Int = 0,
  val losses: Int = 0,
) {
  val accuracyPercent: Int
    get() = if (puzzlesAttempted > 0) ((puzzlesSolved.toFloat() / puzzlesAttempted) * 100f).toInt() else 0
}

class ProgressRepository(
  private val dao: RookieDao,
  private val settingsStore: SettingsDataStore,
) {

  val settingsFlow: Flow<UserSettings> = settingsStore.settingsFlow

  val progressFlow: Flow<ProgressSnapshot> = combine(
    dao.observeCompletedLessons().map { list -> list.map { it.stepKey }.toSet() },
    dao.observePuzzleStats().map { it ?: PuzzleStatsEntity() },
    dao.observePuzzleCategories().map { list -> list.associateBy { it.category } },
    dao.observeSolvedPuzzles().map { list -> list.map { it.puzzleIndex }.toSet() },
    dao.observeGameRecord().map { it ?: GameRecordEntity() },
  ) { completedKeys, pzStats, cats, solvedIndices, gameRecord ->
    ProgressSnapshot(
      completedStepKeys = completedKeys,
      puzzleRating = pzStats.rating,
      puzzlesSolved = pzStats.solved,
      puzzlesAttempted = pzStats.attempted,
      puzzleStreak = pzStats.streak,
      solvedPuzzleIndices = solvedIndices,
      categoryStats = cats,
      wins = gameRecord.wins,
      draws = gameRecord.draws,
      losses = gameRecord.losses,
    )
  }

  suspend fun markLessonStepCompleted(lessonIndex: Int, stepIndex: Int) {
    dao.markLessonStepDone(
      LessonCompletionEntity(
        stepKey = RookieCurriculum.lessonStepKey(lessonIndex, stepIndex),
        lessonIndex = lessonIndex,
        stepIndex = stepIndex,
      )
    )
  }

  suspend fun recordPuzzleAttempt(puzzleIndex: Int, category: String, success: Boolean) {
    val currentStats = dao.getPuzzleStats() ?: PuzzleStatsEntity()
    val currentCat = dao.getPuzzleCategory(category) ?: PuzzleCategoryEntity(category = category)

    val nextStats = if (success) {
      currentStats.copy(
        solved = currentStats.solved + 1,
        attempted = currentStats.attempted + 1,
        streak = currentStats.streak + 1,
        rating = currentStats.rating + 12,
      )
    } else {
      currentStats.copy(
        attempted = currentStats.attempted + 1,
        streak = 0,
        rating = max(400, currentStats.rating - 8),
      )
    }
    dao.upsertPuzzleStats(nextStats)

    val nextCat = if (success) {
      currentCat.copy(attempted = currentCat.attempted + 1, solved = currentCat.solved + 1)
    } else {
      currentCat.copy(attempted = currentCat.attempted + 1)
    }
    dao.upsertPuzzleCategory(nextCat)

    if (success) {
      dao.markPuzzleSolved(SolvedPuzzleEntity(puzzleIndex = puzzleIndex))
    }
  }

  suspend fun recordGameOutcome(win: Boolean = false, draw: Boolean = false, loss: Boolean = false) {
    val current = dao.getGameRecord() ?: GameRecordEntity()
    dao.upsertGameRecord(
      current.copy(
        wins = current.wins + if (win) 1 else 0,
        draws = current.draws + if (draw) 1 else 0,
        losses = current.losses + if (loss) 1 else 0,
      )
    )
  }

  suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
    settingsStore.update(transform)
  }
}
